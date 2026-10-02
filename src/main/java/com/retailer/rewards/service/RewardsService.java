package com.retailer.rewards.service;

import com.retailer.rewards.dto.CustomerRewards;
import com.retailer.rewards.dto.MonthlyRewards;
import com.retailer.rewards.dto.RewardsSummary;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.RewardsDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates per-transaction reward points into monthly and total points per customer.
 */
@Service
public class RewardsService {

    private final RewardsDataRepository repository;
    private final RewardPointsCalculator calculator;

    public RewardsService(RewardsDataRepository repository, RewardPointsCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    /**
     * Rewards for every customer between {@code from} and {@code to} (both inclusive).
     * A missing bound defaults to the earliest / latest recorded transaction date.
     */
    public RewardsSummary getRewards(LocalDate from, LocalDate to) {
        Period period = resolvePeriod(from, to);
        Map<Long, List<Transaction>> transactionsByCustomer = transactionsIn(period).stream()
                .collect(Collectors.groupingBy(Transaction::customerId));

        List<CustomerRewards> customers = repository.findAllCustomers().stream()
                .map(c -> buildRewards(c, transactionsByCustomer.getOrDefault(c.id(), List.of()), period))
                .toList();
        return new RewardsSummary(period.from(), period.to(), customers);
    }

    /**
     * Rewards for a single customer between {@code from} and {@code to} (both inclusive).
     */
    public CustomerRewards getRewardsForCustomer(long customerId, LocalDate from, LocalDate to) {
        Customer customer = repository.findCustomerById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        Period period = resolvePeriod(from, to);
        List<Transaction> transactions = transactionsIn(period).stream()
                .filter(t -> t.customerId() == customerId)
                .toList();
        return buildRewards(customer, transactions, period);
    }

    private CustomerRewards buildRewards(Customer customer, Collection<Transaction> transactions, Period period) {
        Map<YearMonth, Long> pointsByMonth = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.date()),
                        Collectors.summingLong(t -> calculator.calculatePoints(t.amount()))));

        // Every month in the period is listed, including months without any points.
        List<MonthlyRewards> monthly = period.months().stream()
                .map(month -> new MonthlyRewards(month, pointsByMonth.getOrDefault(month, 0L)))
                .toList();
        long total = monthly.stream().mapToLong(MonthlyRewards::points).sum();
        return new CustomerRewards(customer.id(), customer.name(), monthly, total);
    }

    private List<Transaction> transactionsIn(Period period) {
        return period.isEmpty() ? List.of() : repository.findTransactionsBetween(period.from(), period.to());
    }

    private Period resolvePeriod(LocalDate from, LocalDate to) {
        List<Transaction> all = repository.findAllTransactions();
        LocalDate start = from != null ? from : all.stream().map(Transaction::date).min(LocalDate::compareTo).orElse(null);
        LocalDate end = to != null ? to : all.stream().map(Transaction::date).max(LocalDate::compareTo).orElse(null);

        if (start != null && end != null && start.isAfter(end)) {
            throw new InvalidDateRangeException("'from' (" + start + ") must not be after 'to' (" + end + ")");
        }
        return new Period(start, end);
    }

    /**
     * Inclusive date range; empty (both bounds null) when there is no data to derive a default range from.
     */
    private record Period(LocalDate from, LocalDate to) {

        boolean isEmpty() {
            return from == null || to == null;
        }

        List<YearMonth> months() {
            List<YearMonth> months = new ArrayList<>();
            if (isEmpty()) {
                return months;
            }
            for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
                months.add(m);
            }
            return months;
        }
    }
}
