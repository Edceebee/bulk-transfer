package com.interswitch.bulktransaction.service;

import com.interswitch.bulktransaction.dto.request.BulkTransactionRequest;
import com.interswitch.bulktransaction.dto.request.TransactionRequest;
import com.interswitch.bulktransaction.dto.response.BulkTransactionResponse;
import com.interswitch.bulktransaction.dto.response.TransactionResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class BulkTransactionService {

    private final TransactionProcessorService transactionProcessorService;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Map<String, BulkTransactionResponse> batchResults = new ConcurrentHashMap<>();
    private final Set<String> processedBatchIds = ConcurrentHashMap.newKeySet();

    public BulkTransactionService(TransactionProcessorService transactionProcessorService,
                                  MeterRegistry meterRegistry) {
        this.transactionProcessorService = transactionProcessorService;

        // Initialize metrics counters
        this.successCounter = Counter.builder("transactions.success")
                .description("Number of successful transactions")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("transactions.failure")
                .description("Number of failed transactions")
                .register(meterRegistry);
    }

    public BulkTransactionResponse processBulkTransactions(BulkTransactionRequest request) {
        log.info("STARTING bulk transaction processing for batchId: {}", request.getBatchId());

        // Check for duplicate batch ID
        if (!processedBatchIds.add(request.getBatchId())) {
            log.warn("IDEMPOTENCY: BatchId {} already processed. Returning previous results.", request.getBatchId());
            return batchResults.get(request.getBatchId());
        }

        List<TransactionResult> results = new ArrayList<>();

        for (TransactionRequest transaction : request.getTransactions()) {
            log.info("PROCESSING transactionId: {} for batchId: {}",
                    transaction.getTransactionId(), request.getBatchId());

            try {
                // Call the external service - Spring AOP can intercept this!
                TransactionResult result = transactionProcessorService.processTransaction(transaction);
                results.add(result);

                if ("SUCCESS".equals(result.getStatus())) {
                    successCounter.increment();
                    log.info("SUCCESS transactionId: {}", transaction.getTransactionId());
                } else {
                    failureCounter.increment();
                    log.info("FAILED transactionId: {} - Reason: {}",
                            transaction.getTransactionId(), result.getReason());
                }
            } catch (Exception e) {
                log.error("UNEXPECTED ERROR processing transactionId: {} in batchId: {} - Error: {}",
                        transaction.getTransactionId(), request.getBatchId(), e.getMessage());

                TransactionResult failedResult = TransactionResult.builder()
                        .transactionId(transaction.getTransactionId())
                        .status("FAILED")
                        .reason("Unexpected error: " + e.getMessage())
                        .build();
                results.add(failedResult);
                failureCounter.increment();
            }
        }

        BulkTransactionResponse response = BulkTransactionResponse.builder()
                .batchId(request.getBatchId())
                .results(results)
                .build();
        batchResults.put(request.getBatchId(), response);

        log.info("COMPLETED bulk transaction processing for batchId: {}", request.getBatchId());
        return response;
    }

    public BulkTransactionResponse getBatchResults(String batchId) {
        log.info("Retrieving results for batchId: {}", batchId);
        BulkTransactionResponse response = batchResults.get(batchId);
        if (response == null) {
            throw new RuntimeException("Batch not found: " + batchId);
        }
        return response;
    }
}