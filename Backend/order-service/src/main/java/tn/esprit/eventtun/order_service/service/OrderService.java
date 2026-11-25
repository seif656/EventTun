
package tn.esprit.eventtun.order_service.service;

import tn.esprit.eventtun.order_service.dto.OrderDtos.*;
import tn.esprit.eventtun.order_service.entity.Order;
import tn.esprit.eventtun.order_service.entity.OrderItem;
import tn.esprit.eventtun.order_service.enums.OrderStatus;
import tn.esprit.eventtun.order_service.repository.OrderRepository;
import tn.esprit.eventtun.order_service.saga.OrderSagaService;
import tn.esprit.eventtun.order_service.saga.OrderSagaService.SagaResult;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.SeatReservationResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderSagaService orderSagaService;

    public OrderService(
        OrderRepository orderRepository,
        OrderSagaService orderSagaService) {

        this.orderRepository = orderRepository;
        this.orderSagaService = orderSagaService;
    }

    // Convertir une entité Order en DTO
    private OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }

    // Convertir une entité Order en DTO avec un message
    private OrderResponse toResponse(Order order, String message) {

        List<OrderItemResponse> itemResponses =
            order.getItems().stream()
                .map(item -> new OrderItemResponse(
                    item.getId(),
                    order.getId(),
                    item.getEventId(),
                    item.getCategoryId(),
                    item.getQuantity(),
                    item.getUnitPrice()
                ))
                .toList();

        return new OrderResponse(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getCreatedAt(),
            itemResponses,
            message
        );
    }

    // Créer une commande avec la Saga
    public OrderResponse createOrderWithSaga(CreateOrderRequest request) {

        // 1. Créer une commande initiale PENDING
        Order order = new Order();

        order.setUserId(request.userId());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.saveAndFlush(order);

        // 2. Exécuter la Saga
        SagaResult sagaResult;

        try {
            sagaResult = orderSagaService.execute(
                savedOrder.getId(),
                request
            );

        } catch (RuntimeException exception) {

            // En cas d'erreur inattendue, garder une trace
            // de la commande dont le résultat est incertain.
            Order failedOrder = orderRepository.findById(
                savedOrder.getId()
            ).orElse(savedOrder);

            failedOrder.setStatus(OrderStatus.COMPENSATION_PENDING);

            orderRepository.save(failedOrder);

            throw new IllegalStateException(
                "Erreur pendant l'exécution de la Saga : "
                    + exception.getMessage(),
                exception
            );
        }

        // 3. Récupérer la commande enregistrée
        Order finalOrder = orderRepository.findById(
            savedOrder.getId()
        ).orElseThrow(() ->
            new RuntimeException("Order not found")
        );

        // 4. Enregistrer les lignes de réservation
        if (sagaResult.reservedItems() != null) {

            for (SeatReservationResponse reservedItem :
                sagaResult.reservedItems()) {

                OrderItem item = new OrderItem();

                item.setEventId(request.eventId());
                item.setCategoryId(reservedItem.categoryId());
                item.setQuantity(reservedItem.quantity());
                item.setUnitPrice(reservedItem.unitPrice());

                finalOrder.addItem(item);
            }
        }

        // 5. Enregistrer le montant final et le statut
        finalOrder.setTotalAmount(
            sagaResult.totalAmount()
        );

        finalOrder.setStatus(
            sagaResult.status()
        );

        // 6. Sauvegarder le résultat final
        Order updatedOrder = orderRepository.save(finalOrder);

        // 7. Retourner également le message de la Saga
        return toResponse(updatedOrder, sagaResult.message());
    }

    // Création d'une commande locale de test
    @Transactional
    public OrderResponse createLocalOrder(
        CreateOrderRequest request,
        List<BigDecimal> unitPrices) {

        if (unitPrices == null ||
            unitPrices.size() != request.items().size()) {

            throw new IllegalArgumentException(
                "A valid price is required for each item"
            );
        }

        Order order = new Order();

        order.setUserId(request.userId());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < request.items().size(); i++) {

            OrderItemRequest requestItem = request.items().get(i);
            BigDecimal price = unitPrices.get(i);

            if (price == null ||
                price.compareTo(BigDecimal.ZERO) < 0) {

                throw new IllegalArgumentException(
                    "Invalid unit price"
                );
            }

            OrderItem item = new OrderItem();

            item.setEventId(request.eventId());
            item.setCategoryId(requestItem.categoryId());
            item.setQuantity(requestItem.quantity());
            item.setUnitPrice(price);

            order.addItem(item);

            BigDecimal lineTotal = price.multiply(
                BigDecimal.valueOf(requestItem.quantity())
            );

            total = total.add(lineTotal);
        }

        order.setTotalAmount(total);

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    // Consulter une commande
    public OrderResponse getOrderById(Long id) {

        Order order = orderRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Order not found"));

        return toResponse(order);
    }

    // Consulter l'historique et filtrer les commandes
    public List<OrderResponse> getOrders(
        Long userId,
        OrderStatus status) {

        return orderRepository.searchOrders(userId, status)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    // Consulter les lignes d'une commande
    public List<OrderItemResponse> getOrderItems(Long orderId) {

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() ->
                new RuntimeException("Order not found"));

        return order.getItems().stream()
            .map(item -> new OrderItemResponse(
                item.getId(),
                order.getId(),
                item.getEventId(),
                item.getCategoryId(),
                item.getQuantity(),
                item.getUnitPrice()
            ))
            .toList();
    }

    // Annuler une commande
    @Transactional
    public OrderResponse cancelOrder(Long id) {

        Order order = orderRepository.findById(id)
            .orElseThrow(() ->
                new RuntimeException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {

            throw new IllegalStateException(
                "Only pending orders can be cancelled locally"
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        return toResponse(orderRepository.save(order));
    }
}
