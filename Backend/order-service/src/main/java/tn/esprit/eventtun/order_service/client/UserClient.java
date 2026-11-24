
package tn.esprit.eventtun.order_service.client;

import tn.esprit.eventtun.order_service.client.ExternalServiceDtos.UserInfo;

public interface UserClient {

    UserInfo getUser(Long userId);
}
