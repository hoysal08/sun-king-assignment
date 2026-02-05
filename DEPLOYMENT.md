# GCP VM Deployment Guide

Simple deployment guide for running the OMS system on a GCP VM.

## Prerequisites

1. **GCP VM Instance** with:
   - Ubuntu 20.04+ or Debian 11+
   - At least 4 vCPU, 8GB RAM (recommended: 8 vCPU, 16GB RAM)
   - 50GB+ disk space
   - External IP address

2. **Firewall Rules** - Open these ports in GCP Firewall:
   - `8081` - Order Service
   - `8082` - Inventory Service
   - `5432` - PostgreSQL (optional, for direct DB access)
   - `9092` - Kafka (optional, for debugging)

## Quick Deployment

### Step 1: Connect to your VM

```bash
gcloud compute ssh YOUR_VM_NAME --zone YOUR_ZONE
```

### Step 2: Clone the repository

```bash
git clone YOUR_REPO_URL
cd sun-king-assignment
```

Or upload the project files using `scp`:

```bash
# From your local machine
scp -r sun-king-assignment USER@VM_IP:~/
```

### Step 3: Run deployment script

```bash
chmod +x deploy.sh
./deploy.sh
```

The script will:
- Install Docker and Docker Compose if needed
- Build all services
- Start all containers
- Verify health

### Step 4: Configure GCP Firewall

Allow incoming traffic on ports 8081 and 8082:

```bash
# Using gcloud CLI
gcloud compute firewall-rules create oms-services \
    --allow tcp:8081,tcp:8082 \
    --source-ranges 0.0.0.0/0 \
    --description "OMS Services"
```

Or via GCP Console:
1. Go to VPC Network → Firewall
2. Create Firewall Rule
3. Allow TCP ports: `8081, 8082`
4. Source: `0.0.0.0/0` (or restrict to your IP)
5. Target: Your VM instance

### Step 5: Access Services

Get your VM's external IP:

```bash
gcloud compute instances describe YOUR_VM_NAME --zone YOUR_ZONE --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

Then access:

- **Order Service Swagger**: `http://YOUR_VM_IP:8081/swagger-ui.html`
- **Inventory Service Swagger**: `http://YOUR_VM_IP:8082/swagger-ui.html`
- **Order Service API**: `http://YOUR_VM_IP:8081/api/v1/orders`
- **Inventory Service API**: `http://YOUR_VM_IP:8082/api/v1/inventory`

## Manual Deployment (Alternative)

If you prefer manual steps:

```bash
# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Build and start
docker-compose -f docker-compose.prod.yml up -d --build

# Check logs
docker-compose -f docker-compose.prod.yml logs -f
```

## Service Management

### View Logs
```bash
docker-compose -f docker-compose.prod.yml logs -f
# Or for specific service
docker-compose -f docker-compose.prod.yml logs -f order-service
```

### Stop Services
```bash
docker-compose -f docker-compose.prod.yml down
```

### Restart Services
```bash
docker-compose -f docker-compose.prod.yml restart
```

### Update and Redeploy
```bash
# Pull latest code
git pull

# Rebuild and restart
docker-compose -f docker-compose.prod.yml up -d --build
```

## Health Checks

Check if services are running:

```bash
# Order Service
curl http://localhost:8081/actuator/health

# Inventory Service
curl http://localhost:8082/actuator/health
```

## Troubleshooting

### Services not starting
```bash
# Check container status
docker-compose -f docker-compose.prod.yml ps

# Check logs
docker-compose -f docker-compose.prod.yml logs
```

### Port already in use
```bash
# Find process using port
sudo lsof -i :8081
sudo lsof -i :8082

# Kill process if needed
sudo kill -9 <PID>
```

### Out of memory
Increase VM size or add swap:
```bash
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### Can't access from outside
1. Check firewall rules in GCP Console
2. Verify VM has external IP
3. Check if services are listening:
   ```bash
   sudo netstat -tlnp | grep 8081
   sudo netstat -tlnp | grep 8082
   ```

## Production Considerations

For production use, consider:

1. **Use HTTPS**: Set up nginx with Let's Encrypt SSL
2. **Restrict Firewall**: Only allow specific IPs, not 0.0.0.0/0
3. **Backup Databases**: Set up automated backups
4. **Monitoring**: Add monitoring tools (Prometheus, Grafana)
5. **Logging**: Centralized logging (ELK stack)
6. **Auto-restart**: Use systemd to auto-start on boot

### Auto-start on boot (Optional)

Create systemd service:

```bash
sudo nano /etc/systemd/system/oms.service
```

Add:
```ini
[Unit]
Description=OMS Services
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/home/YOUR_USER/sun-king-assignment
ExecStart=/usr/local/bin/docker-compose -f docker-compose.prod.yml up -d
ExecStop=/usr/local/bin/docker-compose -f docker-compose.prod.yml down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

Enable:
```bash
sudo systemctl enable oms.service
sudo systemctl start oms.service
```

## Base URLs

Once deployed, your base URLs will be:

- **Order Service**: `http://YOUR_VM_IP:8081`
- **Inventory Service**: `http://YOUR_VM_IP:8082`

Swagger UI endpoints:
- **Order Service**: `http://YOUR_VM_IP:8081/swagger-ui.html`
- **Inventory Service**: `http://YOUR_VM_IP:8082/swagger-ui.html`

API Documentation:
- **Order Service**: `http://YOUR_VM_IP:8081/api-docs`
- **Inventory Service**: `http://YOUR_VM_IP:8082/api-docs`
