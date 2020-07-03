# Full managed entities

keycloak-config-cli manage some entities absolutely. It means for example if a group isn't defined
inside the import json but other groups are specified, keycloak-config-cli will calculate the
difference and delete the group from keycloak.

In some cases it is required to include some keycloak defaults because keycloak-config-cli can't
detect if the entity comes from a user or auto created by keycloak itself.

There are 2 modes to ensure a specific behavior:

### 1. Keycloak should not manage type of entities:

For example if you don't define any `groups` inside the import json, keycloak does not touch any `groups`.

### 2. Keycloak manage type of entities:

For example define any `groups` you want inside the import json, keycloak ensure that the groups are available but other
groups will be deleted.

### 2.1 Special behavior:

For example if you define `groups` but set an empty array, keycloak will delete all groups in keycloak.

## Supported full managed entities

| Type                        | Additional Information                                                           | Property Name               |
| --------------------------- | -------------------------------------------------------------------------------- | --------------------------- |
| Groups                      | -                                                                                | `group`                     |
| Required Actions            | You have to copy the default one to you import json.                             | `required-action`           |
| Client Scopes               | -                                                                                | `client-scope`              |
| Scope Mappings              | -                                                                                | `scope-mapping`             |
| Components                  | You have to copy the default components to you import json.                      | `component`                 |
| Sub Components              | You have to copy the default components to you import json.                      | `sub-component`             |
| Authentication Flows        | You have to copy the default components to you import json, expect bulitin flows | `authentication-flow`       |
| Identity Providers          | -                                                                                | `identity-provider`         |
| Identity Provider Mappers   | -                                                                                | `identity-provider-mapper`  |

## Disable deletion of managed entities

If you won't delete properties of a specific type, you can disable this behavior by default a properties like `import.managed.<entity>=<full|no-delete>`, e.g.:
`import.managed.required-actions=no-delete`

## State management

If `import.state` is set to `true` (default value), keycloak-config-cli will purge only resources they created before by keycloak-config-cli.
If `import.state` is set to `false`, keycloak-config-cli will purge all existing entities if they not defined in import json.

### Supported entities

Following entities does have saved state:

- Required Actions
- Clients
- Components
