package tn.eventtun.eventservice.dto;

import jakarta.validation.constraints.NotNull;
import tn.eventtun.eventservice.model.EventStatus;

public record EventStatusUpdateRequest(

    @NotNull
    EventStatus status

) {
}
