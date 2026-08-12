package com.billpay.worker.repo;

import com.billpay.worker.domain.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, String> {

    Optional<Batch> findFirstByStatusOrderByCreatedAtAsc(String status);
}