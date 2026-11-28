package tn.esprit.eventtun.order_service.client;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketResponse;

public interface TicketClient {

    TicketResponse issueTickets(TicketRequest request);
}
