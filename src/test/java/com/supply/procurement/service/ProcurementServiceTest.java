package com.supply.procurement.service;

import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.CustomerRequest;
import com.supply.order.dto.OrderItemRequest;
import com.supply.order.dto.OrderRequest;
import com.supply.order.dto.ProductRequest;
import com.supply.order.entity.Unit;
import com.supply.order.repository.CustomerRepository;
import com.supply.order.repository.OrderItemRepository;
import com.supply.order.repository.OrderRepository;
import com.supply.order.repository.ProductRepository;
import com.supply.order.service.CustomerService;
import com.supply.order.service.OrderService;
import com.supply.order.service.ProductService;
import com.supply.pricing.dto.DailyPriceRequest;
import com.supply.pricing.repository.DailyPriceRepository;
import com.supply.pricing.service.PricingService;
import com.supply.procurement.dto.ProcurementSummary;
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

@SpringBootTest(classes = com.supply.supply_app.SupplyAppApplication.class)
@Testcontainers
class ProcurementServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ProcurementService procurementService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 4, 15);

    private UUID customer1Id;
    private UUID customer2Id;
    private UUID pricedProductId;
    private UUID unpricedProductId;

    @BeforeEach
    void setUp() {
        RegisterRequest req = new RegisterRequest("Proc Tenant", "proc@test.com", "password123");
        var auth = authService.register(req);
        TenantContext.set(auth.getTenantId());

        customer1Id = customerService.create(new CustomerRequest("Müşteri 1", null, null, null)).getId();
        customer2Id = customerService.create(new CustomerRequest("Müşteri 2", null, null, null)).getId();
        pricedProductId = productService.create(new ProductRequest("Domates", Unit.KG, null)).getId();
        unpricedProductId = productService.create(new ProductRequest("Biber", Unit.KG, null)).getId();

        // Sadece Domates için fiyat girildi
        pricingService.createOrUpdate(new DailyPriceRequest(
                pricedProductId, TEST_DATE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(12.50)));
    }

    @AfterEach
    void tearDown() {
        dailyPriceRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        customerRepository.deleteAll();
        productRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void getDailySummary_whenMultipleOrdersSameProduct_thenQuantitiesAggregated() {
        // Müşteri 1: 5 kg Domates
        var order1 = orderService.createOrder(new OrderRequest(customer1Id, TEST_DATE));
        orderService.addItemToOrder(order1.getId(),
                new OrderItemRequest(pricedProductId, BigDecimal.valueOf(5), null));

        // Müşteri 2: 3 kg Domates
        var order2 = orderService.createOrder(new OrderRequest(customer2Id, TEST_DATE));
        orderService.addItemToOrder(order2.getId(),
                new OrderItemRequest(pricedProductId, BigDecimal.valueOf(3), null));

        ProcurementSummary summary = procurementService.getDailySummary(TEST_DATE);

        assertThat(summary.getItems()).hasSize(1);
        assertThat(summary.getItems().get(0).getTotalQuantity())
                .isEqualByComparingTo(BigDecimal.valueOf(8));
        assertThat(summary.getItems().get(0).isHasPrice()).isTrue();
        // totalCost = 8 * 10.00 = 80.00
        assertThat(summary.getTotalCost()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
    }

    @Test
    void getDailySummary_whenNoPriceForProduct_thenHasPriceFalse() {
        var order = orderService.createOrder(new OrderRequest(customer1Id, TEST_DATE));
        orderService.addItemToOrder(order.getId(),
                new OrderItemRequest(unpricedProductId, BigDecimal.valueOf(4), null));

        ProcurementSummary summary = procurementService.getDailySummary(TEST_DATE);

        assertThat(summary.getItems()).hasSize(1);
        assertThat(summary.getItems().get(0).isHasPrice()).isFalse();
        assertThat(summary.getItems().get(0).getUnitCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getConsolidatedList_whenCalled_thenOnlyPricedItems() {
        var order = orderService.createOrder(new OrderRequest(customer1Id, TEST_DATE));
        // Fiyatlı ürün
        orderService.addItemToOrder(order.getId(),
                new OrderItemRequest(pricedProductId, BigDecimal.valueOf(5), null));
        // Fiyatsız ürün
        orderService.addItemToOrder(order.getId(),
                new OrderItemRequest(unpricedProductId, BigDecimal.valueOf(2), null));

        ProcurementSummary consolidated = procurementService.getConsolidatedList(TEST_DATE);

        assertThat(consolidated.getItems()).hasSize(1);
        assertThat(consolidated.getItems().get(0).getProduct().getId()).isEqualTo(pricedProductId);
        assertThat(consolidated.getItems().get(0).isHasPrice()).isTrue();
    }
}