
package tn.esprit.eventtun.order_service.service;

import tn.esprit.eventtun.order_service.dto.OrderDtos.*;
import tn.esprit.eventtun.order_service.entity.Order;
import tn.esprit.eventtun.order_service.entity.OrderItem;
import tn.esprit.eventtun.order_service.enums.OrderStatus;
import tn.esprit.eventtun.order_service.repository.OrderRepository;
import tn.esprit.eventtun.order_service.saga.OrderSagaService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderSagaService orderSagaService;

    @InjectMocks
    private OrderService orderService;

    // 1. Création d'une commande locale
    @Test
    void testCreateLocalOrder() {

        CreateOrderRequest request = new CreateOrderRequest(
            1L, 10L,
            List.of(new OrderItemRequest(2L, 2))
        );

        when(orderRepository.save(any(Order.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.createLocalOrder(
            request,
            List.of(new BigDecimal("25.00"))
        );

        assertEquals(1L, result.userId());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());
        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(1, result.items().size());
        assertEquals(2, result.items().get(0).quantity());

        verify(orderRepository).save(any(Order.class));
    }

    // 2. Refuser une liste de prix incomplète
    @Test
    void testCreateLocalOrderWithInvalidPrices() {

        CreateOrderRequest request = new CreateOrderRequest(
            1L, 10L,
            List.of(new OrderItemRequest(2L, 2))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createLocalOrder(request, List.of())
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    // 3. Refuser un prix négatif
    @Test
    void testCreateLocalOrderWithNegativePrice() {

        CreateOrderRequest request = new CreateOrderRequest(
            1L, 10L,
            List.of(new OrderItemRequest(2L, 2))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> orderService.createLocalOrder(
                request,
                List.of(new BigDecimal("-10.00"))
            )
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    // 4. Consulter une commande existante
    @Test
    void testGetOrderById() {

        Order order = createTestOrder();

        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(order));

        OrderResponse result = orderService.getOrderById(1L);

        assertEquals(1L, result.id());
        assertEquals(1L, result.userId());
        assertEquals(new BigDecimal("50.00"), result.totalAmount());
        assertEquals(OrderStatus.PENDING, result.status());

        verify(orderRepository).findById(1L);
    }

    // 5. Consulter une commande inexistante
    @Test
    void testGetOrderByIdNotFound() {

        when(orderRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertThrows(
            RuntimeException.class,
            () -> orderService.getOrderById(99L)
        );
    }

    // 6. Rechercher les commandes
    @Test
    void testGetOrders() {

        Order order = createTestOrder();

        when(orderRepository.searchOrders(1L, OrderStatus.PENDING))
            .thenReturn(List.of(order));

        List<OrderResponse> result = orderService.getOrders(
            1L, OrderStatus.PENDING
        );

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).userId());
        assertEquals(OrderStatus.PENDING, result.get(0).status());

        verify(orderRepository).searchOrders(1L, OrderStatus.PENDING);
    }

    // 7. Consulter les lignes d'une commande
    @Test
    void testGetOrderItems() {

        Order order = createTestOrder();

        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(order));

        List<OrderItemResponse> result =
            orderService.getOrderItems(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).eventId());
        assertEquals(2L, result.get(0).categoryId());
        assertEquals(2, result.get(0).quantity());
        assertEquals(
            new BigDecimal("25.00"),
            result.get(0).unitPrice()
        );
    }

    // 8. Annuler une commande PENDING
    @Test
    void testCancelOrder() {

        Order order = createTestOrder();

        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse result = orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, result.status());

        verify(orderRepository).save(order);
    }

    // 9. Refuser l'annulation d'une commande confirmée
    @Test
    void testCancelConfirmedOrder() {

        Order order = createTestOrder();
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L))
            .thenReturn(Optional.of(order));

        assertThrows(
            IllegalStateException.class,
            () -> orderService.cancelOrder(1L)
        );

        verify(orderRepository, never()).save(any(Order.class));
    }

    // Méthode utilitaire
    private Order createTestOrder() {

        Order order = new Order();

        order.setId(1L);
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setStatus(OrderStatus.PENDING);

        OrderItem item = new OrderItem();

        item.setEventId(10L);
        item.setCategoryId(2L);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("25.00"));

        order.addItem(item);

        return order;
    }
}
