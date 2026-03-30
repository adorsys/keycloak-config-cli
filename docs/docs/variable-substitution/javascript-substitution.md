---
title: JavaScript Substitution
description: Advanced JavaScript-based variable substitution
sidebar_position: 2
---

# JavaScript Substitution

JavaScript substitution provides advanced capabilities for dynamic configuration generation in keycloak-config-cli.

## Enabling JavaScript Substitution

```bash
# Enable both basic and JavaScript substitution
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export IMPORT_VAR_SUBSTITUTION_JAVASCRIPT_ENABLED=true

# Or via command line
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.var-substitution.javascript.enabled=true
```

## JavaScript Expression Syntax

JavaScript expressions use the following syntax:

```json
{
  "field": "$(javascript:your_javascript_expression_here)"
}
```

## Available Context

### Environment Variables

Access environment variables through the `env` object:

```json
{
  "realm": "$(javascript:env.REALM_NAME || 'default')",
  "enabled": "$(javascript:env.ENABLED === 'true')"
}
```

### System Properties

Access Java system properties:

```json
{
  "clientId": "$(javascript:system.getProperty('client.id') || 'default-client')"
}
```

### Built-in Functions

Utility functions available in JavaScript expressions:

#### `uuid()`

Generate a random UUID:

```json
{
  "id": "$(javascript:uuid())"
}
```

#### `timestamp()`

Get current timestamp:

```json
{
  "created": "$(javascript:timestamp())"
}
```

#### `base64(string)`

Base64 encode a string:

```json
{
  "secret": "$(javascript:base64('my-secret-value'))"
}
```

#### `md5(string)`

Generate MD5 hash:

```json
{
  "checksum": "$(javascript:md5('some-value'))"
}
```

## Advanced Examples

### Dynamic Realm Names

```json
{
  "realm": "$(javascript:'realm-' + (env.ENVIRONMENT || 'dev') + '-' + new Date().getFullYear())"
}
```

### Conditional Configuration

```json
{
  "enabled": "$(javascript:env.ENVIRONMENT === 'production')",
  "sslRequired": "$(javascript:env.SSL_ENABLED === 'true' ? 'all' : 'none')"
}
```

### User Generation

```json
{
  "users": [
    {
      "username": "$(javascript:'admin-' + (env.ADMIN_SUFFIX || '001'))",
      "enabled": true,
      "email": "$(javascript:'admin-' + (env.ADMIN_SUFFIX || '001') + '@' + (env.DOMAIN || 'localhost'))",
      "credentials": [
        {
          "type": "password",
          "value": "$(javascript:env.ADMIN_PASSWORD || 'admin123')",
          "temporary": false
        }
      ]
    }
  ]
}
```

### Client Configuration with Dynamic URLs

```json
{
  "clients": [
    {
      "clientId": "$(javascript:'app-' + env.ENVIRONMENT)",
      "name": "$(javascript:'Application (' + env.ENVIRONMENT + ')')",
      "enabled": true,
      "redirectUris": [
        "$(javascript:'https://' + (env.DOMAIN || 'localhost:3000') + '/callback')",
        "$(javascript:'https://' + (env.DOMAIN || 'localhost:3000') + '/silent-refresh')"
      ],
      "webOrigins": [
        "$(javascript:'https://' + (env.DOMAIN || 'localhost:3000'))"
      ]
    }
  ]
}
```

### Role Generation

```json
{
  "roles": {
    "realm": [
      {
        "name": "$(javascript:'user-' + new Date().getFullYear())",
        "description": "$(javascript:'Standard user role for ' + new Date().getFullYear())"
      },
      {
        "name": "$(javascript:env.ADMIN_ROLE_NAME || 'administrator')",
        "description": "$(javascript:'Administrator role with full access')"
      }
    ]
  }
}
```

## Complex Logic Examples

### Multi-Environment Configuration

```json
{
  "realm": "$(javascript:" +
    "  env.ENVIRONMENT === 'prod' ? 'production-realm' : " +
    "  env.ENVIRONMENT === 'staging' ? 'staging-realm' : " +
    "  'development-realm'" +
    ")",
  "enabled": "$(javascript:env.ENVIRONMENT !== 'maintenance')",
  "registrationAllowed": "$(javascript:env.ENVIRONMENT === 'development')"
}
```

### Dynamic User Creation

```json
{
  "users": "$(javascript:" +
    "  const users = [];" +
    "  const userCount = parseInt(env.USER_COUNT || '3');" +
    "  for (let i = 1; i <= userCount; i++) {" +
    "    users.push({" +
    "      username: 'user' + String(i).padStart(3, '0')," +
    "      enabled: true," +
    "      email: 'user' + String(i).padStart(3, '0') + '@' + (env.DOMAIN || 'example.com')," +
    "      firstName: 'User ' + i," +
    "      lastName: 'Test'," +
    "      credentials: [{" +
    "        type: 'password'," +
    "        value: env.DEFAULT_PASSWORD || 'password123'," +
    "        temporary: false" +
    "      }]" +
    "    });" +
    "  }" +
    "  return users;" +
    ")"
}
```

## Security Considerations

### Input Validation

Always validate and sanitize inputs:

```json
{
  "realm": "$(javascript:" +
    "  const realm = env.REALM_NAME || '';" +
    "  if (!/^[a-zA-Z0-9-]+$/.test(realm)) {" +
    "    throw new Error('Invalid realm name: ' + realm);" +
    "  }" +
    "  return realm;" +
    ")"
}
```

### Sensitive Data Handling

Avoid exposing sensitive information in error messages:

```json
{
  "clients": [
    {
      "clientId": "$(javascript:env.CLIENT_ID || 'default-client')",
      "secret": "$(javascript:" +
        "  const secret = env.CLIENT_SECRET;" +
        "  if (!secret) throw new Error('CLIENT_SECRET required');" +
        "  return secret;" +  // Will be masked in logs
        ")"
    }
  ]
}
```

## Performance Considerations

### Efficient JavaScript

- Keep expressions simple and fast
- Avoid complex loops in large arrays
- Cache expensive operations

### Example: Efficient vs Inefficient

```json
// Efficient - simple operations
{
  "realm": "$(javascript:env.REALM_NAME || 'default')",
  "enabled": "$(javascript:env.ENVIRONMENT !== 'test')"
}

// Inefficient - complex operations
{
  "users": "$(javascript:" +
    "  // This creates 1000 users and may be slow" +
    "  const users = [];" +
    "  for (let i = 0; i < 1000; i++) {" +
    "    users.push(/* complex user object */);" +
    "  }" +
    "  return users;" +
    ")"
}
```

## Error Handling

### Try-Catch Patterns

```json
{
  "realm": "$(javascript:" +
    "  try {" +
    "    return env.REALM_NAME;" +
    "  } catch (e) {" +
    "    console.error('Error getting realm name:', e);" +
    "    return 'default-realm';" +
    "  }" +
    ")"
}
```

### Default Values

Always provide sensible defaults:

```json
{
  "sslRequired": "$(javascript:" +
    "  const ssl = env.SSL_REQUIRED;" +
    "  if (ssl === 'true') return 'all';" +
    "  if (ssl === 'false') return 'none';" +
    "  return 'external';" +  // default
    ")"
}
```

## Testing JavaScript Expressions

### Dry Run Mode

Test expressions without applying changes:

```bash
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.var-substitution.javascript.enabled=true \
  --import.dry-run=true \
  --import.files=config-with-js.json
```

### Debug Output

Enable debug logging to see evaluation results:

```bash
java -jar keycloak-config-cli.jar \
  --logging.level.de.adorsys.keycloak.config=DEBUG \
  --import.var-substitution.javascript.enabled=true
```

## Best Practices

1. **Keep it Simple**: Use JavaScript only when necessary
2. **Provide Defaults**: Always have fallback values
3. **Validate Input**: Check and validate environment variables
4. **Test Thoroughly**: Use dry-run mode before production
5. **Document Logic**: Comment complex expressions
6. **Consider Performance**: Avoid expensive operations

## Next Steps

- [Environment Variables](./environment-variables) - Environment variable management
- [Configuration](../configuration/overview) - General configuration options
- [Docker & Helm](../docker-helm/docker-usage) - Container deployment
