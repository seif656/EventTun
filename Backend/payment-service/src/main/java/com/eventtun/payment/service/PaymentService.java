package com.eventtun.payment.service;

import com.eventtun.payment.dto.PaymentRequest;
import com.eventtun.payment.dto.RefundRequest;
import com.eventtun.payment.entity.Payment;
import com.eventtun.payment.entity.PaymentStatus;
import com.eventtun.payment.entity.Refund;
import com.eventtun.payment.entity.RefundStatus;
import com.eventtun.payment.repository.PaymentRepository;
import com.eventtun.payment.repository.RefundRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Logique métier du Payment Service.
 */
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentSimulator simulator;

    public PaymentService(PaymentRepository paymentRepository,
                          RefundRepository refundRepository,
                          PaymentSimulator simulator) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.simulator = simulator;
    }

    /**
     * Paiement simulé, idempotent par orderId.
     * - première demande : paiement créé (SUCCESS ou FAILED), created = true
     * - demande identique rejouée : paiement existant, created = false, aucun nouveau débit
     * - même orderId avec un contenu différent : 409
     * Le paiement FAILED est enregistré : c'est le controller qui répond 409.
     */
    public OperationResult<Payment> pay(PaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            return replay(existing.get(), request);
        }

        Payment payment = new Payment();
        payment.setOrderId(request.orderId());
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setStatus(simulator.isAccepted(request.amount()) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        try {
            return new OperationResult<>(paymentRepository.saveAndFlush(payment), true);
        } catch (DataIntegrityViolationException e) {
            // Deux demandes simultanées pour la même commande : la contrainte unique a bloqué la seconde
            Payment saved = paymentRepository.findByOrderId(request.orderId()).orElseThrow(() -> e);
            return replay(saved, request);
        }
    }

    /** Demande rejouée : même contenu -> résultat existant ; contenu différent -> 409. */
    private OperationResult<Payment> replay(Payment existing, PaymentRequest request) {
        boolean sameRequest = existing.getAmount().compareTo(request.amount()) == 0
            && existing.getMethod() == request.method();
        if (!sameRequest) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Un paiement existe déjà pour la commande " + request.orderId() + " avec un contenu différent");
        }
        return new OperationResult<>(existing, false);
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
     * Remboursement total d'un paiement réussi, idempotent.
     * - paiement inexistant : 404
     * - remboursement déjà fait, même montant : remboursement existant, created = false
     * - remboursement déjà fait, autre montant : 409
     * - paiement non SUCCESS (FAILED) : 409
     * - montant supérieur au montant payé : 400
     */
    @Transactional
    public OperationResult<Refund> refund(Long paymentId, RefundRequest request) {
        Payment payment = getPayment(paymentId);

        List<Refund> existing = refundRepository.findByPaymentId(paymentId);
        if (!existing.isEmpty()) {
            Refund previous = existing.get(0);
            if (previous.getAmount().compareTo(request.amount()) == 0) {
                return new OperationResult<>(previous, false);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Le paiement " + paymentId + " a déjà été remboursé");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Paiement " + paymentId + " non remboursable (statut " + payment.getStatus() + ")");
        }
        if (request.amount().compareTo(payment.getAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le montant remboursé dépasse le montant payé (" + payment.getAmount() + ")");
        }

        Refund refund = new Refund();
        refund.setPaymentId(paymentId);
        refund.setAmount(request.amount());
        refund.setReason(request.reason());
        refund.setStatus(RefundStatus.COMPLETED);
        Refund saved = refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        return new OperationResult<>(saved, true);
    }

    public List<Refund> getRefunds(Long paymentId) {
        getPayment(paymentId); // 404 si le paiement n'existe pas
        return refundRepository.findByPaymentId(paymentId);
    }
}
