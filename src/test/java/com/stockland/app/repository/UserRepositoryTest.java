//package com.stockland.app.repository;
//
//import com.stockland.app.model.User;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
//
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@DataJpaTest
//class UserRepositoryTest {
//
//    // Inject the UserRepository to test its methods
//    @Autowired
//    private UserRepository userRepository;
//
//    // Test for saving and finding user by username
//    @Test
//    @DisplayName("Should save and find user by username")
//    void shouldFindUserByUsername() {
//        User user = new User();
//        user.setUsername("john");
//        user.setEmail("john@example.com");
//        user.setPassword("password");
//        user.setRole("ROLE_USER");
//
//        userRepository.save(user);
//
//        Optional<User> foundUser = userRepository.findByUsername("john");
//
//        assertTrue(foundUser.isPresent());
//        assertEquals("john", foundUser.get().getUsername());
//    }
//
//    // Test for finding user by email
//    @Test
//    @DisplayName("Should find user by email")
//    void shouldFindUserByEmail() {
//        User user = new User();
//        user.setUsername("mike");
//        user.setEmail("mike@example.com");
//        user.setPassword("password");
//        user.setRole("ROLE_USER");
//
//        userRepository.save(user);
//
//        Optional<User> foundUser = userRepository.findByEmail("mike@example.com");
//
//        assertTrue(foundUser.isPresent());
//        assertEquals("mike@example.com", foundUser.get().getEmail());
//    }
//
//    // Test for finding user by username when user does not exist
//    @Test
//    @DisplayName("Should return empty when user not found")
//    void shouldReturnEmptyIfNotFound() {
//        Optional<User> foundUser = userRepository.findByUsername("unknown");
//        assertTrue(foundUser.isEmpty());
//    }
//}