package com.stockland.app.service;

import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.Property;
import com.stockland.app.model.PropertyRepository;
import com.stockland.app.model.PropertyType;
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

    private boolean isValid(String input){
        for(var type : PropertyType.values()){
            if(type.name().equalsIgnoreCase(input)){
                return true;
            }
        }

        return false;
    }

    public PropertyResponseDTO saveProperty(PropertyRequestDTO propertyRequestDTO) {
        Property newProperty = PropertyBuilder(propertyRequestDTO);

        propertyRepository.save(newProperty);

        return PropertyResponseDTOBuilder(newProperty);
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

    public List<PropertyResponseDTO> findByPrice(Double price){
        List<Property> propertyList = propertyRepository.findByPrice(price);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    public List<PropertyResponseDTO> findByPriceLessThanEqual(Double price){
        List<Property> propertyList = propertyRepository.findByPriceLessThanEqual(price);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    public List<PropertyResponseDTO> findByPriceGreaterThanEqual(Double price){
        List<Property> propertyList = propertyRepository.findByPriceGreaterThanEqual(price);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    public List<PropertyResponseDTO> findByPriceBetween(Double min, Double max){
        List<Property> propertyList = propertyRepository.findByPriceBetween(min, max);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    // Finds by property type: BUY, SELL
    public List<PropertyResponseDTO> findByPropertyType(String propertyType){
        boolean valid = isValid(propertyType);

        if(!valid){
            return List.of();
        }

        PropertyType type = PropertyType.valueOf(propertyType.toUpperCase());

        List<Property> propertyList = propertyRepository.findByPropertyType(type);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    public List<PropertyResponseDTO> findByStatus(String status){
        List<Property> propertyList = propertyRepository.findByStatus(status);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    public List<PropertyResponseDTO> findAll(){
        List<Property> propertyList = propertyRepository.findAll();

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }
}
