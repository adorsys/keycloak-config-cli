# Partial Imports

When managing Keycloak configurations, you often need to update only specific parts of a realm without affecting other components. Understanding how to perform partial imports allows you to maintain modular, focused configuration files and avoid unintended changes to unrelated realm settings.

Related issues: [#1161](https://github.com/adorsys/keycloak-config-cli/issues/1161)

## The Problem

Users often encounter challenges when trying to update specific realm components because:
- Running a full realm import can affect unrelated configurations
- It's unclear how to update only clients without touching users or groups
- Separate teams managing different components need isolated configuration files
- Fear of accidentally deleting resources not included in the import file
- Uncertainty about which resources will be managed vs. left untouched
- Complex realms require modular configuration management

## What are Partial Imports?

Partial imports allow you to import only specific parts of a realm configuration without affecting other components. Instead of managing the entire realm in one large file, you can:

- Update only clients without touching users
- Add authentication flows without modifying existing ones
- Manage groups separately from roles
- Update specific client scopes without affecting others
- Split configuration across multiple files by component type or team ownership

## How Partial Imports Work

With `import.remote-state.enabled=true` (default), keycloak-config-cli tracks which resources it manages. This enables true partial imports where:

1. **Only specified resources are managed** - Resources in your config file are created/updated
2. **Unspecified resources are untouched** - Resources not in your config remain unchanged
3. **Removal is explicit** - Only resources previously managed by keycloak-config-cli can be deleted
4. **Multiple files can coexist** - Different files can manage different parts of the realm

## Usage

### Importing Only Clients

**Scenario:** You want to add or update clients without affecting users, groups, or other realm settings.

**File: `clients-config.yaml`**
```yaml
realm: "myrealm"
clients:
  - clientId: "app-frontend"
    enabled: true
    publicClient: true
    redirectUris:
      - "https://app.example.com/*"
    webOrigins:
      - "https://app.example.com"
  
  - clientId: "app-backend"
    enabled: true
    publicClient: false
    serviceAccountsEnabled: true
    clientAuthenticatorType: "client-secret"
```

**Result:**
- Only the two clients are created/updated
- Existing users remain unchanged
- Existing groups remain unchanged
- Other clients not in this file remain untouched
- No realm settings are modified

**Import command:**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=clients-config.yaml
```

---

### Importing Only Users

**Scenario:** HR team manages users in a separate file from the infrastructure team's client configurations.

**File: `users-config.yaml`**
```yaml
realm: "myrealm"
users:
  - username: "john.doe"
    email: "john.doe@example.com"
    firstName: "John"
    lastName: "Doe"
    enabled: true
    realmRoles:
      - "user"
    credentials:
      - type: "password"
        value: "temp123"
        temporary: true
  
  - username: "jane.smith"
    email: "jane.smith@example.com"
    firstName: "Jane"
    lastName: "Smith"
    enabled: true
    realmRoles:
      - "user"
      - "manager"
```

**Result:**
- Only users are managed
- Clients remain unchanged
- Groups remain unchanged
- Authentication flows remain unchanged

---

### Importing Only Groups

**Scenario:** Manage organizational structure separately from user accounts.

**File: `groups-config.yaml`**
```yaml
realm: "myrealm"
groups:
  - name: "Engineering"
    path: "/Engineering"
    attributes:
      department: ["engineering"]
    subGroups:
      - name: "Backend"
        path: "/Engineering/Backend"
      - name: "Frontend"
        path: "/Engineering/Frontend"
  
  - name: "Sales"
    path: "/Sales"
    attributes:
      department: ["sales"]
```

**Result:**
- Only groups are managed
- Users remain unchanged
- User group memberships added via this file are managed
- Other realm components remain unchanged

---

### Importing Only Authentication Flows

**Scenario:** Security team manages authentication flows independently.

**File: `auth-flows-config.yaml`**
```yaml
realm: "myrealm"
authenticationFlows:
  - alias: "browser-with-mfa"
    description: "Browser flow with mandatory MFA"
    providerId: "basic-flow"
    topLevel: true
    builtIn: false
    authenticationExecutions:
      - authenticator: "auth-cookie"
        requirement: "ALTERNATIVE"
        priority: 10
      
      - authenticator: "identity-provider-redirector"
        requirement: "ALTERNATIVE"
        priority: 20
      
      - authenticator: "auth-username-password-form"
        requirement: "REQUIRED"
        priority: 30
      
      - authenticator: "auth-otp-form"
        requirement: "REQUIRED"
        priority: 40
```

**Result:**
- Only authentication flows are managed
- Browser binding not changed unless explicitly set
- Clients remain unchanged
- Users remain unchanged

---

### Importing Client Scopes Only

**Scenario:** Create reusable scopes without affecting existing clients.

**File: `client-scopes-config.yaml`**
```yaml
realm: "myrealm"
clientScopes:
  - name: "custom-claims"
    description: "Custom application claims"
    protocol: "openid-connect"
    attributes:
      include.in.token.scope: "true"
      display.on.consent.screen: "true"
    protocolMappers:
      - name: "department-mapper"
        protocol: "openid-connect"
        protocolMapper: "oidc-usermodel-attribute-mapper"
        config:
          user.attribute: "department"
          claim.name: "department"
          jsonType.label: "String"
          id.token.claim: "true"
          access.token.claim: "true"
```

**Result:**
- Only client scopes are created/updated
- Existing clients are not modified
- Client scope assignments to clients remain unchanged unless explicitly set

---

## Multiple File Strategy

### Organizing by Component Type

**Structure:**
```
config/
├── realm-settings.yaml      # Basic realm settings
├── clients.yaml             # All clients
├── users.yaml               # All users
├── groups.yaml              # Group structure
├── roles.yaml               # Realm and client roles
├── auth-flows.yaml          # Authentication flows
└── client-scopes.yaml       # Reusable scopes
```

**Import all files:**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations='config/*.yaml'
```

**Import specific component:**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=config/clients.yaml
```

---

### Organizing by Environment

**Structure:**
```
config/
├── base/
│   ├── clients.yaml         # Common clients
│   └── roles.yaml           # Common roles
├── dev/
│   ├── realm-settings.yaml  # Dev-specific settings
│   └── users.yaml           # Dev test users
├── staging/
│   └── realm-settings.yaml  # Staging settings
└── production/
    └── realm-settings.yaml  # Production settings
```

**Import base + environment:**
```bash
# Import base configuration
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/base/*.yaml'

# Then import environment-specific
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/production/*.yaml'
```

---

### Organizing by Team

**Structure:**

```
config/
├── infrastructure/
│   ├── clients.yaml         # Infrastructure team manages clients
│   └── auth-flows.yaml      # Authentication configuration
├── hr/
│   └── users.yaml           # HR team manages users
└── security/
    ├── roles.yaml           # Security team manages roles
    └── groups.yaml          # Permission groups
```

**Each team imports their files:**
```bash
# Infrastructure team
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/infrastructure/*.yaml'

# HR team
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/hr/*.yaml'
```

---

## Behavior with Remote State

### With Remote State Enabled (Default)
```yaml
# File 1: clients.yaml
realm: "myrealm"
clients:
  - clientId: "app-1"
    enabled: true
```
```yaml
# File 2: users.yaml
realm: "myrealm"
users:
  - username: "user-1"
    enabled: true
```

**Behavior:**
1. Import `clients.yaml` → Creates/updates `app-1` client
2. Import `users.yaml` → Creates/updates `user-1` user
3. `app-1` client remains unchanged by user import
4. `user-1` user remains unchanged by client import
5. Both files can be maintained independently

**This is true partial import!**

---

### Without Remote State
```bash
--import.remote-state.enabled=false
```

**Behavior:**
- Each import attempts to sync the entire realm
- Resources not in the file may be removed
- Partial imports are risky without remote state
- **Not recommended for partial import strategy**

**Important:** Always use `import.remote-state.enabled=true` (default) for partial imports.

---

## Common Pitfalls

### 1. Expecting Unmanaged Resources to Update

**Problem:**
```yaml
# First import
realm: "myrealm"
clients:
  - clientId: "app-1"
    enabled: true
```

Later, manually creating `app-2` via Admin Console, then expecting:
```yaml
# Second import
realm: "myrealm"
clients:
  - clientId: "app-1"
    enabled: true
  - clientId: "app-2"  # Manually created
    enabled: false     # Trying to disable it
```

**Result:** If `app-2` was created manually (not via keycloak-config-cli), changes to it may not be applied consistently.

**Solution:** Once you start managing a resource with keycloak-config-cli, always manage it through config files, not the Admin Console.

---

### 2. Conflicting Imports from Multiple Files

**Problem:**
```yaml
# File 1: team-a.yaml
realm: "myrealm"
clients:
  - clientId: "shared-client"
    enabled: true
```
```yaml
# File 2: team-b.yaml
realm: "myrealm"
clients:
  - clientId: "shared-client"
    enabled: false  # Conflicts with File 1
```

**Result:** Last import wins. Client state depends on import order.

**Solution:** Establish ownership rules:
- One file per resource
- Use naming conventions (e.g., `team-a-*` clients)
- Document which team owns which resources

---

### 3. Incomplete Partial Configuration

**Problem:**
```yaml
# Trying to update only one field of a client
realm: "myrealm"
clients:
  - clientId: "existing-client"
    enabled: false  # Only this field
```

**Result:** Other fields of the client may be reset to defaults or remain unchanged unpredictably.

**Solution:** Include all relevant fields when updating a resource:
```yaml
realm: "myrealm"
clients:
  - clientId: "existing-client"
    enabled: false
    publicClient: true
    redirectUris:
      - "https://app.example.com/*"
    # Include all fields you want to maintain
```

---

### 4. Removing Resources Across Files

**Problem:** Resource managed by File A, trying to remove it via File B.

**File A (initial):**
```yaml
realm: "myrealm"
clients:
  - clientId: "app-1"
```

**File B (attempting removal):**
```yaml
realm: "myrealm"
clients: []  # Empty, expecting app-1 to be deleted
```

**Result:** `app-1` is not deleted because File B didn't create it.

**Solution:** Remove resources from the original file that created them:
- Delete from File A, or
- Use the same file consistently for a resource

---

### 5. Forgetting Realm Name

**Problem:**
```yaml
# Missing realm name
clients:
  - clientId: "app-1"
```

**Result:** Import fails or applies to wrong realm.

**Solution:** Always specify the realm:
```yaml
realm: "myrealm"  # Always include this
clients:
  - clientId: "app-1"
```

---

## Best Practices

1. **Always Use Remote State**: Keep `import.remote-state.enabled=true` for partial imports
2. **One Resource, One Owner**: Assign each resource to a single configuration file
3. **Organize Logically**: Group related resources (e.g., all clients together, all users together)
4. **Use Naming Conventions**: Prefix resources by team/purpose (e.g., `frontend-*`, `backend-*`)
5. **Document Ownership**: Maintain a README showing which file manages which resources
6. **Version Control Everything**: Store all configuration files in Git
7. **Small, Focused Files**: Better than one large file for maintainability
8. **Validate Before Import**: Use `--import.validate=true` to catch errors
9. **Test in Development**: Apply partial imports to dev environment first
10. **Automate in CI/CD**: Use separate jobs for different component imports

---

## Configuration Options
```bash
# Enable remote state for partial imports (default)
--import.remote-state.enabled=true

# Validate configuration before import
--import.validate=true

# Import multiple files with pattern
--import.files.locations='config/*.yaml'

# Import specific files (comma-separated)
--import.files.locations='config/clients.yaml,config/users.yaml'

# Enable parallel import for performance
--import.parallel=true
```

---

## Import Strategies

### Strategy 1: Component-Based

**Best for:** Large teams with specialized roles
```
config/
├── 01-realm-settings.yaml
├── 02-roles.yaml
├── 03-groups.yaml
├── 04-clients.yaml
├── 05-users.yaml
└── 06-auth-flows.yaml
```

**Benefits:**
- Clear separation of concerns
- Different teams own different files
- Easy to understand structure

---

### Strategy 2: Application-Based

**Best for:** Microservices architecture
```
config/
├── app-frontend/
│   ├── client.yaml
│   ├── roles.yaml
│   └── scopes.yaml
├── app-backend/
│   ├── client.yaml
│   └── service-account.yaml
└── shared/
    ├── common-roles.yaml
    └── common-groups.yaml
```

**Benefits:**
- Application-centric organization
- Easy to deploy/remove entire applications
- Clear dependencies

---

### Strategy 3: Environment-Based

**Best for:** Multiple environments (dev, staging, prod)
```
config/
├── base/              # Common to all environments
│   ├── clients.yaml
│   └── roles.yaml
├── overlays/
│   ├── dev/
│   │   └── realm-settings.yaml
│   ├── staging/
│   │   └── realm-settings.yaml
│   └── production/
│       └── realm-settings.yaml
```

**Benefits:**
- Reuse common configuration
- Environment-specific overrides
- Consistent base across environments

---

## Real-World Example

### Scenario: E-commerce Platform

**Teams:**
- Infrastructure: Manages clients and authentication
- Backend: Manages service accounts
- Frontend: Manages public clients
- HR: Manages users

**Configuration Structure:**
```
config/
├── infrastructure/
│   ├── realm-settings.yaml
│   │   realm: "ecommerce"
│   │   displayName: "E-commerce Platform"
│   │
│   └── auth-flows.yaml
│       realm: "ecommerce"
│       authenticationFlows: [...]
│
├── backend/
│   └── service-clients.yaml
│       realm: "ecommerce"
│       clients:
│         - clientId: "order-service"
│         - clientId: "payment-service"
│         - clientId: "inventory-service"
│
├── frontend/
│   └── public-clients.yaml
│       realm: "ecommerce"
│       clients:
│         - clientId: "web-shop"
│         - clientId: "mobile-app"
│
└── hr/
    └── employees.yaml
        realm: "ecommerce"
        users:
          - username: "john.doe"
          - username: "jane.smith"
```

**Import Process:**
```bash
# Infrastructure team (rarely changes)
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/infrastructure/*.yaml'

# Backend team (updates service configs)
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/backend/*.yaml'

# Frontend team (updates client apps)
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/frontend/*.yaml'

# HR team (manages employees)
java -jar keycloak-config-cli.jar \
  --import.files.locations='config/hr/*.yaml'
```

**Benefits:**
- Each team works independently
- No conflicts between teams
- Clear ownership
- Easy to track changes in Git

---

## Advanced: Selective Import with Cache Keys

Use cache keys to manage different sets of resources:
```bash
# Import and track clients separately
java -jar keycloak-config-cli.jar \
  --import.cache.key=clients \
  --import.files.locations=clients.yaml

# Import and track users separately
java -jar keycloak-config-cli.jar \
  --import.cache.key=users \
  --import.files.locations=users.yaml
```

**Benefits:**
- Independent checksum tracking
- Can reimport one without affecting the other
- Useful for scheduled imports of different components

---

## Troubleshooting

### Resources Not Being Updated

**Symptom:** Changes in configuration file don't appear in Keycloak

**Possible causes:**
1. Resource created manually (not managed by keycloak-config-cli)
2. Different cache key used
3. Remote state disabled

**Diagnosis:**
```bash
# Check remote state
--import.remote-state.enabled=true

# Force reimport
--import.cache.enabled=false
```

---

### Resources Being Deleted Unexpectedly

**Symptom:** Resources disappear after import

**Possible causes:**
1. Remote state disabled
2. Resource removed from config file that created it
3. Full realm export used instead of partial import

**Solution:**
- Always use `import.remote-state.enabled=true`
- Only manage resources you intend to control
- Keep resource definitions in their original files

---

### Import Order Matters

**Symptom:** Import fails due to missing dependencies

**Cause:** Trying to assign roles/groups that don't exist yet

**Solution:** Import in dependency order:
```bash
# 1. Roles first
--import.files.locations=roles.yaml

# 2. Groups next
--import.files.locations=groups.yaml

# 3. Users last (can reference roles and groups)
--import.files.locations=users.yaml
```

---

## Consequences

When using partial imports:

1. **Independent Updates**: Different teams can update their components without conflicts
2. **Reduced Risk**: Only specified resources are affected by each import
3. **Modular Configuration**: Easier to maintain small, focused files
4. **Requires Discipline**: Teams must follow ownership conventions
5. **Remote State Essential**: Partial imports require `import.remote-state.enabled=true`
6. **Git-Friendly**: Smaller files mean clearer Git diffs and easier reviews

---

## Migration Guide

### From Full Realm Exports to Partial Imports

**Step 1: Export current realm**
```bash
# Export full realm from Keycloak Admin Console
# Realm Settings → Export → Export full realm
```

**Step 2: Split into components**
```bash
# Create separate files
config/
├── clients.yaml       # Extract clients section
├── users.yaml         # Extract users section
├── roles.yaml         # Extract roles section
└── groups.yaml        # Extract groups section
```

**Step 3: Clean up each file**
- Remove UUIDs
- Remove default values
- Keep only managed resources

**Step 4: Import incrementally**
```bash
# Import one component at a time
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/roles.yaml

# Verify, then import next
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/clients.yaml
```

**Step 5: Establish ownership**
- Document which team owns which file
- Set up CI/CD pipelines per component
- Train teams on their responsibilities

---

## Related Issues

- [#1161 - Support Partial Imports into Keycloak](https://github.com/adorsys/keycloak-config-cli/issues/1161)
- [#1237 - Working with Arrays](arrays.md)

---

## Additional Resources

- [Managed Resources Documentation](../managed-resource.md)
- [Import Settings Documentation](../import-settings.md)
- [Remote State Management](remote-state-management.md)