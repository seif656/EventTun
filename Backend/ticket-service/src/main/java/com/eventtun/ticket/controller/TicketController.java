package com.eventtun.ticket.controller;

import com.eventtun.ticket.dto.IssueTicketsRequest;
import com.eventtun.ticket.dto.ProblemDetailResponse;
import com.eventtun.ticket.dto.UpdateTicketStatusRequest;
import com.eventtun.ticket.dto.ValidateTicketRequest;
import com.eventtun.ticket.model.Ticket;
import com.eventtun.ticket.model.TicketValidation;
import com.eventtun.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST du Ticket Service.
 *
 * VALIDATION 1 : les endpoints sont declares et documentes (Swagger/OpenAPI).
 * La logique metier est reportee : chaque methode renvoie 501 Not Implemented.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Emission, consultation et validation des billets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Emettre les billets d'une commande",
            description = "Appele par Order Service apres paiement et confirmation des places. "
                    + "Emet exactement `quantity` billets, chacun avec un uniqueCode unique. "
                    + "Idempotent par orderId : 200 avec les billets existants si la demande est identique, "
                    + "409 si le contenu differe.")
    @ApiResponse(responseCode = "201", description = "Billets emis",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Ticket.class))))
    @ApiResponse(responseCode = "200", description = "Rejeu identique : billets existants renvoyes",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Ticket.class))))
    @ApiResponse(responseCode = "400", description = "Requete invalide",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    @ApiResponse(responseCode = "409", description = "Billets deja emis avec un contenu different",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    public ResponseEntity<Void> issueTickets(@Valid @RequestBody IssueTicketsRequest request) {
        return notImplemented();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un billet")
    @ApiResponse(responseCode = "200", description = "Billet trouve",
            content = @Content(schema = @Schema(implementation = Ticket.class)))
    @ApiResponse(responseCode = "404", description = "Billet introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    public ResponseEntity<Void> getTicket(@PathVariable String id) {
        return notImplemented();
    }

    @GetMapping
    @Operation(summary = "Lister les billets d'un utilisateur ou d'une commande",
            description = "Filtres en query string : ownerId (billets d'un utilisateur) et/ou orderId (billets d'une commande).")
    @ApiResponse(responseCode = "200", description = "Liste (eventuellement vide)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = Ticket.class))))
    public ResponseEntity<Void> searchTickets(@RequestParam(required = false) Long ownerId,
                                              @RequestParam(required = false) Long orderId) {
        return notImplemented();
    }

    @PostMapping("/{id}/validations")
    @Operation(summary = "Valider un billet a l'entree",
            description = "Le controleur saisit/scanne le code du billet. Passage atomique VALID -> USED. "
                    + "Une seconde utilisation est refusee (409). Chaque tentative est enregistree.")
    @ApiResponse(responseCode = "201", description = "Validation acceptee",
            content = @Content(schema = @Schema(implementation = TicketValidation.class)))
    @ApiResponse(responseCode = "404", description = "Billet introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    @ApiResponse(responseCode = "409", description = "Billet deja utilise ou annule",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    public ResponseEntity<Void> validateTicket(@PathVariable String id,
                                               @Valid @RequestBody ValidateTicketRequest request) {
        return notImplemented();
    }

    @GetMapping("/{id}/validations")
    @Operation(summary = "Historique des validations d'un billet")
    @ApiResponse(responseCode = "200", description = "Historique des tentatives",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TicketValidation.class))))
    @ApiResponse(responseCode = "404", description = "Billet introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    public ResponseEntity<Void> getValidations(@PathVariable String id) {
        return notImplemented();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Annuler un billet",
            description = "Passe le billet a CANCELLED. Pour annuler les billets d'une commande, Order filtre d'abord "
                    + "avec GET /api/tickets?orderId=. Un billet USED ne peut pas etre annule (409).")
    @ApiResponse(responseCode = "200", description = "Billet annule",
            content = @Content(schema = @Schema(implementation = Ticket.class)))
    @ApiResponse(responseCode = "404", description = "Billet introuvable",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    @ApiResponse(responseCode = "409", description = "Billet deja utilise",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailResponse.class)))
    public ResponseEntity<Void> updateStatus(@PathVariable String id,
                                             @Valid @RequestBody UpdateTicketStatusRequest request) {
        return notImplemented();
    }

    private ResponseEntity<Void> notImplemented() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
