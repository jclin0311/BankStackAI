package com.bill.service;

import com.bill.dto.BillerRequest;
import com.bill.dto.BillerResponse;
import com.bill.dto.PageResponse;
import com.bill.model.Biller;
import com.bill.repository.BillerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillerService {

    private final BillerRepository repo;

    public BillerResponse create(String customerId, BillerRequest req) {
        if (repo.existsByCustomerIdAndReferenceNumber(customerId, req.getReferenceNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Biller already exists for this referenceNumber"
            );
        }

        Biller b = Biller.builder()
                .customerId(customerId)
                .name(req.getName())
                .referenceNumber(req.getReferenceNumber())
                .category(req.getCategory())
                .status("ACTIVE")
                .build();

        b = repo.save(b);
        return toDto(b);
    }

    public PageResponse<BillerResponse> list(final String customerId,
                                             final int limit,
                                             final int offset) {

        final int safeLimit = (limit > 0) ? limit : 10;
        final int safeOffset = Math.max(0, offset);
        final int pageIndex = safeOffset / safeLimit;

        final Pageable pageable = PageRequest.of(pageIndex, safeLimit);
        // Agar ordering chahiye:
        // PageRequest.of(pageIndex, safeLimit, Sort.by("createdAt").descending());

        final Page<Biller> page = repo.findByCustomerId(customerId, pageable);

        final List<BillerResponse> items = new ArrayList<>();
        for (Biller b : page.getContent()) {
            items.add(toDto(b));
        }

        final PageResponse<BillerResponse> response = new PageResponse<>();
        response.setItems(items);
        response.setTotal(page.getTotalElements());
        response.setLimit(safeLimit);
        response.setOffset(safeOffset);

        return response;
    }

    public BillerResponse get(String customerId, UUID id) {
        Biller b = repo.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Biller not found"));
        return toDto(b);
    }

    public void delete(String customerId, UUID id) {
        Biller b = repo.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Biller not found"));
        repo.delete(b);
    }

    public boolean isActive(String referenceNumber) {
        // Global registry-style check: any ACTIVE biller with this referenceNumber
        return repo.existsByReferenceNumberAndStatus(referenceNumber, "ACTIVE");
    }

    private BillerResponse toDto(Biller b) {
        return BillerResponse.builder()
                .id(b.getId())
                .name(b.getName())
                .referenceNumber(b.getReferenceNumber())
                .category(b.getCategory())
                .status(b.getStatus())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
}
