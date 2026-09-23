package com.cocheraya.service;

import com.cocheraya.dto.CheckInResponse;
import com.cocheraya.dto.CheckOutResponse;
import com.cocheraya.dto.QRResponse;
import com.cocheraya.dto.WalletResponse;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.entity.QRCode;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.entity.User;
import com.cocheraya.exception.InvalidOperationException;
import com.cocheraya.exception.QRCodeExpiredException;
import com.cocheraya.repository.QRCodeRepository;
import com.cocheraya.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInOutServiceTest {

    @Mock
    private QRCodeRepository qrCodeRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private CheckInOutService checkInOutService;

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
        parkingSpace.setAddress("Av. Principal 123");
        parkingSpace.setPricePerHour(new BigDecimal("6.00"));
        parkingSpace.setStatus(ParkingSpaceStatus.RESERVED);
        parkingSpace.setHost(host);

        reservation = new Reservation();
        reservation.setId(100L);
        reservation.setDriver(driver);
        reservation.setParkingSpace(parkingSpace);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));
    }

    // ==================== generateQR ====================

    @Test
    @DisplayName("shouldGenerateQRSuccessfully")
    void shouldGenerateQRSuccessfully() {
        // Arrange
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(qrCodeRepository.findByReservationId(100L)).thenReturn(Optional.empty());
        when(qrCodeRepository.save(any(QRCode.class))).thenAnswer(invocation -> {
            QRCode qr = invocation.getArgument(0);
            qr.setId(1L);
            return qr;
        });

        // Act
        QRResponse response = checkInOutService.generateQR(100L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getCode()).isNotBlank();
        assertThat(response.getQrImageBase64()).isNotBlank();
        assertThat(response.getReservationId()).isEqualTo(100L);
        verify(qrCodeRepository).save(any(QRCode.class));
    }

    @Test
    @DisplayName("shouldThrowInvalidOperationExceptionWhenReservationNotPendingOrActive")
    void shouldThrowInvalidOperationExceptionWhenReservationNotPendingOrActive() {
        // Arrange
        reservation.setStatus(ReservationStatus.FINISHED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> checkInOutService.generateQR(100L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pendientes o activas");

        verify(qrCodeRepository, never()).save(any());
    }

    // ==================== checkIn ====================

    @Test
    @DisplayName("shouldCheckInSuccessfully")
    void shouldCheckInSuccessfully() {
        // Arrange
        QRCode qrCode = new QRCode();
        qrCode.setId(1L);
        qrCode.setCode("test-uuid-code");
        qrCode.setReservation(reservation);
        qrCode.setUsedForCheckin(false);
        qrCode.setUsedForCheckout(false);
        qrCode.setExpiresAt(LocalDateTime.now().plusMinutes(20));

        when(qrCodeRepository.findByCode("test-uuid-code")).thenReturn(Optional.of(qrCode));
        when(qrCodeRepository.save(any(QRCode.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        CheckInResponse response = checkInOutService.checkIn("test-uuid-code");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Check-in exitoso");
        assertThat(response.getTimestamp()).isNotNull();

        assertThat(qrCode.getUsedForCheckin()).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getStartTime()).isNotNull();
        assertThat(parkingSpace.getStatus()).isEqualTo(ParkingSpaceStatus.OCCUPIED);

        verify(qrCodeRepository).save(qrCode);
        verify(reservationRepository).save(reservation);
        verify(messagingTemplate).convertAndSend(eq("/topic/parking-updates"), any(Object.class));
    }

    @Test
    @DisplayName("shouldThrowQRCodeExpiredExceptionWhenQRExpired")
    void shouldThrowQRCodeExpiredExceptionWhenQRExpired() {
        // Arrange
        QRCode qrCode = new QRCode();
        qrCode.setId(1L);
        qrCode.setCode("expired-code");
        qrCode.setReservation(reservation);
        qrCode.setUsedForCheckin(false);
        qrCode.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // already expired

        when(qrCodeRepository.findByCode("expired-code")).thenReturn(Optional.of(qrCode));

        // Act & Assert
        assertThatThrownBy(() -> checkInOutService.checkIn("expired-code"))
                .isInstanceOf(QRCodeExpiredException.class)
                .hasMessageContaining("expirado");

        verify(reservationRepository, never()).save(any());
    }

    // ==================== checkOut ====================

    @Test
    @DisplayName("shouldCheckOutAndChargeWallet")
    void shouldCheckOutAndChargeWallet() {
        // Arrange
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setStartTime(LocalDateTime.now().minusMinutes(30));

        QRCode qrCode = new QRCode();
        qrCode.setId(1L);
        qrCode.setCode("checkout-code");
        qrCode.setReservation(reservation);
        qrCode.setUsedForCheckin(true);
        qrCode.setUsedForCheckout(false);
        qrCode.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        WalletResponse walletResponse = new WalletResponse();
        walletResponse.setUserId(1L);
        walletResponse.setBalance(new BigDecimal("97.00"));

        when(qrCodeRepository.findByCode("checkout-code")).thenReturn(Optional.of(qrCode));
        when(walletService.charge(eq(1L), any(BigDecimal.class), eq(reservation)))
                .thenReturn(walletResponse);
        when(qrCodeRepository.save(any(QRCode.class))).thenAnswer(i -> i.getArgument(0));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        CheckOutResponse response = checkInOutService.checkOut("checkout-code");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDurationMinutes()).isGreaterThanOrEqualTo(30);
        assertThat(response.getTotalCharged()).isNotNull();
        assertThat(response.getRemainingBalance()).isEqualByComparingTo(new BigDecimal("97.00"));
        assertThat(response.getParkingSpaceName()).isEqualTo("Cochera Centro");
        assertThat(response.getReservationId()).isEqualTo(100L);

        assertThat(qrCode.getUsedForCheckout()).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FINISHED);
        assertThat(reservation.getEndTime()).isNotNull();
        assertThat(parkingSpace.getStatus()).isEqualTo(ParkingSpaceStatus.AVAILABLE);

        verify(walletService).charge(eq(1L), any(BigDecimal.class), eq(reservation));
        verify(walletService).creditHost(eq(2L), any(BigDecimal.class), eq(reservation));
        verify(messagingTemplate).convertAndSend(eq("/topic/parking-updates"), any(Object.class));
    }
}
