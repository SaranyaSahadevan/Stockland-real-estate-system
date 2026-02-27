package com.stockland.app.controller;

import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import com.stockland.app.service.FavoriteService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerTest {

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private PropertyService propertyService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FavoriteController favoriteController;

    private MockMvc mockMvc;

    private User user;
    private Property property;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(favoriteController).build();

        user = new User();
        user.setId(1L);
        user.setUsername("john");

        property = new Property();
        property.setId(10L);
        property.setTitle("Nice House");

        // Authenticate as "john" in the SecurityContextHolder
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("john", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // GET /favorites/add/{id} — redirects to /property/{id} after adding favorite
    @Test
    @DisplayName("GET /favorites/add/{id} adds favorite and redirects to property page")
    void addFavorite_AddsFavoriteAndRedirects() throws Exception {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertyById(10L)).thenReturn(property);

        mockMvc.perform(get("/favorites/add/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/property/10"));

        verify(favoriteService).addFavorite(user, property);
    }

    // GET /favorites/add/{id} — falls back to findByEmail when username lookup returns empty
    @Test
    @DisplayName("GET /favorites/add/{id} resolves user by email when username not found")
    void addFavorite_ResolvesUserByEmail_WhenUsernameNotFound() throws Exception {
        // Override SecurityContextHolder with an email-as-principal
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("john@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(propertyService.getPropertyById(10L)).thenReturn(property);

        mockMvc.perform(get("/favorites/add/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/property/10"));

        verify(userRepository).findByEmail("john@example.com");
        verify(favoriteService).addFavorite(user, property);
    }

    // GET /favorites/remove/{id} — redirects to /dashboard after removing favorite
    @Test
    @DisplayName("GET /favorites/remove/{id} removes favorite and redirects to dashboard")
    void removeFavorite_RemovesFavoriteAndRedirects() throws Exception {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertyById(10L)).thenReturn(property);

        mockMvc.perform(get("/favorites/remove/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(favoriteService).removeFavorite(user, property);
    }

    // GET /favorites/remove/{id} — falls back to findByEmail when username lookup returns empty
    @Test
    @DisplayName("GET /favorites/remove/{id} resolves user by email when username not found")
    void removeFavorite_ResolvesUserByEmail_WhenUsernameNotFound() throws Exception {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("john@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(propertyService.getPropertyById(10L)).thenReturn(property);

        mockMvc.perform(get("/favorites/remove/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(userRepository).findByEmail("john@example.com");
        verify(favoriteService).removeFavorite(user, property);
    }

    // GET /favorites/add/{id} — does NOT add duplicate when service guards it (service behaviour already tested separately,
    // but controller must always delegate regardless)
    @Test
    @DisplayName("GET /favorites/add/{id} always delegates to FavoriteService regardless of existing state")
    void addFavorite_AlwaysDelegatesToService() throws Exception {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertyById(10L)).thenReturn(property);

        mockMvc.perform(get("/favorites/add/10"))
                .andExpect(status().is3xxRedirection());

        verify(favoriteService, times(1)).addFavorite(any(), any());
    }
}

