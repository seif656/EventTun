package tn.esprit.eventtun.order_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Order Service API")
                .version("1.0.0")
                .description("EventTun - gestion des commandes et coordination de l'achat de billets "
                        + "(Validation 1 : contrat REST documente, logique metier prevue ulterieurement)."));
    }
}