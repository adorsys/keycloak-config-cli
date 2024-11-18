# Table of Contents

- [Overview](#overview)
- [Environment Setup](#environment-setup)
- [Creating the Keycloak Configuration Import Script](#creating-the-keycloak-configuration-import-script)
- [JSON Configuration File: State Management Example](#json-configuration-file-state-management-example)
- [Verifying Remote State Management](#verifying-remote-state-management)
- [Conclusion](#conclusion)

## Overview

### What is Keycloak?

Keycloak is an open-source identity and access management solution that simplifies authentication and authorization processes. When running Keycloak in Docker, it is crucial to ensure remote state management, which helps track configuration changes while preserving existing resources. This guide highlights how to use the keycloak-config-cli tool, which allows you to manage configurations efficiently.

### Benefits of Remote State Management

Remote state management ensures that configurations are tracked and only modified when necessary, enabling safe and controlled updates to Keycloak configurations without altering unmanaged resources.

## Environment Setup

### Directory Structure

Ensure your project directory has the following structure:

```plaintext
keycloak-setup/
├── docker-compose.yml
├── import.sh
├── keycloak-config-cli.jar
└── realms/
└── state-management.json
```

Each file serves the following purpose:

- `docker-compose.yml`: Defines the Keycloak service.
- `import.sh`: Custom shell script for running the Keycloak Config CLI against our Keycloak instance.
- `keycloak-config-cli.jar`: Keycloak-config-cli is compatible with different versions of Keycloak and actively maintained.
- `realms/state-management.json`: JSON file with realm configuration.

### Docker Compose Configuration

In `docker-compose.yml`, configure the Keycloak service without a Keycloak Config CLI container, as we will be handling imports manually in this case.

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:25.0.1
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: localhost
      KC_HTTP_PORT: "8080"
      KC_HTTP_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
      KC_HEALTH_ENABLED: "true"
    ports:
      - "8080:8080"
    volumes:
      - ./realms:/opt/keycloak/data/import:z
    command:
      - "start-dev"
```

This file configures Keycloak with essential parameters and maps `./realms` for importing configuration files.

## Creating the Keycloak Configuration Import Script

The `import.sh` script uses the `keycloak-config-cli.jar` to apply configurations. This script will:

1. Copy the config file to the container.
2. Run the import using the CLI JAR file, with remote state management enabled.

### Script Functionality

Create `import.sh` as follows:

```bash
#!/bin/bash

# Check if a configuration file is provided
if [ -z "$1" ]; then
  echo "Usage: ./import.sh <config-file>"
  exit 1
fi

CONFIG_FILE=$1

# Run the Keycloak Config CLI tool with the updated options
java -jar keycloak-config-cli.jar \
  --keycloak.url="http://localhost:8080" \
  --keycloak.user="admin" \
  --keycloak.password="admin" \
  --import.managed.group="full" \
  --import.remote-state.enabled="true" \
  --import.files.locations="$CONFIG_FILE"

echo "Import of $CONFIG_FILE with remote-state enabled is complete."
```

## JSON Configuration File: State Management Example

Create `state-management.json` under `realms/`, which defines a Keycloak realm, clients, and roles.

### Defining a Realm Configuration

Define the realm, clients, roles, and scope mappings for demonstration:

```json
{
  "realm": "master",
  "enabled": true,
  "clients": [
    {
      "clientId": "imported-client",
      "enabled": true,
      "protocol": "openid-connect",
      "fullScopeAllowed": false
    }
  ],
  "roles": {
    "realm": [
      {
        "name": "my-role",
        "description": "A test role"
      }
    ]
  },
  "clientScopes": [
    {
      "name": "custom-scope",
      "description": "Custom client scope",
      "protocol": "openid-connect",
      "attributes": {
        "include.in.token.scope": "true"
      }
    }
  ],
  "scopeMappings": [
    {
      "client": "imported-client",
      "roles": [
        "my-role"
      ]
    }
  ]
}
```

## Verifying Remote State Management

With remote state management enabled, Keycloak Config CLI will only modify resources it initially created, preserving custom or manually added configurations.

### Starting Keycloak

To start Keycloak, run:

```shell
docker-compose up -d
```

### Testing Remote State

Manually create a dedicated client to test remote state management.

##  Conclusion

In this guide, we covered the basics of setting up Keycloak with Docker, creating an import script for configuration, and enabling remote state management using `keycloak-config-cli`.

Feel free to reach out if you have any questions or need further assistance!