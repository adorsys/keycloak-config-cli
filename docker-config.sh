#!/usr/bin/env bash

docker build -t adorsys.de/keycloak-config-cli:latest .
docker run -e KEYCLOAK_URL=http://localhost:8080/auth \
    -e KEYCLOAK_ADMIN=admin \
    -e KEYCLOAK_ADMIN_PASSWORD=admin123 \
    -v `pwd`/example-config:/opt/keycloak-config-cli/configs \
    adorsys.de/keycloak-config-cli:latest
