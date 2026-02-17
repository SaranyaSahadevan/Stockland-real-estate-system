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
public class PropertyFilterRequestDTO {
    private String location;
    private Double minPrice;
    private Double maxPrice;
    private ActionType actionType;
    private PropertyType propertyType;
    private String status;
}
