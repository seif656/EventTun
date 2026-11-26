package tn.esprit.eventtun.order_service.client;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.ReservationRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.SeatReservationResponse;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.ReservationStatusUpdateRequest;

public interface EventClient {

    SeatReservationResponse reserveSeats(
        Long categoryId,
        ReservationRequest request
    );

    SeatReservationResponse confirmReservation(
        Long categoryId,
        Long orderId,
        ReservationStatusUpdateRequest request
    );

    void releaseReservation(
        Long categoryId,
        Long orderId
    );
}
