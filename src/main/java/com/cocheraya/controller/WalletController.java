package com.cocheraya.controller;

import com.cocheraya.dto.EarningsSummaryResponse;
import com.cocheraya.dto.TopUpRequest;
import com.cocheraya.dto.WalletResponse;
import com.cocheraya.dto.WalletTransactionResponse;
import com.cocheraya.entity.User;
import com.cocheraya.service.IWalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final IWalletService walletService;

    

    
    @GetMapping("/balance")
    public ResponseEntity<WalletResponse> getBalance() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.getBalance(user.getId()));
    }

    

    
    @PostMapping("/top-up")
    public ResponseEntity<WalletResponse> topUp(@Valid @RequestBody TopUpRequest request) {
        User user = getAuthenticatedUser();
        WalletResponse response = walletService.topUp(user.getId(), request.getAmount());
        return ResponseEntity.ok(response);
    }

    

    
    @GetMapping("/transactions")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getAuthenticatedUser();
        Page<WalletTransactionResponse> transactions = walletService.getTransactionHistory(
                user.getId(), PageRequest.of(page, size)
        );
        return ResponseEntity.ok(transactions);
    }

    /**
     * Retorna el historial filtrado por tipo de transacción (TOPUP, CHARGE, REFUND).
     *
     * @param type tipo de transacción a filtrar (string)
     * @param page número de página (default: 0)
     * @param size cantidad de elementos por página (default: 10)
     * @return página de transacciones del tipo indicado
     */
    @GetMapping("/transactions/{type}")
    public ResponseEntity<Page<WalletTransactionResponse>> getTransactionsByType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        User user = getAuthenticatedUser();
        Page<WalletTransactionResponse> transactions = walletService.getTransactionHistoryByType(
                user.getId(), type, PageRequest.of(page, size)
        );
        return ResponseEntity.ok(transactions);
    }

    

    
    @GetMapping("/earnings")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<EarningsSummaryResponse> getEarningsSummary() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(walletService.getEarningsSummary(user.getId()));
    }

    

    
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}
