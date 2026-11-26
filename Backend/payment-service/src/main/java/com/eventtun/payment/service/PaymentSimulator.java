package com.eventtun.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Simule la réponse d'une banque (aucun paiement réel).
 * Règle : montant <= plafond -> accepté ; au-delà -> refusé.
 */
@Component
public class PaymentSimulator {

    private final BigDecimal maxAmount;

    public PaymentSimulator(@Value("${payment.simulation.max-amount:500}") BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public boolean isAccepted(BigDecimal amount) {
        return amount.compareTo(maxAmount) <= 0;
    }
}
