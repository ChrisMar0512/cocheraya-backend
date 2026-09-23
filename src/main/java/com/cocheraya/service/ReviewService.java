package com.cocheraya.service;

import com.cocheraya.dto.CreateReviewRequest;
import com.cocheraya.dto.ParkingSpaceReviewsResponse;
import com.cocheraya.dto.ReviewResponse;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.entity.Review;
import com.cocheraya.entity.User;
import com.cocheraya.exception.DuplicateResourceException;
import com.cocheraya.exception.InvalidOperationException;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.exception.UnauthorizedOperationException;
import com.cocheraya.repository.ReservationRepository;
import com.cocheraya.repository.ReviewRepository;
import com.cocheraya.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        User reviewer = getAuthenticatedUser();
        
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada con id: " + request.getReservationId()));

        if (reservation.getStatus() != ReservationStatus.FINISHED) {
            throw new InvalidOperationException("Solo puedes reseñar reservas completadas");
        }

        if (reviewRepository.existsByReservationIdAndReviewerId(reservation.getId(), reviewer.getId())) {
            throw new DuplicateResourceException("Ya has enviado una reseña para esta reserva");
        }

        User driver = reservation.getDriver();
        User host = reservation.getParkingSpace().getHost();

        if (!reviewer.getId().equals(driver.getId()) && !reviewer.getId().equals(host.getId())) {
            throw new UnauthorizedOperationException("No tienes permiso para reseñar esta reserva");
        }

        User reviewee = reviewer.getId().equals(driver.getId()) ? host : driver;

        Review review = new Review();
        review.setReservation(reservation);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setParkingSpace(reservation.getParkingSpace());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);
        return mapToResponse(savedReview);
    }

    public ParkingSpaceReviewsResponse getReviewsForParkingSpace(Long parkingSpaceId) {
        List<ReviewResponse> reviews = reviewRepository.findByParkingSpaceIdOrderByCreatedAtDesc(parkingSpaceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        Double avgRating = reviewRepository.averageRatingByParkingSpaceId(parkingSpaceId);
        long totalReviews = reviewRepository.countByParkingSpaceId(parkingSpaceId);

        ParkingSpaceReviewsResponse response = new ParkingSpaceReviewsResponse();
        response.setParkingSpaceId(parkingSpaceId);
        response.setAverageRating(avgRating);
        response.setTotalReviews((int) totalReviews);
        response.setReviews(reviews);
        return response;
    }

    public List<ReviewResponse> getReviewsForUser(Long userId) {
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado: " + email));
    }

    private ReviewResponse mapToResponse(Review review) {
        ReviewResponse response = modelMapper.map(review, ReviewResponse.class);
        response.setReviewerName(review.getReviewer().getName());
        response.setRevieweeName(review.getReviewee().getName());
        return response;
    }
}
