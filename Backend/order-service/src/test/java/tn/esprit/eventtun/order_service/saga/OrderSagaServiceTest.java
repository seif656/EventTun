
package tn.esprit.eventtun.order_service.saga;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.eventtun.order_service.client.UserClient;
import tn.esprit.eventtun.order_service.client.EventClient;
import tn.esprit.eventtun.order_service.client.PaymentClient;
import tn.esprit.eventtun.order_service.client.TicketClient;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.*;
import tn.esprit.eventtun.order_service.dto.OrderDtos.*;
import tn.esprit.eventtun.order_service.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private EventClient eventClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private TicketClient ticketClient;

    @InjectMocks
    private OrderSagaService orderSagaService;

    @Test
    void testSuccessfulOrder() {

        // 1. Préparer la demande
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        // 2. Simuler un utilisateur actif
        when(userClient.getUser(1L))
                .thenReturn(new UserInfo(1L, true, false));

        // 3. Simuler une réservation réussie
        ReservationResponse reservation = new ReservationResponse(
                100L,
                true,
                List.of(new ReservationItem(
                        1L,
                        2,
                        new BigDecimal("25.00")
                ))
        );

        when(eventClient.reserveSeats(anyList()))
                .thenReturn(reservation);

        // 4. Simuler un paiement réussi
        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(200L, true));

        // 5. Simuler la confirmation de réservation
        when(eventClient.confirmReservation(100L))
                .thenReturn(true);

        // 6. Simuler la génération des billets
        when(ticketClient.issueTickets(any(TicketRequest.class)))
                .thenReturn(new TicketResponse(
                        true,
                        List.of(1001L, 1002L)
                ));

        // 7. Exécuter la Saga
        OrderSagaService.SagaResult result =
                orderSagaService.execute(10L, request);

        // 8. Vérifier le résultat
        assertEquals(OrderStatus.CONFIRMED, result.status());

        assertEquals(10L, result.orderId());

        assertEquals(
                new BigDecimal("50.00"),
                result.totalAmount()
        );

        assertEquals(100L, result.reservationId());

        assertEquals(200L, result.paymentId());

        assertEquals(
                List.of(1001L, 1002L),
                result.ticketIds()
        );

        // 9. Vérifier les appels aux services
        verify(userClient).getUser(1L);
        verify(eventClient).reserveSeats(anyList());
        verify(paymentClient).pay(any(PaymentRequest.class));
        verify(eventClient).confirmReservation(100L);
        verify(ticketClient).issueTickets(any(TicketRequest.class));

        // Aucun remboursement ne doit être effectué
        verify(paymentClient, never()).refund(anyLong());
    }
    @Test
    void testBlockedUser() {

        CreateOrderRequest request = new CreateOrderRequest(
                1L, 1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        when(userClient.getUser(1L))
                .thenReturn(new UserInfo(1L, true, true));

        OrderSagaService.SagaResult result =
                orderSagaService.execute(11L, request);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(BigDecimal.ZERO, result.totalAmount());

        verify(userClient).getUser(1L);

        verifyNoInteractions(eventClient, paymentClient, ticketClient);
    }
    @Test
    void testPaymentFailure() {

        CreateOrderRequest request = new CreateOrderRequest(
                1L, 1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        when(userClient.getUser(1L))
                .thenReturn(new UserInfo(1L, true, false));

        ReservationResponse reservation = new ReservationResponse(
                100L,
                true,
                List.of(new ReservationItem(
                        1L, 2, new BigDecimal("25.00")
                ))
        );

        when(eventClient.reserveSeats(anyList()))
                .thenReturn(reservation);

        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(null, false));

        when(eventClient.releaseReservation(100L))
                .thenReturn(true);

        OrderSagaService.SagaResult result =
                orderSagaService.execute(12L, request);

        // Vérifier le résultat
        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());
        assertEquals(100L, result.reservationId());
        assertNull(result.paymentId());

        // Vérifier les appels aux services
        verify(userClient).getUser(1L);
        verify(eventClient).reserveSeats(anyList());
        verify(paymentClient).pay(any(PaymentRequest.class));
        verify(eventClient).releaseReservation(100L);

        // Vérifier qu'aucun billet n'est généré
        verify(eventClient, never()).confirmReservation(100L);
        verify(ticketClient, never()).issueTickets(any(TicketRequest.class));

        // Aucun remboursement car le paiement a été refusé
        verify(paymentClient, never()).refund(anyLong());
    }
    @Test
    void testReservationConfirmationFailure() {

        CreateOrderRequest request = new CreateOrderRequest(
                1L, 1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        when(userClient.getUser(1L))
                .thenReturn(new UserInfo(1L, true, false));

        ReservationResponse reservation = new ReservationResponse(
                100L,
                true,
                List.of(new ReservationItem(
                        1L, 2, new BigDecimal("25.00")
                ))
        );

        when(eventClient.reserveSeats(anyList()))
                .thenReturn(reservation);

        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(200L, true));

        when(eventClient.confirmReservation(100L))
                .thenReturn(false);

        when(paymentClient.refund(200L))
                .thenReturn(true);

        when(eventClient.releaseReservation(100L))
                .thenReturn(true);

        OrderSagaService.SagaResult result =
                orderSagaService.execute(13L, request);

        // Vérifier le résultat
        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());

        // Vérifier la compensation
        verify(paymentClient).refund(200L);
        verify(eventClient).releaseReservation(100L);

        // Aucun billet ne doit être généré
        verify(ticketClient, never())
                .issueTickets(any(TicketRequest.class));
    }
    @Test
    void testTicketGenerationFailure() {

        CreateOrderRequest request = new CreateOrderRequest(
                1L, 1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        when(userClient.getUser(1L))
                .thenReturn(new UserInfo(1L, true, false));

        ReservationResponse reservation = new ReservationResponse(
                100L,
                true,
                List.of(new ReservationItem(
                        1L, 2, new BigDecimal("25.00")
                ))
        );

        when(eventClient.reserveSeats(anyList()))
                .thenReturn(reservation);

        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(200L, true));

        when(eventClient.confirmReservation(100L))
                .thenReturn(true);

        when(ticketClient.issueTickets(any(TicketRequest.class)))
                .thenReturn(new TicketResponse(false, List.of()));

        when(paymentClient.refund(200L))
                .thenReturn(true);

        when(eventClient.releaseReservation(100L))
                .thenReturn(true);

        OrderSagaService.SagaResult result =
                orderSagaService.execute(14L, request);

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());

        verify(paymentClient).refund(200L);
        verify(eventClient).releaseReservation(100L);
        verify(ticketClient).issueTickets(any(TicketRequest.class));
    }
    @Test
    void testCompensationFailure() {

        CreateOrderRequest request = new CreateOrderRequest(
                1L, 1L,
                List.of(new OrderItemRequest(1L, 2))
        );

        when(userClient.getUser(1L))
                .thenReturn(new UserInfo(1L, true, false));

        ReservationResponse reservation = new ReservationResponse(
                100L,
                true,
                List.of(new ReservationItem(
                        1L, 2, new BigDecimal("25.00")
                ))
        );

        when(eventClient.reserveSeats(anyList()))
                .thenReturn(reservation);

        when(paymentClient.pay(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(200L, true));

        when(eventClient.confirmReservation(100L))
                .thenReturn(false);

        // Le remboursement échoue
        when(paymentClient.refund(200L))
                .thenReturn(false);

        // La libération des places réussit
        when(eventClient.releaseReservation(100L))
                .thenReturn(true);

        OrderSagaService.SagaResult result =
                orderSagaService.execute(15L, request);

        assertEquals(
                OrderStatus.COMPENSATION_PENDING,
                result.status()
        );

        verify(paymentClient).refund(200L);
        verify(eventClient).releaseReservation(100L);
    }
}
