# Working with Arrays

Related issues: [#1237](https://github.com/adorsys/keycloak-config-cli/issues/1237)

## Overview

Arrays in keycloak-config-cli configuration files represent collections such as
redirect URIs, web origins, roles, group memberships, protocol mappers, and
client scopes.

Understanding how arrays are handled is critical, as incorrect assumptions
about array behavior can lead to unexpected deletions or duplicated resources.

This use case explains how arrays behave during imports and how to safely add,
remove, or update array items.

---

## Array Behavior and Import Model

keycloak-config-cli follows a **declarative configuration model**.

This means:
- Configuration files describe the **desired end state**
- keycloak-config-cli reconciles Keycloak’s current state to match that file

Array behavior depends on whether **remote state tracking** is enabled.

### Array Behavior with Remote State

| Setting | Behavior |
|-------|----------|
| `import.remote-state.enabled=true` (default) | keycloak-config-cli tracks resources it manages and updates only those items |
| `import.remote-state.enabled=false` | Arrays are replaced entirely with the contents of the configuration file |

---

## Use Case: Adding Items to an Array

### Scenario

Add a new redirect URI to an existing client without removing already configured
redirect URIs.

### Configuration

```yaml
realm: "myrealm"
clients:
  - clientId: "my-app"
    redirectUris:
      - "https://app.example.com/callback"
      - "https://app.example.com/oauth/callback"
      - "http://localhost:3000/callback" # New URI
