package com.stockland.app.repository;

import com.stockland.app.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // Test for saving and finding user by username
    @Test
    @DisplayName("Should save and find user by username")
    void shouldFindUserByUsername() {
        User user = User.builder()
                .username("john")
                .email("john@example.com")
                .password("password")
                .role("ROLE_USER")
                .fullName("John Doe")
                .build();

        userRepository.save(user);

        Optional<User> foundUser = userRepository.findByUsername("john");

        assertTrue(foundUser.isPresent());
        assertEquals("john", foundUser.get().getUsername());
    }

    // Test for finding user by email
    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        User user = User.builder()
                .username("mike")
                .email("mike@example.com")
                .password("password")
                .role("ROLE_USER")
                .fullName("Mike Smith")
                .build();

        userRepository.save(user);

        Optional<User> foundUser = userRepository.findByEmail("mike@example.com");

        assertTrue(foundUser.isPresent());
        assertEquals("mike@example.com", foundUser.get().getEmail());
    }

    // Test for finding user by username when user does not exist
    @Test
    @DisplayName("Should return empty when user not found by username")
    void shouldReturnEmptyIfNotFoundByUsername() {
        Optional<User> foundUser = userRepository.findByUsername("unknown");
        assertTrue(foundUser.isEmpty());
    }

    // Test for finding user by email when user does not exist
    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyIfNotFoundByEmail() {
        Optional<User> foundUser = userRepository.findByEmail("nobody@example.com");
        assertTrue(foundUser.isEmpty());
    }
}
