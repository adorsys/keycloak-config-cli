# Fine-Grained Admin Permissions (FGAP) V2

Keycloak 26.2.x introduced Fine-Grained Admin Permissions V2 (FGAP V2), a significant update to the permission model for managing administrative access to Keycloak resources. Understanding the differences between V1 and V2, and how to configure permissions correctly in keycloak-config-cli, is essential for managing realm security in Keycloak 26.2+.

Related issues: [#1301](https://github.com/adorsys/keycloak-config-cli/issues/1301)

## The Problem

Users encounter challenges with fine-grained permissions in Keycloak 26.2+ because:
- FGAP V2 introduces breaking changes from V1
- The permission model changed from `realm-management` client to `admin-permissions` client
- Resource and policy configuration syntax is different between versions
- Existing V1 configurations don't work with V2
- The `admin-permissions` client is system-managed and cannot be configured via imports
- Authorization settings for `admin-permissions` are blocked by Keycloak API
- Placeholder syntax for referencing resources changed
- It's unclear how to migrate from V1 to V2

## What Changed in Keycloak 26.2

### FGAP V1 (Keycloak < 26.2)

**Configuration Location:** `realm-management` client

**Resource Naming:** Used UUIDs in resource/policy names
```yaml
clients:
  - clientId: "realm-management"
    authorizationSettings:
      resources:
        - name: "client.resource.$my-client-id"
          type: "Client"
      policies:
        - name: "manage.permission.client.$my-client-id"
          type: "scope"
```

**Characteristics:**
- Authorization configured on `realm-management` client
- UUID-based resource naming with `$placeholder` syntax
- Full API access to authorization settings
- Can be managed via keycloak-config-cli

---

### FGAP V2 (Keycloak 26.2+)

**Configuration Location:** `admin-permissions` client (system-managed)

**Resource Naming:** Uses `authorizationSchema` with typed resources
```yaml
realm: "my-realm"
adminPermissionsEnabled: true  # Creates admin-permissions client

# admin-permissions client is system-managed
# Cannot configure authorization settings via import
```

**Characteristics:**
- Authorization configured on `admin-permissions` client
- Schema-based resource type definitions
- Cleaner permission model
- **API access blocked** - cannot import authorization settings
- Must manage permissions through Admin Console or dedicated FGAP V2 APIs

---

## Key Differences: V1 vs V2

| Aspect | FGAP V1 | FGAP V2 |
|--------|---------|---------|
| **Client** | `realm-management` | `admin-permissions` |
| **Keycloak Version** | < 26.2 | 26.2+ |
| **Configuration** | Via keycloak-config-cli | System-managed, blocked from imports |
| **Resource Types** | Dynamic, UUID-based | Schema-defined (Clients, Groups, Users, Roles) |
| **Policy Types** | Generic scope policies | Type-specific permissions |
| **Resource Naming** | `client.resource.$id` | Direct reference or schema-based |
| **Import Support** | Full | Limited (realm-level only) |
| **Management** | Config files | Admin Console or FGAP V2 API |

---

## Configuration in Keycloak 26.2+

### Enable FGAP V2

**Scenario:** Enable fine-grained permissions for a realm.
```yaml
realm: "my-realm"
adminPermissionsEnabled: true  # Enables FGAP V2
enabled: true

# Do NOT include admin-permissions client configuration
# It is system-managed by Keycloak
```

**Result:**
- `admin-permissions` client is automatically created
- FGAP V2 is enabled for the realm
- Permissions must be configured through Admin Console

**Important:** Unlike V1, you **cannot** configure authorization settings for `admin-permissions` client via config files.

---

### What You CAN Configure (Custom Clients)

You can still configure full authorization for your **own clients** (not `admin-permissions`):
```yaml
realm: "my-realm"
adminPermissionsEnabled: true

clients:
  - clientId: "my-custom-app"
    enabled: true
    authorizationServicesEnabled: true
    authorizationSettings:
      allowRemoteResourceManagement: true
      policyEnforcementMode: "ENFORCING"
      
      resources:
        - name: "premium-resource"
          type: "urn:my-custom-app:resources:premium"
          ownerManagedAccess: false
          scopes:
            - name: "view"
            - name: "edit"
            - name: "delete"
      
      policies:
        - name: "admin-only-policy"
          type: "role"
          logic: "POSITIVE"
          decisionStrategy: "UNANIMOUS"
          config:
            roles: '[{"id":"admin","required":true}]'
        
        - name: "premium-resource-permission"
          type: "resource"
          logic: "POSITIVE"
          decisionStrategy: "UNANIMOUS"
          config:
            defaultResourceType: "urn:my-custom-app:resources:premium"
            resources: '["premium-resource"]'
            applyPolicies: '["admin-only-policy"]'

roles:
  realm:
    - name: "admin"
      description: "Administrator role"
```

**This works because:**
- It's your custom client, not the system `admin-permissions` client
- Full authorization API access is available for custom clients
- Standard FGAP configuration applies

---

### What You CANNOT Configure

**This DOES NOT work:**
```yaml
realm: "my-realm"
adminPermissionsEnabled: true

clients:
  - clientId: "admin-permissions"  # ❌ System-managed client
    authorizationSettings:
      resources:
        - name: "client.resource.$my-client"
          type: "Client"
      policies:
        - name: "manage.permission.client.$my-client"
          type: "scope"
          config:
            resources: '["client.resource.$my-client"]'
            scopes: '["manage"]'
```

**Why it fails:**
- `admin-permissions` client is system-managed
- Keycloak blocks standard Authorization Services API calls for this client
- Returns `400 Bad Request` with `unknown_error`
- By design to prevent external modification of the system permission model

**Error you'll see:**
```
Skipping authorization settings for 'admin-permissions' client in realm 'my-realm' - 
FGAP V2 manages this client internally and blocks API access.
```

---

## Managing FGAP V2 Permissions

Since you cannot configure `admin-permissions` via imports, here's how to manage permissions:

### Method 1: Keycloak Admin Console

**Steps:**

1. **Navigate to Realm Settings** → **Permissions**
2. Enable permissions for resource types:
   - Users
   - Groups  
   - Clients
   - Roles

3. **Configure permissions per resource:**
   - Go to the specific resource (e.g., **Clients** → select client)
   - Click **Permissions** tab
   - Enable **Permissions Enabled**
   - Configure who can manage, view, map-roles, etc.

**Example: Grant Client Management to a Role**

1. Go to **Clients** → select `my-app` client
2. Click **Permissions** tab
3. Enable **Permissions Enabled**
4. Create policy:
   - Name: `client-admin-policy`
   - Type: `Role`
   - Roles: `client-admin`
5. Create permission:
   - Resource: `my-app`
   - Scopes: `manage`, `view`
   - Policies: `client-admin-policy`

---

### Method 2: FGAP V2 REST API (Advanced)

Keycloak provides dedicated FGAP V2 REST endpoints (separate from standard Authorization Services API).

**Enable Permissions for a Client:**
```bash
# Enable permissions for a specific client
curl -X PUT "http://localhost:8080/admin/realms/my-realm/clients/{client-uuid}/management/permissions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true
  }'
```

**Note:** These are specialized endpoints for FGAP V2, not the standard `/authz/` endpoints.

---

### Method 3: Terraform or Infrastructure as Code

Use Terraform Keycloak provider with FGAP V2 resources:
```hcl
resource "keycloak_realm" "my_realm" {
  realm = "my-realm"
  admin_permissions_enabled = true
}

resource "keycloak_client_permissions" "my_app_permissions" {
  realm_id = keycloak_realm.my_realm.id
  client_id = keycloak_openid_client.my_app.id
  
  enabled = true
}
```

---

## V2 Resource Types and Scopes

FGAP V2 defines four primary resource types, each with specific scopes:

### 1. Clients

**Available Scopes:**
- `view` - View client configuration
- `manage` - Full client management
- `map-roles` - Assign roles to client
- `map-roles-client-scope` - Map client scopes
- `map-roles-composite` - Manage composite roles

**Example Permission:**
"Allow `client-admin` role to manage `my-app` client"

---

### 2. Groups

**Available Scopes:**
- `view` - View group
- `view-members` - View group members
- `manage` - Manage group settings
- `manage-members` - Add/remove members
- `manage-membership` - Control membership
- `impersonate-members` - Impersonate group members

**Example Permission:**
"Allow `hr-admin` role to manage members of `/Employees` group"

---

### 3. Users

**Available Scopes:**
- `view` - View user profile
- `manage` - Full user management
- `map-roles` - Assign roles to user
- `manage-group-membership` - Manage group membership
- `impersonate` - Impersonate user

**Example Permission:**
"Allow `support-team` role to view and impersonate users"

---

### 4. Roles

**Available Scopes:**
- `map-role` - Assign role to users
- `map-role-client-scope` - Map to client scopes
- `map-role-composite` - Use in composite roles

**Example Permission:**
"Allow `role-admin` role to assign `premium-user` role"

---

## Migration from V1 to V2

### Step 1: Identify Current V1 Configuration

**Locate V1 configuration in your files:**
```yaml
# V1 Configuration (Keycloak < 26.2)
clients:
  - clientId: "realm-management"
    authorizationSettings:
      resources:
        - name: "client.resource.$my-app"
          type: "Client"
        - name: "idp.resource.$google-idp"
          type: "IdentityProvider"
      policies:
        - name: "manage.permission.client.$my-app"
          type: "scope"
          config:
            resources: '["client.resource.$my-app"]'
            scopes: '["manage"]'
```

---

### Step 2: Remove V1 Configuration from Config Files

**Create migration plan:**
```yaml
# migration-notes.md
# V1 Permissions to Migrate:
# 1. Client: my-app - managed by client-admin role
# 2. IDP: google-idp - managed by security-team role
# 3. Group: /Developers - managed by team-lead role
```

**Remove from config files:**
```yaml
# Remove realm-management authorization settings
# Keep this commented for reference during migration

# clients:
#   - clientId: "realm-management"
#     authorizationSettings: ...
```

---

### Step 3: Update Realm Configuration
```yaml
realm: "my-realm"
adminPermissionsEnabled: true  # Enable FGAP V2

# Define roles that will be used for permissions
roles:
  realm:
    - name: "client-admin"
      description: "Can manage specific clients"
    - name: "security-team"
      description: "Can manage identity providers"
    - name: "team-lead"
      description: "Can manage developer group"
```

---

### Step 4: Import Updated Configuration
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=realm-v2-config.yaml
```

**Result:**
- FGAP V2 is enabled
- `admin-permissions` client created
- V1 authorization settings removed
- Ready for manual permission configuration

---

### Step 5: Configure Permissions Manually

**For each resource identified in Step 2:**

1. **Client: my-app**
   - Admin Console → **Clients** → `my-app` → **Permissions**
   - Enable permissions
   - Create role policy for `client-admin`
   - Create manage permission with `manage` scope

2. **IDP: google-idp**
   - Admin Console → **Identity Providers** → `google-idp` → **Permissions**
   - Enable permissions
   - Create role policy for `security-team`
   - Create manage permission

3. **Group: /Developers**
   - Admin Console → **Groups** → `/Developers` → **Permissions**
   - Enable permissions
   - Create role policy for `team-lead`
   - Create manage-members permission

---

### Step 6: Test Permissions

**Verify each role can access appropriate resources:**
```bash
# Test as client-admin
# Should be able to manage my-app client

# Test as security-team
# Should be able to manage google-idp

# Test as team-lead
# Should be able to manage /Developers group
```

---

## Common Pitfalls

### 1. Trying to Import admin-permissions Authorization

**Problem:**
```yaml
clients:
  - clientId: "admin-permissions"
    authorizationSettings:
      resources: [...]  # This won't work
```

**Error:**
```
400 Bad Request: unknown_error
```

**Solution:** Remove authorization settings for `admin-permissions` from config files. Use Admin Console instead.

---

### 2. Expecting V1 Syntax to Work

**Problem:**
```yaml
# V1 syntax in Keycloak 26.2+
clients:
  - clientId: "realm-management"
    authorizationSettings:
      resources:
        - name: "client.resource.$my-client"
```

**Result:** Configuration is ignored or causes warnings.

**Solution:** Remove V1 configuration and migrate to V2 approach.

---

### 3. Not Enabling adminPermissionsEnabled

**Problem:**
```yaml
realm: "my-realm"
# Missing: adminPermissionsEnabled: true
```

**Result:** FGAP V2 not enabled, permissions cannot be configured.

**Solution:**
```yaml
realm: "my-realm"
adminPermissionsEnabled: true
```

---

### 4. Confusion Between Custom Client and admin-permissions

**Problem:** Thinking all authorization configuration is blocked.

**Clarification:**
- ✅ Custom client authorization: **Fully supported**
- ❌ `admin-permissions` client authorization: **Blocked**

**You can still do this:**
```yaml
clients:
  - clientId: "my-custom-app"  # Your client
    authorizationSettings:
      resources: [...]  # This works!
```

---

### 5. Missing Role Definitions

**Problem:** Creating permissions for roles that don't exist.

**Solution:** Define roles before configuring permissions:
```yaml
roles:
  realm:
    - name: "client-admin"
      description: "Client administrator"
```

---

## Best Practices

1. **Enable FGAP V2 at Realm Level**
```yaml
   realm: "my-realm"
   adminPermissionsEnabled: true
```

2. **Remove V1 Configuration**
   - Clean up old `realm-management` authorization settings
   - Document migration in comments

3. **Define Roles First**
   - Create all necessary roles before configuring permissions
   - Use descriptive names and descriptions

4. **Document Permissions Externally**
   - Since permissions can't be in config files, maintain documentation
   - Use a `PERMISSIONS.md` file:
```markdown
   # FGAP V2 Permissions
   
   ## Client: my-app
   - Role: client-admin
   - Scopes: manage, view
   
   ## Group: /Developers
   - Role: team-lead
   - Scopes: manage-members
```

5. **Test Permissions Thoroughly**
   - Verify each role has appropriate access
   - Test in dev environment first
   - Document test cases

6. **Use Terraform for Infrastructure as Code**
   - Consider Terraform for permission management
   - Provides version control for permissions
   - Enables reproducible deployments

7. **Regular Audits**
   - Review permissions periodically
   - Remove unused permissions
   - Verify principle of least privilege

8. **Separate Custom Client Authorization**
   - Use custom clients for application-specific authorization
   - Don't mix with FGAP V2 admin permissions
   - Keep concerns separated

---

## Troubleshooting

### Permissions Not Working After Migration

**Symptom:** Users who had permissions in V1 no longer have access in V2

**Cause:** V2 permissions not configured after migration

**Solution:**
1. Verify `adminPermissionsEnabled: true` in realm
2. Check `admin-permissions` client exists
3. Manually configure permissions in Admin Console
4. Verify role assignments

---

### Cannot Enable Permissions for Resource

**Symptom:** "Permissions Enabled" toggle is disabled

**Cause:** FGAP V2 not enabled at realm level

**Solution:**
```yaml
realm: "my-realm"
adminPermissionsEnabled: true
```

Re-import configuration, then try enabling permissions again.

---

### Authorization Settings Import Fails

**Symptom:** Import fails when trying to configure `admin-permissions`

**Error:**
```
Policy with name [xyz] already exists
```

**Cause:** Attempting to configure system-managed client

**Solution:** Remove `admin-permissions` authorization settings from config file:
```yaml
# Remove this:
# clients:
#   - clientId: "admin-permissions"
#     authorizationSettings: ...
```

---

### V1 Configuration Still Present

**Symptom:** Warnings about deprecated configuration

**Solution:** Clean up V1 configuration:
```yaml
# Remove V1 authorization from realm-management
clients:
  - clientId: "realm-management"
    # Remove authorizationSettings block
```

---

## Consequences

When using FGAP V2 in Keycloak 26.2+:

1. **No Config File Management:** Permissions must be managed via Admin Console or API
2. **Migration Required:** V1 configurations must be migrated manually
3. **Documentation Important:** External documentation critical since permissions aren't in config files
4. **Cleaner Model:** V2 provides more intuitive permission structure
5. **Custom Clients Unaffected:** Your own client authorization continues to work normally
6. **API Changes:** Must use dedicated FGAP V2 endpoints, not standard Authorization Services API

---

## Version Compatibility

| Keycloak Version | FGAP Version | Client | Config File Support |
|------------------|--------------|--------|---------------------|
| < 26.2 | V1 | `realm-management` | ✅ Full |
| 26.2+ | V2 | `admin-permissions` | ❌ Realm-level only |

**Recommendation:** For Keycloak 26.2+, use `adminPermissionsEnabled: true` and manage detailed permissions through Admin Console.

---

## Example: Complete V2 Setup
```yaml
realm: "production"
enabled: true
adminPermissionsEnabled: true

# Define roles for permission management
roles:
  realm:
    - name: "client-administrator"
      description: "Can manage production clients"
    
    - name: "user-manager"
      description: "Can manage production users"
    
    - name: "group-manager"
      description: "Can manage organizational groups"

# Define your custom clients (not admin-permissions)
clients:
  - clientId: "production-app"
    enabled: true
    authorizationServicesEnabled: true
    authorizationSettings:
      allowRemoteResourceManagement: true
      policyEnforcementMode: "ENFORCING"
      
      resources:
        - name: "admin-panel"
          type: "urn:production-app:resources:admin"
          scopes:
            - name: "view"
            - name: "edit"
      
      policies:
        - name: "admin-only"
          type: "role"
          logic: "POSITIVE"
          config:
            roles: '[{"id":"admin","required":true}]'
        
        - name: "admin-panel-permission"
          type: "resource"
          logic: "POSITIVE"
          config:
            resources: '["admin-panel"]'
            applyPolicies: '["admin-only"]'

# Users with admin roles
users:
  - username: "client-admin"
    enabled: true
    realmRoles:
      - "client-administrator"
  
  - username: "user-admin"
    enabled: true
    realmRoles:
      - "user-manager"
```

**After importing:**

1. **Configure FGAP V2 permissions manually** in Admin Console:
   - Clients → Enable permissions → Assign to `client-administrator`
   - Users → Enable permissions → Assign to `user-manager`
   - Groups → Enable permissions → Assign to `group-manager`

2. **Document permissions** in `PERMISSIONS.md`:
```markdown
# FGAP V2 Permissions Configuration

## Clients
- Role: client-administrator
- Scope: manage, view
- Resources: All production clients

## Users
- Role: user-manager
- Scope: manage, view, map-roles
- Resources: All production users

## Groups  
- Role: group-manager
- Scope: manage-members, view
- Resources: All production groups
```

---

