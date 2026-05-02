package com.supply.order.repository;

import com.supply.order.entity.Order;
import com.supply.order.entity.OrderItem;
import com.supply.order.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findAllByOrder(Order order);

    Optional<OrderItem> findByOrderAndProduct(Order order, Product product);

    void deleteAllByOrder(Order order);
}
