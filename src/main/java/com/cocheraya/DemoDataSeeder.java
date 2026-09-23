package com.cocheraya;

import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.entity.Review;
import com.cocheraya.entity.User;
import com.cocheraya.repository.ParkingSpaceRepository;
import com.cocheraya.repository.ReservationRepository;
import com.cocheraya.repository.ReviewRepository;
import com.cocheraya.repository.UserRepository;
import com.cocheraya.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class DemoDataSeeder implements CommandLineRunner {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ReviewRepository reviewRepository;
    private final WalletService walletService;

    @Override
    public void run(String... args) {
        if (reservationRepository.count() > 0) {
            log.info("Reservas ya existen — omitiendo DemoDataSeeder.");
            return;
        }

        log.info("Creando datos de demostración (Reserva finalizada y Review)...");

        User driver = userRepository.findByEmail("seeded_driver@cocheraya.com").orElse(null);
        if (driver == null) {
            log.warn("Usuario driver no encontrado, abortando DemoDataSeeder.");
            return;
        }

        List<ParkingSpace> spaces = parkingSpaceRepository.findAll();
        if (spaces.isEmpty()) {
            log.warn("No hay cocheras disponibles, abortando DemoDataSeeder.");
            return;
        }

        
        ParkingSpace barrancoSpace = spaces.stream()
                .filter(s -> s.getTitle().contains("Barranco"))
                .findFirst()
                .orElse(spaces.get(0));

        Reservation reservation = new Reservation();
        reservation.setDriver(driver);
        reservation.setParkingSpace(barrancoSpace);
        reservation.setStatus(ReservationStatus.FINISHED);
        reservation.setReservedAt(LocalDateTime.now().minusHours(2));
        reservation.setStartTime(LocalDateTime.now().minusHours(1).minusMinutes(45));
        reservation.setEndTime(LocalDateTime.now().minusMinutes(30));
        
        Reservation savedReservation = reservationRepository.save(reservation);

        
        BigDecimal costoFicticio = new BigDecimal("8.50");
        try {
            walletService.charge(driver.getId(), costoFicticio, savedReservation);
        } catch (Exception e) {
            log.warn("No se pudo realizar el cobro de demostración: {}", e.getMessage());
        }

        
        Review review = new Review();
        review.setReservation(savedReservation);
        review.setReviewer(driver);
        review.setReviewee(barrancoSpace.getHost());
        review.setParkingSpace(barrancoSpace);
        review.setRating(5);
        review.setComment("Excelente lugar, muy seguro y de fácil acceso.");
        reviewRepository.save(review);

        log.info("✅ Datos de demostración (Reserva, Cobro, Review) creados correctamente.");
    }
}
