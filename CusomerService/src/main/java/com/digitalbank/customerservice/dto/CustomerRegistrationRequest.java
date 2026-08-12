package com.digitalbank.customerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerRegistrationRequest {
    private String email;
    private String password;
    private String customerId; // Link to customer-service record
}
