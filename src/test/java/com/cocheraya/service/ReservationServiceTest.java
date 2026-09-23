package com.cocheraya.service;

import com.cocheraya.dto.ReservationResponse;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.entity.User;
import com.cocheraya.exception.InvalidOperationException;
import com.cocheraya.exception.SpaceNotAvailableException;
import com.cocheraya.repository.ParkingSpaceRepository;
import com.cocheraya.repository.ReservationRepository;
import com.cocheraya.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private ReservationService reservationService;

    private User driver;
    private User host;
    private ParkingSpace parkingSpace;

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
        parkingSpace.setAddress("Av. Principal 123");
        parkingSpace.setPricePerHour(new BigDecimal("5.00"));
        parkingSpace.setStatus(ParkingSpaceStatus.AVAILABLE);
        parkingSpace.setHost(host);
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

    // ==================== createReservation ====================

    @Test
    @DisplayName("shouldCreateReservationWhenSpaceIsAvailable")
    void shouldCreateReservationWhenSpaceIsAvailable() {
        // Arrange
        mockAuthenticatedUser(driver);
        when(entityManager.find(ParkingSpace.class, 10L, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(parkingSpace);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(100L);
            r.setReservedAt(LocalDateTime.now());
            return r;
        });

        // Act
        ReservationResponse response = reservationService.createReservation(10L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getParkingSpace().getTitle()).isEqualTo("Cochera Centro");
        assertThat(response.getDriver().getEmail()).isEqualTo("driver@test.com");

        verify(entityManager).merge(parkingSpace);
        assertThat(parkingSpace.getStatus()).isEqualTo(ParkingSpaceStatus.RESERVED);
        verify(reservationRepository).save(any(Reservation.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/parking-updates"), any(Object.class));
    }

    @Test
    @DisplayName("shouldThrowSpaceNotAvailableExceptionWhenSpaceIsOccupied")
    void shouldThrowSpaceNotAvailableExceptionWhenSpaceIsOccupied() {
        // Arrange
        mockAuthenticatedUser(driver);
        parkingSpace.setStatus(ParkingSpaceStatus.RESERVED);
        when(entityManager.find(ParkingSpace.class, 10L, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(parkingSpace);

        // Act & Assert
        assertThatThrownBy(() -> reservationService.createReservation(10L))
                .isInstanceOf(SpaceNotAvailableException.class)
                .hasMessageContaining("no está disponible");

        verify(reservationRepository, never()).save(any());
    }

    // ==================== cancelReservation ====================

    @Test
    @DisplayName("shouldCancelReservationWhenStatusIsPending")
    void shouldCancelReservationWhenStatusIsPending() {
        // Arrange
        mockAuthenticatedUser(driver);

        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setDriver(driver);
        reservation.setParkingSpace(parkingSpace);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ReservationResponse response = reservationService.cancelReservation(100L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("EXPIRED");
        assertThat(parkingSpace.getStatus()).isEqualTo(ParkingSpaceStatus.AVAILABLE);
        verify(reservationRepository).save(reservation);
        verify(parkingSpaceRepository).save(parkingSpace);
        verify(messagingTemplate).convertAndSend(eq("/topic/parking-updates"), any(Object.class));
    }

    @Test
    @DisplayName("shouldThrowInvalidOperationExceptionWhenCancellingActiveReservation")
    void shouldThrowInvalidOperationExceptionWhenCancellingActiveReservation() {
        // Arrange
        mockAuthenticatedUser(driver);

        Reservation reservation = new Reservation();
        reservation.setId(100L);
        reservation.setDriver(driver);
        reservation.setParkingSpace(parkingSpace);
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setStartTime(LocalDateTime.now());

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> reservationService.cancelReservation(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("No se puede cancelar");

        verify(reservationRepository, never()).save(any());
        verify(parkingSpaceRepository, never()).save(any());
    }
}
