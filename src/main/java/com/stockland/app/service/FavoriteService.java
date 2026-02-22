package com.stockland.app.service;

import com.stockland.app.model.Favorite;
import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import com.stockland.app.repository.FavoriteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;

    public FavoriteService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public void addFavorite(User user, Property property) {
        if (!favoriteRepository.existsByUserAndProperty(user, property)) {
            favoriteRepository.save(new Favorite(user, property));
        }
    }

    public void removeFavorite(User user, Property property) {
        favoriteRepository.findByUserAndProperty(user, property)
                .ifPresent(favoriteRepository::delete);
    }

    public List<Favorite> getFavorites(User user) {
        return favoriteRepository.findByUser(user);
    }
}