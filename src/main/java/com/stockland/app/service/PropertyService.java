package com.stockland.app.service;

import com.stockland.app.model.Property;
import com.stockland.app.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    // CREATE PROPERTY
    public Property createProperty(Property property) {
        return propertyRepository.save(property);
    }

    // GET ALL PROPERTIES
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    // GET PROPERTY BY ID
    public Property getPropertyById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));
    }

    // UPDATE PROPERTY
    public Property updateProperty(Long id, Property updatedProperty) {
        Property existing = getPropertyById(id);

        existing.setTitle(updatedProperty.getTitle());
        existing.setDescription(updatedProperty.getDescription());
        existing.setPrice(updatedProperty.getPrice());
        existing.setLocation(updatedProperty.getLocation());
        existing.setPropertyType(updatedProperty.getPropertyType());
        existing.setBedrooms(updatedProperty.getBedrooms());
        existing.setBathrooms(updatedProperty.getBathrooms());
        existing.setArea(updatedProperty.getArea());
        existing.setStatus(updatedProperty.getStatus());

        return propertyRepository.save(existing);
    }

    // DELETE PROPERTY
    public void deleteProperty(Long id) {
        propertyRepository.deleteById(id);
    }
}
