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
                .version("0.0.1")
                .description("Emission, consultation et validation des billets numeriques. "
                        + "Contrat de la validation 1 : les endpoints sont documentes, "
                        + "la logique metier sera implementee aux validations suivantes "
                        + "(les appels renvoient pour l'instant 501 Not Implemented)."));
    }
}
