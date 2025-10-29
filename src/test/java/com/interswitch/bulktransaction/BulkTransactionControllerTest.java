package com.interswitch.bulktransaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interswitch.bulktransaction.controller.BulkTransactionController;
import com.interswitch.bulktransaction.dto.request.BulkTransactionRequest;
import com.interswitch.bulktransaction.dto.request.TransactionRequest;
import com.interswitch.bulktransaction.dto.response.BulkTransactionResponse;
import com.interswitch.bulktransaction.dto.response.TransactionResult;
import com.interswitch.bulktransaction.security.JwtService;
import com.interswitch.bulktransaction.service.BulkTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BulkTransactionController
 * Tests the REST API endpoints with security and validation
 */
@WebMvcTest(BulkTransactionController.class)
class BulkTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BulkTransactionService bulkTransactionService;

    @MockBean
    private JwtService jwtService;

    private String validToken;

    @BeforeEach
    void setUp() {
        // Mock JWT validation
        when(jwtService.isTokenValid(any())).thenReturn(true);
        when(jwtService.extractUsername(any())).thenReturn("test-user");
        when(jwtService.extractRoles(any())).thenReturn(List.of("USER"));
    }

    /**
     * Test successful bulk transaction submission with USER role
     */
    @Test
    @WithMockUser(roles = "USER")
    void testProcessBulkTransactions_Success() throws Exception {
        // Arrange
        BulkTransactionRequest request = createValidRequest();
        BulkTransactionResponse expectedResponse = createSuccessResponse();

        when(bulkTransactionService.processBulkTransactions(any()))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/bulk-transactions")
                        .with(csrf()) // Add CSRF token for POST requests
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("BATCH001"))
                .andExpect(jsonPath("$.results[0].status").value("SUCCESS"));
    }

    /**
     * Test validation failure for invalid request
     */
    @Test
    @WithMockUser(roles = "USER")
    void testProcessBulkTransactions_ValidationError() throws Exception {
        // Arrange: Create invalid request (missing batchId)
        BulkTransactionRequest invalidRequest = BulkTransactionRequest.builder()
                .batchId("") // Empty batchId should fail validation
                .transactions(Arrays.asList(
                        TransactionRequest.builder()
                                .transactionId("TX001")
                                .fromAccount("123")
                                .toAccount("456")
                                .amount(BigDecimal.valueOf(100.00))
                                .build()
                ))
                .build();

        // Act & Assert: Expect 400 Bad Request
        mockMvc.perform(post("/api/v1/bulk-transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test validation failure for negative amount
     */
    @Test
    @WithMockUser(roles = "USER")
    void testProcessBulkTransactions_InvalidAmount() throws Exception {
        // Arrange: Create request with negative amount
        BulkTransactionRequest invalidRequest = BulkTransactionRequest.builder()
                .batchId("BATCH002")
                .transactions(Arrays.asList(
                        TransactionRequest.builder()
                                .transactionId("TX001")
                                .fromAccount("123")
                                .toAccount("456")
                                .amount(BigDecimal.valueOf(-100.00)) // Negative amount
                                .build()
                ))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/bulk-transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test unauthorized access without authentication
     */
    @Test
    void testProcessBulkTransactions_Unauthorized() throws Exception {
        // Arrange
        BulkTransactionRequest request = createValidRequest();

        // Act & Assert: No authentication, expect 401 or 403
        mockMvc.perform(post("/api/v1/bulk-transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test admin can retrieve batch results
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetBatchResults_AsAdmin_Success() throws Exception {
        // Arrange
        BulkTransactionResponse expectedResponse = createSuccessResponse();

        when(bulkTransactionService.getBatchResults("BATCH001"))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/bulk-transactions/BATCH001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value("BATCH001"));
    }


    // Helper methods
    /**
     * Creates a valid bulk transaction request
     */
    private BulkTransactionRequest createValidRequest() {
        return BulkTransactionRequest.builder()
                .batchId("BATCH001")
                .transactions(Arrays.asList(
                        TransactionRequest.builder()
                                .transactionId("TX001")
                                .fromAccount("123")
                                .toAccount("456")
                                .amount(BigDecimal.valueOf(100.50))
                                .build(),
                        TransactionRequest.builder()
                                .transactionId("TX002")
                                .fromAccount("789")
                                .toAccount("654")
                                .amount(BigDecimal.valueOf(200.00))
                                .build()
                ))
                .build();
    }

    /**
     * Creates a successful bulk transaction response
     */
    private BulkTransactionResponse createSuccessResponse() {
        return BulkTransactionResponse.builder()
                .batchId("BATCH001")
                .results(Arrays.asList(
                        TransactionResult.builder()
                                .transactionId("TX001")
                                .status("SUCCESS")
                                .build(),
                        TransactionResult.builder()
                                .transactionId("TX002")
                                .status("SUCCESS")
                                .build()
                ))
                .build();
    }
}