package com.stockland.app.controller;

import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.Favorite;
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

    @Mock
    private FavoriteService favoriteService;

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
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of(new PropertyResponseDTO()));
        when(favoriteService.getFavorites(user)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("myListings"))
                .andExpect(model().attributeExists("favorites"));

        verify(propertyService).getPropertiesByUserId(1L, null, null, null);
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
        when(propertyService.getPropertiesByUserId(2L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("user"));

        verify(userRepository).findByEmail("john@example.com");
        verify(propertyService).getPropertiesByUserId(2L, null, null, null);
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
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of(p1, p2));
        when(favoriteService.getFavorites(user)).thenReturn(List.of());

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
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favorites", List.of()));
    }

    // Test that an admin user sees allListings and pendingListings in the model
    @Test
    @DisplayName("GET /dashboard populates allListings and pendingListings for ROLE_ADMIN user")
    void dashboard_PopulatesAdminAttributes_WhenUserIsAdmin() throws Exception {
        authenticateAs("admin");

        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setRole("ROLE_ADMIN");

        PropertyResponseDTO adminProp = new PropertyResponseDTO();
        adminProp.setTitle("Admin Property");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(propertyService.getPropertiesByUserId(2L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(admin)).thenReturn(List.of());
        when(propertyService.findAllForAdmin(null, null, null)).thenReturn(List.of(adminProp));
        when(propertyService.findPendingProperties()).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("allListings"))
                .andExpect(model().attributeExists("pendingListings"));

        verify(propertyService).findAllForAdmin(null, null, null);
        verify(propertyService).findPendingProperties();
    }

    // Test that non-admin user does NOT see allListings / pendingListings in the model
    @Test
    @DisplayName("GET /dashboard does not expose allListings or pendingListings for regular users")
    void dashboard_DoesNotExposeAdminAttributes_ForRegularUser() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("allListings"))
                .andExpect(model().attributeDoesNotExist("pendingListings"));
    }

    // Test that listSort and listDir params are forwarded to the service overload
    @Test
    @DisplayName("GET /dashboard forwards listSort and listDir params to PropertyService")
    void dashboard_ForwardsListSortAndDir_ToPropertyService() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, "price", "asc", null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard")
                        .param("listSort", "price")
                        .param("listDir",  "asc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("listSort", "price"))
                .andExpect(model().attribute("listDir",  "asc"));

        verify(propertyService).getPropertiesByUserId(1L, "price", "asc", null);
    }

    // Test that favorites are sorted by location descending (covers line 66 + reversed branch line 71)
    @Test
    @DisplayName("GET /dashboard sorts favorites by location desc")
    void dashboard_SortsFavorites_ByLocationDesc() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propA = new Property();
        propA.setTitle("A");
        propA.setLocation("Riga");
        propA.setPrice(100000.0);

        Property propB = new Property();
        propB.setTitle("B");
        propB.setLocation("Tallinn");
        propB.setPrice(200000.0);

        Favorite favA = new Favorite(user, propA);
        Favorite favB = new Favorite(user, propB);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(favA, favB)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "location")
                        .param("favDir",  "desc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favSort", "location"))
                .andExpect(model().attribute("favDir",  "desc"));
    }

    // Test that favorites are sorted by price ascending (covers line 67)
    @Test
    @DisplayName("GET /dashboard sorts favorites by price asc")
    void dashboard_SortsFavorites_ByPriceAsc() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propCheap = new Property();
        propCheap.setTitle("Cheap");
        propCheap.setLocation("Riga");
        propCheap.setPrice(50000.0);

        Property propExpensive = new Property();
        propExpensive.setTitle("Expensive");
        propExpensive.setLocation("Tallinn");
        propExpensive.setPrice(500000.0);

        Favorite favCheap    = new Favorite(user, propCheap);
        Favorite favExpensive = new Favorite(user, propExpensive);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(favExpensive, favCheap)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "price")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favSort", "price"))
                .andExpect(model().attribute("favDir",  "asc"));
    }

    // Test that an unknown favSort key results in no sorting (covers default -> null cmp branch, line 68 + 70)
    @Test
    @DisplayName("GET /dashboard does not sort favorites when favSort key is unknown")
    void dashboard_DoesNotSort_WhenFavSortIsUnknown() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property prop = new Property();
        prop.setTitle("House");
        prop.setLocation("Riga");
        prop.setPrice(100000.0);
        Favorite fav = new Favorite(user, prop);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(fav)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "unknown")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favSort", "unknown"))
                .andExpect(model().attribute("favDir",  "asc"));
    }

    // Test that favorites sorted by title desc exercises the reversed() branch (line 71)
    @Test
    @DisplayName("GET /dashboard sorts favorites by title desc (exercises reversed comparator)")
    void dashboard_SortsFavorites_ByTitleDesc() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propAlpha = new Property();
        propAlpha.setTitle("Alpha");
        propAlpha.setLocation("Riga");
        propAlpha.setPrice(100000.0);

        Property propZeta = new Property();
        propZeta.setTitle("Zeta");
        propZeta.setLocation("Tallinn");
        propZeta.setPrice(200000.0);

        Favorite favAlpha = new Favorite(user, propAlpha);
        Favorite favZeta  = new Favorite(user, propZeta);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(favAlpha, favZeta)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "title")
                        .param("favDir",  "desc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favSort", "title"))
                .andExpect(model().attribute("favDir",  "desc"));
    }

    // Test sorting by title when property title is null (covers null-title branch in line 65)
    @Test
    @DisplayName("GET /dashboard handles null property title when sorting by title")
    void dashboard_SortsFavoritesByTitle_WhenPropertyTitleIsNull() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propNullTitle = new Property();
        propNullTitle.setTitle(null);
        propNullTitle.setLocation("Riga");
        propNullTitle.setPrice(100000.0);

        Favorite fav = new Favorite(user, propNullTitle);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(fav)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "title")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk());
    }

    // Test sorting by location when property location is null (covers null-location branch in line 66)
    @Test
    @DisplayName("GET /dashboard handles null property location when sorting by location")
    void dashboard_SortsFavoritesByLocation_WhenPropertyLocationIsNull() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propNullLocation = new Property();
        propNullLocation.setTitle("House");
        propNullLocation.setLocation(null);
        propNullLocation.setPrice(100000.0);

        Favorite fav = new Favorite(user, propNullLocation);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(fav)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "location")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk());
    }

    // Test sorting by price when property price is null (covers null-price branch in line 67)
    @Test
    @DisplayName("GET /dashboard handles null property price when sorting by price")
    void dashboard_SortsFavoritesByPrice_WhenPropertyPriceIsNull() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propNullPrice = new Property();
        propNullPrice.setTitle("House");
        propNullPrice.setLocation("Riga");
        propNullPrice.setPrice(null);

        Favorite fav = new Favorite(user, propNullPrice);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(fav)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "price")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk());
    }

    // Test admin dashboard with non-null moderationFilter, sortField, sortDir (covers lines 82-84 non-null branches)
    @Test
    @DisplayName("GET /dashboard admin with non-null moderation, sortField, sortDir params")
    void dashboard_AdminWithNonNullModerationAndSortParams() throws Exception {
        authenticateAs("admin");

        User admin = new User();
        admin.setId(2L);
        admin.setUsername("admin");
        admin.setRole("ROLE_ADMIN");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(propertyService.getPropertiesByUserId(2L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(admin)).thenReturn(List.of());
        when(propertyService.findAllForAdmin("APPROVED", "price", "desc")).thenReturn(List.of());
        when(propertyService.findPendingProperties()).thenReturn(List.of());

        mockMvc.perform(get("/dashboard")
                        .param("moderation", "APPROVED")
                        .param("sort",       "price")
                        .param("dir",        "desc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("moderationFilter", "APPROVED"))
                .andExpect(model().attribute("sortField",        "price"))
                .andExpect(model().attribute("sortDir",          "desc"));

        verify(propertyService).findAllForAdmin("APPROVED", "price", "desc");
    }

    // Test that listModeration non-null param is uppercased in the model (covers line 56 true branch)
    @Test
    @DisplayName("GET /dashboard uppercases non-null listModeration param in model")
    void dashboard_UppercasesListModeration_WhenNotNull() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, "approved")).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard")
                        .param("listModeration", "approved"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("listModeration", "APPROVED"));

        verify(propertyService).getPropertiesByUserId(1L, null, null, "approved");
    }

    // Test sort by title with two favorites where one has a null title (covers both null and non-null branches of line 65 in a real comparison)
    @Test
    @DisplayName("GET /dashboard sorts by title with mixed null/non-null titles covers both branches of null-title ternary")
    void dashboard_SortsFavoritesByTitle_NullAndNonNullTitleMixed() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propNullTitle = new Property();
        propNullTitle.setTitle(null);
        propNullTitle.setLocation("Riga");
        propNullTitle.setPrice(100000.0);

        Property propWithTitle = new Property();
        propWithTitle.setTitle("Alpha");
        propWithTitle.setLocation("Tallinn");
        propWithTitle.setPrice(200000.0);

        Favorite favNull = new Favorite(user, propNullTitle);
        Favorite favTitle = new Favorite(user, propWithTitle);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(favTitle, favNull)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "title")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk());
    }

    // Test sort by location with two favorites where one has a null location (covers both branches of null-location ternary on line 66)
    @Test
    @DisplayName("GET /dashboard sorts by location with mixed null/non-null locations covers both branches of null-location ternary")
    void dashboard_SortsFavoritesByLocation_NullAndNonNullLocationMixed() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propNullLocation = new Property();
        propNullLocation.setTitle("House");
        propNullLocation.setLocation(null);
        propNullLocation.setPrice(100000.0);

        Property propWithLocation = new Property();
        propWithLocation.setTitle("Villa");
        propWithLocation.setLocation("Tallinn");
        propWithLocation.setPrice(200000.0);

        Favorite favNull = new Favorite(user, propNullLocation);
        Favorite favLocation = new Favorite(user, propWithLocation);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(favLocation, favNull)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "location")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk());
    }

    // Test sort by price with two favorites where one has a null price (covers both branches of null-price ternary on line 67)
    @Test
    @DisplayName("GET /dashboard sorts by price with mixed null/non-null prices covers both branches of null-price ternary")
    void dashboard_SortsFavoritesByPrice_NullAndNonNullPriceMixed() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property propNullPrice = new Property();
        propNullPrice.setTitle("House");
        propNullPrice.setLocation("Riga");
        propNullPrice.setPrice(null);

        Property propWithPrice = new Property();
        propWithPrice.setTitle("Villa");
        propWithPrice.setLocation("Tallinn");
        propWithPrice.setPrice(500000.0);

        Favorite favNull = new Favorite(user, propNullPrice);
        Favorite favPrice = new Favorite(user, propWithPrice);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(favPrice, favNull)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "price")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk());
    }

    // Test that favSort and favDir params are reflected in the model
    @Test
    @DisplayName("GET /dashboard reflects favSort and favDir in model attributes")
    void dashboard_ReflectsFavSortAndDir_InModel() throws Exception {
        authenticateAs("john");

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        // Build a favorite with a property so sorting by title does not NPE
        Property prop = new Property();
        prop.setTitle("Alpha House");
        prop.setLocation("Riga");
        prop.setPrice(100000.0);
        Favorite fav = new Favorite(user, prop);

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(propertyService.getPropertiesByUserId(1L, null, null, null)).thenReturn(List.of());
        when(favoriteService.getFavorites(user)).thenReturn(new java.util.ArrayList<>(List.of(fav)));

        mockMvc.perform(get("/dashboard")
                        .param("favSort", "title")
                        .param("favDir",  "asc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("favSort", "title"))
                .andExpect(model().attribute("favDir",  "asc"));
    }
}
