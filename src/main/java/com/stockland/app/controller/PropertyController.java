package com.stockland.app.controller;

import com.stockland.app.dto.PropertyFilterRequestDTO;
import com.stockland.app.dto.PropertyResponseDTO;
import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import com.stockland.app.service.PropertyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/properties")
public class PropertyController {
    @Autowired
    PropertyService propertyService;

    @GetMapping
    public String searchProperties(@Valid PropertyFilterRequestDTO filters,
                                   BindingResult bindingResult,
                                   @PageableDefault(size = 20) Pageable pageable,
                                   Model model){

        model.addAttribute("actions", ActionType.values());
        model.addAttribute("propertyTypes", PropertyType.values());

        if (bindingResult.hasErrors()) {
            String allErrors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            model.addAttribute("filters", filters);
            model.addAttribute("errorMessage", "Invalid fields: " + allErrors);
            return "listings";
        }

        Page<PropertyResponseDTO> properties = propertyService.searchPropertiesWithFilterSortAndPagination(filters, pageable);

        model.addAttribute("properties", properties);
        model.addAttribute("filters", filters);

        return "listings";
    }
}
