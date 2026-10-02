package com.retailer.rewards.controller;

import com.retailer.rewards.dto.CreateTransactionRequest;
import com.retailer.rewards.dto.TransactionResponse;
import com.retailer.rewards.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Records purchases and lists them. New purchases are included in {@code /api/rewards} immediately.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse created = transactionService.createTransaction(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<TransactionResponse> getTransactions(@RequestParam(required = false) Long customerId) {
        return transactionService.getTransactions(customerId);
    }

    @GetMapping("/{transactionId}")
    public TransactionResponse getTransaction(@PathVariable String transactionId) {
        return transactionService.getTransaction(transactionId);
    }
}
