package com.supply.payment.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.invoice.dto.InvoiceRequest;
import com.supply.invoice.dto.InvoiceResponse;
import com.supply.invoice.repository.InvoiceItemRepository;
import com.supply.invoice.repository.InvoiceRepository;
import com.supply.invoice.service.InvoiceService;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.repository.CustomerRepository;
import com.supply.payment.dto.PaymentRequest;
import com.supply.payment.dto.PaymentResponse;
import com.supply.payment.dto.PaymentSummaryResponse;
import com.supply.payment.entity.PaymentMethod;
import com.supply.payment.repository.PaymentRepository;
import com.supply.tenant.dto.RegisterRequest;
import com.supply.tenant.repository.TenantRepository;
import com.supply.tenant.service.AuthService;
import com.supply.order.service.CustomerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.supply.supply_app.SupplyAppApplication.class)
@Testcontainers
class PaymentServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    private UUID invoiceId;
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 4, 16);

    @BeforeEach
    void setUp() {
        var auth = authService.register(new RegisterRequest("Payment Tenant", "payment@test.com", "password123"));
        TenantContext.set(auth.getTenantId());

        UUID customerId = customerService.create(new CustomerRequest("Test Müşteri", "555-0000", null, null)).getId();
        invoiceId = invoiceService.createInvoice(new InvoiceRequest(customerId, TEST_DATE)).getId();
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        invoiceItemRepository.deleteAll();
        invoiceRepository.deleteAll();
        customerRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void createPayment_whenCash_thenIsPaidTrue() {
        PaymentResponse response = paymentService.createPayment(
                new PaymentRequest(invoiceId, PaymentMethod.CASH, BigDecimal.valueOf(150.00), TEST_DATE));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(response.isPaid()).isTrue();
        assertThat(response.getPaymentDate()).isEqualTo(TEST_DATE);
    }

    @Test
    void createPayment_whenCredit_thenIsPaidFalse() {
        PaymentResponse response = paymentService.createPayment(
                new PaymentRequest(invoiceId, PaymentMethod.CREDIT, BigDecimal.valueOf(200.00), TEST_DATE));

        assertThat(response.isPaid()).isFalse();
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT);
    }

    @Test
    void markAsPaid_whenUnpaid_thenSetsIsPaidTrue() {
        PaymentResponse credit = paymentService.createPayment(
                new PaymentRequest(invoiceId, PaymentMethod.CREDIT, BigDecimal.valueOf(100.00), TEST_DATE));

        PaymentResponse result = paymentService.markAsPaid(credit.getId());

        assertThat(result.isPaid()).isTrue();
    }

    @Test
    void markAsPaid_whenAlreadyPaid_thenThrowsException() {
        PaymentResponse cash = paymentService.createPayment(
                new PaymentRequest(invoiceId, PaymentMethod.CASH, BigDecimal.valueOf(100.00), TEST_DATE));

        assertThatThrownBy(() -> paymentService.markAsPaid(cash.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PAYMENT_ALREADY_PAID));
    }

    @Test
    void getUnpaidCredits_whenExists_thenReturnsAll() {
        // İki farklı müşteri için fiş oluştur
        UUID customerId2 = customerService.create(new CustomerRequest("İkinci Müşteri", "555-1111", null, null)).getId();
        UUID invoiceId2 = invoiceService.createInvoice(new InvoiceRequest(customerId2, TEST_DATE)).getId();

        paymentService.createPayment(new PaymentRequest(invoiceId, PaymentMethod.CREDIT, BigDecimal.valueOf(100.00), TEST_DATE));
        paymentService.createPayment(new PaymentRequest(invoiceId2, PaymentMethod.CREDIT, BigDecimal.valueOf(200.00), TEST_DATE));
        paymentService.createPayment(new PaymentRequest(invoiceId, PaymentMethod.CASH, BigDecimal.valueOf(50.00), TEST_DATE));

        List<PaymentResponse> unpaid = paymentService.getUnpaidCredits();

        assertThat(unpaid).hasSize(2);
        assertThat(unpaid).allMatch(p -> !p.isPaid());
    }

    @Test
    void getDailySummary_whenMixedPayments_thenCalculatesCorrectly() {
        UUID customerId2 = customerService.create(new CustomerRequest("İkinci Müşteri", "555-2222", null, null)).getId();
        UUID invoiceId2 = invoiceService.createInvoice(new InvoiceRequest(customerId2, TEST_DATE)).getId();

        paymentService.createPayment(new PaymentRequest(invoiceId, PaymentMethod.CASH, BigDecimal.valueOf(100.00), TEST_DATE));
        paymentService.createPayment(new PaymentRequest(invoiceId2, PaymentMethod.TRANSFER, BigDecimal.valueOf(75.00), TEST_DATE));
        paymentService.createPayment(new PaymentRequest(invoiceId, PaymentMethod.CREDIT, BigDecimal.valueOf(50.00), TEST_DATE));
        // Farklı tarih — özete dahil edilmemeli
        paymentService.createPayment(new PaymentRequest(invoiceId2, PaymentMethod.CASH, BigDecimal.valueOf(999.00), TEST_DATE.plusDays(1)));

        PaymentSummaryResponse summary = paymentService.getDailySummary(TEST_DATE);

        assertThat(summary.getDate()).isEqualTo(TEST_DATE);
        assertThat(summary.getCashTotal()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(summary.getTransferTotal()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
        assertThat(summary.getCreditTotal()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(summary.getCollectedTotal()).isEqualByComparingTo(BigDecimal.valueOf(175.00));
        assertThat(summary.getPayments()).hasSize(3);
    }
}
