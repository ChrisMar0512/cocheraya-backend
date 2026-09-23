package com.cocheraya.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración de ejecución asíncrona para CocheraYa.
 *
 * Habilita {@code @Async} en toda la aplicación y define un pool de hilos
 * dedicado para tareas en segundo plano (envío de correos, notificaciones, etc.).
 * Con esto se evita que el envío de emails bloquee el hilo HTTP principal.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool de hilos reutilizable para tareas asíncronas.
     *
     * @return executor configurado con nombre de hilo identificable
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("CocheraYa-Async-");
        executor.initialize();
        return executor;
    }
}
