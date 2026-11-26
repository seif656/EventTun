package tn.eventtun.eventservice.dto;

import jakarta.validation.constraints.*;

public record ReservationCreateRequest(

    @NotNull
    @Positive
    Long orderId,

    @NotNull
    @Positive
    Integer quantity

) {
}
