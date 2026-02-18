package com.stockland.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/listings")
    public String listings() {
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
