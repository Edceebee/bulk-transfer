package com.interswitch.bulktransaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of individual transaction processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult {

    private String transactionId;
    private String status; // SUCCESS or FAILED
    private String reason; // Error message if failed
}
