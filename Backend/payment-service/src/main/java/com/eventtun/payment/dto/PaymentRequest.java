package com.eventtun.payment.dto;

import com.eventtun.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Corps de POST /api/payments (envoyé par Order Service). */
public record PaymentRequest(
        @NotNull Long orderId,
        @NotNull @Positive BigDecimal amount,
        @NotNull PaymentMethod method
) {
}
