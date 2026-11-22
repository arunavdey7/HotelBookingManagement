package com.hotelbooking.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI hotelBookingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hotel Booking Management Platform API")
                        .version("1.0.0")
                        .description("Modular backend service supporting property discovery, single & multi-property "
                                + "onboarding, concurrency-safe booking, pluggable payments with idempotency, and "
                                + "policy-driven cancellations."));
    }
}
