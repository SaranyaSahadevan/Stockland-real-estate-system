package com.stockland.app.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTest {

    @Test
    @DisplayName("onCreate sets createdAt when it is null")
    void onCreate_SetsCreatedAt_WhenNull() {
        Property property = new Property();
        assertNull(property.getCreatedAt());

        property.onCreate();

        assertNotNull(property.getCreatedAt());
    }

    @Test
    @DisplayName("onCreate does not overwrite createdAt when it is already set")
    void onCreate_DoesNotOverwriteCreatedAt_WhenAlreadySet() {
        LocalDateTime existingTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        Property property = new Property();
        property.setCreatedAt(existingTime);

        property.onCreate();

        assertEquals(existingTime, property.getCreatedAt());
    }
}

