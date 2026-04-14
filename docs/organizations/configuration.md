# Organization Configuration

This section covers detailed configuration options for organizations in keycloak-config-cli.

## Basic Configuration

### Required Properties

Every organization must have at minimum:

```json
{
  "organizations": [
    {
      "name": "Acme Corporation",
      "alias": "acme-corp"
    }
  ]
}
```

| Property | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | String | Yes | Human-readable display name |
| `alias` | String | Yes | Unique identifier (used in URLs and references) |

### Optional Properties

```json
{
  "name": "Acme Corporation",
  "alias": "acme-corp",
  "description": "Main business unit for enterprise operations",
  "domains": ["acme.com", "acme-corp.com"],
  "enabled": true,
  "attributes": {
    "department": "operations",
    "region": "north-america"
  }
}
```

| Property | Type | Default | Description |
|-----------|------|----------|-------------|
| `description` | String | null | Detailed description of the organization |
| `domains` | Array | [] | List of domains associated with the organization |
| `enabled` | Boolean | true | Whether the organization is active |
| `attributes` | Object | {} | Custom key-value attributes |

## Domain Configuration

Domains define which email addresses and URLs are associated with an organization:

```json
{
  "domains": [
    "acme.com",
    "acme-corp.com", 
    "subsidiary.acme.com"
  ]
}
```

### Domain Rules

- **Unique**: Domains cannot be shared between organizations
- **Format**: Must be valid domain names
- **Case insensitive**: `ACME.COM` and `acme.com` are equivalent
- **Subdomains**: Automatically include subdomains (e.g., `*.acme.com`)

## Attributes

Custom attributes allow you to store additional metadata:

```json
{
  "attributes": {
    "department": "operations",
    "region": "north-america",
    "cost-center": "CC-1234",
    "contact-email": "admin@acme.com"
  }
}
```

### Common Attribute Patterns

| Use Case | Attribute Example |
|-----------|-----------------|
| Department | `"department": "operations"` |
| Cost Center | `"cost-center": "CC-1234"` |
| Contact Info | `"contact-email": "admin@acme.com"` |
| Location | `"region": "north-america"` |
| Classification | `"classification": "internal"` |

## Advanced Configuration

### Organization Hierarchy

While Keycloak doesn't natively support nested organizations, you can simulate hierarchy using attributes:

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "attributes": {
        "level": "1",
        "parent": null,
        "type": "corporate"
      }
    },
    {
      "name": "Acme West",
      "alias": "acme-west", 
      "attributes": {
        "level": "2",
        "parent": "acme-corp",
        "type": "regional"
      }
    }
  ]
}
```

### Conditional Configuration

Use variable substitution for environment-specific settings:

```json
{
  "organizations": [
    {
      "name": "${ORG_NAME}",
      "alias": "${ORG_ALIAS}",
      "domains": ["${ORG_DOMAIN}"],
      "enabled": ${ORG_ENABLED}
    }
  ]
}
```

## Configuration Examples

### Complete Enterprise Setup

```json
{
  "realm": "enterprise",
  "organizationsEnabled": true,
  "organizations": [
    {
      "name": "Acme Corporation",
      "alias": "acme-corp",
      "description": "Parent organization for all Acme business units",
      "domains": ["acme.com", "acme-corp.com"],
      "enabled": true,
      "attributes": {
        "type": "corporate",
        "level": "1",
        "established": "2020-01-15"
      }
    },
    {
      "name": "Acme Technology",
      "alias": "acme-tech",
      "description": "Technology and innovation division",
      "domains": ["tech.acme.com"],
      "enabled": true,
      "attributes": {
        "type": "division",
        "level": "2",
        "parent": "acme-corp"
      }
    }
  ]
}
```

### Multi-Tenant SaaS Setup

```json
{
  "realm": "saas-platform",
  "organizationsEnabled": true,
  "organizations": [
    {
      "name": "Client A",
      "alias": "client-a",
      "domains": ["client-a.saas.com"],
      "enabled": true,
      "attributes": {
        "tenant-id": "tenant-a-123",
        "plan": "enterprise",
        "max-users": "1000"
      }
    },
    {
      "name": "Client B", 
      "alias": "client-b",
      "domains": ["client-b.saas.com"],
      "enabled": true,
      "attributes": {
        "tenant-id": "tenant-b-456",
        "plan": "professional",
        "max-users": "500"
      }
    }
  ]
}
```

## Best Practices

1. **Consistent Aliases**: Use naming conventions (e.g., `company-department`)
2. **Descriptive Names**: Make names human-readable and meaningful
3. **Strategic Domains**: Plan domain assignments carefully
4. **Attribute Standards**: Establish consistent attribute naming
5. **Documentation**: Document your attribute schema
6. **Testing**: Validate configurations in development first

## Related Topics

- [Member Management](member-management.md) - Managing organization users
- [Identity Providers](identity-providers.md) - IDP integration