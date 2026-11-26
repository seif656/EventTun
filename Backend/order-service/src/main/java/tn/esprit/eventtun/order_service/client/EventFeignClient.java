
package tn.esprit.eventtun.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.ReservationRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.SeatReservationResponse;

@FeignClient(name = "event-service")
public interface EventFeignClient {

    // Réserver des places pour une catégorie
    @PostMapping("/api/categories/{categoryId}/reservations")
    SeatReservationResponse reserveSeats(
        @PathVariable("categoryId") Long categoryId,
        @RequestBody ReservationRequest request
    );

    // Confirmer une réservation
    @PatchMapping("/api/categories/{categoryId}/reservations/{orderId}/status")
    SeatReservationResponse confirmReservation(
        @PathVariable("categoryId") Long categoryId,
        @PathVariable("orderId") Long orderId,
        @RequestBody ExternalServiceDtos.ReservationStatusUpdateRequest request
    );

    // Libérer une réservation
    @DeleteMapping("/api/categories/{categoryId}/reservations/{orderId}")
    void releaseReservation(
        @PathVariable("categoryId") Long categoryId,
        @PathVariable("orderId") Long orderId
    );
}

