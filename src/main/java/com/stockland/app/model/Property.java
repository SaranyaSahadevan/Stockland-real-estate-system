package com.stockland.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long property_id;
    private String title;
    private String location;
    private Double price;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    private PropertyType propertyType;
    private String status;
}
