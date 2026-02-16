package com.stockland.app.service;

import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.Property;
import com.stockland.app.model.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository){
        this.propertyRepository = propertyRepository;
    }

    private Property PropertyBuilder(PropertyRequestDTO propertyRequestDTO){
        return Property
                .builder()
                .title(propertyRequestDTO.getTitle())
                .location(propertyRequestDTO.getLocation())
                .price(propertyRequestDTO.getPrice())
                .description(propertyRequestDTO.getDescription())
                .propertyType(propertyRequestDTO.getPropertyType())
                .status(propertyRequestDTO.getStatus())
                .build();
    }

    private PropertyResponseDTO PropertyResponseDTOBuilder(Property property){
        return PropertyResponseDTO
                .builder()
                .title(property.getTitle())
                .location(property.getLocation())
                .price(property.getPrice())
                .description(property.getDescription())
                .propertyType(property.getPropertyType())
                .status(property.getStatus())
                .build();
    }

    public Property saveProperty(PropertyRequestDTO propertyRequestDTO) {
        Property newProperty = PropertyBuilder(propertyRequestDTO);

        return propertyRepository.save(newProperty);
    }

    public PropertyResponseDTO findById(long id) {
        Optional<Property> propertyOptional = propertyRepository.findById(id);

        if (propertyOptional.isEmpty()) {
            throw new RuntimeException("Property not found with id: " + id);
        }

        Property property = propertyOptional.get();

        return PropertyResponseDTOBuilder(property);
    }

    public void deleteById(long id) {
        propertyRepository.deleteById(id);
    }

    public List<PropertyResponseDTO> findByLocation(String location) {
        List<Property> propertyList = propertyRepository.findByLocationContainingIgnoreCase(location);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    List<PropertyResponseDTO> findByPrice(Double price){
        List<Property> propertyList = propertyRepository.findByPrice(price);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    List<PropertyResponseDTO> findByPriceLessThanEqual(Double price){
        List<Property> propertyList = propertyRepository.findByPriceLessThanEqual(price);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    List<PropertyResponseDTO> findByPriceGreaterThanEqual(Double price){
        List<Property> propertyList = propertyRepository.findByPriceGreaterThanEqual(price);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    List<PropertyResponseDTO> findByPriceBetween(Double min, Double max){
        List<Property> propertyList = propertyRepository.findByPriceBetween(min, max);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }


}
