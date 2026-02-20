package com.stockland.app.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatbotControllerTest {

    @InjectMocks
    private ChatbotController chatbotController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatbotController).build();
    }

    // Test that a null message returns the prompt response
    @Test
    @DisplayName("POST /api/chat with null message returns prompt response")
    void chat_ReturnsPrompt_WhenMessageIsNull() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": null}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reply").value("Please type a message 😊"));
    }

    // Test that a blank/whitespace-only message returns the prompt response
    @Test
    @DisplayName("POST /api/chat with blank message returns prompt response")
    void chat_ReturnsPrompt_WhenMessageIsBlank() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"   \"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reply").value("Please type a message 😊"));
    }

    // Test that an empty string message returns the prompt response
    @Test
    @DisplayName("POST /api/chat with empty string message returns prompt response")
    void chat_ReturnsPrompt_WhenMessageIsEmpty() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Please type a message 😊"));
    }

    // Test that a normal message returns the echo response containing the message
    @Test
    @DisplayName("POST /api/chat with valid message returns echo response")
    void chat_ReturnsEcho_WhenMessageIsValid() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Hello!\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reply").value("✅ Backend connected! You said: Hello!"));
    }

    // Test that leading/trailing whitespace is trimmed from a valid message
    @Test
    @DisplayName("POST /api/chat trims whitespace from message before echoing")
    void chat_TrimsWhitespace_BeforeEchoing() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"  Hello!  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("✅ Backend connected! You said: Hello!"));
    }
}

