package com.retailer.rewards.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the REST API against the seed data set in {@code data/transactions.json}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RewardsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsMonthlyAndTotalPointsForAllCustomers() throws Exception {
        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-07-03"))
                .andExpect(jsonPath("$.to").value("2026-09-30"))
                .andExpect(jsonPath("$.customers", hasSize(5)))
                .andExpect(jsonPath("$.customers[*].customerName")
                        .value(contains("Alice Johnson", "Bob Smith", "Carol Davis", "David Lee", "Eva Martinez")))
                .andExpect(jsonPath("$.customers[*].totalPoints").value(contains(525, 652, 865, 220, 0)))
                // Bob: Jul $50 + $51 -> 1, Aug $99.99 -> 49, Sep $101 + $350.75 -> 52 + 550
                .andExpect(jsonPath("$.customers[1].monthlyRewards[*].month").value(contains("2026-07", "2026-08", "2026-09")))
                .andExpect(jsonPath("$.customers[1].monthlyRewards[*].points").value(contains(1, 49, 602)));
    }

    @Test
    void returnsRewardsForSingleCustomer() throws Exception {
        mockMvc.perform(get("/api/rewards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$.monthlyRewards[*].points").value(contains(115, 250, 160)))
                .andExpect(jsonPath("$.totalPoints").value(525));
    }

    @Test
    void listsMonthsWithoutPurchasesAsZero() throws Exception {
        mockMvc.perform(get("/api/rewards/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRewards[*].points").value(contains(0, 220, 0)))
                .andExpect(jsonPath("$.totalPoints").value(220));
    }

    @Test
    void filtersByDateRange() throws Exception {
        mockMvc.perform(get("/api/rewards/3").param("from", "2026-08-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRewards[*].month").value(contains("2026-08", "2026-09")))
                .andExpect(jsonPath("$.monthlyRewards[*].points").value(contains(0, 15)))
                .andExpect(jsonPath("$.totalPoints").value(15));
    }

    @Test
    void unknownCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/rewards/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"))
                .andExpect(jsonPath("$.detail").value("Customer 999 not found"));
    }

    @Test
    void invertedDateRangeReturns400() throws Exception {
        mockMvc.perform(get("/api/rewards").param("from", "2026-09-01").param("to", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid date range"));
    }

    @Test
    void malformedDateReturns400() throws Exception {
        mockMvc.perform(get("/api/rewards").param("from", "07/01/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request parameter"));
    }

    @Test
    void nonNumericCustomerIdReturns400() throws Exception {
        mockMvc.perform(get("/api/rewards/abc"))
                .andExpect(status().isBadRequest());
    }
}
