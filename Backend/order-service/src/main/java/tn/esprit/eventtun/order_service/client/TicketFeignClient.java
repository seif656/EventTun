
package tn.esprit.eventtun.order_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketInfo;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketRequest;

@FeignClient(name = "ticket-service")
public interface TicketFeignClient {

    @PostMapping("/api/tickets")
    List<TicketInfo> issueTickets(
        @RequestBody TicketRequest request
    );
}
