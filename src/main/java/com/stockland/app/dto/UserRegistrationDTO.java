package com.stockland.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegistrationDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be 3-20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Pattern(regexp = "^$|^[+]?[0-9 ()\\-]{7,20}$", message = "Invalid phone number format")
    private String phoneNumber;
}