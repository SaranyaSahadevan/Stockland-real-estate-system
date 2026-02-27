package com.stockland.app.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

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
    private Double area;
    private Integer roomCount;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status")
    private ModerationStatus moderationStatus;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Builder.Default
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "featured", nullable = false, columnDefinition = "boolean default false")
    private boolean featured;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
