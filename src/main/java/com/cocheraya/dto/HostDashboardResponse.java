package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HostDashboardResponse {

    
    private long totalSpaces;

    
    private long totalReservationsCompleted;

    
    private BigDecimal totalEarnings;

    
    private Double averageRating;

    
    private List<ReservationSummary> recentReservations;

    

    
    @Data
    public static class ReservationSummary {

        private Long reservationId;
        private String driverName;
        private String parkingSpaceTitle;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime endTime;
        private java.math.BigDecimal pricePerHour;
    }
}

