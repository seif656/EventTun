package tn.eventtun.eventservice.repository;
import tn.eventtun.eventservice.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
public interface EventRepository extends JpaRepository<Event, Long> {}
