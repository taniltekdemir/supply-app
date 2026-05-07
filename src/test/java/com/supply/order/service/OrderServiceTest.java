package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.dto.OrderItemRequest;
import com.supply.order.dto.OrderItemResponse;
import com.supply.order.dto.OrderRequest;
import com.supply.order.dto.OrderResponse;
import com.supply.order.dto.ProductRequest;
import com.supply.order.entity.OrderStatus;
import com.supply.order.entity.Unit;
import com.supply.order.repository.CustomerRepository;
import com.supply.order.repository.OrderItemRepository;
import com.supply.order.repository.OrderRepository;
import com.supply.order.repository.ProductRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = com.supply.supply_app.SupplyAppApplication.class)
@Testcontainers
class OrderServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    private UUID customerId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        RegisterRequest req = new RegisterRequest("Order Tenant", "order@test.com", "password123");
        var auth = authService.register(req);
        TenantContext.set(auth.getTenantId());

        customerId = customerService.create(new CustomerRequest("Test Müşteri", "555-0000", null, null, null)).getId();
        productId = productService.create(new ProductRequest("Domates", Unit.KG, null)).getId();
    }

    @AfterEach
    void tearDown() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void createOrder_whenValidRequest_thenReturnOrderResponse() {
        OrderRequest request = new OrderRequest(customerId, LocalDate.of(2025, 1, 15));

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(response.getOrderDate()).isEqualTo(LocalDate.of(2025, 1, 15));
        assertThat(response.getCustomer().getId()).isEqualTo(customerId);
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void addItemToOrder_whenOrderIsOpen_thenReturnOrderItemResponse() {
        OrderResponse order = orderService.createOrder(new OrderRequest(customerId, LocalDate.now()));
        OrderItemRequest itemRequest = new OrderItemRequest(productId, BigDecimal.valueOf(5.5), "taze olsun");

        OrderItemResponse item = orderService.addItemToOrder(order.getId(), itemRequest);

        assertThat(item.getId()).isNotNull();
        assertThat(item.getProduct().getId()).isEqualTo(productId);
        assertThat(item.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5.5));
        assertThat(item.getNotes()).isEqualTo("taze olsun");
    }

    @Test
    void closeOrder_whenOrderIsOpen_thenStatusIsClosed() {
        OrderResponse order = orderService.createOrder(new OrderRequest(customerId, LocalDate.now()));

        OrderResponse closed = orderService.closeOrder(order.getId());

        assertThat(closed.getStatus()).isEqualTo(OrderStatus.CLOSED);
    }

    @Test
    void closeOrder_whenAlreadyClosed_thenThrowException() {
        OrderResponse order = orderService.createOrder(new OrderRequest(customerId, LocalDate.now()));
        orderService.closeOrder(order.getId());

        assertThatThrownBy(() -> orderService.closeOrder(order.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_ALREADY_CLOSED));
    }

    @Test
    void createOrder_whenSameCustomerAndDateAlreadyExists_thenThrowsOrderAlreadyExists() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        orderService.createOrder(new OrderRequest(customerId, date));

        assertThatThrownBy(() -> orderService.createOrder(new OrderRequest(customerId, date)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ORDER_ALREADY_EXISTS));
    }
}
