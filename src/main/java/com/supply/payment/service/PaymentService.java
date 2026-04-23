package com.supply.payment.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.invoice.entity.Invoice;
import com.supply.invoice.repository.InvoiceRepository;
import com.supply.payment.dto.PaymentRequest;
import com.supply.payment.dto.PaymentResponse;
import com.supply.payment.dto.PaymentSummaryResponse;
import com.supply.payment.entity.Payment;
import com.supply.payment.entity.PaymentMethod;
import com.supply.payment.repository.PaymentRepository;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;

    public PaymentResponse createPayment(PaymentRequest request) {
        Tenant tenant = currentTenant();
        Invoice invoice = findInvoiceOrThrow(request.getInvoiceId(), tenant);

        boolean isPaid = request.getPaymentMethod() != PaymentMethod.CREDIT;

        Payment payment = Payment.builder()
                .tenant(tenant)
                .invoice(invoice)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .isPaid(isPaid)
                .paymentDate(request.getPaymentDate())
                .build();

        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse markAsPaid(UUID paymentId) {
        Payment payment = findByIdOrThrow(paymentId);

        if (payment.isPaid()) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PAID);
        }

        payment.markAsPaid();
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getByInvoice(UUID invoiceId) {
        Tenant tenant = currentTenant();
        Invoice invoice = findInvoiceOrThrow(invoiceId, tenant);
        return paymentRepository.findAllByTenantAndInvoice(tenant, invoice)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getUnpaidCredits() {
        return paymentRepository.findAllByTenantAndIsPaidFalse(currentTenant())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getDailySummary(LocalDate date) {
        List<Payment> payments = paymentRepository.findAllByTenantAndPaymentDate(currentTenant(), date);

        List<PaymentResponse> paymentResponses = payments.stream()
                .map(this::toResponse)
                .toList();

        BigDecimal cashTotal = sumByMethod(payments, PaymentMethod.CASH);
        BigDecimal transferTotal = sumByMethod(payments, PaymentMethod.TRANSFER);
        BigDecimal creditTotal = sumByMethod(payments, PaymentMethod.CREDIT);
        BigDecimal collectedTotal = cashTotal.add(transferTotal);

        return PaymentSummaryResponse.builder()
                .date(date)
                .cashTotal(cashTotal)
                .transferTotal(transferTotal)
                .creditTotal(creditTotal)
                .collectedTotal(collectedTotal)
                .payments(paymentResponses)
                .build();
    }

    private BigDecimal sumByMethod(List<Payment> payments, PaymentMethod method) {
        return payments.stream()
                .filter(p -> p.getPaymentMethod() == method)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Payment findByIdOrThrow(UUID id) {
        return paymentRepository.findByIdAndTenant(id, currentTenant())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private Invoice findInvoiceOrThrow(UUID invoiceId, Tenant tenant) {
        return invoiceRepository.findByIdAndTenant(invoiceId, tenant)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND));
    }

    private Tenant currentTenant() {
        return tenantRepository.getReferenceById(TenantContext.get());
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .isPaid(payment.isPaid())
                .paymentDate(payment.getPaymentDate())
                .build();
    }
}
