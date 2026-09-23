package com.cocheraya.service;

import com.cocheraya.dto.WalletResponse;
import com.cocheraya.entity.ParkingSpace;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.User;
import com.cocheraya.entity.Wallet;
import com.cocheraya.entity.WalletTransaction;
import com.cocheraya.exception.InsufficientBalanceException;
import com.cocheraya.repository.WalletRepository;
import com.cocheraya.repository.WalletTransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    @Mock
    private EntityManager entityManager;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("driver@test.com");
        user.setName("Test Driver");
        user.setRole(User.Role.DRIVER);

        wallet = new Wallet();
        wallet.setId(10L);
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
    }

    // ==================== initializeWallet ====================

    @Test
    @DisplayName("shouldInitializeWalletForNewUser")
    void shouldInitializeWalletForNewUser() {
        // Arrange
        when(walletRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet w = invocation.getArgument(0);
            w.setId(10L);
            return w;
        });

        // Act
        Wallet result = walletService.initializeWallet(user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(walletRepository).save(any(Wallet.class));
    }

    // ==================== topUp ====================

    @Test
    @DisplayName("shouldTopUpBalanceSuccessfully")
    void shouldTopUpBalanceSuccessfully() {
        // Arrange
        wallet.setBalance(new BigDecimal("50.00"));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        WalletResponse response = walletService.topUp(1L, new BigDecimal("30.00"));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("80.00"));

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txCaptor.capture());
        WalletTransaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getType()).isEqualTo(WalletTransaction.TransactionType.TOPUP);
        assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(savedTx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    // ==================== charge ====================

    @Test
    @DisplayName("shouldChargeWalletSuccessfully")
    void shouldChargeWalletSuccessfully() {
        // Arrange
        wallet.setBalance(new BigDecimal("100.00"));

        ParkingSpace space = new ParkingSpace();
        space.setId(5L);
        space.setTitle("Cochera Test");

        Reservation reservation = new Reservation();
        reservation.setId(50L);
        reservation.setDriver(user);
        reservation.setParkingSpace(space);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(entityManager.find(Wallet.class, 10L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(wallet);
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        WalletResponse response = walletService.charge(1L, new BigDecimal("25.00"), reservation);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("75.00"));

        verify(entityManager).merge(wallet);
        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getType()).isEqualTo(WalletTransaction.TransactionType.CHARGE);
    }

    @Test
    @DisplayName("shouldThrowInsufficientBalanceExceptionWhenBalanceTooLow")
    void shouldThrowInsufficientBalanceExceptionWhenBalanceTooLow() {
        // Arrange
        wallet.setBalance(new BigDecimal("10.00"));

        ParkingSpace space = new ParkingSpace();
        space.setId(5L);
        space.setTitle("Cochera Test");

        Reservation reservation = new Reservation();
        reservation.setId(50L);
        reservation.setDriver(user);
        reservation.setParkingSpace(space);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(entityManager.find(Wallet.class, 10L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(wallet);

        // Act & Assert
        assertThatThrownBy(() -> walletService.charge(1L, new BigDecimal("50.00"), reservation))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(walletTransactionRepository, never()).save(any());
    }

    // ==================== refund ====================

    @Test
    @DisplayName("shouldRefundSuccessfully")
    void shouldRefundSuccessfully() {
        // Arrange
        wallet.setBalance(new BigDecimal("70.00"));

        ParkingSpace space = new ParkingSpace();
        space.setId(5L);
        space.setTitle("Cochera Test");

        Reservation reservation = new Reservation();
        reservation.setId(50L);
        reservation.setDriver(user);
        reservation.setParkingSpace(space);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        WalletResponse response = walletService.refund(1L, new BigDecimal("20.00"), reservation);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualByComparingTo(new BigDecimal("90.00"));

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txCaptor.capture());
        WalletTransaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getType()).isEqualTo(WalletTransaction.TransactionType.REFUND);
        assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(savedTx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(savedTx.getDescription()).contains("Reembolso");
    }

    // ==================== creditHost ====================

    @Test
    @DisplayName("shouldCreditHostSuccessfully")
    void shouldCreditHostSuccessfully() {
        // Arrange
        wallet.setBalance(new BigDecimal("10.00"));

        ParkingSpace space = new ParkingSpace();
        space.setId(5L);
        space.setTitle("Cochera Host Test");

        Reservation reservation = new Reservation();
        reservation.setId(50L);
        reservation.setDriver(user);
        reservation.setParkingSpace(space);

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        walletService.creditHost(1L, new BigDecimal("15.00"), reservation);

        // Assert
        assertThat(wallet.getBalance()).isEqualByComparingTo(new BigDecimal("25.00"));

        verify(walletRepository).save(wallet);
        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txCaptor.capture());
        WalletTransaction savedTx = txCaptor.getValue();
        assertThat(savedTx.getType()).isEqualTo(WalletTransaction.TransactionType.CHARGE);
        assertThat(savedTx.getAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(savedTx.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(savedTx.getDescription()).contains("Alquiler de");
    }
}
