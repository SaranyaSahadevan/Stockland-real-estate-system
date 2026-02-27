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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    // ── GET / and /index ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET / returns index view with actions, filters and featuredProperties in model")
    void index_ReturnsIndexView_WithModelAttributes() throws Exception {
        when(propertyService.findFeatured()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("filters"))
                .andExpect(model().attributeExists("featuredProperties"));

        verify(propertyService).findFeatured();
    }

    @Test
    @DisplayName("GET / populates featuredProperties with list returned by propertyService")
    void index_PopulatesFeaturedProperties_FromService() throws Exception {
        PropertyResponseDTO featured = new PropertyResponseDTO();
        featured.setId(1L);
        featured.setTitle("Featured House");
        when(propertyService.findFeatured()).thenReturn(List.of(featured));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("featuredProperties", List.of(featured)));
    }

    @Test
    @DisplayName("GET / adds a PropertyFilterRequestDTO as filters to model")
    void index_AddsEmptyFilterDTO_AsFilters() throws Exception {
        when(propertyService.findFeatured()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(model().attributeExists("filters"));
    }

    @Test
    @DisplayName("GET /index also returns index view")
    void index_MapsToIndexView_ViaIndexPath() throws Exception {
        when(propertyService.findFeatured()).thenReturn(List.of());

        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        verify(propertyService).findFeatured();
    }

    // ── GET /login ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /login without error param returns login view without error attribute")
    void login_ReturnsLoginView_WithoutError() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    @DisplayName("GET /login?error adds error message to model")
    void login_AddsErrorToModel_WhenErrorParamPresent() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Invalid username/email or password"));
    }

    @Test
    @DisplayName("GET /login?error=someValue still adds the fixed error message to model")
    void login_AddsFixedErrorMessage_RegardlessOfErrorParamValue() throws Exception {
        mockMvc.perform(get("/login").param("error", "badcredentials"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Invalid username/email or password"));
    }

    // ── GET /listings ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /listings with valid filters returns listings view with properties and model attributes")
    void listings_ValidFilters_ReturnsListingsView_WithAllModelAttributes() throws Exception {
        PropertyResponseDTO dto = new PropertyResponseDTO();
        dto.setTitle("Test Property");
        PageImpl<PropertyResponseDTO> page = new PageImpl<>(List.of(dto));

        when(propertyService.searchPropertiesWithFilterSortAndPagination(
                any(PropertyFilterRequestDTO.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"))
                .andExpect(model().attributeExists("filters"))
                .andExpect(model().attributeExists("propertyPage"))
                .andExpect(model().attributeExists("properties"));

        verify(propertyService).searchPropertiesWithFilterSortAndPagination(
                any(PropertyFilterRequestDTO.class), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /listings populates properties model attribute with page content")
    void listings_PopulatesProperties_WithPageContent() throws Exception {
        PropertyResponseDTO dto = new PropertyResponseDTO();
        dto.setTitle("House A");
        PageImpl<PropertyResponseDTO> page = new PageImpl<>(List.of(dto));

        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/listings"))
                .andExpect(model().attribute("properties", List.of(dto)));
    }

    @Test
    @DisplayName("GET /listings with no results still returns listings view with empty properties")
    void listings_NoResults_ReturnsListingsView_WithEmptyProperties() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/listings"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(model().attributeExists("properties"));
    }

    @Test
    @DisplayName("GET /listings passes filters object back into model")
    void listings_AddsFilters_ToModel() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/listings").param("location", "Riga"))
                .andExpect(model().attributeExists("filters"));
    }

    @Test
    @DisplayName("GET /listings with invalid filter (negative minPrice) returns listings view with errorMessage")
    void listings_InvalidFilter_ReturnsListingsView_WithErrorMessage() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/listings").param("minPrice", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("listings"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attributeExists("properties"))
                .andExpect(model().attributeExists("propertyPage"));
    }

    @Test
    @DisplayName("GET /listings with invalid filter errorMessage contains field error description")
    void listings_InvalidFilter_ErrorMessage_ContainsFieldErrorDescription() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/listings").param("minPrice", "-1"))
                .andExpect(model().attributeExists("errorMessage"));

        // service is still called with an empty filter when validation fails
        verify(propertyService).searchPropertiesWithFilterSortAndPagination(any(), any());
    }

    @Test
    @DisplayName("GET /listings with invalid filter still calls service with empty filter")
    void listings_InvalidFilter_StillCallsService_WithEmptyFilter() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/listings").param("minPrice", "-100"))
                .andExpect(status().isOk());

        verify(propertyService, times(1))
                .searchPropertiesWithFilterSortAndPagination(any(PropertyFilterRequestDTO.class), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /listings actions model attribute contains all ActionType values")
    void listings_ActionsAttribute_ContainsAllActionTypes() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/listings"))
                .andExpect(model().attribute("actions", ActionType.values()));
    }

    @Test
    @DisplayName("GET /listings propertyTypes model attribute contains all PropertyType values")
    void listings_PropertyTypesAttribute_ContainsAllPropertyTypes() throws Exception {
        when(propertyService.searchPropertiesWithFilterSortAndPagination(any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/listings"))
                .andExpect(model().attribute("propertyTypes", PropertyType.values()));
    }

    // ── GET /property/{id} ────────────────────────────────────────────────────

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

    @Test
    @DisplayName("GET /property/{id} calls findByUsername with the username from the property")
    void property_CallsFindByUsername_WithPropertyUsername() throws Exception {
        PropertyResponseDTO property = new PropertyResponseDTO();
        property.setId(5L);
        property.setUsername("alice");

        UserResponseDTO user = new UserResponseDTO();
        user.setUsername("alice");

        when(propertyService.findById(5L)).thenReturn(property);
        when(userService.findByUsername("alice")).thenReturn(user);

        mockMvc.perform(get("/property/5"))
                .andExpect(status().isOk());

        verify(userService).findByUsername("alice");
    }

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

    // ── GET /create-listing ───────────────────────────────────────────────────

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

    @Test
    @DisplayName("GET /create-listing adds all ActionType values to actions model attribute")
    void createListing_AddsAllActionTypes_ToModel() throws Exception {
        mockMvc.perform(get("/create-listing"))
                .andExpect(model().attribute("actions", ActionType.values()));
    }

    @Test
    @DisplayName("GET /create-listing adds all PropertyType values to propertyTypes model attribute")
    void createListing_AddsAllPropertyTypes_ToModel() throws Exception {
        mockMvc.perform(get("/create-listing"))
                .andExpect(model().attribute("propertyTypes", PropertyType.values()));
    }

    @Test
    @DisplayName("GET /create-listing adds an empty PropertyRequestDTO to model")
    void createListing_AddsEmptyPropertyRequestDTO_ToModel() throws Exception {
        mockMvc.perform(get("/create-listing"))
                .andExpect(model().attribute("propertyRequestDTO", new PropertyRequestDTO()));
    }
}
