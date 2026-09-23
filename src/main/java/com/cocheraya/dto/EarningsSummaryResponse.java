package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EarningsSummaryResponse {

    
    private BigDecimal totalEarned;

    
    private BigDecimal earningsThisMonth;

    
    private long transactionCount;
}
