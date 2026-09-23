package com.cocheraya.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para una transacción de billetera.
 *
 * Incluye campos amigables para el frontend:
 * - typeLabel:     etiqueta en español ("Recarga", "Cobro", "Reembolso")
 * - amountDisplay: monto con signo ("+S/. 50.00" o "-S/. 25.00")
 */
@Data
public class WalletTransactionResponse {

    private Long id;

    
    private String typeLabel;

    
    private String amountDisplay;

    
    private BigDecimal amount;

    /** Saldo de la billetera después de esta transacción (para auditoría) */
    private BigDecimal balanceAfter;

    
    private String description;

    /** Momento en que se realizó la transacción */
    private LocalDateTime createdAt;

    
    private Long reservationId;

    private String parkingSpaceName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMinutes;
}
