---
title: Docker Usage
description: Using keycloak-config-cli with Docker and Docker Compose
sidebar_position: 1
---

# Docker Usage

keycloak-config-cli is available as a Docker image and can be easily integrated into containerized workflows.

## Official Docker Image

The official Docker image is available on Docker Hub:

```bash
docker pull adorsys/keycloak-config-cli:latest
```

### Available Tags

- `latest` - Latest stable release
- `vX.Y.Z` - Specific version tags
- `snapshot` - Latest snapshot build

## Basic Docker Usage

### Simple Configuration Import

```bash
docker run --rm \
  -e KEYCLOAK_URL=https://your-keycloak-server.com \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin-password \
  -v /path/to/your/config:/config \
  adorsys/keycloak-config-cli:latest
```

### With Custom Configuration File

```bash
docker run --rm \
  -e KEYCLOAK_URL=https://your-keycloak-server.com \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin-password \
  -v $(pwd)/realm-config.json:/config/realm-config.json \
  adorsys/keycloak-config-cli:latest \
  --import.files=realm-config.json
```

### With Variable Substitution

```bash
docker run --rm \
  -e KEYCLOAK_URL=https://your-keycloak-server.com \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin-password \
  -e IMPORT_VAR_SUBSTITUTION_ENABLED=true \
  -e REALM_NAME=production-realm \
  -v $(pwd)/config-template.json:/config/config-template.json \
  adorsys/keycloak-config-cli:latest
```

## Docker Compose Integration

### Basic Docker Compose

Create a `docker-compose.yml` file:

```yaml
version: '3.8'

services:
  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: https://your-keycloak-server.com
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD: admin-password
      IMPORT_PATH: /config
    volumes:
      - ./config:/config
    restart: on-failure
```

Run with:

```bash
docker-compose up
```

### Multi-Environment Setup

Create environment-specific configurations:

```yaml
version: '3.8'

services:
  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: ${KEYCLOAK_URL}
      KEYCLOAK_USER: ${KEYCLOAK_USER}
      KEYCLOAK_PASSWORD: ${KEYCLOAK_PASSWORD}
      REALM_NAME: ${REALM_NAME}
      IMPORT_VAR_SUBSTITUTION_ENABLED: "true"
    volumes:
      - ./config:/config
      - ./.env:/.env:ro
    restart: on-failure
```

Environment files:

```bash
# .env.development
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_USER=admin
KEYCLOAK_PASSWORD=admin
REALM_NAME=development-realm

# .env.production
KEYCLOAK_URL=https://keycloak.company.com
KEYCLOAK_USER=admin
KEYCLOAK_PASSWORD=${PROD_ADMIN_PASSWORD}
REALM_NAME=production-realm
```

Run with specific environment:

```bash
# Development
docker-compose --env-file .env.development up

# Production
docker-compose --env-file .env.production up
```

### With Keycloak Container

Complete setup with Keycloak:

```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:latest
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: localhost
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: http://keycloak:8080
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD: admin
      IMPORT_PATH: /config
      IMPORT_WAIT_FOR_KEYCLOAK: "true"
      IMPORT_WAIT_FOR_KEYCLOAK_TIMEOUT: "120"
    volumes:
      - ./config:/config
    depends_on:
      keycloak:
        condition: service_healthy
    restart: on-failure
```

## Advanced Docker Configurations

### Custom Network

```bash
# Create network
docker network create keycloak-network

# Run on custom network
docker run --rm \
  --network keycloak-network \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -v ./config:/config \
  adorsys/keycloak-config-cli:latest
```

### Resource Limits

```yaml
version: '3.8'

services:
  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: https://your-keycloak-server.com
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD: admin-password
      IMPORT_PATH: /config
    volumes:
      - ./config:/config
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
```

### Health Check

```yaml
version: '3.8'

services:
  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: https://your-keycloak-server.com
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD: admin-password
      IMPORT_PATH: /config
    volumes:
      - ./config:/config
    healthcheck:
      test: ["CMD", "java", "-jar", "/app/keycloak-config-cli.jar", "--version"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s
```

## CI/CD Integration

### GitHub Actions

```yaml
name: Update Keycloak Configuration

on:
  push:
    paths:
      - 'config/**'

jobs:
  update-keycloak:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Update Keycloak Configuration
        env:
          KEYCLOAK_URL: ${{ secrets.KEYCLOAK_URL }}
          KEYCLOAK_USER: ${{ secrets.KEYCLOAK_USER }}
          KEYCLOAK_PASSWORD: ${{ secrets.KEYCLOAK_PASSWORD }}
        run: |
          docker run --rm \
            -e KEYCLOAK_URL=$KEYCLOAK_URL \
            -e KEYCLOAK_USER=$KEYCLOAK_USER \
            -e KEYCLOAK_PASSWORD=$KEYCLOAK_PASSWORD \
            -v $(pwd)/config:/config \
            adorsys/keycloak-config-cli:latest
```

### GitLab CI

```yaml
update_keycloak:
  image: docker:latest
  services:
    - docker:dind
  script:
    - docker run --rm \
        -e KEYCLOAK_URL=$KEYCLOAK_URL \
        -e KEYCLOAK_USER=$KEYCLOAK_USER \
        -e KEYCLOAK_PASSWORD=$KEYCLOAK_PASSWORD \
        -v $(pwd)/config:/config \
        adorsys/keycloak-config-cli:latest
  only:
    - main
```

## Dockerfile Examples

### Custom Dockerfile with Additional Tools

```dockerfile
FROM adorsys/keycloak-config-cli:latest

# Install additional tools
RUN apk add --no-cache curl jq

# Add custom scripts
COPY scripts/ /usr/local/bin/
RUN chmod +x /usr/local/bin/*.sh

# Set entrypoint
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
```

### Multi-stage Build for Custom Configuration

```dockerfile
FROM adorsys/keycloak-config-cli:latest as base

# Build stage
FROM node:16-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Final stage
FROM base
COPY --from=build /app/dist /config
COPY custom-config.json /config/
```

## Environment Variables in Docker

### Using Environment Files

```bash
# Create .env file
cat > .env << EOF
KEYCLOAK_URL=https://your-keycloak-server.com
KEYCLOAK_USER=admin
KEYCLOAK_PASSWORD=admin-password
REALM_NAME=production-realm
IMPORT_VAR_SUBSTITUTION_ENABLED=true
EOF

# Use with Docker Compose
docker-compose --env-file .env up
```

### Docker Secrets

```bash
# Create Docker secret
echo "admin-password" | docker secret create keycloak_admin_password -

# Use in Docker Compose
version: '3.8'

services:
  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: https://your-keycloak-server.com
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD_FILE: /run/secrets/keycloak_admin_password
    secrets:
      - keycloak_admin_password
    volumes:
      - ./config:/config
```

## Troubleshooting

### Common Docker Issues

1. **Permission Denied**: Fix volume permissions
   ```bash
   sudo chown -R $(id -u):$(id -g) ./config
   ```

2. **Network Issues**: Use proper networking
   ```bash
   docker network create keycloak-net
   docker run --network keycloak-net ...
   ```

3. **Environment Variables**: Verify variable passing
   ```bash
   docker run --rm -e TEST_VAR=test adorsys/keycloak-config-cli:latest env | grep TEST_VAR
   ```

### Debug Mode

Enable debug logging:

```bash
docker run --rm \
  -e KEYCLOAK_URL=https://your-keycloak-server.com \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -e LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG=DEBUG \
  -v ./config:/config \
  adorsys/keycloak-config-cli:latest
```

### Dry Run

Test configuration without applying:

```bash
docker run --rm \
  -e KEYCLOAK_URL=https://your-keycloak-server.com \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -v ./config:/config \
  adorsys/keycloak-config-cli:latest \
  --import.dry-run=true
```

## Best Practices

1. **Use Specific Tags**: Pin to specific versions in production
2. **Environment Variables**: Use environment files for configuration
3. **Volume Management**: Mount configuration files as read-only when possible
4. **Health Checks**: Implement health checks for monitoring
5. **Resource Limits**: Set appropriate resource constraints
6. **Security**: Use Docker secrets for sensitive data

## Next Steps

- [Helm Chart](./helm-chart) - Kubernetes deployment with Helm
- [Configuration](../configuration/overview) - General configuration options
- [Variable Substitution](../variable-substitution/overview) - Dynamic configuration
