package com.retailer.rewards.repository;

import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Access to customers and their transactions.
 */
public interface RewardsDataRepository {

    List<Customer> findAllCustomers();

    Optional<Customer> findCustomerById(long customerId);

    /**
     * All transactions, oldest first.
     */
    List<Transaction> findAllTransactions();

    Optional<Transaction> findTransactionById(String transactionId);

    /**
     * Transactions of one customer, oldest first.
     */
    List<Transaction> findTransactionsByCustomerId(long customerId);

    /**
     * Transactions dated between {@code from} and {@code to}, both inclusive, oldest first.
     */
    List<Transaction> findTransactionsBetween(LocalDate from, LocalDate to);

    /**
     * Stores a new transaction. The repository assigns its id, so {@code transaction.id()} must be null.
     *
     * @return the stored transaction, including its generated id
     */
    Transaction saveTransaction(Transaction transaction);
}
