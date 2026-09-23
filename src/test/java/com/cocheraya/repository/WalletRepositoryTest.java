package com.cocheraya.repository;

import com.cocheraya.config.TestContainersConfig;
import com.cocheraya.entity.User;
import com.cocheraya.entity.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("WalletRepository Tests")
class WalletRepositoryTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("wallet-user@cocheraya.com");
        user.setPassword("encoded-password");
        user.setName("Wallet User");
        user.setRole(User.Role.DRIVER);
        user.setEnabled(true);
        entityManager.persistAndFlush(user);

        wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("150.50"));
        entityManager.persistAndFlush(wallet);
    }

    @Test
    @DisplayName("should return wallet when userId exists")
    void shouldReturnWalletWhenUserIdExists() {
        Optional<Wallet> found = walletRepository.findByUserId(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("should return empty when userId does not exist")
    void shouldReturnEmptyWhenUserIdDoesNotExist() {
        Optional<Wallet> found = walletRepository.findByUserId(99999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should return wallet when user email exists")
    void shouldReturnWalletWhenUserEmailExists() {
        Optional<Wallet> found = walletRepository.findByUserEmail("wallet-user@cocheraya.com");

        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(found.get().getUser().getEmail()).isEqualTo("wallet-user@cocheraya.com");
    }

    @Test
    @DisplayName("should return empty when user email does not exist")
    void shouldReturnEmptyWhenUserEmailDoesNotExist() {
        Optional<Wallet> found = walletRepository.findByUserEmail("nonexistent@cocheraya.com");

        assertThat(found).isEmpty();
    }
}
