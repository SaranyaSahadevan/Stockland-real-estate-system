package com.stockland.app.controller;

import com.stockland.app.service.PropertyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    private final PropertyService propertyService;

    public AdminController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping("/admin/properties")
    public String adminProperties(Model model) {
        model.addAttribute("properties", propertyService.getAllProperties());
        return "admin-properties";
    }
}
