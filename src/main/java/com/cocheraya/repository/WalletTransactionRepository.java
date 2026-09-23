package com.cocheraya.repository;

import com.cocheraya.entity.WalletTransaction;
import com.cocheraya.entity.WalletTransaction.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Repositorio JPA para la entidad WalletTransaction.
 *
 * Incluye queries para:
 * - Historial paginado de transacciones (cronológico descendente)
 * - Historial filtrado por tipo de transacción
 * - Suma total de CHARGE por wallet (ingresos)
 * - Suma de CHARGE por rango de fecha (ingresos mensuales)
 * - Dashboard de ganancias del host
 */
@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    

    
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Retorna el historial filtrado por tipo de transacción (TOPUP, CHARGE, REFUND).
     *
     * @param walletId ID de la billetera
     * @param type     tipo de transacción a filtrar
     * @param pageable configuración de paginación
     * @return página de transacciones del tipo indicado
     */
    Page<WalletTransaction> findByWalletIdAndTypeOrderByCreatedAtDesc(
            Long walletId,
            TransactionType type,
            Pageable pageable
    );

    

    
    @Query("""
        SELECT COALESCE(SUM(wt.amount), 0)
        FROM WalletTransaction wt
        WHERE wt.wallet.id = :walletId
          AND wt.type = com.cocheraya.entity.WalletTransaction$TransactionType.CHARGE
        """)
    BigDecimal sumChargesByWalletId(@Param("walletId") Long walletId);

    
    @Query("""
        SELECT COALESCE(SUM(wt.amount), 0)
        FROM WalletTransaction wt
        WHERE wt.wallet.id = :walletId
          AND wt.type = com.cocheraya.entity.WalletTransaction$TransactionType.CHARGE
          AND wt.createdAt >= :from
          AND wt.createdAt < :to
        """)
    BigDecimal sumChargesByWalletIdAndDateRange(
            @Param("walletId") Long walletId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    
    long countByWalletId(Long walletId);

    

    
    @Query("""
        SELECT COALESCE(SUM(wt.amount), 0)
        FROM WalletTransaction wt
        WHERE wt.reservation.parkingSpace.host.id = :hostId
          AND wt.wallet.user.id = :hostId
          AND wt.type = com.cocheraya.entity.WalletTransaction$TransactionType.CHARGE
        """)
    BigDecimal sumEarningsByHostId(@Param("hostId") Long hostId);
}
