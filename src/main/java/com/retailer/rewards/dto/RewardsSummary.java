package com.retailer.rewards.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Reward points for every customer over the period {@code from} - {@code to} (both inclusive).
 */
public record RewardsSummary(LocalDate from, LocalDate to, List<CustomerRewards> customers) {
}
