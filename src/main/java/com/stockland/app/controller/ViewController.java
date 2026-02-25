package com.stockland.app.controller;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.dto.UserResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import com.stockland.app.service.PropertyService;
import com.stockland.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
    public String login(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("error", "Invalid username/email or password");
        }
        return "login";
    }

    @GetMapping("/listings")
    public String searchProperties(@Valid PropertyFilterRequestDTO filters,
                                   BindingResult bindingResult,
                                   @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
                                   Model model) {

        model.addAttribute("actions", ActionType.values());
        model.addAttribute("propertyTypes", PropertyType.values());

        if (bindingResult.hasErrors()) {
            String allErrors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            Page<PropertyResponseDTO> propertyPage = propertyService.searchPropertiesWithFilterSortAndPagination(new PropertyFilterRequestDTO(), pageable);

            model.addAttribute("propertyPage", propertyPage);
            model.addAttribute("properties", propertyPage.getContent());
            model.addAttribute("filters", filters);
            model.addAttribute("errorMessage", "Invalid fields: " + allErrors);
            return "listings";
        }

        Page<PropertyResponseDTO> propertyPage =
                propertyService.searchPropertiesWithFilterSortAndPagination(filters, pageable);

        model.addAttribute("propertyPage", propertyPage);
        model.addAttribute("properties", propertyPage.getContent());
        model.addAttribute("filters", filters);

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

//    @GetMapping("/edit-property/{id}")
//    public String editProperty(@PathVariable("id") Long id, Model model) {
//        PropertyResponseDTO property = propertyService.findById(id);
//
//        UserResponseDTO user = userService.findByUsername(property.getUsername());
//
//        model.addAttribute("user", user);
//
//        model.addAttribute("actions", ActionType.values());
//        model.addAttribute("propertyTypes", PropertyType.values());
//        model.addAttribute("property", property);
//
//        return "edit-property";
//    }
}
