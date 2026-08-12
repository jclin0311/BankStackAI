package com.payments.orch.service;

import com.billpay.dto.BillPayRequest;
import com.payments.orch.client.BillerRegistryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BillPayValidator {
  private final BillerRegistryClient registry;

  public void validate(BillPayRequest r) {
    if (!registry.isActive(r.billerReferenceNumber())) {
      throw new IllegalArgumentException("BILLER_INACTIVE");
    }
    var exec = LocalDate.parse(r.executionDate());
    if (exec.isBefore(LocalDate.now())) {
      throw new IllegalArgumentException("EXECUTION_DATE_PAST");
    }
    if (!"CAD".equals(r.amount().getCurrency())) {
      throw new IllegalArgumentException("CURRENCY_NOT_ALLOWED");
    }
  }
}
