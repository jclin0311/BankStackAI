// com.digitalbank.customerservice.dto.CustomerCreatedResponse
package com.digitalbank.customerservice.dto;

import java.time.LocalDateTime;
import com.digitalbank.customerservice.model.KycStatus;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerCreatedResponse {
    private String externalId;
    private KycStatus kycStatus;
    private Integer version;
    private LocalDateTime createdAt;
}
