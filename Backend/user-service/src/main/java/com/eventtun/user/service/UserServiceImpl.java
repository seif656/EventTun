package com.eventtun.user.service;

import com.eventtun.user.entity.*;
import com.eventtun.user.repository.UserRepository;
import com.eventtun.user.repository.ClubRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final ClubRepository clubs;
    public UserServiceImpl(UserRepository repository, ClubRepository clubs) {
        this.repository = repository;
        this.clubs = clubs;
    }

    @Override
    public User createUser(User user) {
        String email = user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use.");
        }
        user.setId(null);
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);
        if (user.getRole() == null) user.setRole(Role.STUDENT);
        return repository.saveAndFlush(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() { return repository.findAll(); }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUserById(Long id) { return repository.findById(id); }

    @Override
    public User updateUser(Long id, User input) {
        User user = requireUser(id);
        String email = input.getEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use.");
        }
        Role requestedRole = input.getRole() == null ? Role.STUDENT : input.getRole();
        if (requestedRole != Role.ORGANIZER && clubs.existsByOwnerId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Transfer or delete the user's clubs before changing the ORGANIZER role.");
        }
        user.setFirstName(input.getFirstName());
        user.setLastName(input.getLastName());
        user.setEmail(email);
        user.setKeycloakSubject(input.getKeycloakSubject());
        user.setRole(requestedRole);
        // Profile replacement does not reactivate blocked users.
        return repository.saveAndFlush(user);
    }

    @Override
    public User changeStatus(Long id, UserStatus status) {
        User user = requireUser(id);
        user.setStatus(status);
        return repository.saveAndFlush(user);
    }

    private User requireUser(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
