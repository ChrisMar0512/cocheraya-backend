package com.cocheraya.repository;

import com.cocheraya.config.TestContainersConfig;
import com.cocheraya.entity.User;
import com.cocheraya.entity.Wallet;
import com.cocheraya.entity.WalletTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("WalletTransactionRepository Tests")
class WalletTransactionRepositoryTest {

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("tx-user@cocheraya.com");
        user.setPassword("encoded-password");
        user.setName("Tx User");
        user.setRole(User.Role.DRIVER);
        user.setEnabled(true);
        entityManager.persistAndFlush(user);

        wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("200.00"));
        entityManager.persistAndFlush(wallet);

        // Create 3 transactions
        WalletTransaction tx1 = new WalletTransaction();
        tx1.setWallet(wallet);
        tx1.setType(WalletTransaction.TransactionType.TOPUP);
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setBalanceAfter(new BigDecimal("100.00"));
        tx1.setDescription("Recarga inicial");
        entityManager.persistAndFlush(tx1);

        WalletTransaction tx2 = new WalletTransaction();
        tx2.setWallet(wallet);
        tx2.setType(WalletTransaction.TransactionType.TOPUP);
        tx2.setAmount(new BigDecimal("150.00"));
        tx2.setBalanceAfter(new BigDecimal("250.00"));
        tx2.setDescription("Segunda recarga");
        entityManager.persistAndFlush(tx2);

        WalletTransaction tx3 = new WalletTransaction();
        tx3.setWallet(wallet);
        tx3.setType(WalletTransaction.TransactionType.CHARGE);
        tx3.setAmount(new BigDecimal("50.00"));
        tx3.setBalanceAfter(new BigDecimal("200.00"));
        tx3.setDescription("Cobro estacionamiento");
        entityManager.persistAndFlush(tx3);
    }

    @Test
    @DisplayName("should return transactions ordered by createdAt desc when wallet has transactions")
    void shouldReturnTransactionsOrderedByCreatedAtDescWhenWalletHasTransactions() {
        Page<WalletTransaction> page = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("should return empty page when wallet has no transactions")
    void shouldReturnEmptyPageWhenWalletHasNoTransactions() {
        Page<WalletTransaction> page = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(99999L, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("should return correct count when wallet has transactions")
    void shouldReturnCorrectCountWhenWalletHasTransactions() {
        long count = walletTransactionRepository.countByWalletId(wallet.getId());

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("should return zero count when wallet has no transactions")
    void shouldReturnZeroCountWhenWalletHasNoTransactions() {
        long count = walletTransactionRepository.countByWalletId(99999L);

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("should respect pagination when returning transactions")
    void shouldRespectPaginationWhenReturningTransactions() {
        Page<WalletTransaction> firstPage = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);

        Page<WalletTransaction> secondPage = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(1, 2));

        assertThat(secondPage.getContent()).hasSize(1);
    }
}
