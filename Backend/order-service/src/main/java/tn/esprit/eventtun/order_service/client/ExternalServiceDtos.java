
package tn.esprit.eventtun.order_service.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import tn.esprit.eventtun.order_service.enums.ReservationStatus;

public class ExternalServiceDtos {

    // Réponse du User Service (M1)
    public record UserInfo(
        Long userId,
        boolean exists,
        boolean blocked
    ) {}

    // Requête envoyée au Event Service (M2)
    public record ReservationRequest(
        Long orderId,
        Integer quantity
    ) {}

    // Réponse du Event Service (M2)
    public record SeatReservationResponse(
        Long id,
        Long orderId,
        Long categoryId,
        Integer quantity,
        ReservationStatus status,
        LocalDateTime createdAt,
        BigDecimal unitPrice
    ) {}

    // Mise à jour du statut de réservation
    public record ReservationStatusUpdateRequest(
        ReservationStatus status
    ) {}

    // Requête envoyée au Payment Service (M4)
    public record PaymentRequest(
        Long orderId,
        BigDecimal amount
    ) {}

    // Réponse du Payment Service (M4)
    public record PaymentResponse(
        Long paymentId,
        boolean successful
    ) {}

    // Requête envoyée au Ticket Service (M5)
    public record TicketRequest(
        Long orderId,
        Long eventId,
        Long ownerId,
        Integer quantity
    ) {}

    // Informations d'un billet retourné par M5
    public record TicketInfo(
        String id,
        Long orderId,
        Long eventId,
        Long ownerId,
        String uniqueCode,
        String status,
        LocalDateTime issuedAt
    ) {}

    // Réponse adaptée pour la Saga M3
    public record TicketResponse(
        boolean successful,
        List<String> ticketIds
    ) {}
}
