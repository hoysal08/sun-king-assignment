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

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not installed. Installing..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "✅ Docker Compose installed."
fi

# Stop existing containers if any
echo "🛑 Stopping existing containers..."
docker-compose -f docker-compose.prod.yml down 2>/dev/null || true

# Build and start services
echo "🔨 Building and starting services..."
docker-compose -f docker-compose.prod.yml up -d --build

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check service health
echo "🏥 Checking service health..."
for i in {1..30}; do
    if curl -f http://localhost:8081/actuator/health &>/dev/null && \
       curl -f http://localhost:8082/actuator/health &>/dev/null; then
        echo "✅ All services are healthy!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "⚠️  Services may still be starting. Check logs with: docker-compose -f docker-compose.prod.yml logs"
        exit 1
    fi
    sleep 2
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
echo "   View logs:     docker-compose -f docker-compose.prod.yml logs -f"
echo "   Stop services: docker-compose -f docker-compose.prod.yml down"
echo "   Restart:       docker-compose -f docker-compose.prod.yml restart"
echo ""
