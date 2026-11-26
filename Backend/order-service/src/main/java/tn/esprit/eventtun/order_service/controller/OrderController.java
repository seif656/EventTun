package tn.esprit.eventtun.order_service.controller;

import tn.esprit.eventtun.order_service.dto.OrderDtos.CreateOrderRequest;
import tn.esprit.eventtun.order_service.dto.OrderDtos.OrderItemResponse;
import tn.esprit.eventtun.order_service.dto.OrderDtos.OrderResponse;
import tn.esprit.eventtun.order_service.dto.OrderDtos.UpdateOrderStatusRequest;
import tn.esprit.eventtun.order_service.enums.OrderStatus;
import tn.esprit.eventtun.order_service.exception.OrderCompensationPendingException;
import tn.esprit.eventtun.order_service.exception.OrderRejectedException;
import tn.esprit.eventtun.order_service.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Commandes et coordination de l'achat de billets")
public class OrderController {

    private static final String PROBLEM = "application/problem+json";

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // POST /api/orders : lance la Saga d'achat
    @PostMapping
    @Operation(summary = "Creer une commande et lancer la Saga d'achat")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Commande confirmee (en-tete Location)",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Requete invalide", content = @Content(mediaType = PROBLEM)),
        @ApiResponse(responseCode = "403", description = "Utilisateur bloque", content = @Content(mediaType = PROBLEM)),
        @ApiResponse(responseCode = "404", description = "Utilisateur ou categorie introuvable", content = @Content(mediaType = PROBLEM)),
        @ApiResponse(responseCode = "409", description = "Plus de places ou paiement refuse", content = @Content(mediaType = PROBLEM)),
        @ApiResponse(responseCode = "503", description = "Service indisponible ou compensation en attente", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrderWithSaga(request);

        if (response.status() == OrderStatus.CONFIRMED) {
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
            return ResponseEntity.created(location).body(response);
        }

        if (response.status() == OrderStatus.COMPENSATION_PENDING) {
            throw new OrderCompensationPendingException(response.id());
        }

        // REJECTED (ou tout autre statut non confirme)
        throw new OrderRejectedException(response.id(), response.message());
    }

    // GET /api/orders : consulter et filtrer
    @GetMapping
    @Operation(summary = "Lister les commandes / historique d'un utilisateur (filtres userId et status)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des commandes",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    })
    public ResponseEntity<List<OrderResponse>> getOrders(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) OrderStatus status) {

        return ResponseEntity.ok(orderService.getOrders(userId, status));
    }

    // GET /api/orders/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Consulter une commande")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Commande trouvee",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "404", description = "Commande introuvable", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // GET /api/orders/{id}/items
    @GetMapping("/{id}/items")
    @Operation(summary = "Consulter les lignes d'une commande")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lignes de la commande",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderItemResponse.class)))),
        @ApiResponse(responseCode = "404", description = "Commande introuvable", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderItems(id));
    }

    // PATCH /api/orders/{id}/status : annuler une commande
    @PatchMapping("/{id}/status")
    @Operation(summary = "Annuler une commande (statut CANCELLED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Commande annulee",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))),
        @ApiResponse(responseCode = "400", description = "Statut non autorise", content = @Content(mediaType = PROBLEM)),
        @ApiResponse(responseCode = "404", description = "Commande introuvable", content = @Content(mediaType = PROBLEM)),
        @ApiResponse(responseCode = "409", description = "Annulation impossible dans l'etat actuel", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<OrderResponse> cancelOrder(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrderStatusRequest request) {

        if (request.status() != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Seul le statut CANCELLED est accepte par ce endpoint");
        }

        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
