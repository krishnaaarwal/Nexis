package com.nexis.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<Map<String,Object>>> authServiceFallback(){

        Map<String, Object> response = Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "message", "Auth Service is currently down. Please try again later.",
                "timestamp", LocalDateTime.now().toString(),
                "service", "auth-service"
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }
}
