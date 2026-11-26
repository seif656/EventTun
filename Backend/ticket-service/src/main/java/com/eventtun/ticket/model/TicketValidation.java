package com.eventtun.ticket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Tentative de validation d'un billet a l'entree (collection "ticket_validations"). */
@Document(collection = "ticket_validations")
@Schema(description = "Tentative de validation d'un billet par un controleur")
public class TicketValidation {

    @Id
    @Schema(description = "Identifiant MongoDB", example = "665f1d3b9a1b2c3d4e5f6a80")
    private String id;

    @Indexed
    @Schema(description = "Reference au document Ticket", example = "665f1c2e9a1b2c3d4e5f6a7b")
    private String ticketId;

    @Schema(description = "Identifiant du controleur (User Service)", example = "7")
    private Long controllerId;

    @Schema(description = "Date de la tentative (ISO 8601)", example = "2026-12-01T18:45:00")
    private LocalDateTime validationDate;

    @Schema(description = "Resultat de la tentative", example = "ACCEPTED")
    private ValidationResult result;

    public TicketValidation() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public Long getControllerId() { return controllerId; }
    public void setControllerId(Long controllerId) { this.controllerId = controllerId; }

    public LocalDateTime getValidationDate() { return validationDate; }
    public void setValidationDate(LocalDateTime validationDate) { this.validationDate = validationDate; }

    public ValidationResult getResult() { return result; }
    public void setResult(ValidationResult result) { this.result = result; }
}
