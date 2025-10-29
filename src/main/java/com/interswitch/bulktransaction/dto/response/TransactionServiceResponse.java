package com.interswitch.bulktransaction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from Transaction Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionServiceResponse {

    private String transactionId;
    private String status;
    private String message;
}