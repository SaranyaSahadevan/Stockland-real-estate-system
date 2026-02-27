package com.stockland.app.controller;

import com.stockland.app.dto.PropertyRequestDTO;
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
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockMultipartFile;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserDetails principal(String username, String... roles) {
        return User.withUsername(username)
                .password("password")
                .authorities(roles)
                .build();
    }

    private void authenticateAs(String username, String... roles) {
        UserDetails ud = principal(username, roles);
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(ud, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PropertyResponseDTO buildProperty(Long id, String owner) {
        PropertyResponseDTO p = new PropertyResponseDTO();
        p.setId(id);
        p.setTitle("Test Property");
        p.setLocation("Riga");
        p.setPrice(150000.0);
        p.setUsername(owner);
        p.setActionType(ActionType.BUY);
        p.setPropertyType(PropertyType.HOUSE);
        p.setStatus("available");
        p.setImages(new String[0]);
        return p;
    }

    // ── GET /properties/{id} ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /properties/{id} returns property view with property in model")
    void viewProperty_ReturnsPropertyView_WithPropertyInModel() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        when(propertyService.findById(1L)).thenReturn(property);

        mockMvc.perform(get("/properties/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("property"))
                .andExpect(model().attribute("property", property));

        verify(propertyService).findById(1L);
    }

    @Test
    @DisplayName("GET /properties/{id} calls findById with the correct id")
    void viewProperty_CallsFindById_WithCorrectId() throws Exception {
        PropertyResponseDTO property = buildProperty(42L, "jane");
        when(propertyService.findById(42L)).thenReturn(property);

        mockMvc.perform(get("/properties/42"))
                .andExpect(status().isOk());

        verify(propertyService).findById(42L);
    }

    // ── POST /properties/delete/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("POST /properties/delete/{id} as owner redirects to dashboard?deleted")
    void deleteProperty_AsOwner_RedirectsToDashboard() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("john", "ROLE_USER");

        mockMvc.perform(post("/properties/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?deleted"));

        verify(propertyService).deleteById(1L);
    }

    @Test
    @DisplayName("POST /properties/delete/{id} as admin redirects to dashboard?deleted")
    void deleteProperty_AsAdmin_RedirectsToDashboard() throws Exception {
        PropertyResponseDTO property = buildProperty(2L, "someOwner");
        when(propertyService.findById(2L)).thenReturn(property);
        authenticateAs("admin", "ROLE_ADMIN");

        mockMvc.perform(post("/properties/delete/2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?deleted"));

        verify(propertyService).deleteById(2L);
    }

    @Test
    @DisplayName("POST /properties/delete/{id} as non-owner forwards to /error with 403")
    void deleteProperty_AsNonOwner_ForwardsToErrorPage() throws Exception {
        PropertyResponseDTO property = buildProperty(3L, "alice");
        when(propertyService.findById(3L)).thenReturn(property);
        authenticateAs("bob", "ROLE_USER");

        mockMvc.perform(post("/properties/delete/3"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/error"));

        verify(propertyService, never()).deleteById(anyLong());
    }

    // ── GET /properties/edit/{id} ─────────────────────────────────────────────

    @Test
    @DisplayName("GET /properties/edit/{id} as owner returns edit-listing view with model attributes")
    void editPropertyForm_AsOwner_ReturnsEditListingView() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("john", "ROLE_USER");

        mockMvc.perform(get("/properties/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-listing"))
                .andExpect(model().attributeExists("propertyRequestDTO"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"))
                .andExpect(model().attributeExists("redirectUrl"));

        verify(propertyService).findById(1L);
    }

    @Test
    @DisplayName("GET /properties/edit/{id} as admin returns edit-listing view")
    void editPropertyForm_AsAdmin_ReturnsEditListingView() throws Exception {
        PropertyResponseDTO property = buildProperty(5L, "someOwner");
        when(propertyService.findById(5L)).thenReturn(property);
        authenticateAs("admin", "ROLE_ADMIN");

        mockMvc.perform(get("/properties/edit/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-listing"));
    }

    @Test
    @DisplayName("GET /properties/edit/{id} as non-owner forwards to /error")
    void editPropertyForm_AsNonOwner_ForwardsToErrorPage() throws Exception {
        PropertyResponseDTO property = buildProperty(4L, "alice");
        when(propertyService.findById(4L)).thenReturn(property);
        authenticateAs("bob", "ROLE_USER");

        mockMvc.perform(get("/properties/edit/4"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/error"));
    }

    @Test
    @DisplayName("GET /properties/edit/{id} populates redirectUrl from request param")
    void editPropertyForm_PopulatesRedirectUrl_FromRequestParam() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("john", "ROLE_USER");

        mockMvc.perform(get("/properties/edit/1").param("redirectUrl", "/dashboard?myListings"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("redirectUrl", "/dashboard?myListings"));
    }

    @Test
    @DisplayName("GET /properties/edit/{id} with null price sets empty string in DTO")
    void editPropertyForm_WithNullPrice_SetsEmptyPriceInDto() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        property.setPrice(null);
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("john", "ROLE_USER");

        mockMvc.perform(get("/properties/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-listing"))
                .andExpect(model().attribute("propertyRequestDTO",
                        org.hamcrest.Matchers.hasProperty("price", org.hamcrest.Matchers.is(""))));
    }

    // ── POST /properties/edit/{id} ────────────────────────────────────────────

    @Test
    @DisplayName("POST /properties/edit/{id} with valid data as owner redirects to redirectUrl")
    void editProperty_AsOwner_WithValidData_Redirects() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("john", "ROLE_USER");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        mockMvc.perform(multipart("/properties/edit/1")
                        .file(emptyFile)
                        .param("title", "Updated House")
                        .param("location", "Riga")
                        .param("price", "200000,00")
                        .param("area", "120.0")
                        .param("roomCount", "4")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available")
                        .param("redirectUrl", "/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?updated"));

        verify(propertyService).updateProperty(eq(1L), any(), any(), any(), eq(false));
    }

    @Test
    @DisplayName("POST /properties/edit/{id} with valid data as admin sets isAdmin=true")
    void editProperty_AsAdmin_WithValidData_PassesIsAdminTrue() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "someOwner");
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("admin", "ROLE_ADMIN");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        mockMvc.perform(multipart("/properties/edit/1")
                        .file(emptyFile)
                        .param("title", "Updated House")
                        .param("location", "Riga")
                        .param("price", "200000,00")
                        .param("area", "120.0")
                        .param("roomCount", "4")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available")
                        .param("redirectUrl", "/dashboard"))
                .andExpect(status().is3xxRedirection());

        verify(propertyService).updateProperty(eq(1L), any(), any(), any(), eq(true));
    }

    @Test
    @DisplayName("POST /properties/edit/{id} with validation errors returns edit-listing view")
    void editProperty_WithValidationErrors_ReturnsEditListingView() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("john", "ROLE_USER");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        mockMvc.perform(multipart("/properties/edit/1")
                        .file(emptyFile)
                        .param("title", "")         // blank – triggers @NotBlank
                        .param("location", "")      // blank – triggers @NotBlank
                        .param("redirectUrl", "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-listing"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"));

        verify(propertyService, never()).updateProperty(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("POST /properties/edit/{id} as non-owner forwards to /error")
    void editProperty_AsNonOwner_ForwardsToErrorPage() throws Exception {
        PropertyResponseDTO property = buildProperty(3L, "alice");
        when(propertyService.findById(3L)).thenReturn(property);
        authenticateAs("bob", "ROLE_USER");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        mockMvc.perform(multipart("/properties/edit/3")
                        .file(emptyFile)
                        .param("title", "House")
                        .param("location", "Riga")
                        .param("price", "100000,00")
                        .param("area", "80.0")
                        .param("roomCount", "3")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available"))
                .andExpect(forwardedUrl("/error"));

        verify(propertyService, never()).updateProperty(anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("POST /properties/edit/{id} redirect appends &updated when redirectUrl already has query params")
    void editProperty_AppendsAndUpdated_WhenRedirectUrlHasQueryParams() throws Exception {
        PropertyResponseDTO property = buildProperty(1L, "john");
        when(propertyService.findById(1L)).thenReturn(property);
        authenticateAs("john", "ROLE_USER");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        mockMvc.perform(multipart("/properties/edit/1")
                        .file(emptyFile)
                        .param("title", "Updated House")
                        .param("location", "Riga")
                        .param("price", "200000,00")
                        .param("area", "120.0")
                        .param("roomCount", "4")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available")
                        .param("redirectUrl", "/dashboard?myListings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?myListings&updated"));
    }

    // ── POST /properties/create ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /properties/create with valid data saves property and redirects to /dashboard")
    void createProperty_WithValidData_RedirectsToDashboard() throws Exception {
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setId(1L);
        userDTO.setUsername("john");

        when(userService.findByUsername("john")).thenReturn(userDTO);
        when(userService.usernameExists("john")).thenReturn(true);
        authenticateAs("john", "ROLE_USER");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        mockMvc.perform(multipart("/properties/create")
                        .file(emptyFile)
                        .param("title", "Nice House")
                        .param("location", "Riga")
                        .param("price", "150000,00")
                        .param("area", "100.0")
                        .param("roomCount", "3")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(propertyService).saveProperty(any(PropertyRequestDTO.class), eq(1L), any());
    }

    @Test
    @DisplayName("POST /properties/create with validation errors returns create-listing view")
    void createProperty_WithValidationErrors_ReturnsCreateListingView() throws Exception {
        authenticateAs("john", "ROLE_USER");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        mockMvc.perform(multipart("/properties/create")
                        .file(emptyFile)
                        .param("title", "")      // blank – @NotBlank
                        .param("location", ""))  // blank – @NotBlank
                .andExpect(status().isOk())
                .andExpect(view().name("create-listing"))
                .andExpect(model().attributeExists("actions"))
                .andExpect(model().attributeExists("propertyTypes"))
                .andExpect(model().attributeExists("propertyRequestDTO"));

        verify(propertyService, never()).saveProperty(any(), any(), any());
    }

    @Test
    @DisplayName("POST /properties/create throws RuntimeException when usernameExists returns false")
    void createProperty_ThrowsRuntimeException_WhenUsernameNotFoundAfterSave() {
        UserResponseDTO userDTO = new UserResponseDTO();
        userDTO.setId(1L);
        userDTO.setUsername("john");

        when(userService.findByUsername("john")).thenReturn(userDTO);
        when(userService.usernameExists("john")).thenReturn(false);
        authenticateAs("john", "ROLE_USER");

        MockMultipartFile emptyFile = new MockMultipartFile("imageFiles", new byte[0]);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () ->
                mockMvc.perform(multipart("/properties/create")
                        .file(emptyFile)
                        .param("title", "Nice House")
                        .param("location", "Riga")
                        .param("price", "150000,00")
                        .param("area", "100.0")
                        .param("roomCount", "3")
                        .param("actionType", ActionType.BUY.name())
                        .param("propertyType", PropertyType.HOUSE.name())
                        .param("status", "available"))
        );
    }

    // ── POST /properties/approve/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("POST /properties/approve/{id} calls approveProperty and redirects to default url")
    void approveProperty_CallsServiceAndRedirects_DefaultUrl() throws Exception {
        mockMvc.perform(post("/properties/approve/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?approved"));

        verify(propertyService).approveProperty(10L);
    }

    @Test
    @DisplayName("POST /properties/approve/{id} redirects to custom redirectUrl")
    void approveProperty_RedirectsToCustomUrl() throws Exception {
        mockMvc.perform(post("/properties/approve/10")
                        .param("redirectUrl", "/dashboard?adminPanel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?adminPanel"));

        verify(propertyService).approveProperty(10L);
    }

    // ── POST /properties/reject/{id} ──────────────────────────────────────────

    @Test
    @DisplayName("POST /properties/reject/{id} calls rejectProperty and redirects to default url")
    void rejectProperty_CallsServiceAndRedirects_DefaultUrl() throws Exception {
        mockMvc.perform(post("/properties/reject/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?rejected"));

        verify(propertyService).rejectProperty(7L);
    }

    @Test
    @DisplayName("POST /properties/reject/{id} redirects to custom redirectUrl")
    void rejectProperty_RedirectsToCustomUrl() throws Exception {
        mockMvc.perform(post("/properties/reject/7")
                        .param("redirectUrl", "/dashboard?pending"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard?pending"));

        verify(propertyService).rejectProperty(7L);
    }

    // ── POST /properties/feature/{id} ─────────────────────────────────────────

    @Test
    @DisplayName("POST /properties/feature/{id} calls toggleFeatured and redirects to /dashboard#admin-panel")
    void toggleFeatured_CallsServiceAndRedirects() throws Exception {
        mockMvc.perform(post("/properties/feature/3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard#admin-panel"));

        verify(propertyService).toggleFeatured(3L);
    }
}
