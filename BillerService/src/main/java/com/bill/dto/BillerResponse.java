package com.bill.dto;


import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillerResponse {
private UUID id;
private String name;
private String referenceNumber;
private String category;
private String status;
private OffsetDateTime createdAt;
private OffsetDateTime updatedAt;
}