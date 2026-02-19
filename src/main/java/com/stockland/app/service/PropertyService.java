package com.stockland.app.service;

import com.stockland.app.dto.*;
import com.stockland.app.model.*;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PropertyService(PropertyRepository propertyRepository,
                           UserRepository userRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    private Property buildProperty(@Valid PropertyRequestDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException("Property data required");
        }

        if (!StringUtils.hasText(dto.getTitle())) {
            throw new IllegalArgumentException("Title required");
        }

        if (dto.getPrice() == null || dto.getPrice() <= 0) {
            throw new IllegalArgumentException("Invalid price");
        }

        return Property.builder()
                .title(dto.getTitle().trim())
                .location(dto.getLocation().trim())
                .price(dto.getPrice())
                .description(dto.getDescription())
                .actionType(dto.getActionType())
                .propertyType(dto.getPropertyType())
                .status(dto.getStatus())
                .build();
    }

    private PropertyResponseDTO mapToDTO(Property property) {

        User user = property.getUser();

        return PropertyResponseDTO.builder()
                .id(property.getId())
                .title(property.getTitle())
                .location(property.getLocation())
                .price(property.getPrice())
                .description(property.getDescription())
                .actionType(property.getActionType())
                .propertyType(property.getPropertyType())
                .status(property.getStatus())
                .userID(user.getId())
                .username(user.getUsername())
                .build();
    }

    public PropertyResponseDTO saveProperty(@Valid PropertyRequestDTO dto,
                                            Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("User required");
        }

        Optional<User> userOptional = userRepository.findById(userId);

        User user = userOptional.orElseThrow(
                () -> new RuntimeException("User not found: " + userId)
        );

        Property property = buildProperty(dto);
        property.setUser(user);

        Property saved = propertyRepository.save(property);

        return mapToDTO(saved);
    }

    public PropertyResponseDTO findById(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException("Invalid property ID");
        }

        Property property = propertyRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Property not found: " + id));

        return mapToDTO(property);
    }

    public void deleteById(long id) {

        if (id <= 0) {
            throw new IllegalArgumentException("Invalid property ID");
        }

        propertyRepository.deleteById(id);
    }

    public Page<PropertyResponseDTO> searchPropertiesWithFilterSortAndPagination(
            PropertyFilterRequestDTO filters,
            Pageable pageable
    ) {

        Specification<Property> spec =
                Specification.where((root, query, cb) -> cb.conjunction());

        if (filters != null) {

            if (StringUtils.hasText(filters.getLocation())) {
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("location")),
                                "%" + filters.getLocation().toLowerCase() + "%"));
            }

            if (filters.getMinPrice() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("price"),
                                filters.getMinPrice()));
            }

            if (filters.getMaxPrice() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.lessThanOrEqualTo(root.get("price"),
                                filters.getMaxPrice()));
            }

            if (filters.getActionType() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("actionType"),
                                filters.getActionType()));
            }

            if (filters.getPropertyType() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("propertyType"),
                                filters.getPropertyType()));
            }

            if (StringUtils.hasText(filters.getStatus())) {
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("status")),
                                "%" + filters.getStatus().toLowerCase() + "%"));
            }
        }

        Page<Property> page =
                propertyRepository.findAll(spec, pageable);

        return page.map(this::mapToDTO);
    }
}
