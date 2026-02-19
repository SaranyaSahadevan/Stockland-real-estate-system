package com.stockland.app.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;
    private String username;
    private String password;
    private String role;
    private String email;
    private String fullName;
}
