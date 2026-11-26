package com.eventtun.user.dto;
import com.eventtun.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UserStatusRequest {
    @NotNull
    private UserStatus status;
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus value) { status = value; }
}
