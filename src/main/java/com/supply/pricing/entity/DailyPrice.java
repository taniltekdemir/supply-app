package com.supply.pricing.entity;

import com.supply.common.entity.BaseEntity;
import com.supply.order.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "daily_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_price",
                columnNames = {"tenant_id", "product_id", "date"}
        )
)
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPrice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    public void updatePrices(BigDecimal unitCost, BigDecimal sellingPrice) {
        this.unitCost = unitCost;
        this.sellingPrice = sellingPrice;
    }
}
