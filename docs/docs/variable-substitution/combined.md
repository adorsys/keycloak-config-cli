# Variable Substitution

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

## Configuration Options

| CLI Option | ENV Variable | Description | Default |
|------------|--------------|-------------|---------|
| `--import.var-substitution.enabled` | `IMPORT_VARSUBSTITUTION_ENABLED` | Enable variable substitution | `false` |
| `--import.var-substitution.nested` | `IMPORT_VARSUBSTITUTION_NESTED` | Expand variables in variables | `true` |
| `--import.var-substitution.undefined-is-error` | `IMPORT_VARSUBSTITUTION_UNDEFINEDISTERROR` | Raise exceptions if variables are not defined | `true` |
| `--import.var-substitution.prefix` | `IMPORT_VARSUBSTITUTION_PREFIX` | Variable prefix | `$(` |
| `--import.var-substitution.suffix` | `IMPORT_VARSUBSTITUTION_SUFFIX` | Variable suffix | `)` |

## Basic Variable Substitution

### Environment Variables

Access environment variables using the `$(env:VARIABLE_NAME)` syntax:

```json
{
  "realm": "$(env:REALM_NAME)",
  "enabled": true,
  "users": [
    {
      "username": "$(env:ADMIN_USER)",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "$(env:ADMIN_PASSWORD)",
          "temporary": false
        }
      ]
    }
  ]
}
```

### Spring Boot Properties

Variables exposed by Spring Boot can be accessed by `$(property.name)`:

```json
{
  "realm": "$(spring.application.name)",
  "displayName": "$(app.title)"
}
```

### Default Values

Provide default values using the `:` separator:

```json
{
  "realm": "$(REALM_NAME:default-realm)",
  "sslRequired": "$(SSL_REQUIRED:external)",
  "registrationAllowed": "$(REGISTRATION_ALLOWED:false)"
}
```

## Supported Prefixes

The string substitution supports multiple prefixes for different approaches:

| Prefix | Example | Description |
|--------|---------|-------------|
| `base64Decoder:` | `$(base64Decoder:SGVsbG9Xb3JsZCE=)` | Base64 decode |
| `base64Encoder:` | `$(base64Encoder:HelloWorld!)` | Base64 encode |
| `const:` | `$(const:java.awt.event.KeyEvent.VK_ESCAPE)` | Java constant |
| `date:` | `$(date:yyyy-MM-dd)` | Current date formatted |
| `dns:` | `$(dns:address\|apache.org)` | DNS lookup |
| `env:` | `$(env:USERNAME)` | Environment variable |
| `file:` | `$(file:UTF-8:src/test/resources/document.properties)` | File content |
| `java:` | `$(java:version)` | Java info |
| `localhost:` | `$(localhost:canonical-name)` | Localhost info |
| `properties:` | `$(properties:src/test/resources/document.properties::mykey)` | Properties file value |
| `resourceBundle:` | `$(resourceBundle:org.example.testResourceBundleLookup:mykey)` | Resource bundle |
| `script:` | `$(script:javascript:3 + 4)` | Script execution |
| `sys:` | `$(sys:user.dir)` | System property |
| `urlDecoder:` | `$(urlDecoder:Hello%20World%21)` | URL decode |
| `urlEncoder:` | `$(urlEncoder:Hello World!)` | URL encode |
| `url:` | `$(url:UTF-8:http://www.apache.org)` | URL content |
| `xml:` | `$(xml:src/test/resources/document.xml:/root/path/to/node)` | XML XPath |

### Recursive Substitution

Recursive variable replacement is enabled by default:

```json
{
  "password": "$(file:UTF-8:$(env:KEYCLOAK_PASSWORD_FILE))"
}
```

**Note**: Variable substitution runs before the JSON parser executes. This allows JSON structures or complex values.

**Important**: Since variable substitution is part of keycloak-config-cli, it's done locally. Environment variables must be available where keycloak-config-cli is executed.

**Escaping**: If `import.var-substitution.prefix=${` and `import.var-substitution.suffix=}` (default in keycloak-config-cli 3.x) is set, Keycloak builtin variables like `${role_uma_authorization}` need to be escaped as `$${role_uma_authorization}`.

## JavaScript Substitution

For advanced use cases, enable JavaScript-based variable substitution:

```bash
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export IMPORT_VAR_SUBSTITUTION_JAVASCRIPT_ENABLED=true

# Or via command line
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.var-substitution.javascript.enabled=true
```

### JavaScript Expression Syntax

```json
{
  "field": "$(javascript:your_javascript_expression_here)"
}
```

### Available Context

#### Environment Variables

Access through the `env` object:

```json
{
  "realm": "$(javascript:env.REALM_NAME || 'default')",
  "enabled": "$(javascript:env.ENABLED === 'true')"
}
```

#### System Properties

Access Java system properties:

```json
{
  "clientId": "$(javascript:system.getProperty('client.id') || 'default-client')"
}
```

#### Built-in Functions

| Function | Description | Example |
|----------|-------------|---------|
| `uuid()` | Generate random UUID | `$(javascript:uuid())` |
| `timestamp()` | Current timestamp | `$(javascript:timestamp())` |
| `base64(string)` | Base64 encoding | `$(javascript:base64('my-secret-value'))` |
| `md5(string)` | MD5 hash | `$(javascript:md5('some-value'))` |

### JavaScript Examples

#### Dynamic Realm Names

```json
{
  "realm": "$(javascript:'realm-' + (env.ENVIRONMENT || 'dev') + '-' + new Date().getFullYear())"
}
```

#### Conditional Configuration

```json
{
  "enabled": "$(javascript:env.ENVIRONMENT === 'production')",
  "sslRequired": "$(javascript:env.SSL_ENABLED === 'true' ? 'all' : 'none')"
}
```

#### User Generation

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

#### Client Configuration with Dynamic URLs

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

#### Role Generation

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

#### Multi-Environment Configuration

```json
{
  "realm": "$(javascript:env.ENVIRONMENT === 'prod' ? 'production-realm' : env.ENVIRONMENT === 'staging' ? 'staging-realm' : 'development-realm')",
  "enabled": "$(javascript:env.ENVIRONMENT !== 'maintenance')",
  "registrationAllowed": "$(javascript:env.ENVIRONMENT === 'development')"
}
```

#### Dynamic User Creation

```json
{
  "users": "$(javascript:const users = []; const userCount = parseInt(env.USER_COUNT || '3'); for (let i = 1; i <= userCount; i++) { users.push({ username: 'user' + String(i).padStart(3, '0'), enabled: true, email: 'user' + String(i).padStart(3, '0') + '@' + (env.DOMAIN || 'example.com'), firstName: 'User ' + i, lastName: 'Test', credentials: [{ type: 'password', value: env.DEFAULT_PASSWORD || 'password123', temporary: false }] }); } return users;)"
}
```

#### Input Validation

```json
{
  "realm": "$(javascript:const realm = env.REALM_NAME || ''; if (!/^[a-zA-Z0-9-]+$/.test(realm)) { throw new Error('Invalid realm name: ' + realm); } return realm;)"
}
```

#### Try-Catch Error Handling

```json
{
  "realm": "$(javascript:try { return env.REALM_NAME; } catch (e) { console.error('Error getting realm name:', e); return 'default-realm'; })"
}
```

## Environment-Specific Configuration

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
```

### Production Environment

```bash
# .env.production
export KEYCLOAK_URL=https://keycloak.company.com
export KEYCLOAK_USER=admin
export KEYCLOAK_PASSWORD=${PROD_ADMIN_PASSWORD}
export REALM_NAME=production-realm
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export IMPORT_SSL_REQUIRED=all
```

## Container Deployment

### Docker

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
```

### Kubernetes

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

## Security Considerations

### Environment Variables

- Store sensitive data (passwords, secrets) in environment variables
- Use `.env` files for development (never commit to version control)
- Consider using secret management systems in production

```bash
# .gitignore
.env
.env.local
.env.*.local
secrets/
*.key
*.pem
```

### Secret Management Systems

```bash
# AWS Secrets Manager
export KEYCLOAK_PASSWORD=$(aws secretsmanager get-secret-value --secret-id keycloak-admin-password --query SecretString --output text)

# HashiCorp Vault
export KEYCLOAK_PASSWORD=$(vault kv get -field=password secret/keycloak/admin)

# Azure Key Vault
export KEYCLOAK_PASSWORD=$(az keyvault secret show --vault-name my-vault --name keycloak-admin-password --query value -o tsv)
```

### JavaScript Substitution

- JavaScript substitution has access to system properties
- Use with caution in production environments
- Validate JavaScript expressions before deployment
- Avoid exposing sensitive information in error messages

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
  "realm": "$(javascript:env.REALM_NAME || 'development')",
  "enabled": "$(javascript:env.ENABLED === 'true')"
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

### 5. Validate Required Variables

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

### 6. JavaScript Performance

- Keep expressions simple and fast
- Avoid complex loops in large arrays
- Cache expensive operations

## Troubleshooting

### Common Issues

1. **Variable not substituted**: Ensure variable substitution is enabled
2. **JavaScript errors**: Check syntax and available context
3. **Missing environment variables**: Verify all required variables are set
4. **Permission denied**: Verify file permissions for `.env` files
5. **Special characters**: Quote variables with special characters
6. **Empty variables**: Use default values in substitution syntax

### Debug Mode

Enable debug logging to see substitution process:

```bash
java -jar keycloak-config-cli.jar \
  --logging.level.de.adorsys.keycloak.config=DEBUG \
  --import.var-substitution.enabled=true
```

### Check Loaded Variables

```bash
# List all environment variables
env | grep KEYCLOAK
env | grep IMPORT
env | grep REALM

# Check specific variable
echo "KEYCLOAK_URL is: $KEYCLOAK_URL"
echo "REALM_NAME is: $REALM_NAME"
```

### Testing Variables

Test variable substitution without applying changes:

```bash
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.dry-run=true \
  --import.files=config.json
```

### Variable Expansion Test

Create a test configuration to verify substitution:

```json
{
  "realm": "$(REALM_NAME:test-realm)",
  "test": {
    "keycloak_url": "$(KEYCLOAK_URL:not-set)",
    "user": "$(KEYCLOAK_USER:not-set)",
    "import_enabled": "$(IMPORT_VAR_SUBSTITUTION_ENABLED:false)"
  }
}
```

## Additional Resources

- [Apache Common `StringSubstitutor` documentation](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) - For more information and advanced usage
- [Spring Boot External Configuration](https://docs.spring.io/spring-boot/docs/2.5.0/reference/htmlsingle/#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables) - For Spring Boot property binding
