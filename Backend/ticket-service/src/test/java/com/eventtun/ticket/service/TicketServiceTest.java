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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitaires de la logique metier : aucune base MongoDB n'est necessaire (tout est simule). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private TicketValidationRepository validationRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @InjectMocks
    private TicketService service;

    // ------------------------------------------------------------ helpers

    private static Ticket ticket(String id, TicketStatus status) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setOrderId(101L);
        t.setEventId(12L);
        t.setOwnerId(4L);
        t.setUniqueCode("EVT12-" + id);
        t.setStatus(status);
        return t;
    }

    private static IssueTicketsRequest issueRequest(int quantity) {
        IssueTicketsRequest r = new IssueTicketsRequest();
        r.setOrderId(101L);
        r.setEventId(12L);
        r.setOwnerId(4L);
        r.setQuantity(quantity);
        return r;
    }

    /** Simule la mise a jour atomique : renvoie le billet (succes) ou null (billet plus VALID). */
    private void atomicUpdateReturns(Ticket result) {
        when(mongoTemplate.findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Ticket.class))).thenReturn(result);
    }

    private void saveValidationsAsIs() {
        when(validationRepository.save(any(TicketValidation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------ emission

    @Test
    void issueTickets_creates_exactly_quantity_tickets_with_distinct_codes() {
        when(ticketRepository.findByOrderId(101L)).thenReturn(List.of());
        when(ticketRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IssueResult result = service.issueTickets(issueRequest(3));

        assertTrue(result.created());
        assertEquals(3, result.tickets().size());
        assertEquals(3, new HashSet<>(result.tickets().stream().map(Ticket::getUniqueCode).toList()).size());
        assertTrue(result.tickets().stream().allMatch(t -> t.getStatus() == TicketStatus.VALID));
        assertTrue(result.tickets().stream().allMatch(t -> t.getOrderId().equals(101L)));
    }

    @Test
    void issueTickets_identical_replay_returns_existing_tickets_without_creating_new_ones() {
        List<Ticket> existing = List.of(ticket("a", TicketStatus.VALID), ticket("b", TicketStatus.VALID));
        when(ticketRepository.findByOrderId(101L)).thenReturn(existing);

        IssueResult result = service.issueTickets(issueRequest(2));

        assertFalse(result.created());
        assertEquals(existing, result.tickets());
        verify(ticketRepository, never()).saveAll(any());
    }

    @Test
    void issueTickets_different_quantity_for_same_order_is_a_conflict() {
        when(ticketRepository.findByOrderId(101L))
                .thenReturn(List.of(ticket("a", TicketStatus.VALID), ticket("b", TicketStatus.VALID)));

        assertThrows(ConflictException.class, () -> service.issueTickets(issueRequest(3)));
        verify(ticketRepository, never()).saveAll(any());
    }

    // ------------------------------------------------------------ consultation

    @Test
    void getById_unknown_ticket_throws_not_found() {
        when(ticketRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.getById("zzz"));
    }

    @Test
    void search_without_any_filter_is_a_bad_request() {
        assertThrows(BadRequestException.class, () -> service.search(null, null));
    }

    @Test
    void search_by_order_uses_the_order_filter() {
        List<Ticket> tickets = List.of(ticket("a", TicketStatus.VALID));
        when(ticketRepository.findByOrderId(101L)).thenReturn(tickets);

        assertEquals(tickets, service.search(null, 101L));
    }

    @Test
    void history_of_unknown_ticket_throws_not_found() {
        when(ticketRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.history("zzz"));
    }

    // ------------------------------------------------------------ validation

    @Test
    void validate_first_scan_is_accepted_and_recorded() {
        Ticket valid = ticket("a", TicketStatus.VALID);
        when(ticketRepository.findById("a")).thenReturn(Optional.of(valid));
        atomicUpdateReturns(ticket("a", TicketStatus.USED));
        saveValidationsAsIs();

        TicketValidation validation = service.validate("a", 7L);

        assertEquals(ValidationResult.ACCEPTED, validation.getResult());
        assertEquals("a", validation.getTicketId());
        assertEquals(7L, validation.getControllerId());
    }

    @Test
    void validate_second_scan_is_refused_with_conflict_and_the_attempt_is_recorded() {
        when(ticketRepository.findById("a")).thenReturn(Optional.of(ticket("a", TicketStatus.USED)));
        atomicUpdateReturns(null);                       // le billet n'est plus VALID
        saveValidationsAsIs();

        assertThrows(ConflictException.class, () -> service.validate("a", 7L));

        ArgumentCaptor<TicketValidation> captor = ArgumentCaptor.forClass(TicketValidation.class);
        verify(validationRepository).save(captor.capture());
        assertEquals(ValidationResult.REFUSED_ALREADY_USED, captor.getValue().getResult());
    }

    @Test
    void validate_cancelled_ticket_is_refused_with_conflict() {
        when(ticketRepository.findById("a")).thenReturn(Optional.of(ticket("a", TicketStatus.CANCELLED)));
        atomicUpdateReturns(null);
        saveValidationsAsIs();

        assertThrows(ConflictException.class, () -> service.validate("a", 7L));

        ArgumentCaptor<TicketValidation> captor = ArgumentCaptor.forClass(TicketValidation.class);
        verify(validationRepository).save(captor.capture());
        assertEquals(ValidationResult.REFUSED_CANCELLED, captor.getValue().getResult());
    }

    @Test
    void validate_unknown_ticket_throws_not_found_and_changes_nothing() {
        when(ticketRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.validate("zzz", 7L));
        verify(mongoTemplate, never()).findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Ticket.class));
        verify(validationRepository, never()).save(any());
    }

    // ------------------------------------------------------------ annulation

    @Test
    void cancel_valid_ticket_returns_the_cancelled_ticket() {
        when(ticketRepository.findById("a")).thenReturn(Optional.of(ticket("a", TicketStatus.VALID)));
        atomicUpdateReturns(ticket("a", TicketStatus.CANCELLED));

        assertEquals(TicketStatus.CANCELLED, service.cancel("a", TicketStatus.CANCELLED).getStatus());
    }

    @Test
    void cancel_used_ticket_is_a_conflict() {
        when(ticketRepository.findById("a")).thenReturn(Optional.of(ticket("a", TicketStatus.USED)));
        atomicUpdateReturns(null);

        assertThrows(ConflictException.class, () -> service.cancel("a", TicketStatus.CANCELLED));
    }

    @Test
    void cancel_already_cancelled_ticket_is_idempotent() {
        when(ticketRepository.findById("a")).thenReturn(Optional.of(ticket("a", TicketStatus.CANCELLED)));
        atomicUpdateReturns(null);

        assertEquals(TicketStatus.CANCELLED, service.cancel("a", TicketStatus.CANCELLED).getStatus());
    }

    @Test
    void cancel_with_another_status_than_cancelled_is_a_bad_request() {
        assertThrows(BadRequestException.class, () -> service.cancel("a", TicketStatus.USED));
    }
}
