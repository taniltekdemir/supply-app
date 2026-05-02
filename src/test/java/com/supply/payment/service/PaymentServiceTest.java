package com.supply.payment.service;

import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.repository.CustomerRepository;
import com.supply.order.service.CustomerService;
import com.supply.payment.dto.AddDebtRequest;
import com.supply.payment.dto.AddPaymentRequest;
import com.supply.payment.dto.CustomerAccountResponse;
import com.supply.payment.dto.CustomerTransactionResponse;
import com.supply.payment.dto.DailySummaryResponse;
import com.supply.payment.entity.PaymentMethod;
import com.supply.payment.entity.TransactionType;
import com.supply.payment.repository.CustomerAccountRepository;
import com.supply.payment.repository.CustomerTransactionRepository;
import com.supply.tenant.dto.RegisterRequest;
import com.supply.tenant.repository.TenantRepository;
import com.supply.tenant.service.AuthService;
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

@SpringBootTest(classes = com.supply.supply_app.SupplyAppApplication.class)
@Testcontainers
class PaymentServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private CustomerTransactionRepository customerTransactionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    private UUID customerId;
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 4, 16);

    @BeforeEach
    void setUp() {
        var auth = authService.register(new RegisterRequest("Payment Tenant", "payment@test.com", "password123"));
        TenantContext.set(auth.getTenantId());
        customerId = customerService.create(new CustomerRequest("Test Müşteri", "555-0000", null, null, null)).getId();
    }

    @AfterEach
    void tearDown() {
        customerTransactionRepository.deleteAll();
        customerAccountRepository.deleteAll();
        customerRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void addDebt_whenCalled_thenBalanceIncreases() {
        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(150.00), TEST_DATE, null, null));

        CustomerAccountResponse account = paymentService.getAccountByCustomer(customerId);

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(account.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void addPayment_whenCalled_thenBalanceDecreases() {
        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(200.00), TEST_DATE, null, null));
        paymentService.addPayment(new AddPaymentRequest(customerId, BigDecimal.valueOf(80.00), TEST_DATE, PaymentMethod.CASH, null));

        CustomerAccountResponse account = paymentService.getAccountByCustomer(customerId);

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(120.00));
    }

    @Test
    void addPayment_whenExceedsDebt_thenBalanceNegative() {
        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(50.00), TEST_DATE, null, null));
        paymentService.addPayment(new AddPaymentRequest(customerId, BigDecimal.valueOf(100.00), TEST_DATE, PaymentMethod.BANK_TRANSFER, null));

        CustomerAccountResponse account = paymentService.getAccountByCustomer(customerId);

        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(-50.00));
    }

    @Test
    void getDailySummary_whenMixedTransactions_thenCalculatesCorrectly() {
        UUID customerId2 = customerService.create(new CustomerRequest("İkinci Müşteri", "555-1111", null, null, null)).getId();

        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(200.00), TEST_DATE, null, null));
        paymentService.addDebt(new AddDebtRequest(customerId2, BigDecimal.valueOf(150.00), TEST_DATE, null, null));
        paymentService.addPayment(new AddPaymentRequest(customerId, BigDecimal.valueOf(50.00), TEST_DATE, PaymentMethod.CASH, null));
        paymentService.addPayment(new AddPaymentRequest(customerId2, BigDecimal.valueOf(75.00), TEST_DATE, PaymentMethod.BANK_TRANSFER, null));
        // farklı tarih — özete dahil edilmemeli
        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(999.00), TEST_DATE.plusDays(1), null, null));

        DailySummaryResponse summary = paymentService.getDailySummary(TEST_DATE);

        assertThat(summary.getDate()).isEqualTo(TEST_DATE);
        assertThat(summary.getTotalNewDebt()).isEqualByComparingTo(BigDecimal.valueOf(350.00));
        assertThat(summary.getTotalSales()).isEqualByComparingTo(BigDecimal.valueOf(350.00));
        assertThat(summary.getTotalCash()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(summary.getTotalBankTransfer()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
        assertThat(summary.getTotalCreditCard()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalCollected()).isEqualByComparingTo(BigDecimal.valueOf(125.00));
        // openDebtBalance: (200-50) + (150-75) + 999 = 150 + 75 + 999 = 1224
        assertThat(summary.getOpenDebtBalance()).isEqualByComparingTo(BigDecimal.valueOf(1224.00));
    }

    @Test
    void getAllAccounts_whenCalled_thenSortedByBalanceDesc() {
        UUID customerId2 = customerService.create(new CustomerRequest("İkinci Müşteri", "555-1111", null, null, null)).getId();
        UUID customerId3 = customerService.create(new CustomerRequest("Üçüncü Müşteri", "555-2222", null, null, null)).getId();

        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(100.00), TEST_DATE, null, null));
        paymentService.addDebt(new AddDebtRequest(customerId2, BigDecimal.valueOf(300.00), TEST_DATE, null, null));
        paymentService.addDebt(new AddDebtRequest(customerId3, BigDecimal.valueOf(50.00), TEST_DATE, null, null));

        List<CustomerAccountResponse> accounts = paymentService.getAllAccounts();

        assertThat(accounts).hasSize(3);
        assertThat(accounts.get(0).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(300.00));
        assertThat(accounts.get(1).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        assertThat(accounts.get(2).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    void addDebt_withInvoiceId_thenTransactionLinkedToInvoice() {
        CustomerTransactionResponse response = paymentService.addDebt(
                new AddDebtRequest(customerId, BigDecimal.valueOf(75.00), TEST_DATE, null, "test borç"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getType()).isEqualTo(TransactionType.DEBT);
        assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
        assertThat(response.getCustomer().getId()).isEqualTo(customerId);
        assertThat(response.getNotes()).isEqualTo("test borç");
    }

    @Test
    void getTransactionsByCustomer_whenMultipleTransactions_thenReturnsAll() {
        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(100.00), TEST_DATE, null, null));
        paymentService.addPayment(new AddPaymentRequest(customerId, BigDecimal.valueOf(40.00), TEST_DATE, PaymentMethod.CASH, null));
        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(60.00), TEST_DATE.plusDays(1), null, null));

        List<CustomerTransactionResponse> transactions = paymentService.getTransactionsByCustomer(customerId);

        assertThat(transactions).hasSize(3);
    }

    @Test
    void getTransactionsByDate_whenQueried_thenOnlyMatchingDate() {
        UUID customerId2 = customerService.create(new CustomerRequest("İkinci Müşteri", "555-1111", null, null, null)).getId();

        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(100.00), TEST_DATE, null, null));
        paymentService.addDebt(new AddDebtRequest(customerId2, BigDecimal.valueOf(200.00), TEST_DATE, null, null));
        paymentService.addDebt(new AddDebtRequest(customerId, BigDecimal.valueOf(999.00), TEST_DATE.plusDays(1), null, null));

        List<CustomerTransactionResponse> result = paymentService.getTransactionsByDate(TEST_DATE);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getDate().isEqual(TEST_DATE));
    }
}
