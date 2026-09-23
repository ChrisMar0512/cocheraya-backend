package com.cocheraya;

import com.cocheraya.entity.User;
import com.cocheraya.repository.UserRepository;
import com.cocheraya.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final WalletService walletService;

    private static final String SEED_CHECK_EMAIL = "admin@cocheraya.com";
    private static final String DEFAULT_PASSWORD = "SecurePassword123";

    @Override
    public void run(String... args) {
        
        if (userRepository.findByEmail(SEED_CHECK_EMAIL).isPresent()) {
            log.info("Datos de prueba ya existen — omitiendo DataSeeder.");
            return;
        }

        log.info("Sembrando datos de prueba iniciales...");

        
        User driver = new User();
        driver.setName("Carlos Driver");
        driver.setEmail("seeded_driver@cocheraya.com");
        driver.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        driver.setPhone("999111222");
        driver.setRole(User.Role.DRIVER);
        driver.setEnabled(true);

        User savedDriver = userRepository.save(driver);
        walletService.initializeWallet(savedDriver);
        
        walletService.topUp(savedDriver, new BigDecimal("100.00"));

        log.info("Usuario DRIVER creado: {} — saldo inicial S/. 100", driver.getEmail());

        
        User host = new User();
        host.setName("María Host");
        host.setEmail("seeded_host@cocheraya.com");
        host.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        host.setPhone("999333444");
        host.setRole(User.Role.HOST);
        host.setEnabled(true);

        User savedHost = userRepository.save(host);
        walletService.initializeWallet(savedHost);

        log.info("Usuario HOST creado: {}", host.getEmail());

        
        User admin = new User();
        admin.setName("Admin CocheraYa");
        admin.setEmail(SEED_CHECK_EMAIL);
        admin.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setPhone("999000000");
        admin.setRole(User.Role.HOST);
        admin.setEnabled(true);
        userRepository.save(admin);

        log.info("✅ Datos de prueba creados exitosamente. " +
                "Credenciales: seeded_driver@cocheraya.com / {} y seeded_host@cocheraya.com / {}",
                DEFAULT_PASSWORD, DEFAULT_PASSWORD);
    }
}
