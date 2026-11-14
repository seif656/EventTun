package tn.eventtun.eventservice.repository;
import tn.eventtun.eventservice.model.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {}
