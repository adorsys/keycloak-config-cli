# Password History Limit Errors

When repeatedly importing user configurations with password credentials, Keycloak's password history policy can cause imports to fail once the history limit is reached. Understanding how password history works and how to configure credentials properly in keycloak-config-cli is essential for reliable, repeatable imports.

Related issues: [#1112](https://github.com/adorsys/keycloak-config-cli/issues/1112)

## The Problem

Users encounter password history errors when repeatedly importing realms because:
- Password history policy tracks previously used passwords
- Each import attempt with the same password adds to the history
- Once the history limit is reached, Keycloak rejects the password
- keycloak-config-cli treats the error as a failure, stopping the import
- Keycloak itself only logs a warning, not an error
- It's unclear how to configure idempotent password imports
- Existing configuration files assume first-time user creation

## Understanding Password History
## Related Issues

- [#1112 - Password history limit errors on repeated imports](https://github.com/adorsys/keycloak-config-cli/issues/1112)
- [#904 - Error when updating users with credentials](https://github.com/adorsys/keycloak-config-cli/issues/904)

---

## Additional Resources

- [Keycloak Password Policies Documentation](https://www.keycloak.org/docs/latest/server_admin/#password-policies)
- [Variable Substitution](../variable-substitution/overview.md)
- [User Management Best Practices](https://www.keycloak.org/docs/latest/server_admin/#user-management)

### How Password History Works

Keycloak's password history policy prevents users from reusing recent passwords:

1. **History Tracking**
   - Keycloak stores hashes of previous passwords
   - Number of tracked passwords defined by policy
   - History per user, not global

2. **Validation on Password Change**
   - New password compared against history
   - Rejected if found in recent history
   - Applies to user-initiated and admin-initiated changes

3. **Configuration Import Behavior**
   - Each import with credentials counts as password change
   - Repeated imports with same password fill history
   - Eventually hits limit and fails

---

## The Error

### Typical Error Scenario

**Configuration:**
```json
{
  "realm": "master",
  "passwordPolicy": "hashIterations(27500) and passwordHistory(3)",
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "Password123",
          "temporary": false
        }
      ]
    }
  ]
}
```
step1

![User created successfully with initial password](../static/images/password-history-images/password-history-initial.png)

step2

![User created successfully with initial password](../static/images/password-history-images/password-history-initial2.png)

 ```
 running import 3 more times will trigger an error message

 ```

step1

![Import fails after hitting password history limit](../static/images/password-history-images/password-history-error.png)

step2

![Import fails after hitting password history limit](../static/images/password-history-images/password-history-error1.png)

**Error Message:**
```
Failed to update user 'john.doe'
Error: invalidPasswordHistoryMessage - Invalid password: must not be equal to any of last 3 passwords
```

---

## Solutions

### Solution 1: Remove Credentials from Repeated Imports

**Scenario:** Set passwords once, then remove from configuration.

**Initial Import:**
```json
{
  "realm": "master",
  "passwordPolicy": "hashIterations(27500) and passwordHistory(3)",
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "Password123",
          "temporary": false
        }
      ]
    }
  ]
}
```

**Subsequent Imports:**
```json
{
  "realm": "master",
  "passwordPolicy": "hashIterations(27500) and passwordHistory(3)",
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "enabled": true
    }
  ]
}
```

**Workflow:**
```bash
java -jar keycloak-config-cli.jar \
  --import.files.locations=initial-setup.json

java -jar keycloak-config-cli.jar \
  --import.files.locations=realm-config.json
```

**Benefits:**
- Credentials set once
- No password history conflicts on repeated imports
- Clear separation of initial setup vs. configuration updates

---

### Solution 2: Use Unique Passwords per Import

**Scenario:** Development/testing environments where password uniqueness is needed.
```json
{
  "realm": "dev-realm",
  "passwordPolicy": "hashIterations(27500) and passwordHistory(3)",
  "users": [
    {
      "username": "test.user",
      "email": "test@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "TestPass-20260226143022",
          "temporary": true
        }
      ]
    }
  ]
}
```

**Result:**
- Each import uses different password
- Never hits history limit
- Useful for automated testing

**Note:** Requires variable substitution enabled:
```bash
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.files.locations=realm-config.json
```

---

### Solution 4: Reduce or Disable Password History

**Scenario:** Development environments where password history isn't needed.
```json
{
  "realm": "dev-realm",
  "passwordPolicy": "hashIterations(27500) and passwordHistory(0)",
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "Password123",
          "temporary": false
        }
      ]
    }
  ]
}
```

**Result:**
- No password history tracking
- Repeated imports work without errors
- Same password can be set repeatedly

**Warning:** Only for development. Production should have password history enabled.

---

### Solution 5: Separate User Creation from Updates

**Scenario:** Manage users separately from other realm configuration.

**Structure:**
```
config/
├── realm-base.json           # Realm settings, clients, roles
├── users-initial.json        # User creation with passwords
└── users-updates.json        # User updates without passwords
```

**realm-base.json:**
```json
{
  "realm": "myrealm",
  "passwordPolicy": "hashIterations(27500) and passwordHistory(3)",
  "clients": [
    {
      "clientId": "my-app"
    }
  ]
}
```

**users-initial.json (run once):**
```json
{
  "realm": "myrealm",
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "TempPassword123",
          "temporary": true
        }
      ]
    }
  ]
}
```

**users-updates.json (repeatable):**
```json
{
  "realm": "myrealm",
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@example.com",
      "enabled": true,
      "realmRoles": ["user"],
      "groups": ["/Employees"]
    }
  ]
}
```

**Workflow:**
```bash
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/realm-base.json

java -jar keycloak-config-cli.jar \
  --import.files.locations=config/users-initial.json

java -jar keycloak-config-cli.jar \
  --import.files.locations=config/realm-base.json

java -jar keycloak-config-cli.jar \
  --import.files.locations=config/users-updates.json
```

---

## Password Policy Configuration

### Understanding Password History Policy
```json
{
  "passwordPolicy": "passwordHistory(3)"
}
```

**Format:** `passwordHistory(N)` where N is number of previous passwords to track

**Values:**
- `0` = Disabled (any password accepted)
- `1` = Previous password cannot be reused
- `3` = Last 3 passwords cannot be reused (common)
- `12` = Last 12 passwords cannot be reused (high security)

### Complete Password Policy Example
```json
{
  "realm": "secure-realm",
  "passwordPolicy": "length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and notUsername and passwordHistory(5) and hashIterations(27500)"
}
```

**Breakdown:**
- `length(12)`: Minimum 12 characters
- `upperCase(1)`: At least 1 uppercase letter
- `lowerCase(1)`: At least 1 lowercase letter
- `digits(1)`: At least 1 number
- `specialChars(1)`: At least 1 special character
- `notUsername`: Password cannot contain username
- `passwordHistory(5)`: Last 5 passwords cannot be reused
- `hashIterations(27500)`: Password hash iterations

---

## Credential Configuration Best Practices

### Development Environment
```json
{
  "realm": "dev",
  "passwordPolicy": "length(8) and passwordHistory(0)",
  "users": [
    {
      "username": "dev.user",
      "email": "dev@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "devpass123",
          "temporary": false
        }
      ]
    }
  ]
}
```

**Characteristics:**
- Simple password requirements
- No password history
- Non-temporary passwords for convenience
- Repeated imports work without issues

---

### Staging Environment
```json
{
  "realm": "staging",
  "passwordPolicy": "length(10) and upperCase(1) and lowerCase(1) and digits(1) and passwordHistory(3)",
  "users": [
    {
      "username": "staging.user",
      "email": "staging@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "StagingPass123",
          "temporary": true
        }
      ]
    }
  ]
}
```

**Characteristics:**
- Moderate password requirements
- Limited password history (3)
- Temporary passwords
- Balances security and usability

---

### Production Environment
```json
{
  "realm": "production",
  "passwordPolicy": "length(14) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and notUsername and passwordHistory(12) and hashIterations(27500)",
  "users": [
    {
      "username": "prod.user",
      "email": "prod@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "ComplexTempP@ss123!",
          "temporary": true
        }
      ],
      "requiredActions": ["UPDATE_PASSWORD", "CONFIGURE_TOTP"]
    }
  ]
}
```

**Characteristics:**
- Strong password requirements
- Extensive password history (12)
- Always temporary passwords
- Additional required actions (MFA)

---

## Working with Existing Users

### Scenario: Update User Without Changing Password
```json
{
  "realm": "myrealm",
  "users": [
    {
      "username": "existing.user",
      "email": "updated@example.com",
      "enabled": true,
      "realmRoles": ["user", "manager"]
    }
  ]
}
```

**Result:**
- Email updated
- Role added
- Password remains unchanged
- No password history impact

---

### Scenario: Force Password Reset
```json
{
  "realm": "myrealm",
  "users": [
    {
      "username": "existing.user",
      "email": "user@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "NewTemporaryPassword123",
          "temporary": true
        }
      ],
      "requiredActions": ["UPDATE_PASSWORD"]
    }
  ]
}
```

**Result:**
- Password changed to temporary value
- User required to change on next login
- Password history incremented by 1

---

## Common Pitfalls

### 1. Repeated Imports with Same Password

**Problem:**
```json
{
  "users": [
    {
      "username": "john.doe",
      "credentials": [
        {
          "type": "password",
          "value": "Password123",
          "temporary": false
        }
      ]
    }
  ]
}
```

**After 3 imports (passwordHistory(3)):**
```
Error: Invalid password: must not be equal to any of last 3 passwords
```

**Solution:** Use `temporary: true`:
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "Password123",
      "temporary": true
    }
  ]
}
```

---

### 2. Not Understanding Temporary Password Behavior

**Misconception:** Temporary passwords change on every import

**Reality:** Temporary passwords remain the same on subsequent imports, avoiding password history issues

**Behavior:**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "TempPass123",
      "temporary": true
    }
  ]
}
```

Subsequent imports with same config:
- Password remains "TempPass123"
- No new password history entries
- User still required to change on first login

---

### 3. Copying Production Config to Dev

**Problem:** Production password policies in dev environment
```json
{
  "realm": "dev",
  "passwordPolicy": "length(14) and passwordHistory(12)",
  "users": [
    {
      "username": "dev.user",
      "credentials": [
        {
          "type": "password",
          "value": "TestPass123",
          "temporary": false
        }
      ]
    }
  ]
}
```

**Result:** Repeated imports quickly hit history limit in dev

**Solution:** Use appropriate policy for environment:
```json
{
  "realm": "dev",
  "passwordPolicy": "length(8) and passwordHistory(0)"
}
```

---

### 4. Not Removing Credentials After Initial Setup

**Problem:** Credentials remain in config file indefinitely
```json
{
  "users": [
    {
      "username": "john.doe",
      "credentials": [
        {
          "type": "password",
          "value": "Password123"
        }
      ]
    }
  ]
}
```

**Result:** Every import attempts to set password

**Solution:** Separate initial setup from updates:
```json
{
  "users": [
    {
      "username": "john.doe",
      "email": "john@example.com",
      "realmRoles": ["user"]
    }
  ]
}
```

---

### 5. Forgetting Variable Substitution Flag

**Problem:** Using dynamic passwords without enabling substitution
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "Pass-20260226"
    }
  ]
}
```

**Error:** Literal string used, not evaluated

**Solution:** Enable variable substitution:
```bash
java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.files.locations=config.json
```

---

## Best Practices

1. **Always Use Temporary Passwords in Config Files**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "TempPassword123",
      "temporary": true
    }
  ]
}
```

2. **Separate Initial Setup from Updates**
   - `users-initial.json` - Run once with passwords
   - `users-updates.json` - Repeatable without passwords

3. **Use Environment Variables for Sensitive Data**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "${USER_INITIAL_PASSWORD}",
      "temporary": true
    }
  ]
}
```

4. **Document Password Policy Requirements**
```json
{
  "passwordPolicy": "length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and passwordHistory(5)"
}
```

5. **Different Policies per Environment**
   - Dev: Lenient (passwordHistory(0))
   - Staging: Moderate (passwordHistory(3))
   - Prod: Strict (passwordHistory(12))

6. **Use Required Actions**
```json
{
  "users": [
    {
      "username": "john.doe",
      "credentials": [
        {
          "type": "password",
          "value": "TempPass123",
          "temporary": true
        }
      ],
      "requiredActions": ["UPDATE_PASSWORD", "CONFIGURE_TOTP"]
    }
  ]
}
```

7. **Test Import Repeatability**
```bash
for i in {1..5}; do
  java -jar keycloak-config-cli.jar \
    --import.files.locations=config.json
done
```

8. **Monitor Password History Count**
   - Check user credential history in Admin Console
   - Users → select user → Credentials tab
   - View password history entries

---

## Troubleshooting

### Import Fails with Password History Error

**Symptom:**
```
Failed to update user 'john.doe'
Error: invalidPasswordHistoryMessage
```

**Diagnosis:**

1. Check password policy in Admin Console: Realm Settings → Security defenses → Password policy

2. Check user's password history count: Admin Console → Users → john.doe → Credentials

3. Check if credentials in config:
```bash
jq '.users[] | select(.username == "john.doe") | .credentials' config.json
```

**Solution:**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "TempPass123",
      "temporary": true
    }
  ]
}
```

---

### Password Policy Too Strict for Testing

**Symptom:** Cannot create test users due to password requirements

**Solution:** Override for test realm:
```json
{
  "realm": "test",
  "passwordPolicy": "length(8) and passwordHistory(0)"
}
```

Or create separate test user:
```json
{
  "users": [
    {
      "username": "test.user",
      "credentials": [
        {
          "type": "password",
          "value": "Test1234!",
          "temporary": true
        }
      ]
    }
  ]
}
```

---

### User Cannot Login After Import

**Symptom:** User exists but cannot authenticate

**Possible causes:**
1. Password set as temporary
2. Required actions pending
3. User not enabled

**Check configuration:**
```json
{
  "users": [
    {
      "username": "john.doe",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "Password123",
          "temporary": false
        }
      ],
      "requiredActions": []
    }
  ]
}
```

---

### Password History Not Clearing

**Symptom:** Old passwords still in history after time

**Explanation:** Password history doesn't expire by time, only by count

**Solution:**
- Increase passwordHistory count to accommodate more password changes
- Or manually clear via Admin Console if necessary (not recommended)
- Or have user change password N+1 times to clear old entries

---

## Configuration Options
```bash
--import.var-substitution.enabled=true

--import.validate=true

--import.remote-state.enabled=true
```

---

## Consequences

When dealing with password history in keycloak-config-cli:

1. **Repeated Imports Increment History:** Each import with credentials counts as password change
2. **History Limit Causes Failures:** Once limit reached, import fails completely
3. **Temporary Passwords Are Idempotent:** Using `temporary: true` prevents history accumulation
4. **No Credentials = No Password Change:** Omitting credentials preserves existing passwords
5. **Environment-Specific Policies Required:** Dev/staging/prod need different password policies
6. **User Must Change Temporary Password:** First login requires password change

---

## Complete Example: Production-Ready Configuration
```json
{
  "realm": "production",
  "enabled": true,
  "passwordPolicy": "length(14) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and notUsername and passwordHistory(12) and hashIterations(27500)",
  "roles": {
    "realm": [
      {
        "name": "user",
        "description": "Standard user"
      },
      {
        "name": "admin",
        "description": "Administrator"
      }
    ]
  },
  "users": [
    {
      "username": "john.doe",
      "email": "john.doe@company.com",
      "firstName": "John",
      "lastName": "Doe",
      "enabled": true,
      "emailVerified": true,
      "credentials": [
        {
          "type": "password",
          "value": "${JOHN_DOE_TEMP_PASSWORD}",
          "temporary": true
        }
      ],
      "realmRoles": ["user"],
      "requiredActions": ["UPDATE_PASSWORD", "CONFIGURE_TOTP"]
    },
    {
      "username": "admin.user",
      "email": "admin@company.com",
      "firstName": "Admin",
      "lastName": "User",
      "enabled": true,
      "emailVerified": true,
      "credentials": [
        {
          "type": "password",
          "value": "${ADMIN_TEMP_PASSWORD}",
          "temporary": true
        }
      ],
      "realmRoles": ["admin"],
      "requiredActions": ["UPDATE_PASSWORD", "CONFIGURE_TOTP"]
    },
    {
      "username": "service-account-backend",
      "enabled": true,
      "serviceAccountClientId": "backend-api"
    }
  ],
  "clients": [
    {
      "clientId": "backend-api",
      "serviceAccountsEnabled": true,
      "standardFlowEnabled": false,
      "directAccessGrantsEnabled": false
    }
  ]
}
```

**Import with environment variables:**
```bash
export JOHN_DOE_TEMP_PASSWORD="ComplexTemp123!"
export ADMIN_TEMP_PASSWORD="AdminTemp456!"

java -jar keycloak-config-cli.jar \
  --keycloak.url=https://keycloak.company.com \
  --keycloak.user=admin \
  --keycloak.password=${KEYCLOAK_ADMIN_PASSWORD} \
  --import.var-substitution.enabled=true \
  --import.files.locations=production-realm.json \
  --import.validate=true \
  --import.remote-state.enabled=true
```

**Result:**
- Strong password policy enforced
- Temporary passwords require change
- MFA required for all users
- Passwords from environment variables
- Repeatable imports (temporary passwords don't increment history)
- Service accounts use client credentials, not passwords

---
