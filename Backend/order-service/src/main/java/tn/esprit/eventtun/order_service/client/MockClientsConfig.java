
package tn.esprit.eventtun.order_service.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

import static tn.esprit.eventtun.order_service.client.ExternalServiceDtos.*;

@Configuration
public class MockClientsConfig {

    // Utilisateur existant mais BLOQUÉ (test)
    @Bean
    public UserClient userClient() {
        return userId -> new UserInfo(
                userId,
                true,
                false
        );
    }

    @Bean
    public EventClient eventClient() {
        return new EventClient() {

            @Override
            public ReservationResponse reserveSeats(
                    List<ReservationRequest> items) {

                List<ReservationItem> reservedItems =
                        items.stream()
                                .map(item -> new ReservationItem(
                                        item.categoryId(),
                                        item.quantity(),
                                        new BigDecimal("25.00")
                                ))
                                .toList();

                return new ReservationResponse(
                        100L,
                        true,
                        reservedItems
                );
            }

            @Override
            public boolean confirmReservation(Long reservationId) {
                return true;
            }

            @Override
            public boolean releaseReservation(Long reservationId) {
                return true;
            }
        };
    }

    @Bean
    public PaymentClient paymentClient() {
        return new PaymentClient() {

            @Override
            public PaymentResponse pay(PaymentRequest request) {
                return new PaymentResponse(200L, true);
            }

            @Override
            public boolean refund(Long paymentId) {
                return true;
            }
        };
    }

    @Bean
    public TicketClient ticketClient() {
        return request -> new TicketResponse(
                true,
                List.of(1001L, 1002L)
        );
    }
}

