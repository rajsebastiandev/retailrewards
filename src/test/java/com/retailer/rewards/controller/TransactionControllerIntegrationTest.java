package com.retailer.rewards.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of {@code /api/transactions}. These tests add transactions to the shared in-memory store,
 * so assertions on rewards are relative (before vs after) and the context is discarded afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void newTransactionIsIncludedInRewards() throws Exception {
        long septemberBefore = septemberPoints(5);
        long totalBefore = totalPoints(5);

        createTransaction(5, "120.00", "2026-09-20")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", matchesPattern("T\\d+")))
                .andExpect(header().string("Location", startsWith("http://localhost/api/transactions/T")))
                .andExpect(jsonPath("$.customerId").value(5))
                .andExpect(jsonPath("$.customerName").value("Eva Martinez"))
                .andExpect(jsonPath("$.amount").value(120.00))
                .andExpect(jsonPath("$.date").value("2026-09-20"))
                .andExpect(jsonPath("$.points").value(90));

        assertThat(septemberPoints(5)).isEqualTo(septemberBefore + 90);
        assertThat(totalPoints(5)).isEqualTo(totalBefore + 90);
    }

    @Test
    void createdTransactionCanBeFetchedAndListed() throws Exception {
        String body = createTransaction(4, "75.00", "2026-08-30")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.points").value(25));

        mockMvc.perform(get("/api/transactions").param("customerId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)))
                .andExpect(jsonPath("$[*].customerId", everyItem(is(4))));
    }

    @Test
    void listsSeedTransactionsWithPoints() throws Exception {
        mockMvc.perform(get("/api/transactions").param("customerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("T1001"))
                .andExpect(jsonPath("$[0].amount").value(120.00))
                .andExpect(jsonPath("$[0].points").value(90));
    }

    @Test
    void unknownCustomerReturns404() throws Exception {
        createTransaction(999, "50.00", "2026-09-01")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer 999 not found"));
    }

    @Test
    void unknownTransactionReturns404() throws Exception {
        mockMvc.perform(get("/api/transactions/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Transaction not found"));
    }

    @Test
    void listingForUnknownCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/transactions").param("customerId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFieldsReturn400WithFieldErrors() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "amount": -10, "date": "%s"}""".formatted(tomorrow)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.amount").value("must be at least 0.01"))
                .andExpect(jsonPath("$.errors.date").value("must not be in the future"));
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.*", hasSize(3)));
    }

    @Test
    void tooManyDecimalPlacesReturns400() throws Exception {
        createTransaction(1, "10.999", "2026-09-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId": 1, "amount": 10, "date": "09/01/2026"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request body"));
    }

    private ResultActions createTransaction(long customerId, String amount, String date) throws Exception {
        return mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerId": %d, "amount": %s, "date": "%s"}""".formatted(customerId, amount, date)));
    }

    private long septemberPoints(long customerId) throws Exception {
        String body = mockMvc.perform(get("/api/rewards/" + customerId)).andReturn().getResponse().getContentAsString();
        List<Number> points = JsonPath.read(body, "$.monthlyRewards[?(@.month == '2026-09')].points");
        return points.get(0).longValue();
    }

    private long totalPoints(long customerId) throws Exception {
        String body = mockMvc.perform(get("/api/rewards/" + customerId)).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.totalPoints")).longValue();
    }
}
