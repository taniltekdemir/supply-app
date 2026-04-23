package com.supply.payment.repository;

import com.supply.order.entity.Customer;
import com.supply.payment.entity.CustomerAccount;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, UUID> {

    Optional<CustomerAccount> findByCustomerAndTenant(Customer customer, Tenant tenant);

    List<CustomerAccount> findAllByTenantOrderByBalanceDesc(Tenant tenant);
}
