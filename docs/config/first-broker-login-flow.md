# First Broker Login Flow Configuration

When configuring Keycloak identity provider integration, the `firstBrokerLoginFlow` setting controls the authentication flow that executes after a user first logs in through an identity provider. This flow handles actions like creating new user accounts or linking existing Keycloak accounts to identity provider accounts.

Related issues: [#1481](https://github.com/adorsys/keycloak-config-cli/issues/1481)

## The Problem

Users often encounter HTTP 500 Internal Server Error (NullPointerException) when importing a realm configuration that defines a custom firstBrokerLoginFlow because:
- The keycloak-config-cli tries to update the realm before creating the authentication flow
- The flow reference exists in the realm configuration before the flow itself is imported
- The import order doesn't account for dependencies between realm settings and authentication flows

## The Solution

The keycloak-config-cli now automatically handles the correct import order for first broker login flows:
1. Authentication flows are imported first
2. Then realm configuration (including flow references) is applied

## Configuration Example

Here's a complete example of configuring a custom first broker login flow:

```yaml
realm: my-realm
firstBrokerLoginFlow: first-broker-login-sso-only
authenticationFlows:
  - alias: first-broker-login-sso-only
    description: Actions taken after first broker login with identity provider account, which is not yet linked to any Keycloak account
    providerId: basic-flow
    topLevel: true
    builtIn: false
    authenticationExecutions:
      - authenticatorConfig: first-broker-login-sso-only-create-unique-user-config
        authenticator: idp-create-user-if-unique
        authenticatorFlow: false
        requirement: ALTERNATIVE
        priority: 20
        userSetupAllowed: false
      - authenticator: idp-auto-link
        authenticatorFlow: false
        requirement: ALTERNATIVE
        priority: 60
        userSetupAllowed: false
```

## Key Components

### Flow Definition
- **alias**: Unique identifier for the flow
- **description**: Human-readable description of the flow's purpose
- **providerId**: Type of flow (typically "basic-flow")
- **topLevel**: Indicates if this is a top-level flow
- **builtIn**: Whether this is a built-in Keycloak flow

### Authentication Executions
- **authenticator**: The authenticator to execute (e.g., `idp-create-user-if-unique`, `idp-auto-link`)
- **authenticatorConfig**: Configuration for the authenticator (optional)
- **requirement**: Execution requirement (`ALTERNATIVE`, `REQUIRED`, `DISABLED`)
- **priority**: Execution order (lower numbers execute first)
- **userSetupAllowed**: Whether users can configure this during setup

## Common Use Cases

### 1. Create User If Unique
This execution creates a new Keycloak user if the identity provider user doesn't exist:

```yaml
- authenticator: idp-create-user-if-unique
  requirement: ALTERNATIVE
  priority: 20
```

### 2. Auto-Link Existing Users
This execution automatically links the identity provider account to an existing Keycloak user:

```yaml
- authenticator: idp-auto-link
  requirement: ALTERNATIVE
  priority: 60
```

## Best Practices

1. **Flow Naming**: Use descriptive aliases that clearly indicate the flow's purpose
2. **Priority Ordering**: Set priorities to ensure proper execution sequence
3. **Requirement Logic**: Use `ALTERNATIVE` for multiple options, `REQUIRED` for mandatory steps
4. **Testing**: Always test flows in a development environment before production deployment

## Troubleshooting

### Common Errors

**HTTP 500 Internal Server Error**
- **Cause**: Flow referenced in realm configuration before being created
- **Solution**: Ensure flow definition exists in the same configuration file as the realm reference

**Flow Not Found**
- **Cause**: Incorrect flow alias or missing flow definition
- **Solution**: Verify the flow alias matches exactly between the definition and realm reference

**Authentication Failures**
- **Cause**: Incorrect authenticator configuration or execution order
- **Solution**: Review authenticator requirements and priorities

### Debugging Steps

1. Check that the flow alias in `firstBrokerLoginFlow` matches the flow definition
2. Verify all required authenticators are properly configured
3. Test the flow manually in the Keycloak admin console
4. Review keycloak-config-cli logs for detailed error messages

## Migration from Previous Versions

If you're upgrading from a version before this fix, you may need to:

1. Ensure your authentication flow definitions are complete and valid
2. Verify that all referenced flows exist in your configuration
3. Test the import process in a non-production environment

The fix ensures that authentication flows are always imported before realm settings that reference them, eliminating the previous ordering issues.
