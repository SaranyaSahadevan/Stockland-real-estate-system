package com.stockland.app.dto;

import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "Price is required")
    @Pattern(regexp = "^\\d{1,12}(,\\d{1,2})?$", message = "Price must be in format: 1234,99 or 1234,00 (comma as decimal separator, max 2 decimal digits)")
    private String price;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be positive")
    private Double area;

    @NotNull(message = "Room number is required")
    @Positive(message = "Room number must be positive")
    private Integer roomCount;

    private String description;

    @NotNull(message = "Deal type is required")
    private ActionType actionType;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotBlank(message = "Status is required")
    private String status;
}
