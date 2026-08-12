// controller/TransactionController.java
package com.account.controller;

import com.account.dto.TransactionResponse;
import com.account.model.Transaction;
import com.account.mapper.TransactionMapper;
import com.account.service.TransactionService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @GetMapping("/accounts/{accountId}/transactions")
    @PreAuthorize("hasAuthority('SCOPE_fdx:transactions.read')")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccount(
            @PathVariable("accountId") UUID accountId,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(name = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(name = "limit", defaultValue = "5") int limit,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "type", required = false) String type
    ) throws AccessDeniedException {

        Page<Transaction> page = transactionService.findTransactionsByAccount(
                accountId, startDate, endDate, limit, offset, type
        );

        List<TransactionResponse> dtos = page.getContent().stream()
                .map(transactionMapper::toResponse)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    
    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("hasAuthority('SCOPE_fdx:transactions.read')")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable UUID transactionId) {
        Transaction transaction = transactionService.findById(transactionId);
        TransactionResponse dto = transactionMapper.toResponse(transaction);

        return ResponseEntity.ok(dto);
    }
    
}
