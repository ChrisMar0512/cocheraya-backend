package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletResponse {

    
    private Long userId;

    
    private BigDecimal balance;

    
    private LocalDateTime updatedAt;
}
