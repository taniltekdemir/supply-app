package com.supply.order.repository;

import com.supply.order.entity.Product;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findAllByTenant(Tenant tenant);

    Optional<Product> findByIdAndTenant(UUID id, Tenant tenant);
}
