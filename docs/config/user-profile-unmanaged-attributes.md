# User Profile Unmanaged Attribute Policy

When configuring Keycloak user profiles, the `unmanagedAttributePolicy` setting controls how the system handles user attributes that aren't explicitly defined in the user profile configuration. Understanding how to properly configure this policy is essential for managing custom user attributes and controlling attribute permissions.

Related issues: [#1016](https://github.com/adorsys/keycloak-config-cli/issues/1016)

## The Problem

Users often encounter challenges when configuring user profile unmanaged attribute policies because:
- Setting `unmanagedAttributePolicy` directly at the top level doesn't work as expected
- The correct location for this setting within the user profile configuration is unclear
- Different Keycloak versions have different user profile configuration structures
- Error messages don't clearly indicate where the policy should be placed
- The relationship between managed and unmanaged attributes is not well documented
- Understanding the impact of different policy values requires trial and error

## What is unmanagedAttributePolicy?

The `unmanagedAttributePolicy` determines how Keycloak handles user attributes that are NOT explicitly defined in the user profile configuration. These are attributes that users or administrators might add outside the defined profile schema.

**Policy Values:**

| Value | Behavior | Use Case |
|-------|----------|----------|
| `ENABLED` | Unmanaged attributes can be created and edited by users | Allow custom user attributes freely |
| `ADMIN_EDIT` | Only admins can create/edit unmanaged attributes | Restrict attribute creation to admins |
| `ADMIN_VIEW` | Only admins can view unmanaged attributes; users cannot | Hide custom attributes from users |

## Correct Configuration Location

### The Wrong Way (Common Mistake)

**This DOES NOT work:**
```yaml
realm: "myrealm"
userProfile:
  unmanagedAttributePolicy: "ENABLED"  # ❌ Wrong location!
  attributes:
    - name: "username"
      required: true
```

**Why it fails:** The `unmanagedAttributePolicy` is not a direct child of `userProfile`. It must be nested within the profile configuration structure.

---

### The Correct Way

**This WORKS:**
```yaml
realm: "myrealm"
userProfile:
  attributes:
    - name: "username"
      displayName: "${username}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations: {}
    
    - name: "email"
      displayName: "${email}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        email: {}
    
    - name: "firstName"
      displayName: "${firstName}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    
    - name: "lastName"
      displayName: "${lastName}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
  
  groups: []
  
  unmanagedAttributePolicy: "ENABLED"  # ✅ Correct location!
```

**Key points:**
- `unmanagedAttributePolicy` is at the same level as `attributes` and `groups`
- It's inside `userProfile` but not inside `attributes`
- All managed attributes should be explicitly defined before setting the policy

---

## Usage

### Allow Users to Add Custom Attributes

**Scenario:** You want users to be able to add their own custom attributes (like department, phone extension, etc.)
```yaml
realm: "myrealm"
userProfile:
  attributes:
    - name: "username"
      displayName: "${username}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    
    - name: "email"
      displayName: "${email}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        email: {}
  
  unmanagedAttributePolicy: "ENABLED"
```

**Result:**
- Users can add custom attributes via API or account management
- Users can view and edit their own custom attributes
- Admins can also view and edit all attributes
- Useful for flexible, self-service attribute management

**Example of user adding custom attribute:**
```json
{
  "username": "john.doe",
  "email": "john@example.com",
  "attributes": {
    "department": ["Engineering"],
    "phone_extension": ["1234"]
  }
}
```

---

### Restrict Unmanaged Attributes to Admin Only (Edit)

**Scenario:** Only administrators should be able to create and modify unmanaged attributes.
```yaml
realm: "myrealm"
userProfile:
  attributes:
    - name: "username"
      displayName: "${username}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    
    - name: "email"
      displayName: "${email}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        email: {}
    
    - name: "firstName"
      displayName: "${firstName}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    
    - name: "lastName"
      displayName: "${lastName}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
  
  unmanagedAttributePolicy: "ADMIN_EDIT"
```

**Result:**
- Users can view unmanaged attributes but cannot create or edit them
- Only admins can create, view, and edit unmanaged attributes
- Useful for controlled attribute management with admin oversight

---

### Hide Unmanaged Attributes from Users

**Scenario:** Unmanaged attributes should be completely hidden from regular users.
```yaml
realm: "myrealm"
userProfile:
  attributes:
    - name: "username"
      displayName: "${username}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    
    - name: "email"
      displayName: "${email}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        email: {}
  
  unmanagedAttributePolicy: "ADMIN_VIEW"
```

**Result:**
- Users cannot view, create, or edit unmanaged attributes
- Only admins can see and manage all attributes
- Useful for storing sensitive or system-level attributes
- Users only see attributes explicitly defined with user view permissions

---

## Complete User Profile Configuration Example

**Scenario:** A corporate environment with defined attributes and controlled unmanaged attribute access.
```yaml
realm: "corporate"
userProfile:
  attributes:
    # Standard Keycloak attributes
    - name: "username"
      displayName: "${username}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin"]  # Only admin can change username
      validations:
        length:
          min: 3
          max: 255
        username-prohibited-characters: {}
    
    - name: "email"
      displayName: "${email}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        email: {}
        length:
          max: 255
    
    - name: "firstName"
      displayName: "${firstName}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        length:
          max: 255
        person-name-prohibited-characters: {}
    
    - name: "lastName"
      displayName: "${lastName}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        length:
          max: 255
        person-name-prohibited-characters: {}
    
    # Custom managed attributes
    - name: "department"
      displayName: "Department"
      required: false
      permissions:
        view: ["admin", "user"]
        edit: ["admin"]  # Only admin can set department
      validations:
        length:
          max: 100
    
    - name: "employeeId"
      displayName: "Employee ID"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin"]  # Only admin can set employee ID
      validations:
        length:
          min: 5
          max: 20
    
    - name: "phoneNumber"
      displayName: "Phone Number"
      required: false
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        pattern:
          pattern: "^\\+?[1-9]\\d{1,14}$"
          error-message: "Invalid phone number format"
  
  groups: []
  
  unmanagedAttributePolicy: "ADMIN_EDIT"
```

**This configuration:**
- Defines 6 managed attributes with clear permissions
- Allows admins to add additional attributes as needed
- Prevents users from adding arbitrary attributes
- Enforces validation on all managed attributes
- Provides clear separation between user-editable and admin-only fields

---

## Attribute Types: Managed vs Unmanaged

### Managed Attributes

**Definition:** Explicitly defined in the `attributes` array with:
- Name
- Display name
- Permissions
- Validations
- Required flag

**Example:**
```yaml
attributes:
  - name: "department"
    displayName: "Department"
    required: false
    permissions:
      view: ["admin", "user"]
      edit: ["admin"]
```

**Characteristics:**
- Full control over permissions
- Validation rules enforced
- Clear documentation in configuration
- Consistent behavior across the realm

---

### Unmanaged Attributes

**Definition:** Attributes NOT in the `attributes` array, added dynamically by users or systems.

**Examples:**
- `custom_field_1`
- `legacy_system_id`
- `temporary_flag`

**Behavior:** Controlled by `unmanagedAttributePolicy`

**When to use:**
- Temporary attributes during migrations
- Integration with external systems
- User-defined custom fields
- Attributes that vary per user type

---

## Common Pitfalls

### 1. Wrong Configuration Level

**Problem:**
```yaml
realm: "myrealm"
unmanagedAttributePolicy: "ENABLED"  # ❌ At realm level
userProfile:
  attributes:
    - name: "username"
```

**Solution:**
```yaml
realm: "myrealm"
userProfile:
  attributes:
    - name: "username"
  unmanagedAttributePolicy: "ENABLED"  # ✅ Inside userProfile
```

---

### 2. Missing Required Attributes

**Problem:**
```yaml
userProfile:
  unmanagedAttributePolicy: "ENABLED"  # Missing standard attributes
```

**Solution:** Always include standard Keycloak attributes:
```yaml
userProfile:
  attributes:
    - name: "username"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    - name: "email"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
  unmanagedAttributePolicy: "ENABLED"
```

---

### 3. Incomplete Attribute Definitions

**Problem:**
```yaml
attributes:
  - name: "department"  # Missing permissions!
```

**What happens:** Attribute behavior is unpredictable.

**Solution:** Always include complete attribute definitions:
```yaml
attributes:
  - name: "department"
    displayName: "Department"
    required: false
    permissions:
      view: ["admin", "user"]
      edit: ["admin"]
    validations: {}
```

---

### 4. Conflicting Policies and Permissions

**Problem:**
```yaml
userProfile:
  attributes:
    - name: "customField"
      permissions:
        edit: ["user"]  # Users can edit
  unmanagedAttributePolicy: "ADMIN_EDIT"  # But unmanaged attributes are admin-only
```

**Confusion:** If `customField` is managed, users can edit it. But if it becomes unmanaged (removed from config), only admins can edit.

**Solution:** Be consistent with your policy:
- If allowing user attributes, use `ENABLED`
- If restricting to admins, define all needed attributes as managed

---

### 5. Exporting and Re-importing User Profiles

**Problem:** Exporting a realm and attempting to re-import the user profile without proper structure.

**Exported (may be incomplete):**
```json
{
  "userProfile": {
    "attributes": [...]
  }
}
```

**Solution:** Ensure `unmanagedAttributePolicy` is included:
```yaml
userProfile:
  attributes: [...]
  groups: []
  unmanagedAttributePolicy: "ADMIN_EDIT"
```

---

## Best Practices

1. **Always Define Core Attributes:** Include username, email, firstName, lastName as managed attributes
2. **Use Managed Attributes for Important Fields:** Define critical business attributes explicitly
3. **Choose Policy Based on Security:** Use `ADMIN_EDIT` or `ADMIN_VIEW` for production environments
4. **Document Custom Attributes:** Comment your configuration explaining custom fields
5. **Test Permission Changes:** Verify users can/cannot edit attributes as expected
6. **Version Control User Profiles:** Track changes to user profile configuration in Git
7. **Validate After Import:** Check Admin Console to verify policy was applied correctly
8. **Consider Migration Impact:** Changing policy affects existing unmanaged attributes

---

## Validation and Permissions Reference

### Permission Values

| Permission | Who Can Access |
|------------|---------------|
| `["admin", "user"]` | Both admins and users |
| `["admin"]` | Only admins |
| `["user"]` | Only users (rare, usually combined with admin) |

### Common Validations
```yaml
validations:
  # Email format
  email: {}
  
  # String length
  length:
    min: 3
    max: 255
  
  # Pattern matching
  pattern:
    pattern: "^[A-Z]{2}\\d{4}$"
    error-message: "Must be 2 letters + 4 digits"
  
  # Username restrictions
  username-prohibited-characters: {}
  
  # Person name restrictions
  person-name-prohibited-characters: {}
```

---

## Migration Scenarios

### From No User Profile to Managed Profile

**Step 1: Export current user attributes**
```bash
# Identify existing custom attributes in your realm
# Check users via Admin Console or API
```

**Step 2: Create user profile configuration**
```yaml
realm: "myrealm"
userProfile:
  attributes:
    # Standard attributes
    - name: "username"
      displayName: "${username}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    
    - name: "email"
      displayName: "${email}"
      required: true
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
      validations:
        email: {}
    
    # Existing custom attributes (now managed)
    - name: "department"
      displayName: "Department"
      required: false
      permissions:
        view: ["admin", "user"]
        edit: ["admin"]
    
    - name: "employeeId"
      displayName: "Employee ID"
      required: false
      permissions:
        view: ["admin", "user"]
        edit: ["admin"]
  
  unmanagedAttributePolicy: "ADMIN_EDIT"  # Lock down future attributes
```

**Step 3: Import configuration**
```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url=http://localhost:8080 \
  --keycloak.user=admin \
  --keycloak.password=admin \
  --import.files.locations=user-profile-config.yaml
```

**Step 4: Verify in Admin Console**
- Navigate to: **Realm Settings** → **User Profile**
- Check that all attributes are defined
- Verify `unmanagedAttributePolicy` is set correctly

---

### Changing Policy from ENABLED to ADMIN_EDIT

**Scenario:** You've allowed users to create custom attributes, now you want to lock it down.

**Current:**
```yaml
userProfile:
  attributes: [...]
  unmanagedAttributePolicy: "ENABLED"
```

**New:**
```yaml
userProfile:
  attributes:
    # Add any commonly used unmanaged attributes as managed
    - name: "phoneExtension"  # Was unmanaged, now managed
      displayName: "Phone Extension"
      required: false
      permissions:
        view: ["admin", "user"]
        edit: ["admin", "user"]
    
    - name: "officeLocation"  # Was unmanaged, now managed
      displayName: "Office Location"
      required: false
      permissions:
        view: ["admin", "user"]
        edit: ["admin"]
  
  unmanagedAttributePolicy: "ADMIN_EDIT"  # Changed
```

**Impact:**
- Existing unmanaged attributes remain but become read-only for users
- Users cannot create new unmanaged attributes
- Any frequently used unmanaged attributes should be promoted to managed

---

## Troubleshooting

### Policy Not Applied

**Symptom:** Users can still edit unmanaged attributes despite `ADMIN_EDIT` setting

**Diagnosis:**
```bash
# Check current user profile via API
curl -s "http://localhost:8080/admin/realms/myrealm/users/profile" \
  -H "Authorization: Bearer $TOKEN" | jq '.unmanagedAttributePolicy'
```

**Possible causes:**
1. Policy not at correct level in configuration
2. Configuration not imported successfully
3. Keycloak version doesn't support user profiles

**Solution:**
- Verify configuration structure
- Check import logs for errors
- Ensure Keycloak version 15+ (user profiles introduced in v15)

---

### Attribute Permissions Not Working

**Symptom:** Users can edit attributes marked as admin-only

**Cause:** Attribute is unmanaged and policy is `ENABLED`

**Solution:** Move attribute to managed attributes list:
```yaml
attributes:
  - name: "restrictedField"
    permissions:
      view: ["admin", "user"]
      edit: ["admin"]  # Now enforced
```

---

### Cannot Create Users After Enabling Profile

**Symptom:** User creation fails after enabling user profile

**Cause:** Required attributes not provided during user creation

**Solution:** Ensure all `required: true` attributes are provided:
```json
{
  "username": "john.doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true
}
```

Or mark optional in profile:
```yaml
attributes:
  - name: "firstName"
    required: false  # Make optional
```

---

## Configuration Options
```bash
# Validate configuration before import
--import.validate=true

# Enable remote state management
--import.remote-state.enabled=true
```

---

## Consequences

When configuring user profile unmanaged attribute policy:

1. **ENABLED Policy**: Maximum flexibility but less control over data structure
2. **ADMIN_EDIT Policy**: Balanced approach for most organizations
3. **ADMIN_VIEW Policy**: Highest security but may hide useful information from users
4. **Managed Attributes Required**: Standard attributes (username, email, etc.) must be explicitly defined
5. **Existing Attributes Unaffected**: Changing policy doesn't remove existing unmanaged attributes
6. **Validation Only on Managed**: Unmanaged attributes have no validation rules

---

## Security Considerations

1. **Least Privilege:** Use `ADMIN_EDIT` or `ADMIN_VIEW` in production
2. **Sensitive Data:** Store sensitive attributes as managed with admin-only permissions
3. **Data Leakage:** `ENABLED` policy allows users to add arbitrary data
4. **Compliance:** Some regulations require controlled attribute management
5. **Audit Trail:** Managed attributes provide better audit capabilities

---

## Keycloak Version Compatibility

| Keycloak Version | User Profile Support | unmanagedAttributePolicy |
|------------------|---------------------|--------------------------|
| < 15.0.0 | No | N/A |
| 15.0.0 - 18.0.0 | Experimental | Limited |
| 19.0.0+ | Stable | Full support |
| 21.0.0+ | Enhanced | Recommended |

**Note:** For Keycloak versions before 15, user profile configuration is not available.



