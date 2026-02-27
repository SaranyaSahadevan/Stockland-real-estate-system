package com.stockland.app.service;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.Image;
import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import com.stockland.app.repository.FavoriteRepository;
import com.stockland.app.repository.ImageRepository;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.model.ModerationStatus;
import com.stockland.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final FavoriteRepository favoriteRepository;

    public PropertyService(PropertyRepository propertyRepository, UserRepository userRepository, ImageRepository imageRepository, FavoriteRepository favoriteRepository){
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
        this.favoriteRepository = favoriteRepository;
    }

    private Property PropertyBuilder(PropertyRequestDTO propertyRequestDTO){
        return Property
                .builder()
                .title(propertyRequestDTO.getTitle())
                .location(propertyRequestDTO.getLocation())
                .price(parsePrice(propertyRequestDTO.getPrice()))
                .description(propertyRequestDTO.getDescription())
                .actionType(propertyRequestDTO.getActionType())
                .propertyType(propertyRequestDTO.getPropertyType())
                .status(propertyRequestDTO.getStatus())
                .roomCount(propertyRequestDTO.getRoomCount())
                .area(propertyRequestDTO.getArea())
                .build();
    }

    private Double parsePrice(String price) {
        if (price == null || price.isBlank()) return null;
        return Double.parseDouble(price.replace(",", "."));
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
                .moderationStatus(property.getModerationStatus())
                .userID(user.getId())
                .username(user.getUsername())
                .images(imageUrls)
                .Area(property.getArea())
                .roomCount(property.getRoomCount())
                .createdAt(property.getCreatedAt())
                .featured(property.isFeatured())
                .build();
    }

    public Property getPropertyById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
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
        newProperty.setModerationStatus(ModerationStatus.PENDING);

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
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        favoriteRepository.deleteByProperty(property);
        propertyRepository.delete(property);
    }

//    public PropertyResponseDTO updateProperty(Long id, PropertyRequestDTO dto) {
//        return updateProperty(id, dto, true);
//    }
    @Transactional
    public PropertyResponseDTO updateProperty(Long id, PropertyRequestDTO dto, MultipartFile[] newImages, List<String> imageUrlsToDelete, boolean isAdmin) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));

        // Deletes marked images
        if(imageUrlsToDelete != null && !imageUrlsToDelete.isEmpty()){
            for(String url : imageUrlsToDelete){
                Optional<Image> optionalImg = imageRepository.findByUrl(url);
                if (!optionalImg.isEmpty()) {
                    Image img = optionalImg.get();

                    cloudinaryService.deleteFile(img.getPublic_id());

                    imageRepository.delete(img);
                    property.getImages().remove(img);
                }
            }
        }

        property.setTitle(dto.getTitle());
        property.setLocation(dto.getLocation());
        property.setPrice(parsePrice(dto.getPrice()));
        property.setDescription(dto.getDescription());
        property.setActionType(dto.getActionType());
        property.setPropertyType(dto.getPropertyType());
        property.setStatus(dto.getStatus());
        property.setArea(dto.getArea());
        property.setRoomCount(dto.getRoomCount());

        // Adds additionally provided images
        if (newImages != null && newImages.length > 0) {
            for (MultipartFile file : newImages) {
                if (!file.isEmpty()) {
                    Map uploadResult = cloudinaryService.uploadFile(file, "properties");
                    Image newImg = Image.builder()
                            .url(uploadResult.get("secure_url").toString())
                            .public_id(uploadResult.get("public_id").toString())
                            .property(property)
                            .build();
                    imageRepository.save(newImg);
                    property.getImages().add(newImg);
                }
            }
        }

        if (!isAdmin) {
            property.setModerationStatus(ModerationStatus.PENDING);
        }

        Property saved = propertyRepository.save(property);
        return PropertyResponseDTOBuilder(saved);
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

    public List<PropertyResponseDTO> findAllForAdmin(String moderationFilter) {
        return findAllForAdmin(moderationFilter, null, null);
    }

    public List<PropertyResponseDTO> findAllForAdmin(String moderationFilter, String sortField, String sortDir) {
        List<Property> propertyList;

        if (moderationFilter != null && !moderationFilter.isBlank()) {
            try {
                ModerationStatus status = ModerationStatus.valueOf(moderationFilter.toUpperCase());
                propertyList = propertyRepository.findByModerationStatus(status);
            } catch (IllegalArgumentException e) {
                propertyList = propertyRepository.findAll();
            }
        } else {
            propertyList = propertyRepository.findAll();
        }

        List<PropertyResponseDTO> responseList = new ArrayList<>();
        for (Property property : propertyList) {
            responseList.add(PropertyResponseDTOBuilder(property));
        }

        boolean desc = "desc".equalsIgnoreCase(sortDir);

        if (sortField != null && !sortField.isBlank()) {
            Comparator<PropertyResponseDTO> comparator = switch (sortField.toLowerCase()) {
                case "id"        -> Comparator.comparingLong(p -> p.getId() != null ? p.getId() : 0L);
                case "title"     -> Comparator.comparing(p -> p.getTitle() != null ? p.getTitle().toLowerCase() : "");
                case "owner"     -> Comparator.comparing(p -> p.getUsername() != null ? p.getUsername().toLowerCase() : "");
                case "price"     -> Comparator.comparingDouble(p -> p.getPrice() != null ? p.getPrice() : 0.0);
                case "location"  -> Comparator.comparing(p -> p.getLocation() != null ? p.getLocation().toLowerCase() : "");
                case "status"    -> Comparator.comparing(p -> p.getStatus() != null ? p.getStatus().toLowerCase() : "");
                case "createdat" -> Comparator.comparing(
                        PropertyResponseDTO::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                case "featured"  -> Comparator.comparingInt(p -> p.isFeatured() ? 0 : 1);
                default          -> null;
            };

            if (comparator != null) {
                if (desc) comparator = comparator.reversed();
                responseList.sort(comparator);
            }
        } else {
            // Default: PENDING first, then APPROVED, then REJECTED; within each group newest first
            responseList.sort(Comparator
                    .<PropertyResponseDTO, Integer>comparing(p -> {
                        if (p.getModerationStatus() == ModerationStatus.PENDING)  return 0;
                        if (p.getModerationStatus() == ModerationStatus.APPROVED) return 1;
                        return 2;
                    })
                    .thenComparing(PropertyResponseDTO::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()))
            );
        }

        return responseList;
    }

    @Transactional
    public Page<PropertyResponseDTO> searchPropertiesWithFilterSortAndPagination(
            PropertyFilterRequestDTO filters,
            Pageable pageable
    ){
        Specification<Property> spec = Specification.where((root, query, cb) ->
                cb.equal(root.get("moderationStatus"), ModerationStatus.APPROVED));

        if (filters.getMinArea() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("area"), filters.getMinArea()));
        }

        if (filters.getMaxArea() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("area"), filters.getMaxArea()));
        }

        if (filters.getMinRooms() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("roomCount"), filters.getMinRooms()));
        }

        if (filters.getMaxRooms() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("roomCount"), filters.getMaxRooms()));
        }

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
        return getPropertiesByUserId(userId, null, null, null);
    }

    public List<PropertyResponseDTO> getPropertiesByUserId(Long userId, String sortField, String sortDir) {
        return getPropertiesByUserId(userId, sortField, sortDir, null);
    }

    public List<PropertyResponseDTO> getPropertiesByUserId(Long userId, String sortField, String sortDir, String moderationFilter) {
        List<Property> properties = propertyRepository.findByUserId(userId);

        List<PropertyResponseDTO> responseList = new ArrayList<>();

        for (Property property : properties) {
            responseList.add(PropertyResponseDTOBuilder(property));
        }

        // Apply moderation filter
        if (moderationFilter != null && !moderationFilter.isBlank()) {
            try {
                ModerationStatus status = ModerationStatus.valueOf(moderationFilter.toUpperCase());
                responseList.removeIf(p -> p.getModerationStatus() != status);
            } catch (IllegalArgumentException ignored) {}
        }

        // Apply sort
        boolean desc = "desc".equalsIgnoreCase(sortDir);
        if (sortField != null && !sortField.isBlank()) {
            Comparator<PropertyResponseDTO> comparator = switch (sortField.toLowerCase()) {
                case "title"     -> Comparator.comparing(p -> p.getTitle() != null ? p.getTitle().toLowerCase() : "");
                case "price"     -> Comparator.comparingDouble(p -> p.getPrice() != null ? p.getPrice() : 0.0);
                case "status"    -> Comparator.comparing(p -> p.getStatus() != null ? p.getStatus().toLowerCase() : "");
                case "createdat" -> Comparator.comparing(
                        PropertyResponseDTO::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()));
                default          -> null;
            };
            if (comparator != null) {
                if (desc) comparator = comparator.reversed();
                responseList.sort(comparator);
            }
        }

        return responseList;
    }

    public PropertyResponseDTO approveProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        property.setModerationStatus(ModerationStatus.APPROVED);
        return PropertyResponseDTOBuilder(propertyRepository.save(property));
    }

    public PropertyResponseDTO rejectProperty(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        property.setModerationStatus(ModerationStatus.REJECTED);
        return PropertyResponseDTOBuilder(propertyRepository.save(property));
    }

    public List<PropertyResponseDTO> findPendingProperties() {
        List<Property> properties = propertyRepository.findByModerationStatus(ModerationStatus.PENDING);
        List<PropertyResponseDTO> responseList = new ArrayList<>();
        for (Property property : properties) {
            responseList.add(PropertyResponseDTOBuilder(property));
        }

        return responseList;
    }

    public PropertyResponseDTO toggleFeatured(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
        property.setFeatured(!property.isFeatured());
        return PropertyResponseDTOBuilder(propertyRepository.save(property));
    }

    public List<PropertyResponseDTO> findFeatured() {
        List<Property> properties = propertyRepository.findByFeaturedTrue();
        List<PropertyResponseDTO> responseList = new ArrayList<>();
        for (Property property : properties) {
            responseList.add(PropertyResponseDTOBuilder(property));
        }
        return responseList;
    }

//    public Page<PropertyResponseDTO> findAll(Pageable pageable) {
//        Page<Property> entities = propertyRepository.findAll(pageable);
//        return entities.map(this::PropertyResponseDTOBuilder);
//    }
}
