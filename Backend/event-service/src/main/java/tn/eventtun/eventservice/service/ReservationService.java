package tn.eventtun.eventservice.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import tn.eventtun.eventservice.dto.*;
import tn.eventtun.eventservice.exception.*;
import tn.eventtun.eventservice.mapper.EventMapper;
import tn.eventtun.eventservice.model.*;
import tn.eventtun.eventservice.repository.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final EventRepository eventRepository;
    private final TicketCategoryRepository categoryRepository;
    private final SeatReservationRepository reservationRepository;
    private final EventMapper mapper;

    public ReservationService(
        EventRepository eventRepository,
        TicketCategoryRepository categoryRepository,
        SeatReservationRepository reservationRepository,
        EventMapper mapper) {

        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.reservationRepository = reservationRepository;
        this.mapper = mapper;
    }

    public record ReservationResult(
        SeatReservationDTO reservation,
        boolean created
    ) {
    }

    // =====================================
    // 1. RESERVE SEATS
    // =====================================

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResult reserveSeats(
        Long categoryId,
        ReservationCreateRequest request) {

        // Validate request
        if (request.orderId() == null
            || request.orderId() <= 0
            || request.quantity() == null
            || request.quantity() <= 0) {

            throw new BusinessException(
                "Order ID and quantity must be positive"
            );
        }

        // Lock event before category
        Event event = findEventForCategoryUpdate(categoryId);

        TicketCategory category =
            findCategoryForUpdate(categoryId);

        // Check idempotence
        var existingReservation = reservationRepository
            .findByOrderIdAndCategoryId(
                request.orderId(),
                categoryId
            );

        if (existingReservation.isPresent()) {

            SeatReservation reservation = existingReservation.get();

            if (!reservation.getQuantity().equals(request.quantity())) {

                throw new ConflictException(
                    "Reservation already exists with a different quantity"
                );
            }

            return new ReservationResult(
                mapper.toSeatReservationDTO(
                    reservation,
                    category.getPrice()
                ),
                false
            );
        }

        // Only published events accept new reservations
        ensureEventPublished(event);

        // Check available stock
        if (category.getAvailableQuantity() < request.quantity()) {

            throw new ConflictException(
                "Insufficient available quantity"
            );
        }

        // Decrease available stock
        category.setAvailableQuantity(
            category.getAvailableQuantity() - request.quantity()
        );

        // Create reservation with HELD status
        SeatReservation reservation =
            mapper.toSeatReservation(categoryId, request);

        SeatReservation savedReservation =
            reservationRepository.save(reservation);

        return new ReservationResult(
            mapper.toSeatReservationDTO(
                savedReservation,
                category.getPrice()
            ),
            true
        );
    }

    // =====================================
    // 2. RELEASE RESERVATION
    // =====================================

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void releaseReservation(
        Long categoryId,
        Long orderId) {

        TicketCategory category =
            findCategoryForUpdate(categoryId);

        SeatReservation reservation =
            findReservation(categoryId, orderId);

        // Already released: do nothing
        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            return;
        }

        // Check stock consistency before restoring
        long restoredQuantity =
            (long) category.getAvailableQuantity()
                + reservation.getQuantity();

        if (restoredQuantity > category.getCapacity()) {

            throw new ConflictException(
                "Available quantity cannot exceed category capacity"
            );
        }

        // Restore stock
        category.setAvailableQuantity((int) restoredQuantity);

        reservation.setStatus(ReservationStatus.RELEASED);
    }

    // =====================================
    // 3. CONFIRM RESERVATION
    // =====================================

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SeatReservationDTO confirmReservation(
        Long categoryId,
        Long orderId,
        ReservationStatusUpdateRequest request) {

        if (request.status() != ReservationStatus.CONFIRMED) {

            throw new BusinessException(
                "Only CONFIRMED status is allowed"
            );
        }

        // Lock event before category
        Event event = findEventForCategoryUpdate(categoryId);

        TicketCategory category =
            findCategoryForUpdate(categoryId);

        SeatReservation reservation =
            findReservation(categoryId, orderId);

        // Released reservation cannot be confirmed
        if (reservation.getStatus() == ReservationStatus.RELEASED) {

            throw new ConflictException(
                "A released reservation cannot be confirmed"
            );
        }

        // Already confirmed: return existing reservation
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {

            return mapper.toSeatReservationDTO(
                reservation,
                category.getPrice()
            );
        }

        // Prevent confirmation after event cancellation
        ensureEventPublished(event);

        reservation.setStatus(ReservationStatus.CONFIRMED);

        return mapper.toSeatReservationDTO(
            reservation,
            category.getPrice()
        );
    }

    // =====================================
    // 4. GET EVENT RESERVATIONS
    // =====================================

    public List<SeatReservationDTO> getEventReservations(Long eventId) {

        if (!eventRepository.existsById(eventId)) {

            throw new ResourceNotFoundException(
                "Event not found with id: " + eventId
            );
        }

        List<TicketCategory> categories =
            categoryRepository.findByEventId(eventId);

        if (categories.isEmpty()) {
            return List.of();
        }

        Map<Long, TicketCategory> categoriesById =
            categories.stream()
                .collect(Collectors.toMap(
                    TicketCategory::getId,
                    Function.identity()
                ));

        List<Long> categoryIds = categories.stream()
            .map(TicketCategory::getId)
            .toList();

        return reservationRepository
            .findByCategoryIdIn(categoryIds)
            .stream()
            .map(reservation -> {

                BigDecimal unitPrice = categoriesById
                    .get(reservation.getCategoryId())
                    .getPrice();

                return mapper.toSeatReservationDTO(
                    reservation,
                    unitPrice
                );
            })
            .toList();
    }

    // =====================================
    // PRIVATE UTILITY METHODS
    // =====================================

    private Event findEventForCategoryUpdate(Long categoryId) {

        TicketCategory category = categoryRepository
            .findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + categoryId
            ));

        Long eventId = category.getEventId();

        return eventRepository.findByIdForUpdate(eventId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Event not found with id: " + eventId
            ));
    }

    private void ensureEventPublished(Event event) {

        if (event.getStatus() != EventStatus.PUBLISHED) {

            throw new ConflictException(
                "Only published events accept reservations"
            );
        }
    }

    private TicketCategory findCategoryForUpdate(Long categoryId) {

        return categoryRepository.findByIdForUpdate(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + categoryId
            ));
    }

    private SeatReservation findReservation(
        Long categoryId,
        Long orderId) {

        return reservationRepository
            .findByOrderIdAndCategoryId(orderId, categoryId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Reservation not found for order: "
                    + orderId
                    + " and category: "
                    + categoryId
            ));
    }
}
