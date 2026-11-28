
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
import tn.esprit.eventtun.order_service.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    private CreateOrderRequest request() {
        return new CreateOrderRequest(
            1L,
            1L,
            List.of(new OrderItemRequest(1L, 2))
        );
    }

    private SeatReservationResponse reservation() {
        return new SeatReservationResponse(
            100L,
            10L,
            1L,
            2,
            ReservationStatus.HELD,
            LocalDateTime.now(),
            new BigDecimal("25.00")
        );
    }

    private SeatReservationResponse confirmedReservation() {
        return new SeatReservationResponse(
            100L,
            10L,
            1L,
            2,
            ReservationStatus.CONFIRMED,
            LocalDateTime.now(),
            new BigDecimal("25.00")
        );
    }

    private void mockSuccessfulReservation() {
        when(eventClient.reserveSeats(
            eq(1L),
            any(ReservationRequest.class)
        )).thenReturn(reservation());
    }

    private void mockSuccessfulPayment() {
        when(paymentClient.pay(any(PaymentRequest.class)))
            .thenReturn(new PaymentResponse(200L, true));
    }

    private void mockSuccessfulConfirmation(Long orderId) {
        when(eventClient.confirmReservation(
            eq(1L),
            eq(orderId),
            any(ReservationStatusUpdateRequest.class)
        )).thenReturn(confirmedReservation());
    }

    private void mockSuccessfulRelease(Long orderId) {
        doNothing().when(eventClient).releaseReservation(1L, orderId);
    }

    @Test
    void testSuccessfulOrder() {

        when(userClient.getUser(1L))
            .thenReturn(new UserInfo(1L, true, false));

        mockSuccessfulReservation();
        mockSuccessfulPayment();
        mockSuccessfulConfirmation(10L);

        when(ticketClient.issueTickets(any(TicketRequest.class)))
            .thenReturn(new TicketResponse(
                true,
                List.of("ticket-1001", "ticket-1002")
            ));

        OrderSagaService.SagaResult result =
            orderSagaService.execute(10L, request());

        assertEquals(OrderStatus.CONFIRMED, result.status());
        assertEquals(10L, result.orderId());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());
        assertEquals(100L, result.reservationId());
        assertEquals(200L, result.paymentId());

        assertEquals(
            List.of("ticket-1001", "ticket-1002"),
            result.ticketIds()
        );

        verify(userClient).getUser(1L);

        verify(eventClient).reserveSeats(
            eq(1L),
            any(ReservationRequest.class)
        );

        verify(paymentClient).pay(any(PaymentRequest.class));

        verify(eventClient).confirmReservation(
            eq(1L),
            eq(10L),
            any(ReservationStatusUpdateRequest.class)
        );

        verify(ticketClient).issueTickets(argThat(ticketRequest ->
            ticketRequest.orderId().equals(10L)
                && ticketRequest.eventId().equals(1L)
                && ticketRequest.ownerId().equals(1L)
                && ticketRequest.quantity().equals(2)
        ));

        verify(paymentClient, never()).refund(anyLong());
    }

    @Test
    void testBlockedUser() {

        when(userClient.getUser(1L))
            .thenReturn(new UserInfo(1L, true, true));

        OrderSagaService.SagaResult result =
            orderSagaService.execute(11L, request());

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(BigDecimal.ZERO, result.totalAmount());

        verify(userClient).getUser(1L);
        verifyNoInteractions(eventClient, paymentClient, ticketClient);
    }

    @Test
    void testPaymentFailure() {

        when(userClient.getUser(1L))
            .thenReturn(new UserInfo(1L, true, false));

        mockSuccessfulReservation();

        when(paymentClient.pay(any(PaymentRequest.class)))
            .thenReturn(new PaymentResponse(null, false));

        mockSuccessfulRelease(12L);

        OrderSagaService.SagaResult result =
            orderSagaService.execute(12L, request());

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());
        assertEquals(100L, result.reservationId());
        assertNull(result.paymentId());

        verify(eventClient).reserveSeats(
            eq(1L),
            any(ReservationRequest.class)
        );

        verify(paymentClient).pay(any(PaymentRequest.class));

        verify(eventClient).releaseReservation(1L, 12L);

        verify(eventClient, never()).confirmReservation(
            anyLong(),
            anyLong(),
            any(ReservationStatusUpdateRequest.class)
        );

        verify(ticketClient, never())
            .issueTickets(any(TicketRequest.class));

        verify(paymentClient, never()).refund(anyLong());
    }

    @Test
    void testReservationConfirmationFailure() {

        when(userClient.getUser(1L))
            .thenReturn(new UserInfo(1L, true, false));

        mockSuccessfulReservation();
        mockSuccessfulPayment();

        when(eventClient.confirmReservation(
            eq(1L),
            eq(13L),
            any(ReservationStatusUpdateRequest.class)
        )).thenReturn(null);

        when(paymentClient.refund(200L))
            .thenReturn(true);

        mockSuccessfulRelease(13L);

        OrderSagaService.SagaResult result =
            orderSagaService.execute(13L, request());

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());

        verify(paymentClient).refund(200L);
        verify(eventClient).releaseReservation(1L, 13L);

        verify(ticketClient, never())
            .issueTickets(any(TicketRequest.class));
    }

    @Test
    void testTicketGenerationFailure() {

        when(userClient.getUser(1L))
            .thenReturn(new UserInfo(1L, true, false));

        mockSuccessfulReservation();
        mockSuccessfulPayment();
        mockSuccessfulConfirmation(14L);

        when(ticketClient.issueTickets(any(TicketRequest.class)))
            .thenReturn(new TicketResponse(false, List.of()));

        when(paymentClient.refund(200L))
            .thenReturn(true);

        mockSuccessfulRelease(14L);

        OrderSagaService.SagaResult result =
            orderSagaService.execute(14L, request());

        assertEquals(OrderStatus.REJECTED, result.status());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());

        verify(paymentClient).refund(200L);
        verify(eventClient).releaseReservation(1L, 14L);
        verify(ticketClient).issueTickets(any(TicketRequest.class));
    }

    @Test
    void testCompensationFailure() {

        when(userClient.getUser(1L))
            .thenReturn(new UserInfo(1L, true, false));

        mockSuccessfulReservation();
        mockSuccessfulPayment();

        when(eventClient.confirmReservation(
            eq(1L),
            eq(15L),
            any(ReservationStatusUpdateRequest.class)
        )).thenReturn(null);

        when(paymentClient.refund(200L))
            .thenReturn(false);

        mockSuccessfulRelease(15L);

        OrderSagaService.SagaResult result =
            orderSagaService.execute(15L, request());

        assertEquals(
            OrderStatus.COMPENSATION_PENDING,
            result.status()
        );

        verify(paymentClient).refund(200L);
        verify(eventClient).releaseReservation(1L, 15L);
    }
}
