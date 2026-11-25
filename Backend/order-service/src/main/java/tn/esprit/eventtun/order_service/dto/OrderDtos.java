package tn.esprit.eventtun.order_service.dto;

import tn.esprit.eventtun.order_service.enums.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    public record OrderItemRequest(

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity

    ) {}

    public record CreateOrderRequest(

        @NotNull(message = "User ID is required")
        @Positive
        Long userId,

        @NotNull(message = "Event ID is required")
        @Positive
        Long eventId,

        @NotEmpty(message = "At least one item is required")
        List<@Valid OrderItemRequest> items

    ) {}

    public record UpdateOrderStatusRequest(

        @NotNull(message = "Status is required")
        OrderStatus status

    ) {}

    public record OrderItemResponse(
        Long id,
        Long orderId,
        Long eventId,
        Long categoryId,
        Integer quantity,
        BigDecimal unitPrice
    ) {}

    public record OrderResponse(
        Long id,
        Long userId,
        BigDecimal totalAmount,
        OrderStatus status,
        LocalDateTime createdAt,
        List<OrderItemResponse>items,
        String message
    ) {}
}
