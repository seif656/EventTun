package com.eventtun.ticket.exception;

/** Conflit avec l'etat actuel (billet deja utilise, emission differente...) : traduit en 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
