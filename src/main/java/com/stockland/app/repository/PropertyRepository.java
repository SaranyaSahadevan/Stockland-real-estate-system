package com.stockland.app.repository;

import com.stockland.app.model.Property;
import com.stockland.app.model.PropertyStatus;
import com.stockland.app.model.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByLocationContainingIgnoreCase(String location);

    List<Property> findByPropertyType(PropertyType propertyType);

    List<Property> findByStatus(PropertyStatus status);

    List<Property> findByPriceBetween(BigDecimal min, BigDecimal max);
}
