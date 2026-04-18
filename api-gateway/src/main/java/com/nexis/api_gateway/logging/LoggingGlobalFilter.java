package com.nexis.api_gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    // 1. Manually create the Logger instance (This is exactly what @Slf4j does under the hood)
    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // 2. Use the logger normally
        log.info("Incoming Request: [HTTP {}] - Path: {}", method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {

            long executionTime = System.currentTimeMillis() - startTime;
            var statusCode = exchange.getResponse().getStatusCode();

            log.info("Outgoing Response: [HTTP {}] - Path: {} - Status: {} - Time: {}ms",
                    method, path, statusCode, executionTime);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}