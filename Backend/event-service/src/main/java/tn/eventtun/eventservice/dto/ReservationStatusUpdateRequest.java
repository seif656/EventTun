package tn.eventtun.eventservice.dto;

import jakarta.validation.constraints.NotNull;
import tn.eventtun.eventservice.model.ReservationStatus;

public record ReservationStatusUpdateRequest(

    @NotNull
    ReservationStatus status

) {
}
