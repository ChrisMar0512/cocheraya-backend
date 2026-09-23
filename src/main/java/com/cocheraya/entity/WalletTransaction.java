package com.cocheraya.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro inmutable de una operación financiera en la billetera de un usuario.
 *
 * AUDITORÍA COMPLETA — balanceAfter:
 *   El campo balanceAfter almacena el saldo de la billetera DESPUÉS de aplicar esta
 *   transacción. Este campo es CRÍTICO porque permite reconstruir el historial completo
 *   de saldo sin depender del campo balance actual del Wallet.
 *
 *   ¿Por qué es necesario? Si un bug, race condition o error en el código corrompe
 *   el campo wallet.balance (por ejemplo, un charge que no se registra o un topUp
 *   duplicado), se puede auditar transacción por transacción para encontrar exactamente
 *   dónde diverge el saldo esperado del saldo real:
 *
 *     Transacción 1: TOPUP   +100.00  → balanceAfter = 100.00  ✓
 *     Transacción 2: CHARGE   -25.00  → balanceAfter =  75.00  ✓
 *     Transacción 3: CHARGE   -30.00  → balanceAfter =  45.00  ✓
 *     Transacción 4: TOPUP    +50.00  → balanceAfter =  95.00  ← ¡Aquí debería ser 95
 *                                                                     pero wallet.balance dice 145!
 *
 *   Sin balanceAfter, encontrar este tipo de errores requeriría recalcular la suma de
 *   todas las transacciones desde cero, lo cual es lento y no muestra el punto exacto
 *   de la divergencia.
 *
 * ÍNDICES:
 *   - idx_wallet_tx_wallet_id:  optimiza la consulta de historial por wallet (paginado).
 *   - idx_wallet_tx_created_at: optimiza la ordenación cronológica del historial.
 */
@Entity
@Table(
    name = "wallet_transaction",
    indexes = {
        @Index(name = "idx_wallet_tx_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_wallet_tx_created_at", columnList = "created_at")
    }
)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Billetera a la que pertenece esta transacción */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Monto de la transacción en soles (siempre positivo, el signo lo define el tipo) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Saldo de la billetera DESPUÉS de aplicar esta transacción.
     * Ver comentario de clase sobre por qué este campo es crítico para auditoría.
     */
    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    /**
     * Reserva asociada a esta transacción.
     * Es null para transacciones TOPUP (no hay reserva vinculada).
     * Para CHARGE y REFUND siempre tiene la reserva que originó la operación.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Momento de creación de la transacción — inmutable */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    

    public enum TransactionType {
        
        TOPUP,
        
        CHARGE,
        
        REFUND
    }
}
