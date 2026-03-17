---
title: Installation
description: Learn how to install and set up keycloak-config-cli
sidebar_position: 2
---

# Installation

keycloak-config-cli can be installed and run in multiple ways depending on your environment and preferences.

## Prerequisites

- Java 17 or higher
- Access to a Keycloak server

## Installation Methods

### Docker (Recommended)

The easiest way to run keycloak-config-cli is using Docker.

Mount your realm configuration files into the container (for example into `/config`) and point the CLI to the files via `IMPORT_FILES_LOCATIONS`.

For Docker environment variables, replace dots with underscores (Spring relaxed binding).

```bash
docker run --rm \
  -e KEYCLOAK_URL=https://your-keycloak-server.com \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -e KEYCLOAK_AVAILABILITYCHECK_ENABLED=true \
  -e KEYCLOAK_AVAILABILITYCHECK_TIMEOUT=120s \
  -e IMPORT_FILES_LOCATIONS=/config/* \
  -v /path/to/your/config:/config:ro \
  adorsys/keycloak-config-cli:latest
```

### Docker Compose

For more complex setups, you can use Docker Compose:

```yaml
version: '3.8'
services:
  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: https://your-keycloak-server.com
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD: admin
      KEYCLOAK_AVAILABILITYCHECK_ENABLED: "true"
      KEYCLOAK_AVAILABILITYCHECK_TIMEOUT: "120s"
      IMPORT_FILES_LOCATIONS: /config/*
    volumes:
      - ./config:/config:ro
```

### Manual Download

You can download the latest JAR file from the [GitHub releases](https://github.com/adorsys/keycloak-config-cli/releases):

```bash
wget https://github.com/adorsys/keycloak-config-cli/releases/latest/download/keycloak-config-cli.jar

java -jar keycloak-config-cli.jar \
  --keycloak.url=https://your-keycloak-server.com \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=/path/to/config/*.json
```

### Build from Source

If you want to build from source:

```bash
git clone https://github.com/adorsys/keycloak-config-cli.git
cd keycloak-config-cli
./mvnw clean package

java -jar ./target/keycloak-config-cli.jar \
  --keycloak.url=https://your-keycloak-server.com \
  --keycloak.ssl-verify=true \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=./contrib/example-config/moped.json
```

### Homebrew (macOS)

For macOS users, you can install via Homebrew:

```bash
brew tap adorsys/keycloak-config-cli
brew install keycloak-config-cli
```

## Configuration

keycloak-config-cli can be configured through:

- Environment variables
- Command line arguments
- Configuration files
- Spring Boot properties

### Environment Variables

Key environment variables include:

- `KEYCLOAK_URL`: URL of your Keycloak server
- `KEYCLOAK_USER`: Admin username
- `KEYCLOAK_PASSWORD`: Admin password
- `IMPORT_FILES_LOCATIONS`: One or more files/globs to import (for example `/config/*`)
- `IMPORT_VAR_SUBSTITUTION_ENABLED`: Enable variable substitution

### Command Line Arguments

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=https://your-keycloak-server.com \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=/path/to/config/*.json
```

## Verification

To verify your installation:

```bash
java -jar keycloak-config-cli.jar --version
```

This should display the version information and confirm that the CLI is properly installed.

## Next Steps

- [Quick Start](quick-start.md) - Create your first configuration
- [Configuration](configuration.md) - Learn about advanced configuration options
