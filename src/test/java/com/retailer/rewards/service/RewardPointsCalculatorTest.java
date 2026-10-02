package com.retailer.rewards.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RewardPointsCalculatorTest {

    private final RewardPointsCalculator calculator = new RewardPointsCalculator();

    @ParameterizedTest(name = "${0} -> {1} points")
    @CsvSource({
            "0,       0",
            "10,      0",
            "49.99,   0",
            "50,      0",    // lower threshold: nothing over $50 yet
            "50.99,   0",    // partial dollars do not count
            "51,      1",
            "75.50,   25",
            "99.99,   49",
            "100,     50",   // upper threshold: full 50 from the $50-$100 band
            "100.99,  50",
            "101,     52",
            "120,     90",   // example from the specification: 2x20 + 1x50
            "120.75,  90",
            "200,     250",
            "500,     850",
            "1000000, 1999850"
    })
    void calculatesPointsPerTransaction(BigDecimal amount, long expectedPoints) {
        assertThat(calculator.calculatePoints(amount)).isEqualTo(expectedPoints);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> calculator.calculatePoints(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> calculator.calculatePoints(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
