---
title: Limitations
description: Known limitations and constraints of keycloak-config-cli
sidebar_position: 3
---

# Limitations

This document outlines the known limitations and constraints of keycloak-config-cli. Understanding these limitations will help you plan your configuration strategy effectively.

## General Limitations

### Configuration Scope

- **Realm-Only**: keycloak-config-cli manages realm-level configurations only
- **No Server Admin**: Cannot modify server-level settings (e.g., master realm admin)
- **No Theme Management**: Cannot import/export theme files
- **No Extension Management**: Cannot manage custom extensions or modules

### Import/Export Limitations

- **Incremental Updates**: Cannot perform incremental updates (full realm import required)
- **Cross-Realm Dependencies**: Cannot manage dependencies between realms
- **Live Configuration**: Cannot modify running configurations without reimport
- **Rollback**: No built-in rollback mechanism (use version control)

## Feature-Specific Limitations

### User Management

#### Password Policies
- **Limited Validation**: Cannot enforce complex password policies during import
- **Temporary Passwords**: Limited support for temporary password requirements
- **Password History**: Cannot manage password history through configuration

#### User Federation
- **Read-Only**: Can read federated users but cannot modify federation settings
- **Sync Limitations**: No real-time synchronization with external user stores
- **Mapping Constraints**: Limited attribute mapping capabilities

#### Group Management
- **Nesting Limits**: Deep group nesting may cause performance issues
- **Membership Limits**: Large group memberships (1000+ users) may timeout
- **Dynamic Groups**: Cannot manage dynamic group rules

### Client Management

#### Client Scopes
- **Complex Scopes**: Limited support for complex client scope configurations
- **Scope Mappings**: Cannot manage fine-grained scope mappings
- **Resource Server**: Limited OpenID Connect resource server features

#### Client Credentials
- **Secret Rotation**: No automatic secret rotation
- **JWT Settings**: Limited JWT configuration options
- **Service Accounts**: Limited service account management

### Role Management

#### Composite Roles
- **Circular Dependencies**: Cannot resolve circular role dependencies
- **Performance**: Large numbers of composite roles may slow down imports
- **Conflicts**: Limited conflict resolution for role assignments

#### Permission Management
- **Fine-Grained Limits**: Some fine-grained permissions not fully supported
- **Resource Limits**: Large numbers of protected resources may cause issues
- **Policy Evaluation**: Complex policy evaluation may not work as expected

## Technical Limitations

### Performance

#### Memory Usage
- **Large Configurations**: Configurations >10MB may cause OutOfMemory errors
- **User Count**: >10,000 users may require increased heap size
- **Complex Roles**: >1000 roles with complex hierarchies impact performance

#### Network Limitations
- **Timeout Issues**: Large imports may timeout on slow networks
- **Connection Limits**: Limited concurrent connections to Keycloak
- **SSL/TLS**: Some corporate SSL configurations may not work

#### File System
- **File Size**: Single configuration files >50MB not supported
- **Encoding**: Limited to UTF-8 encoded files
- **Path Length**: Long file paths may cause issues on Windows

### Security Limitations

#### Credential Management
- **Plain Text Passwords**: Passwords in configuration files are stored in plain text
- **Secret Encryption**: No built-in encryption for sensitive data
- **Access Control**: No built-in access control for configuration files

#### Variable Substitution
- **JavaScript Security**: JavaScript substitution has access to system properties
- **Code Injection**: Potential for code injection in variable substitution
- **Resource Access**: Limited access to external resources in JavaScript

## Platform-Specific Limitations

### Docker Limitations

#### Resource Constraints
- **Default Limits**: Default Docker image has conservative memory limits
- **Volume Mounts**: Complex volume mounting may not work as expected
- **Networking**: Docker networking limitations may affect Keycloak connectivity

#### Environment Variables
- **Character Limits**: Environment variable values limited to 128KB
- **Special Characters**: Some special characters in environment variables may cause issues
- **Binary Data**: Cannot pass binary data through environment variables

### Kubernetes Limitations

#### RBAC Constraints
- **Permission Requirements**: Requires extensive RBAC permissions for full functionality
- **Service Account**: Limited service account token lifetime
- **Namespace Isolation**: Cross-namespace operations not supported

#### Storage Limitations
- **ConfigMap Size**: Large ConfigMaps may exceed Kubernetes limits
- **Secret Management**: Limited secret size and number
- **Persistent Volumes**: Complex volume configurations may not work

## Keycloak Version Limitations

### Legacy Versions

#### Keycloak 6.x - 10.x
- **API Compatibility**: Limited API compatibility with newer features
- **Authentication Flows**: Some modern authentication flows not supported
- **User Storage**: Limited user storage configuration options

#### Keycloak 11.x - 14.x
- **Partial Feature Support**: Some newer features only partially supported
- **Deprecated Features**: Some deprecated features may cause warnings
- **Performance**: Performance improvements in newer versions not available

### Red Hat SSO Limitations

#### Version Differences
- **Feature Parity**: Red Hat SSO may lag behind upstream Keycloak
- **Patching Required**: May require custom builds for compatibility
- **Support Limitations**: Limited community support for RH SSO specific issues

## Configuration Format Limitations

### JSON/YAML Constraints

#### Syntax Limitations
- **Comments**: JSON does not support comments
- **References**: No support for JSON references ($ref)
- **Validation**: Limited schema validation for complex structures

#### Data Type Limitations
- **Binary Data**: Cannot handle binary data in configurations
- **Date Formats**: Limited date format support
- **Numeric Precision**: Floating-point precision limitations

### File Format Limitations

#### Multiple Files
- **Dependency Resolution**: No automatic dependency resolution between files
- **Load Order**: File loading order may affect results
- **Cross-References**: Limited cross-file referencing capabilities

## Operational Limitations

### Import Strategies

#### CREATE_OR_UPDATE
- **Atomic Operations**: Not fully atomic - partial failures may occur
- **Conflict Resolution**: Limited conflict resolution strategies
- **Rollback**: No automatic rollback on failed imports

#### Partial Imports
- **Selective Import**: Limited ability to import specific sections
- **Dependency Management**: Cannot manage import dependencies
- **Validation**: Limited pre-import validation capabilities

### Error Handling

#### Error Reporting
- **Generic Errors**: Some errors may be reported generically
- **Recovery**: Limited automatic error recovery
- **Debugging**: Limited debugging information for complex issues

## Workarounds and Mitigations

### Performance Mitigations

#### Large Configurations
```bash
# Increase heap size for large configurations
export JAVA_OPTS="-Xmx2g -Xms1g"

# Split large configurations into smaller files
java -jar keycloak-config-cli.jar \
  --import.files=users.json,clients.json,roles.json
```

#### Network Issues
```bash
# Increase timeout for slow networks
java -jar keycloak-config-cli.jar \
  --import.connection.timeout=120000

# Use retry logic
for i in {1..3}; do
  java -jar keycloak-config-cli.jar --import.files=config.json && break
  sleep 30
done
```

### Security Mitigations

#### Credential Management
```bash
# Use environment variables for sensitive data
export KEYCLOAK_PASSWORD=$(vault kv get -field=password secret/keycloak)

# Use secret management in Kubernetes
apiVersion: v1
kind: Secret
metadata:
  name: keycloak-credentials
type: Opaque
data:
  password: <base64-encoded-password>
```

#### Variable Substitution Security
```json
{
  "users": [
    {
      "username": "$(javascript:env.USER || 'default')",
      "password": "$(javascript:env.PASSWORD || throw new Error('PASSWORD required'))"
    }
  ]
}
```

### Configuration Structure Mitigations

#### Modular Configuration
```json
// Split large configurations
{
  "realm": "my-realm",
  "enabled": true
}

// users.json
{
  "users": [...]
}

// clients.json  
{
  "clients": [...]
}
```

## Future Improvements

### Planned Enhancements

- **Incremental Updates**: Support for incremental configuration updates
- **Better Validation**: Enhanced configuration validation
- **Performance**: Improved performance for large configurations
- **Security**: Enhanced security features for credential management

### Known Issues Being Addressed

- **Memory Usage**: Optimizations for large configuration handling
- **Error Messages**: More descriptive error messages
- **Documentation**: Improved limitation documentation
- **Testing**: Enhanced test coverage for edge cases

## Best Practices

### Configuration Design

1. **Modular Approach**: Split large configurations into smaller, focused files
2. **Version Control**: Use Git for configuration management
3. **Testing**: Test configurations in development environments
4. **Documentation**: Document complex configurations and workarounds

### Operational Practices

1. **Backup Strategy**: Regular backups of working configurations
2. **Rollback Plan**: Have rollback procedures documented
3. **Monitoring**: Monitor import operations and errors
4. **Performance Testing**: Test with realistic data volumes

## Next Steps

- [Keycloak Versions](./keycloak-versions) - Version compatibility information
- [Red Hat SSO Support](./rhso-support) - Red Hat SSO specific guidance
- [Configuration](../configuration/overview) - General configuration options
