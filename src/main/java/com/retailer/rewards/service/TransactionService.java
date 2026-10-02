package com.retailer.rewards.service;

import com.retailer.rewards.dto.CreateTransactionRequest;
import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.TransactionNotFoundException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.RewardsDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Records and looks up purchases. Rewards are always calculated from the current transactions,
 * so a newly recorded purchase shows up in the rewards endpoints straight away.
 */
@Service
public class TransactionService {

    private final RewardsDataRepository repository;
    private final RewardPointsCalculator calculator;

    public TransactionService(RewardsDataRepository repository, RewardPointsCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Customer customer = findCustomer(request.customerId());
        Transaction saved = repository.saveTransaction(
                new Transaction(null, customer.id(), request.amount(), request.date()));
        return toResponse(saved, customer);
    }

    public TransactionResponse getTransaction(String transactionId) {
        Transaction transaction = repository.findTransactionById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        return toResponse(transaction, findCustomer(transaction.customerId()));
    }

    /**
     * All transactions, or only those of {@code customerId} when it is not null.
     */
    public List<TransactionResponse> getTransactions(Long customerId) {
        if (customerId == null) {
            return repository.findAllTransactions().stream()
                    .map(t -> toResponse(t, findCustomer(t.customerId())))
                    .toList();
        }
        Customer customer = findCustomer(customerId);
        return repository.findTransactionsByCustomerId(customerId).stream()
                .map(t -> toResponse(t, customer))
                .toList();
    }

    private Customer findCustomer(long customerId) {
        return repository.findCustomerById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private TransactionResponse toResponse(Transaction t, Customer customer) {
        return new TransactionResponse(t.id(), customer.id(), customer.name(), t.amount(), t.date(),
                calculator.calculatePoints(t.amount()));
    }
}
