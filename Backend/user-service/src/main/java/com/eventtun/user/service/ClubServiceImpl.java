package com.eventtun.user.service;

import com.eventtun.user.entity.*;
import com.eventtun.user.repository.ClubRepository;
import com.eventtun.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ClubServiceImpl implements ClubService {
    private final ClubRepository clubs;
    private final UserRepository users;

    public ClubServiceImpl(ClubRepository clubs, UserRepository users) {
        this.clubs = clubs;
        this.users = users;
    }

    @Override
    public Club createClub(Club club) {
        checkOwner(club.getOwnerId());
        club.setId(null);
        club.setCreatedAt(LocalDateTime.now());
        return clubs.saveAndFlush(club);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> findAllClubs() { return clubs.findAll(); }

    @Override
    @Transactional(readOnly = true)
    public Optional<Club> findClubById(Long id) { return clubs.findById(id); }

    @Override
    public Club updateClub(Long id, Club input) {
        Club club = requireClub(id);
        checkOwner(input.getOwnerId());
        club.setName(input.getName());
        club.setDescription(input.getDescription());
        club.setOwnerId(input.getOwnerId());
        return clubs.saveAndFlush(club);
    }

    @Override
    public void deleteClub(Long id) {
        clubs.delete(requireClub(id));
        clubs.flush();
    }

    private Club requireClub(Long id) {
        return clubs.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Club not found."));
    }

    private void checkOwner(Long ownerId) {
        User owner = users.findById(ownerId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Club owner not found."));
        if (owner.getRole() != Role.ORGANIZER) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Club owner must have the ORGANIZER role.");
        }
    }
}
