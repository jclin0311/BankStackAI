package com.billpay.dto;

import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.UUID;

import com.account.dto.AmountDTO;

public record BillPayRequest(
  @NotNull UUID debtorAccountId,
  @NotBlank String billerReferenceNumber,
  @NotBlank String invoiceReference,
  @NotBlank String executionDate,          // yyyy-MM-dd
  @NotNull  AmountDTO amount,
  String note
 
) {}
