package com.stockland.app.controller;

import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ViewController {
    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("actions", ActionType.values());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/listings")
    public String listings(Model model) {
        model.addAttribute("actions", ActionType.values());
        model.addAttribute("propertyTypes", PropertyType.values());
        return "listings";
    }

    @GetMapping("/property")
    public String property() {
        return "property";
    }

    @GetMapping("/create-listing")
    public String createListing() {
        return "create-listing";
    }

    @GetMapping("/logout")
    public String logout() {
        return "logout";
    }
}

