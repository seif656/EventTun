package com.eventtun.ticket.dto;

import com.eventtun.ticket.model.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Corps de PATCH /api/tickets/{id}/status (annulation d'un billet). */
@Schema(description = "Changement d'etat d'un billet (CANCELLED)")
public class UpdateTicketStatusRequest {

    @NotNull
    @Schema(example = "CANCELLED")
    private TicketStatus status;

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
}
