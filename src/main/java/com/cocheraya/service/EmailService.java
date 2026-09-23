package com.cocheraya.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;

/**
 * Servicio de envío de correos electrónicos para CocheraYa.
 *
 * Todos los métodos de envío están marcados con {@code @Async} para que
 * se ejecuten en el pool de hilos configurado en {@link com.cocheraya.config.AsyncConfig},
 * sin bloquear el hilo HTTP que atiende la solicitud del usuario.
 *
 * Principio de diseño: un fallo en el envío de email NUNCA debe interrumpir
 * el flujo principal de negocio. Por eso todas las excepciones se capturan
 * y loggean sin propagarse.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    /**
     * Envía el correo de bienvenida al usuario recién registrado.
     *
     * @param to       dirección de correo del destinatario
     * @param userName nombre del usuario para personalizar el email
     */
    @Async("taskExecutor")
    public void sendWelcomeEmail(String to, String userName, String role) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("role", role);

            String htmlContent = templateEngine.process("welcome-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Bienvenido a CocheraYa");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de bienvenida enviado exitosamente a {}", to);
        } catch (Exception e) {
            log.error("Error al enviar email de bienvenida a {}: {}", to, e.getMessage());
        }
    }

    /**
     * Envía el resumen de estacionamiento al conductor tras el checkout.
     *
     * @param to               dirección de correo del destinatario
     * @param userName         nombre del conductor
     * @param parkingSpaceName nombre de la cochera utilizada
     * @param minutes          duración del estacionamiento en minutos
     * @param totalCharged     monto total cobrado (S/.)
     * @param remainingBalance saldo restante en la wallet del conductor
     */
    @Async("taskExecutor")
    public void sendCheckoutSummary(String to, String userName, String parkingSpaceName,
                                    long minutes, BigDecimal totalCharged,
                                    BigDecimal remainingBalance) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("parkingSpaceName", parkingSpaceName);
            context.setVariable("minutes", minutes);
            context.setVariable("totalCharged", totalCharged);
            context.setVariable("remainingBalance", remainingBalance);

            String htmlContent = templateEngine.process("checkout-summary", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Resumen de tu estacionamiento — CocheraYa");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email de resumen de checkout enviado exitosamente a {}", to);
        } catch (Exception e) {
            log.error("Error al enviar email de resumen de checkout a {}: {}", to, e.getMessage());
        }
    }
}
