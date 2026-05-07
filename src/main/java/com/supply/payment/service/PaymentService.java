package com.supply.payment.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.invoice.entity.Invoice;
import com.supply.invoice.repository.InvoiceRepository;
import com.supply.order.entity.Customer;
import com.supply.order.service.CustomerService;
import com.supply.payment.dto.AddDebtRequest;
import com.supply.payment.dto.AddPaymentRequest;
import com.supply.payment.dto.CustomerAccountResponse;
import com.supply.payment.dto.CustomerTransactionResponse;
import com.supply.payment.dto.DailySummaryResponse;
import com.supply.payment.entity.CustomerAccount;
import com.supply.payment.entity.CustomerTransaction;
import com.supply.payment.entity.PaymentMethod;
import com.supply.payment.entity.TransactionType;
import com.supply.payment.repository.CustomerAccountRepository;
import com.supply.payment.repository.CustomerTransactionRepository;
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

    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerTransactionRepository customerTransactionRepository;
    private final CustomerService customerService;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;

    public CustomerTransactionResponse addDebt(AddDebtRequest request) {
        Tenant tenant = currentTenant();
        Customer customer = customerService.findByIdOrThrow(request.getCustomerId());
        CustomerAccount account = findAccountOrThrow(customer, tenant);
        Invoice invoice = resolveInvoice(request.getInvoiceId(), tenant);

        CustomerTransaction transaction = CustomerTransaction.builder()
                .tenant(tenant)
                .customer(customer)
                .type(TransactionType.DEBT)
                .amount(request.getAmount())
                .date(request.getDate())
                .invoice(invoice)
                .notes(request.getNotes())
                .build();

        customerTransactionRepository.save(transaction);
        account.addDebt(request.getAmount());
        customerAccountRepository.save(account);

        return toTransactionResponse(transaction);
    }

    public CustomerTransactionResponse addPayment(AddPaymentRequest request) {
        Tenant tenant = currentTenant();
        Customer customer = customerService.findByIdOrThrow(request.getCustomerId());
        CustomerAccount account = findAccountOrThrow(customer, tenant);

        CustomerTransaction transaction = CustomerTransaction.builder()
                .tenant(tenant)
                .customer(customer)
                .type(TransactionType.PAYMENT)
                .amount(request.getAmount())
                .date(request.getDate())
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        customerTransactionRepository.save(transaction);
        account.addPayment(request.getAmount());
        customerAccountRepository.save(account);

        return toTransactionResponse(transaction);
    }

    @Transactional(readOnly = true)
    public CustomerAccountResponse getAccountByCustomer(UUID customerId) {
        Customer customer = customerService.findByIdOrThrow(customerId);
        CustomerAccount account = findAccountOrThrow(customer, currentTenant());
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public List<CustomerAccountResponse> getAllAccounts() {
        return customerAccountRepository.findAllByTenantOrderByBalanceDesc(currentTenant())
                .stream()
                .map(this::toAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerTransactionResponse> getTransactionsByCustomer(UUID customerId) {
        Customer customer = customerService.findByIdOrThrow(customerId);
        return customerTransactionRepository.findAllByCustomerAndTenant(customer, currentTenant())
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerTransactionResponse> getTransactionsByDate(LocalDate date) {
        return customerTransactionRepository.findAllByTenantAndDate(currentTenant(), date)
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DailySummaryResponse getDailySummary(LocalDate date) {
        Tenant tenant = currentTenant();
        List<CustomerTransaction> transactions = customerTransactionRepository.findAllByTenantAndDate(tenant, date);

        List<CustomerTransaction> debts = transactions.stream()
                .filter(t -> t.getType() == TransactionType.DEBT)
                .toList();

        List<CustomerTransaction> payments = transactions.stream()
                .filter(t -> t.getType() == TransactionType.PAYMENT)
                .toList();

        BigDecimal totalNewDebt = sum(debts);
        BigDecimal totalCash = sumByMethod(payments, PaymentMethod.CASH);
        BigDecimal totalBankTransfer = sumByMethod(payments, PaymentMethod.BANK_TRANSFER);
        BigDecimal totalCreditCard = sumByMethod(payments, PaymentMethod.CREDIT_CARD);
        BigDecimal totalCollected = totalCash.add(totalBankTransfer).add(totalCreditCard);

        BigDecimal openDebtBalance = customerAccountRepository.findAllByTenantOrderByBalanceDesc(tenant)
                .stream()
                .map(CustomerAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DailySummaryResponse.builder()
                .date(date)
                .totalSales(totalNewDebt)
                .totalNewDebt(totalNewDebt)
                .totalCash(totalCash)
                .totalBankTransfer(totalBankTransfer)
                .totalCreditCard(totalCreditCard)
                .totalCollected(totalCollected)
                .openDebtBalance(openDebtBalance)
                .build();
    }

    private CustomerAccount findAccountOrThrow(Customer customer, Tenant tenant) {
        return customerAccountRepository.findByCustomerAndTenant(customer, tenant)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private Invoice resolveInvoice(UUID invoiceId, Tenant tenant) {
        if (invoiceId == null) return null;
        return invoiceRepository.findByIdAndTenant(invoiceId, tenant)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVOICE_NOT_FOUND));
    }

    private BigDecimal sum(List<CustomerTransaction> transactions) {
        return transactions.stream()
                .map(CustomerTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumByMethod(List<CustomerTransaction> transactions, PaymentMethod method) {
        return transactions.stream()
                .filter(t -> t.getPaymentMethod() == method)
                .map(CustomerTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Tenant currentTenant() {
        return tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
    }

    private CustomerAccountResponse toAccountResponse(CustomerAccount account) {
        return CustomerAccountResponse.builder()
                .customerId(account.getCustomer().getId())
                .customerName(account.getCustomer().getName())
                .balance(account.getBalance())
                .lastUpdated(account.getLastUpdated())
                .build();
    }

    private CustomerTransactionResponse toTransactionResponse(CustomerTransaction transaction) {
        return CustomerTransactionResponse.builder()
                .id(transaction.getId())
                .customer(customerService.toResponse(transaction.getCustomer()))
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .date(transaction.getDate())
                .paymentMethod(transaction.getPaymentMethod())
                .invoiceId(transaction.getInvoice() != null ? transaction.getInvoice().getId() : null)
                .notes(transaction.getNotes())
                .build();
    }
}
