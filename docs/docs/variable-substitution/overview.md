---
title: Variable Substitution Overview
description: Learn about variable substitution capabilities in keycloak-config-cli
sidebar_position: 1
---

# Variable Substitution Overview

keycloak-config-cli supports powerful variable substitution capabilities that allow you to create dynamic, environment-specific configurations.

## Enabling Variable Substitution

Variable substitution is **disabled by default** and must be explicitly enabled:

```bash
# Environment variable
export IMPORT_VAR_SUBSTITUTION_ENABLED=true

# Or via command line argument
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true
```

## Basic Variable Substitution

### Environment Variables

Access environment variables using the `$(variable.name)` syntax:

```json
{
  "realm": "$(REALM_NAME)",
  "enabled": true,
  "users": [
    {
      "username": "$(ADMIN_USER)",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "$(ADMIN_PASSWORD)",
          "temporary": false
        }
      ]
    }
  ]
}
```

### Spring Boot Properties

Access Spring Boot configuration properties:

```json
{
  "realm": "${spring.application.name}",
  "displayName": "${app.title}"
}
```

## JavaScript Variable Substitution

For advanced use cases, keycloak-config-cli supports JavaScript-based variable substitution.

### Enabling JavaScript Substitution

```bash
export IMPORT_VAR_SUBSTITUTION_JAVASCRIPT_ENABLED=true
```

### JavaScript Functions

You can use JavaScript functions for complex transformations:

```json
{
  "realm": "$(javascript: 'my-realm-' + new Date().getFullYear())",
  "users": [
    {
      "username": "$(javascript: 'user-' + Math.random().toString(36).substr(2, 9))",
      "enabled": true,
      "email": "$(javascript: 'user-' + Math.random().toString(36).substr(2, 9) + '@example.com')"
    }
  ]
}
```

### Available JavaScript Context

JavaScript expressions have access to:

- **Environment variables**: `env.VARIABLE_NAME`
- **System properties**: `system.property.name`
- **Built-in functions**:
  - `uuid()`: Generate random UUID
  - `timestamp()`: Current timestamp
  - `base64(string)`: Base64 encoding
  - `md5(string)`: MD5 hash

### JavaScript Examples

```json
{
  "realm": "$(javascript: env.REALM_NAME || 'default-realm')",
  "clients": [
    {
      "clientId": "$(javascript: 'app-' + env.ENVIRONMENT)",
      "secret": "$(javascript: system.getProperty('client.secret') || 'default-secret')",
      "redirectUris": [
        "$(javascript: 'https://' + env.DOMAIN + '/callback')"
      ]
    }
  ]
}
```

## Security Considerations

### Environment Variables

- Store sensitive data (passwords, secrets) in environment variables
- Use `.env` files for development (never commit to version control)
- Consider using secret management systems in production

### JavaScript Substitution

- JavaScript substitution has access to system properties
- Use with caution in production environments
- Validate JavaScript expressions before deployment

## Best Practices

### 1. Use Descriptive Variable Names

```bash
# Good
export KEYCLOAK_ADMIN_USER=admin
export KEYCLOAK_ADMIN_PASSWORD=secure-password
export APPLICATION_REALM_NAME=production

# Avoid
export USER=admin
export PASS=123
export NAME=prod
```

### 2. Provide Default Values

```json
{
  "realm": "$(javascript: env.REALM_NAME || 'development')",
  "enabled": "$(javascript: env.ENABLED === 'true')"
}
```

### 3. Separate Configuration by Environment

```bash
# development.env
REALM_NAME=dev-realm
KEYCLOAK_URL=http://localhost:8080

# production.env  
REALM_NAME=prod-realm
KEYCLOAK_URL=https://keycloak.company.com
```

### 4. Use Configuration Files

For complex setups, create separate configuration files:

```bash
# config/application-dev.properties
import.var-substitution.enabled=true
keycloak.url=http://localhost:8080
realm.name=development

# config/application-prod.properties
import.var-substitution.enabled=true
keycloak.url=https://keycloak.company.com
realm.name=production
```

## Troubleshooting

### Common Issues

1. **Variable not substituted**: Ensure variable substitution is enabled
2. **JavaScript errors**: Check syntax and available context
3. **Missing environment variables**: Verify all required variables are set

### Debug Mode

Enable debug logging to see substitution process:

```bash
java -jar keycloak-config-cli.jar \
  --logging.level.de.adorsys.keycloak.config=DEBUG \
  --import.var-substitution.enabled=true
```

### Testing Variables

Test variable substitution without applying changes:

```bash
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.dry-run=true \
  --import.files=config.json
```

## Next Steps

- [JavaScript Substitution](./javascript-substitution) - Advanced JavaScript usage
- [Environment Variables](./environment-variables) - Environment variable management
- [Configuration](../configuration/overview) - General configuration options
