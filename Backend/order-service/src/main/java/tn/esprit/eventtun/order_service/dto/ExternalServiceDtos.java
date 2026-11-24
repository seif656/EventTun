
package tn.esprit.eventtun.order_service.client;

import java.math.BigDecimal;
import java.util.List;

public class ExternalServiceDtos {

    // Réponse du User Service (M1)
    public record UserInfo(
            Long userId,
            boolean exists,
            boolean blocked
    ) {}

    // Informations et réservation auprès du Event Service (M2)
    public record ReservationRequest(
            Long eventId,
            Long categoryId,
            Integer quantity
    ) {}

    public record ReservationItem(
            Long categoryId,
            Integer quantity,
            BigDecimal unitPrice
    ) {}

    public record ReservationResponse(
            Long reservationId,
            boolean successful,
            List<ReservationItem> items
    ) {}

    // Réponse du Payment Service (M4)
    public record PaymentRequest(
            Long orderId,
            BigDecimal amount
    ) {}

    public record PaymentResponse(
            Long paymentId,
            boolean successful
    ) {}

    // Réponse du Ticket Service (M5)
    public record TicketRequest(
            Long orderId,
            Long userId,
            Long eventId
    ) {}

    public record TicketResponse(
            boolean successful,
            List<Long> ticketIds
    ) {}
}

