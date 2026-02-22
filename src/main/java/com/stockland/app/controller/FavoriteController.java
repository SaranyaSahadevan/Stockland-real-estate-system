package com.stockland.app.controller;

import com.stockland.app.model.Property;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import com.stockland.app.service.FavoriteService;
import com.stockland.app.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final PropertyService propertyService;
    private final UserRepository userRepository;

    @Autowired
    public FavoriteController(FavoriteService favoriteService,
                              PropertyService propertyService,
                              UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.propertyService = propertyService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));
    }

    @GetMapping("/favorites/add/{id}")
    public String addFavorite(@PathVariable Long id) {
        User user = getCurrentUser();
        Property property = propertyService.getPropertyById(id);
        favoriteService.addFavorite(user, property);
        return "redirect:/property/" + id;
    }

    @GetMapping("/favorites/remove/{id}")
    public String removeFavorite(@PathVariable Long id) {
        User user = getCurrentUser();
        Property property = propertyService.getPropertyById(id);
        favoriteService.removeFavorite(user, property);
        return "redirect:/dashboard";
    }
}