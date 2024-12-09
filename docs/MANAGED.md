## Resource Management
### Introduction

This document explains how keycloak-config-cli (kc-cli) manages resources in Keycloak, including its default behavior, customization options, and impact on various resource types.

### How keycloak-config-cli Tracks Resources

- keycloak-config-cli stores information about resources it creates as realm attributes in the Keycloak database.
- This tracking mechanism allows kc-cli to manage these resources in subsequent runs.

### Default Behavior

- By default, kc-cli will delete and recreate resources that it initially created in previous runs.
- This ensures that the Keycloak configuration always matches the state defined in your configuration files.

### Customizing Resource Management

- The `import.managed.*` family of properties allows you to customize this behavior.
- Setting these properties to `no-delete` will prevent kc-cli from deleting resources, even if they're no longer present in your configuration files.

### Impact on User Federations

- This behavior applies to user federations (such as LDAP and Active Directory).
- When a user federation is deleted and recreated, all users created by that federation will also be deleted.
- This includes associated data like offline tokens.

### Full Managed Resources

keycloak-config-cli manages some types of resources absolutely. For example, if a `group` isn't defined inside the import JSON but other `groups` are specified, keycloak-config-cli will calculate the difference and delete the `group` from Keycloak.

In some cases, it is required to include some Keycloak defaults because keycloak-config-cli can't detect if the entity comes from a user or is auto-created by Keycloak itself.

### Management Modes

1. **Keycloak Should Not Manage Type of Resources**:
    - If you don't define any `groups` inside the import JSON, Keycloak does not touch any `groups`.

2. **Keycloak Manages Type of Resources**:
    - If you define any `groups` you want inside the import JSON, Keycloak ensures that those groups are available but deletes other groups.
    - If you define `groups` but set an empty array, Keycloak will delete all groups in Keycloak.

### Supported Full Managed Resources

| Type                            | Additional Information                                                           | Resource Name                    |
|---------------------------------|----------------------------------------------------------------------------------|----------------------------------|
| Groups                          | -                                                                                | `group`                          |
| Required Actions                | You have to copy the default one to your import JSON.                           | `required-action`                |
| Client Scopes                   | -                                                                                | `client-scope`                   |
| Scope Mappings                  | -                                                                                | `scope-mapping`                  |
| Client Scope Mappings           | -                                                                                | `client-scope-mapping`           |
| Roles                           | -                                                                                | `role`                           |
| Components                      | You have to copy the default components to your import JSON.                    | `component`                      |
| Sub Components                  | You have to copy the default components to your import JSON.                    | `sub-component`                  |
| Authentication Flows            | You have to copy the default components to your import JSON, except built-in flows.| `authentication-flow`            |
| Identity Providers              | -                                                                                | `identity-provider`              |
| Identity Provider Mappers       | -                                                                                | `identity-provider-mapper`       |
| Clients                         | -                                                                                | `client`                         |
| Clients Authorization Resources  | The 'Default Resource' is always included.                                       | `client-authorization-resources` |
| Clients Authorization Policies   | -                                                                                | `client-authorization-policies`  |
| Clients Authorization Scopes     | -                                                                                | `client-authorization-scopes`    |
| Message Bundles                 | Only message bundles imported with config-cli will be managed/deleted.         | `message-bundles`                |

### Disabling Deletion of Managed Entities

If you don't want to delete properties of a specific type, you can disable this behavior by setting properties like `import.managed.<entity>=<full|no-delete>`, e.g.:

```properties
import.managed.required-action=no-delete
```
### State management

If `import.remote-state.enabled` is set to `true` (default value), keycloak-config-cli will purge only resources they created before by keycloak-config-cli. If `import.remote-state.enabled` is set to `false`, keycloak-config-cli will purge all existing entities if they are not defined in import json.
