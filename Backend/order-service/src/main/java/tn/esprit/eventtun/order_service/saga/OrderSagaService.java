
package tn.esprit.eventtun.order_service.saga;

import org.springframework.stereotype.Service;

import tn.esprit.eventtun.order_service.client.UserClient;
import tn.esprit.eventtun.order_service.client.EventClient;
import tn.esprit.eventtun.order_service.client.PaymentClient;
import tn.esprit.eventtun.order_service.client.TicketClient;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.*;
import tn.esprit.eventtun.order_service.dto.OrderDtos.CreateOrderRequest;
import tn.esprit.eventtun.order_service.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class OrderSagaService {

    private final UserClient userClient;
    private final EventClient eventClient;
    private final PaymentClient paymentClient;
    private final TicketClient ticketClient;

    public OrderSagaService(
            UserClient userClient,
            EventClient eventClient,
            PaymentClient paymentClient,
            TicketClient ticketClient) {

        this.userClient = userClient;
        this.eventClient = eventClient;
        this.paymentClient = paymentClient;
        this.ticketClient = ticketClient;
    }

    // Résultat final de la Saga
    public record SagaResult(
            Long orderId,
            OrderStatus status,
            BigDecimal totalAmount,
            Long reservationId,
            Long paymentId,
            List<Long> ticketIds,
            List<ReservationItem> reservedItems,
            String message
    ) {}

    // Exécution du processus d'achat
    public SagaResult execute(
            Long orderId,
            CreateOrderRequest request) {

        // ÉTAPE 1 : Vérifier l'utilisateur (M1)
        UserInfo user = userClient.getUser(request.userId());

        if (user == null || !user.exists() || user.blocked()) {
            return result(
                    orderId,
                    OrderStatus.REJECTED,
                    BigDecimal.ZERO,
                    null,
                    null,
                    List.of(),
                    "Utilisateur inexistant ou bloqué"
            );
        }

        // ÉTAPE 2 : Réserver les places (M2)
        List<ReservationRequest> reservationRequests =
                request.items().stream()
                        .map(item -> new ReservationRequest(
                                request.eventId(),
                                item.categoryId(),
                                item.quantity()
                        ))
                        .toList();

        ReservationResponse reservation =
                eventClient.reserveSeats(reservationRequests);

        if (reservation == null
                || !reservation.successful()
                || reservation.reservationId() == null) {

            return result(
                    orderId,
                    OrderStatus.REJECTED,
                    BigDecimal.ZERO,
                    null,
                    null,
                    List.of(),
                    "La réservation des places a échoué"
            );
        }

        Long reservationId = reservation.reservationId();

        // ÉTAPE 3 : Récupérer les prix officiels de M2
        BigDecimal totalAmount;

        try {
            totalAmount = calculateTotal(request, reservation);
        } catch (RuntimeException exception) {

            boolean released = releaseReservation(reservationId);

            return result(
                    orderId,
                    released
                            ? OrderStatus.REJECTED
                            : OrderStatus.COMPENSATION_PENDING,
                    BigDecimal.ZERO,
                    reservationId,
                    null,
                    List.of(),
                    reservation.items(),
                    "Prix ou quantités de réservation invalides"
            );
        }

        // ÉTAPE 4 : Effectuer le paiement (M4)
        PaymentResponse payment;

        try {
            payment = paymentClient.pay(
                    new PaymentRequest(orderId, totalAmount)
            );
        } catch (RuntimeException exception) {

            boolean released = releaseReservation(reservationId);

            return result(
                    orderId,
                    released
                            ? OrderStatus.REJECTED
                            : OrderStatus.COMPENSATION_PENDING,
                    totalAmount,
                    reservationId,
                    null,
                    List.of(),
                    reservation.items(),
                    "Erreur lors de l'appel au service de paiement"
            );
        }

        if (payment == null
                || !payment.successful()
                || payment.paymentId() == null) {

            boolean released = releaseReservation(reservationId);

            return result(
                    orderId,
                    released
                            ? OrderStatus.REJECTED
                            : OrderStatus.COMPENSATION_PENDING,
                    totalAmount,
                    reservationId,
                    null,
                    List.of(),
                    reservation.items(),
                    "Paiement refusé"
            );
        }

        Long paymentId = payment.paymentId();

        // ÉTAPE 5 : Confirmer la réservation (M2)
        boolean reservationConfirmed;

        try {
            reservationConfirmed =
                    eventClient.confirmReservation(reservationId);
        } catch (RuntimeException exception) {
            reservationConfirmed = false;
        }

        if (!reservationConfirmed) {

            boolean compensated =
                    compensate(reservationId, paymentId);

            return result(
                    orderId,
                    compensated
                            ? OrderStatus.REJECTED
                            : OrderStatus.COMPENSATION_PENDING,
                    totalAmount,
                    reservationId,
                    paymentId,
                    List.of(),
                    reservation.items(),
                    "Confirmation de réservation échouée"
            );
        }

        // ÉTAPE 6 : Émettre les billets (M5)
        TicketResponse tickets;

        try {
            tickets = ticketClient.issueTickets(
                    new TicketRequest(
                            orderId,
                            request.userId(),
                            request.eventId()
                    )
            );
        } catch (RuntimeException exception) {
            tickets = null;
        }

        if (tickets == null || !tickets.successful()) {

            boolean compensated =
                    compensate(reservationId, paymentId);

            return result(
                    orderId,
                    compensated
                            ? OrderStatus.REJECTED
                            : OrderStatus.COMPENSATION_PENDING,
                    totalAmount,
                    reservationId,
                    paymentId,
                    List.of(),
                    reservation.items(),
                    "Émission des billets échouée"
            );
        }

        // ÉTAPE 7 : Tout est réussi
        return result(
                orderId,
                OrderStatus.CONFIRMED,
                totalAmount,
                reservationId,
                paymentId,
                tickets.ticketIds() == null
                        ? List.of()
                        : tickets.ticketIds(),
                reservation.items(),
                "Commande confirmée avec succès"
        );
    }

    // Calcul du montant à partir des prix fournis par M2
    private BigDecimal calculateTotal(
            CreateOrderRequest request,
            ReservationResponse reservation) {

        if (reservation.items() == null
                || reservation.items().isEmpty()) {

            throw new IllegalStateException(
                    "Aucun prix retourné par Event Service"
            );
        }

        Map<Long, Integer> requestedQuantities = new HashMap<>();

        request.items().forEach(item ->
                requestedQuantities.merge(
                        item.categoryId(),
                        item.quantity(),
                        Integer::sum
                )
        );

        Map<Long, Integer> reservedQuantities = new HashMap<>();

        BigDecimal total = BigDecimal.ZERO;

        for (ReservationItem item : reservation.items()) {

            if (item.categoryId() == null
                    || item.unitPrice() == null
                    || item.unitPrice().compareTo(BigDecimal.ZERO) < 0
                    || item.quantity() == null
                    || item.quantity() <= 0) {

                throw new IllegalStateException(
                        "Prix ou quantité invalide"
                );
            }

            reservedQuantities.merge(
                    item.categoryId(),
                    item.quantity(),
                    Integer::sum
            );

            total = total.add(
                    item.unitPrice().multiply(
                            BigDecimal.valueOf(item.quantity())
                    )
            );
        }

        if (!requestedQuantities.equals(reservedQuantities)) {
            throw new IllegalStateException(
                    "Les quantités réservées ne correspondent pas à la commande"
            );
        }

        return total;
    }

    // Libérer les places
    private boolean releaseReservation(Long reservationId) {
        try {
            return eventClient.releaseReservation(reservationId);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    // Rembourser et libérer les places
    private boolean compensate(
            Long reservationId,
            Long paymentId) {

        boolean refundOk;

        try {
            refundOk = paymentClient.refund(paymentId);
        } catch (RuntimeException exception) {
            refundOk = false;
        }

        boolean releaseOk = releaseReservation(reservationId);

        return refundOk && releaseOk;
    }

    // Création du résultat de la Saga
    private SagaResult result(
            Long orderId,
            OrderStatus status,
            BigDecimal amount,
            Long reservationId,
            Long paymentId,
            List<Long> ticketIds,
            String message) {

        return result(
                orderId,
                status,
                amount,
                reservationId,
                paymentId,
                ticketIds,
                List.of(),
                message
        );
    }

    private SagaResult result(
            Long orderId,
            OrderStatus status,
            BigDecimal amount,
            Long reservationId,
            Long paymentId,
            List<Long> ticketIds,
            List<ReservationItem> reservedItems,
            String message) {

        return new SagaResult(
                orderId,
                status,
                amount,
                reservationId,
                paymentId,
                ticketIds,
                reservedItems,
                message
        );
    }
}
