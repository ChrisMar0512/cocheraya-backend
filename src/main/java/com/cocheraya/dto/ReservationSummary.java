package com.cocheraya.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservationSummary {

    private Long id;
    private String status;
    private LocalDateTime reservedAt;

    
    private String driverName;
}
