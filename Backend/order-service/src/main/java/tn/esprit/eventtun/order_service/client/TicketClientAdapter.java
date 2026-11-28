
package tn.esprit.eventtun.order_service.client;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketInfo;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketResponse;

@Component
public class TicketClientAdapter implements TicketClient {

    private final TicketFeignClient ticketFeignClient;

    public TicketClientAdapter(TicketFeignClient ticketFeignClient) {
        this.ticketFeignClient = ticketFeignClient;
    }

    @Override
    public TicketResponse issueTickets(TicketRequest request) {

        List<TicketInfo> tickets =
            ticketFeignClient.issueTickets(request);

        if (tickets == null || tickets.isEmpty()) {
            return new TicketResponse(false, List.of());
        }

        List<String> ticketIds = tickets.stream()
            .map(TicketInfo::id)
            .filter(Objects::nonNull)
            .toList();

        return new TicketResponse(
            !ticketIds.isEmpty(),
            ticketIds
        );
    }
}
