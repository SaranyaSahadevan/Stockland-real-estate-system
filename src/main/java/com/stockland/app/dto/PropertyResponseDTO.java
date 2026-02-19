package com.stockland.app.dto;

import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyResponseDTO {
    private Long id;
    private String title;
    private String location;
    private Double price;
    private String description;
    private ActionType actionType;
    private PropertyType propertyType;
    private String status;
    // User info
    private long userID;
    private String username;
}

