package tn.eventtun.eventservice.dto;

import tn.eventtun.eventservice.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeatReservationDTO(

    Long id,
    Long orderId,
    Long categoryId,
    Integer quantity,
    ReservationStatus status,
    LocalDateTime createdAt,
    BigDecimal unitPrice

) {
}
