package com.eventtun.ticket.repository;

import com.eventtun.ticket.model.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends MongoRepository<Ticket, String> {

    List<Ticket> findByOwnerId(Long ownerId);

    List<Ticket> findByOrderId(Long orderId);

    List<Ticket> findByOwnerIdAndOrderId(Long ownerId, Long orderId);

    Optional<Ticket> findByUniqueCode(String uniqueCode);
}
