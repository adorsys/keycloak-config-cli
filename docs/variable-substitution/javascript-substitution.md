---
title: JavaScript Substitution
description: Advanced JavaScript-based variable substitution
sidebar_position: 2
---

# JavaScript Substitution

JavaScript substitution provides advanced capabilities for dynamic configuration generation in keycloak-config-cli.

## Enabling JavaScript Substitution

#### Enable both basic and JavaScript substitution

```bash
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export IMPORT_VAR_SUBSTITUTION_JAVASCRIPT_ENABLED=true
```

#### Or via command line

```bash
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
  "realm": "$${javascript: 'realm-' + (env.APP_ENV || 'default').toLowerCase()}",
  "enabled": true
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


## Some Examples

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


## Advanced Use Cases

### Dynamic User Generation with Roles

```json
{
  "users": "$(javascript:" +
    "  const users = [];" +
    "  const count = parseInt(env.USER_COUNT || '3');" +
    "  for (let i = 1; i <= count; i++) {" +
    "    users.push({" +
    "      username: 'user' + String(i).padStart(3, '0')," +
    "      enabled: true," +
    "      email: 'user' + String(i).padStart(3, '0') + '@' + (env.DOMAIN || 'example.com')," +
    "      firstName: 'User ' + i," +
    "      lastName: 'Test'," +
    "      realmRoles: ['user']," +
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

### Multi-Environment Configuration

```json
{
  "realm": "$(javascript:" +
    "  const env = env.ENVIRONMENT || 'development';" +
    "  const map = {" +
    "    'dev': 'development-realm'," +
    "    'staging': 'staging-realm'," +
    "    'prod': 'production-realm'" +
    "  };" +
    "  return map[env] || 'default-realm';" +
    ")",
  "enabled": "$(javascript: env.ENVIRONMENT !== 'maintenance')",
  "sslRequired": "$(javascript: ['production', 'staging'].includes(env.ENVIRONMENT) ? 'all' : 'none')",
  "registrationAllowed": "$(javascript: env.ENVIRONMENT === 'development')"
}
```

### Dynamic Client Configuration

```json
{
  "clients": [
    {
      "clientId": "$(javascript: 'app-' + (env.ENVIRONMENT || 'dev'))",
      "name": "$(javascript: (env.APP_NAME || 'My App') + ' (' + (env.ENVIRONMENT || 'dev') + ')')",
      "enabled": true,
      "redirectUris": "$(javascript:" +
        "  const base = env.APP_URL || 'https://example.com';" +
        "  return [base + '/callback', base + '/silent-renew'];" +
        ")",
      "webOrigins": "$(javascript:" +
        "  const base = env.APP_URL || 'https://example.com';" +
        "  return [base];" +
        ")",
      "attributes": {
        "environment": "$(javascript: env.ENVIRONMENT || 'dev')",
        "deployed_at": "$(javascript: new Date().toISOString())",
        "version": "$(javascript: env.APP_VERSION || '1.0.0')"
      }
    }
  ]
}
```

---


## Best Practices

1. **Keep it Simple**: Use JavaScript only when necessary
2. **Provide Defaults**: Always have fallback values
3. **Validate Input**: Check and validate environment variables
4. **Test Thoroughly**: Use dry-run mode before production
5. **Document Logic**: Comment complex expressions
6. **Consider Performance**: Avoid expensive operations
7. **Use Type Coercion**: Explicitly convert types when needed
8. **Handle Errors**: Implement proper error handling
9. **Avoid Side Effects**: Keep expressions pure and predictable
10. **Security First**: Never expose sensitive data in expressions

---

## Complete Example

### Comprehensive Configuration

**realm.json:**
```json
{
  "realm": "$(javascript:" +
    "  const env = env.ENVIRONMENT || 'development';" +
    "  const map = {'dev': 'dev-realm', 'staging': 'staging-realm', 'prod': 'prod-realm'};" +
    "  return map[env] || 'default-realm';" +
    ")",
  "displayName": "$(javascript: (env.APP_NAME || 'My App') + ' - ' + (env.ENVIRONMENT || 'dev'))",
  "enabled": "$(javascript: env.ENVIRONMENT !== 'maintenance')",
  "sslRequired": "$(javascript: ['production', 'staging'].includes(env.ENVIRONMENT) ? 'all' : 'none')",
  "registrationAllowed": "$(javascript: env.ENVIRONMENT === 'development')",
  "attributes": "$(javascript:" +
    "  return {" +
    "    environment: env.ENVIRONMENT || 'development'," +
    "    version: env.APP_VERSION || '1.0.0'," +
    "    deployed_at: new Date().toISOString()," +
    "    company: env.COMPANY || 'Default Company'" +
    "  };" +
    ")",
  "clients": [
    {
      "clientId": "$(javascript: 'app-' + (env.ENVIRONMENT || 'dev'))",
      "enabled": true,
      "redirectUris": "$(javascript:" +
        "  const base = env.APP_URL || 'https://example.com';" +
        "  return [base + '/callback', base + '/silent-renew'];" +
        ")",
      "webOrigins": "$(javascript: [env.APP_URL || 'https://example.com'])"
    }
  ]
}
```

---

## Next Steps

- [Overview](overview.md) - Variable substitution introduction
- [Environment Variables](environment-variables.md) - Environment variable management
- [File Operations](file-operations.md) - File content and properties
- [Encoding & Decoding](encoding-decoding.md) - Base64 and URL operations
- [Network Operations](network-operations.md) - DNS and URL content
- [Java Integration](java-integration.md) - Java constants and version
- [System Information](system-information.md) - Date and localhost information
- [Configuration](../config/overview.md) - General configuration options
