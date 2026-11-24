package tn.eventtun.eventservice.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tn.eventtun.eventservice.dto.*;
import tn.eventtun.eventservice.service.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(
        ReservationService reservationService) {

        this.reservationService = reservationService;
    }

    // 8. RESERVE SEATS

    @PostMapping("/categories/{id}/reservations")
    public ResponseEntity<SeatReservationDTO> reserveSeats(
        @PathVariable Long id,
        @Valid @RequestBody ReservationCreateRequest request) {

        ReservationService.ReservationResult result =
            reservationService.reserveSeats(id, request);

        HttpStatus status = result.created()
            ? HttpStatus.CREATED
            : HttpStatus.OK;

        return ResponseEntity
            .status(status)
            .body(result.reservation());
    }

    // 9. RELEASE RESERVATION

    @DeleteMapping("/categories/{id}/reservations/{orderId}")
    public ResponseEntity<Void> releaseReservation(
        @PathVariable Long id,
        @PathVariable Long orderId) {

        reservationService.releaseReservation(id, orderId);

        return ResponseEntity.noContent().build();
    }

    // 10. CONFIRM RESERVATION

    @PatchMapping(
        "/categories/{id}/reservations/{orderId}/status")
    public ResponseEntity<SeatReservationDTO> confirmReservation(
        @PathVariable Long id,
        @PathVariable Long orderId,
        @Valid @RequestBody
        ReservationStatusUpdateRequest request) {

        return ResponseEntity.ok(
            reservationService.confirmReservation(
                id, orderId, request)
        );
    }

    // 11. GET EVENT RESERVATIONS

    @GetMapping("/events/{id}/reservations")
    public ResponseEntity<List<SeatReservationDTO>>
    getEventReservations(@PathVariable Long id) {

        return ResponseEntity.ok(
            reservationService.getEventReservations(id)
        );
    }
}
