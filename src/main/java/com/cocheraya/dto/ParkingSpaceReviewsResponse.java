package com.cocheraya.dto;

import lombok.Data;

import java.util.List;

@Data
public class ParkingSpaceReviewsResponse {

    
    private Long parkingSpaceId;

    
    private Double averageRating;

    
    private int totalReviews;

    
    private List<ReviewResponse> reviews;
}
