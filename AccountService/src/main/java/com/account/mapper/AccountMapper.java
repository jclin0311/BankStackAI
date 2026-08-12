package com.account.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.account.dto.AccountRequest;
import com.account.dto.AccountResponse;
import com.account.model.Account;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    Account toEntity(AccountRequest request);

    @Mapping(target = "maskedAccountNumber", source = "accountNumber", qualifiedByName = "mask")
    AccountResponse toDto(Account account);

    @Named("mask")
    default String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return "*****";
        return "*****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
