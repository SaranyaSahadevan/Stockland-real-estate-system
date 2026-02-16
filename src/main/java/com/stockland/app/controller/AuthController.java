package com.stockland.app.controller;

import com.stockland.app.model.Role;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String email,
                               @RequestParam String password,
                               Model model) {

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("message", "Username already exists");
            return "register";
        }
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("message", "Email already exists");
            return "register";
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(Role.USER))
                .build();

        userRepository.save(user);

        model.addAttribute("message", "Registration successful. Please login.");
        return "login";
    }
}
