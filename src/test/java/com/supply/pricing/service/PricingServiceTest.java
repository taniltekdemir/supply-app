package com.supply.pricing.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.ProductRequest;
import com.supply.order.entity.Unit;
import com.supply.order.repository.ProductRepository;
import com.supply.order.service.ProductService;
import com.supply.pricing.dto.DailyPriceRequest;
import com.supply.pricing.dto.DailyPriceResponse;
import com.supply.pricing.repository.DailyPriceRepository;
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
class PricingServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PricingService pricingService;

    @Autowired
    private ProductService productService;

    @Autowired
    private DailyPriceRepository dailyPriceRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuthService authService;

    private UUID productId;
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 4, 15);

    @BeforeEach
    void setUp() {
        RegisterRequest req = new RegisterRequest("Pricing Tenant", "pricing@test.com", "password123");
        var auth = authService.register(req);
        TenantContext.set(auth.getTenantId());

        productId = productService.create(new ProductRequest("Domates", Unit.KG, null)).getId();
    }

    @AfterEach
    void tearDown() {
        dailyPriceRepository.deleteAll();
        productRepository.deleteAll();
        tenantRepository.deleteAll();
        TenantContext.clear();
    }

    @Test
    void createOrUpdate_whenNewPrice_thenSaves() {
        DailyPriceRequest request = new DailyPriceRequest(productId, TEST_DATE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(12.50));

        DailyPriceResponse response = pricingService.createOrUpdate(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getProduct().getId()).isEqualTo(productId);
        assertThat(response.getDate()).isEqualTo(TEST_DATE);
        assertThat(response.getUnitCost()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(response.getSellingPrice()).isEqualByComparingTo(BigDecimal.valueOf(12.50));
    }

    @Test
    void createOrUpdate_whenPriceExists_thenUpdates() {
        DailyPriceRequest initial = new DailyPriceRequest(productId, TEST_DATE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(12.00));
        DailyPriceResponse first = pricingService.createOrUpdate(initial);

        DailyPriceRequest updated = new DailyPriceRequest(productId, TEST_DATE,
                BigDecimal.valueOf(11.00), BigDecimal.valueOf(14.00));
        DailyPriceResponse second = pricingService.createOrUpdate(updated);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getUnitCost()).isEqualByComparingTo(BigDecimal.valueOf(11.00));
        assertThat(second.getSellingPrice()).isEqualByComparingTo(BigDecimal.valueOf(14.00));
        assertThat(dailyPriceRepository.count()).isEqualTo(1);
    }

    @Test
    void getPriceByProductAndDate_whenNotFound_thenThrowsException() {
        UUID randomProductId = productService.create(
                new ProductRequest("Biber", Unit.KG, null)).getId();

        assertThatThrownBy(() -> pricingService.getPriceByProductAndDate(randomProductId, TEST_DATE))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRICE_NOT_FOUND));
    }

    @Test
    void profitMargin_whenCalculated_thenCorrect() {
        // unitCost=10, sellingPrice=12.50 → margin = (2.50/10)*100 = 25.00
        DailyPriceRequest request = new DailyPriceRequest(productId, TEST_DATE,
                BigDecimal.valueOf(10.00), BigDecimal.valueOf(12.50));

        DailyPriceResponse response = pricingService.createOrUpdate(request);

        assertThat(response.getProfitMargin()).isEqualByComparingTo(BigDecimal.valueOf(25.00));
    }
}
