package com.cocheraya.controller;

import com.cocheraya.dto.CreateReviewRequest;
import com.cocheraya.dto.ParkingSpaceReviewsResponse;
import com.cocheraya.dto.ReviewResponse;
import com.cocheraya.service.IReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final IReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request));
    }

    @GetMapping("/by-parking-space/{id}")
    public ResponseEntity<ParkingSpaceReviewsResponse> getReviewsForParkingSpace(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewsForParkingSpace(id));
    }

    @GetMapping("/by-user/{id}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForUser(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(id));
    }
}
