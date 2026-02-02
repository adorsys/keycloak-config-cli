# Working with Arrays

In keycloak-config-cli configuration files, arrays represent collections such as redirect URIs, roles, group memberships, and protocol mappers. Understanding how arrays behave during import is critical for managing configurations correctly, especially when adding, removing, or updating items.

Related issues: [#1237](https://github.com/adorsys/keycloak-config-cli/issues/1237)

## The Problem

When working with arrays in configuration files, users often encounter confusion about:
- Whether adding a new item will remove existing items
- How to remove specific items from an array
- Why duplicate items appear or items don't update as expected
- How keycloak-config-cli determines which items to manage

## Array Behavior with Remote State

The behavior of arrays depends on the `import.remote-state.enabled` setting:

| Setting | Behavior |
|---------|----------|
| `import.remote-state.enabled=true` (default) | keycloak-config-cli tracks managed items. Only items created/managed by keycloak-config-cli are modified. |
| `import.remote-state.enabled=false` | Arrays are replaced entirely with the configuration file contents. |

## Usage

### Adding Items to Arrays

**Scenario:** Add a new redirect URI to an existing client without removing existing ones.
```yaml
realm: "myrealm"
clients:
  - clientId: "my-app"
    redirectUris:
      - "https://app.example.com/callback"
      - "https://app.example.com/oauth/callback"
      - "http://localhost:3000/callback"  # New URI being added
```

**Result:**
- With remote state enabled (default): New URI is added; existing URIs that were created by keycloak-config-cli remain
- Without remote state: Redirect URIs list contains only these three values

### Removing Items from Arrays

**Scenario:** Remove an outdated redirect URI that's no longer valid.

**Before:**
```yaml
clients:
  - clientId: "my-app"
    redirectUris:
      - "https://app.example.com/callback"
      - "https://old-app.example.com/callback"  # This needs to be removed
      - "http://localhost:3000/callback"
```

**After:**
```yaml
clients:
  - clientId: "my-app"
    redirectUris:
      - "https://app.example.com/callback"
      - "http://localhost:3000/callback"
```

**Result:** Simply remove the item from your configuration file. On the next import, keycloak-config-cli will detect the item is missing and remove it from Keycloak.

### Updating Array Items

**Scenario:** Modify an existing protocol mapper's configuration.
```yaml
clients:
  - clientId: "my-app"
    protocolMappers:
      - name: "username-mapper"
        protocol: "openid-connect"
        protocolMapper: "oidc-usermodel-property-mapper"
        config:
          user.attribute: "username"
          claim.name: "preferred_username"
          jsonType.label: "String"
          id.token.claim: "true"  # Updated from "false"
```

**Result:** The existing protocol mapper is updated with the new configuration. keycloak-config-cli identifies the item by its `name` property and updates it rather than creating a duplicate.

## How Array Items Are Identified

Different array types use different properties for identification:

| Array Type | Identification Property | Example |
|------------|------------------------|---------|
| Clients | `clientId` | `"my-app"` |
| Users | `username` or `email` | `"john.doe"` |
| Groups | `name` or `path` | `"/developers"` |
| Roles | `name` | `"admin"` |
| Protocol Mappers | `name` | `"username-mapper"` |
| Identity Providers | `alias` | `"google"` |

**Important:** If the identification property doesn't match exactly, a new item will be created instead of updating the existing one.

## Common Pitfalls

### 1. Duplicate Items

**Problem:**
```yaml
users:
  - username: "john.doe"
    groups:
      - "developers"
      - "developers"  # Duplicate
```

**Solution:** Remove duplicates from your configuration. Keycloak may reject duplicate values.

### 2. Order Dependency

**Problem:** Assuming array items will maintain a specific order.

**Solution:** Don't rely on array ordering for business logic. Keycloak doesn't guarantee order preservation for most arrays.

### 3. Mixed Management Approaches

**Problem:** Manually adding items via Keycloak Admin UI while also managing them via keycloak-config-cli.

**Solution:**
- **Option A**: Manage everything via keycloak-config-cli (recommended)
- **Option B**: Use `import.remote-state.enabled=true` (default) and only manage specific items via config. Items created manually in the UI will remain untouched.

## Best Practices

1. **Be Explicit**: Always list all desired values in configuration files
2. **Use Remote State**: Keep `import.remote-state.enabled=true` (default) for better control
3. **Validate First**: Use `--import.validate=true` before applying changes
4. **Version Control**: Store configurations in Git to track changes over time
5. **Test in Dev First**: Apply changes to development environments before production
6. **Document Assumptions**: Add comments in configuration files explaining why certain values exist

## Configuration Options
```bash
# Enable remote state (default) - tracks which items keycloak-config-cli manages
--import.remote-state.enabled=true

# Validate configuration before import
--import.validate=true

# Enable parallel import for better performance with large arrays
--import.parallel=true
```

## Consequences

When working with arrays in keycloak-config-cli:

1. **Remote State Tracking**: With `import.remote-state.enabled=true` (default), keycloak-config-cli only manages resources it created. Manually added items remain untouched.
2. **Full Replacement Mode**: With `import.remote-state.enabled=false`, the entire array is replaced with what's in your configuration file.
3. **Item Identification**: Items are matched by their identifying property (e.g., `name`, `clientId`). Changing this property creates a new item instead of updating.
4. **No Guaranteed Ordering**: Array item order may not be preserved. Don't rely on position for application logic.