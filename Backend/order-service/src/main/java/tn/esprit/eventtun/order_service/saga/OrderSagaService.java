
package tn.esprit.eventtun.order_service.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tn.esprit.eventtun.order_service.client.UserClient;
import tn.esprit.eventtun.order_service.client.EventClient;
import tn.esprit.eventtun.order_service.client.PaymentClient;
import tn.esprit.eventtun.order_service.client.TicketClient;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.*;
import tn.esprit.eventtun.order_service.dto.OrderDtos.CreateOrderRequest;
import tn.esprit.eventtun.order_service.enums.OrderStatus;
import tn.esprit.eventtun.order_service.enums.ReservationStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderSagaService {

    private static final Logger log =
        LoggerFactory.getLogger(OrderSagaService.class);

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

    public record SagaResult(
        Long orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        Long reservationId,
        Long paymentId,
        List<String> ticketIds,
        List<SeatReservationResponse> reservedItems,
        String message
    ) {}

    // Exécution de la Saga
    public SagaResult execute(Long orderId, CreateOrderRequest request) {

        log.info("Début Saga : orderId={}", orderId);

        // ÉTAPE 1 : Vérifier l'utilisateur (M1)
        UserInfo user = userClient.getUser(request.userId());

        if (user == null || !user.exists() || user.blocked()) {
            log.warn(
                "Utilisateur inexistant ou bloqué : userId={}",
                request.userId()
            );

            return result(
                orderId,
                OrderStatus.REJECTED,
                BigDecimal.ZERO,
                null,
                null,
                List.of(),
                List.of(),
                "Utilisateur inexistant ou bloqué"
            );
        }

        // ÉTAPE 2 : Regrouper les quantités par catégorie
        Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();

        request.items().forEach(item ->
            requestedQuantities.merge(
                item.categoryId(),
                item.quantity(),
                Integer::sum
            )
        );

        // Quantité totale de billets demandée à M5
        int totalQuantity = requestedQuantities.values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum();

        // ÉTAPE 3 : Réserver chaque catégorie auprès de M2
        List<SeatReservationResponse> reservedItems = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {

            Long categoryId = entry.getKey();
            Integer quantity = entry.getValue();

            try {
                log.info(
                    "Réservation demandée : orderId={}, categoryId={}, quantity={}",
                    orderId, categoryId, quantity
                );

                SeatReservationResponse reservation = eventClient.reserveSeats(
                    categoryId,
                    new ReservationRequest(orderId, quantity)
                );

                if (reservation == null
                    || reservation.id() == null
                    || reservation.categoryId() == null
                    || reservation.quantity() == null
                    || reservation.unitPrice() == null) {

                    log.error(
                        "Réponse de réservation invalide : categoryId={}",
                        categoryId
                    );

                    boolean released = releaseReservations(
                        reservedItems, orderId
                    );

                    return result(
                        orderId,
                        released
                            ? OrderStatus.REJECTED
                            : OrderStatus.COMPENSATION_PENDING,
                        BigDecimal.ZERO,
                        firstReservationId(reservedItems),
                        null,
                        List.of(),
                        reservedItems,
                        "La réservation des places a échoué"
                    );
                }

                if (!categoryId.equals(reservation.categoryId())
                    || !quantity.equals(reservation.quantity())
                    || reservation.status() != ReservationStatus.HELD) {

                    log.error(
                        "Informations de réservation invalides : {}",
                        reservation
                    );

                    reservedItems.add(reservation);

                    boolean released = releaseReservations(
                        reservedItems, orderId
                    );

                    return result(
                        orderId,
                        released
                            ? OrderStatus.REJECTED
                            : OrderStatus.COMPENSATION_PENDING,
                        BigDecimal.ZERO,
                        firstReservationId(reservedItems),
                        null,
                        List.of(),
                        reservedItems,
                        "Les informations de réservation sont invalides"
                    );
                }

                reservedItems.add(reservation);

                log.info(
                    "Réservation réussie : reservationId={}, categoryId={}, status={}",
                    reservation.id(),
                    categoryId,
                    reservation.status()
                );

            } catch (RuntimeException exception) {

                log.error(
                    "ERREUR RÉSERVATION : orderId={}, categoryId={}",
                    orderId,
                    categoryId,
                    exception
                );

                boolean released = releaseReservations(
                    reservedItems, orderId
                );

                return result(
                    orderId,
                    released
                        ? OrderStatus.REJECTED
                        : OrderStatus.COMPENSATION_PENDING,
                    BigDecimal.ZERO,
                    firstReservationId(reservedItems),
                    null,
                    List.of(),
                    reservedItems,
                    "Erreur lors de la réservation auprès de Event Service"
                );
            }
        }

        // ÉTAPE 4 : Calculer le montant avec les prix de M2
        BigDecimal totalAmount;

        try {
            totalAmount = calculateTotal(
                requestedQuantities,
                reservedItems
            );

            log.info(
                "Montant total calculé : orderId={}, amount={}",
                orderId,
                totalAmount
            );

        } catch (RuntimeException exception) {

            log.error(
                "ERREUR CALCUL MONTANT : orderId={}",
                orderId,
                exception
            );

            boolean released = releaseReservations(
                reservedItems, orderId
            );

            return result(
                orderId,
                released
                    ? OrderStatus.REJECTED
                    : OrderStatus.COMPENSATION_PENDING,
                BigDecimal.ZERO,
                firstReservationId(reservedItems),
                null,
                List.of(),
                reservedItems,
                "Prix ou quantités de réservation invalides"
            );
        }

        // ÉTAPE 5 : Effectuer le paiement (M4)
        PaymentResponse payment;

        try {
            log.info(
                "Paiement demandé : orderId={}, amount={}",
                orderId,
                totalAmount
            );

            payment = paymentClient.pay(
                new PaymentRequest(orderId, totalAmount)
            );

        } catch (RuntimeException exception) {

            log.error(
                "ERREUR APPEL PAIEMENT : orderId={}",
                orderId,
                exception
            );

            boolean released = releaseReservations(
                reservedItems, orderId
            );

            return result(
                orderId,
                released
                    ? OrderStatus.REJECTED
                    : OrderStatus.COMPENSATION_PENDING,
                totalAmount,
                firstReservationId(reservedItems),
                null,
                List.of(),
                reservedItems,
                "Erreur lors de l'appel au service de paiement"
            );
        }

        if (payment == null
            || !payment.successful()
            || payment.paymentId() == null) {

            log.error("PAIEMENT REFUSÉ : orderId={}", orderId);

            boolean released = releaseReservations(
                reservedItems, orderId
            );

            return result(
                orderId,
                released
                    ? OrderStatus.REJECTED
                    : OrderStatus.COMPENSATION_PENDING,
                totalAmount,
                firstReservationId(reservedItems),
                null,
                List.of(),
                reservedItems,
                "Paiement refusé"
            );
        }

        Long paymentId = payment.paymentId();

        log.info(
            "Paiement accepté : orderId={}, paymentId={}",
            orderId,
            paymentId
        );

        // ÉTAPE 6 : Confirmer chaque réservation auprès de M2
        boolean allConfirmed = true;

        for (SeatReservationResponse item : reservedItems) {

            try {
                log.info(
                    "Confirmation demandée : orderId={}, categoryId={}",
                    orderId,
                    item.categoryId()
                );

                SeatReservationResponse confirmation =
                    eventClient.confirmReservation(
                        item.categoryId(),
                        orderId,
                        new ReservationStatusUpdateRequest(
                            ReservationStatus.CONFIRMED
                        )
                    );

                log.info(
                    "Réponse confirmation : orderId={}, categoryId={}, status={}",
                    orderId,
                    item.categoryId(),
                    confirmation == null
                        ? "NULL"
                        : confirmation.status()
                );

                if (confirmation == null
                    || confirmation.status() != ReservationStatus.CONFIRMED) {

                    log.error(
                        "Confirmation refusée : orderId={}, categoryId={}",
                        orderId,
                        item.categoryId()
                    );

                    allConfirmed = false;
                    break;
                }

            } catch (RuntimeException exception) {

                log.error(
                    "ERREUR CONFIRMATION : orderId={}, categoryId={}",
                    orderId,
                    item.categoryId(),
                    exception
                );

                allConfirmed = false;
                break;
            }
        }

        if (!allConfirmed) {

            log.error(
                "Échec confirmation : lancement compensation, orderId={}",
                orderId
            );

            boolean compensated = compensate(
                reservedItems,
                orderId,
                paymentId
            );

            return result(
                orderId,
                compensated
                    ? OrderStatus.REJECTED
                    : OrderStatus.COMPENSATION_PENDING,
                totalAmount,
                firstReservationId(reservedItems),
                paymentId,
                List.of(),
                reservedItems,
                "Confirmation de réservation échouée"
            );
        }

        // ÉTAPE 7 : Générer les billets auprès de M5
        TicketResponse tickets;

        try {
            log.info(
                "Émission des billets demandée : orderId={}, quantity={}",
                orderId,
                totalQuantity
            );

            tickets = ticketClient.issueTickets(
                new TicketRequest(
                    orderId,
                    request.eventId(),
                    request.userId(),
                    totalQuantity
                )
            );

        } catch (RuntimeException exception) {

            log.error(
                "ERREUR ÉMISSION BILLETS : orderId={}",
                orderId,
                exception
            );

            tickets = null;
        }

        if (tickets == null
            || !tickets.successful()
            || tickets.ticketIds() == null
            || tickets.ticketIds().isEmpty()) {

            log.error(
                "Émission des billets échouée : orderId={}",
                orderId
            );

            boolean compensated = compensate(
                reservedItems,
                orderId,
                paymentId
            );

            return result(
                orderId,
                compensated
                    ? OrderStatus.REJECTED
                    : OrderStatus.COMPENSATION_PENDING,
                totalAmount,
                firstReservationId(reservedItems),
                paymentId,
                List.of(),
                reservedItems,
                "Émission des billets échouée"
            );
        }

        // ÉTAPE 8 : Succès de la Saga
        log.info("Saga réussie : orderId={}", orderId);

        return result(
            orderId,
            OrderStatus.CONFIRMED,
            totalAmount,
            firstReservationId(reservedItems),
            paymentId,
            tickets.ticketIds(),
            reservedItems,
            "Commande confirmée avec succès"
        );
    }

    // Calcul du montant à partir des prix officiels de M2
    private BigDecimal calculateTotal(
        Map<Long, Integer> requestedQuantities,
        List<SeatReservationResponse> reservedItems) {

        if (reservedItems == null || reservedItems.isEmpty()) {
            throw new IllegalStateException(
                "Aucune réservation retournée par Event Service"
            );
        }

        Map<Long, Integer> reservedQuantities = new LinkedHashMap<>();

        BigDecimal total = BigDecimal.ZERO;

        for (SeatReservationResponse item : reservedItems) {

            if (item.categoryId() == null
                || item.quantity() == null
                || item.quantity() <= 0
                || item.unitPrice() == null
                || item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {

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

    // Libérer toutes les réservations
    private boolean releaseReservations(
        List<SeatReservationResponse> reservedItems,
        Long orderId) {

        boolean allReleased = true;

        for (SeatReservationResponse item : reservedItems) {

            try {
                log.info(
                    "Libération réservation : orderId={}, categoryId={}",
                    orderId,
                    item.categoryId()
                );

                eventClient.releaseReservation(
                    item.categoryId(),
                    orderId
                );

            } catch (RuntimeException exception) {

                log.error(
                    "ERREUR LIBÉRATION : orderId={}, categoryId={}",
                    orderId,
                    item.categoryId(),
                    exception
                );

                allReleased = false;
            }
        }

        return allReleased;
    }

    // Rembourser et libérer les réservations
    private boolean compensate(
        List<SeatReservationResponse> reservedItems,
        Long orderId,
        Long paymentId) {

        boolean refundOk;

        try {
            log.info(
                "Remboursement demandé : paymentId={}",
                paymentId
            );

            refundOk = paymentClient.refund(paymentId);

            log.info(
                "Résultat remboursement : paymentId={}, success={}",
                paymentId,
                refundOk
            );

        } catch (RuntimeException exception) {

            log.error(
                "ERREUR REMBOURSEMENT : paymentId={}",
                paymentId,
                exception
            );

            refundOk = false;
        }

        boolean releaseOk = releaseReservations(
            reservedItems,
            orderId
        );

        return refundOk && releaseOk;
    }

    // Conserver le premier ID pour compatibilité avec SagaResult
    private Long firstReservationId(
        List<SeatReservationResponse> items) {

        if (items == null || items.isEmpty()) {
            return null;
        }

        return items.get(0).id();
    }

    // Création du résultat final
    private SagaResult result(
        Long orderId,
        OrderStatus status,
        BigDecimal amount,
        Long reservationId,
        Long paymentId,
        List<String> ticketIds,
        List<SeatReservationResponse> reservedItems,
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
