
package tn.esprit.eventtun.order_service.client;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.ReservationRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.ReservationResponse;

import java.util.List;

public interface EventClient {

    ReservationResponse reserveSeats(
            List<ReservationRequest> items
    );

    boolean confirmReservation(Long reservationId);

    boolean releaseReservation(Long reservationId);
}
