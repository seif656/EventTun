package tn.esprit.eventtun.order_service.exception;

/** La Saga s'est terminee avec le statut COMPENSATION_PENDING (une compensation reste a resoudre). */
public class OrderCompensationPendingException extends RuntimeException {

    private final Long orderId;

    public OrderCompensationPendingException(Long orderId) {
        super("Order compensation pending");
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}