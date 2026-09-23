package com.cocheraya;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicación CocheraYa.
 * Marketplace de estacionamiento on-demand en Lima, Perú.
 *
 * @EnableScheduling habilita la ejecución de métodos @Scheduled usados por
 * ReservationScheduler para detectar reservas expiradas y enviar notificaciones.
 *
 * @EnableAsync habilita la ejecución de métodos @Async usados por
 * EmailService y FirebaseNotificationService para no bloquear el hilo principal.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CocherayaApplication {

    @PostConstruct
    public void init() {
        // Asegura que la aplicación use siempre la zona horaria de Lima (UTC-5) en cualquier servidor (incluyendo AWS)
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CocherayaApplication.class, args);
    }
}
