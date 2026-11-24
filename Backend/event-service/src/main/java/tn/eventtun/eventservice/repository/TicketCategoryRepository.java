package tn.eventtun.eventservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.eventtun.eventservice.model.TicketCategory;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketCategoryRepository
    extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM TicketCategory c WHERE c.id = :id")
    Optional<TicketCategory> findByIdForUpdate(
        @Param("id") Long id);
}
