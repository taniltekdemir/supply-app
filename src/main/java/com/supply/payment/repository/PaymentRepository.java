package com.supply.payment.repository;

import com.supply.invoice.entity.Invoice;
import com.supply.payment.entity.Payment;
import com.supply.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndTenant(UUID id, Tenant tenant);

    List<Payment> findAllByTenantAndInvoice(Tenant tenant, Invoice invoice);

    List<Payment> findAllByTenantAndIsPaidFalse(Tenant tenant);

    List<Payment> findAllByTenantAndPaymentDate(Tenant tenant, LocalDate date);
}
