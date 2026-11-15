package tn.esprit.eventtun.order_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.eventtun.order_service.dto.OrderDtos.CreateOrderRequest;
import tn.esprit.eventtun.order_service.dto.OrderDtos.OrderItemResponse;
import tn.esprit.eventtun.order_service.dto.OrderDtos.OrderResponse;
import tn.esprit.eventtun.order_service.dto.OrderDtos.UpdateOrderStatusRequest;
import tn.esprit.eventtun.order_service.enums.OrderStatus;

import java.net.URI;

/**
 * Validation 1 : le contrat REST est documente (Swagger / OpenAPI).
 * La logique metier (Saga d'achat) n'est pas encore developpee :
 * chaque methode repond donc 501 tant qu'elle n'est pas implementee.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Commandes et coordination de l'achat de billets")
public class OrderController {

    private static final String PROBLEM = "application/problem+json";

    @PostMapping
    @Operation(summary = "Creer une commande et lancer la Saga d'achat")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Commande creee (en-tete Location)",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requete invalide", content = @Content(mediaType = PROBLEM)),
            @ApiResponse(responseCode = "403", description = "Utilisateur bloque", content = @Content(mediaType = PROBLEM)),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou categorie introuvable", content = @Content(mediaType = PROBLEM)),
            @ApiResponse(responseCode = "409", description = "Plus de places ou paiement refuse", content = @Content(mediaType = PROBLEM)),
            @ApiResponse(responseCode = "503", description = "Service appele indisponible", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return notImplemented("/api/orders");
    }

    @GetMapping
    @Operation(summary = "Lister les commandes / historique d'un utilisateur (filtres userId et status)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des commandes",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponse.class))))
    })
    public ResponseEntity<Object> listOrders(@RequestParam(required = false) Long userId,
                                             @RequestParam(required = false) OrderStatus status) {
        return notImplemented("/api/orders");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une commande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commande trouvee",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "404", description = "Commande introuvable", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<Object> getOrder(@PathVariable Long id) {
        return notImplemented("/api/orders/" + id);
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Consulter les lignes d'une commande")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lignes de la commande",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderItemResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Commande introuvable", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<Object> getOrderItems(@PathVariable Long id) {
        return notImplemented("/api/orders/" + id + "/items");
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Annuler une commande (statut CANCELLED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Commande annulee",
                    content = @Content(schema = @Schema(implementation = OrderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Statut non autorise", content = @Content(mediaType = PROBLEM)),
            @ApiResponse(responseCode = "404", description = "Commande introuvable", content = @Content(mediaType = PROBLEM)),
            @ApiResponse(responseCode = "409", description = "Annulation impossible (billet deja utilise)", content = @Content(mediaType = PROBLEM))
    })
    public ResponseEntity<Object> updateStatus(@PathVariable Long id,
                                               @Valid @RequestBody UpdateOrderStatusRequest request) {
        return notImplemented("/api/orders/" + id + "/status");
    }

    private ResponseEntity<Object> notImplemented(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_IMPLEMENTED,
                "Endpoint documente ; logique metier prevue apres la validation 1.");
        problem.setTitle("Not Implemented");
        problem.setInstance(URI.create(instance));
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}