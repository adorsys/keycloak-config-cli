# Supported Features

This section provides a comprehensive overview of all features supported by keycloak-config-cli.

## Overview

keycloak-config-cli supports comprehensive management of Keycloak realms through import operations, with support for users, groups, roles, clients, and organizations.

## Core Features

### Realm Management

- **Realm Creation**: Create new Keycloak realms
- **Realm Updates**: Modify existing realm configurations
- **Theme Configuration**: Configure realm themes and branding
- **Localization**: Set up realm languages and internationalization
- **Security Settings**: Configure realm security policies and requirements

### User Management

- **User Creation**: Create new users with various attributes
- **User Updates**: Modify existing user properties and credentials
- **User Deletion**: Remove users from realm
- **Bulk Operations**: Process multiple users efficiently
- **Password Policies**: Enforce password complexity and history
- **Multi-Factor Authentication**: Configure MFA requirements
- **User Federation**: Integrate with LDAP and other identity providers

### Group Management

- **Group Creation**: Create new groups with hierarchies
- **Group Updates**: Modify group properties and memberships
- **Group Deletion**: Remove groups and clean up memberships
- **Group Hierarchies**: Support nested group structures
- **Role Mapping**: Map groups to specific roles
- **Case Sensitivity**: Handle case-sensitive group names

### Role Management

- **Role Creation**: Define new roles with permissions
- **Composite Roles**: Create roles that combine other roles
- **Client Roles**: Manage application-specific roles
- **Permission Mapping**: Configure fine-grained permissions
- **Role Hierarchies**: Support role inheritance and composition

### Client Management

- **Client Registration**: Create and configure OAuth2 clients
- **Client Secrets**: Manage client authentication secrets
- **Redirect URIs**: Configure allowed callback URLs
- **Token Exchange**: Enable token exchange capabilities
- **Client Scopes**: Define OAuth2 scope permissions
- **Grant Types**: Configure authorization grant flows

### Organization Management (Keycloak 26+)

- **Organization Creation**: Create multi-tenant organizations
- **Organization Updates**: Modify organization properties
- **Organization Deletion**: Remove organizations cleanly
- **Member Management**: Manage organization user memberships
- **Domain Management**: Configure organization domains
- **Identity Provider Integration**: Link IDPs to organizations
- **Pagination Support**: Efficient handling of large organization sets

## Advanced Features

### Variable Substitution

- **Environment Variables**: Substitute values from environment
- **JavaScript Substitution**: Dynamic value transformation with JavaScript
- **JSON Arrays**: Support for array variable substitution
- **Conditional Logic**: If/else statements in substitutions
- **Custom Functions**: Extend substitution capabilities

### Import Strategies

- **Full Import**: Complete realm synchronization
- **Partial Import**: Selective resource updates
- **Conflict Resolution**: Handle import conflicts intelligently
- **State Management**: Track import operations and changes
- **Transaction Support**: Atomic import operations
- **Rollback Capabilities**: Undo failed imports

### Validation Features

- **JSON Schema Validation**: Validate configuration against schemas
- **Cross-Reference Validation**: Check resource dependencies
- **Custom Validators**: Implement validation rules
- **Dry Run Mode**: Preview changes before applying
- **Error Reporting**: Detailed validation feedback

### Performance Features

- **Parallel Processing**: Multi-threaded import operations
- **Batch Operations**: Efficient bulk processing
- **Connection Pooling**: Reuse Keycloak connections
- **Caching**: Optimize repeated operations
- **Throttling**: Rate limiting for API protection

### Security Features

- **SSL/TLS Support**: Secure communication with Keycloak
- **Truststore Configuration**: Custom certificate management
- **Authentication Methods**: Multiple authentication strategies
- **Permission Validation**: Verify access rights before operations
- **Secret Management**: Secure handling of sensitive data

## Integration Features

### Docker Support

- **Official Images**: Pre-built Docker containers
- **Docker Compose**: Multi-container deployments
- **Environment Configuration**: Container-specific settings
- **Volume Management**: Persistent data storage
- **Health Checks**: Container monitoring capabilities

### Kubernetes Support

- **Helm Charts**: Kubernetes deployment templates
- **Custom Resources**: Kubernetes-specific configurations
- **Service Accounts**: Kubernetes service account management
- **Ingress Support**: External access configuration
- **Monitoring Integration**: Prometheus metrics and health checks

### CI/CD Integration

- **GitHub Actions**: Automated testing and deployment
- **Maven Integration**: Build system compatibility
- **TestContainers**: Integration testing framework
- **Docker Registry**: Automated image publishing
- **Release Automation**: Version management and releases

## Configuration Features

### Flexible Configuration

- **Command Line Options**: Comprehensive CLI interface
- **Environment Variables**: Environment-based configuration
- **Configuration Files**: JSON/YAML configuration support
- **Property Files**: Java properties configuration
- **Profile Support**: Environment-specific settings

### Logging Features

- **Structured Logging**: JSON and text log formats
- **Log Levels**: Configurable logging verbosity
- **Performance Metrics**: Operation timing and statistics
- **Error Tracking**: Comprehensive error reporting
- **Audit Logging**: Security event logging

## Compatibility Features

### Keycloak Version Support

- **Keycloak 26+**: Latest features and full API support
- **Keycloak 23-25**: Legacy compatibility mode
- **Keycloak 21-22**: Basic feature support
- **Version Detection**: Automatic capability detection
- **Graceful Degradation**: Feature fallbacks for older versions

### Platform Support

- **Linux**: Full feature support
- **macOS**: Complete compatibility
- **Windows**: Native Windows support
- **Container Platforms**: Docker and Kubernetes support

## Extensibility Features

### Plugin Architecture

- **Custom Extensions**: Plugin system for custom features
- **Event Hooks**: Pre/post operation callbacks
- **Custom Validators**: Extensible validation framework
- **Custom Importers**: Support for custom resource types

### API Integration

- **REST API**: Direct Keycloak API usage
- **Admin Client**: Keycloak admin client integration
- **Custom Endpoints**: Extension point support
- **Batch Operations**: Bulk API operations

## Limitations

### Current Limitations

- **Real-time Updates**: No real-time synchronization
- **UI Management**: CLI-only (no web interface)
- **Multi-realm**: Single realm per import operation
- **Live Migration**: No live database migration support

### Planned Features

- **Web Interface**: Future GUI management
- **Multi-realm Support**: Cross-realm operations
- **Real-time Sync**: Live synchronization capabilities
- **Advanced Analytics**: Usage statistics and reporting

## Feature Matrix

| Feature | Keycloak 26+ | Keycloak 23-25 | Keycloak 21-22 | Status |
|----------|------------------|------------------|------------------|--------|
| Organizations | ✅ Full | ❌ Not Available | ❌ Not Available | **New** |
| Variable Substitution | ✅ Full | ✅ Full | ✅ Basic | **Mature** |
| Parallel Processing | ✅ Full | ✅ Full | ⚠️ Limited | **Mature** |
| State Management | ✅ Full | ✅ Full | ⚠️ Limited | **Mature** |
| Fine-grained Permissions | ✅ Full | ✅ Full | ⚠️ Limited | **Mature** |
| Token Exchange | ✅ Full | ✅ Full | ⚠️ Limited | **Mature** |
| Docker Support | ✅ Full | ✅ Full | ✅ Full | **Mature** |
| Helm Charts | ✅ Full | ✅ Full | ✅ Full | **Mature** |

## Examples

### Basic Realm Setup

```json
{
  "realm": "my-realm",
  "enabled": true,
  "users": [
    {
      "username": "admin",
      "email": "admin@example.com",
      "enabled": true,
      "credentials": [{
        "type": "password",
        "value": "admin123",
        "temporary": false
      }]
    }
  ],
  "clients": [
    {
      "clientId": "my-app",
      "name": "My Application",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "app-secret",
      "redirectUris": ["http://localhost:3000/*"]
    }
  ]
}
```

### Organization Configuration (Keycloak 26+)

```json
{
  "organizations": [
    {
      "name": "Acme Corporation",
      "alias": "acme-corp",
      "domains": ["acme.com", "acme-corp.com"],
      "enabled": true,
      "members": [
        {
          "username": "john.doe",
          "roles": ["member"]
        }
      ],
      "identityProviders": ["oidc-google"]
    }
  ]
}
```

### Variable Substitution Example

```json
{
  "users": [
    {
      "username": "${USER_PREFIX}_admin",
      "email": "${USER_PREFIX}@example.com",
      "enabled": true
    }
  ]
}
```

## Best Practices

1. **Start Small**: Begin with basic configurations and expand
2. **Use Validation**: Enable validation to catch errors early
3. **Test Thoroughly**: Use dry-run mode before production
4. **Document Changes**: Maintain clear configuration history
5. **Monitor Performance**: Track import times and resource usage
6. **Plan for Scale**: Design configurations for growth

## Related Topics

- [Configuration](../config/overview.md) - Detailed configuration options
- [Import Settings](../config/import-settings.md) - Import behavior control
- [Variable Substitution](../variable-substitution/overview.md) - Dynamic configuration
- [Organizations](../organizations/overview.md) - Organization management (Keycloak 26+)
