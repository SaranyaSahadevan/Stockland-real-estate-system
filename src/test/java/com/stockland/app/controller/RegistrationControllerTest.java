package com.stockland.app.controller;

import com.stockland.app.dto.UserRegistrationDTO;
import com.stockland.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private RegistrationController registrationController;

    private MockMvc mockMvc;


    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/"); // Adjust this path based on your actual view location
        viewResolver.setSuffix(".html"); // Adjust this suffix based on your actual view file extension

        mockMvc = MockMvcBuilders.standaloneSetup(registrationController)
                .setViewResolvers(viewResolver)
                .build();
    }

    // Test to verify that the registration form is displayed correctly with an empty UserRegistrationDTO
    // in the model when the /register endpoint is accessed via GET
    @Test
    void showRegistrationForm_ReturnsRegisterView_WithEmptyDTO() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    // Test to verify that when the registration form is submitted with invalid data, the controller returns the
    // register view again and does not call the userService to register the user
    @Test
    void registerUser_ReturnsRegisterView_WhenValidationFails() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "")
                        .param("email", "not-an-email")
                        .param("fullName", "")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(userService, never()).registerUser(any());
    }

    // Test to verify that when the registration form is submitted with valid data, the controller calls the
    // userService to register the user and redirects to the login page with a success message
    @Test
    void registerUser_RedirectsToLogin_WhenRegistrationSucceeds() throws Exception {
        doNothing().when(userService).registerUser(any(UserRegistrationDTO.class));

        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("email", "test@example.com")
                        .param("fullName", "Test User")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    // Test to verify that when the registration form is submitted with a username that already exists, the controller
    // calls the userService to register the user, which throws an IllegalArgumentException, and the controller
    // catches this exception and redirects back to the register page with an error message
    @Test
    void registerUser_RedirectsToRegister_WhenUsernameAlreadyExists() throws Exception {
        doThrow(new IllegalArgumentException("Username already exists"))
                .when(userService).registerUser(any(UserRegistrationDTO.class));

        mockMvc.perform(post("/register")
                        .param("username", "existinguser")
                        .param("email", "test@example.com")
                        .param("fullName", "Test User")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("error", "Username already exists"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }

    // Test to verify that when the registration form is submitted with an email that already exists, the controller
    // calls the userService to register the user, which throws an IllegalArgumentException, and the controller
    // catches this exception and redirects back to the register page with an error message
    @Test
    void registerUser_RedirectsToRegister_WhenEmailAlreadyExists() throws Exception {
        doThrow(new IllegalArgumentException("Email already exists"))
                .when(userService).registerUser(any(UserRegistrationDTO.class));

        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("email", "existing@example.com")
                        .param("fullName", "Test User")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register"))
                .andExpect(flash().attribute("error", "Email already exists"));

        verify(userService, times(1)).registerUser(any(UserRegistrationDTO.class));
    }
}
