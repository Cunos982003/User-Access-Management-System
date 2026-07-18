package com.r2s.uam.gateway.config;

import com.r2s.uam.gateway.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GatewayRouteConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${app.gateway.services.auth:#{null}}")
    private String authServiceUri;

    @Value("${app.gateway.services.user:#{null}}")
    private String userServiceUri;

    @Value("${app.gateway.services.notification:#{null}}")
    private String notificationServiceUri;

    @Value("${app.gateway.services.audit:#{null}}")
    private String auditServiceUri;

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-public", r -> r
                .path("/api/v1/auth/register", "/api/v1/auth/verify-otp", "/api/v1/auth/login",
                      "/api/v1/auth/refresh-token", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password")
                .filters(f -> f.stripPrefix(0))
                .uri(authServiceUri))

            .route("auth-protected", r -> r
                .path("/api/v1/auth/change-password", "/api/v1/auth/logout",
                      "/api/v1/auth/me")
                .filters(f -> f.filter(jwtFilter).stripPrefix(0))
                .uri(authServiceUri))

            .route("user-service", r -> r
                .path("/api/v1/users/**")
                .filters(f -> f.filter(jwtFilter).stripPrefix(0))
                .uri(userServiceUri))

            .route("notification-service", r -> r
                .path("/api/v1/notifications/**")
                .filters(f -> f.filter(jwtFilter).stripPrefix(0))
                .uri(notificationServiceUri))

            .route("audit-service", r -> r
                .path("/api/v1/audit/**")
                .filters(f -> f.filter(jwtFilter).stripPrefix(0))
                .uri(auditServiceUri))

            .route("swagger", r -> r
                .path("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**")
                .filters(f -> f.stripPrefix(0))
                .uri(authServiceUri))

            .build();
    }
}