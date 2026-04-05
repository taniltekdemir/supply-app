package com.supply.common.exception;

import com.supply.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_whenNotFound_thenReturns404() {
        BusinessException ex = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    void handleBusinessException_whenConflict_thenReturns409() {
        BusinessException ex = new BusinessException(ErrorCode.TENANT_ALREADY_EXISTS);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.TENANT_ALREADY_EXISTS.getMessage());
    }

    @Test
    void handleBusinessException_whenCustomMessage_thenReturnsCustomMessage() {
        String customMessage = "Özel hata mesajı";
        BusinessException ex = new BusinessException(ErrorCode.VALIDATION_ERROR, customMessage);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo(customMessage);
    }

    @Test
    void handleValidationException_whenSingleFieldInvalid_thenReturns400WithMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("dto", "email", "E-posta boş olamaz");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("E-posta boş olamaz");
    }

    @Test
    void handleValidationException_whenMultipleFieldsInvalid_thenJoinsMessages() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> fieldErrors = List.of(
                new FieldError("dto", "email", "E-posta boş olamaz"),
                new FieldError("dto", "name", "Ad boş olamaz")
        );
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("E-posta boş olamaz", "Ad boş olamaz");
    }

    @Test
    void handleAccessDeniedException_whenCalled_thenReturns403() {
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDeniedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    void handleException_whenUnexpectedError_thenReturns500() {
        Exception ex = new RuntimeException("Beklenmedik hata");

        ResponseEntity<ApiResponse<Void>> response = handler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
    }
}
