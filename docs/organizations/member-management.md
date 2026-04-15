# Member Management

This section covers managing users within organizations, including member assignment, roles, and membership lifecycle.

## Overview

Organization member management allows you to control which users belong to specific organizations and what roles they have within those organizations.

## Member Configuration

### Basic Member Assignment

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "members": [
        {
          "username": "john.doe",
          "roles": ["member"]
        },
        {
          "username": "jane.smith",
          "roles": ["member", "admin"]
        }
      ]
    }
  ]
}
```

### Advanced Member Configuration

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "members": [
        {
          "username": "john.doe",
          "roles": ["member"],
          "attributes": {
            "department": "engineering",
            "manager": "jane.smith"
          }
        },
        {
          "username": "jane.smith",
          "roles": ["member", "admin"],
          "attributes": {
            "department": "engineering",
            "level": "senior"
          }
        }
      ]
    }
  ]
}
```

## Member Properties

| Property | Type | Required | Description |
|-----------|------|----------|-------------|
| `username` | String | Yes | Username of the member |
| `roles` | Array | No | List of organization roles for the member |
| `attributes` | Object | No | Custom attributes for the member |

## Available Roles

### Standard Organization Roles

- **member** - Basic organization membership
- **admin** - Organization administrator with full access
- **manager** - Can manage other members
- **viewer** - Read-only access to organization resources

### Custom Roles

Organizations can define custom roles based on specific needs:

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "members": [
        {
          "username": "john.doe",
          "roles": ["member", "project-lead", "team-manager"]
        }
      ]
    }
  ]
}
```

## Bulk Member Operations

### Adding Multiple Members

```json
{
  "organizations": [
    {
      "name": "Large Corp",
      "alias": "large-corp",
      "members": [
        {
          "username": "user1@largecorp.com",
          "roles": ["member"]
        },
        {
          "username": "user2@largecorp.com",
          "roles": ["member"]
        },
        {
          "username": "user3@largecorp.com",
          "roles": ["member", "admin"]
        }
      ]
    }
  ]
}
```

### Role-Based Member Assignment

```json
{
  "organizations": [
    {
      "name": "Department A",
      "alias": "dept-a",
      "members": [
        {
          "username": "manager.a",
          "roles": ["member", "admin", "manager"]
        },
        {
          "username": "lead.a",
          "roles": ["member", "team-lead"]
        },
        {
          "username": "dev1.a",
          "roles": ["member"]
        },
        {
          "username": "dev2.a",
          "roles": ["member"]
        }
      ]
    }
  ]
}
```

## Member Attributes

### Custom Member Attributes

Store organization-specific information for each member:

```json
{
  "organizations": [
    {
      "name": "Tech Company",
      "alias": "tech-co",
      "members": [
        {
          "username": "alice.tech",
          "roles": ["member"],
          "attributes": {
            "employee-id": "EMP001",
            "department": "engineering",
            "location": "san-francisco",
            "start-date": "2023-01-15",
            "skill-level": "senior"
          }
        }
      ]
    }
  ]
}
```

### Attribute-Based Grouping

Use attributes for organizational structure:

```json
{
  "organizations": [
    {
      "name": "Global Corp",
      "alias": "global-corp",
      "members": [
        {
          "username": "regional.manager.us",
          "roles": ["member", "admin"],
          "attributes": {
            "region": "north-america",
            "level": "regional-manager",
            "reports": "ceo@globalcorp.com"
          }
        },
        {
          "username": "team.lead.eu",
          "roles": ["member", "manager"],
          "attributes": {
            "region": "europe",
            "level": "team-lead",
            "team": "engineering"
          }
        }
      ]
    }
  ]
}
```

## Import Behavior

### Member Creation

When importing members:

- **Existing Users**: Added to organization if they exist in realm
- **New Users**: Must exist in realm before being added to organizations
- **Role Assignment**: Roles are applied to organization membership
- **Attributes**: Custom attributes are stored with member record

### Member Updates

- **Role Changes**: Member roles are updated to match configuration
- **Attribute Updates**: Custom attributes are updated
- **Membership Changes**: Users can be added or removed from organizations

## Best Practices

### Member Organization

1. **Consistent Naming**: Use predictable username patterns
2. **Role Hierarchies**: Establish clear role-based access patterns
3. **Attribute Standards**: Define consistent attribute schemas
4. **Bulk Operations**: Use efficient batch member imports
5. **Regular Audits**: Monitor membership changes and access

### Security Considerations

1. **Principle of Least Privilege**: Assign minimum necessary roles
2. **Regular Reviews**: Periodically audit member roles and access
3. **Separation of Duties**: Avoid concentration of administrative rights
4. **Access Logging**: Monitor member management activities
5. **Role Validation**: Ensure role assignments are appropriate

## Related Topics

- [Configuration](configuration.md) - Organization setup
- [Identity Providers](identity-providers.md) - IDP integration
- [Examples](examples.md) - Complete scenarios with examples
