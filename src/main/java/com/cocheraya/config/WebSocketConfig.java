package com.cocheraya.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSocket con STOMP para CocheraYa.
 * Permite comunicación en tiempo real para notificaciones de disponibilidad
 * de cocheras, actualizaciones de reservas y seguimiento de ubicación.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configura el message broker en memoria.
     * - /topic: prefijo para mensajes broadcast (publicar a múltiples suscriptores)
     * - /app: prefijo para mensajes dirigidos a métodos @MessageMapping del servidor
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker simple en memoria para mensajes de tipo publish-subscribe
        config.enableSimpleBroker("/topic");
        
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Registra el endpoint WebSocket con soporte SockJS.
     * SockJS proporciona fallback HTTP para navegadores que no soportan WebSocket nativo.
     * El endpoint /ws está permitido en SecurityConfig sin autenticación.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  
                .withSockJS();

        // Raw WebSocket endpoint for native clients (like React Native)
        registry.addEndpoint("/ws-raw")
                .setAllowedOriginPatterns("*");
    }
}
