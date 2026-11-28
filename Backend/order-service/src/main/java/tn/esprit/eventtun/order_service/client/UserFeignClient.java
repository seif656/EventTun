
package tn.esprit.eventtun.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/api/users/{id}")
    UserServiceResponse getUser(@PathVariable("id") Long id);

    record UserServiceResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String status
    ) {}
}
