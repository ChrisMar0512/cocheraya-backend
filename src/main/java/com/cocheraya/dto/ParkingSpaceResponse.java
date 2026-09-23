package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ParkingSpaceResponse {

    private Long id;
    private String title;
    private String description;
    private String address;
    private BigDecimal pricePerHour;
    private String status;

    /** Latitud extraída del Point de PostGIS (eje Y en WGS84) */
    private Double latitude;

    /** Longitud extraída del Point de PostGIS (eje X en WGS84) */
    private Double longitude;

    
    private String photoUrl;

    
    private List<String> features;

    
    private String hostName;

    
    private Integer favoritesCount;
}
