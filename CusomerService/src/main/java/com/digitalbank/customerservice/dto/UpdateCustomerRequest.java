package com.digitalbank.customerservice.dto;

import lombok.Data;

@Data
public class UpdateCustomerRequest {
    private String fullName;
    private String email;
    private String phone;
}