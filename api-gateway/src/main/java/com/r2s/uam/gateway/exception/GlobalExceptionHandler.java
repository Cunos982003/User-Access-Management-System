package com.r2s.uam.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status;
        String message;

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getMessage();
        } else if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service unavailable. Please try again later.";
        } else if (ex.getMessage() != null && ex.getMessage().contains("500")) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Gateway error: " + ex.getMessage();
        } else {
            status = HttpStatus.BAD_GATEWAY;
            message = "An unexpected error occurred";
        }

        response.setStatusCode(status);

        String body = String.format(
            "{\"success\":false,\"statusCode\":%d,\"message\":\"%s\",\"timestamp\":\"%s\"}",
            status.value(), message, java.time.Instant.now()
        );

        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(body.getBytes()))
        );
    }
}