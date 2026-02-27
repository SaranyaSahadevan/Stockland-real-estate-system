package com.stockland.app.service;

import com.stockland.app.dto.UserRegistrationDTO;
import com.stockland.app.dto.UserResponseDTO;
import com.stockland.app.model.User;
import com.stockland.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    // Mock dependencies for UserService and inject them into the service instance for testing
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    // Initialize mocks before each test to ensure a clean state for each test case
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Test for checking if a username exists in the repository, expecting true when the user is found
    @Test
    void usernameExists_ReturnsTrue_WhenUserExists() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(new User()));
        assertTrue(userService.usernameExists("john"));
    }

    // Test for checking if a username exists in the repository, expecting false when the user is not found
    @Test
    void usernameExists_ReturnsFalse_WhenUserDoesNotExist() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        assertFalse(userService.usernameExists("john"));
    }

    // Test for checking if an email exists in the repository, expecting true when the email is found
    @Test
    void emailExists_ReturnsTrue_WhenEmailExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(new User()));
        assertTrue(userService.emailExists("test@example.com"));
    }

    // Test for checking if an email exists in the repository, expecting false when the email is not found
    @Test
    void emailExists_ReturnsFalse_WhenEmailDoesNotExist() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        assertFalse(userService.emailExists("test@example.com"));
    }

    // Test for registering a user, verifying that the password is encoded and the role is set before saving the user to the repository
    @Test
    void registerUser_SavesUserWithEncodedPasswordAndRole() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("john");
        dto.setEmail("john@example.com");
        dto.setFullName("John Doe");
        dto.setPassword("plainPassword");

        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");

        userService.registerUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals("ROLE_USER", savedUser.getRole());
    }

    // Test that registerUser throws IllegalArgumentException when username already exists
    @Test
    void registerUser_ThrowsException_WhenUsernameAlreadyExists() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("john");
        dto.setEmail("john@example.com");
        dto.setPassword("plainPassword");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
    }

    // Test that registerUser throws IllegalArgumentException when email already exists
    @Test
    void registerUser_ThrowsException_WhenEmailAlreadyExists() {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setUsername("john");
        dto.setEmail("john@example.com");
        dto.setPassword("plainPassword");

        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
    }

    // Test that findByUsername returns correct DTO when user exists
    @Test
    void findByUsername_ReturnsDTO_WhenUserExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setFullName("John Doe");
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserResponseDTO result = userService.findByUsername("john");

        assertNotNull(result);
        assertEquals("john", result.getUsername());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John Doe", result.getFullName());
        assertEquals("ROLE_USER", result.getRole());
        assertEquals(1L, result.getId());
    }

    // Test that findByUsername throws UsernameNotFoundException when user does not exist
    @Test
    void findByUsername_ThrowsException_WhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.findByUsername("unknown"));
    }

    // Test for loading user details by username, expecting to return user details when the user exists in the repository by username
    @Test
    void loadUserByUsername_ReturnsUserDetails_WhenUserExistsByUsername() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encodedPassword");
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("john");

        assertEquals("john", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    // Test for loading user details by username, expecting to return user details when the user exists in the repository by email if not found by username
    @Test
    void loadUserByUsername_ReturnsUserDetails_WhenUserExistsByEmail() {
        User user = new User();
        user.setUsername("john");
        user.setPassword("encodedPassword");
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("test@example.com");

        assertEquals("john", userDetails.getUsername());
    }

    // Test for loading user details by username, expecting to throw a UsernameNotFoundException when the user is not found in the repository by either username or email
    @Test
    void loadUserByUsername_ThrowsException_WhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("unknown");
        });
    }
}
