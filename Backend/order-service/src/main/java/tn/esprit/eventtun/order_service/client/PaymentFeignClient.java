
package tn.esprit.eventtun.order_service.client;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
    name = "payment-service",
    url = "${services.payment.url}"
)
public interface PaymentFeignClient {

    @PostMapping("/api/payments")
    PaymentData createPayment(@RequestBody PaymentRequest request);

    @GetMapping("/api/payments/{id}")
    PaymentData getPayment(@PathVariable("id") Long id);

    @GetMapping("/api/payments")
    List<PaymentData> getPaymentsByOrderId(
        @RequestParam("orderId") Long orderId
    );

    @PostMapping("/api/payments/{id}/refunds")
    RefundData refundPayment(
        @PathVariable("id") Long id,
        @RequestBody RefundRequest request
    );

    record PaymentRequest(
        Long orderId,
        BigDecimal amount,
        String method
    ) {}

    record PaymentData(
        Long id,
        Long orderId,
        BigDecimal amount,
        String method,
        String status
    ) {}

    record RefundRequest(
        BigDecimal amount,
        String reason
    ) {}

    record RefundData(
        Long id,
        Long paymentId,
        BigDecimal amount,
        String reason
    ) {}
}
