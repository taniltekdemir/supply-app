package com.supply.order.repository;

import com.supply.order.entity.Customer;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findAllByTenant(Tenant tenant);

    Optional<Customer> findByIdAndTenant(UUID id, Tenant tenant);

    boolean existsByNameAndTenant(String name, Tenant tenant);
}
