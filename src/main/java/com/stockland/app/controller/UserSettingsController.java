package com.stockland.app.controller;

import com.stockland.app.model.User;
import com.stockland.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserSettingsController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserSettingsController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/settings")
    public String settingsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);
        return "settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@RequestParam String username,
                                 @RequestParam String email,
                                 @RequestParam String fullName,
                                 @RequestParam(required = false) String currentPassword,
                                 @RequestParam(required = false) String newPassword,
                                 Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();

        User user = userService.getUserByUsername(currentUsername);

        // username check
        if (!username.equals(user.getUsername()) && userService.usernameExists(username)) {
            model.addAttribute("error", "Username is already taken.");
            model.addAttribute("user", user);
            return "settings";
        }

        // email check
        if (!email.equals(user.getEmail()) && userService.emailExists(email)) {
            model.addAttribute("error", "Email is already taken.");
            model.addAttribute("user", user);
            return "settings";
        }

        // password change
        if (newPassword != null && !newPassword.isEmpty()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                model.addAttribute("error", "Current password is incorrect.");
                model.addAttribute("user", user);
                return "settings";
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(fullName);

        userService.saveUser(user);

        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                user.getPassword(),
                auth.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        model.addAttribute("success", "Settings updated successfully.");
        model.addAttribute("user", user);

        return "settings";
    }
}

