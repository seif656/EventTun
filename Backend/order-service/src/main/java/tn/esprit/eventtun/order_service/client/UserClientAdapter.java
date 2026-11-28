
package tn.esprit.eventtun.order_service.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import feign.FeignException;
import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.UserInfo;

@Component
public class UserClientAdapter implements UserClient {

    private final UserFeignClient userFeignClient;

    public UserClientAdapter(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @Override
    public UserInfo getUser(Long userId) {
        try {
            UserFeignClient.UserServiceResponse user =
                userFeignClient.getUser(userId);

            if (user == null || user.id() == null) {
                return new UserInfo(userId, false, false);
            }

            boolean blocked = "BLOCKED".equalsIgnoreCase(user.status());

            return new UserInfo(
                user.id(),
                true,
                blocked
            );

        } catch (FeignException.NotFound exception) {
            return new UserInfo(userId, false, false);
        }
    }
}
