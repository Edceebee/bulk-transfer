package com.interswitch.bulktransaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for bulk transaction processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTransactionResponse {

    private String batchId;
    private List<TransactionResult> results;
}