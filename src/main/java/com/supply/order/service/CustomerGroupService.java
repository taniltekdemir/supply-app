package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerGroupRequest;
import com.supply.order.dto.CustomerGroupResponse;
import com.supply.order.entity.Customer;
import com.supply.order.entity.CustomerGroup;
import com.supply.order.repository.CustomerGroupRepository;
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
public class CustomerGroupService {

    private final CustomerGroupRepository customerGroupRepository;
    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    public CustomerGroupResponse create(CustomerGroupRequest request) {
        Tenant tenant = currentTenant();
        CustomerGroup group = customerGroupRepository.save(
                CustomerGroup.builder()
                        .tenant(tenant)
                        .name(request.getName())
                        .description(request.getDescription())
                        .color(request.getColor())
                        .build());
        return toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<CustomerGroupResponse> getAll() {
        return customerGroupRepository.findAllByTenant(currentTenant())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerGroupResponse getById(UUID id) {
        return toResponse(findByIdOrThrow(id));
    }

    public CustomerGroupResponse update(UUID id, CustomerGroupRequest request) {
        CustomerGroup group = findByIdOrThrow(id);
        group.update(request.getName(), request.getDescription(), request.getColor());
        return toResponse(customerGroupRepository.save(group));
    }

    public void delete(UUID id) {
        Tenant tenant = currentTenant();
        CustomerGroup group = findByIdOrThrow(id);

        List<Customer> members = customerRepository.findAllByTenantAndGroup(tenant, group);
        members.forEach(c -> c.assignGroup(null));
        customerRepository.saveAll(members);

        customerGroupRepository.delete(group);
    }

    public CustomerGroup findByIdOrThrow(UUID id) {
        return customerGroupRepository.findByIdAndTenant(id, currentTenant())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_GROUP_NOT_FOUND));
    }

    private Tenant currentTenant() {
        return tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
    }

    public CustomerGroupResponse toResponse(CustomerGroup group) {
        long count = customerRepository.countByGroupAndTenant(group, currentTenant());
        return CustomerGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .color(group.getColor())
                .customerCount(count)
                .build();
    }
}
