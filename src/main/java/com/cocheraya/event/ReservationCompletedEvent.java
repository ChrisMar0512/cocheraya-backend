package com.cocheraya.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * Evento de dominio publicado cuando un conductor completa el checkout
 * de una reserva (estacionamiento finalizado).
 *
 * El {@link CocherayaEventListener} escucha este evento para enviar
 * el correo con el resumen de la sesión de estacionamiento.
 */
@Getter
public class ReservationCompletedEvent extends ApplicationEvent {

    private final Long reservationId;
    private final String driverEmail;
    private final String driverName;
    private final String parkingSpaceName;
    private final long durationMinutes;
    private final BigDecimal totalCharged;
    private final BigDecimal remainingBalance;

    /**
     * Crea una nueva instancia del evento de reserva completada.
     *
     * @param source           fuente del evento
     * @param reservationId    identificador de la reserva
     * @param driverEmail      correo del conductor
     * @param driverName       nombre del conductor
     * @param parkingSpaceName nombre de la cochera utilizada
     * @param durationMinutes  duración del estacionamiento en minutos
     * @param totalCharged     monto total cobrado
     * @param remainingBalance saldo restante en la wallet del conductor
     */
    public ReservationCompletedEvent(Object source, Long reservationId, String driverEmail,
                                     String driverName, String parkingSpaceName,
                                     long durationMinutes, BigDecimal totalCharged,
                                     BigDecimal remainingBalance) {
        super(source);
        this.reservationId = reservationId;
        this.driverEmail = driverEmail;
        this.driverName = driverName;
        this.parkingSpaceName = parkingSpaceName;
        this.durationMinutes = durationMinutes;
        this.totalCharged = totalCharged;
        this.remainingBalance = remainingBalance;
    }
}
