package com.stockland.app.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    List<Property> findByPrice(Double price);

    List<Property> findByPriceLessThanEqual(Double price);

    List<Property> findByPriceGreaterThanEqual(Double price);

    List<Property> findByPriceBetween(Double min, Double max);

    List<Property> findByPropertyType(PropertyType propertyType);

    List<Property> findByLocationContainingIgnoreCase(String location);

    List<Property> findByStatus(String status);
}
