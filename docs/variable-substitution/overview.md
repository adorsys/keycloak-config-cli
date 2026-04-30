---
title: Variable Substitution Overview
description: Learn about variable substitution capabilities in keycloak-config-cli
sidebar_position: 1
---

# Variable Substitution Overview

keycloak-config-cli supports powerful variable substitution capabilities that allow you to create dynamic, environment-specific configurations.

## Enabling Variable Substitution

Variable substitution is **disabled by default** and must be explicitly enabled:

#### Environment variable

```bash
export IMPORT_VAR_SUBSTITUTION_ENABLED=true
export IMPORT_VAR_SUBSTITUTION_JAVASCRIPT_ENABLED=true
export IMPORT_VAR_SUBSTITUTION_SCRIPT_EVALUATION_ENABLED=true
```

#### Command line argument

```bash
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.var-substitution.javascript.enabled=true \
  --import.var-substitution.script-evaluation.enabled=true
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

#### Good

```bash
export KEYCLOAK_ADMIN_USER=admin
export KEYCLOAK_ADMIN_PASSWORD=secure-password
export APPLICATION_REALM_NAME=production
```

#### Avoid

```bash
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
REALM_NAME=dev-realm
KEYCLOAK_URL=http://localhost:8080

REALM_NAME=prod-realm
KEYCLOAK_URL=https://keycloak.company.com
```

## Available Variable Substitution Features

keycloak-config-cli supports the following variable substitution types:

### Environment & System Access
- **Environment Variables** - `$(env:VARIABLE_NAME)` - Access environment variables
- **System Properties** - `$(sys:PROPERTY_NAME)` - Access Java system properties
- **Spring Boot Properties** - `${spring.property.name}` - Access Spring Boot configuration

### File Operations
- **File Content** - `$(file:ENCODING:PATH)` - Read entire file contents
- **Properties File** - `$(properties:FILE_PATH::KEY)` - Extract values from Java properties files
- **Resource Bundle** - `$(resourceBundle:BUNDLE_NAME::KEY)` - Access resource bundle values
- **XML XPath** - `$(xml:FILE_PATH:EXPRESSION)` - Query XML documents using XPath

### Encoding & Decoding
- **Base64 Encoder** - `$(base64Encoder:STRING)` - Encode string to Base64
- **Base64 Decoder** - `$(base64Decoder:STRING)` - Decode Base64 string
- **URL Encoder** - `$(urlEncoder:STRING)` - URL-encode special characters
- **URL Decoder** - `$(urlDecoder:STRING)` - Decode URL-encoded strings

### Network Operations
- **DNS Lookup** - `$(dns:TYPE|DOMAIN)` - Resolve domain names to IP addresses
- **URL Content (HTTP)** - `$(url:ENCODING:URL)` - Retrieve content from HTTP URLs
- **URL Content (HTTPS)** - `$(url:ENCODING:HTTPS_URL)` - Retrieve content from HTTPS URLs
- **URL Content (File)** - `$(url:ENCODING:file:///PATH)` - Read content via file:// URL scheme

### Java Integration
- **Java Constants** - `$(const:CLASS_NAME.FIELD_NAME)` - Access Java static final fields
- **Java Version** - `$(java:version)` - Retrieve Java runtime version information

### System Information
- **Date Formatting** - `$(date:FORMAT_PATTERN)` - Format current date and time
- **Localhost** - `$(localhost:PROPERTY)` - Retrieve localhost information (hostname, IP)

### JavaScript Substitution
- **JavaScript** - `$(javascript:EXPRESSION)` - Evaluate JavaScript expressions for complex transformations

## Quick Reference

| Feature | Syntax | Documentation |
|---------|--------|---------------|
| Environment Variable | `$(env:USERNAME)` | [Environment Variables](environment-variables.md) |
| System Property | `$(sys:user.dir)` | [Environment Variables](environment-variables.md) |
| File Content | `$(file:UTF-8:path/to/file.txt)` | [File Operations](file-operations.md) |
| Properties File | `$(properties:config.properties::key)` | [File Operations](file-operations.md) |
| Resource Bundle | `$(resourceBundle:org.example.messages::key)` | [File Operations](file-operations.md) |
| XML XPath | `$(xml:config.xml:/root/@attribute)` | [File Operations](file-operations.md) |
| Base64 Encoder | `$(base64Encoder:HelloWorld!)` | [Encoding & Decoding](encoding-decoding.md) |
| Base64 Decoder | `$(base64Decoder:SGVsbG9Xb3JsZCE=)` | [Encoding & Decoding](encoding-decoding.md) |
| URL Encoder | `$(urlEncoder:Hello World!)` | [Encoding & Decoding](encoding-decoding.md) |
| URL Decoder | `$(urlDecoder:Hello%20World%21)` | [Encoding & Decoding](encoding-decoding.md) |
| Java Constant | `$(const:java.lang.Math.PI)` | [Java Integration](java-integration.md) |
| Java Version | `$(java:version)` | [Java Integration](java-integration.md) |
| JavaScript | `$(javascript:expression)` | [JavaScript Substitution](javascript-substitution.md) |

## Next Steps

- [JavaScript Substitution](javascript-substitution.md) - Advanced JavaScript usage
- [Environment Variables](environment-variables.md) - Environment variable management
- [File Operations](file-operations.md) - File content and properties
- [Encoding & Decoding](encoding-decoding.md) - Base64 and URL operations
- [Java Integration](java-integration.md) - Java constants and version
- [Configuration](../config/overview.md) - General configuration options
