package com.stockland.app.service;

import com.stockland.app.model.Favorite;
import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import com.stockland.app.repository.FavoriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @InjectMocks
    private FavoriteService favoriteService;

    private User user;
    private Property property;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("john");

        property = new Property();
        property.setId(10L);
        property.setTitle("Nice House");
    }

    // ── addFavorite ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("addFavorite saves new Favorite when not already present")
    void addFavorite_SavesFavorite_WhenNotAlreadyPresent() {
        when(favoriteRepository.existsByUserAndProperty(user, property)).thenReturn(false);

        favoriteService.addFavorite(user, property);

        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("addFavorite saves a Favorite with the correct user and property")
    void addFavorite_SavesFavorite_WithCorrectUserAndProperty() {
        when(favoriteRepository.existsByUserAndProperty(user, property)).thenReturn(false);

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        favoriteService.addFavorite(user, property);

        verify(favoriteRepository).save(captor.capture());
        Favorite saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals(property, saved.getProperty());
    }

    @Test
    @DisplayName("addFavorite always calls existsByUserAndProperty to check duplicates")
    void addFavorite_AlwaysChecksExistence() {
        when(favoriteRepository.existsByUserAndProperty(user, property)).thenReturn(false);

        favoriteService.addFavorite(user, property);

        verify(favoriteRepository, times(1)).existsByUserAndProperty(user, property);
    }

    @Test
    @DisplayName("addFavorite does not save when Favorite already exists")
    void addFavorite_DoesNotSave_WhenAlreadyPresent() {
        when(favoriteRepository.existsByUserAndProperty(user, property)).thenReturn(true);

        favoriteService.addFavorite(user, property);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("addFavorite still calls existsByUserAndProperty even when favorite already exists")
    void addFavorite_StillChecksExistence_WhenAlreadyPresent() {
        when(favoriteRepository.existsByUserAndProperty(user, property)).thenReturn(true);

        favoriteService.addFavorite(user, property);

        verify(favoriteRepository, times(1)).existsByUserAndProperty(user, property);
    }

    // ── removeFavorite ────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeFavorite deletes existing Favorite")
    void removeFavorite_DeletesFavorite_WhenPresent() {
        Favorite favorite = new Favorite(user, property);
        when(favoriteRepository.findByUserAndProperty(user, property))
                .thenReturn(Optional.of(favorite));

        favoriteService.removeFavorite(user, property);

        verify(favoriteRepository).delete(favorite);
    }

    @Test
    @DisplayName("removeFavorite deletes the exact Favorite returned by the repository")
    void removeFavorite_DeletesExactFavoriteFromRepository() {
        Favorite favorite = new Favorite(user, property);
        favorite.setId(99L);
        when(favoriteRepository.findByUserAndProperty(user, property))
                .thenReturn(Optional.of(favorite));

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.forClass(Favorite.class);
        favoriteService.removeFavorite(user, property);

        verify(favoriteRepository).delete(captor.capture());
        assertEquals(99L, captor.getValue().getId());
    }

    @Test
    @DisplayName("removeFavorite does nothing when Favorite is not present")
    void removeFavorite_DoesNothing_WhenNotPresent() {
        when(favoriteRepository.findByUserAndProperty(user, property))
                .thenReturn(Optional.empty());

        favoriteService.removeFavorite(user, property);

        verify(favoriteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeFavorite always calls findByUserAndProperty")
    void removeFavorite_AlwaysCallsFind() {
        when(favoriteRepository.findByUserAndProperty(user, property))
                .thenReturn(Optional.empty());

        favoriteService.removeFavorite(user, property);

        verify(favoriteRepository, times(1)).findByUserAndProperty(user, property);
    }

    // ── getFavorites ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getFavorites returns list of Favorites for the user")
    void getFavorites_ReturnsFavorites_ForUser() {
        Favorite f1 = new Favorite(user, property);
        when(favoriteRepository.findByUser(user)).thenReturn(List.of(f1));

        List<Favorite> result = favoriteService.getFavorites(user);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(property, result.get(0).getProperty());
        assertEquals(user, result.get(0).getUser());
    }

    @Test
    @DisplayName("getFavorites returns exact list provided by repository")
    void getFavorites_ReturnsExactRepositoryList() {
        Property property2 = new Property();
        property2.setId(20L);
        Favorite f1 = new Favorite(user, property);
        Favorite f2 = new Favorite(user, property2);
        List<Favorite> expected = List.of(f1, f2);
        when(favoriteRepository.findByUser(user)).thenReturn(expected);

        List<Favorite> result = favoriteService.getFavorites(user);

        assertSame(expected, result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getFavorites returns empty list when user has no favorites")
    void getFavorites_ReturnsEmptyList_WhenNoFavorites() {
        when(favoriteRepository.findByUser(user)).thenReturn(List.of());

        List<Favorite> result = favoriteService.getFavorites(user);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getFavorites always delegates to findByUser with the correct user")
    void getFavorites_DelegatesToRepository_WithCorrectUser() {
        when(favoriteRepository.findByUser(user)).thenReturn(List.of());

        favoriteService.getFavorites(user);

        verify(favoriteRepository, times(1)).findByUser(user);
    }
}

