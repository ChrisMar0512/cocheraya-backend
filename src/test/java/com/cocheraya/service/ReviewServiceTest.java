package com.cocheraya.service;

import com.cocheraya.dto.CreateReviewRequest;
import com.cocheraya.dto.ReviewResponse;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.entity.Review;
import com.cocheraya.entity.User;
import com.cocheraya.exception.DuplicateResourceException;
import com.cocheraya.exception.InvalidOperationException;
import com.cocheraya.exception.UnauthorizedOperationException;
import com.cocheraya.repository.ReservationRepository;
import com.cocheraya.repository.ReviewRepository;
import com.cocheraya.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private ReviewService reviewService;

    private User driver;
    private User host;
    private ParkingSpace parkingSpace;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        driver = new User();
        driver.setId(1L);
        driver.setEmail("driver@test.com");
        driver.setName("Test Driver");
        driver.setRole(User.Role.DRIVER);

        host = new User();
        host.setId(2L);
        host.setEmail("host@test.com");
        host.setName("Test Host");
        host.setRole(User.Role.HOST);

        parkingSpace = new ParkingSpace();
        parkingSpace.setId(10L);
        parkingSpace.setTitle("Cochera Centro");
        parkingSpace.setPricePerHour(new BigDecimal("5.00"));
        parkingSpace.setHost(host);

        reservation = new Reservation();
        reservation.setId(100L);
        reservation.setDriver(driver);
        reservation.setParkingSpace(parkingSpace);
        reservation.setStatus(ReservationStatus.FINISHED);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getEmail());
        SecurityContextHolder.setContext(securityContext);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    // ==================== createReview ====================

    @Test
    @DisplayName("shouldCreateReviewSuccessfully")
    void shouldCreateReviewSuccessfully() {
        // Arrange
        mockAuthenticatedUser(driver);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setReservationId(100L);
        request.setRating(5);
        request.setComment("Excelente cochera");

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.existsByReservationIdAndReviewerId(100L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });

        // Act
        ReviewResponse response = reviewService.createReview(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Excelente cochera");
        assertThat(response.getReviewerName()).isEqualTo("Test Driver");
        assertThat(response.getRevieweeName()).isEqualTo("Test Host");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("shouldThrowInvalidOperationExceptionWhenReservationNotFinished")
    void shouldThrowInvalidOperationExceptionWhenReservationNotFinished() {
        // Arrange
        mockAuthenticatedUser(driver);
        reservation.setStatus(ReservationStatus.ACTIVE);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setReservationId(100L);
        request.setRating(4);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("completadas");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowDuplicateResourceExceptionWhenAlreadyReviewed")
    void shouldThrowDuplicateResourceExceptionWhenAlreadyReviewed() {
        // Arrange
        mockAuthenticatedUser(driver);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setReservationId(100L);
        request.setRating(4);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.existsByReservationIdAndReviewerId(100L, 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Ya has enviado");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("shouldThrowUnauthorizedOperationExceptionWhenNotParticipant")
    void shouldThrowUnauthorizedOperationExceptionWhenNotParticipant() {
        // Arrange
        User stranger = new User();
        stranger.setId(99L);
        stranger.setEmail("stranger@test.com");
        stranger.setName("Stranger");
        stranger.setRole(User.Role.DRIVER);

        mockAuthenticatedUser(stranger);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setReservationId(100L);
        request.setRating(3);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reviewRepository.existsByReservationIdAndReviewerId(100L, 99L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(request))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("No tienes permiso");

        verify(reviewRepository, never()).save(any());
    }
}
