package com.billpay.worker.repo;

import com.billpay.worker.domain.BatchLine;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchLineRepository extends JpaRepository<BatchLine, Long> {

    long countByBatchId(UUID batchId);
    List<BatchLine> findAllByBatchId(UUID batchId);

}