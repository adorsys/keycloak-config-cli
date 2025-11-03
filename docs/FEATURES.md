# Supported features

| Feature                                            | Since  | Description                                                                                              |
|----------------------------------------------------|--------|----------------------------------------------------------------------------------------------------------|
| Create clients                                     | 1.0.0  | Create client configuration (inclusive protocolMappers) while creating or updating realms                |
| Update clients                                     | 1.0.0  | Update client configuration (inclusive protocolMappers) while updating realms                            |
| Manage fine-grained authorization of clients       | 2.2.0  | Add and remove fine-grained authorization resources and policies of clients                              |
| Add roles                                          | 1.0.0  | Add roles while creating or updating realms                                                              |
| Update roles                                       | 1.0.0  | Update role properties while updating realms                                                             |
| Add composites to roles                            | 1.3.0  | Add role with realm-level and client-level composite roles while creating or updating realms             |
| Add composites to roles                            | 1.3.0  | Add realm-level and client-level composite roles to existing role while creating or updating realms      |
| Remove composites from roles                       | 1.3.0  | Remove realm-level and client-level composite roles from existing role while creating or updating realms |
| Add users                                          | 1.0.0  | Add users (inclusive password!) while creating or updating realms                                        |
| Add users with roles                               | 1.0.0  | Add users with realm-level and client-level roles while creating or updating realms                      |
| Update users                                       | 1.0.0  | Update user properties (inclusive password!) while updating realms                                       |
| Add role to user                                   | 1.0.0  | Add realm-level and client-level roles to user while updating realm                                      |
| Remove role from user                              | 1.0.0  | Remove realm-level or client-level roles from user while updating realm                                  |
| Add groups to user                                 | 2.0.0  | Add groups to user while updating realm                                                                  |
| Remove groups from user                            | 2.0.0  | Remove groups from user while updating realm                                                             |
| Add authentication flows and executions            | 1.0.0  | Add authentication flows and executions while creating or updating realms                                |
| Update authentication flows and executions         | 1.0.0  | Update authentication flow properties and executions while updating realms                               |
| Remove authentication flows and executions         | 2.0.0  | Remove existing authentication flow properties and executions while updating realms                      |
| Update builtin authentication flows and executions | 2.0.0  | Update builtin authentication flow properties and executions while updating realms                       |
| Add authentication configs                         | 1.0.0  | Add authentication configs while creating or updating realms                                             |
| Update authentication configs                      | 2.0.0  | Update authentication configs while updating realms                                                      |
| Remove authentication configs                      | 2.0.0  | Remove existing authentication configs while updating realms                                             |
| Add components                                     | 1.0.0  | Add components while creating or updating realms                                                         |
| Update components                                  | 1.0.0  | Update components properties while updating realms                                                       |
| Remove components                                  | 2.0.0  | Remove existing sub-components while creating or updating realms                                         |
| Update sub-components                              | 1.0.0  | Add sub-components properties while creating or updating realms                                          |
| Remove sub-components                              | 2.0.0  | Remove existing sub-components while creating or updating realms                                         |
| Add groups                                         | 1.3.0  | Add groups (inclusive subgroups!) to realm while creating or updating realms                             |
| Update groups                                      | 1.3.0  | Update existing group properties and attributes while creating or updating realms                        |
| Remove groups                                      | 1.3.0  | Remove existing groups while updating realms                                                             |
| Add/Remove group attributes                        | 1.3.0  | Add or remove group attributes in existing groups while updating realms                                  |
| Add/Remove group roles                             | 1.3.0  | Add or remove roles to/from existing groups while updating realms                                        |
| Update/Remove subgroups                            | 1.3.0  | Like groups, subgroups may also be added/updated and removed while updating realms                       |
| Add scope-mappings                                 | 1.0.0  | Add scope-mappings while creating or updating realms                                                     |
| Add roles to scope-mappings                        | 1.0.0  | Add roles to existing scope-mappings while updating realms                                               |
| Remove roles from scope-mappings                   | 1.0.0  | Remove roles from existing scope-mappings while updating realms                                          |
| Add required-actions                               | 1.0.0  | Add required-actions while creating or updating realms                                                   |
| Update required-actions                            | 1.0.0  | Update properties of existing required-actions while updating realms                                     |
| Remove required-actions                            | 2.0.0  | Remove existing required-actions while updating realms                                                   |
| Add identity providers                             | 1.2.0  | Add identity providers while creating or updating realms                                                 |
| Update identity providers                          | 1.2.0  | Update identity providers while updating realms (improved with 2.0.0)                                    |
| Remove identity providers                          | 2.0.0  | Remove identity providers while updating realms                                                          |
| Add identity provider mappers                      | 2.0.0  | Add identityProviderMappers while updating realms                                                        |
| Update identity provider mappers                   | 2.0.0  | Update identityProviderMappers while updating realms                                                     |
| Remove identity provider mappers                   | 2.0.0  | Remove identityProviderMappers while updating realms                                                     |
| Add clientScopes                                   | 2.0.0  | Add clientScopes (inclusive protocolMappers) while creating or updating realms                           |
| Update clientScopes                                | 2.0.0  | Update existing (inclusive protocolMappers) clientScopes while creating or updating realms               |
| Remove clientScopes                                | 2.0.0  | Remove existing clientScopes while creating or updating realms                                           |
| Add clientScopeMappings                            | 2.5.0  | Add clientScopeMapping while creating or updating realms                                                 |
| Update clientScopeMappings                         | 2.5.0  | Update existing clientScopeMappings while creating or updating realms                                    |
| Remove clientScopeMappings                         | 2.5.0  | Remove existing clientScopeMappings while creating or updating realms                                    |
| Synchronize user federation                        | 3.5.0  | Synchronize the user federation defined on the realm configuration                                       |
| Synchronize user profile                           | 5.4.0  | Synchronize the user profile configuration defined on the realm configuration                            |
| Synchronize client-policies                        | 5.6.0  | Synchronize the client-policies (clientProfiles and clientPolicies) while updating realms                |
| Synchronize message bundles                        | 5.12.0 | Synchronize message bundles defined on the realm configuration                                           |
| Normalize realm exports                            | x.x.x  | Normalize a full realm export to be more minimal                                                         |
| JavaScript Variable Substitution                   | x.x.x  | Evaluate JavaScript expressions in configuration files                                                   |

# Specificities

# Client - authenticationFlowBindingOverrides

`authenticationFlowBindingOverrides` on client is configured by Keycloak like this,

```json
{
  "authenticationFlowBindingOverrides": {
    "browser": "ad7d518c-4129-483a-8351-e1223cb8eead"
  }
}
```

In order to be able to configure this in `keycloak-config-cli`, we use authentication flow alias instead of `id` (which is not known)

`keycloak-config-cli` will automatically resolve the alias reference to its ids.

So if you need this, you have to configure it like :

```json
{
  "authenticationFlowBindingOverrides": {
    "browser": "my awesome browser flow"
  }
}
```

# User - initial password

To set an initial password that is only respect while the user is created, the userLabel must be named `initial`.

```json
{
  "users": [
    {
      "username": "user",
      "email": "user@mail.de",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "userLabel": "initial",
          "value": "start123"
        }
      ]
    }
  ]
}
```

# Fine-grained permissions for Keycloak objects

Keycloak supports two versions of fine-grained admin permissions (FGAP):

## FGAP V1 (Keycloak < 26.2) - realm-management client

The resources and policies are configured on the client named `realm-management`:

```json
{
  "clients": [
    {
      "clientId": "realm-management",
      "authorizationSettings": {
        "allowRemoteResourceManagement": false,
        "policyEnforcementMode": "ENFORCING",
        "resources": [
          {
            "name": "idp.resource.1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22",
            "type": "IdentityProvider",
            "ownerManagedAccess": false,
            "scopes": [
              {
                "name": "token-exchange"
              }
            ]
          }
        ],
        "policies": [
          {
            "name": "token-exchange.permission.idp.1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22",
            "type": "scope",
            "logic": "POSITIVE",
            "decisionStrategy": "UNANIMOUS",
            "config": {
              "resources": "[\"idp.resource.1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22\"]",
              "scopes": "[\"token-exchange\"]"
            }
          }
        ]
      }
    }
  ],
  "identityProviders": [
    {
      "alias": "my-identity-provider",
      "providerId": "oidc",
      "enabled": true
    }
  ]
}
```

Both resources and policies are named in such a way that the name contains the UUID of the referenced entity (identity provider in the example).
This is problematic, as the UUID is not known.

Therefore `keycloak-config-cli` will automatically resolve the object ids during import, using a special dollar syntax:

The following transformations are currently implemented:

| Resource                              | Permission                                                | Resolution strategy                |
|---------------------------------------|-----------------------------------------------------------|------------------------------------|
| `client.resource.$client-id`          | `<scope>.permission.client.$client-id`                    | Find a client by client id         |
| `idp.resource.$alias`                 | `<scope>.permission.idp.$alias`                           | Find an identity provider by alias |
| `role.resource.$Realm Role Name`      | `<scope>.permission.$Realm Role Name` (Note: No `.role.`) | Find a realm role by name          |
| `group.resource.$/Full Path/To Group` | `<scope>.permission.group.$/Full Path/To Group`           | Find a group by full path          |

The dollar only marks the name for substitution but is not part of it. It is an import failure when the referenced entity does not exist.

The example above should therefore be rewritten as:


```json
{
  "clients": [
    {
      "clientId": "realm-management",
      "authorizationSettings": {
        "allowRemoteResourceManagement": false,
        "policyEnforcementMode": "ENFORCING",
        "resources": [
          {
            "name": "idp.resource.$my-identity-provider",
            "type": "IdentityProvider",
            "ownerManagedAccess": false,
            "scopes": [
              {
                "name": "token-exchange"
              }
            ]
          }
        ],
        "policies": [
          {
            "name": "token-exchange.permission.idp.$my-identity-provider",
            "type": "scope",
            "logic": "POSITIVE",
            "decisionStrategy": "UNANIMOUS",
            "config": {
              "resources": "[\"idp.resource.$my-identity-provider\"]",
              "scopes": "[\"token-exchange\"]"
            }
          }
        ]
      }
    }
  ],
  "identityProviders": [
    {
      "alias": "my-identity-provider",
      "providerId": "oidc",
      "enabled": true
    }
  ]
}
```

# Migration Guide

## FGAP V2 (Keycloak 26.2+) - admin-permissions client

Starting with Keycloak 26.2, FGAP V2 is the default. V2 introduces a cleaner permission model with improved manageability.

V2 permissions are configured on the `admin-permissions` client. Unlike V1, V2 uses `authorizationSchema` to define resource types and their available scopes.

### Configuring V2 Permissions

To configure FGAP V2 permissions, enable admin permissions. Note that the `admin-permissions` client itself is system-managed and cannot be fully configured via import files. However, you can configure authorization for your own clients:

```yaml
realm: my-realm
adminPermissionsEnabled: true  # Enables FGAP V2 - creates admin-permissions client
enabled: true

# Note: Do NOT include admin-permissions client with authorizationSettings in your import configuration
# Authorization for this client is system-managed by Keycloak
# Manage permissions through Admin Console after realm import

clients:
  - clientId: test-client
    enabled: true
    authorizationServicesEnabled: true
    authorizationSettings:
      allowRemoteResourceManagement: true
      policyEnforcementMode: ENFORCING
      resources:
        - name: premium-resource
          type: urn:test-client:resources:premium
          ownerManagedAccess: false
          scopes: ["view", "delete"]
      policies:
        - name: only-client-admins
          type: role
          logic: POSITIVE
          decisionStrategy: UNANIMOUS
          config:
            roles: '[{"id":"client-admin","required":true}]'
        - name: premium-resource-permission
          type: resource
          logic: POSITIVE
          decisionStrategy: UNANIMOUS
          config:
            defaultResourceType: urn:test-client:resources:premium
            resources: '["premium-resource"]'
            applyPolicies: '["only-client-admins"]'
roles:
  realm:
    - name: client-admin
      description: Can manage specific clients
```

**Key V2 concepts:**

1. **authorizationSchema** - Defines available resource types (Groups, Users, Clients, Roles) and their scopes. Required for V2.
2. **Permissions as scope policies** - V2 permissions are `type: "scope"` policies with `defaultResourceType` config
3. **Resource references** - Use client IDs, group paths, role names directly (or `$placeholder` syntax)
4. **Policy references** - `applyPolicies` links permissions to access conditions

### V2 Resource Types

V2 defines four resource types, each with specific scopes:

| Resource Type | Available Scopes |
|---------------|------------------|
| **Clients** | view, manage, map-roles, map-roles-client-scope, map-roles-composite |
| **Groups** | manage-members, manage-membership, view, manage, view-members, impersonate-members |
| **Users** | manage-group-membership, view, map-roles, manage, impersonate |
| **Roles** | map-role, map-role-composite, map-role-client-scope |

### Resource Reference Syntax

Both V1 and V2 support placeholder syntax for referencing resources in policy configurations. keycloak-config-cli automatically transforms these placeholders based on the active FGAP version.

**Syntax options:**

| Syntax | V1 Transformation | V2 Transformation | When to Use |
|--------|-------------------|-------------------|-------------|
| `$client-id` (bare) | `client.resource.<uuid>` | `<uuid>` | V2 with `defaultResourceType: "Clients"` |
| `client.resource.$client-id` (full) | `client.resource.<uuid>` | `<uuid>` | Both V1 and V2 |
| `idp.resource.$alias` (full) | `idp.resource.<uuid>` | `<uuid>` | Both V1 and V2 |
| `group.resource.$/path` (full) | `group.resource.<uuid>` | `<uuid>` | Both V1 and V2 |

**V2 bare syntax example (recommended):**
```json
{
  "policies": [
    {
      "name": "manage-test-client-permission",
      "type": "scope",
      "config": {
        "defaultResourceType": "Clients",
        "resources": "[\"$test-client\"]",
        "scopes": "[\"manage\"]"
      }
    }
  ]
}
```

**Full-path syntax example (works in both V1 and V2):**
```json
{
  "config": {
    "resources": "[\"client.resource.$test-client\"]"
  }
}
```

**Supported resource types:**
- `Clients` - Client IDs (e.g., `$my-client-id`)
- `Groups` - Group paths (e.g., `$/my-group` or `$my-group-name`)
- `IdentityProviders` - IDP aliases (e.g., `$my-idp-alias`)
- `Roles` - Role names (e.g., `$my-role-name`)
- `Users` - User references (V2 only)

**Important notes:**
- V2 bare syntax requires `defaultResourceType` in policy config
- Full-path syntax works in both versions without `defaultResourceType`
- keycloak-config-cli auto-detects the FGAP version and applies correct transformation
- V2 requires `authorizationSchema` section - see [example config](../contrib/example-config/fgap-v2.json)

**Troubleshooting:**
If "All Clients" is selected instead of a specific client, the resource reference wasn't transformed. Verify:
1. You're using keycloak-config-cli with FGAP V2 support
2. For bare syntax, `defaultResourceType` is specified
3. The referenced resource exists in the realm

### V2 Import Behavior

When importing V2 configs:
- **Authorization settings for `admin-permissions` client are skipped** - This client is system-managed by Keycloak
- Policies and permissions must be managed through Keycloak Admin Console or dedicated FGAP V2 REST APIs
- The `authorizationSchema` section is processed but resource type definitions are auto-managed by Keycloak

**Important: Do NOT include `admin-permissions` client with `authorizationSettings` in your import files.**

When keycloak-config-cli detects `admin-permissions` client with authorization settings in FGAP V2:
```
Skipping authorization settings for 'admin-permissions' client in realm '[realm]' -
FGAP V2 manages this client internally and blocks API access.
```

**Why this limitation exists:**

Keycloak intentionally blocks standard Authorization Services API endpoints for the `admin-permissions` client in FGAP V2 (returns HTTP 400 with "unknown_error"). This is by design to prevent external modification of the system-managed authorization model. See [Keycloak issue #43977](https://github.com/keycloak/keycloak/issues/43977) for details.

**Correct approach:**
1. Set `adminPermissionsEnabled: true` at realm level
2. Remove `admin-permissions` client from your import configuration
3. Manage permissions post-import through:
   - Keycloak Admin Console → Realm Settings → Permissions
   - Direct FGAP V2 REST API calls (after realm creation)

### Troubleshooting FGAP V2

**Error: "Policy with name [PolicyName] already exists" (409 Conflict)**

This occurs when trying to update policies in the `admin-permissions` client. The client is system-managed in FGAP V2 and cannot be modified via config files.

**Solution:** Remove the `admin-permissions` client from your import configuration and use `adminPermissionsEnabled: true` at the realm level instead.

**Error: "unknown_error" (400 Bad Request)**

Keycloak intentionally blocks API access to `admin-permissions` authorization settings in FGAP V2. This is expected behavior, not a bug.

**Solution:** Same as above - use realm-level flag and manage permissions through Admin Console.

### V1 to V2 Migration

Automatic migration from V1 to V2 is not available per Keycloak documentation. V1 authorization on `realm-management` will be skipped with warnings on V2 realms.

To use V2: Enable `adminPermissionsEnabled: true` and configure permissions on `admin-permissions` client as shown above.


### Keycloak Version 25.0.1

#### Basic Scope Handling

With the introduction of the dedicated "basic" scope in Keycloak, existing realm configurations with custom clients might not contain the `sub` claim anymore. This is because the new `basic` scope that emits those claims might be removed by an explicit `defaultClientScopes` configuration.

A workaround is to configure the `basic` scope explicitly via `defaultClientScopes`:
```yaml
defaultClientScopes:
  - "basic"
```
Ensure that your client configurations include the basic scope to maintain the presence of the sub claim in access tokens.

#### Example Client Configuration
Here is an example of a previously working client definition, which will produce access tokens with the sub claim.
```yaml
  - clientId: app-greetme
    protocol: openid-connect
    name: Acme Greet Me
    description: "App Greet Me Description"
    enabled: true
    publicClient: true
    standardFlowEnabled: true
    directAccessGrantsEnabled: false
    alwaysDisplayInConsole: true
    serviceAccountsEnabled: false
    fullScopeAllowed: false
    rootUrl: "$(env:APPS_FRONTEND_URL_GREETME:https://localhost:9443/apps/greet-me)"
    baseUrl: "/?realm=acme-internal&scope=openid"
    adminUrl: ""
    redirectUris:
      - "/*"
    webOrigins:
      - "+"
    defaultClientScopes:
      - "email"
    optionalClientScopes:
      - "phone"
      - "name"
      - "acme.api"
      - "address"
    attributes:
      "pkce.code.challenge.method": "S256"
      "post.logout.redirect.uris": "+"
```
To ensure the sub claim is present, update the defaultClientScopes to include the basic scope,
```yaml
   - clientId: app-greetme
     protocol: openid-connect
     name: Acme Greet Me
     description: "App Greet Me Description"
     enabled: true
     publicClient: true
     standardFlowEnabled: true
     directAccessGrantsEnabled: false
     alwaysDisplayInConsole: true
     serviceAccountsEnabled: false
     fullScopeAllowed: false
     rootUrl: "$(env:APPS_FRONTEND_URL_GREETME:https://localhost:9443/apps/greet-me)"
     baseUrl: "/?realm=acme-internal&scope=openid"
     adminUrl: ""
     redirectUris:
       - "/*"
     webOrigins:
       - "+"
     defaultClientScopes:
       - "basic"
       - "email"
     optionalClientScopes:
       - "phone"
       - "name"
       - "acme.api"
       - "address"
     attributes:
       "pkce.code.challenge.method": "S256"
       "post.logout.redirect.uris": "+"
```







