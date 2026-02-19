package com.stockland.app.controller;

import com.stockland.app.dto.ChatRequest;
import com.stockland.app.dto.ChatResponse;
import jakarta.validation.Valid;
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
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {

        String message = request.getMessage().trim();

        return new ChatResponse("✅ Backend connected! You said: " + message);
    }
}
