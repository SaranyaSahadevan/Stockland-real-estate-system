package com.stockland.app.repository;

import com.stockland.app.model.ActionType;
import com.stockland.app.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {
    List<Property> findByPrice(Double price);

    List<Property> findByPriceLessThanEqual(Double price);

    List<Property> findByPriceGreaterThanEqual(Double price);

    List<Property> findByPriceBetween(Double min, Double max);

    List<Property> findByActionType(ActionType actionType);

    List<Property> findByLocationContainingIgnoreCase(String location);

    List<Property> findByStatus(String status);

    List<Property> findByUserId(Long userId);
}
