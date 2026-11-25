
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
        Long userId,
        Long eventId
    ) {}

    // Réponse du Ticket Service (M5)
    public record TicketResponse(
        boolean successful,
        List<Long> ticketIds
    ) {}
}
