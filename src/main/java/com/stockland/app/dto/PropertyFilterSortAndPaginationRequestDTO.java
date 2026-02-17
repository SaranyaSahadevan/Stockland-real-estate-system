package com.stockland.app.dto;

import com.stockland.app.model.ActionType;

public class PropertyFilterSortAndPaginationRequestDTO {
    private String location;
    private Double minPrice;
    private Double maxPrice;
    private ActionType actionType;
    private String status;
    private Integer pageCount;
    private SortingType sortingType;
}
