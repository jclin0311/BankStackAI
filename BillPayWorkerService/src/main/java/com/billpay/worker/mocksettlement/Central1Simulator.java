package com.billpay.worker.mocksettlement;


import com.billpay.dto.Pain002FileMessage;
import com.billpay.worker.domain.BatchLine;
import com.billpay.worker.repo.BatchLineRepository;

import com.events.billpay.Pain002Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;



@Component
@RequiredArgsConstructor
@Slf4j
public class Central1Simulator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final BatchLineRepository batchLineRepo;


    public void simulatePain002ForBatch(UUID batchId) {
    	
    	
    	List<UUID> paymentIds = batchLineRepo.findAllByBatchId(batchId)
    	        .stream()
    	        .map(BatchLine::getPaymentId)
    	        .toList();
       
    	
    	List<Pain002Message> items = paymentIds.stream()
    	        .map(pid -> new Pain002Message(
    	                pid,
    	                batchId,
    	                "E2E-" + pid,
    	                "ACTC",
    	                "Accepted",
    	                true,
    	                "Posted successfully",
    	                OffsetDateTime.now()
    	        ))
    	        .toList();

    	    Pain002FileMessage file = new Pain002FileMessage(
    	            batchId,
    	            items,
    	            OffsetDateTime.now()
    	    );

    	    try {
                String json = objectMapper.writeValueAsString(file);
                kafkaTemplate.send("central1.pain002", batchId.toString(), json);
                log.info("Simulated pain.002 file for batch {}", batchId);
            } catch (Exception e) {
                log.error("Failed to simulate Pain002 file for batch {}", batchId, e);
            }
    	
    }
    
    
    
    
}
