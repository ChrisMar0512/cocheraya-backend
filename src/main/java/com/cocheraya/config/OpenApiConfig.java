package com.cocheraya.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI / Swagger UI para CocheraYa.
 * Incluye especificación de autenticación JWT Bearer en cabecera HTTP
 * para permitir la ejecución interactiva de endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI cocheraYaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CocheraYa API — Plataforma Backend")
                        .version("1.0.0")
                        .description("Documentación oficial e interactiva de la API REST de CocheraYa: Marketplace Inteligente y Colaborativo de Estacionamiento On-Demand en Lima Metropolitana. Desarrollado para CS 2031 Desarrollo Basado en Plataforma.")
                        .contact(new Contact()
                                .name("Christian Mar Carrillo")
                                .email("christian.mar@utec.edu.pe"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingrese el token JWT obtenido del endpoint /api/v1/auth/login")));
    }
}