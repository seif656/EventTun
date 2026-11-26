
package tn.esprit.eventtun.order_service.client;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.PaymentRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.PaymentResponse;

public interface PaymentClient {

    PaymentResponse pay(PaymentRequest request);

    boolean refund(Long paymentId);
}
