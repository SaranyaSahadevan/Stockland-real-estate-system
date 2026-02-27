package com.stockland.app.controller;

import com.stockland.app.model.User;
import com.stockland.app.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserSettingsController userSettingsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(userSettingsController)
                .setViewResolvers(viewResolver)
                .build();

        // Default: authenticate as "john"
        authenticateAs("john");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setFullName("John Doe");
        user.setPassword("encodedPassword");
        user.setRole("ROLE_USER");
        return user;
    }

    // GET /settings — returns settings view with the current user in the model
    @Test
    @DisplayName("GET /settings returns settings view with user in model")
    void getSettings_ReturnsSettingsView_WithUserInModel() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attribute("user", user));
    }

    // POST /settings — happy path: saves user, updates SecurityContext and adds success message
    @Test
    @DisplayName("POST /settings with valid data saves user and returns success message")
    void postSettings_Success_SavesUserAndReturnsSuccessMessage() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        // Same username and email as stored → controller skips existence checks entirely

        mockMvc.perform(post("/settings")
                        .param("username",    "john")
                        .param("email",       "john@example.com")
                        .param("fullName",    "John Doe")
                        .param("phoneNumber", "12345678"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attributeExists("success"))
                .andExpect(model().attributeExists("user"));

        verify(userService).saveUser(user);
    }

    // POST /settings — username already taken by another account
    @Test
    @DisplayName("POST /settings returns error when new username is already taken")
    void postSettings_ReturnsError_WhenUsernameAlreadyTaken() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        // Requesting a different username that is taken
        when(userService.usernameExists("newjohn")).thenReturn(true);

        mockMvc.perform(post("/settings")
                        .param("username", "newjohn")
                        .param("email",    "john@example.com")
                        .param("fullName", "John Doe"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attribute("error", "Username is already taken."))
                .andExpect(model().attribute("user", user));

        verify(userService, never()).saveUser(any());
    }

    // POST /settings — username changed but NOT taken → proceeds to save (covers true&&false branch on line 49)
    @Test
    @DisplayName("POST /settings succeeds when new username is not taken")
    void postSettings_Success_WhenNewUsernameIsNotTaken() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        when(userService.usernameExists("johnny")).thenReturn(false);

        mockMvc.perform(post("/settings")
                        .param("username", "johnny")
                        .param("email",    "john@example.com")
                        .param("fullName", "John Doe"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attributeExists("success"));

        verify(userService).saveUser(user);
    }

    // POST /settings — email already taken by another account
    @Test
    @DisplayName("POST /settings returns error when new email is already taken")
    void postSettings_ReturnsError_WhenEmailAlreadyTaken() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        // Same username → usernameExists check skipped; different email → emailExists is checked
        when(userService.emailExists("other@example.com")).thenReturn(true);

        mockMvc.perform(post("/settings")
                        .param("username", "john")
                        .param("email",    "other@example.com")
                        .param("fullName", "John Doe"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attribute("error", "Email is already taken."))
                .andExpect(model().attribute("user", user));

        verify(userService, never()).saveUser(any());
    }

    // POST /settings — email changed but NOT taken → proceeds to save (covers true&&false branch on line 55)
    @Test
    @DisplayName("POST /settings succeeds when new email is not taken")
    void postSettings_Success_WhenNewEmailIsNotTaken() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        when(userService.emailExists("newemail@example.com")).thenReturn(false);

        mockMvc.perform(post("/settings")
                        .param("username", "john")
                        .param("email",    "newemail@example.com")
                        .param("fullName", "John Doe"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attributeExists("success"));

        verify(userService).saveUser(user);
    }

    // POST /settings — wrong current password when changing password
    @Test
    @DisplayName("POST /settings returns error when current password is incorrect")
    void postSettings_ReturnsError_WhenCurrentPasswordIncorrect() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        // Same username and email → existence checks are skipped by the controller
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        mockMvc.perform(post("/settings")
                        .param("username",        "john")
                        .param("email",           "john@example.com")
                        .param("fullName",        "John Doe")
                        .param("currentPassword", "wrongPassword")
                        .param("newPassword",     "newSecret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attribute("error", "Current password is incorrect."));

        verify(userService, never()).saveUser(any());
    }

    // POST /settings — newPassword supplied but currentPassword is null
    @Test
    @DisplayName("POST /settings returns error when newPassword is provided but currentPassword is null")
    void postSettings_ReturnsError_WhenCurrentPasswordIsNull() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);

        mockMvc.perform(post("/settings")
                        .param("username",    "john")
                        .param("email",       "john@example.com")
                        .param("fullName",    "John Doe")
                        // currentPassword param intentionally omitted → arrives as null
                        .param("newPassword", "newSecret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attribute("error", "Current password is incorrect."))
                .andExpect(model().attribute("user", user));

        verify(userService, never()).saveUser(any());
    }

    // POST /settings — correct current password results in new encoded password being saved
    @Test
    @DisplayName("POST /settings updates password when current password is correct")
    void postSettings_UpdatesPassword_WhenCurrentPasswordIsCorrect() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        // Same username and email → existence checks are skipped by the controller
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newSecret123")).thenReturn("newEncodedPassword");

        mockMvc.perform(post("/settings")
                        .param("username",        "john")
                        .param("email",           "john@example.com")
                        .param("fullName",        "John Doe")
                        .param("currentPassword", "correctPassword")
                        .param("newPassword",     "newSecret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attributeExists("success"));

        verify(passwordEncoder).encode("newSecret123");
        verify(userService).saveUser(user);
    }

    // POST /settings — newPassword is empty string → password block skipped, save proceeds (covers true&&false branch on line 64)
    @Test
    @DisplayName("POST /settings skips password change when newPassword is empty string")
    void postSettings_SkipsPasswordChange_WhenNewPasswordIsEmpty() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);

        mockMvc.perform(post("/settings")
                        .param("username",        "john")
                        .param("email",           "john@example.com")
                        .param("fullName",        "John Doe")
                        .param("currentPassword", "anyValue")
                        .param("newPassword",     ""))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attributeExists("success"));

        verify(passwordEncoder, never()).encode(any());
        verify(userService).saveUser(user);
    }

    // POST /settings — blank phoneNumber is stored as null
    @Test
    @DisplayName("POST /settings stores null when phoneNumber is blank")
    void postSettings_StoresNullPhoneNumber_WhenBlank() throws Exception {
        User user = buildUser();
        when(userService.getUserByUsername("john")).thenReturn(user);
        // Same username and email → existence checks are skipped by the controller

        mockMvc.perform(post("/settings")
                        .param("username",     "john")
                        .param("email",        "john@example.com")
                        .param("fullName",     "John Doe")
                        .param("phoneNumber",  "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"))
                .andExpect(model().attributeExists("success"));

        verify(userService).saveUser(argThat(u -> u.getPhoneNumber() == null));
    }
}






