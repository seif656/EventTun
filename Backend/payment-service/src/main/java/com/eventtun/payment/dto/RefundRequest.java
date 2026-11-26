package com.eventtun.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Corps de POST /api/payments/{id}/refunds. */
public record RefundRequest(
        @NotNull @Positive BigDecimal amount,
        @Size(max = 255) String reason
) {
}
