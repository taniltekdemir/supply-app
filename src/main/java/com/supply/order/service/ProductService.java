package com.supply.order.service;

import com.supply.common.exception.BusinessException;
import com.supply.common.exception.ErrorCode;
import com.supply.common.tenant.TenantContext;
import com.supply.order.dto.ProductRequest;
import com.supply.order.dto.ProductResponse;
import com.supply.order.entity.Product;
import com.supply.order.repository.ProductRepository;
import com.supply.tenant.entity.Tenant;
import com.supply.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;

    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .tenant(currentTenant())
                .name(request.getName())
                .unit(request.getUnit())
                .description(request.getDescription())
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAllByTenant(currentTenant())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return toResponse(findByIdOrThrow(id));
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = findByIdOrThrow(id);
        product.update(request.getName(), request.getUnit(), request.getDescription());
        return toResponse(productRepository.save(product));
    }

    public void delete(UUID id) {
        Product product = findByIdOrThrow(id);
        productRepository.delete(product);
    }

    public Product findByIdOrThrow(UUID id) {
        return productRepository.findByIdAndTenant(id, currentTenant())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private Tenant currentTenant() {
        return tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .unit(product.getUnit())
                .description(product.getDescription())
                .build();
    }
}
