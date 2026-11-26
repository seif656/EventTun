package tn.eventtun.eventservice.controller;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.eventtun.eventservice.dto.*;
import tn.eventtun.eventservice.model.EventStatus;
import tn.eventtun.eventservice.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // 1. CREATE EVENT

    @PostMapping
    public ResponseEntity<EventDTO> createEvent(
        @Valid @RequestBody EventCreateRequest request) {

        EventDTO event = eventService.createEvent(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(event);
    }

    // 2. GET EVENTS WITH FILTERS

    @GetMapping
    public ResponseEntity<List<EventDTO>> getEvents(

        @RequestParam(required = false)
        EventStatus status,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,

        @RequestParam(required = false)
        String q) {

        return ResponseEntity.ok(
            eventService.getEvents(status, from, q)
        );
    }

    // 3. GET EVENT BY ID

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getEventById(
        @PathVariable Long id) {

        return ResponseEntity.ok(
            eventService.getEventById(id)
        );
    }

    // 4. UPDATE EVENT

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> updateEvent(
        @PathVariable Long id,
        @Valid @RequestBody EventUpdateRequest request) {

        return ResponseEntity.ok(
            eventService.updateEvent(id, request)
        );
    }

    // 5. UPDATE EVENT STATUS

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventDTO> updateEventStatus(
        @PathVariable Long id,
        @Valid @RequestBody EventStatusUpdateRequest request) {

        return ResponseEntity.ok(
            eventService.updateEventStatus(id, request)
        );
    }
}
