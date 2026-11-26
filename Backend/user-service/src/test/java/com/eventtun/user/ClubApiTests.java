package com.eventtun.user;

import com.eventtun.user.entity.*;
import com.eventtun.user.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClubApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ClubRepository clubs;
    @Autowired UserRepository users;
    long ownerId;

    @BeforeEach void setup() {
        clubs.deleteAll();
        users.deleteAll();
        ownerId = owner(Role.ORGANIZER, "organizer@example.com");
    }
    @AfterEach void cleanup() { clubs.deleteAll(); }

    private long owner(Role role, String email) {
        User user = new User();
        user.setFirstName("Club"); user.setLastName("Owner"); user.setEmail(email);
        user.setRole(role); user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user).getId();
    }
    private String input(String name, long owner) throws Exception {
        return mapper.writeValueAsString(java.util.Map.of("name", name, "ownerId", owner, "description", "University club"));
    }
    private long create() throws Exception {
        String body = mvc.perform(post("/api/clubs").contentType(MediaType.APPLICATION_JSON).content(input("Robotics", ownerId)))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }
    @Test void completeLifecycleAndOwnerTransfer() throws Exception {
        long id = create();
        var createdAt = clubs.findById(id).orElseThrow().getCreatedAt();
        mvc.perform(get("/api/clubs")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/clubs/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Robotics"));
        long newOwner = owner(Role.ORGANIZER, "second@example.com");
        mvc.perform(put("/api/clubs/" + id).contentType(MediaType.APPLICATION_JSON).content(input("Coding", newOwner)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(newOwner));
        assertEquals(createdAt, clubs.findById(id).orElseThrow().getCreatedAt());
        mvc.perform(delete("/api/clubs/" + id)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/clubs/" + id)).andExpect(status().isNotFound());
        assertTrue(users.existsById(newOwner));
    }
    @Test void ownerMustExistAndBeOrganizerOnCreateAndUpdate() throws Exception {
        long student = owner(Role.STUDENT, "student@example.com");
        mvc.perform(post("/api/clubs").contentType(MediaType.APPLICATION_JSON).content(input("Club", student)))
            .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(post("/api/clubs").contentType(MediaType.APPLICATION_JSON).content(input("Club", 999999)))
            .andExpect(status().isNotFound());
        long id = create();
        mvc.perform(put("/api/clubs/" + id).contentType(MediaType.APPLICATION_JSON).content(input("Changed", student)))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/clubs/" + id).contentType(MediaType.APPLICATION_JSON).content(input("Changed", 999999)))
            .andExpect(status().isNotFound());
        assertEquals("Robotics", clubs.findById(id).orElseThrow().getName());
    }
    @Test void invalidInputAndMissingClubs() throws Exception {
        for (String body : new String[]{"{}", "{", input("  ", ownerId), input("a".repeat(256), ownerId), input("Club", 0)}) {
            mvc.perform(post("/api/clubs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(get("/api/clubs/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/clubs/abc")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/clubs/999999").contentType(MediaType.APPLICATION_JSON).content(input("Club", ownerId)))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/clubs/999999")).andExpect(status().isNotFound());
    }
    @Test void swaggerIncludesClubOperations() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/clubs'].post").exists())
            .andExpect(jsonPath("$.paths['/api/clubs/{id}'].delete").exists());
    }

    @Test void ownerRoleCannotChangeUntilAllClubsAreTransferred() throws Exception {
        long clubId = create();
        long secondClub = create();
        String profile = mapper.writeValueAsString(java.util.Map.of(
            "firstName", "Club", "lastName", "Owner", "email", "organizer@example.com"));
        // An omitted role defaults to STUDENT, so it must also be rejected.
        mvc.perform(put("/api/users/" + ownerId).contentType(MediaType.APPLICATION_JSON).content(profile))
            .andExpect(status().isConflict());
        for (Role role : new Role[]{Role.STUDENT, Role.CONTROLLER, Role.ADMIN}) {
            String change = mapper.writeValueAsString(java.util.Map.of(
                "firstName", "Club", "lastName", "Owner", "email", "organizer@example.com", "role", role.name()));
            mvc.perform(put("/api/users/" + ownerId).contentType(MediaType.APPLICATION_JSON).content(change))
                .andExpect(status().isConflict());
        }
        assertEquals(Role.ORGANIZER, users.findById(ownerId).orElseThrow().getRole());
        String sameRole = mapper.writeValueAsString(java.util.Map.of(
            "firstName", "Updated", "lastName", "Owner", "email", "organizer@example.com", "role", "ORGANIZER"));
        mvc.perform(put("/api/users/" + ownerId).contentType(MediaType.APPLICATION_JSON).content(sameRole))
            .andExpect(status().isOk());
        long newOwner = owner(Role.ORGANIZER, "new-owner@example.com");
        mvc.perform(put("/api/clubs/" + clubId).contentType(MediaType.APPLICATION_JSON).content(input("Transferred", newOwner)))
            .andExpect(status().isOk());
        mvc.perform(put("/api/users/" + ownerId).contentType(MediaType.APPLICATION_JSON).content(profile))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/clubs/" + secondClub)).andExpect(status().isNoContent());
        mvc.perform(put("/api/users/" + ownerId).contentType(MediaType.APPLICATION_JSON).content(profile))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
