package com.stockland.app.config;

import com.stockland.app.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // MockitoBean prevents Spring from needing a real DB for UserService/UserDetailsService
    @MockitoBean
    private UserService userService;

    // --- Permitted URLs (no authentication required) ---

    @Test
    @DisplayName("GET / is accessible without authentication")
    void index_IsPermitted_WithoutAuth() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /login is accessible without authentication")
    void login_IsPermitted_WithoutAuth() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /listings is accessible without authentication")
    void listings_IsPermitted_WithoutAuth() throws Exception {
        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /properties is accessible without authentication")
    void properties_IsPermitted_WithoutAuth() throws Exception {
        mockMvc.perform(get("/properties"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /register is accessible without authentication")
    void registerGet_IsPermitted_WithoutAuth() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /register is accessible without authentication")
    void registerPost_IsPermitted_WithoutAuth() throws Exception {
        mockMvc.perform(post("/register").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/chat is accessible without authentication")
    void apiChat_IsPermitted_WithoutAuth() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content("{\"message\": \"hello\"}"))
                .andExpect(status().isOk());
    }


    // --- Authenticated access to protected URLs ---

    @Test
    @WithMockUser(username = "john", roles = "USER")
    @DisplayName("GET /create-listing is accessible when authenticated")
    void createListing_IsAccessible_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/create-listing"))
                .andExpect(status().isOk());
    }

    // --- Logout ---

    @Test
    @WithMockUser(username = "john", roles = "USER")
    @DisplayName("POST /logout redirects to /?logout on success")
    void logout_RedirectsToIndex_OnSuccess() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?logout"));
    }

    // --- PasswordEncoder bean ---

    @Test
    @DisplayName("PasswordEncoder bean encodes and matches a password correctly")
    void passwordEncoder_EncodesAndMatches() {
        String raw = "mySecretPassword";
        String encoded = passwordEncoder.encode(raw);

        assertNotNull(encoded);
        assertNotEquals(raw, encoded);
        assertTrue(passwordEncoder.matches(raw, encoded));
        assertFalse(passwordEncoder.matches("wrongPassword", encoded));
    }

    @Test
    @DisplayName("PasswordEncoder bean uses BCrypt (encoded value starts with $2a$)")
    void passwordEncoder_UsesBCrypt() {
        String encoded = passwordEncoder.encode("test");
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
    }
}

