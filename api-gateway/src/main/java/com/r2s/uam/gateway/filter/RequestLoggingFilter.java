package com.r2s.uam.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());

        log.info("[GATEWAY] {} {} -> {}",
            request.getMethod(),
            request.getPath(),
            request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown"
        );

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("[GATEWAY] Request has Bearer token");
        }

        return chain.filter(exchange).then(
            Mono.fromRunnable(() -> {
                Long startTime = exchange.getAttribute(START_TIME);
                if (startTime != null) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[GATEWAY] {} {} <- {} ({}ms) [{}]",
                        request.getMethod(),
                        request.getPath(),
                        request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown",
                        duration,
                        exchange.getResponse().getStatusCode()
                    );
                }
            })
        );
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the filter chain
    }
}