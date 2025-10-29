package com.interswitch.bulktransaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Main application class for Bulk Transaction Processing Service
 * Enables Spring Boot autoconfiguration, Feign clients for REST calls, and retry mechanism
 */
@SpringBootApplication
@EnableFeignClients
public class BulkTransactionApplication {

	public static void main(String[] args) {
		SpringApplication.run(BulkTransactionApplication.class, args);
	}
}
