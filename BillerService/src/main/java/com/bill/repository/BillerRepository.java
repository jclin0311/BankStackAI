package com.bill.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;


import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bill.model.Biller;

public interface BillerRepository extends JpaRepository<Biller, UUID> {
	
	
	Page<Biller> findByCustomerId(String customerId, Pageable pageable);
	Optional<Biller> findByIdAndCustomerId(UUID id, String customerId);
	boolean existsByCustomerIdAndReferenceNumber(String customerId, String referenceNumber);
	

boolean existsByReferenceNumberAndStatus(String referenceNumber, String status);

}
