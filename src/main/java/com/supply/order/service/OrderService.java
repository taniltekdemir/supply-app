package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.OrderItemRequest;
import com.supply.order.dto.OrderItemResponse;
import com.supply.order.dto.OrderRequest;
import com.supply.order.dto.OrderResponse;
import com.supply.order.entity.Order;
import com.supply.order.entity.OrderItem;
import com.supply.order.entity.OrderStatus;
import com.supply.order.repository.OrderItemRepository;
import com.supply.order.repository.OrderRepository;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TenantRepository tenantRepository;
    private final CustomerService customerService;
    private final ProductService productService;

    public OrderResponse createOrder(OrderRequest request) {
        Tenant tenant = currentTenant();
        var customer = customerService.findByIdOrThrow(request.getCustomerId());

        if (orderRepository.existsByTenantAndCustomerAndOrderDate(tenant, customer, request.getOrderDate())) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_EXISTS);
        }

        Order order = Order.builder()
                .tenant(tenant)
                .customer(customer)
                .orderDate(request.getOrderDate())
                .build();

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public OrderItemResponse addItemToOrder(UUID orderId, OrderItemRequest request) {
        Order order = findOpenOrderOrThrow(orderId);
        var product = productService.findByIdOrThrow(request.getProductId());

        OrderItem item = orderItemRepository.findByOrderAndProduct(order, product)
                .map(existing -> {
                    existing.addQuantity(request.getQuantity());
                    return orderItemRepository.save(existing);
                })
                .orElseGet(() -> {
                    OrderItem newItem = OrderItem.builder()
                            .tenant(currentTenant())
                            .order(order)
                            .product(product)
                            .quantity(request.getQuantity())
                            .notes(request.getNotes())
                            .build();
                    return orderItemRepository.save(newItem);
                });

        return toItemResponse(item);
    }

    public void removeItemFromOrder(UUID orderId, UUID itemId) {
        findOpenOrderOrThrow(orderId);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));

        orderItemRepository.delete(item);
    }

    public OrderResponse closeOrder(UUID orderId) {
        Order order = findOpenOrderOrThrow(orderId);
        order.close();
        return toResponse(orderRepository.save(order));
    }

    public void deleteOrder(UUID orderId) {
        Tenant tenant = currentTenant();
        Order order = orderRepository.findByIdAndTenant(orderId, tenant)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CLOSED);
        }

        orderItemRepository.deleteAllByOrder(order);
        orderRepository.deleteByIdAndTenant(orderId, tenant);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByDate(LocalDate date, UUID groupId) {
        if (groupId != null) {
            return orderRepository.findAllByTenantAndOrderDateAndCustomer_Group_Id(currentTenant(), date, groupId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return orderRepository.findAllByTenantAndOrderDate(currentTenant(), date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOpenOrders() {
        return orderRepository.findAllByTenantAndStatus(currentTenant(), OrderStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Order findOpenOrderOrThrow(UUID orderId) {
        Order order = orderRepository.findByIdAndTenant(orderId, currentTenant())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() == OrderStatus.CLOSED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CLOSED);
        }
        return order;
    }

    private Tenant currentTenant() {
        return tenantRepository.getReferenceById(TenantContext.get());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findAllByOrder(order)
                .stream()
                .map(this::toItemResponse)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .customer(customerService.toResponse(order.getCustomer()))
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .items(items)
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .product(productService.toResponse(item.getProduct()))
                .quantity(item.getQuantity())
                .notes(item.getNotes())
                .build();
    }
}
