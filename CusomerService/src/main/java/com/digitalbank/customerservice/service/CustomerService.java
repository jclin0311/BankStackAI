package com.digitalbank.customerservice.service;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.account.dto.CustomerResponse;
import com.commons.exception.ConflictException;
import com.commons.exception.CustomerNotFoundException;
import com.commons.exception.PreconditionRequiredException;
import com.commons.exception.ResourceNotFoundException;
import com.commons.exception.VersionMismatchException;
import com.digitalbank.customerservice.client.AuthServiceClient;
import com.digitalbank.customerservice.dto.CustomerCreatedResponse;
import com.digitalbank.customerservice.dto.CustomerRegistrationRequest;
import com.digitalbank.customerservice.dto.CustomerRequest;
import com.digitalbank.customerservice.dto.UpdateCustomerRequest;
import com.digitalbank.customerservice.mapper.CustomerMapper;
import com.digitalbank.customerservice.model.Customer;
import com.digitalbank.customerservice.model.KycStatus;
import com.digitalbank.customerservice.repository.CustomerRepository;
import com.digitalbank.customerservice.util.Fingerprints;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final AuthServiceClient authServiceClient;
	private final CustomerRepository repository;
	private final CustomerMapper mapper;

	public CustomerCreatedResponse create(CustomerRequest request) {

		String externalId = request.getExternalId();
		String email = request.getEmail();

		String fp = Fingerprints.customerCreate(request.getFirstName(), request.getLastName(), request.getEmail(),
				request.getPhone(), request.getAddress());

		// Fast path: same externalId already present?
		Optional<Customer> byExt = repository.findByExternalId(externalId);
		if (byExt.isPresent()) {
			Customer ex = byExt.get();
			if (fp.equals(ex.getRequestFingerprint())) {
				return mapper.toCreateResponse(ex); // idempotent replay
			}
			throw new ConflictException("Same externalId used with different data");
		}
		
		
		// Fast path: same Email already present?
				Optional<Customer> byEmail = repository.findByEmail(email);
				if (byEmail.isPresent()) {
					Customer ex = byEmail.get();
					if (fp.equals(ex.getRequestFingerprint())) {
						return mapper.toCreateResponse(ex); // idempotent replay
					}
					throw new ConflictException("Same email used with different data");
				}
				
		

		// New record attempt
		Customer entity = mapper.toEntity(request);
		entity.setExternalId(externalId);
		entity.setActive(false);
		entity.setKycStatus(KycStatus.PENDING);
		entity.setRequestFingerprint(fp);
		Customer saved = repository.saveAndFlush(entity);
		return mapper.toCreateResponse(saved);
		
	}

	public CustomerResponse getByExternalId(String externalId) {
		Customer customer = repository.findByExternalId(externalId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with externalId: " + externalId));
		;
		return mapper.toResponse(customer);
	}

	public boolean exists(String externalId) {
		return repository.findByExternalId(externalId).isPresent();

	}

	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}

	public Integer updateCustomer(String id, UpdateCustomerRequest request, Integer expected) {
		Customer c = repository.findByExternalId(id)
				.orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));

		if (expected == null)
			throw new PreconditionRequiredException("If-Match header required");
		if (!expected.equals(c.getVersion())) {
			throw new VersionMismatchException("Stale version. Current=" + c.getVersion() + ", If-Match=" + expected);
		}

		mapper.updateCustomerFromRequest(request, c);
		Customer saved = repository.save(c); // Hibernate increments version
		return mapper.toResponse(saved).getVersion();
	}

	public Integer updateKycStatus(String id, String kycStatus) {
		Customer c = repository.findByExternalId(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found with externalId: " + id));


		if ("VERIFIED".equalsIgnoreCase(kycStatus)) {
		    CustomerRegistrationRequest request =
		        new CustomerRegistrationRequest(c.getEmail(), "default-password", c.getExternalId());
		    authServiceClient.registerCustomer(request);
		    c.setKycStatus(KycStatus.VERIFIED);
		    c.setActive(true);
		    repository.save(c);
		}
		return c.getVersion();
	}

	public CustomerResponse getById(Long id) {
		return repository.findById(id)
		        .map(mapper::toResponse)
		        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with externalId: " + id));	}
}