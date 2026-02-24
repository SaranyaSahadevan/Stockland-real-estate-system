package com.stockland.app.service;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.Image;
import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import com.stockland.app.repository.ImageRepository;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.model.ActionType;
import com.stockland.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.hibernate.internal.util.MutableLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PropertyService {

    @Autowired
    CloudinaryServiceImpl cloudinaryService;

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;

    public PropertyService(PropertyRepository propertyRepository, UserRepository userRepository, ImageRepository imageRepository){
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
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

        List<Image> storedImages = property.getImages();
        int s = (storedImages != null) ? storedImages.size(): 0;
        String[] imageUrls = new String[s];

        if(storedImages != null){
            for(int i=0; i<s; i++){
                imageUrls[i] = storedImages.get(i).getUrl();
            }
        }

        return PropertyResponseDTO
                .builder()
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
                .images(imageUrls)
                .build();
    }

//    private boolean isValidActionType(String input){
//        for(var type : ActionType.values()){
//            if(type.name().equalsIgnoreCase(input)){
//                return true;
//            }
//        }
//
//        return false;
//    }

    @Transactional
    public PropertyResponseDTO saveProperty(PropertyRequestDTO propertyRequestDTO, Long userId, MultipartFile[] files) {
        Property newProperty = PropertyBuilder(propertyRequestDTO);

        Optional<User> optionalUser = userRepository.findById(userId);

        if(optionalUser.isEmpty()){
            throw new RuntimeException("User couldn't be found when adding new property: " + userId);
        }

        User foundUser = optionalUser.get();

        newProperty.setUser(foundUser);

        Property savedProperty = propertyRepository.save(newProperty);

        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    Map uploadResult = cloudinaryService.uploadFile(file, "properties");

                    Image img = Image
                            .builder()
                            .url(uploadResult.get("secure_url").toString())
                            .public_id(uploadResult.get("public_id").toString())
                            .property(savedProperty)
                            .build();

                    imageRepository.save(img);
                    savedProperty.getImages().add(img);
                }
            }
        }

        return PropertyResponseDTOBuilder(savedProperty);
    }

    @Transactional
    public PropertyResponseDTO findById(long id) {
        Optional<Property> propertyOptional = propertyRepository.findById(id);

        if (propertyOptional.isEmpty()) {
            throw new RuntimeException("Property not found with id: " + id);
        }

        Property property = propertyOptional.get();

        return PropertyResponseDTOBuilder(property);
    }

    @Transactional
    public void deleteById(long id) {
        propertyRepository.deleteById(id);
    }

//    public List<PropertyResponseDTO> findPropertiesByUser(Long userId){
//        List<Property> propertyList = propertyRepository.findAll();
//
//        List<Property> propertyListByUser = new ArrayList<>();
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        Optional<User> user = userRepository.findById(userId);
//
//        if(user.isEmpty()){
//            throw new RuntimeException("Provided user id does not exist when finding properties by user: " + userId);
//        }
//
//        for(var property : propertyList){
//            if(property.getUser().getId().equals(userId)){
//                propertyListByUser.add(property);
//            }
//        }
//
//        for(var property :  propertyListByUser){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

//    public List<PropertyResponseDTO> findByLocation(String location) {
//        List<Property> propertyList = propertyRepository.findByLocationContainingIgnoreCase(location);
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        for(var property :  propertyList){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

//    public List<PropertyResponseDTO> findByPrice(Double price){
//        List<Property> propertyList = propertyRepository.findByPrice(price);
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        for(var property :  propertyList){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

//    public List<PropertyResponseDTO> findByPriceLessThanEqual(Double price){
//        List<Property> propertyList = propertyRepository.findByPriceLessThanEqual(price);
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        for(var property :  propertyList){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

//    public List<PropertyResponseDTO> findByPriceGreaterThanEqual(Double price){
//        List<Property> propertyList = propertyRepository.findByPriceGreaterThanEqual(price);
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        for(var property :  propertyList){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

//    public List<PropertyResponseDTO> findByPriceBetween(Double min, Double max){
//        List<Property> propertyList = propertyRepository.findByPriceBetween(min, max);
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        for(var property :  propertyList){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

//    // Finds by property type: BUY, SELL
//    public List<PropertyResponseDTO> findByActionType(String propertyType){
//        boolean valid = isValidActionType(propertyType);
//
//        if(!valid){
//            return List.of();
//        }
//
//        ActionType type = ActionType.valueOf(propertyType.toUpperCase());
//
//        List<Property> propertyList = propertyRepository.findByActionType(type);
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        for(var property :  propertyList){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

//    public List<PropertyResponseDTO> findByStatus(String status){
//        List<Property> propertyList = propertyRepository.findByStatus(status);
//
//        List<PropertyResponseDTO> responseList = new ArrayList<>();
//
//        for(var property :  propertyList){
//            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);
//
//            responseList.add(newProperty);
//        }
//
//        return responseList;
//    }

    @Transactional
    public List<PropertyResponseDTO> findAll(){
        List<Property> propertyList = propertyRepository.findAll();

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for(var property :  propertyList){
            PropertyResponseDTO newProperty = PropertyResponseDTOBuilder(property);

            responseList.add(newProperty);
        }

        return responseList;
    }

    @Transactional
    public Page<PropertyResponseDTO> searchPropertiesWithFilterSortAndPagination(
            PropertyFilterRequestDTO filters,
            Pageable pageable
    ){
        Specification<Property> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (filters.getLocation() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("location")), "%" + filters.getLocation().toLowerCase() + "%"));
        }

        if (filters.getMinPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), filters.getMinPrice()));
        }

        if (filters.getMaxPrice() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), filters.getMaxPrice()));
        }

        if(filters.getActionType() != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("actionType"), filters.getActionType()));
        }

        if(filters.getPropertyType() != null){
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("propertyType"), filters.getPropertyType()));
        }

        if (filters.getStatus() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("status")), "%" + filters.getStatus().toLowerCase() + "%"));
        }

        Page<Property> entities = propertyRepository.findAll(spec, pageable);

        return entities.map(entity -> PropertyResponseDTOBuilder(entity));
    }

    @Transactional
    public List<PropertyResponseDTO> getPropertiesByUserId(Long userId) {
        List<Property> properties = propertyRepository.findByUserId(userId);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for (Property property : properties) {
            responseList.add(PropertyResponseDTOBuilder(property));
        }

        return responseList;
    }

    @Transactional
    public Page<PropertyResponseDTO> findAll(Pageable pageable) {
        Page<Property> entities = propertyRepository.findAll(pageable);
        return entities.map(this::PropertyResponseDTOBuilder);
    }
}
