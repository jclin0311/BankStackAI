package com.account.repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.account.model.Account;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByCustomerId(String customerId);
    Optional<Account> findByRequestFingerprint(String fingerprint);
    Optional<Account> findByAccountNumber(String accountNumber);
}
