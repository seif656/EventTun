package tn.eventtun.eventservice.mapper;

import org.springframework.stereotype.Component;
import tn.eventtun.eventservice.dto.*;
import tn.eventtun.eventservice.model.*;

import java.math.BigDecimal;

@Component
public class EventMapper {

    // =====================================
    // EVENT
    // =====================================

    public Event toEvent(EventCreateRequest request) {

        Event event = new Event();

        event.setClubId(request.clubId());
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setStatus(EventStatus.DRAFT);

        return event;
    }

    public void updateEvent(
        Event event,
        EventUpdateRequest request) {

        event.setClubId(request.clubId());
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLocation(request.location());
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
    }

    public EventDTO toEventDTO(Event event) {

        return new EventDTO(
            event.getId(),
            event.getClubId(),
            event.getTitle(),
            event.getDescription(),
            event.getLocation(),
            event.getStartDate(),
            event.getEndDate(),
            event.getStatus()
        );
    }

    // =====================================
    // TICKET CATEGORY
    // =====================================

    public TicketCategory toTicketCategory(
        Long eventId,
        TicketCategoryCreateRequest request) {

        TicketCategory category = new TicketCategory();

        category.setEventId(eventId);
        category.setName(request.name());
        category.setPrice(request.price());
        category.setCapacity(request.capacity());
        category.setAvailableQuantity(request.capacity());

        return category;
    }

    public TicketCategoryDTO toTicketCategoryDTO(
        TicketCategory category) {

        return new TicketCategoryDTO(
            category.getId(),
            category.getEventId(),
            category.getName(),
            category.getPrice(),
            category.getCapacity(),
            category.getAvailableQuantity()
        );
    }

    // =====================================
    // SEAT RESERVATION
    // =====================================

    public SeatReservation toSeatReservation(
        Long categoryId,
        ReservationCreateRequest request) {

        SeatReservation reservation =
            new SeatReservation();

        reservation.setOrderId(request.orderId());
        reservation.setCategoryId(categoryId);
        reservation.setQuantity(request.quantity());
        reservation.setStatus(ReservationStatus.HELD);

        return reservation;
    }

    public SeatReservationDTO toSeatReservationDTO(
        SeatReservation reservation,
        BigDecimal unitPrice) {

        return new SeatReservationDTO(
            reservation.getId(),
            reservation.getOrderId(),
            reservation.getCategoryId(),
            reservation.getQuantity(),
            reservation.getStatus(),
            reservation.getCreatedAt(),
            unitPrice
        );
    }
}
