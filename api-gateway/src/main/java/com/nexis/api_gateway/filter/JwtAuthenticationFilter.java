package com.nexis.api_gateway.filter;

import com.nexis.api_gateway.util.GatewayJwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final GatewayJwtUtil jwtUtil;

    public JwtAuthenticationFilter(GatewayJwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(JwtAuthenticationFilter.Config config) {
        return (exchange, chain) -> {

            //1. Get the Request
            var request = exchange.getRequest();

            //2.Check if request need filter or not?
            if(!request.getURI().getPath().contains("/api/auth/login")&&
                    !request.getURI().getPath().contains("/api/auth/signup")){

                // 3. Check for the Authorization Header
                if(!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)){
                    return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
                }

                String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return onError(exchange, "Invalid Authorization Header format", HttpStatus.UNAUTHORIZED);
                }

                String token = authHeader.substring(7);

                try {
                    // 4. Validate token & extract claims
                    jwtUtil.validateToken(token);
                    String userId = jwtUtil.getUserIdFromToken(token);
                    String email = jwtUtil.getEmailFromToken(token);

                    // 5. Mutate the request to add the headers for downstream services

                    /*
                    When you call exchange.getRequest().mutate(),
                    you are telling Spring: "Take this locked request, make an exact copy of it,
                    let me add these new headers to the copy, and
                    then .build() it into a brand new locked request."
                    */

                    request = exchange.getRequest()
                            .mutate()  //mutate means copy (copy of request here)
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email)
                            .build();

                } catch (Exception e) {
                    return onError(exchange, "Unauthorized access to application", HttpStatus.UNAUTHORIZED);
                }
            }

            // 6. Forward the mutated request to the destination service
            return chain.filter(exchange.mutate().request(request).build());

        };


    }
    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties can go here if needed
    }
}
