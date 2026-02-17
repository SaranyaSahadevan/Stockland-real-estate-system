package com.stockland.app.dto;

import com.stockland.app.model.PropertyType;

public class PropertyFilterSortAndPaginationRequestDTO {
    private String location;
    private Double minPrice;
    private Double maxPrice;
    private PropertyType propertyType;
    private String status;
    private Integer pageCount;
    private SortingType sortingType;
}
