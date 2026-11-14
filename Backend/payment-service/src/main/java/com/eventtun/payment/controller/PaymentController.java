package com.eventtun.payment.controller;

import com.eventtun.payment.dto.PaymentRequest;
import com.eventtun.payment.dto.RefundRequest;
import com.eventtun.payment.entity.Payment;
import com.eventtun.payment.entity.Refund;
import com.eventtun.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Paiements simulés et remboursements (appelé uniquement par Order Service)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Effectuer un paiement simulé",
            description = "Idempotent par orderId : une demande identique rejouée renvoie le résultat enregistré sans second débit.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paiement accepté (SUCCESS) ; en-tête Location"),
            @ApiResponse(responseCode = "200", description = "Rejeu identique : paiement existant renvoyé"),
            @ApiResponse(responseCode = "400", description = "Requête invalide"),
            @ApiResponse(responseCode = "409", description = "Paiement refusé (FAILED) ou même orderId avec un contenu différent")
    })
    public ResponseEntity<Payment> pay(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.pay(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(payment.getId()).toUri();
        return ResponseEntity.created(location).body(payment);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un paiement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paiement trouvé"),
            @ApiResponse(responseCode = "404", description = "Paiement introuvable")
    })
    public Payment getPayment(@PathVariable Long id) {
        return paymentService.getPayment(id);
    }

    @GetMapping
    @Operation(summary = "Paiement d'une commande",
            description = "Utilisé par Order après un timeout, avant toute nouvelle tentative ou compensation. Liste vide si aucun paiement.")
    @ApiResponse(responseCode = "200", description = "Liste (0 ou 1 paiement)")
    public List<Payment> getByOrder(@Parameter(description = "Identifiant de la commande") @RequestParam Long orderId) {
        return paymentService.findByOrderId(orderId).map(List::of).orElse(List.of());
    }

    @PostMapping("/{id}/refunds")
    @Operation(summary = "Demander un remboursement",
            description = "Un seul remboursement total par paiement réussi ; rejeu identique -> remboursement existant.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Remboursement créé"),
            @ApiResponse(responseCode = "200", description = "Rejeu identique : remboursement existant renvoyé"),
            @ApiResponse(responseCode = "400", description = "Montant invalide ou supérieur au montant payé"),
            @ApiResponse(responseCode = "404", description = "Paiement introuvable"),
            @ApiResponse(responseCode = "409", description = "Paiement non remboursable (FAILED ou déjà remboursé autrement)")
    })
    public ResponseEntity<Refund> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        Refund refund = paymentService.refund(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{refundId}").buildAndExpand(refund.getId()).toUri();
        return ResponseEntity.created(location).body(refund);
    }

    @GetMapping("/{id}/refunds")
    @Operation(summary = "Consulter les remboursements d'un paiement")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des remboursements"),
            @ApiResponse(responseCode = "404", description = "Paiement introuvable")
    })
    public List<Refund> getRefunds(@PathVariable Long id) {
        return paymentService.getRefunds(id);
    }
}
