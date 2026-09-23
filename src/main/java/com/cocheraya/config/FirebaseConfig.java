package com.cocheraya.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path}")
    private String firebaseCredentialsPath;

    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase ya estaba inicializado, reutilizando instancia existente.");
            return FirebaseApp.getInstance();
        }

        Resource resource = resourceLoader.getResource(firebaseCredentialsPath);
        if (!resource.exists()) {
            log.warn("Archivo de credenciales de Firebase no encontrado en: {}. Se omitirá la inicialización de Firebase y las notificaciones push no estarán disponibles en este ambiente local.", firebaseCredentialsPath);
            return null;
        }

        try (InputStream serviceAccountStream = resource.getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK inicializado correctamente.");
            return app;
        }
    }
}
