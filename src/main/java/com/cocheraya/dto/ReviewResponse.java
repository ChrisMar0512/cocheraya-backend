package com.cocheraya.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {

    private Long id;

    
    private String reviewerName;

    
    private String revieweeName;

    
    private Integer rating;

    
    private String comment;

    
    private LocalDateTime createdAt;
}
