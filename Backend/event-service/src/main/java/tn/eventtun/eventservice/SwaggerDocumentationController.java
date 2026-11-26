package tn.eventtun.eventservice;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Expose uniquement la specification statique, sans implementer les API metier. */
@RestController
public class SwaggerDocumentationController {
    @GetMapping(value = "/openapi/event-service.yaml", produces = "application/yaml")
    public ResponseEntity<String> openApiYaml() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/openapi/event-service.yaml");
        try (var stream = resource.getInputStream()) {
            return ResponseEntity.ok(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
