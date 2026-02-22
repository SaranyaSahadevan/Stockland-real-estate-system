package com.stockland.app.controller;

import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import com.stockland.app.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.stockland.app.service.FavoriteService;

import java.util.List;

@Controller
public class DashboardController {
    private final UserRepository userRepository;
    private final PropertyService propertyService;
    private final FavoriteService favoriteService;

    @Autowired
    public DashboardController(UserRepository userRepository,
                               PropertyService propertyService,
                               FavoriteService favoriteService) {
        this.userRepository = userRepository;
        this.propertyService = propertyService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));
        model.addAttribute("user", user);
        List<PropertyResponseDTO> myListings = propertyService.getPropertiesByUserId(user.getId());
        model.addAttribute("myListings", myListings);
        model.addAttribute("favorites", favoriteService.getFavorites(user));
        return "dashboard";
    }
}