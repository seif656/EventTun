package com.eventtun.ticket.exception;

/** Requete invalide detectee par la couche metier : traduit en 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
