package tn.eventtun.eventservice.dto;

import java.math.BigDecimal;

public record TicketCategoryDTO(

    Long id,
    Long eventId,
    String name,
    BigDecimal price,
    Integer capacity,
    Integer availableQuantity

) {
}
