package com.account.service;

import com.commons.exception.ResourceNotFoundException;
import com.commons.exception.UnauthorizedAccessException;
import com.account.model.Transaction;
import com.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.domain.Sort;


import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;


import org.springframework.data.domain.Pageable;

import java.nio.file.AccessDeniedException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {
	  private final TransactionRepository transactionRepository;

	    public Page<Transaction> findTransactions(OffsetDateTime startDate, OffsetDateTime endDate, int limit, int offset,
	                                              String type) {
	        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("createdAt").descending());
	        Specification<Transaction> spec = TransactionSpecifications.withFilters(startDate, endDate, type);

	        return transactionRepository.findAll(spec, pageable);
	    }

	    
	    
	    private String getCurrentCustomerId() {
	        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	        if (principal instanceof Jwt jwt) {
	        	 String customerId = jwt.getClaim("customer_id");  // 'customer_id' custom claim
	             if (customerId != null) {
	                 return customerId;
	             } else {
	                 throw new UnauthorizedAccessException("Customer ID not found in token.");
	             }
	        }
	        throw new UnauthorizedAccessException("Invalid or missing authentication token.");
	    }
	    
	    

	    
	    public Page<Transaction> findTransactionsByAccount(UUID accountId, OffsetDateTime startDate, OffsetDateTime endDate,
	                                                      int limit, int offset, String type) {
	        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by("createdAt").descending());
	        Specification<Transaction> spec = Specification.where(TransactionSpecifications.accountEquals(accountId))
	                .and(TransactionSpecifications.withFilters(startDate, endDate, type));

	        return transactionRepository.findAll(spec, pageable);
	    }

	    public Transaction findById(UUID transactionId) {

	        return transactionRepository.findByTransactionId(transactionId)
	                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for id: " + transactionId));
	    }

	    public Transaction save(Transaction tx) {
	    	// If caller populated requestFingerprint, dedupe on it	
	    	if (tx.getRequestFingerprint() != null && !tx.getRequestFingerprint().isBlank()) {
	    	Optional<Transaction> dup = transactionRepository.findByRequestFingerprint(tx.getRequestFingerprint());
	    	if (dup.isPresent()) {
	    	log.info("Idempotent replay for transaction fp={}", tx.getRequestFingerprint());
	    	return dup.get();
	    	}
	    	}
	    	//throw new RuntimeException("Simulated failure after account balance update");
            return transactionRepository.save(tx);
	    	}
}