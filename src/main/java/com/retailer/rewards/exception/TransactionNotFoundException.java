package com.retailer.rewards.exception;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String transactionId) {
        super("Transaction " + transactionId + " not found");
    }
}
