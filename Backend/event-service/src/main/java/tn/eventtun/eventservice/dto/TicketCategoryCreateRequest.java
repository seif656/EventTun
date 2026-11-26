package tn.eventtun.eventservice.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TicketCategoryCreateRequest(

    @NotBlank
    @Size(max = 255)
    String name,

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    BigDecimal price,

    @NotNull
    @Positive
    Integer capacity

) {
}
