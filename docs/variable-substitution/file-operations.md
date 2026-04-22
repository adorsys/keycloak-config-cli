# File Operations

File operations in variable substitution allow you to read content from files, properties files, resource bundles, and XML documents to dynamically populate your Keycloak configuration.

## Overview

File operations enable you to:
- Read entire file contents
- Extract specific values from properties files
- Access resource bundle values
- Query XML documents using XPath
- Handle different character encodings

## File Content

### Basic File Reading

Read the entire content of a file:

```json
{
  "realm": "$(file:UTF-8:src/test/resources/document.properties)",
  "displayName": "$(file:UTF-8:/path/to/display-name.txt)"
}
```

### Syntax

```
$(file:ENCODING:PATH)
```

**Parameters:**
- `ENCODING` - Character encoding (e.g., UTF-8, ISO-8859-1)
- `PATH` - File path (relative or absolute)

### Examples

**Reading a realm name from file:**
```json
{
  "realm": "$(file:UTF-8:config/realm-name.txt)"
}
```

**Reading a secret from file:**
```json
{
  "clients": [
    {
      "clientId": "my-app",
      "secret": "$(file:UTF-8:/run/secrets/client-secret)"
    }
  ]
}
```

**Using absolute path with system property:**
```json
{
  "secret": "$(file:UTF-8:file:///$(sys:user.dir)/secrets/password.txt)"
}
```

---

## Properties File

### Reading Properties

Extract specific values from Java properties files:

```json
{
  "realm": "$(properties:config/application.properties::realm.name)",
  "displayName": "$(properties:config/application.properties::app.display.name)"
}
```

### Syntax

```
$(properties:FILE_PATH::KEY)
```

**Parameters:**
- `FILE_PATH` - Path to the properties file
- `KEY` - Property key to extract

### Properties File Example

**application.properties:**
```properties
realm.name=production-realm
app.display.name=My Application
keycloak.url=https://keycloak.company.com
admin.email=admin@company.com
```

**Configuration:**
```json
{
  "realm": "$(properties:config/application.properties::realm.name)",
  "displayName": "$(properties:config/application.properties::app.display.name)",
  "attributes": {
    "keycloak_url": "$(properties:config/application.properties::keycloak.url)",
    "admin_email": "$(properties:config/application.properties::admin.email)"
  }
}
```

### Multiple Properties

You can read multiple properties from the same file:

```json
{
  "realm": "$(properties:config/realm.properties::name)",
  "enabled": "$(properties:config/realm.properties::enabled)",
  "sslRequired": "$(properties:config/realm.properties::ssl.required)"
}
```

---

## Resource Bundle

### Reading Resource Bundles

Access values from Java resource bundles (typically used for internationalization):

```json
{
  "realm": "$(resourceBundle:org.example.messages::realm.name)",
  "displayName": "$(resourceBundle:org.example.messages::app.title)"
}
```

### Syntax

```
$(resourceBundle:BUNDLE_NAME::KEY)
```

**Parameters:**
- `BUNDLE_NAME` - Fully qualified resource bundle name
- `KEY` - Resource key to extract

### Resource Bundle Example

**Resource bundle file (messages.properties):**
```properties
realm.name=Production Realm
app.title=My Application
welcome.message=Welcome to our platform
```

**Configuration:**
```json
{
  "realm": "$(resourceBundle:org.example.messages::realm.name)",
  "displayName": "$(resourceBundle:org.example.messages::app.title)",
  "internationalizationEnabled": true,
  "attributes": {
    "welcome_message": "$(resourceBundle:org.example.messages::welcome.message)"
  }
}
```

### Localization

Resource bundles support multiple locales:

```
messages.properties (default)
messages_fr.properties (French)
messages_de.properties (German)
messages_es.properties (Spanish)
```

---

## XML XPath

### Querying XML Documents

Extract values from XML documents using XPath expressions:

```json
{
  "realm": "$(xml:config/realm-config.xml:/realm/@name)",
  "displayName": "$(xml:config/realm-config.xml:/realm/displayName)"
}
```

### Syntax

```
$(xml:FILE_PATH:EXPRESSION)
```

**Parameters:**
- `FILE_PATH` - Path to the XML file
- `EXPRESSION` - XPath expression to evaluate

### XML Document Example

**realm-config.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<realm name="production-realm">
  <displayName>Production Realm</displayName>
  <settings>
    <sslRequired>external</sslRequired>
    <registrationAllowed>false</registrationAllowed>
  </settings>
  <attributes>
    <attribute name="environment">production</attribute>
    <attribute name="region">us-east-1</attribute>
  </attributes>
</realm>
```

**Configuration:**
```json
{
  "realm": "$(xml:config/realm-config.xml:/realm/@name)",
  "displayName": "$(xml:config/realm-config.xml:/realm/displayName)",
  "sslRequired": "$(xml:config/realm-config.xml:/realm/settings/sslRequired)",
  "registrationAllowed": "$(xml:config/realm-config.xml:/realm/settings/registrationAllowed)"
}
```

### Advanced XPath Examples

**Extract attributes:**
```json
{
  "realm": "$(xml:config/config.xml:/configuration/realm/@id)",
  "enabled": "$(xml:config/config.xml:/configuration/realm/@enabled)"
}
```

**Extract nested elements:**
```json
{
  "passwordPolicy": "$(xml:config/security.xml:/security/passwordPolicy/@value)"
}
```

**Extract list values:**
```json
{
  "attributes": {
    "supported_locales": "$(xml:config/i18n.xml:/locales/locale/text())"
  }
}
```

---

## Character Encodings

### Common Encodings

**UTF-8 (Recommended):**
```json
{
  "realm": "$(file:UTF-8:config/realm.txt)"
}
```

**ISO-8859-1:**
```json
{
  "realm": "$(file:ISO-8859-1:config/realm.txt)"
}
```

**US-ASCII:**
```json
{
  "realm": "$(file:US-ASCII:config/realm.txt)"
}
```

### Encoding Best Practices

1. **Use UTF-8**: Default to UTF-8 for maximum compatibility
2. **Match File Encoding**: Ensure encoding matches the actual file encoding
3. **Test Special Characters**: Verify special characters render correctly
4. **Document Encoding**: Document the encoding used for each file

---

## File Path Handling

### Relative Paths

Relative to the current working directory:

```json
{
  "realm": "$(file:UTF-8:config/realm.txt)"
}
```

### Absolute Paths

Full file system path:

```json
{
  "realm": "$(file:UTF-8:/opt/config/realm.txt)"
}
```

### Dynamic Paths

Using system properties or environment variables:

```json
{
  "realm": "$(file:UTF-8:$(env:CONFIG_DIR)/realm.txt)",
  "secret": "$(file:UTF-8:$(sys:user.home)/.secrets/password.txt)"
}
```

### File URL Scheme

Using file:// URL scheme:

```json
{
  "realm": "$(file:UTF-8:file:///$(sys:user.dir)/config/realm.txt)"
}
```

---

## Error Handling

### File Not Found

If a file doesn't exist, the substitution will fail:

```
Error: File not found: config/realm.txt
```

**Solution:** Ensure file exists and path is correct

### Invalid Encoding

If the encoding doesn't match the file content:

```
Error: Invalid character encoding
```

**Solution:** Use correct encoding or convert file to UTF-8

### Missing Property Key

If a property key doesn't exist:

```
Error: Property 'realm.name' not found in file
```

**Solution:** Verify key exists in properties file

### Invalid XPath Expression

If the XPath expression is malformed:

```
Error: Invalid XPath expression
```

**Solution:** Validate XPath syntax

---

## Security Considerations

### Sensitive Files

**Never commit sensitive files:**
```bash
# .gitignore
secrets/
*.key
*.pem
passwords.txt
```

**Use secure file locations:**
```json
{
  "secret": "$(file:UTF-8:/run/secrets/client-secret)"
}
```

### File Permissions

Ensure proper file permissions:

```bash
# Restrict access to sensitive files
chmod 600 /run/secrets/client-secret
chmod 600 ~/.secrets/password.txt
```

### Path Traversal Prevention

Be careful with user-provided paths:

```json
{
  // Avoid: User-controlled path
  "realm": "$(file:UTF-8:$(user_input))"
  
  // Better: Whitelist allowed paths
  "realm": "$(file:UTF-8:/opt/config/$(user_input))"
}
```

---

## Best Practices

### File Organization

**Organize configuration files:**
```
config/
├── realm/
│   ├── realm-name.txt
│   ├── realm-settings.properties
│   └── realm-config.xml
├── secrets/
│   ├── client-secret.txt
│   └── admin-password.txt
└── i18n/
    ├── messages.properties
    ├── messages_fr.properties
    └── messages_de.properties
```

### Naming Conventions

**Use descriptive file names:**
- `realm-name.txt` - Clear purpose
- `application.properties` - Standard format
- `messages.properties` - Resource bundle convention

### Documentation

**Document file structure:**
```markdown
# Configuration Files

## Realm Configuration
- realm-name.txt: Realm name
- realm-settings.properties: Realm settings

## Secrets
- client-secret.txt: Client secret (not in git)
```

### Validation

**Validate file existence before import:**
```bash
#!/bin/bash
required_files=(
  "config/realm-name.txt"
  "config/application.properties"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Error: Required file not found: $file"
    exit 1
  fi
done
```

---

## Use Cases

### Environment-Specific Configuration

**File-based environment configuration:**
```json
{
  "realm": "$(file:UTF-8:config/$(env:ENVIRONMENT)/realm.txt)",
  "displayName": "$(file:UTF-8:config/$(env:ENVIRONMENT)/display-name.txt)"
}
```

### Secret Management

**Reading secrets from secure files:**
```json
{
  "clients": [
    {
      "clientId": "backend-api",
      "secret": "$(file:UTF-8:/run/secrets/backend-secret)"
    }
  ]
}
```

### Internationalization

**Localized configuration:**
```json
{
  "realm": "$(resourceBundle:org.example.messages::realm.name)",
  "displayName": "$(resourceBundle:org.example.messages::app.title)",
  "internationalizationEnabled": true
}
```

### Configuration from External Systems

**Reading configuration from XML exports:**
```json
{
  "realm": "$(xml:/exports/keycloak-config.xml:/realm/@name)",
  "enabled": "$(xml:/exports/keycloak-config.xml:/realm/@enabled)"
}
```

---

## Complete Example

### Multi-Source Configuration

**realm.json:**
```json
{
  "realm": "$(file:UTF-8:config/realm-name.txt)",
  "displayName": "$(properties:config/application.properties::app.display.name)",
  "enabled": true,
  "sslRequired": "$(xml:config/security.xml:/security/ssl/@required)",
  "clients": [
    {
      "clientId": "my-app",
      "secret": "$(file:UTF-8:/run/secrets/client-secret)",
      "redirectUris": "$(properties:config/application.properties::app.redirect.uris)"
    }
  ],
  "attributes": {
    "welcome_message": "$(resourceBundle:org.example.messages::welcome)",
    "environment": "$(file:UTF-8:config/environment.txt)"
  }
}
```

**Directory structure:**
```
config/
├── realm-name.txt
├── application.properties
├── security.xml
└── environment.txt
/run/secrets/
└── client-secret.txt
src/main/resources/
└── org/example/
    └── messages.properties
```

---

## Next Steps

- [Overview](overview.md) - Variable substitution introduction
- [Environment Variables](environment-variables.md) - Environment variable access
- [JavaScript Substitution](javascript-substitution.md) - Advanced JavaScript evaluation
- [Encoding & Decoding](encoding-decoding.md) - Base64 and URL operations
