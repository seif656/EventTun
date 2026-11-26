package com.eventtun.ticket.service;

import com.eventtun.ticket.dto.IssueTicketsRequest;
import com.eventtun.ticket.exception.BadRequestException;
import com.eventtun.ticket.exception.ConflictException;
import com.eventtun.ticket.exception.TicketNotFoundException;
import com.eventtun.ticket.model.Ticket;
import com.eventtun.ticket.model.TicketStatus;
import com.eventtun.ticket.model.TicketValidation;
import com.eventtun.ticket.model.ValidationResult;
import com.eventtun.ticket.repository.TicketRepository;
import com.eventtun.ticket.repository.TicketValidationRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Couche metier du Ticket Service (Validation 2).
 *
 * Regles principales :
 *  - emission idempotente par orderId (rejeu identique -> billets existants, sinon 409) ;
 *  - passage VALID -> USED et VALID -> CANCELLED par UNE SEULE mise a jour conditionnelle
 *    (findAndModify) : deux controleurs simultanes ne peuvent pas valider le meme billet ;
 *  - chaque tentative de validation est enregistree (acceptee ou refusee).
 */
@Service
public class TicketService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_CODE_ATTEMPTS = 10;

    private final TicketRepository ticketRepository;
    private final TicketValidationRepository validationRepository;
    private final MongoTemplate mongoTemplate;
    private final SecureRandom random = new SecureRandom();

    public TicketService(TicketRepository ticketRepository,
                         TicketValidationRepository validationRepository,
                         MongoTemplate mongoTemplate) {
        this.ticketRepository = ticketRepository;
        this.validationRepository = validationRepository;
        this.mongoTemplate = mongoTemplate;
    }

    // ---------------------------------------------------------------- emission

    public IssueResult issueTickets(IssueTicketsRequest request) {
        List<Ticket> existing = ticketRepository.findByOrderId(request.getOrderId());
        if (!existing.isEmpty()) {
            if (isSameRequest(existing, request)) {
                return new IssueResult(existing, false);          // rejeu identique : 200
            }
            throw new ConflictException("Des billets ont deja ete emis pour la commande "
                    + request.getOrderId() + " avec un contenu different");
        }

        LocalDateTime now = LocalDateTime.now();
        Set<String> codesOfThisRequest = new HashSet<>();
        List<Ticket> toSave = new ArrayList<>();
        for (int i = 0; i < request.getQuantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setOrderId(request.getOrderId());
            ticket.setEventId(request.getEventId());
            ticket.setOwnerId(request.getOwnerId());
            ticket.setUniqueCode(generateUniqueCode(request.getEventId(), codesOfThisRequest));
            ticket.setStatus(TicketStatus.VALID);
            ticket.setIssuedAt(now);
            toSave.add(ticket);
        }
        return new IssueResult(ticketRepository.saveAll(toSave), true);   // 201
    }

    private boolean isSameRequest(List<Ticket> existing, IssueTicketsRequest request) {
        return existing.size() == request.getQuantity()
                && existing.stream().allMatch(t ->
                request.getEventId().equals(t.getEventId())
                        && request.getOwnerId().equals(t.getOwnerId()));
    }

    private String generateUniqueCode(Long eventId, Set<String> alreadyGenerated) {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            StringBuilder suffix = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                suffix.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String code = "EVT" + eventId + "-" + suffix;
            if (alreadyGenerated.add(code) && ticketRepository.findByUniqueCode(code).isEmpty()) {
                return code;
            }
        }
        throw new IllegalStateException("Impossible de generer un code unique");
    }

    // ------------------------------------------------------------ consultation

    public Ticket getById(String id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    public List<Ticket> search(Long ownerId, Long orderId) {
        if (ownerId == null && orderId == null) {
            throw new BadRequestException("Au moins un filtre est requis : ownerId ou orderId");
        }
        if (ownerId != null && orderId != null) {
            return ticketRepository.findByOwnerIdAndOrderId(ownerId, orderId);
        }
        return ownerId != null
                ? ticketRepository.findByOwnerId(ownerId)
                : ticketRepository.findByOrderId(orderId);
    }

    public List<TicketValidation> history(String ticketId) {
        getById(ticketId);                                          // 404 si le billet n'existe pas
        return validationRepository.findByTicketIdOrderByValidationDateAsc(ticketId);
    }

    // -------------------------------------------------------------- validation

    /** Valide un billet a l'entree : VALID -> USED, atomiquement. Toute tentative est enregistree. */
    public TicketValidation validate(String ticketId, Long controllerId) {
        Ticket current = getById(ticketId);                         // 404 si inconnu

        Ticket updated = changeStatusIfValid(ticketId, TicketStatus.USED);
        if (updated != null) {
            return recordAttempt(ticketId, controllerId, ValidationResult.ACCEPTED);
        }

        // La mise a jour conditionnelle n'a rien modifie : le billet n'etait plus VALID.
        Ticket latest = ticketRepository.findById(ticketId).orElse(current);
        if (latest.getStatus() == TicketStatus.CANCELLED) {
            recordAttempt(ticketId, controllerId, ValidationResult.REFUSED_CANCELLED);
            throw new ConflictException("Le billet a ete annule");
        }
        recordAttempt(ticketId, controllerId, ValidationResult.REFUSED_ALREADY_USED);
        throw new ConflictException("Le billet a deja ete utilise");
    }

    // ----------------------------------------------------------------- annulation

    /** Annule un billet : VALID -> CANCELLED. Un billet USED ne peut pas etre annule (409). */
    public Ticket cancel(String ticketId, TicketStatus requestedStatus) {
        if (requestedStatus != TicketStatus.CANCELLED) {
            throw new BadRequestException("Seul le statut CANCELLED est autorise");
        }
        Ticket current = getById(ticketId);                         // 404 si inconnu

        Ticket updated = changeStatusIfValid(ticketId, TicketStatus.CANCELLED);
        if (updated != null) {
            return updated;
        }

        Ticket latest = ticketRepository.findById(ticketId).orElse(current);
        if (latest.getStatus() == TicketStatus.CANCELLED) {
            return latest;                                          // deja annule : resultat identique (200)
        }
        throw new ConflictException("Le billet a deja ete utilise et ne peut plus etre annule");
    }

    // ----------------------------------------------------------------- outils

    /**
     * Mise a jour atomique : change le statut SEULEMENT si le billet est encore VALID.
     * Renvoie le billet modifie, ou null si aucun billet VALID ne correspondait.
     */
    private Ticket changeStatusIfValid(String ticketId, TicketStatus newStatus) {
        Query query = Query.query(Criteria.where("id").is(ticketId)
                .and("status").is(TicketStatus.VALID));
        Update update = new Update().set("status", newStatus);
        return mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Ticket.class);
    }

    private TicketValidation recordAttempt(String ticketId, Long controllerId, ValidationResult result) {
        TicketValidation validation = new TicketValidation();
        validation.setTicketId(ticketId);
        validation.setControllerId(controllerId);
        validation.setValidationDate(LocalDateTime.now());
        validation.setResult(result);
        return validationRepository.save(validation);
    }
}
