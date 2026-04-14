# Organizations

The keycloak-config-cli supports comprehensive organization management, allowing you to import, update, and manage organizations within your Keycloak realms.

## Overview

Organizations in Keycloak provide a way to structure and manage users, groups, and resources in a hierarchical manner. This is particularly useful for multi-tenant applications or enterprise environments where different business units need to be managed separately.

## Supported Features

- **Organization Creation**: Create new organizations with detailed configuration
- **Organization Updates**: Modify existing organization properties
- **Organization Deletion**: Remove organizations when no longer needed
- **Member Management**: Add and remove users from organizations
- **Identity Provider Integration**: Associate identity providers with organizations
- **Pagination Support**: Handle large numbers of organizations efficiently

## Configuration

### Basic Organization Structure

```json
{
  "realm": "my-realm",
  "organizationsEnabled": true,
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "description": "Main business organization",
      "domains": ["acme.com", "acme-corp.com"],
      "enabled": true,
      "identityProviders": ["saml-acme", "oidc-acme"],
      "members": [
        {
          "username": "john.doe",
          "roles": ["admin", "user"]
        }
      ]
    }
  ]
}
```


## Member Management

### Adding Members

```json
{
  "members": [
    {
      "username": "john.doe",
      "roles": ["admin", "user"]
    },
    {
      "username": "jane.smith", 
      "roles": ["user"]
    }
  ]
}
```

### Member Roles

- **admin**: Full administrative access to the organization
- **user**: Standard user access within the organization

## Identity Provider Integration

Organizations can be associated with specific identity providers to control authentication methods:

```json
{
  "identityProviders": ["saml-acme", "oidc-acme"]
}
```

## Pagination

For realms with large numbers of organizations, the keycloak-config-cli automatically handles pagination to ensure all organizations are processed:

- **Default page size**: 100 organizations per page
- **Automatic fallback**: Uses appropriate pagination method based on Keycloak version
- **Error handling**: Graceful degradation for older Keycloak versions


### Troubleshooting

- **Check realm permissions**: Ensure the import user has `manage-organization` permission
- **Verify user existence**: All referenced users must exist in the realm
- **Validate IDP configuration**: Identity providers must be properly configured
- **Review alias uniqueness**: Organization aliases must be unique within the realm

## Best Practices

1. **Use descriptive aliases**: Make organization aliases meaningful and consistent
2. **Plan hierarchy**: Consider your organizational structure before import
3. **Test imports**: Use partial imports first to validate configuration
4. **Monitor logs**: Check import logs for detailed error information
5. **Backup data**: Always backup realm data before bulk operations

## Compatibility

- **Keycloak 26.0.8+**: Full pagination support with modern API
- **Keycloak 26.0.5-26.0.2**: Legacy API support with fallback
- **Keycloak < 26.0**: Organization features not available

## Related Topics

- [Variable Substitution](../variable-substitution/overview.md) - Using variables in organization configs
- [Configuration](../config/overview.md) - Organization setup options
- [Managed Resources](../config/managed-resource.md) - Understanding managed imports
- [Compatibility](../compatibility/overview.md) - Version-specific requirements
