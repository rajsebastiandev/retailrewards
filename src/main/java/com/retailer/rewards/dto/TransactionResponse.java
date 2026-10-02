package com.retailer.rewards.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A recorded purchase together with the reward points it earned.
 */
public record TransactionResponse(String id, long customerId, String customerName, BigDecimal amount, LocalDate date,
                                  long points) {
}
