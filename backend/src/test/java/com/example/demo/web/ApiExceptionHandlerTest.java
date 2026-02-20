package com.example.demo.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class ApiExceptionHandlerTest {

    @Test
    void handleValidation_whenDuplicateFieldErrors_keepsFirst() throws Exception {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "req");
        bindingResult.addError(new FieldError("req", "code", "must not be blank"));
        bindingResult.addError(new FieldError("req", "code", "size must be <= 64"));

        Method method = ApiExceptionHandlerTest.class.getDeclaredMethod("dummy", Object.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<?> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        assertThat(body.get("message")).isEqualTo("validation failed");
        assertThat(body.get("errors")).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertThat(errors).containsEntry("code", "must not be blank");
    }

    @SuppressWarnings("unused")
    private static void dummy(Object ignored) {
        // used for MethodParameter construction
    }
}

