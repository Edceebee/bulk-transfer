package com.interswitch.bulktransaction.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO for downstream Transaction Service call
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionServiceRequest {

    private String transactionId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
}