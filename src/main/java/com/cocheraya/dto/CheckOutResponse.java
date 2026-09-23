package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckOutResponse {

    
    private Long durationMinutes;

    
    private BigDecimal totalCharged;

    
    private BigDecimal remainingBalance;

    
    private String parkingSpaceName;

    
    private Long reservationId;
}
