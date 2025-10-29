package com.interswitch.bulktransaction;

import com.interswitch.bulktransaction.dto.request.BulkTransactionRequest;
import com.interswitch.bulktransaction.dto.request.TransactionRequest;
import com.interswitch.bulktransaction.dto.response.BulkTransactionResponse;
import com.interswitch.bulktransaction.dto.response.TransactionResult;
import com.interswitch.bulktransaction.service.BulkTransactionService;
import com.interswitch.bulktransaction.service.TransactionProcessorService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkTransactionServiceTest {

    @Mock
    private TransactionProcessorService transactionProcessorService;

    private MeterRegistry meterRegistry;
    private BulkTransactionService bulkTransactionService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        bulkTransactionService = new BulkTransactionService(transactionProcessorService, meterRegistry);
    }

    @Test
    void processBulkTransactions_SuccessfulProcessing_ReturnsResults() {
        // Arrange
        BulkTransactionRequest request = createBulkTransactionRequest();

        TransactionResult successResult = TransactionResult.builder()
                .transactionId("TXN-001")
                .status("SUCCESS")
                .build();

        TransactionResult failedResult = TransactionResult.builder()
                .transactionId("TXN-002")
                .status("FAILED")
                .reason("All retry attempts failed")
                .build();

        when(transactionProcessorService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(successResult)
                .thenReturn(failedResult);

        // Act
        BulkTransactionResponse response = bulkTransactionService.processBulkTransactions(request);

        // Assert
        assertNotNull(response);
        assertEquals("BATCH-001", response.getBatchId());
        assertEquals(2, response.getResults().size());

        assertEquals("SUCCESS", response.getResults().get(0).getStatus());
        assertEquals("FAILED", response.getResults().get(1).getStatus());

        verify(transactionProcessorService, times(2)).processTransaction(any(TransactionRequest.class));
    }

    @Test
    void processBulkTransactions_WithException_HandlesGracefully() {
        // Arrange
        BulkTransactionRequest request = createBulkTransactionRequest();

        when(transactionProcessorService.processTransaction(any(TransactionRequest.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        BulkTransactionResponse response = bulkTransactionService.processBulkTransactions(request);

        // Assert
        assertNotNull(response);
        assertEquals("BATCH-001", response.getBatchId());
        assertEquals(2, response.getResults().size());

        // Both should be failed due to exception
        assertEquals("FAILED", response.getResults().get(0).getStatus());
        assertEquals("FAILED", response.getResults().get(1).getStatus());
        assertTrue(response.getResults().get(0).getReason().contains("Unexpected error"));
    }

    @Test
    void processBulkTransactions_DuplicateBatchId_ReturnsCachedResults() {
        // Arrange
        BulkTransactionRequest request = createBulkTransactionRequest();
        BulkTransactionResponse cachedResponse = BulkTransactionResponse.builder()
                .batchId("BATCH-001")
                .results(List.of())
                .build();

        // First call - process normally
        when(transactionProcessorService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(TransactionResult.builder().status("SUCCESS").build());

        bulkTransactionService.processBulkTransactions(request);

        // Second call - should return cached results
        BulkTransactionResponse response = bulkTransactionService.processBulkTransactions(request);

        // Assert
        assertNotNull(response);
        // Verify that processor was only called once (first time)
        verify(transactionProcessorService, times(2)).processTransaction(any(TransactionRequest.class));
    }

    @Test
    void processBulkTransactions_IncrementsMetricsCounters() {
        // Arrange
        BulkTransactionRequest request = createBulkTransactionRequest();

        TransactionResult successResult = TransactionResult.builder()
                .transactionId("TXN-001")
                .status("SUCCESS")
                .build();

        TransactionResult failedResult = TransactionResult.builder()
                .transactionId("TXN-002")
                .status("FAILED")
                .reason("Failed")
                .build();

        when(transactionProcessorService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(successResult)
                .thenReturn(failedResult);

        // Act
        bulkTransactionService.processBulkTransactions(request);

        // Assert
        Counter successCounter = meterRegistry.find("transactions.success").counter();
        Counter failureCounter = meterRegistry.find("transactions.failure").counter();

        assertNotNull(successCounter);
        assertNotNull(failureCounter);
        assertEquals(1.0, successCounter.count());
        assertEquals(1.0, failureCounter.count());
    }

    @Test
    void getBatchResults_ExistingBatch_ReturnsResults() {
        // Arrange
        BulkTransactionRequest request = createBulkTransactionRequest();
        when(transactionProcessorService.processTransaction(any(TransactionRequest.class)))
                .thenReturn(TransactionResult.builder().status("SUCCESS").build());

        bulkTransactionService.processBulkTransactions(request);

        // Act
        BulkTransactionResponse response = bulkTransactionService.getBatchResults("BATCH-001");

        // Assert
        assertNotNull(response);
        assertEquals("BATCH-001", response.getBatchId());
    }

    @Test
    void getBatchResults_NonExistentBatch_ThrowsException() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bulkTransactionService.getBatchResults("NON-EXISTENT"));

        assertEquals("Batch not found: NON-EXISTENT", exception.getMessage());
    }

    private BulkTransactionRequest createBulkTransactionRequest() {
        TransactionRequest tx1 = TransactionRequest.builder()
                .transactionId("TXN-001")
                .fromAccount("123456")
                .toAccount("654321")
                .amount(new BigDecimal("1000.00"))
                .build();

        TransactionRequest tx2 = TransactionRequest.builder()
                .transactionId("TXN-002")
                .fromAccount("123456")
                .toAccount("987654")
                .amount(new BigDecimal("500.00"))
                .build();

        return BulkTransactionRequest.builder()
                .batchId("BATCH-001")
                .transactions(List.of(tx1, tx2))
                .build();
    }
}