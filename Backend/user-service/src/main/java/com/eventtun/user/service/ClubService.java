
package com.eventtun.user.service;

import com.eventtun.user.entity.Club;

import java.util.List;
import java.util.Optional;

public interface ClubService {

    Club createClub(Club club);

    List<Club> findAllClubs();

    Optional<Club> findClubById(Long id);

    Club updateClub(Long id, Club club);
}
