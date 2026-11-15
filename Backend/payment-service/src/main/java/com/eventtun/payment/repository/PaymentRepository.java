package com.eventtun.payment.repository;

import com.eventtun.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Utilisé par Order après un timeout : GET /api/payments?orderId= */
    Optional<Payment> findByOrderId(Long orderId);
}
