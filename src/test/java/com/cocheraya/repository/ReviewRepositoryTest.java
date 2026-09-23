package com.cocheraya.repository;

import com.cocheraya.config.TestContainersConfig;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Review;
import com.cocheraya.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("ReviewRepository Tests")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User driver;
    private User host;
    private ParkingSpace parkingSpace;
    private Reservation reservation1;
    private Reservation reservation2;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setEmail("review-host@cocheraya.com");
        host.setPassword("encoded-password");
        host.setName("Review Host");
        host.setRole(User.Role.HOST);
        host.setEnabled(true);
        entityManager.persistAndFlush(host);

        driver = new User();
        driver.setEmail("review-driver@cocheraya.com");
        driver.setPassword("encoded-password");
        driver.setName("Review Driver");
        driver.setRole(User.Role.DRIVER);
        driver.setEnabled(true);
        entityManager.persistAndFlush(driver);

        parkingSpace = new ParkingSpace();
        parkingSpace.setHost(host);
        parkingSpace.setTitle("Cochera Review Test");
        parkingSpace.setAddress("Av. Larco 789, Miraflores");
        parkingSpace.setPricePerHour(new BigDecimal("8.00"));
        parkingSpace.setStatus(ParkingSpace.ParkingSpaceStatus.AVAILABLE);
        entityManager.persistAndFlush(parkingSpace);

        // Reservation 1 — FINISHED
        reservation1 = new Reservation();
        reservation1.setDriver(driver);
        reservation1.setParkingSpace(parkingSpace);
        reservation1.setStatus(Reservation.ReservationStatus.FINISHED);
        reservation1.setStartTime(LocalDateTime.now().minusHours(3));
        reservation1.setEndTime(LocalDateTime.now().minusHours(2));
        entityManager.persistAndFlush(reservation1);

        // Reservation 2 — FINISHED
        reservation2 = new Reservation();
        reservation2.setDriver(driver);
        reservation2.setParkingSpace(parkingSpace);
        reservation2.setStatus(Reservation.ReservationStatus.FINISHED);
        reservation2.setStartTime(LocalDateTime.now().minusHours(6));
        reservation2.setEndTime(LocalDateTime.now().minusHours(5));
        entityManager.persistAndFlush(reservation2);

        // Review for reservation 1 — rating 4
        Review review1 = new Review();
        review1.setReservation(reservation1);
        review1.setReviewer(driver);
        review1.setReviewee(host);
        review1.setParkingSpace(parkingSpace);
        review1.setRating(4);
        review1.setComment("Buena cochera, bien ubicada");
        entityManager.persistAndFlush(review1);

        // Review for reservation 2 — rating 5
        Review review2 = new Review();
        review2.setReservation(reservation2);
        review2.setReviewer(driver);
        review2.setReviewee(host);
        review2.setParkingSpace(parkingSpace);
        review2.setRating(5);
        review2.setComment("Excelente servicio");
        entityManager.persistAndFlush(review2);
    }

    @Test
    @DisplayName("should return reviews ordered by createdAt desc when parking space has reviews")
    void shouldReturnReviewsOrderedByCreatedAtDescWhenParkingSpaceHasReviews() {
        List<Review> reviews = reviewRepository
                .findByParkingSpaceIdOrderByCreatedAtDesc(parkingSpace.getId());

        assertThat(reviews).hasSize(2);
        for (int i = 0; i < reviews.size() - 1; i++) {
            assertThat(reviews.get(i).getCreatedAt())
                    .isAfterOrEqualTo(reviews.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("should return empty list when parking space has no reviews")
    void shouldReturnEmptyListWhenParkingSpaceHasNoReviews() {
        List<Review> reviews = reviewRepository
                .findByParkingSpaceIdOrderByCreatedAtDesc(99999L);

        assertThat(reviews).isEmpty();
    }

    @Test
    @DisplayName("should return true when review exists for reservation and reviewer")
    void shouldReturnTrueWhenReviewExistsForReservationAndReviewer() {
        boolean exists = reviewRepository
                .existsByReservationIdAndReviewerId(reservation1.getId(), driver.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("should return false when no review exists for reservation and reviewer")
    void shouldReturnFalseWhenNoReviewExistsForReservationAndReviewer() {
        boolean exists = reviewRepository
                .existsByReservationIdAndReviewerId(reservation1.getId(), host.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("should return average rating when parking space has reviews")
    void shouldReturnAverageRatingWhenParkingSpaceHasReviews() {
        Double avgRating = reviewRepository
                .averageRatingByParkingSpaceId(parkingSpace.getId());

        // (4 + 5) / 2 = 4.5
        assertThat(avgRating).isNotNull();
        assertThat(avgRating).isCloseTo(4.5, within(0.01));
    }

    @Test
    @DisplayName("should return null average rating when parking space has no reviews")
    void shouldReturnNullAverageRatingWhenParkingSpaceHasNoReviews() {
        Double avgRating = reviewRepository.averageRatingByParkingSpaceId(99999L);

        assertThat(avgRating).isNull();
    }

    @Test
    @DisplayName("should return correct count when parking space has reviews")
    void shouldReturnCorrectCountWhenParkingSpaceHasReviews() {
        long count = reviewRepository.countByParkingSpaceId(parkingSpace.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("should return zero count when parking space has no reviews")
    void shouldReturnZeroCountWhenParkingSpaceHasNoReviews() {
        long count = reviewRepository.countByParkingSpaceId(99999L);

        assertThat(count).isZero();
    }
}
