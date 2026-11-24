# Fine-Grained Admin Permissions (FGAP) V2

Keycloak 26.2 introduced Fine-Grained Admin Permissions V2 (FGAP V2). It
centralizes administrative policies and permissions in a system-managed
`admin-permissions` client and supports permissions for users, groups, clients,
and roles.

keycloak-config-cli can enable FGAP V2 and import its policies and scope
permissions declaratively on Keycloak 26.2 and later.

## Key concepts

| Aspect | FGAP V1 | FGAP V2 |
| --- | --- | --- |
| Keycloak version | Before 26.2, or the V1 preview feature | 26.2+ |
| Authorization client | `realm-management` | `admin-permissions` |
| Enablement | V1 server feature | `adminPermissionsEnabled: true` |
| Permission model | V1 resources and policies | Typed resources, explicit scopes, and policies |
| keycloak-config-cli import | Standard Authorization Services import | Dedicated FGAP V2 policy and scope-permission APIs |

The `admin-permissions` client is internal and remains under Keycloak's control.
An import file may contain an entry with that `clientId`, but
keycloak-config-cli uses the entry only as the source for policies and scope
permissions. It does not create, replace, or update the system-managed client.

!!! note
    Keycloak does not automatically migrate a V1 authorization model to V2.
    Recreate the required permissions and policies in the V2 model before
    disabling V1.

## Complete import example

This example enables FGAP V2, creates a realm role and a protected client, and
grants administrators with the `client-admin` role the `manage` and `view`
scopes for that client.

```yaml
realm: my-realm
enabled: true
adminPermissionsEnabled: true

roles:
  realm:
    - name: client-admin
      description: Can manage selected clients

clients:
  - clientId: my-client
    enabled: true

  - clientId: admin-permissions
    authorizationSettings:
      policies:
        - name: client-managers-policy
          type: role
          logic: POSITIVE
          decisionStrategy: UNANIMOUS
          config:
            roles: '[{"id":"client-admin","required":true}]'

        - name: manage-my-client-permission
          type: scope
          logic: POSITIVE
          decisionStrategy: UNANIMOUS
          config:
            defaultResourceType: Clients
            resources: '["$my-client"]'
            scopes: '["manage","view"]'
            applyPolicies: '["client-managers-policy"]'

      authorizationSchema:
        resourceTypes:
          Clients:
            type: Clients
            scopes:
              - view
              - manage
              - map-roles
              - map-roles-client-scope
              - map-roles-composite
```

The `authorizationSchema` documents the resource types and scopes in the
configuration. Keycloak manages the server-side resource type definitions;
keycloak-config-cli imports the declared policies and scope permissions through
the dedicated FGAP V2 endpoints.

The role and referenced client must exist in the imported state. Defining them
in the same realm configuration, as above, makes the dependency explicit.

## Resource references

A scope permission's `config` selects the resource type, target resources,
operations, and policies:

```yaml
config:
  defaultResourceType: Clients
  resources: '["$my-client"]'
  scopes: '["manage","view"]'
  applyPolicies: '["client-managers-policy"]'
```

The supported resource types include:

| Resource type | Reference in `resources` | Common scopes |
| --- | --- | --- |
| `Clients` | Client ID, for example `$my-client` | `view`, `manage`, `map-roles` |
| `Groups` | Group path, for example `$/engineering` | `view`, `manage`, `view-members`, `manage-members` |
| `Users` | User reference | `view`, `manage`, `map-roles`, `impersonate` |
| `Roles` | Role name | `map-role`, `map-role-composite`, `map-role-client-scope` |

The exact scopes available for a resource type can change between Keycloak
minor versions. Check the Admin Console or the official Keycloak documentation
for the server version you operate.

### Placeholder forms

For FGAP V2, the recommended form is a bare placeholder together with
`defaultResourceType`:

```json
{
  "defaultResourceType": "Clients",
  "resources": "[\"$my-client\"]"
}
```

The older full form, such as `client.resource.$my-client`, is also accepted and
is useful in configurations shared with FGAP V1. keycloak-config-cli resolves
the placeholder to the target resource identifier before writing the V2
permission.

## Import behavior

During an FGAP V2 import, keycloak-config-cli:

1. enables realm admin permissions when `adminPermissionsEnabled` is `true`;
2. lets Keycloak create and own the `admin-permissions` client;
3. imports role, user, group, and aggregate policies through the policy API;
4. imports `type: scope` policies through the FGAP V2 scope-permission API;
5. resolves configured resource and policy references;
6. reconciles the declarations by name on repeated imports.

The generic Authorization Services update endpoint is not used to replace the
internal client. This distinction allows declarative permission imports without
attempting to manage Keycloak's internal client representation.

## Verify the result in Keycloak

After a successful import:

1. Open the target realm in the Keycloak Admin Console.
2. Open **Permissions** in the left navigation.
3. Open **Policies** and verify the imported policy, such as
   `client-managers-policy`.
4. Open **Permissions** and verify the scope permission, resource type,
   resources, scopes, and associated policies.
5. Use the **Evaluation** tab to test a delegated administrator where
   applicable.

Keycloak requires explicit permission for each operation. For example, a
delegated administrator commonly needs both `view` and `manage`; `manage` does
not implicitly grant `view` in FGAP V2.

## Repeated imports and updates

Policies and scope permissions are reconciled by name. Keep each name unique
within the `admin-permissions` authorization model. A repeated import with the
same configuration should be idempotent; changing the configuration updates the
matching declaration.

### Duplicate policy name (HTTP 409)

If Keycloak returns an error similar to:

```text
Policy with name client-managers-policy already exists
```

check the complete imported configuration for two policy or permission entries
with the same name. Policy and permission names share the authorization model's
name constraints. Remove or rename the duplicate, then run the import again.

### Missing resource or policy reference

If a resource or `applyPolicies` reference cannot be resolved:

- confirm that the client, group, user, role, or policy is present;
- use the exact client ID, group path, role name, or policy name;
- confirm that `defaultResourceType` matches the referenced resource;
- import dependencies in the same configuration or before the permission.

### Permissions section is missing

Confirm that:

- the server is Keycloak 26.2 or later;
- `adminPermissionsEnabled: true` is present on the realm;
- the realm import completed successfully.

## Migrating from FGAP V1

1. Inventory the V1 policies and permissions stored in the
   `realm-management` client.
2. Enable V2 with `adminPermissionsEnabled: true`.
3. Recreate every required operation as an explicit V2 scope. Do not assume
   that `manage` also grants `view`.
4. Convert resource references to typed V2 references.
5. Import and verify the policies and permissions in the centralized
   **Permissions** section.
6. Test delegated administrators before removing the V1 feature or model.

Automatic migration is not available because the V1 and V2 permission models
are fundamentally different.

## Custom client authorization

FGAP V2 affects realm administration permissions. Authorization Services for
your own application clients continue to use their normal
`authorizationSettings` import. Do not move an application's resources and
permissions to `admin-permissions` unless they protect Keycloak administration
operations.

## Official Keycloak documentation

- [Delegating realm administration using permissions](https://www.keycloak.org/docs/latest/server_admin/#_fine_grained_permissions)
- [Keycloak upgrading guide](https://www.keycloak.org/docs/latest/upgrading/)
- [Keycloak release notes](https://www.keycloak.org/docs/latest/release_notes/)

The documentation above describes Keycloak's server model. This page describes
how keycloak-config-cli maps that model to declarative imports.
