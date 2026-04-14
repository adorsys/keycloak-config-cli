# Docker Usage

The `keycloak-config-cli` is published as a Docker container, making it easy to run securely and repeatably without requiring Java to be installed locally.

## Basic Usage

To run the import using Docker, you can mount your local configuration directory into the container.

```bash
docker run --rm \
    -e KEYCLOAK_URL="http://<your keycloak host>:8080/" \
    -e KEYCLOAK_USER="<keycloak admin username>" \
    -e KEYCLOAK_PASSWORD="<keycloak admin password>" \
    -e IMPORT_FILES_LOCATIONS='/config/*' \
    -v $(pwd)/config:/config \
    adorsys/keycloak-config-cli:latest
```

## Docker Compose

You can easily integrate it with a Keycloak instance defined in a `docker-compose.yml` file:

```yaml
version: '3.7'
services:
  keycloak:
    image: quay.io/keycloak/keycloak:latest
    command: start-dev
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin

  config-cli:
    image: adorsys/keycloak-config-cli:latest
    depends_on:
      - keycloak
    environment:
      KEYCLOAK_URL: http://keycloak:8080/
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD: admin
      KEYCLOAK_AVAILABILITYCHECK_ENABLED: "true"
      KEYCLOAK_AVAILABILITYCHECK_TIMEOUT: 120s
      IMPORT_FILES_LOCATIONS: /config/*
    volumes:
      - ./config:/config
```

## Related Topics
- [Helm Chart](helm-chart.md)
- [Kubernetes Deployment](kubernetes-deployment.md)
