package com.account.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.account.dto.*;
import com.account.mapper.AccountMapper;
import com.account.model.*;
import com.account.repository.AccountHoldRepository;
import com.account.repository.AccountRepository;
import com.commons.exception.OwnerAccessDeniedException;
import com.commons.security.CurrentUser;
import com.account.dto.TransactionRequest;
import com.account.model.Transaction;
import com.account.mapper.TransactionMapper;
import org.apache.commons.codec.digest.DigestUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepo;
	private final AccountHoldRepository holdRepo;
	private final AccountMapper mapper;
	private final TransactionService transactionService;
	private final TransactionMapper transactionMapper;

	private final CurrentUser currentUser;

	/* ---------------- Utility ---------------- */

	private static String fingerprintForCreate(AccountRequest r, String idempotencyKey) {
		if (idempotencyKey != null && !idempotencyKey.isBlank())
			return idempotencyKey.trim();
		// Stable fingerprint for idempotent account create
		String base = (r.customerId() + "|" + r.accountType() + "|" + r.accountSubType() + "|" + r.currency() + "|"
				+ r.nickname() + "|" + r.displayName()).toUpperCase();
		return Integer.toHexString(base.hashCode());
	}

	private BigDecimal activeHoldsTotal(UUID accountId) {
		return holdRepo.findByAccountIdAndStatus(accountId, HoldStatus.ACTIVE).stream().map(AccountHold::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

	}

	private void ensureOwnerOrAdmin(Account a) {
		
		
		if (currentUser.hasScope("admin:accounts"))
			return;
		
        var auth = SecurityContextHolder.getContext().getAuthentication();

		var me = currentUser.customerId().orElseThrow(() -> new OwnerAccessDeniedException());
		
		if (!a.getCustomerId().equals(me)) {
			throw new OwnerAccessDeniedException();
		}
	}

	private void emitTransaction(Account acc, String type, BigDecimal amount, String reason, boolean posting,
			BigDecimal balanceAfterOrNull) {

		String currency = acc.getCurrency();

// Build DTO
		TransactionRequest req = new TransactionRequest(acc.getId(), // accountId
				amount, // amount
				currency, // currency
				type, // type: DEBIT/CREDIT/HOLD_PLACED/HOLD_RELEASED
				reason, // reason
				posting ? balanceAfterOrNull : null, // balanceAfter (only for postings)
				OffsetDateTime.now(ZoneOffset.UTC) // occurredAt
		);

// Map to JPA entity
		Transaction tx = transactionMapper.toEntity(req);

		String fingerprint = DigestUtils.sha256Hex(
				acc.getId().toString() + type + amount.toPlainString() + reason + req.occurredAt().toString());
		tx.setRequestFingerprint(fingerprint);

// Persist using same DB + same Spring transaction
		transactionService.save(tx);
	}

	/* ---------------- Queries ---------------- */

	public List<AccountResponse> listAll() {
		return accountRepo.findAll().stream().map(mapper::toDto).toList();
	}

	public AccountResponse get(UUID id) {
		return mapper
				.toDto(accountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found")));
	}

	public AccountBalanceResponse getBalance(UUID id) {
		Account a = accountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
		ensureOwnerOrAdmin(a);
		BigDecimal holds = activeHoldsTotal(id);
		BigDecimal available = a.getBalance().subtract(holds);
		return new AccountBalanceResponse(a.getBalance(), holds, available);
	}

	/** NEW: used by GET /customer/{id}/accounts */
	public List<AccountResponse> findByCustomerId(String customerId) {
		return accountRepo.findByCustomerId(customerId).stream().map(mapper::toDto).toList();
	}

	/** NEW: used by PATCH /accounts/{id}/status */
	@Transactional
	public void updateStatus(UUID id, AccountStatus status) {
		Account a = accountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
		ensureOwnerOrAdmin(a);
		a.setStatus(status);
		accountRepo.save(a);
	}

	/** NEW: used by GET /accounts/{id}/owner */
	public String getCustomerIdForAccount(UUID id) {
		Account a = accountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
		ensureOwnerOrAdmin(a);
		return a.getCustomerId();
	}

	/* ---------------- Commands ---------------- */

	@Transactional
	public AccountResponse create(AccountRequest request, String idempotencyKey) {
		String fp = fingerprintForCreate(request, idempotencyKey);

		Optional<Account> existing = accountRepo.findByRequestFingerprint(fp);
		if (existing.isPresent()) {
			Account a = existing.get();
			return mapper.toDto(a);
		}

		Account entity = mapper.toEntity(request);
		ensureOwnerOrAdmin(entity);
		entity.setRequestFingerprint(fp);
		entity.setBalance(request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance());

		// naive account number generator — replace with real BIN/range later
		entity.setAccountNumber("9" + Math.abs((int) System.nanoTime()));

		return mapper.toDto(accountRepo.save(entity));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public AccountResponse credit(UUID id, PostingRequest r, Integer expectedVersion) {
		Account a = accountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
		ensureOwnerOrAdmin(a);

		if (expectedVersion != null && !expectedVersion.equals(a.getVersion())) {
			throw new IllegalStateException("ETag mismatch");
		}
		a.setBalance(a.getBalance().add(r.amount()));

		Account saved = accountRepo.saveAndFlush(a);

		emitTransaction(saved, "CREDIT", r.amount(), r.reason(), true, saved.getBalance());
		return mapper.toDto(saved);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public AccountResponse debit(UUID id, PostingRequest r, Integer expectedVersion) {
		Account a = accountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found"));
		if (expectedVersion != null && !expectedVersion.equals(a.getVersion())) {
			throw new IllegalStateException("ETag mismatch");
		}
		ensureOwnerOrAdmin(a);

		BigDecimal holds = activeHoldsTotal(id);
		BigDecimal available = a.getBalance().subtract(holds);
		if (r.amount().compareTo(available) > 0) {
			throw new IllegalArgumentException("Insufficient available funds");
		}
		a.setBalance(a.getBalance().subtract(r.amount()));

		Account saved = accountRepo.saveAndFlush(a);

		emitTransaction(saved, "DEBIT", r.amount(), r.reason(), true, saved.getBalance());
		return mapper.toDto(saved);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public HoldResponse createHold(UUID accountId, CreateHoldRequest r) {
		Account a = accountRepo.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("Account not found"));
		ensureOwnerOrAdmin(a);

		String fp = (r.idempotencyKey() != null && !r.idempotencyKey().isBlank()) ? r.idempotencyKey().trim() : null;
		if (fp != null) {
			Optional<AccountHold> ex = holdRepo.findByRequestFingerprint(fp);
			if (ex.isPresent()) {
				AccountHold h = ex.get();
				return new HoldResponse(h.getId(), h.getAmount(), h.getStatus(), h.getCreatedAt(), h.getReleaseAt());
			}
		}

		BigDecimal holds = activeHoldsTotal(accountId);
		BigDecimal available = a.getBalance().subtract(holds);
		if (r.amount().compareTo(available) > 0) {
			throw new IllegalArgumentException("Insufficient available funds for hold");
		}

		AccountHold h = AccountHold.builder().accountId(accountId).amount(r.amount()).status(HoldStatus.ACTIVE)
				.reason(r.reason()).releaseAt(r.releaseAt()).requestFingerprint(fp).build();

		h = holdRepo.save(h);
		emitTransaction(a, "HOLD_PLACED", r.amount(), r.reason(), true, a.getBalance());

		return new HoldResponse(h.getId(), h.getAmount(), h.getStatus(), h.getCreatedAt(), h.getReleaseAt());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public HoldResponse releaseHold(UUID accountId, UUID holdId, String reason) {
		AccountHold h = holdRepo.findById(holdId).orElseThrow(() -> new IllegalArgumentException("Hold not found"));
		if (!h.getAccountId().equals(accountId)) {
			throw new IllegalArgumentException("Hold does not belong to this account");
		}
		if (h.getStatus() != HoldStatus.ACTIVE) {
			return new HoldResponse(h.getId(), h.getAmount(), h.getStatus(), h.getCreatedAt(), h.getReleaseAt());
		}

		h.setStatus(HoldStatus.RELEASED);
		h.setReason(reason);
		h = holdRepo.save(h);
		Account a = accountRepo.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("Account not found"));

		emitTransaction(a, "HOLD_RELEASED", h.getAmount(), reason, true, a.getBalance());

		return new HoldResponse(h.getId(), h.getAmount(), h.getStatus(), h.getCreatedAt(), h.getReleaseAt());
	}
}
