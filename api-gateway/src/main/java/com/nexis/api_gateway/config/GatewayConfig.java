package com.nexis.api_gateway.config;

import com.nexis.api_gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public GatewayConfig(JwtAuthenticationFilter jwtAuthenticationFilter){
        this.jwtAuthenticationFilter=jwtAuthenticationFilter;
    }

    //Resolve by IP Address
    @Bean
    public KeyResolver keyResolver(){
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }

    @Bean
    public RateLimiter rateLimiter(){
        return new RedisRateLimiter(1,1,1);
    }

    //LOCATE THE ROUTE
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder){
        return builder.routes()
                .route("auth-service-route",
                        r-> r.path("/api/auth/**")
                                .filters(
                                        f-> f.filter(jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config()))
                                                .requestRateLimiter(c->c.setRateLimiter(rateLimiter())
                                                        .setKeyResolver(keyResolver()))
                                        )
                                        .uri("lb://auth-service")
                ) //uri,predicate,filter in each route
                .build();
    }



}
