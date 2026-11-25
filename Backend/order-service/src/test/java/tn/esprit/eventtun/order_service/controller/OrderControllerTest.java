
package tn.esprit.eventtun.order_service.controller;

import tn.esprit.eventtun.order_service.dto.OrderDtos.*;
import tn.esprit.eventtun.order_service.enums.OrderStatus;
import tn.esprit.eventtun.order_service.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;

    private OrderResponse testOrder;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
            .standaloneSetup(orderController)
            .build();

        testOrder = new OrderResponse(
            1L,
            1L,
            new BigDecimal("50.00"),
            OrderStatus.PENDING,
            LocalDateTime.now(),
            List.of(
                new OrderItemResponse(
                    10L,
                    1L,
                    10L,
                    2L,
                    2,
                    new BigDecimal("25.00")
                )
            ),
            null
        );
    }

    // 1. POST /api/orders
    @Test
    void testCreateOrder() throws Exception {

        OrderResponse confirmedOrder = new OrderResponse(
            1L,
            1L,
            new BigDecimal("50.00"),
            OrderStatus.CONFIRMED,
            LocalDateTime.now(),
            testOrder.items(),
            null
        );

        when(orderService.createOrderWithSaga(any(CreateOrderRequest.class)))
            .thenReturn(confirmedOrder);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "userId": 1,
                                  "eventId": 10,
                                  "items": [
                                    {
                                      "categoryId": 2,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.totalAmount").value(50.00))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(orderService, times(1))
            .createOrderWithSaga(any(CreateOrderRequest.class));
    }

    // 2. GET /api/orders/{id}
    @Test
    void testGetOrder() throws Exception {

        when(orderService.getOrderById(1L))
            .thenReturn(testOrder);

        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.userId").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService).getOrderById(1L);
    }

    // 3. GET /api/orders
    @Test
    void testGetOrders() throws Exception {

        when(orderService.getOrders(1L, OrderStatus.PENDING))
            .thenReturn(List.of(testOrder));

        mockMvc.perform(get("/api/orders")
                .param("userId", "1")
                .param("status", "PENDING"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(orderService).getOrders(1L, OrderStatus.PENDING);
    }

    // 4. GET /api/orders/{id}/items
    @Test
    void testGetOrderItems() throws Exception {

        List<OrderItemResponse> items = testOrder.items();

        when(orderService.getOrderItems(1L))
            .thenReturn(items);

        mockMvc.perform(get("/api/orders/1/items"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].categoryId").value(2))
            .andExpect(jsonPath("$[0].quantity").value(2))
            .andExpect(jsonPath("$[0].unitPrice").value(25.00));

        verify(orderService).getOrderItems(1L);
    }

    // 5. PATCH /api/orders/{id}/status
    @Test
    void testCancelOrder() throws Exception {

        OrderResponse cancelledOrder = new OrderResponse(
            1L,
            1L,
            new BigDecimal("50.00"),
            OrderStatus.CANCELLED,
            LocalDateTime.now(),
            testOrder.items(),
            null
        );

        when(orderService.cancelOrder(1L))
            .thenReturn(cancelledOrder);

        mockMvc.perform(patch("/api/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "status": "CANCELLED"
                                }
                                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrder(1L);
    }
}
