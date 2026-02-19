package com.stockland.app.controller;

import com.stockland.app.dto.*;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import com.stockland.app.service.PropertyService;
import com.stockland.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/properties")
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String searchProperties(
            @Valid PropertyFilterRequestDTO filters,
            BindingResult bindingResult,
            @PageableDefault(size = 20) Pageable pageable,
            Model model
    ) {

        model.addAttribute("actions", ActionType.values());
        model.addAttribute("propertyTypes", PropertyType.values());

        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(e -> e.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            model.addAttribute("filters", filters);
            model.addAttribute("errorMessage", errors);
            return "listings";
        }

        Page<PropertyResponseDTO> properties =
                propertyService.searchPropertiesWithFilterSortAndPagination(filters, pageable);

        model.addAttribute("properties", properties.getContent());
        model.addAttribute("filters", filters);

        return "listings";
    }

    @PostMapping("/create")
    public String createProperty(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid PropertyRequestDTO propertyRequestDTO,
            BindingResult bindingResult,
            Model model
    ) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("actions", ActionType.values());
            model.addAttribute("propertyTypes", PropertyType.values());
            return "create-listing";
        }

        String username = userDetails.getUsername();

        if (!userService.usernameExists(username)) {
            throw new RuntimeException("User not found: " + username);
        }

        UserResponseDTO user = userService.findByUsername(username);

        propertyService.saveProperty(propertyRequestDTO, user.getId());

        return "redirect:/dashboard";
    }
}
