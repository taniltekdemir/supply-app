package com.supply.order.controller;

import com.supply.common.response.ApiResponse;
import com.supply.order.dto.CustomerGroupRequest;
import com.supply.order.dto.CustomerGroupResponse;
import com.supply.order.service.CustomerGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer-groups")
@RequiredArgsConstructor
public class CustomerGroupController {

    private final CustomerGroupService customerGroupService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerGroupResponse>> create(
            @Valid @RequestBody CustomerGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(customerGroupService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerGroupResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(customerGroupService.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerGroupResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(customerGroupService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerGroupResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CustomerGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(customerGroupService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        customerGroupService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
