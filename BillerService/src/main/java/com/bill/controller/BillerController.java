package com.bill.controller;

import com.commons.security.CurrentUser;
import com.bill.dto.BillerRequest;
import com.bill.dto.BillerResponse;
import com.bill.dto.PageResponse;
import com.bill.service.BillerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BillerController {

    private final BillerService billerService;
    private final CurrentUser currentUser;

    @PostMapping("/billers")
    @PreAuthorize("hasAuthority('SCOPE_fdx:bill.write')")
    public ResponseEntity<BillerResponse> create(
            @RequestBody BillerRequest request,
            HttpServletRequest http
    ) {
        String customerId = currentUser.customerId().get();
        BillerResponse res = billerService.create(customerId, request);
        URI loc = URI.create("/api/v1/billers/" + res.getId());
        return ResponseEntity.created(loc).body(res);
    }

    @GetMapping("/billers")
    @PreAuthorize("hasAuthority('SCOPE_fdx:bill.read')")
    public ResponseEntity<PageResponse<BillerResponse>> list(
            @RequestParam(name="limit",defaultValue = "10") int limit,
            @RequestParam(name="offset", defaultValue = "0") int offset    ) {
        String customerId = currentUser.customerId().get();

        PageResponse<BillerResponse> resp = billerService.list(customerId, limit, offset);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/billers/{id}")
    @PreAuthorize("hasAuthority('SCOPE_fdx:bill.read')")
    public ResponseEntity<BillerResponse> get(
            @PathVariable("id") UUID id
            
    ) {
        String customerId = currentUser.customerId().get();
        return ResponseEntity.ok(billerService.get(customerId, id));
    }

    @DeleteMapping("/billers/{id}")
    @PreAuthorize("hasAuthority('SCOPE_fdx:bill.write')")
    public ResponseEntity<Void> delete(
            @PathVariable("id") UUID id,
            HttpServletRequest http
    ) {
        String customerId = currentUser.customerId().get();
        billerService.delete(customerId, id);
        return ResponseEntity.noContent().build();
    }

    // --- Registry-style lookup for orchestrator ---

    @GetMapping("/billers/{refNum}/active")
    @PreAuthorize("hasAuthority('SCOPE_fdx:bill.read')")
    public ResponseEntity<Boolean> isActive(@PathVariable("refNum") String refNum) {
        boolean active = billerService.isActive(refNum);
        return ResponseEntity.ok(active);
    }
}
