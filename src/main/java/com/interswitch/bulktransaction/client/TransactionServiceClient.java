package com.interswitch.bulktransaction.client;

import com.interswitch.bulktransaction.dto.request.TransactionServiceRequest;
import com.interswitch.bulktransaction.dto.response.TransactionServiceResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client for calling downstream Transaction Service
 * Handles REST API communication with the Transaction Service
 *
 * Configuration:
 * - name: Logical name for the client
 * - url: Base URL of the Transaction Service (configured in application.yml)
 */
@FeignClient(
        name = "transaction-service",
        url = "${transaction-service.url}"
)
public interface TransactionServiceClient {

    /**
     * Processes a single transaction via Transaction Service
     *
     * @param request The transaction request
     * @return Response from Transaction Service
     */
    @PostMapping("/api/v1/transactions")
    TransactionServiceResponse processTransaction(@RequestBody TransactionServiceRequest request);
}