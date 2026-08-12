package com.bill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillerRequest {
    private String name;
    private String referenceNumber;
    private String category; // e.g. Electricity, Internet, Credit Card
}
