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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_id")
    private Long id;
    private String title;
    private String location;
    private Double price;
    @Column(columnDefinition = "TEXT")
    private String description;
    //Values: RENT, BUY
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private ActionType actionType;
    //Values: HOUSE, CONDO, MULTIFAMILY, LAND, APARTMENTS, COMMERCIAL
    @Enumerated(EnumType.STRING)
    @Column(name = "property_type")
    private PropertyType propertyType;
    private String status;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
