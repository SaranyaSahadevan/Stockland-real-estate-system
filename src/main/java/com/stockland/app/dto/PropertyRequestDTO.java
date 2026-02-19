package com.stockland.app.dto;

import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyRequestDTO {

    private Long id;

    @NotBlank(message = "Title required")
    @Size(max = 150, message = "Title too long")
    private String title;

    @NotBlank(message = "Location required")
    private String location;

    @NotNull(message = "Price required")
    @Positive(message = "Price must be positive")
    private Double price;

    @Size(max = 2000, message = "Description too long")
    private String description;

    @NotNull(message = "Action type required")
    private ActionType actionType;

    @NotNull(message = "Property type required")
    private PropertyType propertyType;

    @NotBlank(message = "Status required")
    private String status;
}
