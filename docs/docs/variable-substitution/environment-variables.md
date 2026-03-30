---
title: Environment Variables
description: Managing environment variables for keycloak-config-cli
sidebar_position: 3
---

# Environment Variables

Environment variables are essential for creating dynamic, environment-specific configurations with keycloak-config-cli.

## Keycloak Configuration Variables

### Connection Settings

```bash
# Keycloak server URL
export KEYCLOAK_URL=https://your-keycloak-server.com

# Authentication credentials
export KEYCLOAK_USER=admin
export KEYCLOAK_PASSWORD=admin-password

# Realm to manage
export KEYCLOAK_REALM=master
```

### SSL/TLS Configuration

```bash
# SSL verification
export KEYCLOAK_TRUSTSTORE=/path/to/truststore.jks
export KEYCLOAK_TRUSTSTORE_PASSWORD=changeit
export KEYCLOAK_SSL_SKIP_VERIFICATION=true
```

## Import Configuration Variables

### Basic Import Settings

```bash
# Enable variable substitution
export IMPORT_VAR_SUBSTITUTION_ENABLED=true

# Enable JavaScript substitution
export IMPORT_VAR_SUBSTITUTION_JAVASCRIPT_ENABLED=true

# Import path
export IMPORT_PATH=/path/to/config/files

# Import files (comma-separated)
export IMPORT_FILES=realm.json,users.json,clients.json
```

### Import Behavior

```bash
# Import strategy
export IMPORT_STRATEGY=CREATE_OR_UPDATE

# Skip server info check
export IMPORT_SKIP_SERVER_INFO=true

# Parallel import
export IMPORT_PARALLEL=true

# Connection timeout
export IMPORT_CONNECTION_TIMEOUT=30000
```

## Application Configuration Variables

### Logging Configuration

```bash
# Log level
export LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG=DEBUG

# Log file
export LOGGING_FILE_NAME=keycloak-config.log

# Log pattern
export LOGGING_PATTERN=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Spring Boot Properties

```bash
# Application name
export SPRING_APPLICATION_NAME=keycloak-config-cli

# Profile
export SPRING_PROFILES_ACTIVE=production

# Server port
export SERVER_PORT=8080
```

## Environment-Specific Variables

### Development Environment

```bash
# .env.development
export KEYCLOAK_URL=http://localhost:8080
export KEYCLOAK_USER=admin
export KEYCLOAK_PASSWORD=admin
export REALM_NAME=development-realm
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG=DEBUG
```

### Staging Environment

```bash
# .env.staging
export KEYCLOAK_URL=https://staging-keycloak.company.com
export KEYCLOAK_USER=admin
export KEYCLOAK_PASSWORD=${STAGING_ADMIN_PASSWORD}
export REALM_NAME=staging-realm
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG=INFO
```

### Production Environment

```bash
# .env.production
export KEYCLOAK_URL=https://keycloak.company.com
export KEYCLOAK_USER=admin
export KEYCLOAK_PASSWORD=${PROD_ADMIN_PASSWORD}
export REALM_NAME=production-realm
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG=WARN
export IMPORT_SSL_REQUIRED=all
```

## Custom Application Variables

### Database Configuration

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=keycloak
export DB_USER=keycloak
export DB_PASSWORD=${DB_PASSWORD}
```

### Application Settings

```bash
export APPLICATION_NAME=MyApp
export APPLICATION_VERSION=1.0.0
export ADMIN_EMAIL=admin@company.com
export SUPPORT_EMAIL=support@company.com
```

### Feature Flags

```bash
export FEATURE_USER_REGISTRATION=true
export FEATURE_SOCIAL_LOGIN=true
export FEATURE_MULTI_FACTOR_AUTH=false
export FEATURE_BRAUTE_FORCE_PROTECTION=true
```

## Managing Environment Variables

### Using .env Files

Create environment-specific `.env` files:

```bash
# .env
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_USER=admin
KEYCLOAK_PASSWORD=admin
REALM_NAME=development
```

Load environment variables:

```bash
# Using direnv (recommended)
direnv allow

# Using source
source .env

# Using export
export $(cat .env | xargs)
```

### Docker Environment Variables

In Docker Compose:

```yaml
version: '3.8'
services:
  keycloak-config-cli:
    image: adorsys/keycloak-config-cli:latest
    environment:
      KEYCLOAK_URL: https://keycloak.company.com
      KEYCLOAK_USER: admin
      KEYCLOAK_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      REALM_NAME: ${REALM_NAME}
      IMPORT_VAR_SUBSTITUTION_ENABLED: "true"
    volumes:
      - ./config:/config
      - ./.env:/app/.env:ro
```

### Kubernetes Environment Variables

In Kubernetes ConfigMaps:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: keycloak-config-env
data:
  KEYCLOAK_URL: "https://keycloak.company.com"
  REALM_NAME: "production"
  IMPORT_VAR_SUBSTITUTION_ENABLED: "true"
---
apiVersion: v1
kind: Secret
metadata:
  name: keycloak-config-secrets
type: Opaque
data:
  KEYCLOAK_USER: <base64-encoded>
  KEYCLOAK_PASSWORD: <base64-encoded>
```

## Security Best Practices

### Sensitive Data

Never commit sensitive environment variables to version control:

```bash
# .gitignore
.env
.env.local
.env.*.local
secrets/
*.key
*.pem
```

Use secret management systems:

```bash
# AWS Secrets Manager
export KEYCLOAK_PASSWORD=$(aws secretsmanager get-secret-value --secret-id keycloak-admin-password --query SecretString --output text)

# HashiCorp Vault
export KEYCLOAK_PASSWORD=$(vault kv get -field=password secret/keycloak/admin)

# Azure Key Vault
export KEYCLOAK_PASSWORD=$(az keyvault secret show --vault-name my-vault --name keycloak-admin-password --query value -o tsv)
```

### Environment Variable Validation

Validate required variables in scripts:

```bash
#!/bin/bash
# validate-env.sh

required_vars=(
  "KEYCLOAK_URL"
  "KEYCLOAK_USER" 
  "KEYCLOAK_PASSWORD"
  "REALM_NAME"
)

missing_vars=()

for var in "${required_vars[@]}"; do
  if [[ -z "${!var}" ]]; then
    missing_vars+=("$var")
  fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
  echo "Error: Missing required environment variables:"
  printf '  %s\n' "${missing_vars[@]}"
  exit 1
fi

echo "All required environment variables are set"
```

## Variable Substitution Examples

### Basic Substitution

```json
{
  "realm": "${REALM_NAME}",
  "enabled": "${ENABLED:true}",
  "displayName": "${APPLICATION_DISPLAY_NAME}"
}
```

### Default Values

```json
{
  "realm": "${REALM_NAME:default-realm}",
  "sslRequired": "${SSL_REQUIRED:external}",
  "registrationAllowed": "${REGISTRATION_ALLOWED:false}"
}
```

### Conditional Configuration

```json
{
  "realm": "${REALM_NAME}",
  "enabled": true,
  "bruteForceProtected": "${BRAUTE_FORCE_PROTECTION:true}",
  "passwordPolicy": "${ENVIRONMENT:development}" === "production" ? 
    "hashAlgorithm and passwordHistory and digits and specialChars and length" : 
    "length"
}
```

## Troubleshooting

### Common Issues

1. **Variables not available**: Check if `.env` file is loaded
2. **Permission denied**: Verify file permissions for `.env` files
3. **Special characters**: Quote variables with special characters
4. **Empty variables**: Use default values in substitution syntax

### Debug Environment Variables

Check loaded variables:

```bash
# List all environment variables
env | grep KEYCLOAK
env | grep IMPORT
env | grep REALM

# Check specific variable
echo "KEYCLOAK_URL is: $KEYCLOAK_URL"
echo "REALM_NAME is: $REALM_NAME"
```

### Variable Expansion Test

Create a test configuration to verify substitution:

```json
{
  "realm": "${REALM_NAME:test-realm}",
  "test": {
    "keycloak_url": "${KEYCLOAK_URL:not-set}",
    "user": "${KEYCLOAK_USER:not-set}",
    "import_enabled": "${IMPORT_VAR_SUBSTITUTION_ENABLED:false}"
  }
}
```

## Best Practices

1. **Use Descriptive Names**: Clear, meaningful variable names
2. **Group by Environment**: Separate `.env` files per environment
3. **Document Variables**: Maintain documentation of required variables
4. **Validate Early**: Check required variables before running
5. **Secure Secrets**: Use proper secret management
6. **Version Control**: Exclude sensitive files from git

## Next Steps

- [JavaScript Substitution](./javascript-substitution) - Advanced substitution techniques
- [Configuration](../configuration/overview) - General configuration options
- [Docker & Helm](../docker-helm/docker-usage) - Container deployment
