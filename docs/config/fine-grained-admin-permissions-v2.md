# Fine-Grained Admin Permissions (FGAP) V2

Keycloak 26.2.x introduced Fine-Grained Admin Permissions V2 (FGAP V2), a significant update to the permission model for managing administrative access to Keycloak resources. Understanding the differences between V1 and V2, and how to configure permissions correctly in keycloak-config-cli, is essential for managing realm security in Keycloak 26.2+.

Related issues: [#1301](https://github.com/adorsys/keycloak-config-cli/issues/1301)

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

You can still configure full authorization for your **own clients** (not `admin-permissions`). Custom clients have full authorization API access and standard FGAP configuration applies.

---

### What You CANNOT Configure

**This DOES NOT work:**
```yaml
realm: "my-realm"
adminPermissionsEnabled: true

clients:
  - clientId: "admin-permissions" 
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

### Step 2: Update Realm Configuration
```yaml
realm: "my-realm"
adminPermissionsEnabled: true
```

---

### Step 3: Import Updated Configuration
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=realm-v2-config.yaml
```

---

### Step 4: Configure Permissions Manually

Configure permissions through Admin Console for each resource type (Clients, Groups, Users, Roles).

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
-  Custom client authorization: **Fully supported**
-  `admin-permissions` client authorization: **Blocked**

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

- Enable FGAP V2 at realm level with `adminPermissionsEnabled: true`
- Remove V1 configuration from `realm-management` client
- Define roles before configuring permissions
- Document permissions externally since they can't be in config files
- Test permissions thoroughly in dev environment
- Use Terraform for infrastructure as code if needed
- Regularly audit permissions for least privilege

---

## Troubleshooting

**Permissions not working after migration:** Manually configure V2 permissions in Admin Console.

**Cannot enable permissions for resource:** Ensure `adminPermissionsEnabled: true` is set at realm level.

**Authorization settings import fails:** Remove `admin-permissions` authorization settings from config file (system-managed).

**V1 configuration warnings:** Remove `realm-management` authorization settings.

---

## Version Compatibility

| Keycloak Version | FGAP Version | Client | Config File Support |
|------------------|--------------|--------|---------------------|
| < 26.2 | V1 | `realm-management` |  Full |
| 26.2+ | V2 | `admin-permissions` |  Realm-level only |

**Recommendation:** For Keycloak 26.2+, use `adminPermissionsEnabled: true` and manage detailed permissions through Admin Console.

---
