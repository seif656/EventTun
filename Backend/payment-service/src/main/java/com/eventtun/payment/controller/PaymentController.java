package com.eventtun.payment.controller;

import com.eventtun.payment.dto.PaymentRequest;
import com.eventtun.payment.dto.RefundRequest;
import com.eventtun.payment.entity.Payment;
import com.eventtun.payment.entity.PaymentStatus;
import com.eventtun.payment.entity.Refund;
import com.eventtun.payment.service.OperationResult;
import com.eventtun.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
        description = "Montant <= 500 : accepté ; au-delà : refusé. Idempotent par orderId : "
            + "une demande identique rejouée renvoie le résultat enregistré sans second débit.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Paiement accepté (SUCCESS) ; en-tête Location",
            content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "200", description = "Rejeu identique : paiement existant renvoyé",
            content = @Content(schema = @Schema(implementation = Payment.class))),
        @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content),
        @ApiResponse(responseCode = "409", description = "Paiement refusé (FAILED) ou même orderId avec un contenu différent",
            content = @Content)
    })
    public ResponseEntity<?> pay(@Valid @RequestBody PaymentRequest request) {
        OperationResult<Payment> result = paymentService.pay(request);
        Payment payment = result.value();

        // Paiement refusé : il est enregistré FAILED, mais on répond 409
        if (payment.getStatus() == PaymentStatus.FAILED) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Paiement refusé pour la commande " + payment.getOrderId());
            problem.setTitle("Paiement refusé");
            problem.setProperty("paymentId", payment.getId());
            problem.setProperty("paymentStatus", payment.getStatus());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }

        // Nouveau paiement : 201 + Location ; rejeu : 200
        if (result.created()) {
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(payment.getId()).toUri();
            return ResponseEntity.created(location).body(payment);
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un paiement")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paiement trouvé"),
        @ApiResponse(responseCode = "404", description = "Paiement introuvable", content = @Content)
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
        description = "Un seul remboursement par paiement réussi ; rejeu identique -> remboursement existant.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Remboursement créé, paiement passé à REFUNDED",
            content = @Content(schema = @Schema(implementation = Refund.class))),
        @ApiResponse(responseCode = "200", description = "Rejeu identique : remboursement existant renvoyé",
            content = @Content(schema = @Schema(implementation = Refund.class))),
        @ApiResponse(responseCode = "400", description = "Montant invalide ou supérieur au montant payé", content = @Content),
        @ApiResponse(responseCode = "404", description = "Paiement introuvable", content = @Content),
        @ApiResponse(responseCode = "409", description = "Paiement non remboursable (FAILED) ou déjà remboursé autrement",
            content = @Content)
    })
    public ResponseEntity<Refund> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        OperationResult<Refund> result = paymentService.refund(id, request);
        Refund refund = result.value();
        if (result.created()) {
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{refundId}").buildAndExpand(refund.getId()).toUri();
            return ResponseEntity.created(location).body(refund);
        }
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/{id}/refunds")
    @Operation(summary = "Consulter les remboursements d'un paiement")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des remboursements"),
        @ApiResponse(responseCode = "404", description = "Paiement introuvable", content = @Content)
    })
    public List<Refund> getRefunds(@PathVariable Long id) {
        return paymentService.getRefunds(id);
    }
}
