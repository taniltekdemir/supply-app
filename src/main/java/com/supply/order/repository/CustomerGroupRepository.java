package com.supply.order.repository;

import com.supply.order.entity.CustomerGroup;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerGroupRepository extends JpaRepository<CustomerGroup, UUID> {

    List<CustomerGroup> findAllByTenant(Tenant tenant);

    Optional<CustomerGroup> findByIdAndTenant(UUID id, Tenant tenant);
}
