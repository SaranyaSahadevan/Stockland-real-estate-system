package com.stockland.app.service;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.Property;
import com.stockland.app.model.PropertyType;
import com.stockland.app.model.User;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static com.stockland.app.model.ActionType.BUY;
import static com.stockland.app.model.PropertyType.HOUSE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PropertyServiceIntegrationTest {
    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    private void saveProperty(String title, String location, Double price, String description, ActionType actionType, PropertyType propertyType, String status,
                                String username, String password, String role) {
        User us = new User();
        us.setUsername(username);
        us.setPassword(password);
        us.setRole(role);
        userRepository.save(us);

        Property p = new Property();
        p.setTitle(title);
        p.setLocation(location);
        p.setPrice(price);
        p.setDescription(description);
        p.setActionType(actionType);
        p.setPropertyType(propertyType);
        p.setStatus(status);
        p.setUser(us);
        propertyRepository.save(p);
    }

    @Test
    void testSearchByLocation() {
        saveProperty("villa", "London", 500000.0, "very expensive and luxurious", BUY, HOUSE, "new property", "Anthony", "password", "user");
        saveProperty("mansion","Paris", 300000.0, "very expensive and luxurious", BUY, HOUSE, "new property", "Thomas", "password1", "user");

        PropertyFilterRequestDTO dto = new PropertyFilterRequestDTO();
        dto.setLocation("London");

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(dto, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLocation()).isEqualTo("London");
    }

    @Test
    void testSearchByPrice() {
        saveProperty("villa", "London", 500000.0, "very expensive and luxurious", BUY, HOUSE, "new property", "Anthony", "password", "user");
        saveProperty("mansion","Paris", 300000.0, "very expensive and luxurious", BUY, HOUSE, "new property", "Thomas", "password1", "user");

        PropertyFilterRequestDTO dto = new PropertyFilterRequestDTO();
        dto.setLocation("London");
        dto.setMaxPrice(600000.0);
        dto.setMinPrice(400000.0);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(dto, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLocation()).isEqualTo("London");
        assertThat(result.getContent().get(0).getPrice()).isEqualTo(500000.0);
    }
}
