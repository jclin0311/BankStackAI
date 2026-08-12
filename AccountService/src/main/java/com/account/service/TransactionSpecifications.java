package com.account.service;

import org.springframework.data.jpa.domain.Specification;

import com.account.model.Transaction;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class TransactionSpecifications {

    public static Specification<Transaction> withFilters(OffsetDateTime startDate, OffsetDateTime endDate, String type) {
        return (root, query, cb) -> {
            Predicate p = cb.conjunction();

            if (startDate != null) {
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("occurredAt"), startDate));
            }
            if (endDate != null) {
                p = cb.and(p, cb.lessThanOrEqualTo(root.get("occurredAt"), endDate));
            }
            if (type != null && !type.isEmpty()) {
                p = cb.and(p, cb.equal(root.get("type"), type));
            }
            return p;
        };
    }

    public static Specification<Transaction> accountEquals(UUID accountId) {
        return (root, query, cb) -> cb.equal(root.get("accountId"), accountId);
    }
}
