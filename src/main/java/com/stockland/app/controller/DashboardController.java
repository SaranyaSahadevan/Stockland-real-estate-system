package com.stockland.app.controller;

import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;

@Controller
public class DashboardController {

    private final UserRepository userRepository;

    @Autowired
    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseGet(() ->
                        userRepository.findByEmail(username).orElse(null));

        model.addAttribute("user", user);
        model.addAttribute("favorites", Collections.emptyList());
        model.addAttribute("myListings", Collections.emptyList());

        return "dashboard";
    }
}
