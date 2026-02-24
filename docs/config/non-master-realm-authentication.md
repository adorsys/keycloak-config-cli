# Non-Master Realm Authentication

Starting with Keycloak 26.4.0, access to the global `/admin/serverinfo` endpoint is restricted to users authenticated in the `master` realm. When authenticating against a non-master realm to manage its configuration, keycloak-config-cli needs special configuration to bypass server info fetching and use alternative authentication methods.

Related issues: [#1343](https://github.com/adorsys/keycloak-config-cli/issues/1343)

## The Problem

Users encounter authentication and server info access issues when:
- Authenticating with a user account in a non-master realm (e.g., `my-realm`)
- Keycloak 26.4.0+ restricts `/admin/serverinfo` to master realm users only
- keycloak-config-cli attempts to fetch server version information and fails with permission errors
- The authenticated user doesn't have admin rights in the master realm
- Organizations want to manage realms using realm-specific admin accounts
- Security policies prevent granting master realm access to application administrators

## What Changed in Keycloak 26.4.0+

### Before Keycloak 26.4.0
```bash
# This worked fine
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=my-realm \
  --keycloak.user=realm-admin \
  --keycloak.password=password \
  --import.files.locations=config.yaml
```

**Behavior:**
- Any authenticated user could access `/admin/serverinfo`
- keycloak-config-cli fetched server version successfully
- Configuration import proceeded normally

---

### After Keycloak 26.4.0

**Same command fails:**
```bash
# This now fails with 403 Forbidden
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=my-realm \
  --keycloak.user=realm-admin \
  --keycloak.password=password \
  --import.files.locations=config.yaml
```

**Error:**
```
Error fetching server info: 403 Forbidden
User authenticated in 'my-realm' does not have permission to access /admin/serverinfo
```

**Why it fails:**
- `/admin/serverinfo` is now restricted to master realm users only
- Realm-specific admins no longer have access
- keycloak-config-cli cannot determine Keycloak version
- Import process halts

---

## The Solution: Skip Server Info

Use the `--keycloak.skip-server-info=true` flag to bypass server info fetching.

### Basic Configuration
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=my-realm \
  --keycloak.user=realm-admin \
  --keycloak.password=password \
  --keycloak.skip-server-info=true \
  --import.files.locations=config.yaml
```

**Result:**
- ✅ Server info fetching is skipped
- ✅ Authentication succeeds against `my-realm`
- ✅ Configuration import proceeds normally
- ✅ No master realm access required

---

### With Environment Variables
```bash
export KEYCLOAK_URL=http://localhost:8080
export KEYCLOAK_LOGINREALM=my-realm
export KEYCLOAK_USER=realm-admin
export KEYCLOAK_PASSWORD=password
export KEYCLOAK_SKIPSERVERINFO=true
export IMPORT_FILES_LOCATIONS=config.yaml

java -jar keycloak-config-cli.jar
```

---

### Docker Usage
```bash
docker run \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  -e KEYCLOAK_LOGINREALM=my-realm \
  -e KEYCLOAK_USER=realm-admin \
  -e KEYCLOAK_PASSWORD=password \
  -e KEYCLOAK_SKIPSERVERINFO=true \
  -e IMPORT_FILES_LOCATIONS=/config/realm-config.yaml \
  -v $(pwd)/config:/config \
  adorsys/keycloak-config-cli:latest
```

---

### Kubernetes ConfigMap
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: keycloak-config-cli
data:
  KEYCLOAK_URL: "http://keycloak:8080"
  KEYCLOAK_LOGINREALM: "my-realm"
  KEYCLOAK_USER: "realm-admin"
  KEYCLOAK_SKIPSERVERINFO: "true"
  IMPORT_FILES_LOCATIONS: "/config/realm-config.yaml"
```

---

## When to Use Skip Server Info

### ✅ **Use Skip Server Info When:**

1. **Non-Master Realm Authentication**
    - Authenticating with users from realms other than `master`
    - Using realm-specific admin accounts
    - Managing individual realms independently

2. **Keycloak 26.4.0+**
    - Running Keycloak version 26.4.0 or higher
    - Encountering 403 Forbidden on `/admin/serverinfo`

3. **Security Policies**
    - Organization restricts master realm access
    - Principle of least privilege requires realm-scoped access
    - Service accounts limited to specific realms

4. **Multi-Tenant Environments**
    - Each tenant has separate realm
    - Tenant admins manage their own realms
    - No cross-realm administrative access

---

### ❌ **Don't Use Skip Server Info When:**

1. **Master Realm Authentication**
    - Already authenticating against `master` realm
    - User has appropriate master realm permissions

2. **Version-Specific Features Needed**
    - Relying on automatic version detection for compatibility
    - Using features that vary by Keycloak version

3. **Keycloak < 26.4.0**
    - Server info is accessible to all authenticated users
    - No restriction in place

---

## Consequences of Skipping Server Info

### What Happens

1. **No Version Fetching**
    - keycloak-config-cli cannot automatically determine Keycloak version
    - Version defaults to `unknown`

2. **Alternative Health Check**
    - Uses authentication-based health check instead
    - Validates connectivity via login realm authentication

3. **Compatibility Checks Limited**
    - Some version-specific compatibility checks may be skipped
    - Features may behave differently across versions

4. **Manual Version Specification Recommended**
    - Can explicitly specify version if needed: `--keycloak.version=26.0.0`

---

### Explicit Version Specification

If you know your Keycloak version and want version-specific behavior:
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=my-realm \
  --keycloak.user=realm-admin \
  --keycloak.password=password \
  --keycloak.skip-server-info=true \
  --keycloak.version=26.0.0 \
  --import.files.locations=config.yaml
```

**Benefits:**
- Version-specific features enabled
- Better compatibility handling
- Clearer in logs what version is expected

---

## Complete Configuration Examples

### Example 1: Realm-Specific Admin

**Scenario:** Each realm has its own admin user, no master realm access.
```bash
# Authentication Configuration
--keycloak.url=http://localhost:8080
--keycloak.login-realm=corporate-realm
--keycloak.user=corporate-admin
--keycloak.password=SecurePassword123
--keycloak.skip-server-info=true

# Import Configuration  
--import.files.locations=corporate-realm-config.yaml
--import.remote-state.enabled=true
--import.validate=true
```

**YAML Configuration (`corporate-realm-config.yaml`):**
```yaml
realm: "corporate-realm"
enabled: true
displayName: "Corporate Realm"

clients:
  - clientId: "corporate-app"
    enabled: true
    publicClient: true
    redirectUris:
      - "https://app.corporate.com/*"

users:
  - username: "john.doe"
    email: "john.doe@corporate.com"
    enabled: true
    realmRoles:
      - "user"
```

---

### Example 2: Multi-Realm Management

**Scenario:** Managing multiple realms with separate admin accounts.

**Structure:**
```
config/
├── realm-a/
│   ├── config.yaml
│   └── import.sh
├── realm-b/
│   ├── config.yaml
│   └── import.sh
└── realm-c/
    ├── config.yaml
    └── import.sh
```

**Import Script (`realm-a/import.sh`):**
```bash
#!/bin/bash

java -jar keycloak-config-cli.jar \
  --keycloak.url=${KEYCLOAK_URL} \
  --keycloak.login-realm=realm-a \
  --keycloak.user=${REALM_A_ADMIN_USER} \
  --keycloak.password=${REALM_A_ADMIN_PASSWORD} \
  --keycloak.skip-server-info=true \
  --import.files.locations=config/realm-a/config.yaml \
  --import.remote-state.enabled=true
```

---

### Example 3: CI/CD Pipeline

**Scenario:** Automated realm configuration in CI/CD using service accounts.

**GitLab CI (.gitlab-ci.yml):**
```yaml
deploy-realm-config:
  stage: deploy
  image: adorsys/keycloak-config-cli:latest
  script:
    - |
      java -jar /opt/keycloak-config-cli.jar \
        --keycloak.url=${KEYCLOAK_URL} \
        --keycloak.login-realm=${REALM_NAME} \
        --keycloak.client-id=config-cli-client \
        --keycloak.client-secret=${CLIENT_SECRET} \
        --keycloak.grant-type=client_credentials \
        --keycloak.skip-server-info=true \
        --import.files.locations=config/${REALM_NAME}.yaml \
        --import.validate=true
  only:
    - main
  environment:
    name: production
```

**Service Account Setup:**
1. Create client `config-cli-client` in the target realm
2. Enable "Service Accounts Enabled"
3. Assign appropriate roles to service account
4. Use client credentials grant type

---

### Example 4: Kubernetes Deployment

**Scenario:** Deploy keycloak-config-cli as a Kubernetes Job.
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: keycloak-realm-config
  namespace: keycloak
spec:
  template:
    spec:
      containers:
      - name: config-cli
        image: adorsys/keycloak-config-cli:latest
        env:
        - name: KEYCLOAK_URL
          value: "http://keycloak:8080"
        - name: KEYCLOAK_LOGINREALM
          value: "production-realm"
        - name: KEYCLOAK_USER
          valueFrom:
            secretKeyRef:
              name: keycloak-admin
              key: username
        - name: KEYCLOAK_PASSWORD
          valueFrom:
            secretKeyRef:
              name: keycloak-admin
              key: password
        - name: KEYCLOAK_SKIPSERVERINFO
          value: "true"
        - name: KEYCLOAK_AVAILABILITYCHECK_ENABLED
          value: "true"
        - name: KEYCLOAK_AVAILABILITYCHECK_TIMEOUT
          value: "120s"
        - name: IMPORT_FILES_LOCATIONS
          value: "/config/realm-config.yaml"
        volumeMounts:
        - name: config
          mountPath: /config
      volumes:
      - name: config
        configMap:
          name: realm-configuration
      restartPolicy: OnFailure
```

---

## Authentication Methods with Non-Master Realm

### Method 1: Username/Password (Default)
```bash
--keycloak.login-realm=my-realm
--keycloak.user=admin
--keycloak.password=password
--keycloak.skip-server-info=true
```

**Use when:**
- Interactive or development environments
- Credentials stored securely (secrets management)
- User has appropriate realm management permissions

---

### Method 2: Client Credentials (Service Account)
```bash
--keycloak.login-realm=my-realm
--keycloak.client-id=config-cli-client
--keycloak.client-secret=ClientSecretHere
--keycloak.grant-type=client_credentials
--keycloak.skip-server-info=true
```

**Setup:**
1. Create confidential client in target realm
2. Enable "Service Accounts Enabled"
3. Disable "Standard Flow" and "Direct Access Grants"
4. Assign roles via "Service Account Roles" tab

**Use when:**
- Automated processes (CI/CD)
- No user credentials available
- Principle of least privilege (service account has only needed permissions)

---

### Method 3: Client Credentials with Secret in File
```bash
export CLIENT_SECRET=$(cat /run/secrets/keycloak-client-secret)

java -jar keycloak-config-cli.jar \
  --keycloak.login-realm=my-realm \
  --keycloak.client-id=config-cli-client \
  --keycloak.client-secret=${CLIENT_SECRET} \
  --keycloak.grant-type=client_credentials \
  --keycloak.skip-server-info=true \
  --import.files.locations=config.yaml
```

**Use when:**
- Docker Swarm secrets
- Kubernetes secrets mounted as files
- Enhanced security for secret management

---

## Common Pitfalls

### 1. Forgetting skip-server-info Flag

**Problem:**
```bash
# Missing --keycloak.skip-server-info=true
java -jar keycloak-config-cli.jar \
  --keycloak.login-realm=my-realm \
  --keycloak.user=admin \
  --keycloak.password=password \
  --import.files.locations=config.yaml
```

**Error:**
```
403 Forbidden: /admin/serverinfo
```

**Solution:** Add the flag:
```bash
--keycloak.skip-server-info=true
```

---

### 2. Wrong Login Realm

**Problem:**
```bash
--keycloak.login-realm=master  # User exists in my-realm
--keycloak.user=realm-admin
```

**Error:**
```
401 Unauthorized: User not found
```

**Solution:** Use correct login realm:
```bash
--keycloak.login-realm=my-realm
--keycloak.user=realm-admin
```

---

### 3. Insufficient Permissions

**Problem:** User authenticated but cannot manage realm.

**Error:**
```
403 Forbidden: Insufficient permissions
```

**Cause:** User lacks required realm management roles.

**Solution:** Assign appropriate roles:
1. In Keycloak Admin Console
2. Navigate to: **Users** → select user → **Role Mappings**
3. Assign client roles from `realm-management`:
    - `manage-realm`
    - `manage-clients`
    - `manage-users`
    - Or assign composite role: `realm-admin`

---

### 4. Client Credentials Misconfiguration

**Problem:**
```bash
--keycloak.grant-type=client_credentials
--keycloak.client-id=my-client
--keycloak.client-secret=wrong-secret
```

**Error:**
```
401 Unauthorized: Invalid client credentials
```

**Solution:**
- Verify client secret is correct
- Ensure "Service Accounts Enabled" is ON
- Check client is confidential (not public)

---

### 5. Version Incompatibility Assumptions

**Problem:** Assuming behavior is identical across Keycloak versions when skipping server info.

**Risk:** Features may work differently in different versions.

**Solution:** Explicitly specify version if using version-specific features:
```bash
--keycloak.skip-server-info=true
--keycloak.version=26.0.0
```

---

## Best Practices

1. **Use Service Accounts in Production**
    - More secure than user credentials
    - Easier to rotate secrets
    - Better audit trail

2. **Always Specify skip-server-info for Non-Master**
    - Make it explicit in scripts and documentation
    - Prevents unexpected failures after Keycloak upgrades

3. **Explicitly Set Version When Known**
    - Helps with compatibility checks
    - Makes behavior more predictable
    - Useful for debugging

4. **Use Availability Checks**
```bash
   --keycloak.availability-check.enabled=true
   --keycloak.availability-check.timeout=120s
```
- Ensures Keycloak is ready before import
- Prevents failures in CI/CD pipelines

5. **Separate Configs Per Realm**
    - Each realm has its own configuration file
    - Each realm has its own admin credentials
    - Clear separation of concerns

6. **Enable Validation**
```bash
   --import.validate=true
```
- Catches configuration errors early
- Prevents partial imports

7. **Use Remote State**
```bash
   --import.remote-state.enabled=true
```
- Tracks managed resources
- Safer for partial imports

8. **Secure Credential Management**
    - Use secrets management (Vault, Sealed Secrets, etc.)
    - Never commit credentials to version control
    - Rotate credentials regularly

---

## Troubleshooting

### Cannot Access Realm After Authentication

**Symptom:** Authentication succeeds but cannot access realm configuration

**Diagnosis:**
```bash
# Test authentication
curl -X POST "http://localhost:8080/realms/my-realm/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=realm-admin" \
  -d "password=password" \
  -d "grant_type=password"

# If successful, test realm access
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/my-realm"
```

**Possible causes:**
1. User lacks realm management roles
2. Realm name mismatch
3. Client not authorized

**Solution:** Verify user roles and permissions

---

### Server Info Still Being Accessed

**Symptom:** Still seeing server info access attempts despite skip flag

**Diagnosis:** Check effective configuration:
```bash
java -jar keycloak-config-cli.jar \
  --debug \
  --keycloak.skip-server-info=true \
  ... other options ...
```

**Possible causes:**
- Environment variable overriding command line
- Configuration file conflicting
- Old version of keycloak-config-cli

**Solution:**
- Check environment variables: `env | grep KEYCLOAK`
- Update to latest keycloak-config-cli version
- Ensure no conflicting configurations

---

### Availability Check Fails

**Symptom:** Timeout waiting for Keycloak

**Error:**
```
Keycloak not available after 120s
```

**Possible causes:**
1. Keycloak not fully started
2. Wrong URL
3. Network connectivity issues
4. Realm not created yet

**Solution:**
- Increase timeout: `--keycloak.availability-check.timeout=300s`
- Verify Keycloak is running: `curl http://localhost:8080`
- Check realm exists

---

## Migration Guide

### From Master Realm to Non-Master Realm Authentication

**Before (Keycloak < 26.4.0):**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=my-realm-config.yaml
```

**After (Keycloak 26.4.0+):**

**Option 1: Continue using master realm (not recommended)**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=master \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=my-realm-config.yaml
```

**Option 2: Switch to realm-specific admin (recommended)**
```bash
# 1. Create admin user in target realm
# 2. Assign realm-admin role
# 3. Use skip-server-info

java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=my-realm \
  --keycloak.user=my-realm-admin \
  --keycloak.password=SecurePassword \
  --keycloak.skip-server-info=true \
  --import.files.locations=my-realm-config.yaml
```

**Benefits of Option 2:**
- Follows principle of least privilege
- Each realm independently managed
- Better security separation
- Aligns with multi-tenant architectures

---

## Configuration Reference

### CLI Options
```bash
# Required for non-master realm authentication (Keycloak 26.4.0+)
--keycloak.skip-server-info=true

# Optional: Explicit version specification
--keycloak.version=26.0.0

# Authentication realm
--keycloak.login-realm=my-realm

# User credentials
--keycloak.user=admin
--keycloak.password=password

# OR client credentials
--keycloak.client-id=config-cli
--keycloak.client-secret=secret
--keycloak.grant-type=client_credentials
```

### Environment Variables
```bash
# Required
export KEYCLOAK_SKIPSERVERINFO=true
export KEYCLOAK_LOGINREALM=my-realm

# Optional
export KEYCLOAK_VERSION=26.0.0

# Credentials
export KEYCLOAK_USER=admin
export KEYCLOAK_PASSWORD=password

# OR
export KEYCLOAK_CLIENTID=config-cli
export KEYCLOAK_CLIENTSECRET=secret
export KEYCLOAK_GRANTTYPE=client_credentials
```

---

## Security Considerations

1. **Principle of Least Privilege**
    - Use realm-specific admins
    - Avoid master realm access when possible
    - Grant only necessary permissions

2. **Service Account Best Practice**
    - Preferred over user credentials in automation
    - Easier to audit and rotate
    - Can be scoped to specific clients/roles

3. **Credential Management**
    - Use secrets management systems
    - Rotate credentials regularly
    - Never log or expose credentials

4. **Network Security**
    - Use HTTPS in production
    - Enable SSL verification: `--keycloak.ssl-verify=true`
    - Consider VPN or private networks

5. **Audit Trail**
    - Monitor authentication attempts
    - Log configuration changes
    - Review service account usage

---

## Keycloak Version Compatibility

| Keycloak Version | ServerInfo Access | Skip Flag Required |
|------------------|-------------------|-------------------|
| < 26.4.0 | All authenticated users | No |
| 26.4.0 - 26.x.x | Master realm only | Yes (for non-master) |
| 27.0.0+ | Master realm only | Yes (for non-master) |

**Recommendation:** Always use `--keycloak.skip-server-info=true` when authenticating against non-master realms for forward compatibility.
