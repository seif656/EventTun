package com.eventtun.ticket.model;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/** Billet numerique (collection MongoDB "tickets"). */
@Document(collection = "tickets")
@Schema(description = "Billet numerique emis pour une commande")
public class Ticket {

    @Id
    @Schema(description = "Identifiant MongoDB", example = "665f1c2e9a1b2c3d4e5f6a7b")
    private String id;

    @Indexed
    @Schema(description = "Identifiant de la commande (Order Service)", example = "101")
    private Long orderId;

    @Schema(description = "Identifiant de l'evenement (Event Service)", example = "12")
    private Long eventId;

    @Indexed
    @Schema(description = "Identifiant du proprietaire (User Service)", example = "4")
    private Long ownerId;

    @Indexed(unique = true)
    @Schema(description = "Code unique saisi/scanne par le controleur", example = "EVT12-7F3K9Q2A")
    private String uniqueCode;

    @Schema(description = "Etat du billet", example = "VALID")
    private TicketStatus status;

    @Schema(description = "Date d'emission (ISO 8601)", example = "2026-11-14T22:30:00")
    private LocalDateTime issuedAt;

    public Ticket() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getUniqueCode() { return uniqueCode; }
    public void setUniqueCode(String uniqueCode) { this.uniqueCode = uniqueCode; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}
