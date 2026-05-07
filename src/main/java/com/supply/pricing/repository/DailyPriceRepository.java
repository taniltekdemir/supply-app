package com.supply.pricing.repository;

import com.supply.order.entity.Product;
import com.supply.pricing.entity.DailyPrice;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyPriceRepository extends JpaRepository<DailyPrice, UUID> {

    List<DailyPrice> findAllByTenantAndDate(Tenant tenant, LocalDate date);

    Optional<DailyPrice> findByTenantAndProductAndDate(Tenant tenant, Product product, LocalDate date);

    List<DailyPrice> findAllByTenantAndDateBetween(Tenant tenant, LocalDate start, LocalDate end);

    boolean existsByTenantAndProductAndDate(Tenant tenant, Product product, LocalDate date);

    Optional<DailyPrice> findTopByTenantAndProductOrderByDateDesc(Tenant tenant, Product product);
}
