package com.eventtun.user;

import com.eventtun.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @BeforeEach void reset() { users.deleteAll(); }

    private String input(String email) {
        return "{\"firstName\":\"Seif\",\"lastName\":\"Test\",\"email\":\"" + email + "\"}";
    }
    private long create(String email) throws Exception {
        String body = mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(input(email)))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }
    @Test void userLifecycleAndBlocking() throws Exception {
        long id = create("Seif@example.com");
        mvc.perform(get("/api/users/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("seif@example.com"));
        mvc.perform(get("/api/users")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        for (int i = 0; i < 2; i++) {
            mvc.perform(patch("/api/users/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BLOCKED"));
        }
        mvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON).content(input("new@example.com")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new@example.com"))
            .andExpect(jsonPath("$.status").value("BLOCKED"));
        mvc.perform(patch("/api/users/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(delete("/api/users/" + id)).andExpect(status().isMethodNotAllowed());
    }
    @Test void duplicateEmailOnCreateAndUpdate() throws Exception {
        long first = create("one@example.com");
        long second = create("two@example.com");
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(input("ONE@example.com")))
            .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(put("/api/users/" + second).contentType(MediaType.APPLICATION_JSON).content(input("ONE@example.com")))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/users/" + first).contentType(MediaType.APPLICATION_JSON).content(input("one@example.com")))
            .andExpect(status().isOk());
    }
    @Test void invalidRequestsAndMissingUsers() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(input("bad-email")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/users/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mvc.perform(put("/api/users/999999").contentType(MediaType.APPLICATION_JSON).content(input("valid@example.com")))
            .andExpect(status().isNotFound());
        mvc.perform(patch("/api/users/999999/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BLOCKED\"}"))
            .andExpect(status().isNotFound());
        for (String body : new String[]{"{}", "{\"status\":null}", "{\"status\":\"INVALID\"}", "{"}) {
            mvc.perform(patch("/api/users/1/status").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
    }
    @Test void swaggerDocumentsImplementedOperations() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/users'].post").exists())
            .andExpect(jsonPath("$.paths['/api/users/{id}/status'].patch").exists());
    }
}
