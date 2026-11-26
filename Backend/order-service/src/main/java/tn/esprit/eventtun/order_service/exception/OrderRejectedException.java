
package tn.esprit.eventtun.order_service.exception;

/**
 * La Saga s'est terminée avec le statut REJECTED.
 */
public class OrderRejectedException extends RuntimeException {

    private final Long orderId;

    public OrderRejectedException(Long orderId, String message) {
        super(message);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
