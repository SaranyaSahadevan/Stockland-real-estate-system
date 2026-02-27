package com.stockland.app.service;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.Image;
import com.stockland.app.model.ModerationStatus; //NOSONAR – used in assertions
import com.stockland.app.model.Property;
import com.stockland.app.model.PropertyType;
import com.stockland.app.model.User;
import com.stockland.app.repository.FavoriteRepository;
import com.stockland.app.repository.ImageRepository;
import com.stockland.app.repository.PropertyRepository;
import com.stockland.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PropertyServiceTest {

    private PropertyRepository propertyRepository;
    private UserRepository userRepository;
    private ImageRepository imageRepository;
    private FavoriteRepository favoriteRepository;
    private CloudinaryServiceImpl cloudinaryService;
    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        propertyRepository   = mock(PropertyRepository.class);
        userRepository       = mock(UserRepository.class);
        imageRepository      = mock(ImageRepository.class);
        favoriteRepository   = mock(FavoriteRepository.class);
        cloudinaryService    = mock(CloudinaryServiceImpl.class);

        propertyService = new PropertyService(propertyRepository, userRepository, imageRepository, favoriteRepository);
        // inject the cloudinary mock via reflection (field is @Autowired)
        try {
            var field = PropertyService.class.getDeclaredField("cloudinaryService");
            field.setAccessible(true);
            field.set(propertyService, cloudinaryService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User buildUser(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private Property buildProperty(long id, User owner) {
        Property p = new Property();
        p.setId(id);
        p.setTitle("House " + id);
        p.setLocation("Riga");
        p.setPrice(100000.0);
        p.setArea(90.0);
        p.setRoomCount(3);
        p.setActionType(ActionType.BUY);
        p.setPropertyType(PropertyType.HOUSE);
        p.setStatus("available");
        p.setModerationStatus(ModerationStatus.PENDING);
        p.setUser(owner);
        p.setImages(new ArrayList<>());
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    private PropertyRequestDTO buildRequestDTO() {
        PropertyRequestDTO dto = new PropertyRequestDTO();
        dto.setTitle("Nice House");
        dto.setLocation("Riga");
        dto.setPrice("150000,00");
        dto.setArea(100.0);
        dto.setRoomCount(3);
        dto.setActionType(ActionType.BUY);
        dto.setPropertyType(PropertyType.HOUSE);
        dto.setStatus("available");
        return dto;
    }

    // ── getPropertyById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getPropertyById returns Property when found")
    void getPropertyById_ReturnsProperty_WhenFound() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        Property result = propertyService.getPropertyById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("getPropertyById throws RuntimeException when not found")
    void getPropertyById_ThrowsException_WhenNotFound() {
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> propertyService.getPropertyById(99L));

        assertTrue(ex.getMessage().contains("Property not found with id: 99"));
    }

    // ── saveProperty ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveProperty saves property and returns DTO when user exists and no files")
    void saveProperty_ReturnsDTO_WhenUserExistsAndNoFiles() {
        User user = buildUser(1L, "john");
        Property saved = buildProperty(1L, user);
        saved.setModerationStatus(ModerationStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenReturn(saved);

        PropertyResponseDTO result = propertyService.saveProperty(buildRequestDTO(), 1L, null);

        assertNotNull(result);
        assertEquals("House 1", result.getTitle());
        assertEquals(ModerationStatus.PENDING, result.getModerationStatus());
        verify(propertyRepository).save(any(Property.class));
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("saveProperty throws RuntimeException when user not found")
    void saveProperty_ThrowsException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> propertyService.saveProperty(buildRequestDTO(), 1L, null));

        assertTrue(ex.getMessage().contains("User couldn't be found"));
        verifyNoInteractions(cloudinaryService, imageRepository);
    }

    @Test
    @DisplayName("saveProperty uploads images to Cloudinary when files are provided")
    void saveProperty_UploadsImages_WhenFilesProvided() {
        User user = buildUser(1L, "john");
        Property saved = buildProperty(1L, user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenReturn(saved);
        when(cloudinaryService.uploadFile(any(), eq("properties")))
                .thenReturn(Map.of("secure_url", "http://img.url", "public_id", "properties/img1"));

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        PropertyResponseDTO result = propertyService.saveProperty(buildRequestDTO(), 1L, new org.springframework.web.multipart.MultipartFile[]{file});

        assertNotNull(result);
        verify(cloudinaryService).uploadFile(eq(file), eq("properties"));

        org.mockito.ArgumentCaptor<Image> imageCaptor = org.mockito.ArgumentCaptor.forClass(Image.class);
        verify(imageRepository).save(imageCaptor.capture());
        Image capturedImage = imageCaptor.getValue();
        assertEquals("http://img.url", capturedImage.getUrl());
        assertEquals("properties/img1", capturedImage.getPublic_id());
        assertEquals(saved, capturedImage.getProperty());
    }

    @Test
    @DisplayName("saveProperty skips empty files during upload")
    void saveProperty_SkipsEmptyFiles() {
        User user = buildUser(1L, "john");
        Property saved = buildProperty(1L, user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenReturn(saved);

        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        propertyService.saveProperty(buildRequestDTO(), 1L, new org.springframework.web.multipart.MultipartFile[]{emptyFile});

        verifyNoInteractions(cloudinaryService);
        verify(imageRepository, never()).save(any());
    }

    @Test
    @DisplayName("saveProperty sets ModerationStatus to PENDING")
    void saveProperty_SetsModerationStatusToPending() {
        User user = buildUser(1L, "john");
        Property saved = buildProperty(1L, user);
        saved.setModerationStatus(ModerationStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenReturn(saved);

        PropertyResponseDTO result = propertyService.saveProperty(buildRequestDTO(), 1L, null);

        assertEquals(ModerationStatus.PENDING, result.getModerationStatus());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns DTO when property exists")
    void findById_ReturnsDTO_WhenExists() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        PropertyResponseDTO result = propertyService.findById(1L);

        assertNotNull(result);
        assertEquals("House 1", result.getTitle());
        assertEquals("john", result.getUsername());
    }

    @Test
    @DisplayName("findById throws RuntimeException when property not found")
    void findById_ThrowsException_WhenNotFound() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> propertyService.findById(1L));

        assertTrue(ex.getMessage().contains("Property not found with id: 1"));
    }

    @Test
    @DisplayName("findById maps image URLs correctly")
    void findById_MapsImageUrls() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        Image img = Image.builder().id(1L).url("http://img1.url").property(property).build();
        property.setImages(new ArrayList<>(List.of(img)));

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        PropertyResponseDTO result = propertyService.findById(1L);

        assertArrayEquals(new String[]{"http://img1.url"}, result.getImages());
    }

    // ── deleteById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteById deletes property when it exists")
    void deleteById_DeletesProperty_WhenExists() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        assertDoesNotThrow(() -> propertyService.deleteById(1L));

        verify(favoriteRepository).deleteByProperty(property);
        verify(propertyRepository).delete(property);
    }

    @Test
    @DisplayName("deleteById throws RuntimeException when property not found")
    void deleteById_ThrowsException_WhenNotFound() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> propertyService.deleteById(1L));

        assertTrue(ex.getMessage().contains("Property not found with id: 1"));
        verify(propertyRepository, never()).delete(any(Property.class));
    }

    // ── updateProperty ────────────────────────────────────────────────────────
    void updateProperty_UpdatesFields_WhenPropertyFound() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PropertyRequestDTO dto = buildRequestDTO();
        dto.setTitle("Updated Title");

        PropertyResponseDTO result = propertyService.updateProperty(1L, dto, null, null, false);

        assertEquals("Updated Title", result.getTitle());
        assertEquals(ModerationStatus.PENDING, result.getModerationStatus()); // isAdmin=false resets to PENDING
        verify(propertyRepository).save(existing);
    }

    @Test
    @DisplayName("updateProperty does not reset moderation status when isAdmin=true")
    void updateProperty_KeepsModerationStatus_WhenAdmin() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        existing.setModerationStatus(ModerationStatus.APPROVED);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponseDTO result = propertyService.updateProperty(1L, buildRequestDTO(), null, null, true);

        assertEquals(ModerationStatus.APPROVED, result.getModerationStatus());
    }

    @Test
    @DisplayName("updateProperty throws RuntimeException when property not found")
    void updateProperty_ThrowsException_WhenNotFound() {
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> propertyService.updateProperty(99L, buildRequestDTO(), null, null, false));

        assertTrue(ex.getMessage().contains("Property not found with id: 99"));
    }

    @Test
    @DisplayName("updateProperty deletes images from Cloudinary for URLs in imageUrlsToDelete")
    void updateProperty_DeletesImages_FromCloudinary() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        Image img = Image.builder().id(1L).url("http://img1.url").public_id("properties/img1").property(existing).build();
        existing.setImages(new ArrayList<>(List.of(img)));

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.findByUrl("http://img1.url")).thenReturn(Optional.of(img));

        propertyService.updateProperty(1L, buildRequestDTO(), null, List.of("http://img1.url"), false);

        verify(cloudinaryService).deleteFile("properties/img1");
        verify(imageRepository).delete(img);
    }

    @Test
    @DisplayName("updateProperty uploads new images to Cloudinary when newImages are provided")
    void updateProperty_UploadsNewImages_WhenProvided() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cloudinaryService.uploadFile(any(), eq("properties")))
                .thenReturn(Map.of("secure_url", "http://new.url", "public_id", "properties/new"));

        MockMultipartFile file = new MockMultipartFile("file", "new.jpg", "image/jpeg", "bytes".getBytes());
        propertyService.updateProperty(1L, buildRequestDTO(), new org.springframework.web.multipart.MultipartFile[]{file}, null, false);

        verify(cloudinaryService).uploadFile(eq(file), eq("properties"));
        verify(imageRepository).save(any(Image.class));
    }

    @Test
    @DisplayName("updateProperty skips imageUrlsToDelete entry when image URL not found in repository")
    void updateProperty_SkipsDelete_WhenImageUrlNotFound() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.findByUrl("http://missing.url")).thenReturn(Optional.empty());

        propertyService.updateProperty(1L, buildRequestDTO(), null, List.of("http://missing.url"), false);

        verifyNoInteractions(cloudinaryService);
        verify(imageRepository, never()).delete(any());
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll returns DTOs for all properties")
    void findAll_ReturnsAllPropertyDTOs() {
        User user = buildUser(1L, "john");
        when(propertyRepository.findAll()).thenReturn(List.of(buildProperty(1L, user), buildProperty(2L, user)));

        List<PropertyResponseDTO> result = propertyService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findAll returns empty list when no properties exist")
    void findAll_ReturnsEmptyList_WhenNoProperties() {
        when(propertyRepository.findAll()).thenReturn(List.of());

        List<PropertyResponseDTO> result = propertyService.findAll();

        assertTrue(result.isEmpty());
    }

    // ── findAllForAdmin ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findAllForAdmin with valid moderationFilter returns filtered properties")
    void findAllForAdmin_ReturnsFiltered_ByModerationStatus() {
        User user = buildUser(1L, "john");
        Property pending = buildProperty(1L, user);
        pending.setModerationStatus(ModerationStatus.PENDING);

        when(propertyRepository.findByModerationStatus(ModerationStatus.PENDING))
                .thenReturn(List.of(pending));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin("PENDING");

        assertEquals(1, result.size());
        assertEquals(ModerationStatus.PENDING, result.get(0).getModerationStatus());
    }

    @Test
    @DisplayName("findAllForAdmin with null filter returns all properties")
    void findAllForAdmin_ReturnsAll_WhenFilterIsNull() {
        User user = buildUser(1L, "john");
        when(propertyRepository.findAll()).thenReturn(List.of(buildProperty(1L, user), buildProperty(2L, user)));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findAllForAdmin with invalid moderationFilter falls back to all properties")
    void findAllForAdmin_FallsBackToAll_WhenFilterInvalid() {
        User user = buildUser(1L, "john");
        when(propertyRepository.findAll()).thenReturn(List.of(buildProperty(1L, user)));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin("INVALID");

        assertEquals(1, result.size());
        verify(propertyRepository).findAll();
    }

    @Test
    @DisplayName("findAllForAdmin sorts by title ascending")
    void findAllForAdmin_SortsByTitleAsc() {
        User user = buildUser(1L, "john");
        Property pB = buildProperty(1L, user); pB.setTitle("B house");
        Property pA = buildProperty(2L, user); pA.setTitle("A house");
        when(propertyRepository.findAll()).thenReturn(List.of(pB, pA));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "title", "asc");

        assertEquals("A house", result.get(0).getTitle());
        assertEquals("B house", result.get(1).getTitle());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by price descending")
    void findAllForAdmin_SortsByPriceDesc() {
        User user = buildUser(1L, "john");
        Property cheap = buildProperty(1L, user); cheap.setPrice(50000.0);
        Property expensive = buildProperty(2L, user); expensive.setPrice(200000.0);
        when(propertyRepository.findAll()).thenReturn(List.of(cheap, expensive));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "price", "desc");

        assertEquals(200000.0, result.get(0).getPrice());
        assertEquals(50000.0, result.get(1).getPrice());
    }

    @Test
    @DisplayName("findAllForAdmin default sort puts PENDING properties first")
    void findAllForAdmin_DefaultSort_PendingFirst() {
        User user = buildUser(1L, "john");
        Property approved = buildProperty(1L, user); approved.setModerationStatus(ModerationStatus.APPROVED);
        Property pending  = buildProperty(2L, user); pending.setModerationStatus(ModerationStatus.PENDING);
        when(propertyRepository.findAll()).thenReturn(List.of(approved, pending));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, null, null);

        assertEquals(ModerationStatus.PENDING, result.get(0).getModerationStatus());
        assertEquals(ModerationStatus.APPROVED, result.get(1).getModerationStatus());
    }

    // ── searchPropertiesWithFilterSortAndPagination ───────────────────────────

    @Test
    @DisplayName("searchProperties returns page of DTOs when properties match")
    void searchProperties_ReturnsPageOfDTOs_WhenMatch() {
        User user = buildUser(1L, "john");
        Page<Property> page = new PageImpl<>(List.of(buildProperty(1L, user)));
        when(propertyRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(
                new PropertyFilterRequestDTO(), Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("House 1", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("searchProperties returns empty page when no properties match")
    void searchProperties_ReturnsEmptyPage_WhenNoMatch() {
        when(propertyRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(
                new PropertyFilterRequestDTO(), Pageable.unpaged());

        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("searchProperties applies all filter fields")
    void searchProperties_AppliesAllFilters() {
        PropertyFilterRequestDTO filter = new PropertyFilterRequestDTO();
        filter.setLocation("Riga");
        filter.setMinPrice(50000.0);
        filter.setMaxPrice(200000.0);
        filter.setMinArea(60.0);
        filter.setMaxArea(150.0);
        filter.setMinRooms(2);
        filter.setMaxRooms(5);
        filter.setActionType(ActionType.BUY);
        filter.setPropertyType(PropertyType.APARTMENTS);
        filter.setStatus("available");

        User user = buildUser(1L, "john");
        Page<Property> page = new PageImpl<>(List.of(buildProperty(1L, user)));
        when(propertyRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<PropertyResponseDTO> result = propertyService.searchPropertiesWithFilterSortAndPagination(
                filter, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        verify(propertyRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // ── getPropertiesByUserId ─────────────────────────────────────────────────

    @Test
    @DisplayName("getPropertiesByUserId returns list of DTOs for a given user")
    void getPropertiesByUserId_ReturnsDTOs() {
        User user = buildUser(1L, "john");
        when(propertyRepository.findByUserId(1L))
                .thenReturn(List.of(buildProperty(1L, user), buildProperty(2L, user)));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getPropertiesByUserId returns empty list when user has no properties")
    void getPropertiesByUserId_ReturnsEmptyList_WhenNoProperties() {
        when(propertyRepository.findByUserId(99L)).thenReturn(List.of());

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getPropertiesByUserId with moderationFilter keeps only matching status")
    void getPropertiesByUserId_FiltersByModerationStatus() {
        User user = buildUser(1L, "john");
        Property approved = buildProperty(1L, user); approved.setModerationStatus(ModerationStatus.APPROVED);
        Property pending  = buildProperty(2L, user); pending.setModerationStatus(ModerationStatus.PENDING);
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(approved, pending));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, null, null, "APPROVED");

        assertEquals(1, result.size());
        assertEquals(ModerationStatus.APPROVED, result.get(0).getModerationStatus());
    }

    @Test
    @DisplayName("getPropertiesByUserId with invalid moderationFilter returns all properties")
    void getPropertiesByUserId_ReturnsAll_WhenModerationFilterInvalid() {
        User user = buildUser(1L, "john");
        when(propertyRepository.findByUserId(1L))
                .thenReturn(List.of(buildProperty(1L, user), buildProperty(2L, user)));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, null, null, "NONSENSE");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getPropertiesByUserId sorts by title ascending")
    void getPropertiesByUserId_SortsByTitleAsc() {
        User user = buildUser(1L, "john");
        Property pZ = buildProperty(1L, user); pZ.setTitle("Z house");
        Property pA = buildProperty(2L, user); pA.setTitle("A house");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pZ, pA));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "title", "asc");

        assertEquals("A house", result.get(0).getTitle());
        assertEquals("Z house", result.get(1).getTitle());
    }

    @Test
    @DisplayName("getPropertiesByUserId sorts by price descending")
    void getPropertiesByUserId_SortsByPriceDesc() {
        User user = buildUser(1L, "john");
        Property cheap     = buildProperty(1L, user); cheap.setPrice(30000.0);
        Property expensive = buildProperty(2L, user); expensive.setPrice(500000.0);
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(cheap, expensive));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "price", "desc");

        assertEquals(500000.0, result.get(0).getPrice());
        assertEquals(30000.0, result.get(1).getPrice());
    }

    // ── approveProperty ───────────────────────────────────────────────────────

    @Test
    @DisplayName("approveProperty sets status to APPROVED and returns DTO")
    void approveProperty_SetsApproved_AndReturnsDTO() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponseDTO result = propertyService.approveProperty(1L);

        assertEquals(ModerationStatus.APPROVED, result.getModerationStatus());
        verify(propertyRepository).save(property);
    }

    @Test
    @DisplayName("approveProperty throws RuntimeException when property not found")
    void approveProperty_ThrowsException_WhenNotFound() {
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> propertyService.approveProperty(99L));

        assertTrue(ex.getMessage().contains("Property not found with id: 99"));
    }

    // ── rejectProperty ────────────────────────────────────────────────────────

    @Test
    @DisplayName("rejectProperty sets status to REJECTED and returns DTO")
    void rejectProperty_SetsRejected_AndReturnsDTO() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponseDTO result = propertyService.rejectProperty(1L);

        assertEquals(ModerationStatus.REJECTED, result.getModerationStatus());
        verify(propertyRepository).save(property);
    }

    @Test
    @DisplayName("rejectProperty throws RuntimeException when property not found")
    void rejectProperty_ThrowsException_WhenNotFound() {
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> propertyService.rejectProperty(99L));

        assertTrue(ex.getMessage().contains("Property not found with id: 99"));
    }

    // ── findPendingProperties ─────────────────────────────────────────────────

    @Test
    @DisplayName("findPendingProperties returns only PENDING properties")
    void findPendingProperties_ReturnsPendingProperties() {
        User user = buildUser(1L, "john");
        Property pending = buildProperty(1L, user);
        pending.setModerationStatus(ModerationStatus.PENDING);
        when(propertyRepository.findByModerationStatus(ModerationStatus.PENDING))
                .thenReturn(List.of(pending));

        List<PropertyResponseDTO> result = propertyService.findPendingProperties();

        assertEquals(1, result.size());
        assertEquals(ModerationStatus.PENDING, result.get(0).getModerationStatus());
    }

    @Test
    @DisplayName("findPendingProperties returns empty list when no pending properties")
    void findPendingProperties_ReturnsEmpty_WhenNoPending() {
        when(propertyRepository.findByModerationStatus(ModerationStatus.PENDING)).thenReturn(List.of());

        List<PropertyResponseDTO> result = propertyService.findPendingProperties();

        assertTrue(result.isEmpty());
    }

    // ── toggleFeatured ────────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleFeatured flips featured from false to true")
    void toggleFeatured_FlipsFalseToTrue() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        property.setFeatured(false);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponseDTO result = propertyService.toggleFeatured(1L);

        assertTrue(result.isFeatured());
        verify(propertyRepository).save(property);
    }

    @Test
    @DisplayName("toggleFeatured flips featured from true to false")
    void toggleFeatured_FlipsTrueToFalse() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        property.setFeatured(true);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponseDTO result = propertyService.toggleFeatured(1L);

        assertFalse(result.isFeatured());
    }

    @Test
    @DisplayName("toggleFeatured throws RuntimeException when property not found")
    void toggleFeatured_ThrowsException_WhenNotFound() {
        when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> propertyService.toggleFeatured(99L));

        assertTrue(ex.getMessage().contains("Property not found with id: 99"));
    }

    // ── parsePrice (via saveProperty) ─────────────────────────────────────────

    @Test
    @DisplayName("parsePrice returns null when price is null")
    void parsePrice_ReturnsNull_WhenPriceIsNull() {
        User user = buildUser(1L, "john");
        Property saved = buildProperty(1L, user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenReturn(saved);

        PropertyRequestDTO dto = buildRequestDTO();
        dto.setPrice(null);

        assertDoesNotThrow(() -> propertyService.saveProperty(dto, 1L, null));
    }

    @Test
    @DisplayName("parsePrice returns null when price is blank")
    void parsePrice_ReturnsNull_WhenPriceIsBlank() {
        User user = buildUser(1L, "john");
        Property saved = buildProperty(1L, user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenReturn(saved);

        PropertyRequestDTO dto = buildRequestDTO();
        dto.setPrice("   ");

        assertDoesNotThrow(() -> propertyService.saveProperty(dto, 1L, null));
    }

    // ── PropertyResponseDTOBuilder — null images branch ───────────────────────

    @Test
    @DisplayName("findById maps zero images when property has null images list")
    void findById_HandlesNullImages() {
        User user = buildUser(1L, "john");
        Property property = buildProperty(1L, user);
        property.setImages(null);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        PropertyResponseDTO result = propertyService.findById(1L);

        assertNotNull(result.getImages());
        assertEquals(0, result.getImages().length);
    }

    // ── saveProperty — empty files array (length > 0 but all empty) ──────────

    @Test
    @DisplayName("saveProperty with empty files array (length 0) does not upload")
    void saveProperty_EmptyFilesArray_DoesNotUpload() {
        User user = buildUser(1L, "john");
        Property saved = buildProperty(1L, user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenReturn(saved);

        propertyService.saveProperty(buildRequestDTO(), 1L,
                new org.springframework.web.multipart.MultipartFile[0]);

        verifyNoInteractions(cloudinaryService);
    }

    // ── updateProperty — empty new-images array ───────────────────────────────

    @Test
    @DisplayName("updateProperty with empty newImages array does not upload")
    void updateProperty_EmptyNewImagesArray_DoesNotUpload() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        propertyService.updateProperty(1L, buildRequestDTO(),
                new org.springframework.web.multipart.MultipartFile[0], null, false);

        verifyNoInteractions(cloudinaryService);
    }

    @Test
    @DisplayName("updateProperty skips empty file in newImages array")
    void updateProperty_SkipsEmptyFileInNewImages() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        propertyService.updateProperty(1L, buildRequestDTO(),
                new org.springframework.web.multipart.MultipartFile[]{emptyFile}, null, false);

        verifyNoInteractions(cloudinaryService);
        verify(imageRepository, never()).save(any());
    }

    // ── findAllForAdmin — remaining sort cases ────────────────────────────────

    @Test
    @DisplayName("findAllForAdmin sorts by id ascending")
    void findAllForAdmin_SortsByIdAsc() {
        User user = buildUser(1L, "john");
        Property p5 = buildProperty(5L, user);
        Property p1 = buildProperty(1L, user);
        when(propertyRepository.findAll()).thenReturn(List.of(p5, p1));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "id", "asc");

        assertEquals(1L, result.get(0).getId());
        assertEquals(5L, result.get(1).getId());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by owner ascending")
    void findAllForAdmin_SortsByOwnerAsc() {
        User john = buildUser(1L, "john");
        User alice = buildUser(2L, "alice");
        Property pJ = buildProperty(1L, john);
        Property pA = buildProperty(2L, alice);
        when(propertyRepository.findAll()).thenReturn(List.of(pJ, pA));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "owner", "asc");

        assertEquals("alice", result.get(0).getUsername());
        assertEquals("john",  result.get(1).getUsername());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by location ascending")
    void findAllForAdmin_SortsByLocationAsc() {
        User user = buildUser(1L, "john");
        Property pZ = buildProperty(1L, user); pZ.setLocation("Ventspils");
        Property pA = buildProperty(2L, user); pA.setLocation("Riga");
        when(propertyRepository.findAll()).thenReturn(List.of(pZ, pA));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "location", "asc");

        assertEquals("Riga",      result.get(0).getLocation());
        assertEquals("Ventspils", result.get(1).getLocation());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by status ascending")
    void findAllForAdmin_SortsByStatusAsc() {
        User user = buildUser(1L, "john");
        Property pSold = buildProperty(1L, user); pSold.setStatus("sold");
        Property pAvail = buildProperty(2L, user); pAvail.setStatus("available");
        when(propertyRepository.findAll()).thenReturn(List.of(pSold, pAvail));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "status", "asc");

        assertEquals("available", result.get(0).getStatus());
        assertEquals("sold",      result.get(1).getStatus());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by createdAt ascending")
    void findAllForAdmin_SortsByCreatedAtAsc() {
        User user = buildUser(1L, "john");
        Property pOld = buildProperty(1L, user); pOld.setCreatedAt(LocalDateTime.now().minusDays(5));
        Property pNew = buildProperty(2L, user); pNew.setCreatedAt(LocalDateTime.now());
        when(propertyRepository.findAll()).thenReturn(List.of(pNew, pOld));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "createdAt", "asc");

        assertTrue(result.get(0).getCreatedAt().isBefore(result.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("findAllForAdmin sorts by featured descending (featured first)")
    void findAllForAdmin_SortsByFeaturedDesc() {
        User user = buildUser(1L, "john");
        Property pNotFeatured = buildProperty(1L, user); pNotFeatured.setFeatured(false);
        Property pFeatured    = buildProperty(2L, user); pFeatured.setFeatured(true);
        when(propertyRepository.findAll()).thenReturn(List.of(pNotFeatured, pFeatured));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "featured", "asc");

        assertTrue(result.get(0).isFeatured());
        assertFalse(result.get(1).isFeatured());
    }

    @Test
    @DisplayName("findAllForAdmin with unknown sort field keeps original order")
    void findAllForAdmin_UnknownSortField_KeepsOrder() {
        User user = buildUser(1L, "john");
        Property p1 = buildProperty(1L, user); p1.setTitle("First");
        Property p2 = buildProperty(2L, user); p2.setTitle("Second");
        when(propertyRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "unknown", "asc");

        assertEquals("First",  result.get(0).getTitle());
        assertEquals("Second", result.get(1).getTitle());
    }

    @Test
    @DisplayName("findAllForAdmin with blank moderationFilter returns all properties")
    void findAllForAdmin_ReturnsAll_WhenFilterIsBlank() {
        User user = buildUser(1L, "john");
        when(propertyRepository.findAll()).thenReturn(List.of(buildProperty(1L, user)));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin("   ");

        assertEquals(1, result.size());
        verify(propertyRepository).findAll();
    }

    @Test
    @DisplayName("findAllForAdmin default sort puts REJECTED last")
    void findAllForAdmin_DefaultSort_RejectedLast() {
        User user = buildUser(1L, "john");
        Property rejected = buildProperty(1L, user); rejected.setModerationStatus(ModerationStatus.REJECTED);
        Property pending  = buildProperty(2L, user); pending.setModerationStatus(ModerationStatus.PENDING);
        Property approved = buildProperty(3L, user); approved.setModerationStatus(ModerationStatus.APPROVED);
        when(propertyRepository.findAll()).thenReturn(List.of(rejected, approved, pending));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, null, null);

        assertEquals(ModerationStatus.PENDING,  result.get(0).getModerationStatus());
        assertEquals(ModerationStatus.APPROVED, result.get(1).getModerationStatus());
        assertEquals(ModerationStatus.REJECTED, result.get(2).getModerationStatus());
    }

    // ── getPropertiesByUserId — remaining sort cases ──────────────────────────

    @Test
    @DisplayName("getPropertiesByUserId sorts by status ascending")
    void getPropertiesByUserId_SortsByStatusAsc() {
        User user = buildUser(1L, "john");
        Property pSold  = buildProperty(1L, user); pSold.setStatus("sold");
        Property pAvail = buildProperty(2L, user); pAvail.setStatus("available");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pSold, pAvail));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "status", "asc");

        assertEquals("available", result.get(0).getStatus());
        assertEquals("sold",      result.get(1).getStatus());
    }

    @Test
    @DisplayName("getPropertiesByUserId sorts by createdAt ascending")
    void getPropertiesByUserId_SortsByCreatedAtAsc() {
        User user = buildUser(1L, "john");
        Property pOld = buildProperty(1L, user); pOld.setCreatedAt(LocalDateTime.now().minusDays(3));
        Property pNew = buildProperty(2L, user); pNew.setCreatedAt(LocalDateTime.now());
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pNew, pOld));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "createdAt", "asc");

        assertTrue(result.get(0).getCreatedAt().isBefore(result.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("getPropertiesByUserId with unknown sort field keeps original order")
    void getPropertiesByUserId_UnknownSortField_KeepsOrder() {
        User user = buildUser(1L, "john");
        Property p1 = buildProperty(1L, user); p1.setTitle("Alpha");
        Property p2 = buildProperty(2L, user); p2.setTitle("Beta");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(p1, p2));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "unknown", "asc");

        assertEquals("Alpha", result.get(0).getTitle());
        assertEquals("Beta",  result.get(1).getTitle());
    }

    @Test
    @DisplayName("getPropertiesByUserId with blank moderationFilter returns all properties")
    void getPropertiesByUserId_ReturnsAll_WhenModerationFilterIsBlank() {
        User user = buildUser(1L, "john");
        when(propertyRepository.findByUserId(1L))
                .thenReturn(List.of(buildProperty(1L, user), buildProperty(2L, user)));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, null, null, "   ");

        assertEquals(2, result.size());
    }

    // ── findAllForAdmin — APPROVED filter ─────────────────────────────────────

    @Test
    @DisplayName("findAllForAdmin with APPROVED filter returns only approved properties")
    void findAllForAdmin_ReturnsFiltered_ByApprovedStatus() {
        User user = buildUser(1L, "john");
        Property approved = buildProperty(1L, user);
        approved.setModerationStatus(ModerationStatus.APPROVED);
        when(propertyRepository.findByModerationStatus(ModerationStatus.APPROVED))
                .thenReturn(List.of(approved));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin("APPROVED");

        assertEquals(1, result.size());
        assertEquals(ModerationStatus.APPROVED, result.get(0).getModerationStatus());
    }

    // ── findAllForAdmin ── findFeatured ──────────────────────────────────────

    @Test
    @DisplayName("findFeatured returns only featured properties")
    void findFeatured_ReturnsFeaturedProperties() {
        User user = buildUser(1L, "john");
        Property featured = buildProperty(1L, user);
        featured.setFeatured(true);
        when(propertyRepository.findByFeaturedTrue()).thenReturn(List.of(featured));

        List<PropertyResponseDTO> result = propertyService.findFeatured();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isFeatured());
    }

    @Test
    @DisplayName("findFeatured returns empty list when no featured properties")
    void findFeatured_ReturnsEmpty_WhenNoneFeatured() {
        when(propertyRepository.findByFeaturedTrue()).thenReturn(List.of());

        List<PropertyResponseDTO> result = propertyService.findFeatured();

        assertTrue(result.isEmpty());
    }

    // ── updateProperty — empty (non-null) imageUrlsToDelete list ─────────────

    @Test
    @DisplayName("updateProperty with non-null but empty imageUrlsToDelete skips deletion block")
    void updateProperty_SkipsDeletionBlock_WhenImageUrlsToDeleteIsEmpty() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Pass an empty (not null) list — covers the !imageUrlsToDelete.isEmpty() == false branch
        propertyService.updateProperty(1L, buildRequestDTO(), null, List.of(), false);

        verifyNoInteractions(cloudinaryService);
        verify(imageRepository, never()).delete(any());
    }

    // ── findAllForAdmin — blank (non-null) sortField keeps default sort ───────

    @Test
    @DisplayName("findAllForAdmin with blank sortField uses default sort")
    void findAllForAdmin_BlankSortField_UsesDefaultSort() {
        User user = buildUser(1L, "john");
        Property approved = buildProperty(1L, user); approved.setModerationStatus(ModerationStatus.APPROVED);
        Property pending  = buildProperty(2L, user); pending.setModerationStatus(ModerationStatus.PENDING);
        when(propertyRepository.findAll()).thenReturn(List.of(approved, pending));

        // "   " is non-null but blank — covers the !sortField.isBlank() == false branch
        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "   ", "asc");

        assertEquals(ModerationStatus.PENDING,  result.get(0).getModerationStatus());
        assertEquals(ModerationStatus.APPROVED, result.get(1).getModerationStatus());
    }

    // ── getPropertiesByUserId — blank (non-null) sortField keeps order ────────

    @Test
    @DisplayName("getPropertiesByUserId with blank sortField keeps original order")
    void getPropertiesByUserId_BlankSortField_KeepsOrder() {
        User user = buildUser(1L, "john");
        Property p1 = buildProperty(1L, user); p1.setTitle("Alpha");
        Property p2 = buildProperty(2L, user); p2.setTitle("Beta");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(p1, p2));

        // "   " is non-null but blank — covers the !sortField.isBlank() == false branch
        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "   ", "asc");

        assertEquals("Alpha", result.get(0).getTitle());
        assertEquals("Beta",  result.get(1).getTitle());
    }

    // ── findAllForAdmin — null-fallback branches in sort comparators ──────────

    @Test
    @DisplayName("findAllForAdmin sort by id falls back to 0 when id is null")
    void findAllForAdmin_SortById_NullIdFallsBackToZero() {
        User user = buildUser(1L, "john");
        Property pWithId    = buildProperty(5L, user);
        Property pNullId    = buildProperty(1L, user); pNullId.setId(null);
        when(propertyRepository.findAll()).thenReturn(List.of(pWithId, pNullId));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "id", "asc");

        // null id treated as 0, so null-id property should come first
        assertNull(result.get(0).getId());
        assertEquals(5L, result.get(1).getId());
    }

    @Test
    @DisplayName("findAllForAdmin sort by title falls back to empty string when title is null")
    void findAllForAdmin_SortByTitle_NullTitleFallsBackToEmpty() {
        User user = buildUser(1L, "john");
        Property pNullTitle = buildProperty(1L, user); pNullTitle.setTitle(null);
        Property pWithTitle = buildProperty(2L, user); pWithTitle.setTitle("Z house");
        when(propertyRepository.findAll()).thenReturn(List.of(pWithTitle, pNullTitle));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "title", "asc");

        assertNull(result.get(0).getTitle());
        assertEquals("Z house", result.get(1).getTitle());
    }

    @Test
    @DisplayName("findAllForAdmin sort by owner falls back to empty string when username is null")
    void findAllForAdmin_SortByOwner_NullUsernameFallsBackToEmpty() {
        User userNull = buildUser(1L, null);
        User userJohn = buildUser(2L, "john");
        Property pNull = buildProperty(1L, userNull);
        Property pJohn = buildProperty(2L, userJohn);
        when(propertyRepository.findAll()).thenReturn(List.of(pJohn, pNull));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "owner", "asc");

        assertNull(result.get(0).getUsername());
        assertEquals("john", result.get(1).getUsername());
    }

    @Test
    @DisplayName("findAllForAdmin sort by price falls back to 0.0 when price is null")
    void findAllForAdmin_SortByPrice_NullPriceFallsBackToZero() {
        User user = buildUser(1L, "john");
        Property pNullPrice = buildProperty(1L, user); pNullPrice.setPrice(null);
        Property pWithPrice = buildProperty(2L, user); pWithPrice.setPrice(500.0);
        when(propertyRepository.findAll()).thenReturn(List.of(pWithPrice, pNullPrice));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "price", "asc");

        assertNull(result.get(0).getPrice());
        assertEquals(500.0, result.get(1).getPrice());
    }

    @Test
    @DisplayName("findAllForAdmin sort by location falls back to empty string when location is null")
    void findAllForAdmin_SortByLocation_NullLocationFallsBackToEmpty() {
        User user = buildUser(1L, "john");
        Property pNullLoc = buildProperty(1L, user); pNullLoc.setLocation(null);
        Property pWithLoc = buildProperty(2L, user); pWithLoc.setLocation("Riga");
        when(propertyRepository.findAll()).thenReturn(List.of(pWithLoc, pNullLoc));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "location", "asc");

        assertNull(result.get(0).getLocation());
        assertEquals("Riga", result.get(1).getLocation());
    }

    @Test
    @DisplayName("findAllForAdmin sort by status falls back to empty string when status is null")
    void findAllForAdmin_SortByStatus_NullStatusFallsBackToEmpty() {
        User user = buildUser(1L, "john");
        Property pNullStatus = buildProperty(1L, user); pNullStatus.setStatus(null);
        Property pWithStatus = buildProperty(2L, user); pWithStatus.setStatus("available");
        when(propertyRepository.findAll()).thenReturn(List.of(pWithStatus, pNullStatus));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "status", "asc");

        assertNull(result.get(0).getStatus());
        assertEquals("available", result.get(1).getStatus());
    }

    // ── getPropertiesByUserId — null-fallback branches in sort comparators ────

    @Test
    @DisplayName("getPropertiesByUserId sort by title falls back to empty string when title is null")
    void getPropertiesByUserId_SortByTitle_NullTitleFallsBackToEmpty() {
        User user = buildUser(1L, "john");
        Property pNullTitle = buildProperty(1L, user); pNullTitle.setTitle(null);
        Property pWithTitle = buildProperty(2L, user); pWithTitle.setTitle("Z house");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pWithTitle, pNullTitle));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "title", "asc");

        assertNull(result.get(0).getTitle());
        assertEquals("Z house", result.get(1).getTitle());
    }

    @Test
    @DisplayName("getPropertiesByUserId sort by price falls back to 0.0 when price is null")
    void getPropertiesByUserId_SortByPrice_NullPriceFallsBackToZero() {
        User user = buildUser(1L, "john");
        Property pNullPrice = buildProperty(1L, user); pNullPrice.setPrice(null);
        Property pWithPrice = buildProperty(2L, user); pWithPrice.setPrice(500.0);
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pWithPrice, pNullPrice));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "price", "asc");

        assertNull(result.get(0).getPrice());
        assertEquals(500.0, result.get(1).getPrice());
    }

    @Test
    @DisplayName("getPropertiesByUserId sort by status falls back to empty string when status is null")
    void getPropertiesByUserId_SortByStatus_NullStatusFallsBackToEmpty() {
        User user = buildUser(1L, "john");
        Property pNullStatus = buildProperty(1L, user); pNullStatus.setStatus(null);
        Property pWithStatus = buildProperty(2L, user); pWithStatus.setStatus("available");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pWithStatus, pNullStatus));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "status", "asc");

        assertNull(result.get(0).getStatus());
        assertEquals("available", result.get(1).getStatus());
    }

    // ── findAllForAdmin — desc sort for every sort field ─────────────────────

    @Test
    @DisplayName("findAllForAdmin sorts by id descending")
    void findAllForAdmin_SortsByIdDesc() {
        User user = buildUser(1L, "john");
        Property p1 = buildProperty(1L, user);
        Property p5 = buildProperty(5L, user);
        when(propertyRepository.findAll()).thenReturn(List.of(p1, p5));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "id", "desc");

        assertEquals(5L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by title descending")
    void findAllForAdmin_SortsByTitleDesc() {
        User user = buildUser(1L, "john");
        Property pA = buildProperty(1L, user); pA.setTitle("A house");
        Property pZ = buildProperty(2L, user); pZ.setTitle("Z house");
        when(propertyRepository.findAll()).thenReturn(List.of(pA, pZ));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "title", "desc");

        assertEquals("Z house", result.get(0).getTitle());
        assertEquals("A house", result.get(1).getTitle());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by owner descending")
    void findAllForAdmin_SortsByOwnerDesc() {
        User alice = buildUser(1L, "alice");
        User john  = buildUser(2L, "john");
        Property pA = buildProperty(1L, alice);
        Property pJ = buildProperty(2L, john);
        when(propertyRepository.findAll()).thenReturn(List.of(pA, pJ));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "owner", "desc");

        assertEquals("john",  result.get(0).getUsername());
        assertEquals("alice", result.get(1).getUsername());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by price ascending")
    void findAllForAdmin_SortsByPriceAsc() {
        User user = buildUser(1L, "john");
        Property cheap     = buildProperty(1L, user); cheap.setPrice(50000.0);
        Property expensive = buildProperty(2L, user); expensive.setPrice(200000.0);
        when(propertyRepository.findAll()).thenReturn(List.of(expensive, cheap));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "price", "asc");

        assertEquals(50000.0,  result.get(0).getPrice());
        assertEquals(200000.0, result.get(1).getPrice());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by location descending")
    void findAllForAdmin_SortsByLocationDesc() {
        User user = buildUser(1L, "john");
        Property pR = buildProperty(1L, user); pR.setLocation("Riga");
        Property pV = buildProperty(2L, user); pV.setLocation("Ventspils");
        when(propertyRepository.findAll()).thenReturn(List.of(pR, pV));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "location", "desc");

        assertEquals("Ventspils", result.get(0).getLocation());
        assertEquals("Riga",      result.get(1).getLocation());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by status descending")
    void findAllForAdmin_SortsByStatusDesc() {
        User user = buildUser(1L, "john");
        Property pAvail = buildProperty(1L, user); pAvail.setStatus("available");
        Property pSold  = buildProperty(2L, user); pSold.setStatus("sold");
        when(propertyRepository.findAll()).thenReturn(List.of(pAvail, pSold));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "status", "desc");

        assertEquals("sold",      result.get(0).getStatus());
        assertEquals("available", result.get(1).getStatus());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by createdAt descending")
    void findAllForAdmin_SortsByCreatedAtDesc() {
        User user = buildUser(1L, "john");
        Property pOld = buildProperty(1L, user); pOld.setCreatedAt(LocalDateTime.now().minusDays(5));
        Property pNew = buildProperty(2L, user); pNew.setCreatedAt(LocalDateTime.now());
        when(propertyRepository.findAll()).thenReturn(List.of(pOld, pNew));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "createdAt", "desc");

        assertTrue(result.get(0).getCreatedAt().isAfter(result.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("findAllForAdmin sorts by createdAt with null value — nullsLast branch")
    void findAllForAdmin_SortsByCreatedAt_NullsLast() {
        User user = buildUser(1L, "john");
        Property pNull = buildProperty(1L, user); pNull.setCreatedAt(null);
        Property pWith = buildProperty(2L, user); pWith.setCreatedAt(LocalDateTime.now());
        when(propertyRepository.findAll()).thenReturn(List.of(pNull, pWith));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "createdAt", "asc");

        // nullsLast: the non-null item should come first
        assertNotNull(result.get(0).getCreatedAt());
        assertNull(result.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("findAllForAdmin sorts by featured ascending (non-featured first)")
    void findAllForAdmin_SortsByFeaturedAsc() {
        User user = buildUser(1L, "john");
        Property pFeatured    = buildProperty(1L, user); pFeatured.setFeatured(true);
        Property pNotFeatured = buildProperty(2L, user); pNotFeatured.setFeatured(false);
        when(propertyRepository.findAll()).thenReturn(List.of(pFeatured, pNotFeatured));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, "featured", "desc");

        assertFalse(result.get(0).isFeatured());
        assertTrue(result.get(1).isFeatured());
    }

    // ── getPropertiesByUserId — desc sort for every sort field ────────────────

    @Test
    @DisplayName("getPropertiesByUserId sorts by title descending")
    void getPropertiesByUserId_SortsByTitleDesc() {
        User user = buildUser(1L, "john");
        Property pA = buildProperty(1L, user); pA.setTitle("A house");
        Property pZ = buildProperty(2L, user); pZ.setTitle("Z house");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pA, pZ));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "title", "desc");

        assertEquals("Z house", result.get(0).getTitle());
        assertEquals("A house", result.get(1).getTitle());
    }

    @Test
    @DisplayName("getPropertiesByUserId sorts by price ascending")
    void getPropertiesByUserId_SortsByPriceAsc() {
        User user = buildUser(1L, "john");
        Property cheap     = buildProperty(1L, user); cheap.setPrice(30000.0);
        Property expensive = buildProperty(2L, user); expensive.setPrice(500000.0);
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(expensive, cheap));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "price", "asc");

        assertEquals(30000.0,  result.get(0).getPrice());
        assertEquals(500000.0, result.get(1).getPrice());
    }

    @Test
    @DisplayName("getPropertiesByUserId sorts by status descending")
    void getPropertiesByUserId_SortsByStatusDesc() {
        User user = buildUser(1L, "john");
        Property pAvail = buildProperty(1L, user); pAvail.setStatus("available");
        Property pSold  = buildProperty(2L, user); pSold.setStatus("sold");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pAvail, pSold));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "status", "desc");

        assertEquals("sold",      result.get(0).getStatus());
        assertEquals("available", result.get(1).getStatus());
    }

    @Test
    @DisplayName("getPropertiesByUserId sorts by createdAt descending")
    void getPropertiesByUserId_SortsByCreatedAtDesc() {
        User user = buildUser(1L, "john");
        Property pOld = buildProperty(1L, user); pOld.setCreatedAt(LocalDateTime.now().minusDays(3));
        Property pNew = buildProperty(2L, user); pNew.setCreatedAt(LocalDateTime.now());
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pOld, pNew));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "createdAt", "desc");

        assertTrue(result.get(0).getCreatedAt().isAfter(result.get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("getPropertiesByUserId sorts by createdAt with null value — nullsLast branch")
    void getPropertiesByUserId_SortsByCreatedAt_NullsLast() {
        User user = buildUser(1L, "john");
        Property pNull = buildProperty(1L, user); pNull.setCreatedAt(null);
        Property pWith = buildProperty(2L, user); pWith.setCreatedAt(LocalDateTime.now());
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pNull, pWith));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "createdAt", "asc");

        assertNotNull(result.get(0).getCreatedAt());
        assertNull(result.get(1).getCreatedAt());
    }

    // ── getPropertiesByUserId — null-fallback for createdAt ──────────────────

    @Test
    @DisplayName("getPropertiesByUserId sort by createdAt falls back to nullsLast when value is null")
    void getPropertiesByUserId_SortByCreatedAt_NullFallsBackToLast() {
        User user = buildUser(1L, "john");
        Property pNull = buildProperty(1L, user); pNull.setCreatedAt(null);
        Property pWith = buildProperty(2L, user); pWith.setCreatedAt(LocalDateTime.now().minusDays(1));
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pNull, pWith));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "createdAt", "asc");

        assertNotNull(result.get(0).getCreatedAt());
        assertNull(result.get(1).getCreatedAt());
    }

    // ── getPropertiesByUserId — 2-arg overload delegates correctly ────────────

    @Test
    @DisplayName("getPropertiesByUserId 2-arg overload (sortField+sortDir) delegates to 4-arg")
    void getPropertiesByUserId_TwoArgOverload_Delegates() {
        User user = buildUser(1L, "john");
        Property pZ = buildProperty(1L, user); pZ.setTitle("Z house");
        Property pA = buildProperty(2L, user); pA.setTitle("A house");
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(pZ, pA));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, "title", "asc");

        assertEquals("A house", result.get(0).getTitle());
        assertEquals("Z house", result.get(1).getTitle());
    }

    // ── findAllForAdmin — 1-arg overload delegates correctly ─────────────────

    @Test
    @DisplayName("findAllForAdmin 1-arg overload delegates to 3-arg (default sort applied)")
    void findAllForAdmin_OneArgOverload_Delegates() {
        User user = buildUser(1L, "john");
        Property approved = buildProperty(1L, user); approved.setModerationStatus(ModerationStatus.APPROVED);
        Property pending  = buildProperty(2L, user); pending.setModerationStatus(ModerationStatus.PENDING);
        when(propertyRepository.findAll()).thenReturn(List.of(approved, pending));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null);

        // Default sort: PENDING first
        assertEquals(ModerationStatus.PENDING,  result.get(0).getModerationStatus());
        assertEquals(ModerationStatus.APPROVED, result.get(1).getModerationStatus());
    }

    // ── findAllForAdmin — REJECTED filter ────────────────────────────────────

    @Test
    @DisplayName("findAllForAdmin filters by REJECTED moderation status")
    void findAllForAdmin_FiltersRejected() {
        User user = buildUser(1L, "john");
        Property rejected = buildProperty(1L, user); rejected.setModerationStatus(ModerationStatus.REJECTED);
        when(propertyRepository.findByModerationStatus(ModerationStatus.REJECTED))
                .thenReturn(List.of(rejected));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin("REJECTED");

        assertEquals(1, result.size());
        assertEquals(ModerationStatus.REJECTED, result.get(0).getModerationStatus());
    }

    @Test
    @DisplayName("findAllForAdmin with invalid filter falls back to all properties")
    void findAllForAdmin_InvalidFilter_ReturnsAll() {
        User user = buildUser(1L, "john");
        Property p1 = buildProperty(1L, user);
        Property p2 = buildProperty(2L, user);
        when(propertyRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin("INVALID_STATUS");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findAllForAdmin default sort orders PENDING first, then APPROVED, then REJECTED")
    void findAllForAdmin_DefaultSort_ThreeStatuses() {
        User user = buildUser(1L, "john");
        Property approved = buildProperty(1L, user); approved.setModerationStatus(ModerationStatus.APPROVED);
        approved.setCreatedAt(LocalDateTime.now().minusDays(1));
        Property pending  = buildProperty(2L, user); pending.setModerationStatus(ModerationStatus.PENDING);
        pending.setCreatedAt(LocalDateTime.now());
        Property rejected = buildProperty(3L, user); rejected.setModerationStatus(ModerationStatus.REJECTED);
        rejected.setCreatedAt(LocalDateTime.now().minusDays(2));
        when(propertyRepository.findAll()).thenReturn(List.of(approved, rejected, pending));

        List<PropertyResponseDTO> result = propertyService.findAllForAdmin(null, null, null);

        assertEquals(ModerationStatus.PENDING,  result.get(0).getModerationStatus());
        assertEquals(ModerationStatus.APPROVED, result.get(1).getModerationStatus());
        assertEquals(ModerationStatus.REJECTED, result.get(2).getModerationStatus());
    }

    // ── parsePrice — null input ───────────────────────────────────────────────

    @Test
    @DisplayName("saveProperty with null price results in null price on DTO")
    void saveProperty_NullPrice_ReturnsNullPrice() {
        User user = buildUser(1L, "john");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            p.setId(99L);
            p.setImages(new ArrayList<>());
            return p;
        });

        PropertyRequestDTO dto = buildRequestDTO();
        dto.setPrice(null); // null price → parsePrice returns null

        PropertyResponseDTO result = propertyService.saveProperty(dto, 1L, null);

        assertNull(result.getPrice());
    }

    @Test
    @DisplayName("saveProperty with blank price results in null price on DTO")
    void saveProperty_BlankPrice_ReturnsNullPrice() {
        User user = buildUser(1L, "john");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(propertyRepository.save(any())).thenAnswer(inv -> {
            Property p = inv.getArgument(0);
            p.setId(99L);
            p.setImages(new ArrayList<>());
            return p;
        });

        PropertyRequestDTO dto = buildRequestDTO();
        dto.setPrice("   "); // blank price → parsePrice returns null

        PropertyResponseDTO result = propertyService.saveProperty(dto, 1L, null);

        assertNull(result.getPrice());
    }

    // ── updateProperty — isAdmin=true branch ─────────────────────────────────

    @Test
    @DisplayName("updateProperty with isAdmin=true does NOT reset moderation status to PENDING")
    void updateProperty_IsAdmin_DoesNotResetModerationStatus() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        existing.setModerationStatus(ModerationStatus.APPROVED);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PropertyRequestDTO dto = buildRequestDTO();
        PropertyResponseDTO result = propertyService.updateProperty(1L, dto, null, null, true);

        // With isAdmin=true the status should remain APPROVED (not reset to PENDING)
        assertEquals(ModerationStatus.APPROVED, result.getModerationStatus());
    }

    // ── updateProperty — imageUrlsToDelete URL not found ─────────────────────

    @Test
    @DisplayName("updateProperty skips deletion when URL not found in image repo")
    void updateProperty_SkipsDeletion_WhenUrlNotFound() {
        User user = buildUser(1L, "john");
        Property existing = buildProperty(1L, user);
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(propertyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(imageRepository.findByUrl("http://missing.jpg")).thenReturn(Optional.empty());

        PropertyRequestDTO dto = buildRequestDTO();
        // Should not throw — just silently skip the missing URL
        PropertyResponseDTO result = propertyService.updateProperty(
                1L, dto, null, List.of("http://missing.jpg"), false);

        assertNotNull(result);
        verify(imageRepository, never()).delete(any());
    }

    // ── getPropertiesByUserId (4-arg) — valid filter removes non-matching ─────

    @Test
    @DisplayName("getPropertiesByUserId 4-arg filters by APPROVED and removes PENDING")
    void getPropertiesByUserId_FourArg_FiltersByApproved() {
        User user = buildUser(1L, "john");
        Property approved = buildProperty(1L, user); approved.setModerationStatus(ModerationStatus.APPROVED);
        Property pending  = buildProperty(2L, user); pending.setModerationStatus(ModerationStatus.PENDING);
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(approved, pending));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, null, null, "APPROVED");

        assertEquals(1, result.size());
        assertEquals(ModerationStatus.APPROVED, result.get(0).getModerationStatus());
    }

    @Test
    @DisplayName("getPropertiesByUserId 4-arg with invalid moderationFilter returns all")
    void getPropertiesByUserId_FourArg_InvalidFilter_ReturnsAll() {
        User user = buildUser(1L, "john");
        Property p1 = buildProperty(1L, user);
        Property p2 = buildProperty(2L, user);
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(p1, p2));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L, null, null, "GARBAGE");

        assertEquals(2, result.size());
    }

    // ── getPropertiesByUserId (1-arg) explicit call ───────────────────────────

    @Test
    @DisplayName("getPropertiesByUserId 1-arg overload (no sort/filter) delegates to 4-arg")
    void getPropertiesByUserId_OneArgOverload_Delegates() {
        User user = buildUser(1L, "john");
        Property p = buildProperty(1L, user);
        when(propertyRepository.findByUserId(1L)).thenReturn(List.of(p));

        List<PropertyResponseDTO> result = propertyService.getPropertiesByUserId(1L);

        assertEquals(1, result.size());
    }
}


