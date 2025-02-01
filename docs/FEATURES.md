# Supported features

| Feature                                            | Since | Description                                                                                              |
|----------------------------------------------------|-------|----------------------------------------------------------------------------------------------------------|
| Create clients                                     | 1.0.0 | Create client configuration (inclusive protocolMappers) while creating or updating realms                |
| Update clients                                     | 1.0.0 | Update client configuration (inclusive protocolMappers) while updating realms                            |
| Manage fine-grained authorization of clients       | 2.2.0 | Add and remove fine-grained authorization resources and policies of clients                              |
| Add roles                                          | 1.0.0 | Add roles while creating or updating realms                                                              |
| Update roles                                       | 1.0.0 | Update role properties while updating realms                                                             |
| Add composites to roles                            | 1.3.0 | Add role with realm-level and client-level composite roles while creating or updating realms             |
| Add composites to roles                            | 1.3.0 | Add realm-level and client-level composite roles to existing role while creating or updating realms      |
| Remove composites from roles                       | 1.3.0 | Remove realm-level and client-level composite roles from existing role while creating or updating realms |
| Add users                                          | 1.0.0 | Add users (inclusive password!) while creating or updating realms                                        |
| Add users with roles                               | 1.0.0 | Add users with realm-level and client-level roles while creating or updating realms                      |
| Update users                                       | 1.0.0 | Update user properties (inclusive password!) while updating realms                                       |
| Add role to user                                   | 1.0.0 | Add realm-level and client-level roles to user while updating realm                                      |
| Remove role from user                              | 1.0.0 | Remove realm-level or client-level roles from user while updating realm                                  |
| Add groups to user                                 | 2.0.0 | Add groups to user while updating realm                                                                  |
| Remove groups from user                            | 2.0.0 | Remove groups from user while updating realm                                                             |
| Add authentication flows and executions            | 1.0.0 | Add authentication flows and executions while creating or updating realms                                |
| Update authentication flows and executions         | 1.0.0 | Update authentication flow properties and executions while updating realms                               |
| Remove authentication flows and executions         | 2.0.0 | Remove existing authentication flow properties and executions while updating realms                      |
| Update builtin authentication flows and executions | 2.0.0 | Update builtin authentication flow properties and executions while updating realms                       |
| Add authentication configs                         | 1.0.0 | Add authentication configs while creating or updating realms                                             |
| Update authentication configs                      | 2.0.0 | Update authentication configs while updating realms                                                      |
| Remove authentication configs                      | 2.0.0 | Remove existing authentication configs while updating realms                                             |
| Add components                                     | 1.0.0 | Add components while creating or updating realms                                                         |
| Update components                                  | 1.0.0 | Update components properties while updating realms                                                       |
| Remove components                                  | 2.0.0 | Remove existing sub-components while creating or updating realms                                         |
| Update sub-components                              | 1.0.0 | Add sub-components properties while creating or updating realms                                          |
| Remove sub-components                              | 2.0.0 | Remove existing sub-components while creating or updating realms                                         |
| Add groups                                         | 1.3.0 | Add groups (inclusive subgroups!) to realm while creating or updating realms                             |
| Update groups                                      | 1.3.0 | Update existing group properties and attributes while creating or updating realms                        |
| Remove groups                                      | 1.3.0 | Remove existing groups while updating realms                                                             |
| Add/Remove group attributes                        | 1.3.0 | Add or remove group attributes in existing groups while updating realms                                  |
| Add/Remove group roles                             | 1.3.0 | Add or remove roles to/from existing groups while updating realms                                        |
| Update/Remove subgroups                            | 1.3.0 | Like groups, subgroups may also be added/updated and removed while updating realms                       |
| Add scope-mappings                                 | 1.0.0 | Add scope-mappings while creating or updating realms                                                     |
| Add roles to scope-mappings                        | 1.0.0 | Add roles to existing scope-mappings while updating realms                                               |
| Remove roles from scope-mappings                   | 1.0.0 | Remove roles from existing scope-mappings while updating realms                                          |
| Add required-actions                               | 1.0.0 | Add required-actions while creating or updating realms                                                   |
| Update required-actions                            | 1.0.0 | Update properties of existing required-actions while updating realms                                     |
| Remove required-actions                            | 2.0.0 | Remove existing required-actions while updating realms                                                   |
| Add identity providers                             | 1.2.0 | Add identity providers while creating or updating realms                                                 |
| Update identity providers                          | 1.2.0 | Update identity providers while updating realms (improved with 2.0.0)                                    |
| Remove identity providers                          | 2.0.0 | Remove identity providers while updating realms                                                          |
| Add identity provider mappers                      | 2.0.0 | Add identityProviderMappers while updating realms                                                        |
| Update identity provider mappers                   | 2.0.0 | Update identityProviderMappers while updating realms                                                     |
| Remove identity provider mappers                   | 2.0.0 | Remove identityProviderMappers while updating realms                                                     |
| Add clientScopes                                   | 2.0.0 | Add clientScopes (inclusive protocolMappers) while creating or updating realms                           |
| Update clientScopes                                | 2.0.0 | Update existing (inclusive protocolMappers) clientScopes while creating or updating realms               |
| Remove clientScopes                                | 2.0.0 | Remove existing clientScopes while creating or updating realms                                           |
| Add clientScopeMappings                            | 2.5.0 | Add clientScopeMapping while creating or updating realms                                                 |
| Update clientScopeMappings                         | 2.5.0 | Update existing clientScopeMappings while creating or updating realms                                    |
| Remove clientScopeMappings                         | 2.5.0 | Remove existing clientScopeMappings while creating or updating realms                                    |
| Synchronize user federation                        | 3.5.0 | Synchronize the user federation defined on the realm configuration                                       |
| Synchronize user profile                           | 5.4.0 | Synchronize the user profile configuration defined on the realm configuration                            |
| Synchronize client-policies                        | 5.6.0 | Synchronize the client-policies (clientProfiles and clientPolicies) while updating realms                |
| Synchronize message bundles                        | 5.12.0 | Synchronize message bundles defined on the realm configuration                                           |
| Normalize realm exports                            | x.x.x | Normalize a full realm export to be more minimal                                                         |

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

Keycloak supports configuring access to certain resource (such as clients, identity providers, roles and groups) using advanced policies.

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







