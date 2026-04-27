---
title: Non-Master Realm Authentication
description: Guide for using keycloak-config-cli with non-master realm authentication and the skip-server-info flag
sidebar_position: 5
---

# Non-Master Realm Authentication

Starting with Keycloak 26.4.0, access to the global `/admin/serverinfo` endpoint is restricted to users authenticated in the `master` realm. When authenticating against a non-master realm to manage its configuration, keycloak-config-cli needs special configuration to bypass server info fetching.

Related issues: [#1343](https://github.com/adorsys/keycloak-config-cli/issues/1343)

## The Problem

Users encounter authentication and server info access issues when:

- Authenticating with a user account in a non-master realm (e.g., `my-realm`)
- Keycloak 26.4.0+ restricts `/admin/serverinfo` to master realm users only
- keycloak-config-cli attempts to fetch server version information and fails with permission errors
- The authenticated user does not have admin rights in the master realm

## What Changed in Keycloak 26.4.0+

### Before Keycloak 26.4.0

This worked fine:
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=my-realm \
  --keycloak.user=realm-admin \
  --keycloak.password=password \
  --import.files.locations=path/to/config-file
```

**Behavior:**

- Any authenticated user could access `/admin/serverinfo`
- keycloak-config-cli fetched server version successfully
- Configuration import proceeded normally

### After Keycloak 26.4.0

**Same command fails:**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.login-realm=my-realm \
  --keycloak.user=realm-admin \
  --keycloak.password=password \
  --import.files.locations=path/to/config-file
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
  --import.files.locations=path/to/config-file
```

**Result:**

- Server info fetching is skipped
- Authentication succeeds against `my-realm`
- Configuration import proceeds normally
- No master realm access required

### With Environment Variables
```bash
export KEYCLOAK_URL=http://localhost:8080
export KEYCLOAK_LOGINREALM=my-realm
export KEYCLOAK_USER=realm-admin
export KEYCLOAK_PASSWORD=password
export KEYCLOAK_SKIPSERVERINFO=true
export IMPORT_FILES_LOCATIONS=path/to/config-file

java -jar keycloak-config-cli.jar
```

### Docker Usage
```bash
docker run \
  -e KEYCLOAK_URL=http://keycloak:8080 \
  -e KEYCLOAK_LOGINREALM=my-realm \
  -e KEYCLOAK_USER=realm-admin \
  -e KEYCLOAK_PASSWORD=password \
  -e KEYCLOAK_SKIPSERVERINFO=true \
  -e IMPORT_FILES_LOCATIONS=path/to/config-file
  -v $(pwd)/config:/config \
  adorsys/keycloak-config-cli:latest
```

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
  IMPORT_FILES_LOCATIONS: "path/to/config-file"
```

## When to Use Skip Server Info

### Use Skip Server Info When:

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

### Do Not Use Skip Server Info When:

1. **Master Realm Authentication**
    - Already authenticating against `master` realm
    - User has appropriate master realm permissions

2. **Version-Specific Features Needed**
    - Relying on automatic version detection for compatibility
    - Using features that vary by Keycloak version

3. **Keycloak < 26.4.0**
    - Server info is accessible to all authenticated users
    - No restriction in place

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
  --import.files.locations=path/to/config-file
```

**Benefits:**

- Version-specific features enabled
- Better compatibility handling
- Clearer in logs what version is expected

## Authentication Methods with Non-Master Realm

### Method 1: Username/Password (Default)
```bash
--keycloak.login-realm=my-realm
--keycloak.user=realm-admin
--keycloak.password=password
--keycloak.skip-server-info=true
```

**Use when:**

- Interactive or development environments
- Credentials stored securely (secrets management)
- User has appropriate realm management permissions

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

## Common Pitfalls

### 1. Forgetting skip-server-info Flag

**Problem:**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.login-realm=my-realm \
  --keycloak.user=admin \
  --keycloak.password=password \
  --import.files.locations=path/to/config-file
```

**Error:**
```
403 Forbidden: /admin/serverinfo
```

**Solution:** Add the flag:
```bash
--keycloak.skip-server-info=true
```

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

## Troubleshooting

### Test Authentication
```bash
curl -X POST "http://localhost:8080/realms/my-realm/protocol/openid-connect/token" \
  -d "client_id=admin-cli" \
  -d "username=realm-admin" \
  -d "password=password" \
  -d "grant_type=password"
```

### Test Server Info Access
```bash
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/my-realm/protocol/openid-connect/token" \
  -d "client_id=admin-cli" -d "username=realm-admin" -d "password=password" \
  -d "grant_type=password" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

curl -H "Authorization: Bearer $TOKEN" \
  -w "\nHTTP Status: %{http_code}\n" \
  http://localhost:8080/admin/serverinfo
```

## Quick Reference

| Option | CLI Flag | Environment Variable |
|--------|----------|---------------------|
| Skip server info | `--keycloak.skip-server-info=true` | `KEYCLOAK_SKIPSERVERINFO=true` |
| Keycloak URL | `--keycloak.url` | `KEYCLOAK_URL` |
| Login realm | `--keycloak.login-realm` | `KEYCLOAK_LOGINREALM` |
| Username | `--keycloak.user` | `KEYCLOAK_USER` |
| Password | `--keycloak.password` | `KEYCLOAK_PASSWORD` |
| Config file | `--import.files.locations` | `IMPORT_FILES_LOCATIONS` |

## Keycloak Version Compatibility

| Keycloak Version | ServerInfo Access | Skip Flag Required |
|------------------|-------------------|-------------------|
| < 26.4.0 | All authenticated users | No |
| 26.4.0 - 26.x.x | Master realm only | Yes (for non-master) |
| 27.0.0+ | Master realm only | Yes (for non-master) |

**Recommendation:** Always use `--keycloak.skip-server-info=true` when authenticating against non-master realms for forward compatibility.
