package com.retailer.rewards.service;

import com.retailer.rewards.dto.CustomerRewards;
import com.retailer.rewards.dto.MonthlyRewards;
import com.retailer.rewards.dto.RewardsSummary;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.RewardsDataRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class RewardsServiceTest {

    private static final Customer ALICE = new Customer(1, "Alice");
    private static final Customer BOB = new Customer(2, "Bob");

    private final List<Transaction> transactions = List.of(
            tx("A1", 1, "120.00", "2026-01-10"),  // 90
            tx("A2", 1, "60.00", "2026-01-25"),   // 10
            tx("A3", 1, "200.00", "2026-03-05"),  // 250
            tx("B1", 2, "75.00", "2026-02-14")    // 25
    );

    private final RewardsService service = new RewardsService(
            new StubRepository(List.of(ALICE, BOB), transactions), new RewardPointsCalculator());

    @Test
    void sumsPointsPerMonthAndIncludesMonthsWithoutPurchases() {
        CustomerRewards alice = service.getRewardsForCustomer(1, null, null);

        assertThat(alice.monthlyRewards()).containsExactly(
                new MonthlyRewards(YearMonth.of(2026, 1), 100),
                new MonthlyRewards(YearMonth.of(2026, 2), 0),
                new MonthlyRewards(YearMonth.of(2026, 3), 250));
        assertThat(alice.totalPoints()).isEqualTo(350);
    }

    @Test
    void defaultPeriodSpansAllRecordedTransactions() {
        RewardsSummary summary = service.getRewards(null, null);

        assertThat(summary.from()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(summary.to()).isEqualTo(LocalDate.of(2026, 3, 5));
        assertThat(summary.customers())
                .extracting(CustomerRewards::customerName, CustomerRewards::totalPoints)
                .containsExactly(
                        tuple("Alice", 350L),
                        tuple("Bob", 25L));
    }

    @Test
    void onlyCountsTransactionsInsideRequestedRange() {
        CustomerRewards alice = service.getRewardsForCustomer(1, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 2, 28));

        assertThat(alice.monthlyRewards()).containsExactly(
                new MonthlyRewards(YearMonth.of(2026, 1), 10),
                new MonthlyRewards(YearMonth.of(2026, 2), 0));
        assertThat(alice.totalPoints()).isEqualTo(10);
    }

    @Test
    void customerWithoutTransactionsInRangeEarnsZero() {
        CustomerRewards bob = service.getRewardsForCustomer(2, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(bob.monthlyRewards()).containsExactly(new MonthlyRewards(YearMonth.of(2026, 3), 0));
        assertThat(bob.totalPoints()).isZero();
    }

    @Test
    void unknownCustomerIsRejected() {
        assertThatThrownBy(() -> service.getRewardsForCustomer(99, null, null))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void fromAfterToIsRejected() {
        assertThatThrownBy(() -> service.getRewards(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void emptyDataSetProducesEmptySummary() {
        RewardsService emptyService = new RewardsService(
                new StubRepository(List.of(ALICE), List.of()), new RewardPointsCalculator());

        RewardsSummary summary = emptyService.getRewards(null, null);

        assertThat(summary.customers()).singleElement().satisfies(c -> {
            assertThat(c.monthlyRewards()).isEmpty();
            assertThat(c.totalPoints()).isZero();
        });
    }

    private static Transaction tx(String id, long customerId, String amount, String date) {
        return new Transaction(id, customerId, new BigDecimal(amount), LocalDate.parse(date));
    }

    private record StubRepository(List<Customer> customers, List<Transaction> transactions) implements RewardsDataRepository {

        @Override
        public List<Customer> findAllCustomers() {
            return customers;
        }

        @Override
        public Optional<Customer> findCustomerById(long customerId) {
            return customers.stream().filter(c -> c.id() == customerId).findFirst();
        }

        @Override
        public List<Transaction> findAllTransactions() {
            return transactions;
        }

        @Override
        public Optional<Transaction> findTransactionById(String transactionId) {
            return transactions.stream().filter(t -> t.id().equals(transactionId)).findFirst();
        }

        @Override
        public List<Transaction> findTransactionsByCustomerId(long customerId) {
            return transactions.stream().filter(t -> t.customerId() == customerId).toList();
        }

        @Override
        public List<Transaction> findTransactionsBetween(LocalDate from, LocalDate to) {
            return transactions.stream()
                    .filter(t -> !t.date().isBefore(from) && !t.date().isAfter(to))
                    .toList();
        }

        @Override
        public Transaction saveTransaction(Transaction transaction) {
            throw new UnsupportedOperationException("Not needed by RewardsService");
        }
    }
}
