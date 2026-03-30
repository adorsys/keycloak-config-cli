# Docker & Helm

keycloak-config-cli is available as a Docker image and Helm chart for easy deployment.

## Docker

### Quick Start

```bash
docker run --rm \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin123 \
  -e IMPORT_FILES_LOCATIONS=/config/* \
  -v "$PWD/realms":/config \
  adorsys/keycloak-config-cli:latest
```

### Available Tags

- `latest` - Latest stable release
- `6.x.x` - Specific version
- `edge` - Latest development build

### Environment Variables

| Variable | Description |
|----------|-------------|
| `KEYCLOAK_URL` | Keycloak server URL |
| `KEYCLOAK_USER` | Admin username |
| `KEYCLOAK_PASSWORD` | Admin password |
| `IMPORT_FILES_LOCATIONS` | Path to config files |

## Helm Chart

### Installation

```bash
helm repo add adorsys https://adorsys.github.io/keycloak-config-cli
helm install keycloak-config-cli adorsys/keycloak-config-cli
```

### As Init Container

Use keycloak-config-cli as an init container to configure Keycloak before your application starts.

### Kubernetes Deployment

See [Helm Chart Deployment](helm-chart.md) for detailed Kubernetes deployment instructions.
