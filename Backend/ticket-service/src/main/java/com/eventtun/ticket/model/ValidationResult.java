package com.eventtun.ticket.model;

/** Resultat d'une tentative de validation a l'entree. */
public enum ValidationResult {
    ACCEPTED,
    REFUSED_ALREADY_USED,
    REFUSED_CANCELLED
}
