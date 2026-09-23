package com.cocheraya.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento enviado vía WebSocket al topic /topic/parking-updates cuando
 * el estado de una cochera cambia (por reserva, expiración, cancelación, etc.).
 *
 * Los clientes suscritos a este topic reciben actualizaciones en tiempo real
 * para refrescar el mapa de cocheras disponibles sin hacer polling.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParkingUpdateEvent {

    
    private Long parkingSpaceId;

    
    private String newStatus;
}
