package com.supply.invoice.repository;

import com.supply.invoice.entity.Invoice;
import com.supply.invoice.entity.InvoiceStatus;
import com.supply.order.entity.Customer;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndTenant(UUID id, Tenant tenant);

    List<Invoice> findAllByTenantAndStatus(Tenant tenant, InvoiceStatus status);

    List<Invoice> findAllByTenantAndInvoiceDate(Tenant tenant, LocalDate date);

    List<Invoice> findAllByTenantAndInvoiceDateBetween(Tenant tenant, LocalDate start, LocalDate end);

    List<Invoice> findAllByTenantAndCustomerAndInvoiceDate(Tenant tenant, Customer customer, LocalDate date);
}
