package com.payments.orch.service;

import com.account.dto.PostingRequest;
import com.events.billpay.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.orch.client.AccountClient;
import com.payments.orch.client.AccountM2MClient;
import com.payments.orch.domain.Payment;
import com.payments.orch.domain.PaymentState;
import com.payments.orch.domain.ProcessedEvent;
import com.events.billpay.BillpayStatusEvent;
import com.payments.orch.repo.PaymentRepo;
import com.payments.orch.repo.ProcessedEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatusConsumer {
  private final PaymentRepo paymentRepo;
  private final ProcessedEventRepo processed;
  private final ObjectMapper om;
  private final AccountM2MClient accountM2MClient;


  @KafkaListener(topics="billpay.status", groupId="payment-api")
  @Transactional
  public void onMessage(String message) throws Exception {
    var evt = om.readValue(message, BillpayStatusEvent.class);
    if (processed.existsByHandlerAndEventId("status", evt.eventId().toString())) return;

    Payment p = paymentRepo.findById((evt.paymentId())).orElse(null);
    if (p == null) return;

    if ("POSTED".equalsIgnoreCase(evt.status())) {
    	PostingRequest r = new PostingRequest(p.getAmountValue(), p.getReason());
    	accountM2MClient.releaseHold(p.getDebtorAccountId(), p.getPaymentId());
    	accountM2MClient.debit(p.getDebtorAccountId(),null, r);
    	p.setState(PaymentState.POSTED);
    } else {
    	accountM2MClient.releaseHold(p.getDebtorAccountId(), p.getPaymentId());
      p.setState(PaymentState.FAILED);
    }
   // p.setExternalStatusCode(evt.status());
    p.setReason(evt.reason());
    p.setUpdatedAt(OffsetDateTime.now());
    paymentRepo.save(p);

    processed.save(ProcessedEvent.builder()
        .handler("status").eventId(evt.eventId().toString())
        .processedAt(OffsetDateTime.now()).build());
  }
}
