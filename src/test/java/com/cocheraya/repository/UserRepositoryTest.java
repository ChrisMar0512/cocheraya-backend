package com.cocheraya.repository;

import com.cocheraya.config.TestContainersConfig;
import com.cocheraya.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(TestContainersConfig.class)
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User driver;
    private User host;

    @BeforeEach
    void setUp() {
        driver = new User();
        driver.setEmail("driver@cocheraya.com");
        driver.setPassword("encoded-password-123");
        driver.setName("Test Driver");
        driver.setPhone("987654321");
        driver.setRole(User.Role.DRIVER);
        driver.setEnabled(true);
        entityManager.persistAndFlush(driver);

        host = new User();
        host.setEmail("host@cocheraya.com");
        host.setPassword("encoded-password-456");
        host.setName("Test Host");
        host.setPhone("912345678");
        host.setRole(User.Role.HOST);
        host.setEnabled(true);
        entityManager.persistAndFlush(host);
    }

    @Test
    @DisplayName("should return user when email exists")
    void shouldReturnUserWhenEmailExists() {
        Optional<User> found = userRepository.findByEmail("driver@cocheraya.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Driver");
        assertThat(found.get().getRole()).isEqualTo(User.Role.DRIVER);
        assertThat(found.get().getEmail()).isEqualTo("driver@cocheraya.com");
    }

    @Test
    @DisplayName("should return empty optional when email does not exist")
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<User> found = userRepository.findByEmail("unknown@cocheraya.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should return correct user when multiple users exist")
    void shouldReturnCorrectUserWhenMultipleUsersExist() {
        Optional<User> foundDriver = userRepository.findByEmail("driver@cocheraya.com");
        Optional<User> foundHost = userRepository.findByEmail("host@cocheraya.com");

        assertThat(foundDriver).isPresent();
        assertThat(foundHost).isPresent();
        assertThat(foundDriver.get().getId()).isNotEqualTo(foundHost.get().getId());
        assertThat(foundDriver.get().getRole()).isEqualTo(User.Role.DRIVER);
        assertThat(foundHost.get().getRole()).isEqualTo(User.Role.HOST);
    }

    @Test
    @DisplayName("should be case sensitive when searching by email")
    void shouldBeCaseSensitiveWhenSearchingByEmail() {
        Optional<User> found = userRepository.findByEmail("DRIVER@COCHERAYA.COM");

        assertThat(found).isEmpty();
    }
}
