# Skip Server Info

This section covers the `--keycloak.skip-server-info` configuration option for keycloak-config-cli.

## Overview

When using non-master realm authentication, keycloak-config-cli needs to skip fetching Keycloak server information to avoid authentication issues.

## Configuration

### Command Line Option

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.skip-server-info=true \
  --import.files=config.json
```

### Environment Variable

```bash
export KEYCLOAK_SKIPSERVERINFO=true
java -jar keycloak-config-cli.jar \
  --import.files=config.json
```

## Use Cases

### Non-Master Realm Authentication

When authenticating against a non-master realm, the standard server info endpoint may not be accessible or may return incorrect information.

**Example Configuration:**
```json
{
  "realm": "my-realm",
  "users": [
    {
      "username": "service-account",
      "enabled": true
    }
  ]
}
```

### Service Account Authentication

When using service accounts with specific realm access, skipping server info can prevent permission conflicts.

**Example Configuration:**
```json
{
  "realm": "restricted-realm",
  "clients": [
    {
      "clientId": "my-service",
      "secret": "service-secret"
    }
  ]
}
```

### Custom Authentication Flows

For custom authentication setups where the standard Keycloak endpoints are not available or modified.

**Example Configuration:**
```json
{
  "realm": "custom-realm",
  "authentication": {
    "customFlow": true,
    "endpoint": "https://custom.auth.example.com"
  }
}
```

## Benefits

1. **Faster Startup**: Skip server info retrieval reduces initialization time
2. **Reduced Errors**: Avoids authentication conflicts with non-master realms
3. **Better Security**: Reduces exposure to potentially sensitive server information
4. **Custom Compatibility**: Works with custom authentication setups

## Advanced Configuration

### Conditional Skipping

```bash
# Skip only for specific realms
java -jar keycloak-config-cli.jar \
  --keycloak.skip-server-info=true \
  --import.realm=non-master-realm \
  --import.files=non-master-config.json

# Use server info for master realm
java -jar keycloak-config-cli.jar \
  --keycloak.skip-server-info=false \
  --import.realm=master-realm \
  --import.files=master-config.json
```

### Environment-Based Configuration

```bash
# Development environment
if [ "$ENVIRONMENT" = "development" ]; then
  export KEYCLOAK_SKIPSERVERINFO=true
fi

# Production environment
if [ "$ENVIRONMENT" = "production" ]; then
  export KEYCLOAK_SKIPSERVERINFO=false
fi

java -jar keycloak-config-cli.jar \
  --import.files=config.json
```

## Troubleshooting

### Common Issues

#### Authentication Fails

**Error:** `Authentication failed: Unable to retrieve server information`

**Solution:** Ensure `--keycloak.skip-server-info=true` is set for non-master realms

#### Permission Denied

**Error:** `Access denied: Insufficient permissions for server info`

**Solution:** Use appropriate service account with realm access

#### Connection Timeout

**Error:** `Connection timeout: Unable to reach Keycloak server`

**Solution:** Check network connectivity and server availability

### Debug Mode

```bash
# Enable debug logging for server info
java -jar keycloak-config-cli.jar \
  --keycloak.skip-server-info=true \
  --logging.level.keycloak=DEBUG \
  --import.files=config.json
```

## Best Practices

1. **Environment Detection**: Use environment variables to control skipping behavior
2. **Realm-Specific Configuration**: Different settings for different realm types
3. **Error Handling**: Implement proper error handling for authentication failures
4. **Testing**: Test both with and without server info skipping
5. **Documentation**: Document authentication requirements for each realm

## Related Topics

- [Configuration](../config/overview.md) - General configuration options
- [Non-Master Realm Authentication](../config/non-master-realm-authentication.md) - Detailed authentication setup
- [Import Settings](../config/import-settings.md) - Import configuration options
