
package tn.esprit.eventtun.order_service.controller;

import tn.esprit.eventtun.order_service.dto.OrderDtos.*;
import tn.esprit.eventtun.order_service.enums.OrderStatus;
import tn.esprit.eventtun.order_service.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // POST /api/orders
    // Créer une commande avec la Saga
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response =
                orderService.createOrderWithSaga(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // POST /api/orders/local-test
    // Créer une commande avec des prix fournis manuellement
    @PostMapping("/local-test")
    public ResponseEntity<OrderResponse> createLocalOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestParam List<BigDecimal> prices) {

        OrderResponse response =
                orderService.createLocalOrder(request, prices);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET /api/orders/{id}
    // Consulter une commande
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.getOrderById(id)
        );
    }

    // GET /api/orders
    // Consulter et filtrer les commandes
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) OrderStatus status) {

        return ResponseEntity.ok(
                orderService.getOrders(userId, status)
        );
    }

    // GET /api/orders/{id}/items
    // Consulter les lignes d'une commande
    @GetMapping("/{id}/items")
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                orderService.getOrderItems(id)
        );
    }

    // PATCH /api/orders/{id}/status
    // Annuler une commande
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        if (request.status() != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException(
                    "Only CANCELLED status is accepted by this endpoint"
            );
        }

        return ResponseEntity.ok(
                orderService.cancelOrder(id)
        );
    }
}
