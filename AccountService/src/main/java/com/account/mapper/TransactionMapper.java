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

    /* Entity -> Response. type and occurredAt are what let a caller tell a hold
     * apart from the debit that follows it; both are enums/timestamps on the entity. */
    @Mappings({
        @Mapping(target = "id",         expression = "java(entity.getTransactionId())"),
        @Mapping(target = "type",       expression = "java(entity.getType() == null ? null : entity.getType().name())"),
        @Mapping(target = "status",     expression = "java(entity.getStatus() == null ? null : entity.getStatus().name())"),
        @Mapping(target = "occurredAt", expression = "java(entity.getOccurredAt() != null ? entity.getOccurredAt() : entity.getCreatedAt())")
    })
    TransactionResponse toResponse(Transaction entity);
    // Helper
    default AmountDTO toAmountDTO(BigDecimal value) {
        if (value == null) return null;
        return AmountDTO.builder().currency("CAD").value(value).build();
    }
}
