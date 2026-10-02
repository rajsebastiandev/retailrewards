package com.retailer.rewards.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single recorded purchase.
 */
public record Transaction(String id, long customerId, BigDecimal amount, LocalDate date) {
}
