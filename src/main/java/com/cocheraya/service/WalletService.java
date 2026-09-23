package com.cocheraya.service;

import com.cocheraya.dto.EarningsSummaryResponse;
import com.cocheraya.dto.WalletResponse;
import com.cocheraya.dto.WalletTransactionResponse;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.User;
import com.cocheraya.entity.Wallet;
import com.cocheraya.entity.WalletTransaction;
import com.cocheraya.entity.WalletTransaction.TransactionType;
import com.cocheraya.exception.InsufficientBalanceException;
import com.cocheraya.exception.ResourceNotFoundException;
import com.cocheraya.repository.WalletRepository;
import com.cocheraya.repository.WalletTransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Servicio de billetera virtual de CocheraYa.
 *
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │  ⚠️  REGLA DE ORO — NO VIOLAR BAJO NINGUNA CIRCUNSTANCIA:                 │
 * │                                                                            │
 * │  Ningún método puede modificar wallet.balance sin crear y guardar un       │
 * │  WalletTransaction en la MISMA @Transactional.                             │
 * │                                                                            │
 * │  Si se modifica el balance sin registrar la transacción, se rompe la       │
 * │  auditoría completa del sistema: los valores de balanceAfter dejan de      │
 * │  ser confiables, el historial queda inconsistente y es imposible           │
 * │  reconstruir cómo se llegó al saldo actual.                                │
 * │                                                                            │
 * │  El campo balanceAfter en WalletTransaction existe precisamente para       │
 * │  poder auditar transacción por transacción y detectar bugs o fraudes.      │
 * └──────────────────────────────────────────────────────────────────────────────┘
 *
 * OPERACIONES:
 *   - initializeWallet: crear billetera con saldo 0 (registro de usuario)
 *   - topUp:   recargar saldo (+)
 *   - charge:  cobrar por reserva (-) con PESSIMISTIC_WRITE
 *   - refund:  reembolsar por cancelación (+)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService implements IWalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final EntityManager entityManager;
    private final ModelMapper modelMapper;

    

    
    @Transactional
    public Wallet initializeWallet(User user) {
        
        if (walletRepository.findByUserId(user.getId()).isPresent()) {
            log.warn("El usuario {} ya tiene billetera — omitiendo creación.", user.getEmail());
            return walletRepository.findByUserId(user.getId()).get();
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);

        Wallet saved = walletRepository.save(wallet);
        log.info("Billetera creada para usuario: {} — saldo inicial S/. 0.00", user.getEmail());
        return saved;
    }

    

    
    @Transactional
    public WalletResponse topUp(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El monto de recarga debe ser mayor a 0");
        }

        Wallet wallet = findWalletByUserIdOrThrow(userId);

        
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // Registrar transacción (REGLA DE ORO: siempre junto al cambio de balance)
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.TOPUP);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setDescription("Recarga de saldo");
        walletTransactionRepository.save(tx);

        log.info("Recarga exitosa: usuario={}, monto=S/. {}, nuevoSaldo=S/. {}",
                userId, amount, newBalance);
        return mapToWalletResponse(wallet);
    }

    
    @Transactional
    public void topUp(User user, BigDecimal amount) {
        topUp(user.getId(), amount);
    }

    

    /**
     * Cobra al usuario por una reserva completada.
     *
     * SEGURIDAD ANTE CONCURRENCIA (PESSIMISTIC_WRITE):
     *
     * ¿Por qué se necesita PESSIMISTIC_WRITE aquí?
     * Si el mismo usuario hace check-out desde dos dispositivos simultáneamente
     * (o si un bug dispara el cobro dos veces), ambos threads podrían:
     *
     *   Thread A: lee wallet.balance = S/. 100  ✓
     *   Thread B: lee wallet.balance = S/. 100  ✓  (¡ANTES de que A persista!)
     *   Thread A: resta S/. 30 → balance = S/. 70 → guarda
     *   Thread B: resta S/. 30 → balance = S/. 70 → guarda  ← ¡COBRÓ SOLO UNA VEZ!
     *
     * En este caso el usuario pagó S/. 30 pero el balance solo bajó S/. 30 en vez de S/. 60.
     * Con PESSIMISTIC_WRITE, Thread B espera a que Thread A termine y re-lee balance = S/. 70,
     * cobrando correctamente a S/. 40.
     *
     * @param userId      ID del usuario a cobrar
     * @param amount      monto a cobrar
     * @param reservation reserva que originó el cobro
     * @return estado actualizado de la billetera
     * @throws IllegalStateException si el saldo es insuficiente
     */
    @Transactional
    public WalletResponse charge(Long userId, BigDecimal amount, Reservation reservation) {
        // PESSIMISTIC_WRITE → SELECT ... FOR UPDATE en PostgreSQL
        Wallet wallet = entityManager.find(
                Wallet.class,
                findWalletByUserIdOrThrow(userId).getId(),
                LockModeType.PESSIMISTIC_WRITE
        );

        
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo insuficiente para completar el pago. " +
                    "Saldo actual: S/. " + wallet.getBalance() + ", monto requerido: S/. " + amount
            );
        }

        
        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        entityManager.merge(wallet);

        // Registrar transacción (REGLA DE ORO)
        String parkingTitle = reservation.getParkingSpace().getTitle();
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.CHARGE);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReservation(reservation);
        tx.setDescription("Reserva en " + parkingTitle);
        walletTransactionRepository.save(tx);

        log.info("Cobro exitoso: usuario={}, monto=S/. {}, cochera={}, nuevoSaldo=S/. {}",
                userId, amount, parkingTitle, newBalance);
        return mapToWalletResponse(wallet);
    }

    

    
    @Transactional
    public WalletResponse refund(Long userId, BigDecimal amount, Reservation reservation) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);

        
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        // Registrar transacción (REGLA DE ORO)
        String parkingTitle = reservation.getParkingSpace().getTitle();
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.REFUND);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReservation(reservation);
        tx.setDescription("Reembolso por cancelación en " + parkingTitle);
        walletTransactionRepository.save(tx);

        log.info("Reembolso exitoso: usuario={}, monto=S/. {}, cochera={}, nuevoSaldo=S/. {}",
                userId, amount, parkingTitle, newBalance);
        return mapToWalletResponse(wallet);
    }

    @Transactional
    public void creditHost(Long hostId, BigDecimal amount, Reservation reservation) {
        Wallet wallet = findWalletByUserIdOrThrow(hostId);
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        String parkingTitle = reservation.getParkingSpace().getTitle();
        String driverName = reservation.getDriver().getName();
        WalletTransaction tx = new WalletTransaction();
        tx.setWallet(wallet);
        tx.setType(TransactionType.CHARGE);
        tx.setAmount(amount);
        tx.setBalanceAfter(newBalance);
        tx.setReservation(reservation);
        tx.setDescription("Alquiler de " + parkingTitle + " (por " + driverName + ")");
        walletTransactionRepository.save(tx);

        log.info("Crédito a host exitoso: host={}, monto=S/. {}, cochera={}, nuevoSaldo=S/. {}",
                hostId, amount, parkingTitle, newBalance);
    }

    

    
    @Transactional(readOnly = true)
    public WalletResponse getBalance(Long userId) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        return mapToWalletResponse(wallet);
    }

    
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionHistory(Long userId, Pageable pageable) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        return walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::mapToTransactionResponse);
    }

    /**
     * Retorna el historial filtrado por tipo de transacción.
     *
     * @param userId   ID del usuario
     * @param type     tipo de transacción (TOPUP, CHARGE, REFUND)
     * @param pageable configuración de paginación
     * @return página de transacciones del tipo indicado
     */
    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> getTransactionHistoryByType(
            Long userId, String type, Pageable pageable
    ) {
        Wallet wallet = findWalletByUserIdOrThrow(userId);
        TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
        return walletTransactionRepository
                .findByWalletIdAndTypeOrderByCreatedAtDesc(wallet.getId(), transactionType, pageable)
                .map(this::mapToTransactionResponse);
    }

    
    @Transactional(readOnly = true)
    public EarningsSummaryResponse getEarningsSummary(Long hostId) {
        Wallet wallet = findWalletByUserIdOrThrow(hostId);

        EarningsSummaryResponse summary = new EarningsSummaryResponse();

        
        summary.setTotalEarned(
                walletTransactionRepository.sumChargesByWalletId(wallet.getId())
        );

        
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        summary.setEarningsThisMonth(
                walletTransactionRepository.sumChargesByWalletIdAndDateRange(
                        wallet.getId(), monthStart, monthEnd
                )
        );

        
        summary.setTransactionCount(
                walletTransactionRepository.countByWalletId(wallet.getId())
        );

        log.info("Resumen de ganancias generado para host id={}", hostId);
        return summary;
    }

    

    
    private Wallet findWalletByUserIdOrThrow(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billetera no encontrada para el usuario con id: " + userId
                ));
    }

    
    private WalletResponse mapToWalletResponse(Wallet wallet) {
        WalletResponse response = modelMapper.map(wallet, WalletResponse.class);
        response.setUserId(wallet.getUser().getId());
        return response;
    }

    
    private WalletTransactionResponse mapToTransactionResponse(WalletTransaction tx) {
        WalletTransactionResponse response = modelMapper.map(tx, WalletTransactionResponse.class);

        
        switch (tx.getType()) {
            case TOPUP -> {
                response.setTypeLabel("Recarga");
                response.setAmountDisplay("+S/. " + tx.getAmount());
            }
            case CHARGE -> {
                if (tx.getWallet().getUser().getRole() == com.cocheraya.entity.User.Role.HOST) {
                    response.setTypeLabel("Ingreso por alquiler");
                    response.setAmountDisplay("+S/. " + tx.getAmount());
                } else {
                    response.setTypeLabel("Cobro por reserva");
                    response.setAmountDisplay("-S/. " + tx.getAmount());
                }
            }
            case REFUND -> {
                response.setTypeLabel("Reembolso");
                response.setAmountDisplay("+S/. " + tx.getAmount());
            }
        }

        
        if (tx.getReservation() != null) {
            response.setReservationId(tx.getReservation().getId());
            response.setParkingSpaceName(tx.getReservation().getParkingSpace().getTitle());
            response.setStartTime(tx.getReservation().getStartTime());
            response.setEndTime(tx.getReservation().getEndTime());
            if (tx.getReservation().getStartTime() != null && tx.getReservation().getEndTime() != null) {
                long duration = java.time.temporal.ChronoUnit.MINUTES.between(
                        tx.getReservation().getStartTime(),
                        tx.getReservation().getEndTime()
                );
                response.setDurationMinutes(duration);
            }
        }

        return response;
    }
}
