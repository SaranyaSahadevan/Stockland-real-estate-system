package com.stockland.app.controller;

import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.Favorite;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import com.stockland.app.service.PropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.stockland.app.service.FavoriteService;

import java.util.Comparator;
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
    public String dashboard(Model model,
                            @RequestParam(value = "moderation",     required = false) String moderationFilter,
                            @RequestParam(value = "sort",           required = false) String sortField,
                            @RequestParam(value = "dir",            required = false) String sortDir,
                            @RequestParam(value = "favSort",        required = false) String favSort,
                            @RequestParam(value = "favDir",         required = false) String favDir,
                            @RequestParam(value = "listSort",       required = false) String listSort,
                            @RequestParam(value = "listDir",        required = false) String listDir,
                            @RequestParam(value = "listModeration", required = false) String listModeration) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(username).orElse(null));
        model.addAttribute("user", user);

        // My Listings with sorting and moderation filter
        List<PropertyResponseDTO> myListings = propertyService.getPropertiesByUserId(user.getId(), listSort, listDir, listModeration);
        model.addAttribute("myListings", myListings);
        model.addAttribute("listSort",       listSort       != null ? listSort.toLowerCase()       : "");
        model.addAttribute("listDir",        listDir        != null ? listDir.toLowerCase()        : "");
        model.addAttribute("listModeration", listModeration != null ? listModeration.toUpperCase() : "");

        // Favorites with sorting
        List<Favorite> favorites = favoriteService.getFavorites(user);
        String favSortNorm = favSort != null ? favSort.toLowerCase() : "";
        String favDirNorm  = favDir  != null ? favDir.toLowerCase()  : "";
        boolean favDesc = "desc".equalsIgnoreCase(favDir);
        if (!favSortNorm.isBlank()) {
            Comparator<Favorite> cmp = switch (favSortNorm) {
                case "title"    -> Comparator.comparing(f -> f.getProperty().getTitle() != null ? f.getProperty().getTitle().toLowerCase() : "");
                case "location" -> Comparator.comparing(f -> f.getProperty().getLocation() != null ? f.getProperty().getLocation().toLowerCase() : "");
                case "price"    -> Comparator.comparingDouble(f -> f.getProperty().getPrice() != null ? f.getProperty().getPrice() : 0.0);
                default         -> null;
            };
            if (cmp != null) {
                if (favDesc) cmp = cmp.reversed();
                favorites.sort(cmp);
            }
        }
        model.addAttribute("favorites", favorites);
        model.addAttribute("favSort", favSortNorm);
        model.addAttribute("favDir",  favDirNorm);

        if ("ROLE_ADMIN".equals(user.getRole())) {
            model.addAttribute("allListings", propertyService.findAllForAdmin(moderationFilter, sortField, sortDir));
            model.addAttribute("pendingListings", propertyService.findPendingProperties());
            model.addAttribute("moderationFilter", moderationFilter != null ? moderationFilter.toUpperCase() : "");
            model.addAttribute("sortField", sortField  != null ? sortField.toLowerCase()  : "");
            model.addAttribute("sortDir",   sortDir    != null ? sortDir.toLowerCase()    : "");
        }

        return "dashboard";
    }
}