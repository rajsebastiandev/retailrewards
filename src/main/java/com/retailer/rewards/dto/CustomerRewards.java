package com.retailer.rewards.dto;

import java.util.List;

/**
 * Reward points for one customer, broken down by month, plus the total for the period.
 */
public record CustomerRewards(long customerId, String customerName, List<MonthlyRewards> monthlyRewards, long totalPoints) {
}
