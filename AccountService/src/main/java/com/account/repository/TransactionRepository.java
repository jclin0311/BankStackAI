package com.account.repository;


import org.springframework.data.jpa.domain.Specification;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.account.model.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	 // Paging + Specs (no need to declare; provided by JpaSpecificationExecutor)
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    List<Transaction> findByAccountIdOrderByOccurredAtDesc(UUID accountId);

    List<Transaction> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID accountId, OffsetDateTime from, OffsetDateTime to);

    List<Transaction> findByAccountIdAndTypeOrderByOccurredAtDesc(UUID accountId, TransactionType type);
    Optional<Transaction> findByTransactionId(UUID transactionId);

    long countByAccountIdAndStatus(UUID accountId, TransactionStatus status);

    // 👇 Legacy support (only if you keep requestFingerprint on the entity)
    Optional<Transaction> findByRequestFingerprint(String requestFingerprint);
    Optional<Transaction> findByAccountIdAndRequestFingerprint(UUID accountId, String requestFingerprint);

}