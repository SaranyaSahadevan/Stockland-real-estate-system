package com.stockland.app.dto;

import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyRequestDTO {
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private Double price;

    private String description;

    @NotNull(message = "Deal type is required")
    private ActionType actionType;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotBlank(message = "Status is required")
    private String status;
}
