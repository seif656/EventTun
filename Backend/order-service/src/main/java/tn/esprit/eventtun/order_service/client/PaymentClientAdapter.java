
package tn.esprit.eventtun.order_service.client;

import java.util.List;

import org.springframework.stereotype.Component;

import feign.FeignException;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.PaymentRequest;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.PaymentResponse;

@Component
public class PaymentClientAdapter implements PaymentClient {

    private final PaymentFeignClient paymentFeignClient;

    public PaymentClientAdapter(PaymentFeignClient paymentFeignClient) {
        this.paymentFeignClient = paymentFeignClient;
    }

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        PaymentFeignClient.PaymentRequest paymentRequest =
            new PaymentFeignClient.PaymentRequest(
                request.orderId(),
                request.amount(),
                "CARD"
            );

        try {
            PaymentFeignClient.PaymentData payment =
                paymentFeignClient.createPayment(paymentRequest);

            return new PaymentResponse(
                payment.id(),
                "SUCCESS".equalsIgnoreCase(payment.status())
            );

        } catch (FeignException.Conflict exception) {
            // M4 peut retourner 409 lorsqu'un paiement est refusé.
            List<PaymentFeignClient.PaymentData> payments =
                paymentFeignClient.getPaymentsByOrderId(request.orderId());

            if (payments != null && !payments.isEmpty()) {
                PaymentFeignClient.PaymentData payment = payments.get(0);

                return new PaymentResponse(
                    payment.id(),
                    "SUCCESS".equalsIgnoreCase(payment.status())
                );
            }

            return new PaymentResponse(null, false);
        }
    }

    @Override
    public boolean refund(Long paymentId) {
        PaymentFeignClient.PaymentData payment =
            paymentFeignClient.getPayment(paymentId);

        if (payment == null || payment.amount() == null) {
            return false;
        }

        paymentFeignClient.refundPayment(
            paymentId,
            new PaymentFeignClient.RefundRequest(
                payment.amount(),
                "Échec de la Saga EventTun"
            )
        );

        return true;
    }
}
