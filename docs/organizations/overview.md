# Organizations Overview

Organizations provide multi-tenant capabilities for Keycloak, allowing you to group and manage users, clients, and identity providers within organizational boundaries.

## Version Support

- **Keycloak 26.3.3+**: Full support for organization features
- **keycloak-config-cli 6.5.0+**: Complete organization management capabilities

## What Are Organizations?

Organizations in Keycloak represent logical groupings that can contain:

- **Members** - Users who belong to the organization
- **Identity Providers** - Authentication methods specific to the organization
- **Domains** - Email domains associated with the organization
- **Settings** - Organization-specific configuration and policies

## Key Features

- **Multi-Tenant Architecture**: Isolate users and resources by organization
- **Domain-Based Routing**: Automatically route users based on email domains
- **Organization-Specific IDPs**: Each organization can have dedicated identity providers
- **Member Management**: Control user membership and roles within organizations

## Getting Started

1. **Ensure Compatibility**: Use Keycloak 26.3.3+ and keycloak-config-cli 6.5.0+
2. **Create Organization Definition**: Add organization to your configuration file
3. **Import Configuration**: Use keycloak-config-cli to apply changes

## Related Topics

- [Member Management](member-management.md) - Managing organization users
- [Identity Providers](identity-providers.md) - IDP integration
- [Examples](examples.md) - Complete scenarios with examples
