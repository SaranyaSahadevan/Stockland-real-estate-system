package com.stockland.app.service;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.Property;
import com.stockland.app.model.PropertyType;
import com.stockland.app.model.User;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PropertyServiceTest {

    private PropertyRepository propertyRepository;
    private UserRepository userRepository;
    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyRepository = mock(PropertyRepository.class);
        userRepository = mock(UserRepository.class);
        propertyService = new PropertyService(propertyRepository, userRepository);
    }

    @Test
    @DisplayName("saveProperty should save property and return DTO")
    void saveProperty_Success() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PropertyRequestDTO dto = new PropertyRequestDTO();
        dto.setTitle("Test Property");

        Property savedProperty = new Property();
        savedProperty.setId(1L);
        savedProperty.setTitle("Test Property");
        savedProperty.setUser(user);

        when(propertyRepository.save(any(Property.class))).thenReturn(savedProperty);

        PropertyResponseDTO result = propertyService.saveProperty(dto, 1L);

        assertNotNull(result);
        assertEquals("Test Property", result.getTitle());
        assertEquals(1L, result.getUserID());
    }

    @Test
    @DisplayName("saveProperty should throw exception if user not found")
    void saveProperty_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        PropertyRequestDTO dto = new PropertyRequestDTO();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> propertyService.saveProperty(dto, 1L));

        assertTrue(exception.getMessage().contains("User couldn't be found"));
    }

    @Test
    @DisplayName("findById should return DTO if property exists")
    void findById_Success() {
        User user = new User();
        user.setId(1L);
        Property property = new Property();
        property.setId(1L);
        property.setTitle("Test Property");
        property.setUser(user);

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        PropertyResponseDTO result = propertyService.findById(1L);
        assertNotNull(result);
        assertEquals("Test Property", result.getTitle());
    }

    @Test
    @DisplayName("findById should throw exception if property not found")
    void findById_NotFound() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> propertyService.findById(1L));

        assertTrue(exception.getMessage().contains("Property not found"));
    }

    @Test
    @DisplayName("searchPropertiesWithFilterSortAndPagination should apply all filters")
    void searchPropertiesWithAllFilters() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("Riga");
        filter.setMinPrice(50000.0);
        filter.setMaxPrice(200000.0);
        filter.setActionType(ActionType.BUY);
        filter.setPropertyType(PropertyType.APARTMENTS);
        filter.setStatus("available");

        Pageable pageable = Pageable.unpaged();

        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property property = new Property();
        property.setId(1L);
        property.setTitle("Test Property");
        property.setLocation("Riga");
        property.setPrice(120000.0);
        property.setActionType(ActionType.BUY);
        property.setPropertyType(PropertyType.APARTMENTS);
        property.setStatus("available");
        property.setUser(user);

        Page<Property> page = new PageImpl<>(List.of(property));

        when(propertyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(filter, pageable);

        assertEquals(1, result.getTotalElements());
        PropertyResponseDTO dto = result.getContent().get(0);
        assertEquals("Test Property", dto.getTitle());
        assertEquals("john", dto.getUsername());
    }

    // Дополнительно — тесты для каждого отдельного фильтра
    @Test
    void searchProperties_FilterByLocationOnly() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("Riga");
        Pageable pageable = Pageable.unpaged();

        Page<Property> page = new PageImpl<>(List.of(buildPropertyWithUser()));
        when(propertyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(filter, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchProperties_FilterByMinMaxPrice() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setMinPrice(1000.0);
        filter.setMaxPrice(5000.0);
        Pageable pageable = Pageable.unpaged();

        Page<Property> page = new PageImpl<>(List.of(buildPropertyWithUser()));
        when(propertyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(filter, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchProperties_FilterByActionAndType() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setActionType(ActionType.RENT);
        filter.setPropertyType(PropertyType.HOUSE);
        Pageable pageable = Pageable.unpaged();

        Page<Property> page = new PageImpl<>(List.of(buildPropertyWithUser()));
        when(propertyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(filter, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void searchProperties_FilterByStatus() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setStatus("sold");
        Pageable pageable = Pageable.unpaged();

        Page<Property> page = new PageImpl<>(List.of(buildPropertyWithUser()));
        when(propertyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(filter, pageable);
        assertEquals(1, result.getTotalElements());
    }

    private Property buildPropertyWithUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        Property property = new Property();
        property.setId(1L);
        property.setUser(user);
        return property;
    }

    @Test
    @DisplayName("getPropertiesByUserId should return list of property DTOs for a given user")
    void getPropertiesByUserId_ReturnsListOfProperties() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");

        Property p1 = new Property();
        p1.setId(1L);
        p1.setTitle("House A");
        p1.setUser(user);

        Property p2 = new Property();
        p2.setId(2L);
        p2.setTitle("Apartment B");
        p2.setUser(user);

        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(p1, p2));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L);

        assertEquals(2, result.size());
        assertEquals("House A", result.get(0).getTitle());
        assertEquals("Apartment B", result.get(1).getTitle());
    }

    @Test
    @DisplayName("getPropertiesByUserId should return empty list when user has no properties")
    void getPropertiesByUserId_ReturnsEmptyList_WhenNoProperties() {
        when(propertyRepository.findByUserId(99L)).thenReturn(List.of());

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(99L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("searchPropertiesWithFilterSortAndPagination should return empty page when no properties match")
    void searchProperties_ReturnsEmptyPage_WhenNoMatch() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("NonExistentCity");
        Pageable pageable = Pageable.unpaged();

        when(propertyRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(filter, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }
}