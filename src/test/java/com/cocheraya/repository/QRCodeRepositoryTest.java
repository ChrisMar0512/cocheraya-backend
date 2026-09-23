package com.cocheraya.repository;

import com.cocheraya.config.TestContainersConfig;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.QRCode;
import com.cocheraya.entity.Reservation;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("QRCodeRepository Tests")
class QRCodeRepositoryTest {

    @Autowired
    private QRCodeRepository qrCodeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Reservation reservation;
    private QRCode qrCode;
    private String qrUuid;

    @BeforeEach
    void setUp() {
        User host = new User();
        host.setEmail("qr-host@cocheraya.com");
        host.setPassword("encoded-password");
        host.setName("QR Host");
        host.setRole(User.Role.HOST);
        host.setEnabled(true);
        entityManager.persistAndFlush(host);

        User driver = new User();
        driver.setEmail("qr-driver@cocheraya.com");
        driver.setPassword("encoded-password");
        driver.setName("QR Driver");
        driver.setRole(User.Role.DRIVER);
        driver.setEnabled(true);
        entityManager.persistAndFlush(driver);

        ParkingSpace parkingSpace = new ParkingSpace();
        parkingSpace.setHost(host);
        parkingSpace.setTitle("Cochera QR Test");
        parkingSpace.setAddress("Jr. de la Unión 456, Lima");
        parkingSpace.setPricePerHour(new BigDecimal("7.50"));
        parkingSpace.setStatus(ParkingSpace.ParkingSpaceStatus.RESERVED);
        entityManager.persistAndFlush(parkingSpace);

        reservation = new Reservation();
        reservation.setDriver(driver);
        reservation.setParkingSpace(parkingSpace);
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        entityManager.persistAndFlush(reservation);

        qrUuid = UUID.randomUUID().toString();
        qrCode = new QRCode();
        qrCode.setReservation(reservation);
        qrCode.setCode(qrUuid);
        qrCode.setUsedForCheckin(false);
        qrCode.setUsedForCheckout(false);
        qrCode.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        entityManager.persistAndFlush(qrCode);
    }

    @Test
    @DisplayName("should return QR code when code exists")
    void shouldReturnQRCodeWhenCodeExists() {
        Optional<QRCode> found = qrCodeRepository.findByCode(qrUuid);

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo(qrUuid);
        assertThat(found.get().getUsedForCheckin()).isFalse();
        assertThat(found.get().getUsedForCheckout()).isFalse();
        assertThat(found.get().getReservation().getId()).isEqualTo(reservation.getId());
    }

    @Test
    @DisplayName("should return empty when code does not exist")
    void shouldReturnEmptyWhenCodeDoesNotExist() {
        Optional<QRCode> found = qrCodeRepository.findByCode("nonexistent-uuid-code");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should return QR code when reservation id exists")
    void shouldReturnQRCodeWhenReservationIdExists() {
        Optional<QRCode> found = qrCodeRepository.findByReservationId(reservation.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo(qrUuid);
        assertThat(found.get().getReservation().getId()).isEqualTo(reservation.getId());
    }

    @Test
    @DisplayName("should return empty when reservation id does not exist")
    void shouldReturnEmptyWhenReservationIdDoesNotExist() {
        Optional<QRCode> found = qrCodeRepository.findByReservationId(99999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should return QR code with correct expiration time")
    void shouldReturnQRCodeWithCorrectExpirationTime() {
        Optional<QRCode> found = qrCodeRepository.findByCode(qrUuid);

        assertThat(found).isPresent();
        assertThat(found.get().getExpiresAt()).isAfter(LocalDateTime.now());
    }
}
