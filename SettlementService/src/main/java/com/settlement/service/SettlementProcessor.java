package com.settlement.service;

import com.events.billpay.BillBatchReadyEvent;
import com.events.billpay.BillBatchRetryEvent;
import com.events.billpay.Pain002Message;
import com.settlement.domain.BillBatchSettlement;
import com.settlement.domain.SettlementStatus;
import com.settlement.repo.BillBatchSettlementRepository;
import com.events.billpay.BillpayStatusEvent;
import com.events.billpay.BillBatchSubmittedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementProcessor {

    private static final int MAX_RETRIES = 3;

    private final BillBatchSettlementRepository settlementRepo;
    private final Pain001Builder pain001Builder;
    private final Central1Client central1Client;
    private final SettlementEventPublisher eventPublisher;

    @Transactional
    public void processNewBatch(BillBatchReadyEvent event) {
        log.info("Processing NEW batchId={}", event.batchId());
        doProcess(event.batchId());
    }

    @Transactional
    public void retryBatch(BillBatchRetryEvent event) {
        log.info("Processing RETRY attempt={} for batchId={}",
                event.attemptNumber(), event.batchId());
        doProcess(event.batchId());
    }

    
    private void doProcess(UUID batchId) {
        BillBatchSettlement settlement = settlementRepo.findByBatchId(batchId)
            .orElseGet(() -> {
                BillBatchSettlement s = new BillBatchSettlement();
                s.setBatchId(batchId);
                s.setCreatedAt(OffsetDateTime.now());
                s.setRetryCount(0);
                s.setStatus(SettlementStatus.READY);
                return s;
            });

        // Idempotency guard: already uploaded/submitted? don’t redo.
        if ((settlement.getStatus() == SettlementStatus.UPLOADED
                || settlement.getStatus() == SettlementStatus.SUBMITTED)
            && settlement.getCentralReference() != null) {
            log.info("Batch {} already uploaded/submitted (centralRef={}), skipping.", batchId, settlement.getCentralReference());
            return;
        }


        // Build file only if not already built
        if (settlement.getPain001FileName() == null) {
            String fileName = pain001Builder.buildFileForBatch(batchId);
            settlement.setPain001FileName(fileName);
            settlement.setStatus(SettlementStatus.FILE_BUILT);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);
        }

        try {
            String centralRef = central1Client.upload(settlement.getPain001FileName());
            settlement.setCentralReference(centralRef);
            settlement.setStatus(SettlementStatus.UPLOADED);
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);

            eventPublisher.publishBatchSubmitted(new BillBatchSubmittedEvent(batchId, centralRef));

            // optional: mark SUBMITTED only after successful publish (or via outbox)
            settlement.setStatus(SettlementStatus.SUBMITTED);
            settlementRepo.save(settlement);

        } catch (Exception ex) {
            log.error("Upload failed for batchId={} attempt={} : {}", batchId, settlement.getRetryCount(), ex.getMessage(), ex);
            settlement.setStatus(SettlementStatus.FAILED);
            settlement.setLastError(ex.getMessage());
            settlement.setUpdatedAt(OffsetDateTime.now());
            settlementRepo.save(settlement);

            handleFailureWithRetryOrDlq(settlement, ex);
        }
    }

    
    
    
    
    
    private void handleFailureWithRetryOrDlq(BillBatchSettlement settlement, Exception ex) {
        UUID batchId = settlement.getBatchId();

        if (settlement.getRetryCount() < MAX_RETRIES) {
            int nextAttempt = settlement.getRetryCount() + 1;

            BillBatchRetryEvent retryEvent = new BillBatchRetryEvent(
                    batchId,
                    nextAttempt,
                    "Auto-retry from SettlementService after failure: " + ex.getMessage(),
                    OffsetDateTime.now()
            );

            log.info("Scheduling retry attempt {} for batchId={}", nextAttempt, batchId);
            eventPublisher.publishBatchRetry(retryEvent); // topic: bill.batch.retry

        } else {
            log.warn("Max retries ({}) reached for batchId={}, sending to DLQ",
                    MAX_RETRIES, batchId);
            eventPublisher.publishDlq(batchId, ex.getMessage());
        }
    }

    @Transactional
    public void handlePain002(Pain002Message msg) {
        log.info("Handling pain.002 for paymentId={} batchId={}", msg.paymentId(), msg.batchId());

        String status = msg.success() ? "POSTED" : "FAILED";
        String reason = msg.reason();

        BillpayStatusEvent statusEvent = new BillpayStatusEvent(
        		UUID.randomUUID(),
                msg.paymentId(),
                msg.batchId(),
                status,
                reason,
                OffsetDateTime.now()
                
        );

        eventPublisher.publishBillpayStatus(statusEvent);
    }
}
