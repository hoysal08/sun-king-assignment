#!/bin/bash

set -e

echo "🚀 Starting OMS Deployment on GCP VM..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Installing Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sudo sh get-docker.sh
    sudo usermod -aG docker $USER
    rm get-docker.sh
    echo "✅ Docker installed. You may need to log out and back in."
fi

# Determine which compose command to use and check permissions
if docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="docker compose"
    echo "✅ Using Docker Compose plugin (docker compose)"
elif sudo docker compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="sudo docker compose"
    echo "✅ Using Docker Compose plugin with sudo"
elif command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker-compose"
    echo "✅ Using Docker Compose standalone (docker-compose)"
elif sudo docker-compose version &> /dev/null 2>&1; then
    COMPOSE_CMD="sudo docker-compose"
    echo "✅ Using Docker Compose standalone with sudo"
else
    echo "❌ Docker Compose is not installed. Installing standalone version..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    COMPOSE_CMD="docker-compose"
    echo "✅ Docker Compose installed."
fi

# Test docker access and use sudo if needed
if ! docker ps &> /dev/null; then
    if sudo docker ps &> /dev/null; then
        echo "⚠️  Using sudo for docker commands (user not in docker group yet)"
        DOCKER_CMD="sudo docker"
        # Update COMPOSE_CMD if it doesn't already have sudo
        if [[ ! "$COMPOSE_CMD" =~ ^sudo ]]; then
            COMPOSE_CMD="sudo $COMPOSE_CMD"
        fi
    else
        echo "❌ Cannot access Docker. Please run 'newgrp docker' or log out and back in."
        exit 1
    fi
else
    DOCKER_CMD="docker"
fi

# Stop existing containers if any
echo "🛑 Stopping existing containers..."
$COMPOSE_CMD -f docker-compose.prod.yml down 2>/dev/null || true

# Build and start services
echo "🔨 Building and starting services..."
$COMPOSE_CMD -f docker-compose.prod.yml up -d --build

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service health
echo "🏥 Checking service health..."
for i in {1..60}; do
    if curl -f http://localhost:8081/actuator/health &>/dev/null 2>&1 && \
       curl -f http://localhost:8082/actuator/health &>/dev/null 2>&1; then
        echo "✅ All services are healthy!"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "⚠️  Services may still be starting. Check logs with: $COMPOSE_CMD -f docker-compose.prod.yml logs"
        echo "    Or check individual service logs: $COMPOSE_CMD -f docker-compose.prod.yml logs order-service"
        exit 1
    fi
    echo "   Waiting for services... ($i/60)"
    sleep 3
done

# Get external IP
EXTERNAL_IP=$(curl -s http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip -H "Metadata-Flavor: Google" 2>/dev/null || echo "YOUR_VM_IP")

echo ""
echo "🎉 Deployment Complete!"
echo ""
echo "📍 Service URLs (replace YOUR_VM_IP with your GCP VM external IP):"
echo "   Order Service API:        http://${EXTERNAL_IP}:8081"
echo "   Order Service Swagger:    http://${EXTERNAL_IP}:8081/swagger-ui.html"
echo "   Inventory Service API:    http://${EXTERNAL_IP}:8082"
echo "   Inventory Service Swagger: http://${EXTERNAL_IP}:8082/swagger-ui.html"
echo ""
echo "📋 Useful commands:"
echo "   View logs:     $COMPOSE_CMD -f docker-compose.prod.yml logs -f"
echo "   Stop services: $COMPOSE_CMD -f docker-compose.prod.yml down"
echo "   Restart:       $COMPOSE_CMD -f docker-compose.prod.yml restart"
echo ""
