package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.dto.CustomerResponse;
import com.supply.order.entity.Customer;
import com.supply.order.repository.CustomerRepository;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    public CustomerResponse create(CustomerRequest request) {
        Tenant tenant = currentTenant();

        if (customerRepository.existsByNameAndTenant(request.getName(), tenant)) {
            throw new BusinessException(ErrorCode.CUSTOMER_ALREADY_EXISTS);
        }

        Customer customer = Customer.builder()
                .tenant(tenant)
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .notes(request.getNotes())
                .build();

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getAll() {
        return customerRepository.findAllByTenant(currentTenant())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return toResponse(findByIdOrThrow(id));
    }

    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = findByIdOrThrow(id);
        customer.update(request.getName(), request.getPhone(), request.getAddress(), request.getNotes());
        return toResponse(customerRepository.save(customer));
    }

    public void delete(UUID id) {
        Customer customer = findByIdOrThrow(id);
        customerRepository.delete(customer);
    }

    public Customer findByIdOrThrow(UUID id) {
        return customerRepository.findByIdAndTenant(id, currentTenant())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private Tenant currentTenant() {
        return tenantRepository.getReferenceById(TenantContext.get());
    }

    public CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .notes(customer.getNotes())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
