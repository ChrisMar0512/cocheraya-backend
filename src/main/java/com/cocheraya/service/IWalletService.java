package com.cocheraya.service;

import com.cocheraya.dto.EarningsSummaryResponse;
import com.cocheraya.dto.WalletResponse;
import com.cocheraya.dto.WalletTransactionResponse;
import com.cocheraya.entity.Reservation;
import com.cocheraya.entity.User;
import com.cocheraya.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface IWalletService {
    Wallet initializeWallet(User user);
    WalletResponse topUp(Long userId, BigDecimal amount);
    void topUp(User user, BigDecimal amount);
    WalletResponse charge(Long userId, BigDecimal amount, Reservation reservation);
    WalletResponse refund(Long userId, BigDecimal amount, Reservation reservation);
    void creditHost(Long hostId, BigDecimal amount, Reservation reservation);
    WalletResponse getBalance(Long userId);
    Page<WalletTransactionResponse> getTransactionHistory(Long userId, Pageable pageable);
    Page<WalletTransactionResponse> getTransactionHistoryByType(Long userId, String type, Pageable pageable);
    EarningsSummaryResponse getEarningsSummary(Long hostId);
}
