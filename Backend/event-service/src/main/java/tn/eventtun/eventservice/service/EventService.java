package tn.eventtun.eventservice.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.eventtun.eventservice.dto.*;
import tn.eventtun.eventservice.exception.*;
import tn.eventtun.eventservice.mapper.EventMapper;
import tn.eventtun.eventservice.model.*;
import tn.eventtun.eventservice.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final TicketCategoryRepository categoryRepository;
    private final EventMapper mapper;

    public EventService(
        EventRepository eventRepository,
        TicketCategoryRepository categoryRepository,
        EventMapper mapper) {

        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.mapper = mapper;
    }

    // =====================================
    // 1. CREATE EVENT
    // =====================================

    @Transactional
    public EventDTO createEvent(EventCreateRequest request) {

        validateDates(
            request.startDate(),
            request.endDate()
        );

        Event event = mapper.toEvent(request);

        Event savedEvent = eventRepository.save(event);

        return mapper.toEventDTO(savedEvent);
    }

    // =====================================
    // 2. GET EVENTS WITH FILTERS
    // =====================================

    public List<EventDTO> getEvents(
        EventStatus status,
        LocalDateTime from,
        String q) {

        Specification<Event> specification =
            (root, query, cb) -> cb.conjunction();

        if (status != null) {

            specification = specification.and(
                (root, query, cb) ->
                    cb.equal(root.get("status"), status)
            );
        }

        if (from != null) {

            specification = specification.and(
                (root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("startDate"),
                        from
                    )
            );
        }

        if (q != null && !q.isBlank()) {

            String keyword = q.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

            String pattern = "%" + keyword + "%";

            specification = specification.and(
                (root, query, cb) -> cb.or(

                    cb.like(
                        cb.lower(root.get("title")),
                        pattern,
                        '\\'
                    ),

                    cb.like(
                        cb.lower(root.get("description")),
                        pattern,
                        '\\'
                    ),

                    cb.like(
                        cb.lower(root.get("location")),
                        pattern,
                        '\\'
                    )
                )
            );
        }

        return eventRepository.findAll(specification)
            .stream()
            .map(mapper::toEventDTO)
            .toList();
    }

    // =====================================
    // 3. GET EVENT BY ID
    // =====================================

    public EventDTO getEventById(Long id) {

        Event event = findEvent(id);

        return mapper.toEventDTO(event);
    }

    // =====================================
    // 4. UPDATE EVENT
    // =====================================

    @Transactional
    public EventDTO updateEvent(
        Long id,
        EventUpdateRequest request) {

        // Lock the event during the transaction
        Event event = findEventForUpdate(id);

        ensureEventNotCancelled(event);

        validateDates(
            request.startDate(),
            request.endDate()
        );

        mapper.updateEvent(event, request);

        return mapper.toEventDTO(event);
    }

    // =====================================
    // 5. UPDATE EVENT STATUS
    // =====================================

    @Transactional
    public EventDTO updateEventStatus(
        Long id,
        EventStatusUpdateRequest request) {

        // Prevent concurrent status modifications
        Event event = findEventForUpdate(id);

        EventStatus currentStatus = event.getStatus();
        EventStatus newStatus = request.status();

        if (newStatus == null) {

            throw new BusinessException(
                "Event status is required"
            );
        }

        // Idempotence
        if (currentStatus == newStatus) {

            return mapper.toEventDTO(event);
        }

        boolean validTransition = switch (currentStatus) {

            case DRAFT ->
                newStatus == EventStatus.PUBLISHED
                    || newStatus == EventStatus.CANCELLED;

            case PUBLISHED ->
                newStatus == EventStatus.CANCELLED;

            case CANCELLED -> false;
        };

        if (!validTransition) {

            throw new ConflictException(
                "Invalid event status transition from "
                    + currentStatus
                    + " to "
                    + newStatus
            );
        }

        event.setStatus(newStatus);

        return mapper.toEventDTO(event);
    }

    // =====================================
    // 6. CREATE TICKET CATEGORY
    // =====================================

    @Transactional
    public TicketCategoryDTO createCategory(
        Long eventId,
        TicketCategoryCreateRequest request) {

        // Prevent category creation during cancellation
        Event event = findEventForUpdate(eventId);

        ensureEventNotCancelled(event);

        TicketCategory category =
            mapper.toTicketCategory(eventId, request);

        TicketCategory savedCategory =
            categoryRepository.save(category);

        return mapper.toTicketCategoryDTO(savedCategory);
    }

    // =====================================
    // 7. GET EVENT CATEGORIES
    // =====================================

    public List<TicketCategoryDTO> getCategories(
        Long eventId) {

        findEvent(eventId);

        return categoryRepository.findByEventId(eventId)
            .stream()
            .map(mapper::toTicketCategoryDTO)
            .toList();
    }

    // =====================================
    // PRIVATE UTILITY METHODS
    // =====================================

    private Event findEvent(Long id) {

        return eventRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Event not found with id: " + id
                )
            );
    }

    private Event findEventForUpdate(Long id) {

        return eventRepository.findByIdForUpdate(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Event not found with id: " + id
                )
            );
    }

    private void ensureEventNotCancelled(Event event) {

        if (event.getStatus() == EventStatus.CANCELLED) {

            throw new ConflictException(
                "A cancelled event cannot be updated"
            );
        }
    }

    private void validateDates(
        LocalDateTime startDate,
        LocalDateTime endDate) {

        if (startDate == null || endDate == null) {

            throw new BusinessException(
                "Start date and end date are required"
            );
        }

        if (!endDate.isAfter(startDate)) {

            throw new BusinessException(
                "End date must be after start date"
            );
        }
    }
}
