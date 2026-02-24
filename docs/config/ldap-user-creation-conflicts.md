# LDAP User Creation Conflicts

When a Keycloak realm is configured with LDAP user federation, attempting to create local users directly through keycloak-config-cli or the Admin API often results in 400 Bad Request errors. Understanding the relationship between LDAP federation and local user management is essential for avoiding conflicts and properly managing users in LDAP-enabled realms.

Related issues: [#1291](https://github.com/adorsys/keycloak-config-cli/issues/1291)

## The Problem

Users encounter 400 errors when creating users in LDAP-enabled realms because:
- LDAP user federation makes the realm read-only for user management
- Local user creation conflicts with LDAP as the source of truth
- Keycloak attempts to create users in LDAP, which may fail due to permissions or configuration
- It's unclear whether users should be created in LDAP or Keycloak
- Error messages don't clearly indicate LDAP is the cause
- Existing configuration files assume local user management
- Federated users have different attribute handling than local users

## Understanding LDAP User Federation

### How LDAP Federation Works

When LDAP user federation is enabled:

1. **LDAP as Source of Truth**
   - User data is stored in LDAP directory
   - Keycloak synchronizes users from LDAP
   - Authentication happens against LDAP

2. **Read-Only User Management**
   - Users cannot be created directly in Keycloak
   - User modifications sync back to LDAP (if configured)
   - Some attributes become read-only

3. **Synchronization**
   - Full sync: Imports all LDAP users
   - Changed sync: Imports only modified users
   - Periodic or on-demand sync

---

## The Error

### Typical Error Message
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=realm-with-users.yaml
```

**Error:**
```
400 Bad Request: User creation failed
Could not create user: LDAP write operations not permitted
```

Or:
```
400 Bad Request: Insufficient permissions to create user in LDAP
```

Or:
```
500 Internal Server Error: LDAP connection failed during user creation
```

### Why It Happens

**Configuration with LDAP federation:**
```yaml
realm: "corporate"
components:
  - name: "ldap"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      vendor: ["Active Directory"]
      connectionUrl: ["ldap://ldap.corporate.com:389"]
      usersDn: ["CN=Users,DC=corporate,DC=com"]
      # ... LDAP configuration

users:
  - username: "john.doe"  # ❌ This will fail!
    email: "john.doe@corporate.com"
    enabled: true
```

**Result:** Keycloak attempts to create the user, but:
- LDAP may not allow user creation via Keycloak
- LDAP write permissions may not be configured
- User creation requires specific LDAP attributes not provided
- LDAP connection may be read-only

---

## Solutions

### Solution 1: Create Users in LDAP Directly (Recommended)

**Best Practice:** Manage users in LDAP, not in Keycloak.

**Configuration (No Local Users):**
```yaml
realm: "corporate"
enabled: true

# LDAP User Federation
components:
  - name: "corporate-ldap"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      enabled: ["true"]
      priority: ["0"]
      
      # Connection Settings
      vendor: ["Active Directory"]
      connectionUrl: ["ldap://ldap.corporate.com:389"]
      bindDn: ["CN=keycloak-service,CN=Users,DC=corporate,DC=com"]
      bindCredential: ["password123"]
      
      # User Settings
      usersDn: ["CN=Users,DC=corporate,DC=com"]
      userObjectClasses: ["person", "organizationalPerson", "user"]
      usernameLDAPAttribute: ["sAMAccountName"]
      rdnLDAPAttribute: ["cn"]
      uuidLDAPAttribute: ["objectGUID"]
      
      # Sync Settings
      fullSyncPeriod: ["86400"]  # Daily full sync
      changedSyncPeriod: ["3600"]  # Hourly changed sync
      
      # Authentication
      authType: ["simple"]
      editMode: ["READ_ONLY"]  # Important!

# Do NOT include users section
# Users come from LDAP
```

**Result:**
- Users are synchronized from LDAP automatically
- No 400 errors on import
- LDAP remains source of truth
- User management happens in LDAP tools

**Create users in LDAP using:**
- Active Directory Users and Computers
- `ldapadd` command
- LDAP management tools
- Corporate user provisioning system

---

### Solution 2: Separate Local and Federated Users

**Scenario:** Need both LDAP users and local service accounts.

**Strategy:** Use LDAP for regular users, local users for service accounts only.
```yaml
realm: "corporate"
enabled: true

# LDAP for regular users
components:
  - name: "corporate-ldap"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      enabled: ["true"]
      priority: ["1"]  # Lower priority
      
      vendor: ["Active Directory"]
      connectionUrl: ["ldap://ldap.corporate.com:389"]
      bindDn: ["CN=keycloak-service,CN=Users,DC=corporate,DC=com"]
      bindCredential: ["password123"]
      
      usersDn: ["CN=Users,DC=corporate,DC=com"]
      usernameLDAPAttribute: ["sAMAccountName"]
      
      editMode: ["READ_ONLY"]

# Local users for service accounts only
users:
  - username: "keycloak-admin"
    email: "keycloak-admin@corporate.com"
    enabled: true
    credentials:
      - type: "password"
        value: "SecureAdminPassword"
        temporary: false
    realmRoles:
      - "admin"
  
  - username: "api-service-account"
    email: "api@corporate.com"
    enabled: true
    serviceAccountClientId: "backend-service"
```

**Result:**
- LDAP users for human users (priority 1)
- Local users for service accounts (priority 0 - default)
- No conflicts between federation and local users
- Local users not synced to LDAP

**Important:** Local service accounts won't authenticate against LDAP.

---

### Solution 3: Configure LDAP for Write Operations

**Scenario:** Need Keycloak to create users in LDAP.

**Requirements:**
- LDAP service account with write permissions
- Proper DN structure configured
- All required LDAP attributes mapped
```yaml
realm: "corporate"

components:
  - name: "corporate-ldap"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      enabled: ["true"]
      priority: ["0"]
      
      # Connection with write permissions
      vendor: ["Active Directory"]
      connectionUrl: ["ldap://ldap.corporate.com:389"]
      bindDn: ["CN=keycloak-admin,CN=Users,DC=corporate,DC=com"]
      bindCredential: ["AdminPassword123"]
      
      # User Settings
      usersDn: ["CN=Users,DC=corporate,DC=com"]
      userObjectClasses: ["person", "organizationalPerson", "user"]
      usernameLDAPAttribute: ["sAMAccountName"]
      rdnLDAPAttribute: ["cn"]
      uuidLDAPAttribute: ["objectGUID"]
      
      # Enable Write Mode
      editMode: ["WRITABLE"]  # Allows Keycloak to write to LDAP
      
      # Required Attributes for User Creation
      firstNameLDAPAttribute: ["givenName"]
      lastNameLDAPAttribute: ["sn"]
      emailLDAPAttribute: ["mail"]

# Now users can be created
users:
  - username: "john.doe"
    email: "john.doe@corporate.com"
    firstName: "John"
    lastName: "Doe"
    enabled: true
    credentials:
      - type: "password"
        value: "TempPassword123"
        temporary: true
```

**Important Considerations:**
- LDAP service account needs appropriate permissions
- All required LDAP attributes must be provided
- Password policies must match LDAP requirements
- User creation may still fail if LDAP has additional constraints

---

### Solution 4: Skip Federated User Attributes

**Scenario:** Updating federated users but hitting read-only attribute errors.

**Use the skip-attributes flag:**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.behaviors.skip-attributes-for-federated-user=true \
  --import.files.locations=realm-config.yaml
```

**Configuration:**
```yaml
realm: "corporate"

components:
  - name: "ldap"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      editMode: ["READ_ONLY"]
      # ... other LDAP settings

users:
  - username: "existing.ldap.user"
    # Attributes will be set to null to avoid read-only conflicts
    realmRoles:
      - "user"
```

**Result:**
- Attributes set to null for federated users
- Avoids read-only conflicts
- Role assignments still work
- Useful for managing roles/groups without touching LDAP attributes

---

## LDAP Edit Modes

Understanding edit modes is crucial:

| Edit Mode | User Creation | User Updates | Use Case |
|-----------|---------------|--------------|----------|
| `READ_ONLY` | ❌ Not allowed | ❌ Not allowed | LDAP is authoritative, no Keycloak modifications |
| `WRITABLE` | ✅ Via LDAP | ✅ Sync to LDAP | Keycloak can modify LDAP |
| `UNSYNCED` | ✅ Local only | ✅ Local only | Users fetched from LDAP but changes stay local |

### READ_ONLY (Recommended for Most Cases)
```yaml
config:
  editMode: ["READ_ONLY"]
```

**Behavior:**
- Users imported from LDAP
- No user creation in Keycloak
- No user modifications synced to LDAP
- LDAP remains single source of truth

**Use when:**
- LDAP is managed by another team
- Corporate directory is authoritative
- No LDAP write permissions available

---

### WRITABLE
```yaml
config:
  editMode: ["WRITABLE"]
```

**Behavior:**
- Users can be created in Keycloak
- User changes sync back to LDAP
- Requires LDAP write permissions

**Use when:**
- Keycloak manages user lifecycle
- LDAP write permissions available
- Need user self-service features

**Requirements:**
- Service account with LDAP write permissions
- All required LDAP attributes configured
- Matching password policies

---

### UNSYNCED
```yaml
config:
  editMode: ["UNSYNCED"]
```

**Behavior:**
- Users imported from LDAP once
- Local modifications don't sync to LDAP
- Changes only in Keycloak database

**Use when:**
- Need local user modifications
- Cannot write to LDAP
- Hybrid approach required

**Warning:** Creates divergence between LDAP and Keycloak data.

---

## Complete LDAP Configuration Examples

### Example 1: Active Directory (Read-Only)
```yaml
realm: "corporate"
enabled: true

components:
  - name: "corporate-ad"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      enabled: ["true"]
      priority: ["0"]
      
      # Connection
      vendor: ["Active Directory"]
      connectionUrl: ["ldaps://ad.corporate.com:636"]
      startTls: ["false"]
      
      # Authentication
      authType: ["simple"]
      bindDn: ["CN=keycloak-svc,CN=Service Accounts,DC=corporate,DC=com"]
      bindCredential: ["ServiceAccountPassword"]
      
      # User Search
      usersDn: ["CN=Users,DC=corporate,DC=com"]
      userObjectClasses: ["person", "organizationalPerson", "user"]
      usernameLDAPAttribute: ["sAMAccountName"]
      rdnLDAPAttribute: ["cn"]
      uuidLDAPAttribute: ["objectGUID"]
      
      # Attributes
      firstNameLDAPAttribute: ["givenName"]
      lastNameLDAPAttribute: ["sn"]
      emailLDAPAttribute: ["mail"]
      
      # Mode
      editMode: ["READ_ONLY"]
      
      # Sync
      fullSyncPeriod: ["86400"]
      changedSyncPeriod: ["3600"]
      
      # Search
      searchScope: ["2"]  # Subtree
      useTruststoreSpi: ["ldapsOnly"]

# No users section - they come from AD
```

---

### Example 2: OpenLDAP (Writable)
```yaml
realm: "development"
enabled: true

components:
  - name: "dev-ldap"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      enabled: ["true"]
      priority: ["0"]
      
      # Connection
      vendor: ["Other"]
      connectionUrl: ["ldap://ldap.dev.local:389"]
      
      # Authentication
      authType: ["simple"]
      bindDn: ["cn=admin,dc=dev,dc=local"]
      bindCredential: ["admin123"]
      
      # User Search
      usersDn: ["ou=users,dc=dev,dc=local"]
      userObjectClasses: ["inetOrgPerson"]
      usernameLDAPAttribute: ["uid"]
      rdnLDAPAttribute: ["uid"]
      uuidLDAPAttribute: ["entryUUID"]
      
      # Attributes
      firstNameLDAPAttribute: ["givenName"]
      lastNameLDAPAttribute: ["sn"]
      emailLDAPAttribute: ["mail"]
      
      # Mode - Allow Keycloak to create users
      editMode: ["WRITABLE"]
      
      # Sync
      fullSyncPeriod: ["-1"]  # Disabled
      changedSyncPeriod: ["-1"]  # Disabled

# Users can be created via Keycloak
users:
  - username: "developer1"
    email: "dev1@dev.local"
    firstName: "Developer"
    lastName: "One"
    enabled: true
    credentials:
      - type: "password"
        value: "DevPassword123"
        temporary: false
```

---

### Example 3: Hybrid (LDAP + Local Service Accounts)
```yaml
realm: "production"
enabled: true

# LDAP for employees
components:
  - name: "employee-ldap"
    providerId: "ldap"
    providerType: "org.keycloak.storage.UserStorageProvider"
    config:
      enabled: ["true"]
      priority: ["1"]  # Lower priority than local
      
      vendor: ["Active Directory"]
      connectionUrl: ["ldaps://ad.company.com:636"]
      bindDn: ["CN=keycloak,CN=Service Accounts,DC=company,DC=com"]
      bindCredential: ["$(env:LDAP_PASSWORD)"]
      
      usersDn: ["OU=Employees,DC=company,DC=com"]
      usernameLDAPAttribute: ["sAMAccountName"]
      
      editMode: ["READ_ONLY"]
      
      # Filter only active employees
      customUserSearchFilter: ["(&(objectClass=user)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))"]

# Local service accounts (priority 0 by default)
users:
  - username: "monitoring-service"
    email: "monitoring@company.com"
    enabled: true
    serviceAccountClientId: "monitoring-client"
  
  - username: "backup-service"
    email: "backup@company.com"
    enabled: true
    serviceAccountClientId: "backup-client"
  
  - username: "break-glass-admin"
    email: "admin@company.com"
    enabled: true
    credentials:
      - type: "password"
        value: "$(env:ADMIN_PASSWORD)"
        temporary: false
    realmRoles:
      - "admin"
```

---

## Common Pitfalls

### 1. Creating Local Users in LDAP-Enabled Realm

**Problem:**
```yaml
components:
  - name: "ldap"
    config:
      editMode: ["READ_ONLY"]

users:
  - username: "john.doe"  # ❌ Will fail!
```

**Error:** `400 Bad Request: User creation not permitted`

**Solution:** Remove users from config, create in LDAP instead.

---

### 2. Missing Required LDAP Attributes

**Problem:**
```yaml
users:
  - username: "john.doe"
    # Missing firstName, lastName, email
    enabled: true
```

**Error:** `400 Bad Request: Required LDAP attributes missing`

**Solution:** Provide all required attributes:
```yaml
users:
  - username: "john.doe"
    firstName: "John"
    lastName: "Doe"
    email: "john.doe@company.com"
    enabled: true
```

---

### 3. Insufficient LDAP Permissions

**Problem:** Service account lacks write permissions

**Error:** `500 Internal Server Error: Insufficient permissions to create user in LDAP`

**Solution:** 
- Grant write permissions to LDAP service account
- Or use `READ_ONLY` mode and create users in LDAP directly

---

### 4. Trying to Modify Federated User Attributes

**Problem:**
```yaml
users:
  - username: "existing.ldap.user"
    email: "newemail@company.com"  # ❌ Read-only!
```

**Error:** `400 Bad Request: Cannot modify read-only attribute`

**Solution:** Use skip-attributes flag:
```bash
--import.behaviors.skip-attributes-for-federated-user=true
```

---

### 5. Wrong User DN Structure

**Problem:**
```yaml
config:
  usersDn: ["CN=Users,DC=corporate,DC=com"]
  # But users are actually in OU=People,DC=corporate,DC=com
```

**Result:** Users not found, sync fails

**Solution:** Verify correct DN in LDAP:
```bash
# Test LDAP search
ldapsearch -H ldap://ldap.corporate.com \
  -D "CN=admin,DC=corporate,DC=com" \
  -w password \
  -b "DC=corporate,DC=com" \
  "(objectClass=user)"
```

---

## Best Practices

1. **Use READ_ONLY Mode for Corporate Directories**
```yaml
   config:
     editMode: ["READ_ONLY"]
```
   - Safest approach
   - LDAP remains authoritative
   - No write permission issues

2. **Separate Local Service Accounts**
   - LDAP for human users
   - Local users for service accounts
   - Clear separation of concerns

3. **Don't Include User Definitions with LDAP**
```yaml
   # Good
   components:
     - name: "ldap"
       config: ...
   # No users section
   
   # Bad
   components:
     - name: "ldap"
   users:
     - username: "..." # Conflict!
```

4. **Use Environment Variables for Credentials**
```yaml
   bindCredential: ["$(env:LDAP_PASSWORD)"]
```
   - Never commit passwords
   - Use secrets management

5. **Configure Proper Sync Schedules**
```yaml
   fullSyncPeriod: ["86400"]    # Daily
   changedSyncPeriod: ["3600"]  # Hourly
```
   - Balance freshness vs. load
   - Monitor sync performance

6. **Test LDAP Connection First**
```bash
   ldapsearch -H ldap://ldap.company.com \
     -D "CN=keycloak,CN=Users,DC=company,DC=com" \
     -w password \
     -b "DC=company,DC=com" \
     "(objectClass=user)"
```
   - Verify before configuring Keycloak
   - Check permissions

7. **Use SSL/TLS for LDAP Connections**
```yaml
   connectionUrl: ["ldaps://ldap.company.com:636"]
   useTruststoreSpi: ["ldapsOnly"]
```
   - Secure credential transmission
   - Production requirement

8. **Document LDAP Schema Requirements**
   - Required attributes
   - Object classes
   - DN structure
   - Keep in repository

---

## Troubleshooting

### Users Not Syncing from LDAP

**Symptom:** LDAP configured but no users appear

**Diagnosis:**
1. Check LDAP connection:
```bash
   # In Keycloak Admin Console
   # User Federation → ldap → Test connection
   # User Federation → ldap → Test authentication
```

2. Check sync status:
```bash
   # User Federation → ldap → Synchronize all users
```

3. Verify search base:
```yaml
   usersDn: ["CN=Users,DC=company,DC=com"]  # Correct?
```

**Solution:**
- Fix connection settings
- Verify usersDn is correct
- Check LDAP service account permissions
- Review Keycloak logs for LDAP errors

---

### User Creation Fails with 400

**Symptom:** `400 Bad Request` when importing users

**Diagnosis:**
```bash
# Check if LDAP is configured
curl -s "http://localhost:8080/admin/realms/myrealm/components" \
  -H "Authorization: Bearer $TOKEN" | \
  jq '.[] | select(.providerType=="org.keycloak.storage.UserStorageProvider")'
```

**Solution:**
- If LDAP present: Remove users from config or switch to WRITABLE mode
- If no LDAP: Check for other user federation (SSSD, Kerberos, etc.)

---

### LDAP Write Operations Fail

**Symptom:** Users configured with WRITABLE mode but creation fails

**Possible causes:**
1. Insufficient LDAP permissions
2. Missing required attributes
3. LDAP password policy violations
4. Wrong DN structure

**Solution:**
```yaml
# Provide all required attributes
users:
  - username: "john.doe"
    firstName: "John"       # Required
    lastName: "Doe"         # Required
    email: "john@company.com"  # Required
    credentials:
      - type: "password"
        value: "ComplexP@ssw0rd123"  # Meet LDAP policy
        temporary: false
```

---

### Federated User Attributes Read-Only

**Symptom:** Cannot update user attributes

**Solution:** Use skip-attributes flag:
```bash
--import.behaviors.skip-attributes-for-federated-user=true
```

Or change to UNSYNCED mode:
```yaml
config:
  editMode: ["UNSYNCED"]
```

---

## Configuration Options
```bash
# Skip attributes for federated users
--import.behaviors.skip-attributes-for-federated-user=true

# Enable user federation sync
--import.behaviors.sync-user-federation=true
```

---

## Consequences

When using LDAP user federation:

1. **User Management Shifts to LDAP:** Cannot create users in Keycloak with READ_ONLY mode
2. **Attribute Limitations:** Some attributes become read-only
3. **Authentication Dependence:** LDAP must be available for authentication
4. **Sync Delays:** User changes in LDAP may not appear immediately in Keycloak
5. **Local vs Federated Complexity:** Need to manage two types of users separately
6. **Password Policies:** LDAP password policies take precedence

---

## Related Issues

- [#1291 - Adding user with LDAP causes 400 error](https://github.com/adorsys/keycloak-config-cli/issues/1291)

---

## Additional Resources

- [Keycloak LDAP Documentation](https://www.keycloak.org/docs/latest/server_admin/#_ldap)
- [User Federation](https://www.keycloak.org/docs/latest/server_admin/#_user-storage-federation)
- [Import Behaviors Configuration](../import-settings.md)
