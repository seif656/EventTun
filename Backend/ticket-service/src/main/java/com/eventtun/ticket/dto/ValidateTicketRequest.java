package com.eventtun.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Corps de POST /api/tickets/{id}/validations. */
@Schema(description = "Demande de validation d'un billet a l'entree")
public class ValidateTicketRequest {

    @NotNull
    @Schema(description = "Identifiant du controleur", example = "7")
    private Long controllerId;

    public Long getControllerId() { return controllerId; }
    public void setControllerId(Long controllerId) { this.controllerId = controllerId; }
}
