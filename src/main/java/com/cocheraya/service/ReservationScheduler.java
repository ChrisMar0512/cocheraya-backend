package com.cocheraya.service;

import com.cocheraya.dto.ParkingUpdateEvent;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.ParkingSpace.ParkingSpaceStatus;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.Reservation.ReservationStatus;
import com.cocheraya.entity.User;
import com.cocheraya.repository.ParkingSpaceRepository;
import com.cocheraya.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler para la gestión automática de reservas expiradas y notificaciones.
 *
 * DISEÑO: los dos métodos @Scheduled están separados intencionalmente para que
 * un fallo en el envío de notificaciones Firebase (red, cuota, token inválido)
 * NO interrumpa ni retrase la limpieza de reservas expiradas que es crítica
 * para el negocio. Si estuvieran juntos, una excepción en Firebase podría hacer
 * rollback de la transacción y dejar cocheras bloqueadas indefinidamente.
 *
 * checkExpiredReservations: limpieza crítica — libera cocheras y marca EXPIRED.
 * sendExpirationWarnings:   cortesía UX — avisa al DRIVER que le quedan 5 min.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationRepository reservationRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final FirebaseNotificationService firebaseNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    

    /**
     * Revisa cada 60 segundos si hay reservas PENDING cuyo tiempo de expiración
     * (expiresAt = reservedAt + 15 min) ya pasó. Por cada una:
     *   1. Cambia el status de la Reservation a EXPIRED.
     *   2. Devuelve el ParkingSpace a AVAILABLE.
     *   3. Emite evento WebSocket para que los clientes actualicen el mapa.
     *
     * fixedRate = 60000 ms = 1 minuto — en el peor caso una cochera queda
     * bloqueada hasta ~1 minuto después de que la reserva realmente expiró.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expired = reservationRepository.findExpiredPendingReservations(now);

        if (expired.isEmpty()) {
            return; 
        }

        for (Reservation reservation : expired) {
            
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            
            ParkingSpace space = reservation.getParkingSpace();
            space.setStatus(ParkingSpaceStatus.AVAILABLE);
            parkingSpaceRepository.save(space);

            // Notificar clientes WebSocket
            ParkingUpdateEvent event = new ParkingUpdateEvent(
                    space.getId(),
                    ParkingSpaceStatus.AVAILABLE.name()
            );
            messagingTemplate.convertAndSend("/topic/parking-updates", event);
        }

        log.info("⏰ Scheduler: {} reservas expiradas procesadas y cocheras liberadas", expired.size());
    }

    // ==================== Notificaciones de Expiración Próxima ====================

    /**
     * Revisa cada 60 segundos si hay reservas PENDING que expirarán en los
     * próximos 5 minutos. Envía una notificación push vía Firebase para que
     * el DRIVER sepa que debe hacer check-in pronto o perderá la reserva.
     *
     * NOTA: este método está separado de checkExpiredReservations para que
     * un fallo en Firebase (token inválido, error de red, cuota excedida)
     * no afecte la limpieza de reservas expiradas que es operación crítica.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional(readOnly = true)
    public void sendExpirationWarnings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveMinutesLater = now.plusMinutes(5);

        List<Reservation> aboutToExpire =
                reservationRepository.findPendingReservationsExpiringIn5Minutes(now, fiveMinutesLater);

        if (aboutToExpire.isEmpty()) {
            return;
        }

        for (Reservation reservation : aboutToExpire) {
            User driver = reservation.getDriver();
            String fcmToken = driver.getFcmToken();

            if (fcmToken != null && !fcmToken.isBlank()) {
                String parkingTitle = reservation.getParkingSpace().getTitle();
                firebaseNotificationService.sendReservationExpiringNotification(
                        fcmToken, parkingTitle
                );
                log.info("Notificación de expiración enviada: driver={}, cochera={}",
                        driver.getEmail(), parkingTitle);
            }
        }

        log.info("📱 Scheduler: {} notificaciones de expiración enviadas", aboutToExpire.size());
    }
}
