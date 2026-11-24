package tn.eventtun.eventservice.dto;

import tn.eventtun.eventservice.model.EventStatus;
import java.time.LocalDateTime;

public record EventDTO(

    Long id,
    Long clubId,
    String title,
    String description,
    String location,
    LocalDateTime startDate,
    LocalDateTime endDate,
    EventStatus status

) {
}
