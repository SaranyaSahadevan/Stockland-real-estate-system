package com.stockland.app.service;

import com.stockland.app.dto.UserResponseDTO;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usernameExists(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return userRepository.findByUsername(username.trim()).isPresent();
    }

    public boolean emailExists(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return userRepository.findByEmail(email.trim()).isPresent();
    }

    public void registerUser(@Valid User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (!StringUtils.hasText(user.getUsername())) {
            throw new IllegalArgumentException("Username required");
        }

        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("Password required");
        }

        if (!StringUtils.hasText(user.getEmail())) {
            throw new IllegalArgumentException("Email required");
        }

        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim());

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");

        userRepository.save(user);
    }

    public UserResponseDTO findByUsername(String username) {

        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username required");
        }

        Optional<User> userOptional =
                userRepository.findByUsername(username.trim());

        User user = userOptional.orElseThrow(
                () -> new RuntimeException("User not found: " + username)
        );

        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .role(user.getRole())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail)
            throws UsernameNotFoundException {

        if (!StringUtils.hasText(usernameOrEmail)) {
            throw new UsernameNotFoundException("Username required");
        }

        Optional<User> userOpt =
                userRepository.findByUsername(usernameOrEmail.trim());

        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(usernameOrEmail.trim());
        }

        User user = userOpt.orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRole())
                .build();
    }
}
