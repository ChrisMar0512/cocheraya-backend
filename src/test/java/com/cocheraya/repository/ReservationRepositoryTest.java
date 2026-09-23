package com.cocheraya.repository;

import com.cocheraya.config.TestContainersConfig;
import com.cocheraya.entity.ParkingSpace;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("ReservationRepository Tests")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User driver;
    private User host;
    private ParkingSpace parkingSpace;

    @BeforeEach
    void setUp() {
        host = new User();
        host.setEmail("res-host@cocheraya.com");
        host.setPassword("encoded-password");
        host.setName("Res Host");
        host.setRole(User.Role.HOST);
        host.setEnabled(true);
        entityManager.persistAndFlush(host);

        driver = new User();
        driver.setEmail("res-driver@cocheraya.com");
        driver.setPassword("encoded-password");
        driver.setName("Res Driver");
        driver.setRole(User.Role.DRIVER);
        driver.setEnabled(true);
        entityManager.persistAndFlush(driver);

        parkingSpace = new ParkingSpace();
        parkingSpace.setHost(host);
        parkingSpace.setTitle("Cochera Centro Lima");
        parkingSpace.setAddress("Av. Arequipa 123, Miraflores");
        parkingSpace.setPricePerHour(new BigDecimal("5.00"));
        parkingSpace.setStatus(ParkingSpace.ParkingSpaceStatus.AVAILABLE);
        // location (PostGIS Point) left null — spatial queries are skipped
        entityManager.persistAndFlush(parkingSpace);

        // Create reservations with different statuses
        Reservation res1 = new Reservation();
        res1.setDriver(driver);
        res1.setParkingSpace(parkingSpace);
        res1.setStatus(Reservation.ReservationStatus.PENDING);
        res1.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        entityManager.persistAndFlush(res1);

        Reservation res2 = new Reservation();
        res2.setDriver(driver);
        res2.setParkingSpace(parkingSpace);
        res2.setStatus(Reservation.ReservationStatus.FINISHED);
        res2.setStartTime(LocalDateTime.now().minusHours(2));
        res2.setEndTime(LocalDateTime.now().minusHours(1));
        entityManager.persistAndFlush(res2);

        Reservation res3 = new Reservation();
        res3.setDriver(driver);
        res3.setParkingSpace(parkingSpace);
        res3.setStatus(Reservation.ReservationStatus.FINISHED);
        res3.setStartTime(LocalDateTime.now().minusHours(5));
        res3.setEndTime(LocalDateTime.now().minusHours(4));
        entityManager.persistAndFlush(res3);
    }

    @Test
    @DisplayName("should return reservations ordered by createdAt desc when driver has reservations")
    void shouldReturnReservationsOrderedByCreatedAtDescWhenDriverHasReservations() {
        List<Reservation> reservations = reservationRepository
                .findByDriverIdOrderByCreatedAtDesc(driver.getId());

        assertThat(reservations).hasSize(3);
        // Verify descending order
        for (int i = 0; i < reservations.size() - 1; i++) {
            assertThat(reservations.get(i).getCreatedAt())
                    .isAfterOrEqualTo(reservations.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("should return empty list when driver has no reservations")
    void shouldReturnEmptyListWhenDriverHasNoReservations() {
        List<Reservation> reservations = reservationRepository
                .findByDriverIdOrderByCreatedAtDesc(99999L);

        assertThat(reservations).isEmpty();
    }

    @Test
    @DisplayName("should return reservations when searching by parking space id")
    void shouldReturnReservationsWhenSearchingByParkingSpaceId() {
        List<Reservation> reservations = reservationRepository
                .findByParkingSpaceId(parkingSpace.getId());

        assertThat(reservations).hasSize(3);
        assertThat(reservations)
                .allMatch(r -> r.getParkingSpace().getId().equals(parkingSpace.getId()));
    }

    @Test
    @DisplayName("should return empty list when parking space has no reservations")
    void shouldReturnEmptyListWhenParkingSpaceHasNoReservations() {
        List<Reservation> reservations = reservationRepository.findByParkingSpaceId(99999L);

        assertThat(reservations).isEmpty();
    }

    @Test
    @DisplayName("should return correct count when filtering by parking space id and status")
    void shouldReturnCorrectCountWhenFilteringByParkingSpaceIdAndStatus() {
        List<Reservation> all = reservationRepository.findByParkingSpaceId(parkingSpace.getId());
        long finishedCount = all.stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.FINISHED)
                .count();
        long pendingCount = all.stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.PENDING)
                .count();

        assertThat(finishedCount).isEqualTo(2);
        assertThat(pendingCount).isEqualTo(1);
    }

    @Test
    @DisplayName("should return all reservations for driver regardless of status")
    void shouldReturnAllReservationsForDriverRegardlessOfStatus() {
        List<Reservation> reservations = reservationRepository.findByDriverId(driver.getId());

        assertThat(reservations).hasSize(3);
        assertThat(reservations)
                .extracting(Reservation::getStatus)
                .containsExactlyInAnyOrder(
                        Reservation.ReservationStatus.PENDING,
                        Reservation.ReservationStatus.FINISHED,
                        Reservation.ReservationStatus.FINISHED
                );
    }
}
