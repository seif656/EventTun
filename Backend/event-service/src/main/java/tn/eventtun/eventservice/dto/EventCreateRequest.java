package tn.eventtun.eventservice.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record EventCreateRequest(

    @NotNull
    @Positive
    Long clubId,

    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 3000)
    String description,

    @NotBlank
    @Size(max = 255)
    String location,

    @NotNull
    LocalDateTime startDate,

    @NotNull
    LocalDateTime endDate

) {
}
