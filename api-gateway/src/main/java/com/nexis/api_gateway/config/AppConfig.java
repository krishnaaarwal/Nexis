package com.nexis.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    //LOCATE THE ROUTE
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder){
        return builder.routes()
                .route(
                        r->
                                r.path("/auth/**")
                                        .filters(
                                                f->
                                                        f.rewritePath("/auth/?(?<segment>.*)","/${segment}")
                                                                .addResponseHeader("-X-CUSTOM-HEADER","added by nexis")
                                        )
                                        .uri("lb://auth-service")
                ) //uri,predicate,filter in each route
                .build();
    }

}
