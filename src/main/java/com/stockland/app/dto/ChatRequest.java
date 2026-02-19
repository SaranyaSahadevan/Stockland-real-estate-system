package com.stockland.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 500, message = "Message too long")
    private String message;
}
