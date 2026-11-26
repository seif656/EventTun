package tn.esprit.eventtun.order_service.client;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.PaymentResponse;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.TicketResponse;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.UserInfo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MockClientsConfig {

    // Mock du User Service (M1)
    @Bean
    public UserClient userClient() {
        return userId -> new UserInfo(
            userId,
            true,
            false
        );
    }

    // Mock du Payment Service (M4)
    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient() {

            @Override
            public PaymentResponse pay(
                ExternalServiceDtos.PaymentRequest request
            ) {
                return new PaymentResponse(
                    200L,
                    true
                );
            }

            @Override
            public boolean refund(Long paymentId) {
                return true;
            }
        };
    }

    // Mock du Ticket Service (M5)
    @Bean
    public TicketClient ticketClient() {
        return request -> new TicketResponse(
            true,
            List.of(1001L, 1002L)
        );
    }
}

