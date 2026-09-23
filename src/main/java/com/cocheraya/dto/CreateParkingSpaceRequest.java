package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateParkingSpaceRequest {

    
    private String title;

    
    private String description;

    
    private String address;

    
    private BigDecimal pricePerHour;

    /** Latitud GPS de la cochera (eje Y en WGS84) */
    private Double latitude;

    /** Longitud GPS de la cochera (eje X en WGS84) */
    private Double longitude;

    
    private List<Long> featureIds;
}
