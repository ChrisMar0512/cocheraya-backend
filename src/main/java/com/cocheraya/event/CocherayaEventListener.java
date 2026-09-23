package com.cocheraya.event;

import com.cocheraya.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener centralizado de eventos de dominio en CocheraYa.
 *
 * Escucha los eventos publicados por los servicios de negocio y delega
 * las acciones correspondientes (envío de emails, logging, etc.).
 * Esto desacopla la lógica de notificación del flujo transaccional principal.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CocherayaEventListener {

    private final EmailService emailService;

    /**
     * Maneja el evento de registro de usuario enviando el correo de bienvenida.
     *
     * @param event evento con los datos del usuario registrado
     */
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Evento recibido: usuario registrado [id={}, email={}, role={}]",
                event.getUserId(), event.getEmail(), event.getRole());
        emailService.sendWelcomeEmail(event.getEmail(), event.getName(), event.getRole().name());
    }

    /**
     * Maneja el evento de reserva completada enviando el resumen por correo.
     *
     * @param event evento con los datos del checkout realizado
     */
    @EventListener
    public void handleReservationCompleted(ReservationCompletedEvent event) {
        log.info("Evento recibido: reserva completada [id={}, conductor={}]",
                event.getReservationId(), event.getDriverEmail());
        emailService.sendCheckoutSummary(
                event.getDriverEmail(),
                event.getDriverName(),
                event.getParkingSpaceName(),
                event.getDurationMinutes(),
                event.getTotalCharged(),
                event.getRemainingBalance()
        );
    }

    
    @EventListener
    public void handleReservationExpired(ReservationExpiredEvent event) {
        log.warn("Evento recibido: reserva expirada [reservationId={}, parkingSpaceId={}]",
                event.getReservationId(), event.getParkingSpaceId());
    }
}
