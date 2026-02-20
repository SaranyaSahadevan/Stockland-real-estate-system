package com.stockland.app.controller;

import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import com.stockland.app.service.PropertyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PropertyService propertyService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Helper to put an authenticated user into the SecurityContextHolder
    private void authenticateAs(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // Test that dashboard returns correct view and all expected model attributes for a user found by username
    @Test
    @DisplayName("GET /dashboard returns dashboard view with user, myListings and favorites for authenticated user")
    void dashboard_ReturnsViewWithModelAttributes_WhenUserFoundByUsername() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L)).thenReturn(List.of(new PropertyResponseDTO()));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("myListings"))
                .andExpect(model().attributeExists("favorites"));

        verify(propertyService).getPropertiesByUserId(1L);
    }

    // Test that dashboard falls back to findByEmail when username lookup returns empty
    @Test
    @DisplayName("GET /dashboard finds user by email when username lookup returns empty")
    void dashboard_FindsUserByEmail_WhenUsernameNotFound() throws Exception {
        authenticateAs("john@example.com");

        User user = new User();
        user.setId(2L);
        user.setUsername("john");

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(2L)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"));

        verify(userRepository).findByEmail("john@example.com");
        verify(propertyService).getPropertiesByUserId(2L);
    }

    // Test that myListings model attribute contains the correct number of listings
    @Test
    @DisplayName("GET /dashboard populates myListings with properties returned by service")
    void dashboard_PopulatesMyListings_WithPropertiesFromService() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        PropertyResponseDTO p1 = new PropertyResponseDTO();
        PropertyResponseDTO p2 = new PropertyResponseDTO();

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("myListings", List.of(p1, p2)));
    }

    // Test that favorites is always an empty list
    @Test
    @DisplayName("GET /dashboard always sets favorites to an empty list")
    void dashboard_SetsFavoritesToEmptyList() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favorites", List.of()));
    }
}
