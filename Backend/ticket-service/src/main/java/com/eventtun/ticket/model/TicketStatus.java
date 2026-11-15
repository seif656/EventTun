package com.eventtun.ticket.model;

/** Etat d'un billet. VALID -> USED (une seule fois) ; VALID -> CANCELLED. */
public enum TicketStatus {
    VALID,
    USED,
    CANCELLED
}
