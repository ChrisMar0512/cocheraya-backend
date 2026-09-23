package com.cocheraya.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckInResponse {

    
    private String message;

    
    private LocalDateTime timestamp;
}
