# Organization Member Management

This section covers managing users within organizations, including adding members, assigning roles, and handling member relationships.

## Overview

Organization members are users who belong to a specific organization and can have specific roles and permissions within that organization context.

## Member Structure

### Basic Member Definition

```json
{
  "members": [
    {
      "username": "john.doe",
      "roles": ["user"]
    }
  ]
}
```

### Complete Member Definition

```json
{
  "members": [
    {
      "username": "john.doe",
      "roles": ["admin", "user"],
      "attributes": {
        "department": "operations",
        "manager": "jane.smith"
      }
    }
  ]
}
```

## Member Properties

| Property | Type | Required | Description |
|-----------|------|----------|-------------|
| `username` | String | Yes | Username of existing Keycloak user |
| `roles` | Array | Yes | List of roles within the organization |
| `attributes` | Object | No | Custom member attributes |

## Organization Roles

### Built-in Roles

| Role | Description | Permissions |
|-------|-------------|--------------|
| `admin` | Full administrative access | Manage organization, members, and settings |
| `user` | Standard user access | Access organization resources, limited management |

### Role Hierarchy

- **admin**: Can manage organization settings, members, and resources
- **user**: Can access organization resources but has limited management capabilities

```json
{
  "members": [
    {
      "username": "org-admin@acme.com",
      "roles": ["admin"]
    },
    {
      "username": "regular-user@acme.com", 
      "roles": ["user"]
    },
    {
      "username": "power-user@acme.com",
      "roles": ["admin", "user"]
    }
  ]
}
```

## Member Management Operations

### Adding Members

#### Single Member

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "members": [
        {
          "username": "new.user@acme.com",
          "roles": ["user"]
        }
      ]
    }
  ]
}
```

#### Multiple Members

```json
{
  "organizations": [
    {
      "name": "Acme Corp", 
      "alias": "acme-corp",
      "members": [
        {
          "username": "user1@acme.com",
          "roles": ["user"]
        },
        {
          "username": "user2@acme.com",
          "roles": ["user"]
        },
        {
          "username": "admin@acme.com",
          "roles": ["admin"]
        }
      ]
    }
  ]
}
```

### Updating Member Roles

To update member roles, modify the roles array:

```json
{
  "members": [
    {
      "username": "john.doe",
      "roles": ["user", "reviewer"]
    }
  ]
}
```

### Removing Members

To remove members, exclude them from the members array:

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "members": [
        {
          "username": "jane.smith",
          "roles": ["admin"]
        }
      ]
    }
  ]
}
```

## Member Attributes

Custom attributes provide additional context about members:

```json
{
  "members": [
    {
      "username": "john.doe",
      "roles": ["user"],
      "attributes": {
        "employee-id": "EMP-1234",
        "department": "engineering",
        "location": "san-francisco",
        "manager": "jane.smith",
        "hire-date": "2023-01-15"
      }
    }
  ]
}
```

### Common Member Attributes

| Attribute | Example | Use Case |
|-----------|----------|-----------|
| Employee ID | `"employee-id": "EMP-1234"` | HR integration |
| Department | `"department": "engineering"` | Organizational structure |
| Location | `"location": "san-francisco"` | Geographic distribution |
| Manager | `"manager": "jane.smith"` | Reporting hierarchy |
| Hire Date | `"hire-date": "2023-01-15"` | Onboarding tracking |

## Import Behavior

### Managed Import (`import.managed.organization=full`)

1. **Add**: New members are created in the organization
2. **Update**: Existing members have their roles updated
3. **Remove**: Members not in import file are removed from organization
4. **Attributes**: Member attributes are synchronized

### Partial Import (`import.managed.organization=partial`)

1. **Add**: Only new members are created
2. **Update**: Existing members are not modified
3. **Remove**: No members are removed
4. **Attributes**: Only new member attributes are added

## Advanced Scenarios

### Bulk Member Import

```json
{
  "organizations": [
    {
      "name": "Acme Corp",
      "alias": "acme-corp",
      "members": [
        {
          "username": "user1@acme.com",
          "roles": ["user"]
        },
        {
          "username": "user2@acme.com", 
          "roles": ["user"]
        },
        {
          "username": "user3@acme.com",
          "roles": ["user"]
        },
        {
          "username": "admin1@acme.com",
          "roles": ["admin"]
        },
        {
          "username": "admin2@acme.com",
          "roles": ["admin"]
        }
      ]
    }
  ]
}
```

### Department-Based Organization

```json
{
  "organizations": [
    {
      "name": "Engineering",
      "alias": "acme-engineering",
      "members": [
        {
          "username": "dev-lead@acme.com",
          "roles": ["admin"],
          "attributes": {
            "department": "engineering",
            "role": "lead"
          }
        },
        {
          "username": "dev1@acme.com",
          "roles": ["user"],
          "attributes": {
            "department": "engineering",
            "role": "developer"
          }
        }
      ]
    }
  ]
}
```

## Best Practices

1. **User Existence**: Ensure all users exist before import
2. **Role Consistency**: Use consistent role naming conventions
3. **Batch Operations**: Import members in batches for better performance
4. **Attribute Standards**: Establish consistent member attribute schemas
5. **Testing**: Validate member imports in development environment
6. **Documentation**: Document role definitions and permissions

## Related Topics

- [Configuration](configuration.md) - Organization setup
- [Identity Providers](identity-providers.md) - IDP integration
