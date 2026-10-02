package com.retailer.rewards.dto;

import java.time.YearMonth;

/**
 * Points earned by a customer in one calendar month, serialized as e.g. {"month": "2026-07", "points": 115}.
 */
public record MonthlyRewards(YearMonth month, long points) {
}
