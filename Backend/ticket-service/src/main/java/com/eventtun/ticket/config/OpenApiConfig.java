package com.eventtun.ticket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("EventTun - Ticket Service")
                .version("0.0.2")
                .description("Emission, consultation et validation des billets numeriques. "
                        + "Validation 2 : les six operations sont implementees. "
                        + "Les erreurs sont renvoyees au format Problem Details (application/problem+json)."));
    }
}
