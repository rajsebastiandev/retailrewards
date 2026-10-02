package com.retailer.rewards.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates the reward points earned by a single transaction:
 * <ul>
 *     <li>2 points for every dollar spent over $100, plus</li>
 *     <li>1 point for every dollar spent between $50 and $100.</li>
 * </ul>
 * Only whole dollars count, so cents are dropped before calculating (e.g. $120.99 earns the same as $120).
 */
@Component
public class RewardPointsCalculator {

    static final long LOWER_THRESHOLD = 50;
    static final long UPPER_THRESHOLD = 100;

    public long calculatePoints(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("Transaction amount must be zero or positive, but was " + amount);
        }
        long dollars = amount.setScale(0, RoundingMode.DOWN).longValueExact();

        long points = 0;
        if (dollars > UPPER_THRESHOLD) {
            points += 2 * (dollars - UPPER_THRESHOLD);
        }
        if (dollars > LOWER_THRESHOLD) {
            points += Math.min(dollars, UPPER_THRESHOLD) - LOWER_THRESHOLD;
        }
        return points;
    }
}
