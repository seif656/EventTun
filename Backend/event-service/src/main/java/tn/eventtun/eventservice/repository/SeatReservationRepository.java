package tn.eventtun.eventservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.eventtun.eventservice.model.SeatReservation;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatReservationRepository
    extends JpaRepository<SeatReservation, Long> {

    Optional<SeatReservation> findByOrderIdAndCategoryId(
        Long orderId, Long categoryId);

    List<SeatReservation> findByCategoryIdIn(
        List<Long> categoryIds);
}
