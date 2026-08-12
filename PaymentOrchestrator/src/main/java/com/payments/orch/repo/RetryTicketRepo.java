package com.payments.orch.repo;

import com.payments.orch.domain.RetryTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface RetryTicketRepo extends JpaRepository<RetryTicket, Long> {

    List<RetryTicket> findByStatusAndNextAttemptAtBefore(
            String status,
            OffsetDateTime now
    );

    List<RetryTicket> findByBatchIdOrderByAttemptAsc(String batchId);
    
    
    List<RetryTicket> findTop200ByStatusAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
            String status,
            OffsetDateTime nextAttemptAt
    );

    
}
