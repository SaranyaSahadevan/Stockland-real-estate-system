package com.stockland.app.dto;

import com.stockland.app.model.PropertyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyRequestDTO {
    private Long property_id;
    private String title;
    private String location;
    private Double price;
    private String description;
    private PropertyType propertyType;
    private String status;
}
