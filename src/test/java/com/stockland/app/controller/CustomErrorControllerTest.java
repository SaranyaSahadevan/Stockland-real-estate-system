package com.stockland.app.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomErrorControllerTest {

    @InjectMocks
    private CustomErrorController customErrorController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(customErrorController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Helper — performs GET /error with a given status code attribute set on the request
    private org.springframework.test.web.servlet.ResultActions performWithStatus(int statusCode) throws Exception {
        return mockMvc.perform(get("/error")
                .requestAttr("jakarta.servlet.error.status_code", statusCode));
    }

    // Test 400 Bad Request
    @Test
    @DisplayName("GET /error with status 400 returns correct title and message")
    void handleError_Returns400_WithCorrectModel() throws Exception {
        performWithStatus(400)
                .andExpect(status().isOk())
                .andExpect(view().name("error/error-page"))
                .andExpect(model().attribute("status", 400))
                .andExpect(model().attribute("title", "Bad Request"))
                .andExpect(model().attribute("message", "The request could not be understood by the server."));
    }

    // Test 401 Unauthorized
    @Test
    @DisplayName("GET /error with status 401 returns correct title and message")
    void handleError_Returns401_WithCorrectModel() throws Exception {
        performWithStatus(401)
                .andExpect(status().isOk())
                .andExpect(view().name("error/error-page"))
                .andExpect(model().attribute("status", 401))
                .andExpect(model().attribute("title", "Unauthorized"))
                .andExpect(model().attribute("message", "You need to log in to access this resource."));
    }

    // Test 403 Access Denied
    @Test
    @DisplayName("GET /error with status 403 returns correct title and message")
    void handleError_Returns403_WithCorrectModel() throws Exception {
        performWithStatus(403)
                .andExpect(status().isOk())
                .andExpect(view().name("error/error-page"))
                .andExpect(model().attribute("status", 403))
                .andExpect(model().attribute("title", "Access Denied"))
                .andExpect(model().attribute("message", "You do not have permission to access this page."));
    }

    // Test 404 Not Found
    @Test
    @DisplayName("GET /error with status 404 returns correct title and message")
    void handleError_Returns404_WithCorrectModel() throws Exception {
        performWithStatus(404)
                .andExpect(status().isOk())
                .andExpect(view().name("error/error-page"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("title", "Page Not Found"))
                .andExpect(model().attribute("message", "Sorry, the page you are looking for does not exist."));
    }

    // Test 503 Service Unavailable
    @Test
    @DisplayName("GET /error with status 503 returns correct title and message")
    void handleError_Returns503_WithCorrectModel() throws Exception {
        performWithStatus(503)
                .andExpect(status().isOk())
                .andExpect(view().name("error/error-page"))
                .andExpect(model().attribute("status", 503))
                .andExpect(model().attribute("title", "Service Unavailable"))
                .andExpect(model().attribute("message", "The service is temporarily unavailable. Please try again later."));
    }

    // Test default (500) — any unrecognised status code falls through to default
    @Test
    @DisplayName("GET /error with status 500 returns default Internal Server Error")
    void handleError_Returns500_WithDefaultModel() throws Exception {
        performWithStatus(500)
                .andExpect(status().isOk())
                .andExpect(view().name("error/error-page"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("title", "Internal Server Error"))
                .andExpect(model().attribute("message", "Something went wrong on our end. Please try again later."));
    }

    // Test unrecognised status code also falls through to default
    @Test
    @DisplayName("GET /error with unrecognised status code returns default Internal Server Error")
    void handleError_ReturnsDefault_WhenStatusCodeUnrecognised() throws Exception {
        performWithStatus(418)
                .andExpect(status().isOk())
                .andExpect(model().attribute("title", "Internal Server Error"));
    }

    // Test null status code defaults to 500
    @Test
    @DisplayName("GET /error with no status code attribute defaults to 500")
    void handleError_DefaultsTo500_WhenStatusCodeIsNull() throws Exception {
        mockMvc.perform(get("/error")) // no requestAttr set
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attribute("title", "Internal Server Error"));
    }

    // Test isLoggedIn is false for unauthenticated user
    @Test
    @DisplayName("GET /error sets isLoggedIn to false when user is not authenticated")
    void handleError_SetsIsLoggedIn_False_WhenNotAuthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        performWithStatus(404)
                .andExpect(model().attribute("isLoggedIn", false));
    }

    // Test isLoggedIn is true for authenticated user
    @Test
    @DisplayName("GET /error sets isLoggedIn to true when user is authenticated")
    void handleError_SetsIsLoggedIn_True_WhenAuthenticated() throws Exception {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "john", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        performWithStatus(404)
                .andExpect(model().attribute("isLoggedIn", true));
    }
}

