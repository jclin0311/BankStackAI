package com.account.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.account.dto.AmountDTO;
import com.account.dto.TransactionRequest;
import com.account.dto.TransactionResponse;
import com.account.model.Transaction;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    // Request -> Entity: names mostly match; BigDecimal -> BigDecimal auto maps.
    Transaction toEntity(TransactionRequest request);

    /* Entity -> Response (minimal: only id/status) */
    @Mappings({
        @Mapping(target = "id",     expression = "java(entity.getTransactionId())"),
        @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    })
    TransactionResponse toResponse(Transaction entity);
    // Helper
    default AmountDTO toAmountDTO(BigDecimal value) {
        if (value == null) return null;
        return AmountDTO.builder().currency("CAD").value(value).build();
    }
}
