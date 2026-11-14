package com.eventtun.ticket.repository;

import com.eventtun.ticket.model.TicketValidation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TicketValidationRepository extends MongoRepository<TicketValidation, String> {

    List<TicketValidation> findByTicketIdOrderByValidationDateAsc(String ticketId);
}
