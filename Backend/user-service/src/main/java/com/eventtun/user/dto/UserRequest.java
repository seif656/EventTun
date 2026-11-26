package com.eventtun.user.dto;

import com.eventtun.user.entity.Role;
import com.eventtun.user.entity.User;
import jakarta.validation.constraints.*;

/** Input excludes server-managed ID and status. */
public class UserRequest {
    @NotBlank @Size(max = 255)
    private String firstName;
    @NotBlank @Size(max = 255)
    private String lastName;
    @NotBlank @Email @Size(max = 255)
    private String email;
    @Size(max = 255)
    private String keycloakSubject;
    private Role role;

    public User toUser() {
        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(email);
        user.setKeycloakSubject(keycloakSubject);
        user.setRole(role);
        return user;
    }
    public String getFirstName() { return firstName; }
    public void setFirstName(String value) { firstName = value; }
    public String getLastName() { return lastName; }
    public void setLastName(String value) { lastName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getKeycloakSubject() { return keycloakSubject; }
    public void setKeycloakSubject(String value) { keycloakSubject = value; }
    public Role getRole() { return role; }
    public void setRole(Role value) { role = value; }
}
