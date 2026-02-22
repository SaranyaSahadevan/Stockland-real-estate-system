package com.stockland.app.model;

import jakarta.persistence.*;

@Entity
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Property property;

    public Favorite() {}

    public Favorite(User user, Property property) {
        this.user = user;
        this.property = property;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Property getProperty() {
        return property;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setProperty(Property property) {
        this.property = property;
    }
}