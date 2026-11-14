package com.eventtun.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOpenApi() {
        return new OpenAPI().info(new Info()
                .title("EventTun - Payment Service API")
                .version("v1")
                .description("Paiements simulés et remboursements. Port 8084, base payments_db. "
                        + "Erreurs au format Problem Details (application/problem+json)."));
    }
}
