package com.bill.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.*;



@Entity
@Table(
name = "billers",
uniqueConstraints = {
@UniqueConstraint(name = "uk_biller_owner_ref", columnNames = {"customer_id", "reference_number"})
},
indexes = {
@Index(name = "idx_biller_customer", columnList = "customer_id")
}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Biller {
@Id
@GeneratedValue
private UUID id;


@Column(name = "customer_id", nullable = false)
private String customerId; // owner (internal customer id)


@Column(nullable = false)
private String name;


@Column(name = "reference_number", nullable = false)
private String referenceNumber; // account number with the biller


@Column(nullable = false)
private String category; // e.g., Electricity, Internet, Credit Card


@Builder.Default
@Column(nullable = false)
private String status = "ACTIVE"; // ACTIVE / INACTIVE


@Column(name = "created_at", nullable = false)
private OffsetDateTime createdAt;


@Column(name = "updated_at", nullable = false)
private OffsetDateTime updatedAt;


@PrePersist
void prePersist() {
var now = OffsetDateTime.now();
createdAt = now;
updatedAt = now;
}


@PreUpdate
void preUpdate() {
updatedAt = OffsetDateTime.now();
}
}