package com.interswitch.bulktransaction.dto.request;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * Request DTO for bulk transaction submission
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTransactionRequest {

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Transactions list cannot be null")
    @Size(min = 1, message = "At least one transaction is required")
    @Valid // Validates each transaction in the list
    private List<TransactionRequest> transactions;
}
