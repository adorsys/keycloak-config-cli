# Managed Resources

This section covers how keycloak-config-cli manages and tracks resources in Keycloak realms.

## Overview

keycloak-config-cli can operate in different management modes, determining how existing resources are handled during imports and updates.

## Management Modes

### Full Management Mode

When `import.managed=true` (default behavior):

- **Create**: New resources are created
- **Update**: Existing resources are updated to match configuration
- **Delete**: Resources not present in import files are removed
- **Synchronize**: Complete alignment between configuration and realm state

### Partial Management Mode

When `import.managed=partial`:

- **Create**: Only new resources are created
- **Update**: Existing resources are left unchanged
- **Delete**: No resources are removed
- **Preserve**: Existing configurations are maintained

## Resource Types

### Users

```json
{
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "enabled": true,
      "attributes": {
        "department": "engineering"
      }
    }
  ]
}
```

### Groups

```json
{
  "groups": [
    {
      "name": "Developers",
      "path": "/developers",
      "attributes": {
        "description": "Development team group"
      }
    }
  ]
}
```

### Roles

```json
{
  "roles": [
    {
      "name": "app-user",
      "description": "Application user role",
      "composite": false,
      "clientRole": true
    }
  ]
}
```

### Clients

```json
{
  "clients": [
    {
      "clientId": "my-app",
      "name": "My Application",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "app-secret",
      "redirectUris": ["http://localhost:3000/*"],
      "webOrigins": ["http://localhost:3000"]
    }
  ]
}
```

### Realms

```json
{
  "realm": "my-realm",
  "enabled": true,
  "displayName": "My Realm",
  "loginTheme": "keycloak",
  "accountTheme": "keycloak"
}
```

### Organizations

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "enabled": true,
      "domains": ["acme.com"]
    }
  ]
}
```

## Management Strategies

### Create-Only Strategy

```bash
java -jar keycloak-config-cli.jar \
  --import.managed=create-only \
  --import.files=new-resources.json
```

### Update-Only Strategy

```bash
java -jar keycloak-config-cli.jar \
  --import.managed=update-only \
  --import.files=updates.json
```

### Delete-Only Strategy

```bash
java -jar keycloak-config-cli.jar \
  --import.managed=delete-only \
  --import.files=cleanup.json
```

## Conflict Resolution

### Automatic Conflict Detection

keycloak-config-cli automatically detects conflicts between:

- **Existing resources** vs **Import configuration**
- **Schema changes** vs **Existing data**
- **Dependency conflicts** between related resources

### Resolution Options

| Strategy | Description | Use Case |
|-----------|-------------|-----------|
| `SKIP` | Skip conflicting resources | Development environments |
| `OVERWRITE` | Replace existing resources | Force updates |
| `MERGE` | Combine configurations | Preserving existing data |

```bash
java -jar keycloak-config-cli.jar \
  --import.conflict-strategy=MERGE \
  --import.conflict.prefer=import
```

## State Tracking

### State File Format

```json
{
  "version": "1.0",
  "timestamp": "2024-01-15T10:30:00Z",
  "realm": "my-realm",
  "resources": {
    "users": {
      "created": 5,
      "updated": 2,
      "deleted": 0
    },
    "groups": {
      "created": 3,
      "updated": 1,
      "deleted": 0
    }
  }
}
```

### State Management Commands

```bash
# Enable state tracking
java -jar keycloak-config-cli.jar \
  --import.state.enabled=true \
  --import.state.file=state.json

# Show current state
java -jar keycloak-config-cli.jar \
  --import.state.show=true \
  --import.state.file=state.json

# Reset state
java -jar keycloak-config-cli.jar \
  --import.state.reset=true \
  --import.state.file=state.json
```

## Selective Management

### Resource Filtering

```bash
java -jar keycloak-config-cli.jar \
  --import.filter.type=users \
  --import.filter.attribute=department=engineering \
  --import.files=engineering-users.json
```

### Exclusion Patterns

```bash
java -jar keycloak-config-cli.jar \
  --import.exclude.type=clients \
  --import.exclude.pattern=test-.* \
  --import.files=production-config.json
```

## Best Practices

### Resource Management

1. **Use Version Control**: Keep configuration files in git
2. **Test in Staging**: Validate changes before production
3. **Backup Data**: Create realm backups before major changes
4. **Document Changes**: Maintain changelog of resource modifications
5. **Use State Tracking**: Enable state management for critical realms

### Conflict Prevention

1. **Unique Naming**: Use consistent naming conventions
2. **Dependency Planning**: Understand resource relationships
3. **Incremental Changes**: Make small, manageable updates
4. **Validation**: Use built-in validation features
5. **Monitoring**: Track resource creation and updates

## Advanced Features

### Custom Resource Types

```json
{
  "customResources": [
    {
      "type": "custom-entity",
      "endpoint": "/api/custom",
      "management": {
        "create": true,
        "read": true,
        "update": true,
        "delete": false
      }
    }
  ]
}
```

### Batch Operations

```bash
java -jar keycloak-config-cli.jar \
  --import.batch.size=100 \
  --import.batch.delay=1000 \
  --import.parallel.enabled=true
```

### Transaction Management

```bash
java -jar keycloak-config-cli.jar \
  --import.transaction.enabled=true \
  --import.transaction.timeout=30000
```

## Related Topics

- [Import Settings](../config/import-settings.md) - Detailed import configuration
- [Configuration](../config/overview.md) - General configuration options
- [State Management](../config/remote-state-management.md) - Advanced state tracking
- [Conflict Resolution](../config/partial-imports.md#2-conflicting-imports-from-multiple-files) - Handling import conflicts
