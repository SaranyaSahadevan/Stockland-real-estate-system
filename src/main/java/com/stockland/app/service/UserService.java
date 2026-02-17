package com.stockland.app.service;

import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private PasswordEncoder encoder;

    public void register(User user) {
        user.setPassword(encoder.encode(user.getPassword()));

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }

        repo.save(user);
    }
}