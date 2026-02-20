package com.stockland.app.controller;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.dto.UserResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import com.stockland.app.service.PropertyService;
import com.stockland.app.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PropertyControllerTest {

    @Mock
    private PropertyService propertyService;

    @Mock
    private UserService userService;

    @InjectMocks
    private PropertyController propertyController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(propertyController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Helper — builds a Spring Security UserDetails principal
    private UserDetails principal(String username) {
        return User.withUsername(username)
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    // Helper — stores UserDetails in SecurityContextHolder so @AuthenticationPrincipal resolves it
    private void authenticateAs(String username) {
        UserDetails ud = principal(username);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                ud, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // GET /properties — valid filters returns listings view with properties and filters in model
    @Test
    @DisplayName("GET /properties with valid filters returns listings view with properties in model")
    void searchProperties_ReturnsListingsView_WithProperties() throws Exception {
        PropertyResponseDTO dto = new PropertyResponseDTO();
        dto.setTitle("Test Property");

        when(propertyService.searchPropertiesWithFilterSortAndPagination(
                any(PropertyFilterRequestDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/properties"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(model().attributeExists("properties"))
                .andExpect(model().attributeExists("filters"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"));

        verify(propertyService).searchPropertiesWithFilterSortAndPagination(
                any(PropertyFilterRequestDTO.class), any(Pageable.class));
    }

    // GET /properties — valid filters with no results still returns listings view
    @Test
    @DisplayName("GET /properties with no matching results still returns listings view")
    void searchProperties_ReturnsListingsView_WhenNoResults() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(
                any(PropertyFilterRequestDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/properties"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(model().attributeExists("properties"));
    }

    // GET /properties — invalid filter (e.g. negative price) returns listings with errorMessage
    @Test
    @DisplayName("GET /properties with invalid filter returns listings view with errorMessage")
    void searchProperties_ReturnsListingsWithError_WhenValidationFails() throws Exception {
        mockMvc.perform(get("/properties")
                        .param("minPrice", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(propertyService, never()).searchPropertiesWithFilterSortAndPagination(any(), any());
    }

    // POST /properties/create — validation errors return create-listing view with model attributes
    @Test
    @DisplayName("POST /properties/create with validation errors returns create-listing view")
    void createProperty_ReturnsCreateListingView_WhenValidationFails() throws Exception {
        // title and location are blank (@NotBlank), price is missing (@NotNull), actionType and propertyType are missing (@NotNull)
        authenticateAs("john");

        mockMvc.perform(post("/properties/create")
                        .param("title", "")
                        .param("location", ""))
                .andExpect(view().name("create-listing"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"))
                .andExpect(model().attributeExists("propertyRequestDTO"));

        verify(propertyService, never()).saveProperty(any(), any());
    }

    // POST /properties/create — valid data saves property and redirects to dashboard
    @Test
    @DisplayName("POST /properties/create with valid data saves property and redirects to dashboard")
    void createProperty_RedirectsToDashboard_WhenSuccessful() throws Exception {
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setId(1L);
        userDTO.setUsername("john");

        when(userService.findByUsername("john")).thenReturn(userDTO);
        when(userService.usernameExists("john")).thenReturn(true);
        when(propertyService.saveProperty(any(), eq(1L))).thenReturn(new PropertyResponseDTO());

        authenticateAs("john");

        mockMvc.perform(post("/properties/create")
                        .param("title", "Nice House")
                        .param("location", "Riga")
                        .param("price", "150000")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(propertyService).saveProperty(any(), eq(1L));
    }

    // POST /properties/create — usernameExists returns false → throws RuntimeException
    @Test
    @DisplayName("POST /properties/create throws RuntimeException when usernameExists returns false")
    void createProperty_ThrowsException_WhenUsernameNotFoundAfterSave() {
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setId(1L);
        userDTO.setUsername("john");

        when(userService.findByUsername("john")).thenReturn(userDTO);
        when(userService.usernameExists("john")).thenReturn(false);
        when(propertyService.saveProperty(any(), eq(1L))).thenReturn(new PropertyResponseDTO());

        authenticateAs("john");

        Exception thrown = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/properties/create")
                        .param("title", "Nice House")
                        .param("location", "Riga")
                        .param("price", "150000")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available"))
        );

        Throwable cause = thrown.getCause() != null ? thrown.getCause() : thrown;
        assertInstanceOf(RuntimeException.class, cause);
        assertTrue(cause.getMessage().contains("Provided username does not exist"));
    }
}

