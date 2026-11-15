package tn.esprit.eventtun.order_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tn.esprit.eventtun.order_service.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Objets echanges en JSON (requetes et reponses) par Order Service. */
public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemRequest(
            @NotNull Long categoryId,
            @NotNull @Min(1) Integer quantity) {
    }

    public record CreateOrderRequest(
            @NotNull Long userId,
            @NotNull Long eventId,
            @NotEmpty List<@Valid OrderItemRequest> items) {
    }

    public record UpdateOrderStatusRequest(
            @NotNull OrderStatus status) {
    }

    public record OrderResponse(
            Long id,
            Long userId,
            BigDecimal totalAmount,
            OrderStatus status,
            LocalDateTime createdAt) {
    }

    public record OrderItemResponse(
            Long id,
            Long orderId,
            Long eventId,
            Long categoryId,
            Integer quantity,
            BigDecimal unitPrice) {
    }
}