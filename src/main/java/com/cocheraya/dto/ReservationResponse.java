package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReservationResponse {

    private Long id;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime expiresAt;

    
    private ParkingSpaceInfo parkingSpace;

    
    private DriverInfo driver;

    

    
    @Data
    public static class ParkingSpaceInfo {
        private Long id;
        private String title;
        private String address;
        private BigDecimal pricePerHour;
    }

    
    @Data
    public static class DriverInfo {
        private Long id;
        private String name;
        private String email;
    }
}
