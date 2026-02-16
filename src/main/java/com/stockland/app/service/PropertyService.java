package com.stockland.app.service;

import com.stockland.app.model.PropertyRepository;
import org.springframework.stereotype.Service;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;

    public PropertyService(PropertyRepository propertyRepository){
        this.propertyRepository = propertyRepository;
    }


}
