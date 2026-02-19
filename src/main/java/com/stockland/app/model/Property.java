package com.stockland.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title too long")
    private String title;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Price required")
    @Positive(message = "Price must be positive")
    private Double price;

    @Column(columnDefinition = "TEXT")
    @Size(max = 2000, message = "Description too long")
    private String description;

    @NotNull(message = "Action type required")
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private ActionType actionType;

    @NotNull(message = "Property type required")
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    private PropertyType propertyType;

    @NotBlank(message = "Status required")
    private String status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;
}
