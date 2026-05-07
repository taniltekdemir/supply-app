package com.supply.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Genel
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Geçersiz istek"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Kayıt bulunamadı"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Yetkilendirme hatası"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok"),

    // Tenant
    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "Hesap bulunamadı"),
    TENANT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Bu e-posta ile kayıtlı hesap var"),

    // Auth
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E-posta veya şifre hatalı"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Oturum süresi doldu"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Geçersiz token"),

    // Customer
    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "Müşteri bulunamadı"),
    CUSTOMER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Bu müşteri zaten kayıtlı"),
    CUSTOMER_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "Müşteri grubu bulunamadı"),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Ürün bulunamadı"),

    // Order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Sipariş bulunamadı"),
    ORDER_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "Sipariş zaten kapatılmış"),
    ORDER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Bu müşteri için bu tarihte zaten sipariş mevcut"),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Sipariş kalemi bulunamadı"),

    // Pricing
    PRICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Bu ürün için fiyat girilmemiş"),

    // Invoice
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Fiş bulunamadı"),
    INVOICE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "Fiş zaten kapatılmış"),
    INVOICE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Bu sipariş için zaten fiş oluşturulmuş"),
    INVOICE_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "Fiş kalemi bulunamadı"),

    // Payment
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Müşteri hesabı bulunamadı");

    private final HttpStatus httpStatus;
    private final String message;
}