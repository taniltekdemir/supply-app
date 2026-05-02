package com.supply.order.entity;

import com.supply.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "customer_groups")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerGroup extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    private String color;

    public void update(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }
}
