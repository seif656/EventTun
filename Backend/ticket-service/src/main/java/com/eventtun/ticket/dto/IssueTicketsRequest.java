package com.eventtun.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Corps de POST /api/tickets : emission des billets d'une commande payee. */
@Schema(description = "Demande d'emission des billets d'une commande")
public class IssueTicketsRequest {

    @NotNull
    @Schema(example = "101")
    private Long orderId;

    @NotNull
    @Schema(example = "12")
    private Long eventId;

    @NotNull
    @Schema(example = "4")
    private Long ownerId;

    @NotNull
    @Positive
    @Schema(description = "Nombre de billets payes = nombre de billets emis", example = "2")
    private Integer quantity;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
