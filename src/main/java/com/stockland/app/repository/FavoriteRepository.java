package com.stockland.app.repository;

import com.stockland.app.model.Favorite;
import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUser(User user);

    Optional<Favorite> findByUserAndProperty(User user, Property property);

    boolean existsByUserAndProperty(User user, Property property);
}
