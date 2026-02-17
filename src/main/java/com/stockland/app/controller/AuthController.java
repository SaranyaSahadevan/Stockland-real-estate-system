package com.stockland.app.controller;

import com.stockland.app.model.User;
import com.stockland.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "login"; // login.html
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "register"; // register.html
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("user") User user) {
        userService.register(user);
        return "redirect:/login";
    }
}