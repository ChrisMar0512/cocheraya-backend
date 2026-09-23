#!/bin/bash
# ==============================================================================
# Script de Despliegue Automatizado en AWS EC2 para CocheraYa Backend
# ==============================================================================
set -e

echo "🚀 Iniciando despliegue de CocheraYa Backend en AWS EC2..."

# 1. Actualizar sistema e instalar Docker
sudo apt-get update -y
sudo apt-get install -y docker.io docker-compose git

# 2. Iniciar y habilitar servicio Docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# 3. Validar variables de entorno requeridas
if [ -z "$SPRING_DATASOURCE_URL" ]; then
    echo "⚠️ ERROR: Variable SPRING_DATASOURCE_URL no definida (ej. jdbc:postgresql://<rds-endpoint>:5432/cocheraya_db)"
    exit 1
fi

# 4. Detener contenedor anterior si existe
echo "🛑 Deteniendo versiones previas..."
docker stop cocheraya-backend || true
docker rm cocheraya-backend || true

# 5. Construir y ejecutar imagen Docker en producción
echo "📦 Construyendo imagen Docker..."
docker build -t cocheraya-backend:latest .

echo "▶️ Ejecutando contenedor en puerto 8080..."
docker run -d \
  --name cocheraya-backend \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="$SPRING_DATASOURCE_URL" \
  -e SPRING_DATASOURCE_USERNAME="$SPRING_DATASOURCE_USERNAME" \
  -e SPRING_DATASOURCE_PASSWORD="$SPRING_DATASOURCE_PASSWORD" \
  -e APP_JWT_SECRET="$APP_JWT_SECRET" \
  -e APP_JWT_EXPIRATION="86400000" \
  -e CORS_ALLOWED_ORIGINS="*" \
  cocheraya-backend:latest

echo "✅ CocheraYa Backend desplegado exitosamente!"
echo "📡 Healthcheck: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/swagger-ui/index.html"