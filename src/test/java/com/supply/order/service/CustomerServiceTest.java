package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.dto.CustomerResponse;
import com.supply.order.repository.CustomerRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.supply.supply_app.SupplyAppApplication.class)
@Testcontainers
class CustomerServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        RegisterRequest req = new RegisterRequest("Test Tenant", "tenant@test.com", "password123");
        var auth = authService.register(req);
        TenantContext.set(auth.getTenantId());
    }

    @AfterEach
    void tearDown() {
        customerRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void create_whenValidRequest_thenReturnCustomerResponse() {
        CustomerRequest request = new CustomerRequest("Ahmet Yılmaz", "555-1234", "İstanbul", null, null);

        CustomerResponse response = customerService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("Ahmet Yılmaz");
        assertThat(response.getPhone()).isEqualTo("555-1234");
    }

    @Test
    void create_whenNameAlreadyExists_thenThrowBusinessException() {
        customerService.create(new CustomerRequest("Ahmet Yılmaz", null, null, null, null));

        assertThatThrownBy(() -> customerService.create(new CustomerRequest("Ahmet Yılmaz", "555-9999", null, null, null)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CUSTOMER_ALREADY_EXISTS));
    }

    @Test
    void getAll_whenCustomersExist_thenReturnList() {
        customerService.create(new CustomerRequest("Müşteri A", null, null, null, null));
        customerService.create(new CustomerRequest("Müşteri B", null, null, null, null));

        List<CustomerResponse> result = customerService.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void getById_whenCustomerExists_thenReturnResponse() {
        CustomerResponse created = customerService.create(new CustomerRequest("Test Müşteri", "555-0000", null, null, null));

        CustomerResponse found = customerService.getById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getName()).isEqualTo("Test Müşteri");
    }

    @Test
    void getById_whenNotFound_thenThrowBusinessException() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> customerService.getById(randomId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND));
    }
}
