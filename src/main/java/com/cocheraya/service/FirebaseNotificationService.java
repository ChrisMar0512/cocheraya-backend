package com.cocheraya.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FirebaseNotificationService {

    /**
     * Envía una notificación push a un dispositivo específico.
     *
     * @param fcmToken token FCM del dispositivo destino (obtenido al hacer login)
     * @param title    título de la notificación
     * @param body     cuerpo/mensaje de la notificación
     */
    @Async
    public void sendNotification(String fcmToken, String title, String body) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("No se pudo enviar notificación: fcmToken nulo o vacío.");
            return;
        }

        // Construir la notificación FCM
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setToken(fcmToken)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Notificación enviada exitosamente. FCM response: {}", response);
        } catch (FirebaseMessagingException e) {
            
            log.error("Error al enviar notificación FCM al token [{}]: {}", fcmToken, e.getMessage());
        }
    }

    /**
     * Envía una notificación predefinida informando que la reserva expirará en 5 minutos.
     * Llamado por el scheduler de reservas antes de que venza el tiempo.
     *
     * @param fcmToken   token FCM del conductor con la reserva activa
     * @param nombreCochera nombre de la cochera donde está la reserva
     */
    @Async
    public void sendReservationExpiringNotification(String fcmToken, String nombreCochera) {
        String title = "⏰ Tu reserva está por vencer";
        String body = "Tu reserva en " + nombreCochera + " expira en 5 minutos";
        sendNotification(fcmToken, title, body);
    }
}
