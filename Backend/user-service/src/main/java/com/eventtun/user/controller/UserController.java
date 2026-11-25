package com.eventtun.user.controller;

import com.eventtun.user.dto.*;
import com.eventtun.user.entity.User;
import com.eventtun.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody UserRequest request) {
        User user = service.createUser(request.toUser());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(location).body(user);
    }

    @GetMapping
    public List<User> list() { return service.findAllUsers(); }

    @GetMapping("/{id}")
    public User get(@PathVariable Long id) {
        checkId(id);
        return service.findUserById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        checkId(id);
        return service.updateUser(id, request.toUser());
    }

    @PatchMapping("/{id}/status")
    public User changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        checkId(id);
        return service.changeStatus(id, request.getStatus());
    }

    private void checkId(Long id) {
        if (id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID must be positive.");
    }
}
