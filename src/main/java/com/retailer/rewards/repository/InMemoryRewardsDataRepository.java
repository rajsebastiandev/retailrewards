package com.retailer.rewards.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Repository backed by in-memory collections. It is seeded and validated from a JSON data set at startup;
 * transactions added through the API live until the application stops.
 */
@Repository
public class InMemoryRewardsDataRepository implements RewardsDataRepository {

    /**
     * Transaction ids are "T" followed by a number, e.g. T1001. Ids for new transactions continue
     * the sequence after the highest seed id, so seed and API-created ids share one format.
     */
    static final String ID_PREFIX = "T";
    private static final Pattern ID_PATTERN = Pattern.compile(ID_PREFIX + "(\\d{1,18})");
    private static final long FIRST_ID_NUMBER = 1001;

    private static final Comparator<Transaction> OLDEST_FIRST =
            Comparator.comparing(Transaction::date).thenComparing(Transaction::id);

    private final Map<Long, Customer> customers;
    private final Map<String, Transaction> transactions = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    public InMemoryRewardsDataRepository(@Value("${rewards.data-file}") Resource dataFile, ObjectMapper objectMapper) {
        DataSet dataSet = read(dataFile, objectMapper);
        this.customers = indexCustomers(dataSet.customers());
        dataSet.transactions().forEach(this::addSeedTransaction);
        idSequence.set(transactions.keySet().stream()
                .mapToLong(InMemoryRewardsDataRepository::idNumber)
                .max()
                .orElse(FIRST_ID_NUMBER - 1));
    }

    @Override
    public List<Customer> findAllCustomers() {
        return List.copyOf(customers.values());
    }

    @Override
    public Optional<Customer> findCustomerById(long customerId) {
        return Optional.ofNullable(customers.get(customerId));
    }

    @Override
    public List<Transaction> findAllTransactions() {
        return find(t -> true);
    }

    @Override
    public Optional<Transaction> findTransactionById(String transactionId) {
        return Optional.ofNullable(transactions.get(transactionId));
    }

    @Override
    public List<Transaction> findTransactionsByCustomerId(long customerId) {
        return find(t -> t.customerId() == customerId);
    }

    @Override
    public List<Transaction> findTransactionsBetween(LocalDate from, LocalDate to) {
        return find(t -> !t.date().isBefore(from) && !t.date().isAfter(to));
    }

    @Override
    public Transaction saveTransaction(Transaction transaction) {
        if (transaction.id() != null) {
            throw new IllegalArgumentException("New transactions must not have an id; one is generated");
        }
        validate(transaction);
        while (true) {
            String id = ID_PREFIX + idSequence.incrementAndGet();
            Transaction saved = new Transaction(id, transaction.customerId(), transaction.amount(), transaction.date());
            if (transactions.putIfAbsent(id, saved) == null) {
                return saved;
            }
        }
    }

    private List<Transaction> find(Predicate<Transaction> filter) {
        return transactions.values().stream().filter(filter).sorted(OLDEST_FIRST).toList();
    }

    private void addSeedTransaction(Transaction t) {
        if (t.id() == null || !ID_PATTERN.matcher(t.id()).matches()) {
            throw new IllegalStateException("Seed transaction id must look like T1001, but was " + t.id());
        }
        validate(t);
        if (transactions.putIfAbsent(t.id(), t) != null) {
            throw new IllegalStateException("Missing or duplicate transaction id " + t.id());
        }
    }

    private void validate(Transaction t) {
        if (!customers.containsKey(t.customerId())) {
            throw new IllegalStateException("Transaction " + t.id() + " references unknown customer " + t.customerId());
        }
        if (t.amount() == null || t.amount().signum() < 0) {
            throw new IllegalStateException("Transaction " + t.id() + " has an invalid amount " + t.amount());
        }
        if (t.date() == null) {
            throw new IllegalStateException("Transaction " + t.id() + " has no date");
        }
    }

    private static long idNumber(String id) {
        Matcher matcher = ID_PATTERN.matcher(id);
        return matcher.matches() ? Long.parseLong(matcher.group(1)) : 0;
    }

    private static DataSet read(Resource dataFile, ObjectMapper objectMapper) {
        try (InputStream in = dataFile.getInputStream()) {
            return objectMapper.readValue(in, DataSet.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load rewards data from " + dataFile, e);
        }
    }

    private static Map<Long, Customer> indexCustomers(List<Customer> customers) {
        Map<Long, Customer> byId = new LinkedHashMap<>();
        customers.stream()
                .sorted(Comparator.comparingLong(Customer::id))
                .forEach(c -> {
                    if (byId.putIfAbsent(c.id(), c) != null) {
                        throw new IllegalStateException("Duplicate customer id " + c.id());
                    }
                });
        return byId;
    }

    private record DataSet(List<Customer> customers, List<Transaction> transactions) {
    }
}
