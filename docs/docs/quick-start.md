---
title: Quick Start
description: Get up and running with keycloak-config-cli in minutes
sidebar_position: 3
---

# Quick Start

This guide will help you get keycloak-config-cli running with a basic configuration in just a few minutes.

## Step 1: Prepare Your Keycloak Environment

Make sure you have:
- A running Keycloak instance
- Admin credentials for Keycloak

## Step 2: Create a Basic Configuration

Create a simple realm configuration file `realm-config.json`:

```json
{
  "realm": "my-realm",
  "enabled": true,
  "displayName": "My Realm",
  "loginTheme": "keycloak",
  "users": [
    {
      "username": "test-user",
      "enabled": true,
      "firstName": "Test",
      "lastName": "User",
      "email": "test@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "test-password",
          "temporary": false
        }
      ]
    }
  ],
  "clients": [
    {
      "clientId": "my-client",
      "name": "My Client",
      "enabled": true,
      "publicClient": true,
      "directAccessGrantsEnabled": true,
      "standardFlowEnabled": true,
      "redirectUris": ["http://localhost:3000/*"]
    }
  ]
}
```

## Step 3: Run keycloak-config-cli

### Using Docker

```bash
docker run --rm \
  -e KEYCLOAK_URL=http://localhost:8080 \
  -e KEYCLOAK_USER=admin \
  -e KEYCLOAK_PASSWORD=admin \
  -v $(pwd)/realm-config.json:/config/realm-config.json \
  adorsys/keycloak-config-cli:latest
```

### Using JAR file

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files=realm-config.json
```

## Step 4: Verify the Results

After running the command, you should see output indicating successful import. Check your Keycloak admin console:

1. Navigate to your Keycloak admin console
2. Select the "my-realm" realm
3. Verify that:
   - The realm exists and is enabled
   - The "test-user" has been created
   - The "my-client" has been configured

## Step 5: Make Changes

Modify your configuration file and run the command again. keycloak-config-cli will:

- Update existing resources
- Create new resources
- Leave unchanged resources as-is

Example modification:

```json
{
  "realm": "my-realm",
  "enabled": true,
  "displayName": "My Updated Realm",
  "users": [
    {
      "username": "test-user",
      "enabled": true,
      "firstName": "Test",
      "lastName": "User",
      "email": "test@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "test-password",
          "temporary": false
        }
      ]
    },
    {
      "username": "second-user",
      "enabled": true,
      "firstName": "Second",
      "lastName": "User",
      "email": "second@example.com",
      "credentials": [
        {
          "type": "password",
          "value": "second-password",
          "temporary": false
        }
      ]
    }
  ],
  "clients": [
    {
      "clientId": "my-client",
      "name": "My Client",
      "enabled": true,
      "publicClient": true,
      "directAccessGrantsEnabled": true,
      "standardFlowEnabled": true,
      "redirectUris": ["http://localhost:3000/*"]
    }
  ]
}
```

## Best Practices

- **Keep configurations minimal**: Only include what you need to change
- **Use version control**: Store your configuration files in git
- **Test in development**: Always test configurations in a non-production environment first
- **Use variable substitution**: For environment-specific values

## Next Steps

- [Configuration](./configuration/overview) - Learn about advanced configuration options
- [Variable Substitution](https://github.com/adorsys/keycloak-config-cli/blob/main/docs-backup/javascript-substitution.md) - Use dynamic values in your configurations
- [Docker & Helm](https://github.com/adorsys/keycloak-config-cli#docker) - Deploy in containerized environments

## Troubleshooting

### Common Issues

1. **Authentication failures**: Verify your Keycloak URL and credentials
2. **Import failures**: Check your JSON syntax and structure
3. **Permission issues**: Ensure the user has sufficient permissions in Keycloak

### Debug Mode

Enable debug logging for more detailed output:

```bash
java -jar keycloak-config-cli.jar \
  --logging.level.de.adorsys.keycloak.config=DEBUG \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files=realm-config.json
```
