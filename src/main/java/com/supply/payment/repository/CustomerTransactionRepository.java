package com.supply.payment.repository;

import com.supply.order.entity.Customer;
import com.supply.payment.entity.CustomerTransaction;
import com.supply.payment.entity.TransactionType;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, UUID> {

    List<CustomerTransaction> findAllByCustomerAndTenant(Customer customer, Tenant tenant);

    List<CustomerTransaction> findAllByTenantAndDate(Tenant tenant, LocalDate date);

    List<CustomerTransaction> findAllByTenantAndDateBetween(Tenant tenant, LocalDate start, LocalDate end);

    List<CustomerTransaction> findAllByTenantAndType(Tenant tenant, TransactionType type);
}
