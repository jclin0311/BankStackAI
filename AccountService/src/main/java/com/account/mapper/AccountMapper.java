package com.account.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.account.dto.AccountRequest;
import com.account.dto.AccountResponse;
import com.account.model.Account;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    /*
     * The ignores are deliberate: each field is owned by AccountService.create or by JPA,
     * not by the request. Declaring them keeps the strict unmapped-target policy useful —
     * a genuinely forgotten field still warns.
     */
    @Mapping(target = "id",                 ignore = true) // JPA identity
    @Mapping(target = "accountNumber",      ignore = true) // generated in AccountService.create
    @Mapping(target = "balance",            ignore = true) // seeded from request.openingBalance()
    @Mapping(target = "requestFingerprint", ignore = true) // idempotency key, set on create
    @Mapping(target = "createdAt",          ignore = true) // auditing
    @Mapping(target = "updatedAt",          ignore = true) // auditing
    @Mapping(target = "version",            ignore = true) // optimistic locking
    Account toEntity(AccountRequest request);

    @Mapping(target = "maskedAccountNumber", source = "accountNumber", qualifiedByName = "mask")
    AccountResponse toDto(Account account);

    @Named("mask")
    default String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "*****";
        return "*****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
