package com.digitalbank.customerservice.mapper;

import com.account.dto.CustomerResponse;
import com.digitalbank.customerservice.dto.*;
import com.digitalbank.customerservice.model.Customer;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toEntity(CustomerRequest request);

    CustomerResponse toResponse(Customer customer);
    
    CustomerCreatedResponse toCreateResponse(Customer customer);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCustomerFromRequest(UpdateCustomerRequest request, @MappingTarget Customer customer);

}
