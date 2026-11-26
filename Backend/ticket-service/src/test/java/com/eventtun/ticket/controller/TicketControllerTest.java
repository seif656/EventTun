package com.eventtun.ticket.controller;

import com.eventtun.ticket.exception.ConflictException;
import com.eventtun.ticket.exception.TicketNotFoundException;
import com.eventtun.ticket.model.Ticket;
import com.eventtun.ticket.model.TicketStatus;
import com.eventtun.ticket.service.IssueResult;
import com.eventtun.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifie les codes HTTP et le format d'erreur (Problem Details). La couche metier est simulee. */
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    private static Ticket ticket() {
        Ticket t = new Ticket();
        t.setId("a");
        t.setOrderId(101L);
        t.setEventId(12L);
        t.setOwnerId(4L);
        t.setUniqueCode("EVT12-ABCD2345");
        t.setStatus(TicketStatus.VALID);
        return t;
    }

    private static final String ISSUE_BODY = "{\"orderId\":101,\"eventId\":12,\"ownerId\":4,\"quantity\":2}";

    @Test
    void post_tickets_first_emission_returns_201() throws Exception {
        when(ticketService.issueTickets(any())).thenReturn(new IssueResult(List.of(ticket()), true));

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(ISSUE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].uniqueCode").value("EVT12-ABCD2345"));
    }

    @Test
    void post_tickets_identical_replay_returns_200() throws Exception {
        when(ticketService.issueTickets(any())).thenReturn(new IssueResult(List.of(ticket()), false));

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(ISSUE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void post_tickets_with_invalid_quantity_returns_400_problem_json() throws Exception {
        String body = "{\"orderId\":101,\"eventId\":12,\"ownerId\":4,\"quantity\":0}";

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void post_tickets_with_different_content_returns_409() throws Exception {
        when(ticketService.issueTickets(any())).thenThrow(new ConflictException("contenu different"));

        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(ISSUE_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void get_unknown_ticket_returns_404_problem_json() throws Exception {
        when(ticketService.getById("zzz")).thenThrow(new TicketNotFoundException("zzz"));

        mockMvc.perform(get("/api/tickets/zzz"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void second_validation_returns_409() throws Exception {
        when(ticketService.validate(eq("a"), anyLong())).thenThrow(new ConflictException("deja utilise"));

        mockMvc.perform(post("/api/tickets/a/validations")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"controllerId\":7}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("deja utilise"));
    }
}
