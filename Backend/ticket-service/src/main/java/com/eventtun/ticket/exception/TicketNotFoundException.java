package com.eventtun.ticket.exception;

/** Billet introuvable : traduit en 404 par GlobalExceptionHandler. */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String ticketId) {
        super("Billet introuvable : " + ticketId);
    }
}
