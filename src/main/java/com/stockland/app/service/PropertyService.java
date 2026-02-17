package com.stockland.app.service;

import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.model.ActionType;
import com.stockland.app.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public PropertyService(PropertyRepository propertyRepository, UserRepository userRepository){
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
    }

    private Property PropertyBuilder(PropertyRequestDTO propertyRequestDTO){
        return Property
                .builder()
                .title(propertyRequestDTO.getTitle())
                .location(propertyRequestDTO.getLocation())
                .price(propertyRequestDTO.getPrice())
                .description(propertyRequestDTO.getDescription())
                .actionType(propertyRequestDTO.getActionType())
                .propertyType(propertyRequestDTO.getPropertyType())
                .status(propertyRequestDTO.getStatus())
                .build();
    }

    private PropertyResponseDTO PropertyResponseDTOBuilder(Property property){
        User user = property.getUser();

        return PropertyResponseDTO
                .builder()
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

    private boolean isValid(String input){
        for(var type : ActionType.values()){
            if(type.name().equalsIgnoreCase(input)){
                return true;
            }
        }

        return false;
    }

    public PropertyResponseDTO saveProperty(PropertyRequestDTO propertyRequestDTO, Long userId) {
        Property newProperty = PropertyBuilder(propertyRequestDTO);

        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()){
            throw new RuntimeException("User couldn't be found when adding new property: " + userId);
        }

        User foundUser = optionalUser.get();

        newProperty.setUser(foundUser);

        Property savedProperty = propertyRepository.save(newProperty);

        return PropertyResponseDTOBuilder(savedProperty);
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

    public List<PropertyResponseDTO> findPropertiesByUser(Long userId){
        List<Property> propertyList = propertyRepository.findAll();

        List<Property> propertyListByUser = new ArrayList<>();

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        Optional<User> user = userRepository.findById(userId);

        if(user.isEmpty()){
            throw new RuntimeException("Provided user id does not exist when finding properties by user: " + userId);
        }

        for(var property : propertyList){
            if(property.getUser().getId().equals(userId)){
                propertyListByUser.add(property);
            }
        }

        for(var property :  propertyListByUser){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
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
    public List<PropertyResponseDTO> findByActionType(String propertyType){
        boolean valid = isValid(propertyType);

        if(!valid){
            return List.of();
        }

        ActionType type = ActionType.valueOf(propertyType.toUpperCase());

        List<Property> propertyList = propertyRepository.findByActionType(type);

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

//    public List<PropertyResponseDTO> searchPropertiesWithFilterSortAndPagination(){
//
//    }
}
