
package com.eventtun.user.service;

import com.eventtun.user.entity.User;
import com.eventtun.user.entity.UserStatus;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User createUser(User user);

    List<User> findAllUsers();

    Optional<User> findUserById(Long id);

    User updateUser(Long id, User user);

    User changeStatus(Long id, UserStatus status);
}
