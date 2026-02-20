package com.stockland.app.service;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.*;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(PropertyService.class)
class PropertyServiceIntegrationTest {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    // The setUp method initializes the test data before each test case is executed.
    @BeforeEach
    void setUp() {
        propertyRepository.deleteAll();
        userRepository.deleteAll();

        User savedUser = userRepository.save(User.builder()
                .username("john")
                .email("john@example.com")
                .password("encoded")
                .role("ROLE_USER")
                .fullName("John Doe")
                .build());

        propertyRepository.save(Property.builder()
                .title("Downtown Apartment")
                .location("Riga")
                .price(120000.0)
                .actionType(ActionType.BUY)
                .propertyType(PropertyType.APARTMENTS)
                .status("available")
                .user(savedUser)
                .build());

        propertyRepository.save(Property.builder()
                .title("City House")
                .location("Jurmala")
                .price(300000.0)
                .actionType(ActionType.RENT)
                .propertyType(PropertyType.HOUSE)
                .status("sold")
                .user(savedUser)
                .build());
    }

    // Each test focuses on verifying that the correct lambda is executed for the corresponding filter field

    // For example, when filtering by location, we check that only properties with "riga" in the location are returned,
    // which indicates that the cb.like lambda for location is working as expected.
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

    // Similarly, for price filters, we check that the correct properties are returned based on the minPrice and maxPrice values,
    // which confirms that the cb.greaterThanOrEqualTo and cb.lessThanOrEqual
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

    // By testing each filter individually and then all combined, we can be confident that the correct lambdas are
    // executed for each filter field in the searchPropertiesWithFilterSortAndPagination method.
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

    // For enum filters like actionType and propertyType, we check that only properties matching the specified
    // enum value are returned, which confirms that the cb.equal lambda for those fields is working correctly.
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

    // For the status filter, we check that properties with a status containing the specified string are returned,
    // which confirms that the cb.like lambda for status is functioning as intended.
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

    // Finally, by testing the combination of all filters together, we can confirm that the method correctly applies
    // all the specified filters and returns only the properties that match all criteria, which indicates that
    // all the corresponding lambdas are executed properly in conjunction.
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

    // This test verifies that when no filters are applied, all properties are returned, which confirms that the method
    // correctly handles the case where no filtering criteria are specified and does not apply any unintended filters.
    @Test
    @DisplayName("No filters — all properties returned")
    void searchProperties_NoFilters_ReturnsAll() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(2, result.getTotalElements());
    }

    // This test verifies that when all filters are applied together, only the properties that match all criteria are
    // returned, which confirms that the method correctly combines all the filters and executes the corresponding
    // lambdas for each filter field to return the expected results.
    @Test
    @DisplayName("All filters combined — only exact match returned")
    void searchProperties_AllFilters_ReturnsExactMatch() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("riga");
        filter.setMinPrice(100000.0);
        filter.setMaxPrice(150000.0);
        filter.setActionType(ActionType.BUY);
        filter.setPropertyType(PropertyType.APARTMENTS);
        filter.setStatus("available");

        Page<PropertyResponseDTO> result = propertyService
                .searchPropertiesWithFilterSortAndPagination(filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("Downtown Apartment", result.getContent().get(0).getTitle());
    }
}
