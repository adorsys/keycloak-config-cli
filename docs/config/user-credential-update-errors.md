# User Credential Update Errors

When updating existing users with new credentials through keycloak-config-cli, various errors can occur depending on password policies, credential types, and update strategies. Understanding how credential updates work and common failure scenarios is essential for reliable user management.

Related issues: [#904](https://github.com/adorsys/keycloak-config-cli/issues/904)

## The Problem

Users encounter credential update errors when modifying existing users because:
- Updating credentials triggers password policy validation
- Credential type mismatches cause failures
- Multiple credential entries can conflict
- Temporary credential flags behave unexpectedly during updates
- Password history policies may reject the new password
- Required actions may not be properly set or cleared
- It's unclear whether credentials are replaced or appended

## Understanding Credential Updates

### How Keycloak Handles Credential Updates

When updating user credentials:

1. **Credential Replacement**
   - New credentials replace existing ones of the same type
   - Old credentials are removed
   - Password history is updated

2. **Password Policy Validation**
   - All password policies apply to credential updates
   - Password history is checked
   - Complexity requirements must be met

3. **Temporary Password Behavior**
   - Setting `temporary: true` requires password change on next login
   - Setting `temporary: false` allows immediate use
   - Updating temporary flag alone doesn't change the password

---

## Common Error Scenarios

### Error 1: Password Policy Violation

**Configuration with password policy:**
```json
{
  "realm": "master",
  "passwordPolicy": "length(12) and upperCase(1) and lowerCase(1) and digits(1)"
}
```

**Attempting to set weak password:**
```json
{
  "realm": "master",
  "users": [
    {
      "username": "test.user",
      "email": "test@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "weak",
          "temporary": false
        }
      ]
    }
  ]
}
```

![Password policy violation error when updating credentials](../static/images/credential-update-images/credential-policy-error.png)

*Import fails with password policy violation error. The password "weak" doesn't meet the realm's policy requirements (minimum 12 characters, uppercase, lowercase, and digits).*

**Error Message:**
```
Failed to update user 'test.user'
Error: Password policy violation - Password must be at least 12 characters
```

**Why it fails:** The new password doesn't meet the realm's password policy requirements.

---

### Error 2: Credential Type Mismatch

**Problem:**
```json
{
  "users": [
    {
      "username": "existing.user",
      "credentials": [
        {
          "type": "otp",
          "value": "123456",
          "temporary": false
        }
      ]
    }
  ]
}
```

**Error:**
```
Failed to update user 'existing.user'
Error: Invalid credential type for user update
```

**Why it fails:** OTP credentials cannot be set directly via configuration. Only password credentials are supported.

---

### Error 3: Multiple Credential Conflicts

**Problem:**
```json
{
  "users": [
    {
      "username": "existing.user",
      "credentials": [
        {
          "type": "password",
          "value": "Password123",
          "temporary": false
        },
        {
          "type": "password",
          "value": "DifferentPassword456",
          "temporary": true
        }
      ]
    }
  ]
}
```

**Result:** Only the last credential is applied, previous ones are ignored.

---

### Error 4: Empty Credentials Array

**Problem:**
```json
{
  "users": [
    {
      "username": "existing.user",
      "credentials": []
    }
  ]
}
```

**Result:** All existing credentials are removed. User cannot log in.

---

## Solutions

### Solution 1: Validate Against Password Policy

**Strategy:** Ensure new passwords meet all policy requirements before import.

**Password policy configuration:**
```json
{
  "realm": "master",
  "passwordPolicy": "length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1)"
}
```

**Compliant user credential update:**
```json
{
  "realm": "master",
  "users": [
    {
      "username": "test.user",
      "email": "test@example.com",
      "firstName": "Test",
      "lastName": "User",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "ComplexP@ssw0rd123",
          "temporary": true
        }
      ]
    }
  ]
}
```
step1 

![Successful credential update meeting policy requirements](../static/images/credential-update-images/credential-update-success1.png)

step2

![Successful credential update meeting policy requirements](../static/images/credential-update-images/credential-update-success2.png)

*User credentials successfully updated with a password that meets all policy requirements. The temporary flag is set, requiring the user to change password on next login.*

**Benefits:**
- Import succeeds
- User can log in immediately (or is forced to change if temporary)
- Complies with security requirements

---

### Solution 2: Use Temporary Passwords for Updates

**Scenario:** Force users to change password after administrative update.
```json
{
  "users": [
    {
      "username": "existing.user",
      "email": "user@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "TempResetP@ss123",
          "temporary": true
        }
      ],
      "requiredActions": ["UPDATE_PASSWORD"]
    }
  ]
}
```

**Benefits:**
- User must change password on next login
- Secure password reset workflow
- Admin doesn't need to know user's permanent password

---

### Solution 3: Update User Without Changing Credentials

**Scenario:** Modify user attributes without touching password.
```json
{
  "users": [
    {
      "username": "existing.user",
      "email": "newemail@example.com",
      "firstName": "Updated",
      "lastName": "Name",
      "enabled": true
    }
  ]
}
```

**Result:**
- User attributes updated
- Password remains unchanged
- No credential validation errors

---

### Solution 4: Clear Required Actions After Update

**Problem:** User has old required actions that conflict with new credentials.

**Solution:**
```json
{
  "users": [
    {
      "username": "existing.user",
      "credentials": [
        {
          "type": "password",
          "value": "NewP@ssword123",
          "temporary": false
        }
      ],
      "requiredActions": []
    }
  ]
}
```

**Result:**
- Password updated
- Required actions cleared
- User can log in without additional steps

---

### Solution 5: Use Password Reset Instead of Update

**Scenario:** Need to reset password without importing full user config.

**Use Keycloak Admin API directly:**
```bash
# Get user ID
USER_ID=$(curl -s "http://localhost:8080/admin/realms/master/users?username=existing.user" \
  -H "Authorization: Bearer $TOKEN" | jq -r '.[0].id')

# Reset password
curl -X PUT "http://localhost:8080/admin/realms/master/users/$USER_ID/reset-password" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "password",
    "value": "NewP@ssword123",
    "temporary": true
  }'
```

---

## Credential Update Patterns

### Pattern 1: Administrative Password Reset
```json
{
  "users": [
    {
      "username": "locked.user",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "AdminResetP@ss123",
          "temporary": true
        }
      ],
      "requiredActions": ["UPDATE_PASSWORD", "VERIFY_EMAIL"]
    }
  ]
}
```

**Use case:** Admin resets password for locked-out user.

---

### Pattern 2: Service Account Credential Update
```json
{
  "users": [
    {
      "username": "api-service",
      "serviceAccountClientId": "backend-api",
      "enabled": true
    }
  ]
}
```

**Use case:** Service accounts don't have password credentials.

---

### Pattern 3: Bulk Password Update
```json
{
  "users": [
    {
      "username": "user1",
      "credentials": [
        {
          "type": "password",
          "value": "${USER1_NEW_PASSWORD}",
          "temporary": true
        }
      ]
    },
    {
      "username": "user2",
      "credentials": [
        {
          "type": "password",
          "value": "${USER2_NEW_PASSWORD}",
          "temporary": true
        }
      ]
    }
  ]
}
```

**With environment variables:**
```bash
export USER1_NEW_PASSWORD="User1NewP@ss123"
export USER2_NEW_PASSWORD="User2NewP@ss123"

java -jar keycloak-config-cli.jar \
  --import.var-substitution.enabled=true \
  --import.files.locations=users-update.json
```

---

## Password Policy Compatibility

### Common Password Policies
```json
{
  "passwordPolicy": "length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1) and notUsername and passwordHistory(5)"
}
```

### Valid Password Examples

For the above policy:

| Password | Valid | Reason |
|----------|-------|--------|
| `Admin123!` | No | Too short (< 12 characters) |
| `adminpassword123!` | No | No uppercase letter |
| `ADMINPASSWORD123!` | No | No lowercase letter |
| `AdminPassword!` | No | No digit |
| `AdminPassword123` | No | No special character |
| `AdminP@ssword123` | Yes | Meets all requirements |
| `ComplexP@ss123` | Yes | Meets all requirements |

---

## Complete Configuration Examples

### Example 1: Secure Password Update
```json
{
  "realm": "production",
  "enabled": true,
  "passwordPolicy": "length(14) and upperCase(1) and lowerCase(1) and digits(2) and specialChars(1) and notUsername and passwordHistory(12)",
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
          "value": "SecureTemp@Pass2024!",
          "temporary": true
        }
      ],
      "requiredActions": ["UPDATE_PASSWORD", "CONFIGURE_TOTP"]
    }
  ]
}
```

---

### Example 2: Update Multiple Users
```json
{
  "realm": "master",
  "users": [
    {
      "username": "user1",
      "email": "user1@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "TempP@ssword123!",
          "temporary": true
        }
      ]
    },
    {
      "username": "user2",
      "email": "user2@example.com",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "TempP@ssword456!",
          "temporary": true
        }
      ]
    },
    {
      "username": "user3",
      "email": "user3@example.com",
      "enabled": false
    }
  ]
}
```

---

### Example 3: Conditional Credential Update
```json
{
  "realm": "master",
  "users": [
    {
      "username": "active.user",
      "enabled": true,
      "credentials": [
        {
          "type": "password",
          "value": "NewP@ssword123",
          "temporary": false
        }
      ]
    },
    {
      "username": "inactive.user",
      "enabled": false
    }
  ]
}
```

---

## Common Pitfalls

### 1. Forgetting Password Policy Requirements

**Problem:**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "simple",
      "temporary": false
    }
  ]
}
```

**Error:** Policy violation

**Solution:** Meet all password policy requirements:
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "ComplexP@ssw0rd123",
      "temporary": false
    }
  ]
}
```

---

### 2. Specifying Multiple Passwords

**Problem:**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "Password1",
      "temporary": true
    },
    {
      "type": "password",
      "value": "Password2",
      "temporary": false
    }
  ]
}
```

**Result:** Only the last password is used.

**Solution:** Specify only one password credential:
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "Password1",
      "temporary": true
    }
  ]
}
```

---

### 3. Empty Credentials Array

**Problem:**
```json
{
  "credentials": []
}
```

**Result:** All credentials removed, user cannot log in.

**Solution:** Either omit credentials or provide valid credential:
```json
{
  "username": "user",
  "email": "user@example.com"
}
```

---

### 4. Not Setting Temporary Flag

**Problem:**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "AdminSetP@ss123"
    }
  ]
}
```

**Result:** Missing `temporary` field, defaults to `false`.

**Solution:** Always specify temporary flag explicitly:
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "AdminSetP@ss123",
      "temporary": true
    }
  ]
}
```

---

### 5. Credential Update with Wrong Type

**Problem:**
```json
{
  "credentials": [
    {
      "type": "otp",
      "value": "123456",
      "temporary": false
    }
  ]
}
```

**Error:** Invalid credential type

**Solution:** Only use `password` type:
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "P@ssword123",
      "temporary": true
    }
  ]
}
```

---

## Best Practices

1. **Always Use Temporary Passwords for Resets**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "TempP@ss123",
      "temporary": true
    }
  ],
  "requiredActions": ["UPDATE_PASSWORD"]
}
```

2. **Validate Passwords Against Policy Before Import**

Check password policy in realm settings and ensure all passwords meet requirements.

3. **Use Environment Variables for Sensitive Data**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "${USER_TEMP_PASSWORD}",
      "temporary": true
    }
  ]
}
```

4. **Document Password Requirements**

Include password policy in configuration comments or separate documentation.

5. **Test Credential Updates in Development First**

Always test credential update configurations in a non-production environment.

6. **Clear Required Actions When Appropriate**
```json
{
  "credentials": [
    {
      "type": "password",
      "value": "NewP@ss123",
      "temporary": false
    }
  ],
  "requiredActions": []
}
```

7. **Don't Update Credentials Unnecessarily**

If password doesn't need changing, omit credentials from configuration.

8. **Use Strong Passwords Even for Temporary**

Even temporary passwords should meet security requirements.

---

## Troubleshooting

### Credential Update Fails with Policy Error

**Symptom:**
```
Failed to update user
Error: Password policy violation
```

**Diagnosis:**

1. Check realm password policy: Realm Settings → Security defenses → Password policy

2. Verify password meets all requirements:
   - Length
   - Uppercase/lowercase
   - Digits
   - Special characters
   - Not username
   - Not in password history

**Solution:** Update password to meet all policy requirements.

---

### User Cannot Login After Update

**Symptom:** User exists but authentication fails

**Possible causes:**
1. Password set as temporary but user hasn't changed it
2. Required actions blocking login
3. User disabled
4. Password doesn't match what was configured

**Check:**
```json
{
  "username": "user",
  "enabled": true,
  "credentials": [
    {
      "type": "password",
      "value": "CorrectP@ss123",
      "temporary": false
    }
  ],
  "requiredActions": []
}
```

---

### Credential Update Succeeds But Password Unchanged

**Symptom:** Import succeeds but user's password didn't change

**Possible cause:** Password already in password history

**Solution:** Use a different password not in history, or use `temporary: true`.

---

## Configuration Options
```bash
--import.var-substitution.enabled=true

--import.validate=true

--import.remote-state.enabled=true
```

---

## Consequences

When updating user credentials:

1. **Policy Validation Always Applies:** All password policies are checked on credential updates
2. **Credentials Are Replaced:** New credentials replace existing ones completely
3. **Password History Tracked:** Each credential update adds to password history
4. **Temporary Flag Controls Behavior:** Determines if user must change password on next login
5. **Required Actions Important:** Must be set appropriately for intended workflow
6. **Service Accounts Different:** Service accounts don't use password credentials

---
