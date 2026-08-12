package com.payments.orch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.client.AccountClient;
import com.payments.orch.domain.Outbox;
import com.payments.orch.domain.Payment;
import com.payments.orch.domain.PaymentState;
import com.events.billpay.*;
import com.payments.orch.repo.OutboxRepo;
import com.payments.orch.repo.PaymentRepo;
import com.account.dto.CreateHoldRequest;
import com.billpay.dto.BillPayRequest;
import com.billpay.dto.PaymentAcceptedResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillPayOrchestrator {

  private final BillPayValidator validator;
  private final AccountClient accounts;
  private final PaymentRepo paymentRepo;
  private final OutboxRepo outboxRepo;
  private final ObjectMapper om;

  @Transactional
  public PaymentAcceptedResponse acceptBillPay(BillPayRequest req, String idemKey) {
    // 1) Idempotency: check if this payment already exists
    var existing = paymentRepo.findByIdempotencyKey(idemKey);
    if (existing.isPresent()) {
      var p = existing.get();
      return new PaymentAcceptedResponse(
          p.getPaymentId(),
          p.getState().name(),
          "/api/v1/payments/" + p.getPaymentId()
      );
    }
    // 2) Business validation (account, biller, execution date, etc.)
    validator.validate(req);


    // 3) Build CreateHoldRequest for AccountService

    var holdReq = new CreateHoldRequest(
            req.amount().getValue(),     // BigDecimal
            "BILLPAY",                // reason
            null,                     // releaseAt (optional)
            idemKey                   // idempotency key
    );
    
   
    //Generate paymentId (ledger/business id)
    // 4) Call AccountService to place hold (JWT relay via FeignTokenRelayConfig) 
    var paymentId=  accounts.placeHold(req.debtorAccountId(), idemKey, holdReq).holdId();

    // 5) Persist Payment row in FUNDS_HELD state
    var now = OffsetDateTime.now();
    var payment = Payment.builder()
        .paymentId(paymentId)
        .state(PaymentState.FUNDS_HELD)
        .debtorAccountId(req.debtorAccountId())
        .billerRefNumber(req.billerReferenceNumber())
        .invoiceReference(req.invoiceReference())
        .executionDate(LocalDate.parse(req.executionDate()))
        .amountValue(req.amount().getValue())
        .amountCcy(req.amount().getCurrency())
        .idempotencyKey(idemKey)
        .createdAt(now)
        .updatedAt(now)
        .build();
    paymentRepo.save(payment);

    // 6) Outbox event: billpay.requested
    var evt = BillPayRequested.builder()
        .eventId(UUID.randomUUID().toString())
        .paymentId(paymentId)
        .debtorAccountId(payment.getDebtorAccountId())
        .billerRefNumber(payment.getBillerRefNumber())
        .invoiceReference(payment.getInvoiceReference())
        .executionDate(payment.getExecutionDate().toString())
        .amountValue(payment.getAmountValue())
        .amountCcy(payment.getAmountCcy())
        .occurredAt(now.toString())
        .schemaVersion("1")
        .channel("billpay")
        .build();

    outboxRepo.save(Outbox.builder()
        .topic("billpay.requested")
        .key(paymentId)
        .payloadJson(write(evt))
        .state("PENDING")
        .createdAt(now)
        .updatedAt(now)
        .build());

    // 7) Return async-202 style response
    return new PaymentAcceptedResponse(
        paymentId,
        PaymentState.FUNDS_HELD.name(),
        "/api/v1/payments/" + paymentId
    );
  }

  public Payment view(UUID paymentId) {
    return paymentRepo.findById(paymentId).orElseThrow();
  }

  private String write(Object o) {
    try {
      return om.writeValueAsString(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
