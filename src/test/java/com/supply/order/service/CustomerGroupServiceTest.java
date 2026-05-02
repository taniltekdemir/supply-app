package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerGroupRequest;
import com.supply.order.dto.CustomerGroupResponse;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.dto.CustomerResponse;
import com.supply.order.repository.CustomerGroupRepository;
import com.supply.order.repository.CustomerRepository;
import com.supply.payment.repository.CustomerAccountRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.supply.supply_app.SupplyAppApplication.class)
@Testcontainers
class CustomerGroupServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private CustomerGroupService customerGroupService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerGroupRepository customerGroupRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void setUp() {
        var auth = authService.register(new RegisterRequest("Group Tenant", "group@test.com", "password123"));
        TenantContext.set(auth.getTenantId());
    }

    @AfterEach
    void tearDown() {
        customerAccountRepository.deleteAll();
        customerRepository.deleteAll();
        customerGroupRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void create_whenValidRequest_thenReturnCustomerGroupResponse() {
        CustomerGroupRequest request = new CustomerGroupRequest("VIP Müşteriler", "Özel indirimli grup", "#FFD700");

        CustomerGroupResponse response = customerGroupService.create(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("VIP Müşteriler");
        assertThat(response.getDescription()).isEqualTo("Özel indirimli grup");
        assertThat(response.getColor()).isEqualTo("#FFD700");
        assertThat(response.getCustomerCount()).isZero();
    }

    @Test
    void delete_whenGroupHasCustomers_thenCustomersGroupSetNull() {
        CustomerGroupResponse group = customerGroupService.create(
                new CustomerGroupRequest("Silinecek Grup", null, null));

        customerService.create(new CustomerRequest("Müşteri 1", null, null, null, group.getId()));
        customerService.create(new CustomerRequest("Müşteri 2", null, null, null, group.getId()));

        customerGroupService.delete(group.getId());

        assertThatThrownBy(() -> customerGroupService.getById(group.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CUSTOMER_GROUP_NOT_FOUND));

        List<CustomerResponse> customers = customerService.getAll();
        assertThat(customers).hasSize(2);
        assertThat(customers).allSatisfy(c -> assertThat(c.getGroup()).isNull());
    }
}
