# Organization Configuration

This section covers all available configuration fields and options for organizations in keycloak-config-cli.

## Basic Organization Properties

### Required Fields

| Property | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | String | Yes | Human-readable organization name |
| `alias` | String | Yes | Unique identifier for the organization |

### Optional Fields

| Property | Type | Required | Description |
|-----------|------|----------|-------------|
| `description` | String | No | Organization description |
| `domains` | Array | No | Email domains associated with organization |
| `enabled` | Boolean | No | Whether organization is enabled (default: true) |
| `attributes` | Object | No | Custom organization attributes |

## Complete Configuration Structure

```json
{
  "organizations": [
    {
      "name": "Organization Name",
      "alias": "org-alias",
      "description": "Organization description",
      "domains": ["example.com", "org.example.com"],
      "enabled": true,
      "identityProviders": ["idp-alias-1", "idp-alias-2"],
      "members": [
        {
          "username": "user1",
          "roles": ["member", "admin"],
          "attributes": {
            "custom-field": "value"
          }
        }
      ],
      "attributes": {
        "custom-org-field": "custom-value"
      }
    }
  ]
}
```

## Field Details

### name

The human-readable name of the organization.

```json
{
  "name": "Acme Corporation"
}
```

**Requirements:**
- Must be unique within the realm
- Can contain spaces and special characters
- Recommended: 1-100 characters

### alias

The unique identifier used to reference the organization.

```json
{
  "alias": "acme-corp"
}
```

**Requirements:**
- Must be unique within the realm
- Cannot contain spaces
- Recommended: lowercase letters, numbers, hyphens only
- Recommended: 1-50 characters

### description

Optional description of the organization's purpose or function.

```json
{
  "description": "Enterprise client with SAML authentication and 500+ users"
}
```

### domains

Array of email domains that automatically route users to this organization.

```json
{
  "domains": ["acme.com", "corp.acme.com", "employees.acme.com"]
}
```

**Behavior:**
- Users with email addresses in these domains are automatically associated
- Multiple domains supported per organization
- Domains must be unique across organizations
- Optional field - if not specified, no automatic routing

### enabled

Controls whether the organization is active.

```json
{
  "enabled": true
}
```

**Values:**
- `true` - Organization is active and functional
- `false` - Organization is disabled but preserved
- Default: `true` if not specified

### identityProviders

Array of identity provider aliases associated with the organization.

```json
{
  "identityProviders": ["saml-enterprise", "oidc-google", "ldap-corporate"]
}
```

**Requirements:**
- Identity providers must exist in the realm
- Aliases must match configured IDP aliases
- Multiple IDPs supported per organization
- Optional field - if not specified, no specific IDPs

### members

Array of member objects defining users and their roles within the organization.

```json
{
  "members": [
    {
      "username": "john.doe",
      "roles": ["member", "admin"],
      "attributes": {
        "employee-id": "EMP001",
        "department": "engineering"
      }
    }
  ]
}
```

**Member Properties:**
- `username` (required): Username of existing Keycloak user
- `roles` (optional): Array of organization roles
- `attributes` (optional): Custom member attributes

### attributes

Custom organization attributes for storing additional metadata.

```json
{
  "attributes": {
    "client-type": "enterprise",
    "support-tier": "premium",
    "max-users": "1000",
    "contract-end": "2024-12-31",
    "billing-contact": "billing@acme.com"
  }
}
```

**Attribute Guidelines:**
- String keys and values only
- No nested objects or arrays
- Use consistent naming conventions
- Avoid reserved attribute names

## Organization Roles

### Standard Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| `member` | Basic organization membership | Access to organization resources |
| `admin` | Organization administrator | Full organization management |
| `manager` | Can manage other members | Member management capabilities |
| `viewer` | Read-only access | View organization information |

### Custom Roles

Organizations can define custom roles based on specific needs:

```json
{
  "members": [
    {
      "username": "project-lead",
      "roles": ["member", "project-manager", "team-lead"]
    }
  ]
}
```

## Configuration Examples

### Minimal Configuration

```json
{
  "organizations": [
    {
      "name": "Basic Org",
      "alias": "basic-org"
    }
  ]
}
```

### Standard Configuration

```json
{
  "organizations": [
    {
      "name": "Standard Corp",
      "alias": "standard-corp",
      "description": "Standard enterprise organization",
      "domains": ["standardcorp.com"],
      "enabled": true,
      "identityProviders": ["saml-enterprise"],
      "members": [
        {
          "username": "admin.standardcorp",
          "roles": ["member", "admin"]
        }
      ]
    }
  ]
}
```

### Advanced Configuration

```json
{
  "organizations": [
    {
      "name": "Advanced Enterprise",
      "alias": "advanced-enterprise",
      "description": "Multi-division enterprise with complex setup",
      "domains": ["enterprise.com", "corp.enterprise.com"],
      "enabled": true,
      "identityProviders": [
        "saml-primary",
        "oidc-secondary",
        "ldap-backup"
      ],
      "members": [
        {
          "username": "ceo.enterprise",
          "roles": ["member", "admin", "executive"],
          "attributes": {
            "executive-level": "c-suite",
            "clearance": "top-secret"
          }
        },
        {
          "username": "manager.enterprise",
          "roles": ["member", "manager"],
          "attributes": {
            "management-level": "senior",
            "team-size": "15"
          }
        }
      ],
      "attributes": {
        "organization-type": "enterprise",
        "compliance": ["fedramp", "sox"],
        "support-tier": "premium",
        "max-users": "5000",
        "contract-level": "platinum"
      }
    }
  ]
}
```

## Best Practices

### Configuration Structure

1. **Consistent Naming**: Use standardized aliases and naming conventions
2. **Complete Information**: Provide all relevant optional fields
3. **Logical Organization**: Group related attributes and members
4. **Documentation**: Include descriptive names and descriptions

### Security Considerations

1. **Principle of Least Privilege**: Assign minimum necessary roles
2. **Attribute Sensitivity**: Avoid storing sensitive data in attributes
3. **Access Controls**: Implement appropriate role-based access
4. **Regular Audits**: Monitor configuration changes

### Performance Optimization

1. **Batch Operations**: Configure multiple members efficiently
2. **Attribute Limits**: Avoid excessive custom attributes
3. **Domain Planning**: Use domain routing strategically
4. **IDP Optimization**: Limit identity providers per organization

## Related Topics

- [Overview](overview.md) - Organization feature overview
- [Member Management](member-management.md) - Managing organization users
- [Identity Providers](identity-providers.md) - IDP integration
- [Examples](examples.md) - Complete configuration scenarios
