package com.eventtun.user.dto;

import com.eventtun.user.entity.Club;
import jakarta.validation.constraints.*;

/** ID and creation time are managed by the server. */
public class ClubRequest {
    @NotBlank @Size(max = 255)
    private String name;
    @Size(max = 255)
    private String description;
    @NotNull @Positive
    private Long ownerId;

    public Club toClub() {
        Club club = new Club();
        club.setName(name.trim());
        club.setDescription(description);
        club.setOwnerId(ownerId);
        return club;
    }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { ownerId = value; }
}
