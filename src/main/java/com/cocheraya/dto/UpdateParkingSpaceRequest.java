package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateParkingSpaceRequest {

    
    private String title;

    
    private String description;

    
    private String address;

    
    private BigDecimal pricePerHour;

    
    private Double latitude;

    
    private Double longitude;

    
    private List<Long> featureIds;
}
