package com.stockland.app.controller;

import com.stockland.app.dto.ChatRequest;
import com.stockland.app.dto.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatbotController {

    @PostMapping(
            value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String userMsg = request.getMessage() == null ? "" : request.getMessage().trim();

        if (userMsg.isEmpty()) {
            return new ChatResponse("Please type a message 😊");
        }

        return new ChatResponse("✅ Backend connected! You said: " + userMsg);
    }
}
