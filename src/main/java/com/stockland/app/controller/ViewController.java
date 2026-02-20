package com.stockland.app.controller;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.dto.UserResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import com.stockland.app.service.PropertyService;
import com.stockland.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
public class ViewController {
    @Autowired
    PropertyService propertyService;

    @Autowired
    UserService userService;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("actions", ActionType.values());

        model.addAttribute("filters", new PropertyFilterRequestDTO());



        List<PropertyResponseDTO> featuredProperties = propertyService.findAll()
                .stream()
                .limit(2)
                .toList();

        model.addAttribute("featuredProperties", featuredProperties);
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/listings")
    public String listings(Model model) {
        model.addAttribute("actions", ActionType.values());
        model.addAttribute("propertyTypes", PropertyType.values());
        model.addAttribute("filters", new PropertyFilterRequestDTO());
        return "listings";
    }

    @GetMapping("/property/{id}")
    public String property(@PathVariable("id") Long id, Model model) {
        PropertyResponseDTO property = propertyService.findById(id);

        UserResponseDTO user = userService.findByUsername(property.getUsername());

        model.addAttribute("user", user);
        model.addAttribute("property", property);

        return "property";
    }

    @GetMapping("/create-listing")
    public String createListing(Model model) {
        model.addAttribute("actions", ActionType.values());
        model.addAttribute("propertyTypes", PropertyType.values());
        model.addAttribute("propertyRequestDTO", new PropertyRequestDTO());
        return "create-listing";
    }

    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }
}
