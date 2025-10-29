package com.interswitch.bulktransaction.controller;

import com.interswitch.bulktransaction.dto.request.BulkTransactionRequest;
import com.interswitch.bulktransaction.dto.response.BulkTransactionResponse;
import com.interswitch.bulktransaction.service.BulkTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for bulk transaction processing
 * Handles incoming bulk transaction requests and delegates processing to service layer
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bulk-transactions")
@RequiredArgsConstructor
public class BulkTransactionController {

    private final BulkTransactionService bulkTransactionService;

    /**
     * Processes a batch of transactions
     * Only users with ROLE_USER or ROLE_ADMIN can submit bulk transactions
     *
     * @param request The bulk transaction request containing batchId and transactions
     * @return Response with processing results for each transaction
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BulkTransactionResponse> processBulkTransactions(
            @Valid @RequestBody BulkTransactionRequest request) {

        log.info("Received bulk transaction request for batchId: {}", request.getBatchId());

        // Process the bulk transaction
        BulkTransactionResponse response = bulkTransactionService.processBulkTransactions(request);

        log.info("Completed processing for batchId: {}", request.getBatchId());

        return ResponseEntity.ok(response);
    }


    /**
     * Retrieves batch processing results (Admin only)
     *
     * @param batchId The batch ID to retrieve results for
     * @return The processing results
     */
    @GetMapping("/{batchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkTransactionResponse> getBatchResults(@PathVariable String batchId) {

        log.info("Admin retrieving results for batchId: {}", batchId);

        BulkTransactionResponse response = bulkTransactionService.getBatchResults(batchId);

        return ResponseEntity.ok(response);
    }
}