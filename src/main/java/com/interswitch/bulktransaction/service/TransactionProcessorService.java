package com.interswitch.bulktransaction.service;

import com.interswitch.bulktransaction.client.TransactionServiceClient;
import com.interswitch.bulktransaction.dto.request.TransactionRequest;
import com.interswitch.bulktransaction.dto.request.TransactionServiceRequest;
import com.interswitch.bulktransaction.dto.response.TransactionResult;
import com.interswitch.bulktransaction.dto.response.TransactionServiceResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionProcessorService {

    private final TransactionServiceClient transactionServiceClient;

    @Retry(name = "transactionService", fallbackMethod = "retryFallback")
    public TransactionResult processTransaction(TransactionRequest transaction) {
        log.info("ATTEMPTING processTransaction for: {}", transaction.getTransactionId());

        TransactionServiceRequest serviceRequest = TransactionServiceRequest.builder()
                .transactionId(transaction.getTransactionId())
                .fromAccount(transaction.getFromAccount())
                .toAccount(transaction.getToAccount())
                .amount(transaction.getAmount())
                .build();

        try {
            TransactionServiceResponse serviceResponse =
                    transactionServiceClient.processTransaction(serviceRequest);

            log.info("SUCCESS processTransaction for: {}", transaction.getTransactionId());

            return TransactionResult.builder()
                    .transactionId(transaction.getTransactionId())
                    .status("SUCCESS")
                    .build();
        } catch (Exception e) {
            log.info("FAILED processTransaction for: {} - Error: {}",
                    transaction.getTransactionId(), e.getMessage());
            throw new RuntimeException("Transaction processing failed", e);
        }
    }

    public TransactionResult retryFallback(TransactionRequest transaction, Exception e) {
        log.warn("RETRY FALLBACK - All retry attempts failed for transactionId: {} - Final Error: {}",
                transaction.getTransactionId(), e.getMessage());
        return TransactionResult.builder()
                .transactionId(transaction.getTransactionId())
                .status("FAILED")
                .reason("All retry attempts failed: " + e.getMessage())
                .build();
    }
}