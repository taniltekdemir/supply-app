package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerGroupResponse;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.dto.CustomerResponse;
import com.supply.order.entity.Customer;
import com.supply.order.entity.CustomerGroup;
import com.supply.order.repository.CustomerGroupRepository;
import com.supply.order.repository.CustomerRepository;
import com.supply.payment.entity.CustomerAccount;
import com.supply.payment.repository.CustomerAccountRepository;
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
    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerGroupRepository customerGroupRepository;
    private final TenantRepository tenantRepository;

    public CustomerResponse create(CustomerRequest request) {
        Tenant tenant = currentTenant();

        if (customerRepository.existsByNameAndTenant(request.getName(), tenant)) {
            throw new BusinessException(ErrorCode.CUSTOMER_ALREADY_EXISTS);
        }

        CustomerGroup group = resolveGroup(request.getGroupId(), tenant);

        Customer customer = customerRepository.save(Customer.builder()
                .tenant(tenant)
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .notes(request.getNotes())
                .group(group)
                .build());

        customerAccountRepository.save(CustomerAccount.builder()
                .tenant(tenant)
                .customer(customer)
                .build());

        return toResponse(customer);
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
        Tenant tenant = currentTenant();
        Customer customer = findByIdOrThrow(id);
        CustomerGroup group = resolveGroup(request.getGroupId(), tenant);

        customer.update(request.getName(), request.getPhone(), request.getAddress(), request.getNotes());
        customer.assignGroup(group);

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

    private CustomerGroup resolveGroup(UUID groupId, Tenant tenant) {
        if (groupId == null) {
            return null;
        }
        return customerGroupRepository.findByIdAndTenant(groupId, tenant)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_GROUP_NOT_FOUND));
    }

    public CustomerResponse toResponse(Customer customer) {
        CustomerGroupResponse groupResponse = null;
        if (customer.getGroup() != null) {
            CustomerGroup g = customer.getGroup();
            groupResponse = CustomerGroupResponse.builder()
                    .id(g.getId())
                    .name(g.getName())
                    .description(g.getDescription())
                    .color(g.getColor())
                    .customerCount(0L)
                    .build();
        }

        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .notes(customer.getNotes())
                .group(groupResponse)
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
