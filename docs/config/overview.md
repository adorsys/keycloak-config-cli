---
title: Configuration Overview
description: Learn about keycloak-config-cli configuration options and supported features
sidebar_position: 1
---

# Configuration Overview

keycloak-config-cli provides extensive configuration capabilities for managing Keycloak realms. This section covers the main configuration options and supported features.

## Supported Features

Keycloak Config CLI simplifies managing Keycloak configurations with its extensive feature set. Below is a summary of the functionalities available:

### Realm Management

| Feature | Since | Description |
|---------|-------|-------------|
| Create realms | 1.0.0 | Create new realms with full configuration |
| Update realms | 1.0.0 | Update existing realm properties |
| Enable/disable realms | 1.0.0 | Control realm availability |

### Client Management

| Feature | Since | Description |
|---------|-------|-------------|
| Create clients | 1.0.0 | Create client configuration (including protocolMappers) |
| Update clients | 1.0.0 | Update client configuration (including protocolMappers) |
| Manage fine-grained authorization | 2.2.0 | Add and remove authorization resources and policies |

### Role Management

| Feature | Since | Description |
|---------|-------|-------------|
| Add roles | 1.0.0 | Add roles while creating or updating realms |
| Update roles | 1.0.0 | Update role properties |
| Add composites to roles | 1.3.0 | Add role with realm-level and client-level composite roles |
| Remove composites from roles | 1.3.0 | Remove composite roles from existing roles |

### User Management

| Feature | Since | Description |
|---------|-------|-------------|
| Add users | 1.0.0 | Add users (including passwords) |
| Update users | 1.0.0 | Update user properties (including passwords) |
| Add users with roles | 1.0.0 | Add users with realm-level and client-level roles |
| Add role to user | 1.0.0 | Add roles to existing users |
| Remove role from user | 1.0.0 | Remove roles from users |
| Add groups to user | 2.0.0 | Add groups to users |
| Remove groups from user | 2.0.0 | Remove groups from users |

### Group Management

| Feature | Since | Description |
|---------|-------|-------------|
| Add groups | 1.3.0 | Add groups (including subgroups) |
| Update groups | 1.3.0 | Update group properties and attributes |
| Remove groups | 1.3.0 | Remove existing groups |
| Add/Remove group attributes | 1.3.0 | Manage group attributes |
| Add/Remove group roles | 1.3.0 | Manage group roles |
| Update/Remove subgroups | 1.3.0 | Manage subgroup hierarchy |

### Authentication & Flows

| Feature | Since | Description |
|---------|-------|-------------|
| Add authentication flows | 1.0.0 | Add authentication flows and executions |
| Update authentication flows | 1.0.0 | Update flow properties and executions |
| Remove authentication flows | 2.0.0 | Remove existing flows and executions |
| Update builtin flows | 2.0.0 | Update builtin authentication flows |
| Add authentication configs | 1.0.0 | Add authentication configurations |
| Update authentication configs | 2.0.0 | Update authentication configurations |
| Remove authentication configs | 2.0.0 | Remove authentication configurations |

### Components & Identity Providers

| Feature | Since | Description |
|---------|-------|-------------|
| Add components | 1.0.0 | Add components to realms |
| Update components | 1.0.0 | Update component properties |
| Remove components | 2.0.0 | Remove existing components |
| Add identity providers | 1.2.0 | Add identity providers |
| Update identity providers | 1.2.0 | Update identity providers |
| Remove identity providers | 2.0.0 | Remove identity providers |
| Manage IDP mappers | 2.0.0 | Add/update identity provider mappers |

### Other Features

| Feature | Since | Description |
|---------|-------|-------------|
| Add scope-mappings | 1.0.0 | Add scope-mappings |
| Manage scope-mapping roles | 1.0.0 | Add/remove roles from scope-mappings |
| Add required-actions | 1.0.0 | Add required-actions |
| Update required-actions | 1.0.0 | Update required-action properties |
| Remove required-actions | 2.0.0 | Remove required-actions |

## Configuration Structure

The configuration follows Keycloak's realm export format. Here's a basic structure:

```json
{
  "realm": "my-realm",
  "enabled": true,
  "displayName": "My Realm",
  "clients": [...],
  "roles": {...},
  "users": [...],
  "groups": [...],
  "identityProviders": [...],
  "components": [...],
  "authenticationFlows": [...],
  "requiredActions": [...]
}
```

## Configuration Options

### Basic Options

- `realm`: The realm name
- `enabled`: Whether the realm is enabled
- `displayName`: Human-readable realm name
- `loginTheme`: Theme for login pages
- `registrationAllowed`: Allow user registration
- `registrationEmailAsUsername`: Use email as username

### Advanced Options

- `sslRequired`: SSL requirements
- `bruteForceProtected`: Enable brute force protection
- `failureFactor`: Number of failures before lockout
- `passwordPolicy`: Password policies
- `otpPolicy`: OTP policies

## Next Steps

More configuration documentation coming soon! For now, check out:
- [GitHub Repository](https://github.com/adorsys/keycloak-config-cli) - Source code and examples
- [Example Configuration](https://github.com/adorsys/keycloak-config-cli/blob/main/contrib/example-config/moped.json) - Full working example
