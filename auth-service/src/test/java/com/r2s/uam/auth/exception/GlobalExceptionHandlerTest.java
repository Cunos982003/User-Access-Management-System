package com.r2s.uam.auth.exception;

import com.r2s.uam.auth.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("should return 404 with correct response body")
        void shouldReturn404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleResourceNotFoundException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getStatusCode()).isEqualTo(404);
            assertThat(response.getBody().getMessage()).isEqualTo("User not found");
        }
    }

    @Nested
    @DisplayName("BadRequestException")
    class BadRequestTests {

        @Test
        @DisplayName("should return 400 with correct response body")
        void shouldReturn400() {
            BadRequestException ex = new BadRequestException("Invalid input");
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleBadRequestException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getStatusCode()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid input");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedTests {

        @Test
        @DisplayName("should return 401 with correct response body")
        void shouldReturn401() {
            UnauthorizedException ex = new UnauthorizedException("Unauthorized access");
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleUnauthorizedException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getStatusCode()).isEqualTo(401);
            assertThat(response.getBody().getMessage()).isEqualTo("Unauthorized access");
        }
    }

    @Nested
    @DisplayName("BadCredentialsException")
    class BadCredentialsTests {

        @Test
        @DisplayName("should return 401 with generic message")
        void shouldReturn401WithGenericMessage() {
            BadCredentialsException ex = new BadCredentialsException("specific technical message");
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleBadCredentialsException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(401);
            // The generic message is returned, not the specific one
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid username or password");
        }
    }

    @Nested
    @DisplayName("AuthenticationException")
    class AuthenticationExceptionTests {

        @Test
        @DisplayName("should return 401 with generic auth failed message")
        void shouldReturn401AuthFailed() {
            AuthenticationException ex = mock(AuthenticationException.class);
            when(ex.getMessage()).thenReturn("specific auth message");
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleAuthenticationException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(401);
            assertThat(response.getBody().getMessage()).isEqualTo("Authentication failed");
        }
    }

    @Nested
    @DisplayName("AccessDeniedException")
    class AccessDeniedTests {

        @Test
        @DisplayName("should return 403 with access denied message")
        void shouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("Access denied message");
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleAccessDeniedException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getStatusCode()).isEqualTo(403);
            assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException (validation errors)")
    class ValidationExceptionTests {

        @Test
        @DisplayName("should return 400 with field-level errors map")
        void shouldReturn400WithFieldErrors() {
            FieldError fe1 = mock(FieldError.class);
            when(fe1.getField()).thenReturn("username");
            when(fe1.getDefaultMessage()).thenReturn("Username is required");

            FieldError fe2 = mock(FieldError.class);
            when(fe2.getField()).thenReturn("email");
            when(fe2.getDefaultMessage()).thenReturn("Email must be valid");

            List<ObjectError> fieldErrors = new ArrayList<>();
            fieldErrors.add(fe1);
            fieldErrors.add(fe2);

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            var bindingResult = mock(org.springframework.validation.BindingResult.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(fieldErrors);

            ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleValidationExceptions(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getStatusCode()).isEqualTo(400);
            assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
            assertThat(response.getBody().getData()).containsEntry("username", "Username is required");
            assertThat(response.getBody().getData()).containsEntry("email", "Email must be valid");
        }

        @Test
        @DisplayName("should return empty errors map for no field errors")
        void shouldReturnEmptyErrorsMap() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            var bindingResult = mock(org.springframework.validation.BindingResult.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(java.util.Collections.emptyList());

            ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleValidationExceptions(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getData()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Generic Exception fallback")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return 500 with exception message")
        void shouldReturn500ForGenericException() {
            Exception ex = new RuntimeException("Something went wrong");
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleGlobalException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getStatusCode()).isEqualTo(500);
            assertThat(response.getBody().getMessage()).contains("Something went wrong");
        }

        @Test
        @DisplayName("should handle null message gracefully")
        void shouldHandleNullMessage() {
            Exception ex = new RuntimeException();
            WebRequest request = mock(WebRequest.class);

            ResponseEntity<ApiResponse<Object>> response = handler.handleGlobalException(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}