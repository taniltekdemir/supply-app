package com.supply.order.repository;

import com.supply.order.entity.Customer;
import com.supply.order.entity.Order;
import com.supply.order.entity.OrderStatus;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByTenantAndOrderDate(Tenant tenant, LocalDate orderDate);

    List<Order> findAllByTenantAndStatus(Tenant tenant, OrderStatus status);

    List<Order> findAllByTenantAndOrderDateAndStatus(Tenant tenant, LocalDate orderDate, OrderStatus status);

    Optional<Order> findByIdAndTenant(UUID id, Tenant tenant);

    void deleteByIdAndTenant(UUID id, Tenant tenant);

    List<Order> findAllByTenantAndOrderDateAndCustomer_Group_Id(
            Tenant tenant, LocalDate orderDate, UUID groupId);

    boolean existsByTenantAndCustomerAndOrderDate(Tenant tenant, Customer customer, LocalDate orderDate);
}
