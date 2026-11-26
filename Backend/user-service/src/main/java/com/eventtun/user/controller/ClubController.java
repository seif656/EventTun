package com.eventtun.user.controller;

import com.eventtun.user.dto.ClubRequest;
import com.eventtun.user.entity.Club;
import com.eventtun.user.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {
    private final ClubService service;
    public ClubController(ClubService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<Club> create(@Valid @RequestBody ClubRequest request) {
        Club club = service.createClub(request.toClub());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(club.getId()).toUri();
        return ResponseEntity.created(location).body(club);
    }

    @GetMapping
    public List<Club> list() { return service.findAllClubs(); }

    @GetMapping("/{id}")
    public Club get(@PathVariable Long id) {
        checkId(id);
        return service.findClubById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found."));
    }

    @PutMapping("/{id}")
    public Club update(@PathVariable Long id, @Valid @RequestBody ClubRequest request) {
        checkId(id);
        return service.updateClub(id, request.toClub());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        checkId(id);
        service.deleteClub(id);
        return ResponseEntity.noContent().build();
    }

    private void checkId(Long id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID must be positive.");
    }
}
