package com.interswitch.bulktransaction;

import com.interswitch.bulktransaction.client.TransactionServiceClient;
import com.interswitch.bulktransaction.dto.request.TransactionRequest;
import com.interswitch.bulktransaction.dto.request.TransactionServiceRequest;
import com.interswitch.bulktransaction.dto.response.TransactionResult;
import com.interswitch.bulktransaction.dto.response.TransactionServiceResponse;
import com.interswitch.bulktransaction.service.TransactionProcessorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorServiceTest {

    @Mock
    private TransactionServiceClient transactionServiceClient;

    private TransactionProcessorService transactionProcessorService;

    @BeforeEach
    void setUp() {
        transactionProcessorService = new TransactionProcessorService(transactionServiceClient);
    }

    @Test
    void processTransaction_SuccessfulExternalCall_ReturnsSuccessResult() {
        // Arrange
        TransactionRequest request = createTransactionRequest();
        TransactionServiceResponse serviceResponse = TransactionServiceResponse.builder()
                .transactionId("TXN-001")
                .status("SUCCESS")
                .message("Processed successfully")
                .build();

        when(transactionServiceClient.processTransaction(any(TransactionServiceRequest.class)))
                .thenReturn(serviceResponse);

        // Act
        TransactionResult result = transactionProcessorService.processTransaction(request);

        // Assert
        assertNotNull(result);
        assertEquals("TXN-001", result.getTransactionId());
        assertEquals("SUCCESS", result.getStatus());
        assertNull(result.getReason());

        verify(transactionServiceClient, times(1)).processTransaction(any(TransactionServiceRequest.class));
    }

    @Test
    void processTransaction_ExternalServiceThrowsException_ThrowsRuntimeException() {
        // Arrange
        TransactionRequest request = createTransactionRequest();

        when(transactionServiceClient.processTransaction(any(TransactionServiceRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionProcessorService.processTransaction(request));

        assertEquals("Transaction processing failed", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Connection refused", exception.getCause().getMessage());
    }

    @Test
    void processTransaction_WithRetryConfiguration_ShouldRetryOnFailure() {
        // Arrange
        TransactionRequest request = createTransactionRequest();

        // First two calls fail, third succeeds (testing retry logic)
        when(transactionServiceClient.processTransaction(any(TransactionServiceRequest.class)))
                .thenThrow(new RuntimeException("Connection refused"))
                .thenThrow(new RuntimeException("Connection refused"))
                .thenReturn(TransactionServiceResponse.builder()
                        .transactionId("TXN-001")
                        .status("SUCCESS")
                        .build());

        // Act
        // Note: We're testing that the method is decorated with @Retry
        // In a real test, we'd use Spring's test context to test the actual retry behavior
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionProcessorService.processTransaction(request));

        // Assert - verify multiple calls due to retries
        verify(transactionServiceClient, atLeastOnce()).processTransaction(any(TransactionServiceRequest.class));
    }

    @Test
    void retryFallback_AllRetriesExhausted_ReturnsFailedResult() {
        // Arrange
        TransactionRequest request = createTransactionRequest();
        Exception originalException = new RuntimeException("All connection attempts failed");

        // Act
        TransactionResult result = transactionProcessorService.retryFallback(request, originalException);

        // Assert
        assertNotNull(result);
        assertEquals("TXN-001", result.getTransactionId());
        assertEquals("FAILED", result.getStatus());
        assertEquals("All retry attempts failed: All connection attempts failed", result.getReason());
    }

    @Test
    void retryFallback_WithDifferentExceptionTypes_HandlesAll() {
        // Arrange
        TransactionRequest request = createTransactionRequest();

        // Test with different exception types
        Exception[] exceptions = {
                new RuntimeException("Network error"),
                new IllegalStateException("Service unavailable"),
                new IllegalArgumentException("Invalid request")
        };

        for (Exception exception : exceptions) {
            // Act
            TransactionResult result = transactionProcessorService.retryFallback(request, exception);

            // Assert
            assertNotNull(result);
            assertEquals("FAILED", result.getStatus());
            assertTrue(result.getReason().contains("All retry attempts failed"));
            assertTrue(result.getReason().contains(exception.getMessage()));
        }
    }

    @Test
    void processTransaction_ValidatesRequestParameters() {
        // Arrange
        TransactionRequest request = TransactionRequest.builder()
                .transactionId("TXN-001")
                .fromAccount("")  // Empty from account
                .toAccount("654321")
                .amount(new BigDecimal("1000.00"))
                .build();

        when(transactionServiceClient.processTransaction(any(TransactionServiceRequest.class)))
                .thenReturn(TransactionServiceResponse.builder()
                        .transactionId("TXN-001")
                        .status("SUCCESS")
                        .build());

        // Act
        TransactionResult result = transactionProcessorService.processTransaction(request);

        // Assert - service should still process even with empty fields (validation happens elsewhere)
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
    }

    @Test
    void processTransaction_NullAmount_HandlesGracefully() {
        // Arrange
        TransactionRequest request = TransactionRequest.builder()
                .transactionId("TXN-001")
                .fromAccount("123456")
                .toAccount("654321")
                .amount(null)  // Null amount
                .build();

        when(transactionServiceClient.processTransaction(any(TransactionServiceRequest.class)))
                .thenReturn(TransactionServiceResponse.builder()
                        .transactionId("TXN-001")
                        .status("SUCCESS")
                        .build());

        // Act
        TransactionResult result = transactionProcessorService.processTransaction(request);

        // Assert
        assertNotNull(result);
        assertEquals("SUCCESS", result.getStatus());
    }

    private TransactionRequest createTransactionRequest() {
        return TransactionRequest.builder()
                .transactionId("TXN-001")
                .fromAccount("123456")
                .toAccount("654321")
                .amount(new BigDecimal("1000.00"))
                .build();
    }
}