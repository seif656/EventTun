package com.eventtun.payment.entity;

/** SUCCESS : paiement accepté ; FAILED : paiement refusé (enregistré) ; REFUNDED : remboursé. */
public enum PaymentStatus {
    SUCCESS,
    FAILED,
    REFUNDED
}
