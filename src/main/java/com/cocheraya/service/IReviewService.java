package com.cocheraya.service;

import com.cocheraya.dto.CreateReviewRequest;
import com.cocheraya.dto.ParkingSpaceReviewsResponse;
import com.cocheraya.dto.ReviewResponse;
import java.util.List;

public interface IReviewService {
    ReviewResponse createReview(CreateReviewRequest request);
    ParkingSpaceReviewsResponse getReviewsForParkingSpace(Long parkingSpaceId);
    List<ReviewResponse> getReviewsForUser(Long userId);
}
