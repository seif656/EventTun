package com.eventtun.payment.service;

import com.eventtun.payment.dto.PaymentRequest;
import com.eventtun.payment.dto.RefundRequest;
import com.eventtun.payment.entity.Payment;
import com.eventtun.payment.entity.Refund;
import com.eventtun.payment.repository.PaymentRepository;
import com.eventtun.payment.repository.RefundRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Logique métier du Payment Service.
 * Validation 1 : les lectures sont branchées sur les repositories ;
 * la simulation de paiement et le remboursement seront implémentés aux validations suivantes.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public PaymentService(PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    /**
     * Règles prévues (non implémentées en validation 1) :
     * - orderId unique : une seule tentative par commande ;
     * - rejeu identique -> 200 avec le résultat enregistré (SUCCESS) ou 409 (FAILED), sans second débit ;
     * - même orderId avec un montant différent -> 409 ;
     * - paiement refusé -> enregistré FAILED, réponse 409.
     */
    public Payment pay(PaymentRequest request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Simulation de paiement prévue pour une validation ultérieure");
    }

    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Paiement " + id + " introuvable"));
    }

    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    /**
     * Règles prévues (non implémentées en validation 1) :
     * - remboursement uniquement d'un paiement SUCCESS (sinon 409) ;
     * - montant <= montant payé (sinon 400) ;
     * - un seul remboursement total par paiement ; rejeu identique -> 200 avec l'existant.
     */
    public Refund refund(Long paymentId, RefundRequest request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "Remboursement prévu pour une validation ultérieure");
    }

    public List<Refund> getRefunds(Long paymentId) {
        getPayment(paymentId); // 404 si le paiement n'existe pas
        return refundRepository.findByPaymentId(paymentId);
    }
}
