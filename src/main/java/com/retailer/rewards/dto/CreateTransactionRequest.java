package com.retailer.rewards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for recording a new purchase, e.g. {"customerId": 1, "amount": 120.00, "date": "2026-09-20"}.
 */
public record CreateTransactionRequest(

        @NotNull(message = "is required")
        @Positive(message = "must be a positive number")
        Long customerId,

        @NotNull(message = "is required")
        @DecimalMin(value = "0.01", message = "must be at least 0.01")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 digits and 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "is required")
        @PastOrPresent(message = "must not be in the future")
        LocalDate date) {
}
