package com.stockland.app.dto;

import com.stockland.app.model.ActionType;
import com.stockland.app.model.PropertyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyFilterRequestDTO {
    private String location;
    @Min(value = 0, message = "Price cannot be negative")
    private Double minPrice;
    @Min(value = 0, message = "Price cannot be negative")
    @Max(value = 999999999, message = "Price cannot exceed 999999999")
    private Double maxPrice;
    private ActionType actionType;
    private PropertyType propertyType;
    private String status;
}
