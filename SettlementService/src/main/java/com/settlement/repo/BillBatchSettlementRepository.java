package com.settlement.repo;

import com.settlement.domain.BillBatchSettlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillBatchSettlementRepository extends JpaRepository<BillBatchSettlement, Long> {

    Optional<BillBatchSettlement> findByBatchId(UUID batchId);
}
