
package tn.esprit.eventtun.order_service.client;

import org.springframework.stereotype.Component;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.ReservationRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.SeatReservationResponse;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.ReservationStatusUpdateRequest;

@Component
public class EventClientAdapter implements EventClient {

    private final EventFeignClient eventFeignClient;

    public EventClientAdapter(EventFeignClient eventFeignClient) {
        this.eventFeignClient = eventFeignClient;
    }

    @Override
    public SeatReservationResponse reserveSeats(
        Long categoryId,
        ReservationRequest request) {

        return eventFeignClient.reserveSeats(categoryId, request);
    }

    @Override
    public SeatReservationResponse confirmReservation(
        Long categoryId,
        Long orderId,
        ReservationStatusUpdateRequest request) {

        return eventFeignClient.confirmReservation(
            categoryId,
            orderId,
            request
        );
    }

    @Override
    public void releaseReservation(
        Long categoryId,
        Long orderId) {

        eventFeignClient.releaseReservation(categoryId, orderId);
    }
}
