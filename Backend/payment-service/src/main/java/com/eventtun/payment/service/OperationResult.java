package com.eventtun.payment.service;

/**
 * Résultat d'une opération idempotente.
 * created = true  : l'objet vient d'être créé (201)
 * created = false : demande rejouée, on renvoie l'existant (200)
 */
public record OperationResult<T>(T value, boolean created) {
}
