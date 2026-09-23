# ============================================================
# CocheraYa — Multi-stage Production Dockerfile
# ============================================================

# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar POM primero para cachear capas de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y empaquetar
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Stage 2: Runtime (distroless / Alpine JRE) ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Variables de entorno por defecto
ENV TZ=America/Lima \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Duser.timezone=America/Lima"

# Crear usuario sin privilegios para mayor seguridad
RUN addgroup -S cocheraya && adduser -S cocheraya -G cocheraya
USER cocheraya

COPY --from=build --chown=cocheraya:cocheraya /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]