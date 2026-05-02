package com.supply.order.entity;

import com.supply.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "customers")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String phone;

    private String address;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private CustomerGroup group;

    public void update(String name, String phone, String address, String notes) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.notes = notes;
    }

    public void assignGroup(CustomerGroup group) {
        this.group = group;
    }
}
