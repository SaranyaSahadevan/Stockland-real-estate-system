package com.stockland.app.controller;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.dto.UserResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import com.stockland.app.service.PropertyService;
import com.stockland.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ViewControllerTest {

    @Mock
    private PropertyService propertyService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ViewController viewController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(viewController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // Test GET / returns index view with actions, filters and featuredProperties in model
    @Test
    @DisplayName("GET / returns index view with correct model attributes")
    void index_ReturnsIndexView_WithModelAttributes() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("filters"))
                .andExpect(model().attributeExists("featuredProperties"));
    }

    // Test GET /index also maps to the index view
    @Test
    @DisplayName("GET /index also returns index view")
    void index_MapsToIndexView_ViaIndexPath() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    // Test GET /login without error param returns login view with no error attribute
    @Test
    @DisplayName("GET /login without error param returns login view without error attribute")
    void login_ReturnsLoginView_WithoutError() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    // Test GET /login?error adds error message to model
    @Test
    @DisplayName("GET /login?error adds error message to model")
    void login_AddsErrorToModel_WhenErrorParamPresent() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid username/email or password"));
    }

    // Test GET /listings returns listings view with actions, propertyTypes and filters in model
    @Test
    @DisplayName("GET /listings returns listings view with correct model attributes")
    void listings_ReturnsListingsView_WithModelAttributes() throws Exception {
        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"))
                .andExpect(model().attributeExists("filters"));
    }

    // Test GET /property/{id} fetches property and user and adds both to model
    @Test
    @DisplayName("GET /property/{id} returns property view with property and user in model")
    void property_ReturnsPropertyView_WithPropertyAndUser() throws Exception {
        PropertyResponseDTO property = new PropertyResponseDTO();
        property.setId(1L);
        property.setUsername("john");

        UserResponseDTO user = new UserResponseDTO();
        user.setUsername("john");

        when(propertyService.findById(1L)).thenReturn(property);
        when(userService.findByUsername("john")).thenReturn(user);

        mockMvc.perform(get("/property/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("property"))
                .andExpect(model().attribute("property", property))
                .andExpect(model().attribute("user", user));

        verify(propertyService).findById(1L);
        verify(userService).findByUsername("john");
    }

    // Test GET /property/{id} delegates to propertyService.findById with the correct id
    @Test
    @DisplayName("GET /property/{id} calls propertyService.findById with the correct id")
    void property_CallsFindById_WithCorrectId() throws Exception {
        PropertyResponseDTO property = new PropertyResponseDTO();
        property.setId(42L);
        property.setUsername("jane");

        UserResponseDTO user = new UserResponseDTO();
        user.setUsername("jane");

        when(propertyService.findById(42L)).thenReturn(property);
        when(userService.findByUsername("jane")).thenReturn(user);

        mockMvc.perform(get("/property/42"))
                .andExpect(status().isOk());

        verify(propertyService).findById(42L);
    }

    // Test GET /create-listing returns create-listing view with actions, propertyTypes and empty DTO in model
    @Test
    @DisplayName("GET /create-listing returns create-listing view with correct model attributes")
    void createListing_ReturnsCreateListingView_WithModelAttributes() throws Exception {
        mockMvc.perform(get("/create-listing"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-listing"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"))
                .andExpect(model().attributeExists("propertyRequestDTO"));
    }
}
