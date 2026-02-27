package com.stockland.app.service;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.*;
import com.stockland.app.repository.FavoriteRepository;
import com.stockland.app.repository.ImageRepository;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PropertyServiceIntegrationTest {

    @MockitoBean
    private CloudinaryServiceImpl cloudinaryService;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    // The setUp method initializes the test data before each test case is executed.
    @BeforeEach
    void setUp() {
        favoriteRepository.deleteAll();
        imageRepository.deleteAll();
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        User savedUser = userRepository.save(User.builder()
                .username("john")
                .email("john@example.com")
                .password("encoded")
                .role("ROLE_USER")
                .fullName("John Doe")
                .build());

        // area=80, rooms=3, price=120k, BUY, APARTMENTS, available, APPROVED
        propertyRepository.save(Property.builder()
                .title("Downtown Apartment")
                .location("Riga")
                .price(120000.0)
                .area(80.0)
                .roomCount(3)
                .actionType(ActionType.BUY)
                .propertyType(PropertyType.APARTMENTS)
                .status("available")
                .moderationStatus(ModerationStatus.APPROVED)
                .user(savedUser)
                .build());

        // area=200, rooms=6, price=300k, RENT, HOUSE, sold, APPROVED
        propertyRepository.save(Property.builder()
                .title("City House")
                .location("Jurmala")
                .price(300000.0)
                .area(200.0)
                .roomCount(6)
                .actionType(ActionType.RENT)
                .propertyType(PropertyType.HOUSE)
                .status("sold")
                .moderationStatus(ModerationStatus.APPROVED)
                .user(savedUser)
                .build());

        // PENDING — must never appear in search results (base spec filters it out)
        propertyRepository.save(Property.builder()
                .title("Pending Property")
                .location("Liepaja")
                .price(50000.0)
                .area(50.0)
                .roomCount(1)
                .actionType(ActionType.BUY)
                .propertyType(PropertyType.APARTMENTS)
                .status("available")
                .moderationStatus(ModerationStatus.PENDING)
                .user(savedUser)
                .build());
    }

    // ── base moderationStatus == APPROVED spec ────────────────────────────────

    @Test
    @DisplayName("Base spec — only APPROVED properties returned; PENDING excluded")
    void searchProperties_BaseSpec_ExcludesPending() {
        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(new PropertyFilterRequestDTO(), Pageable.unpaged());

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().noneMatch(p -> p.getTitle().equals("Pending Property")));
    }

    // ── location ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter by location — cb.like on location executes")
    void searchProperties_FilterByLocation_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("riga");

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("Downtown Apartment", result.getContent().get(0).getTitle());
    }

    // ── price ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter by minPrice — cb.greaterThanOrEqualTo on price executes")
    void searchProperties_FilterByMinPrice_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setMinPrice(200000.0);

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("City House", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Filter by maxPrice — cb.lessThanOrEqualTo on price executes")
    void searchProperties_FilterByMaxPrice_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setMaxPrice(150000.0);

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("Downtown Apartment", result.getContent().get(0).getTitle());
    }

    // ── area ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter by minArea — cb.greaterThanOrEqualTo on area executes")
    void searchProperties_FilterByMinArea_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setMinArea(150.0);   // only City House (200) qualifies

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("City House", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Filter by maxArea — cb.lessThanOrEqualTo on area executes")
    void searchProperties_FilterByMaxArea_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setMaxArea(100.0);   // only Downtown Apartment (80) qualifies

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("Downtown Apartment", result.getContent().get(0).getTitle());
    }

    // ── rooms ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter by minRooms — cb.greaterThanOrEqualTo on roomCount executes")
    void searchProperties_FilterByMinRooms_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setMinRooms(5);   // only City House (6) qualifies

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("City House", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Filter by maxRooms — cb.lessThanOrEqualTo on roomCount executes")
    void searchProperties_FilterByMaxRooms_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setMaxRooms(4);   // only Downtown Apartment (3) qualifies

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("Downtown Apartment", result.getContent().get(0).getTitle());
    }

    // ── actionType ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter by actionType — cb.equal on actionType executes")
    void searchProperties_FilterByActionType_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setActionType(ActionType.RENT);

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("City House", result.getContent().get(0).getTitle());
    }

    // ── propertyType ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter by propertyType — cb.equal on propertyType executes")
    void searchProperties_FilterByPropertyType_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setPropertyType(PropertyType.APARTMENTS);

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("Downtown Apartment", result.getContent().get(0).getTitle());
    }

    // ── status ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter by status — cb.like on status executes")
    void searchProperties_FilterByStatus_ExecutesLambda() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setStatus("sold");

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("City House", result.getContent().get(0).getTitle());
    }

    // ── no filters ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("No filters — all APPROVED properties returned")
    void searchProperties_NoFilters_ReturnsAll() {
        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(new PropertyFilterRequestDTO(), Pageable.unpaged());

        assertEquals(2, result.getTotalElements());
    }

    // ── all filters combined ──────────────────────────────────────────────────

    @Test
    @DisplayName("All filters combined — only exact match returned")
    void searchProperties_AllFilters_ReturnsExactMatch() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("riga");
        filter.setMinPrice(100000.0);
        filter.setMaxPrice(150000.0);
        filter.setMinArea(50.0);
        filter.setMaxArea(100.0);
        filter.setMinRooms(2);
        filter.setMaxRooms(4);
        filter.setActionType(ActionType.BUY);
        filter.setPropertyType(PropertyType.APARTMENTS);
        filter.setStatus("available");

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("Downtown Apartment", result.getContent().get(0).getTitle());
    }

    // ── no match ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Filter with no match returns empty page")
    void searchProperties_NoMatch_ReturnsEmpty() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("nonexistent-city");

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(0, result.getTotalElements());
    }
}
