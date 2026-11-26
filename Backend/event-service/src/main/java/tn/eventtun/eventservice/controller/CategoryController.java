package tn.eventtun.eventservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.eventtun.eventservice.dto.TicketCategoryCreateRequest;
import tn.eventtun.eventservice.dto.TicketCategoryDTO;
import tn.eventtun.eventservice.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/api/events/{id}/categories")
public class CategoryController {

    private final EventService eventService;

    public CategoryController(EventService eventService) {
        this.eventService = eventService;
    }

    // 6. CREATE TICKET CATEGORY

    @PostMapping
    public ResponseEntity<TicketCategoryDTO> createCategory(
        @PathVariable Long id,
        @Valid @RequestBody
        TicketCategoryCreateRequest request) {

        TicketCategoryDTO category =
            eventService.createCategory(id, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(category);
    }

    // 7. GET EVENT CATEGORIES

    @GetMapping
    public ResponseEntity<List<TicketCategoryDTO>> getCategories(
        @PathVariable Long id) {

        return ResponseEntity.ok(
            eventService.getCategories(id)
        );
    }
}
