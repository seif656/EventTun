package com.eventtun.ticket.service;

import com.eventtun.ticket.repository.TicketRepository;
import com.eventtun.ticket.repository.TicketValidationRepository;
import org.springframework.stereotype.Service;

/**
 * Couche metier du Ticket Service.
 *
 * VALIDATION 1 : structure uniquement, la logique metier est reportee.
 *
 * Responsabilites prevues (a implementer aux validations suivantes) :
 *  - issueTickets      : emettre exactement `quantity` billets avec un uniqueCode unique ;
 *                        idempotent par orderId (200 si rejeu identique, 409 si contenu different)
 *  - getById / search  : consulter un billet, filtrer par ownerId / orderId
 *  - validate          : passage atomique VALID -> USED (mise a jour conditionnelle) ;
 *                        trace chaque tentative dans TicketValidation ; 409 si deja USED
 *  - history           : historique des tentatives de validation d'un billet
 *  - cancel            : VALID -> CANCELLED ; 409 si le billet est USED
 */
@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketValidationRepository validationRepository;

    public TicketService(TicketRepository ticketRepository,
                         TicketValidationRepository validationRepository) {
        this.ticketRepository = ticketRepository;
        this.validationRepository = validationRepository;
    }

    // TODO (validations suivantes) : implementer les methodes listees ci-dessus.
}
