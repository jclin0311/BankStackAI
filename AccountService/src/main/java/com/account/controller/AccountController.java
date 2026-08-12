package com.account.controller;

import com.account.dto.AccountBalanceResponse;
import com.account.dto.AccountOwnerResponse;
import com.account.dto.AccountRequest;
import com.account.dto.AccountResponse;
import com.account.dto.AccountStatus;
import com.account.dto.CreateHoldRequest;
import com.account.dto.HoldResponse;
import com.account.dto.PostingRequest;
import com.account.service.AccountService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService service;

    /* ---------------- Accounts ---------------- */

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.write')")
    public ResponseEntity<AccountResponse> create(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AccountRequest request) {
        AccountResponse resp = service.create(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag('"' + String.valueOf(resp.version()) + '"')
                .body(resp);
    }

    @GetMapping("/accounts/{id}")
    @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.read')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable("id") UUID id) {
        AccountResponse r = service.get(id);
        return ResponseEntity.ok()
                .eTag('"' + String.valueOf(r.version()) + '"')
                .body(r);
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('SCOPE_admin:accounts.read')")
    public ResponseEntity<List<AccountResponse>> listAccounts(
            @RequestParam(name = "status", required = false) AccountStatus status,
            @RequestParam(name = "currency", required = false) String currency) {
        // Simple version: return all. (Optional: add filters in service later.)
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/accounts/{id}/balance")
    @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.read')")
    public ResponseEntity<AccountBalanceResponse> getBalances(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.getBalance(id));
    }

    @GetMapping("/customer/{id}/accounts")
    @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.read')")
    public ResponseEntity<List<AccountResponse>> getByCustomer(@PathVariable("id") String id) {
        log.info("Fetching accounts for customer: {}", id);
        return ResponseEntity.ok(service.findByCustomerId(id));
    }

    @PatchMapping("/accounts/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_admin:accounts.write')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable("id") UUID id,
            @RequestParam("status") AccountStatus status) {
        service.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accounts/{id}/owner")
    @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.read')")
    public AccountOwnerResponse getAccountOwner(@PathVariable("id") UUID id) {
        String customerId = service.getCustomerIdForAccount(id);
        return new AccountOwnerResponse(id, customerId);
    }

    /* ---------------- Holds ---------------- */

    @PostMapping("/accounts/{id}/holds")
    @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.write')")
    public ResponseEntity<HoldResponse> placeHold(
            @PathVariable("id") UUID id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateHoldRequest request) {
        HoldResponse resp = service.createHold(id, request.withIdempotencyKey(idempotencyKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/accounts/{id}/holds/{holdId}/release")
    @PreAuthorize("hasAnyAuthority('SCOPE_fdx:accounts.write','SCOPE_admin:accounts')")
    public ResponseEntity<HoldResponse> releaseHold(
            @PathVariable("id") UUID id,
            @PathVariable("holdId") UUID holdId) {
        HoldResponse resp = service.releaseHold(id, holdId, "manual_release");
        return ResponseEntity.ok(resp);
    }

    /* ---------------- Postings ---------------- */

    @PostMapping("/accounts/{id}/credit")
    @PreAuthorize("hasAuthority('SCOPE_fdx:accounts.write')")
    public ResponseEntity<AccountResponse> credit(
            @PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PostingRequest request) {
        Integer expected = parseIfMatch(ifMatch);
        AccountResponse r = service.credit(id, request, expected);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag('"' + String.valueOf(r.version()) + '"')
                .body(r);
    }

    @PostMapping("/accounts/{id}/debit")
    @PreAuthorize("hasAnyAuthority('SCOPE_fdx:accounts.write','SCOPE_admin:accounts')")
    public ResponseEntity<AccountResponse> debit(
            @PathVariable("id") UUID id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody PostingRequest request) {
        Integer expected = parseIfMatch(ifMatch);
        AccountResponse r = service.debit(id, request, expected);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag('"' + String.valueOf(r.version()) + '"')
                .body(r);
    }

    /* ---------------- Helpers ---------------- */

    private Integer parseIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) return null;
        String v = ifMatch.replace("\"", "").trim();
        return Integer.valueOf(v);
    }
}
