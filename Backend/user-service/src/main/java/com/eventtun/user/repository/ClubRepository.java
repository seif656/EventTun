
package com.eventtun.user.repository;

import com.eventtun.user.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClubRepository extends JpaRepository<Club, Long> {

    boolean existsByOwnerId(Long ownerId);

}
