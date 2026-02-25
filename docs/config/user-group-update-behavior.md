# User Group Update Behavior

When updating user configurations in keycloak-config-cli, omitting the `groups` field can unexpectedly remove the user's existing group memberships. Understanding how keycloak-config-cli handles user updates and group assignments is essential for maintaining user group memberships across configuration imports.

Related issues: [#1132](https://github.com/adorsys/keycloak-config-cli/issues/1132)

## The Problem

Users encounter unexpected group removal when updating user configurations because:
- Omitting the `groups` field removes all existing group memberships
- keycloak-config-cli treats missing `groups` as "set groups to empty"
- It's unclear whether updates are additive or replace existing data
- Partial user updates can unintentionally affect group memberships
- Different fields have different update behaviors (some merge, some replace)
- No warning when groups will be removed
- Existing configuration files may not include complete user state

## Understanding User Update Behavior

### How keycloak-config-cli Updates Users

When a user exists and is updated:

1. **Field Presence Matters**
   - Fields present in config: Updated to specified values
   - Fields absent from config: Behavior varies by field type
   - Arrays (like groups): Absence means "set to empty"

2. **Groups Are Replaced, Not Merged**
   - Configuration `groups: []` → All groups removed
   - Configuration missing `groups` → All groups removed
   - Configuration `groups: ["/New"]` → Only "/New", others removed

3. **Remote State Tracking**
   - With `import.remote-state.enabled=true` (default)
   - Tracks which groups were assigned by keycloak-config-cli
   - Only removes groups it previously assigned

---

## The Error Scenario

### Initial User Creation

**First Import:**
```yaml
realm: "myrealm"
groups:
  - name: "Developers"
    path: "/Developers"
  - name: "Architects"
    path: "/Architects"

users:
  - username: "john.doe"
    email: "john.doe@example.com"
    enabled: true
    groups:
      - "/Developers"
      - "/Architects"
```

**Result:** ✅ User created with both groups

---

### User Update Without Groups

**Second Import (updating email):**
```yaml
realm: "myrealm"

users:
  - username: "john.doe"
    email: "john.doe.new@example.com"  # Updated email
    enabled: true
    # groups field omitted
```

**Result:** ❌ User's groups removed!
- Email updated successfully
- User no longer in "/Developers" or "/Architects"
- Groups deleted without warning

---

### What Users Expected

Users typically expect:
- Only specified fields to be updated
- Omitted fields to remain unchanged
- Additive behavior for arrays like groups

**But keycloak-config-cli behavior:**
- Omitted array fields are set to empty
- Groups are replaced, not merged
- Full user state must be specified

---

## Solutions

### Solution 1: Always Include Complete User State (Recommended)

**Strategy:** Always specify all user attributes in every import, including groups.
```yaml
realm: "myrealm"

groups:
  - name: "Developers"
    path: "/Developers"
  - name: "Architects"
    path: "/Architects"

users:
  - username: "john.doe"
    email: "john.doe.new@example.com"
    firstName: "John"
    lastName: "Doe"
    enabled: true
    emailVerified: true
    groups:
      - "/Developers"      # ✅ Explicitly maintained
      - "/Architects"      # ✅ Explicitly maintained
    realmRoles:
      - "user"
    attributes:
      department: ["Engineering"]
```

**Benefits:**
- Predictable behavior
- No unexpected deletions
- Configuration represents complete desired state
- Easy to review what groups user should have

**Best Practice:**
```yaml
# User template - always include all fields
users:
  - username: "USERNAME"
    email: "EMAIL"
    firstName: "FIRST"
    lastName: "LAST"
    enabled: true
    emailVerified: false
    groups: []              # Always present, even if empty
    realmRoles: []          # Always present, even if empty
    clientRoles: {}         # Always present, even if empty
    attributes: {}          # Always present, even if empty
```

---

### Solution 2: Separate User Management from Group Management

**Strategy:** Manage users and their group memberships in separate, focused imports.

**Structure:**
```
config/
├── users-base.yaml         # User definitions without groups
├── groups.yaml             # Group definitions
└── user-groups.yaml        # User-to-group mappings
```

**users-base.yaml:**
```yaml
realm: "myrealm"

users:
  - username: "john.doe"
    email: "john.doe@example.com"
    firstName: "John"
    lastName: "Doe"
    enabled: true
    emailVerified: true
    # No groups field
  
  - username: "jane.smith"
    email: "jane.smith@example.com"
    firstName: "Jane"
    lastName: "Smith"
    enabled: true
    emailVerified: true
    # No groups field
```

**groups.yaml:**
```yaml
realm: "myrealm"

groups:
  - name: "Developers"
    path: "/Developers"
  
  - name: "Architects"
    path: "/Architects"
  
  - name: "Managers"
    path: "/Managers"
```

**user-groups.yaml:**
```yaml
realm: "myrealm"

users:
  - username: "john.doe"
    groups:
      - "/Developers"
      - "/Architects"
  
  - username: "jane.smith"
    groups:
      - "/Developers"
      - "/Managers"
```

**Import workflow:**
```bash
# 1. Import base users (creates users, no groups affected)
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/users-base.yaml

# 2. Import groups (creates groups)
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/groups.yaml

# 3. Import user-group mappings (assigns groups)
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/user-groups.yaml
```

**Benefits:**
- Clear separation of concerns
- Can update user profiles without affecting groups
- Can update groups without affecting users
- Easier to review group assignments

**Trade-offs:**
- More files to manage
- Must run multiple imports
- Need to coordinate import order

---

### Solution 3: Use Minimal User Updates

**Strategy:** Only update users when absolutely necessary; manage groups separately via Admin Console or API.
```yaml
realm: "myrealm"

# Only include users that need updates
users:
  - username: "john.doe"
    email: "john.doe.new@example.com"
    # Minimal update - only email change
    # Groups managed manually via Admin Console
```

**With this approach:**
- Accept that groups are managed outside config files
- Use keycloak-config-cli for user creation only
- Manual group management via Admin Console
- Or use separate tooling for group management

**Not recommended for:**
- Full automation requirements
- GitOps workflows
- Environments requiring reproducibility

---

### Solution 4: Track Groups Externally

**Strategy:** Maintain external documentation or database of user-group mappings.

**GROUP_ASSIGNMENTS.md:**
```markdown
# User Group Assignments

## Developers Group
- john.doe
- jane.smith
- bob.wilson

## Architects Group
- john.doe
- alice.jones

## Managers Group
- jane.smith
- charlie.brown
```

**Configuration generation script:**
```python
#!/usr/bin/env python3
"""
Generate user configs with groups from external registry.
"""
import yaml

# Read group assignments
group_assignments = {
    "/Developers": ["john.doe", "jane.smith", "bob.wilson"],
    "/Architects": ["john.doe", "alice.jones"],
    "/Managers": ["jane.smith", "charlie.brown"]
}

# Read user base data
users_base = [
    {
        "username": "john.doe",
        "email": "john.doe@example.com",
        "firstName": "John",
        "lastName": "Doe"
    },
    # ... more users
]

# Build complete user configs with groups
users_complete = []
for user in users_base:
    username = user["username"]
    
    # Find all groups for this user
    user_groups = [
        group for group, members in group_assignments.items()
        if username in members
    ]
    
    # Add groups to user config
    user["groups"] = user_groups
    user["enabled"] = True
    users_complete.append(user)

# Write complete config
config = {
    "realm": "myrealm",
    "users": users_complete
}

with open("users-complete.yaml", "w") as f:
    yaml.dump(config, f, default_flow_style=False)

print("✅ Generated users-complete.yaml with group assignments")
```

**Usage:**
```bash
# Generate complete user config with groups
python generate_user_config.py

# Import generated config
java -jar keycloak-config-cli.jar \
  --import.files.locations=users-complete.yaml
```

---

### Solution 5: Use Placeholder for Empty Groups

**Strategy:** Always include `groups: []` explicitly, even when empty.
```yaml
realm: "myrealm"

users:
  # User with groups
  - username: "john.doe"
    email: "john.doe@example.com"
    enabled: true
    groups:
      - "/Developers"
      - "/Architects"
  
  # User without groups - explicitly empty
  - username: "jane.smith"
    email: "jane.smith@example.com"
    enabled: true
    groups: []  # ✅ Explicit empty array
  
  # User being updated - preserve existing groups
  - username: "bob.wilson"
    email: "bob.wilson.new@example.com"
    enabled: true
    groups:  # ✅ Must specify to preserve
      - "/Developers"
```

**Benefits:**
- Makes intent clear
- No ambiguity about group membership
- Easy to see who has groups and who doesn't
- Prevents accidental removal

---

## Remote State Behavior

### With Remote State Enabled (Default)
```bash
--import.remote-state.enabled=true  # Default
```

**Behavior:**
- keycloak-config-cli tracks which groups it assigned
- Only removes groups it previously assigned
- Groups assigned manually in Admin Console remain

**Example:**

**Initial Import:**
```yaml
users:
  - username: "john.doe"
    groups:
      - "/Developers"  # Managed by keycloak-config-cli
```

**Manual Assignment in Admin Console:**
- Admin adds john.doe to "/Managers" group

**Second Import (without groups field):**
```yaml
users:
  - username: "john.doe"
    email: "john.doe.new@example.com"
    # groups field omitted
```

**Result:**
- "/Developers" removed (was managed by keycloak-config-cli)
- "/Managers" preserved (was manually assigned)

**Important:** Even with remote state, omitting groups removes managed groups.

---

### Without Remote State
```bash
--import.remote-state.enabled=false
```

**Behavior:**
- No tracking of managed vs. manual groups
- All groups treated the same
- Omitting groups field removes ALL groups

**Not recommended** for most use cases.

---

## Array Field Behavior Summary

Understanding how different array fields behave:

| Field | Omitted | Empty Array `[]` | With Values |
|-------|---------|------------------|-------------|
| `groups` | All removed | All removed | Set to specified |
| `realmRoles` | All removed | All removed | Set to specified |
| `clientRoles` | All removed | All removed | Set to specified |
| `requiredActions` | All removed | All removed | Set to specified |
| `credentials` | Unchanged | All removed | Set to specified |

**Key Takeaway:** Always specify array fields explicitly to avoid unintended removal.

---

## Complete Configuration Examples

### Example 1: Full User State Management
```yaml
realm: "corporate"
enabled: true

groups:
  - name: "Engineering"
    path: "/Engineering"
    subGroups:
      - name: "Backend"
        path: "/Engineering/Backend"
      - name: "Frontend"
        path: "/Engineering/Frontend"
  
  - name: "Management"
    path: "/Management"

roles:
  realm:
    - name: "employee"
      description: "Standard employee"
    - name: "manager"
      description: "People manager"

users:
  # Developer - full state specified
  - username: "john.doe"
    email: "john.doe@corporate.com"
    firstName: "John"
    lastName: "Doe"
    enabled: true
    emailVerified: true
    groups:
      - "/Engineering/Backend"
    realmRoles:
      - "employee"
    attributes:
      department: ["Engineering"]
      employeeId: ["ENG-001"]
    credentials:
      - type: "password"
        value: "TempPassword123"
        temporary: true
  
  # Manager - full state specified
  - username: "jane.smith"
    email: "jane.smith@corporate.com"
    firstName: "Jane"
    lastName: "Smith"
    enabled: true
    emailVerified: true
    groups:
      - "/Engineering/Frontend"
      - "/Management"
    realmRoles:
      - "employee"
      - "manager"
    attributes:
      department: ["Engineering"]
      employeeId: ["ENG-002"]
    credentials:
      - type: "password"
        value: "TempPassword456"
        temporary: true
  
  # Service account - no groups needed, explicitly empty
  - username: "api-service"
    email: "api@corporate.com"
    enabled: true
    serviceAccountClientId: "backend-api"
    groups: []           # ✅ Explicitly no groups
    realmRoles: []       # ✅ Explicitly no roles
    attributes: {}       # ✅ Explicitly no attributes
```

---

### Example 2: Separate Files Strategy

**config/01-groups.yaml:**
```yaml
realm: "corporate"

groups:
  - name: "Engineering"
    path: "/Engineering"
  - name: "Sales"
    path: "/Sales"
  - name: "Support"
    path: "/Support"
```

**config/02-users-profiles.yaml:**
```yaml
realm: "corporate"

users:
  - username: "john.doe"
    email: "john.doe@corporate.com"
    firstName: "John"
    lastName: "Doe"
    enabled: true
    emailVerified: true
    attributes:
      department: ["Engineering"]
      employeeId: ["ENG-001"]
  
  - username: "jane.smith"
    email: "jane.smith@corporate.com"
    firstName: "Jane"
    lastName: "Smith"
    enabled: true
    emailVerified: true
    attributes:
      department: ["Sales"]
      employeeId: ["SALES-001"]
```

**config/03-user-groups.yaml:**
```yaml
realm: "corporate"

users:
  - username: "john.doe"
    groups:
      - "/Engineering"
  
  - username: "jane.smith"
    groups:
      - "/Sales"
```

**Import script:**
```bash
#!/bin/bash
set -e

echo "Importing groups..."
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/01-groups.yaml

echo "Importing user profiles..."
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/02-users-profiles.yaml

echo "Importing user-group assignments..."
java -jar keycloak-config-cli.jar \
  --import.files.locations=config/03-user-groups.yaml

echo "✅ Import complete"
```

---

### Example 3: Conditional Groups Based on Attributes
```yaml
realm: "corporate"

groups:
  - name: "Employees"
    path: "/Employees"
  - name: "Contractors"
    path: "/Contractors"
  - name: "Engineering"
    path: "/Engineering"
  - name: "Sales"
    path: "/Sales"

users:
  # Full-time employee
  - username: "john.doe"
    email: "john.doe@corporate.com"
    enabled: true
    attributes:
      employeeType: ["FTE"]
      department: ["Engineering"]
    groups:
      - "/Employees"
      - "/Engineering"
  
  # Contractor
  - username: "jane.contractor"
    email: "jane@contractor.com"
    enabled: true
    attributes:
      employeeType: ["Contractor"]
      department: ["Engineering"]
    groups:
      - "/Contractors"
      - "/Engineering"
  
  # Sales employee
  - username: "bob.sales"
    email: "bob@corporate.com"
    enabled: true
    attributes:
      employeeType: ["FTE"]
      department: ["Sales"]
    groups:
      - "/Employees"
      - "/Sales"
```

---

## Common Pitfalls

### 1. Updating User Without Specifying Groups

**Problem:**
```yaml
# Initial creation
users:
  - username: "john.doe"
    email: "john@example.com"
    groups:
      - "/Developers"

# Later update (different file or import)
users:
  - username: "john.doe"
    email: "john.updated@example.com"
    # groups omitted - DANGER!
```

**Result:** john.doe removed from "/Developers"

**Solution:**
```yaml
users:
  - username: "john.doe"
    email: "john.updated@example.com"
    groups:
      - "/Developers"  # ✅ Must include
```

---

### 2. Assuming Additive Behavior

**Misconception:** "Adding groups will append to existing groups"

**Reality:** Groups are replaced completely

**Example:**
```yaml
# Initial
users:
  - username: "john.doe"
    groups:
      - "/Developers"

# Later (expecting to ADD /Architects)
users:
  - username: "john.doe"
    groups:
      - "/Architects"  # ❌ Removes /Developers!
```

**Result:** Only "/Architects", "/Developers" removed

**Solution:** Always specify complete list:
```yaml
users:
  - username: "john.doe"
    groups:
      - "/Developers"   # ✅ Keep existing
      - "/Architects"   # ✅ Add new
```

---

### 3. Inconsistent Configuration Sources

**Problem:** Multiple people/systems updating same users

**Scenario:**
```yaml
# Team A's config
users:
  - username: "john.doe"
    email: "john@example.com"
    groups:
      - "/Developers"

# Team B's config (doesn't know about Team A's groups)
users:
  - username: "john.doe"
    attributes:
      department: ["Engineering"]
    # groups omitted - removes Team A's assignment!
```

**Solution:** Centralized user configuration:
```yaml
# SINGLE source of truth
users:
  - username: "john.doe"
    email: "john@example.com"
    attributes:
      department: ["Engineering"]
    groups:
      - "/Developers"
```

---

### 4. Copy-Paste User Templates

**Problem:** Using incomplete templates

**Bad template:**
```yaml
# Incomplete template
- username: "USERNAME"
  email: "EMAIL"
  enabled: true
  # Missing: groups, realmRoles, attributes, etc.
```

**Good template:**
```yaml
# Complete template
- username: "USERNAME"
  email: "EMAIL"
  firstName: "FIRST"
  lastName: "LAST"
  enabled: true
  emailVerified: false
  groups: []              # ✅ Always present
  realmRoles: []          # ✅ Always present
  clientRoles: {}         # ✅ Always present
  attributes: {}          # ✅ Always present
  credentials: []         # ✅ Always present
  requiredActions: []     # ✅ Always present
```

---

### 5. Not Testing Import Repeatability

**Problem:** Not verifying that imports are idempotent

**Test:**
```bash
# Import once
java -jar keycloak-config-cli.jar \
  --import.files.locations=config.yaml

# Check user groups
curl "http://localhost:8080/admin/realms/myrealm/users/{user-id}/groups" \
  -H "Authorization: Bearer $TOKEN"

# Import again
java -jar keycloak-config-cli.jar \
  --import.files.locations=config.yaml

# Check user groups again - should be identical
curl "http://localhost:8080/admin/realms/myrealm/users/{user-id}/groups" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Best Practices

1. **Always Include Complete User State**
```yaml
   users:
     - username: "john.doe"
       email: "john@example.com"
       firstName: "John"
       lastName: "Doe"
       enabled: true
       groups: ["/Developers"]       # Always present
       realmRoles: ["employee"]      # Always present
       attributes:                   # Always present
         department: ["Engineering"]
```

2. **Use Configuration Templates**
```yaml
   # user-template.yaml
   - username: "REPLACE_USERNAME"
     email: "REPLACE_EMAIL"
     firstName: "REPLACE_FIRST"
     lastName: "REPLACE_LAST"
     enabled: true
     emailVerified: false
     groups: []
     realmRoles: []
     clientRoles: {}
     attributes: {}
```

3. **Validate Before Import**
```bash
   # Check all users have groups field
   yq '.users[] | select(has("groups") | not) | .username' config.yaml
```

4. **Document Expected State**
```yaml
   # users.yaml
   users:
     # Engineering team members
     # Expected groups: /Engineering/[Team]
     # Expected roles: employee
     - username: "john.doe"
       # ...
```

5. **Test Idempotency**
```bash
   # Should be safe to run multiple times
   for i in {1..3}; do
     java -jar keycloak-config-cli.jar \
       --import.files.locations=config.yaml
   done
```

6. **Use Version Control**
```bash
   # Track all changes to user configurations
   git add users.yaml
   git commit -m "Add john.doe to /Architects group"
```

7. **Review Group Changes**
```bash
   # See what changed
   git diff users.yaml | grep -A 5 -B 5 "groups:"
```

8. **Separate Concerns When Needed**
   - users-profiles.yaml (email, names, attributes)
   - user-groups.yaml (group assignments only)
   - user-roles.yaml (role assignments only)

---

## Troubleshooting

### Groups Unexpectedly Removed

**Symptom:** User's group memberships disappear after import

**Diagnosis:**
```bash
# Check import logs
grep "Removing.*from group" keycloak-config-cli.log

# Check user's current groups
curl "http://localhost:8080/admin/realms/myrealm/users/{user-id}/groups" \
  -H "Authorization: Bearer $TOKEN"

# Check configuration
grep -A 10 "username: john.doe" config.yaml | grep "groups:"
```

**Cause:** Groups field omitted or empty in configuration

**Solution:** Add groups back to configuration:
```yaml
users:
  - username: "john.doe"
    email: "john@example.com"
    groups:  # ✅ Add this
      - "/Developers"
      - "/Architects"
```

---

### User Has More Groups Than Expected

**Symptom:** User has groups not in configuration

**Possible causes:**
1. Manual assignment via Admin Console
2. Groups assigned by different config file
3. Previous import with different groups

**Check remote state:**
```bash
# With remote state enabled, manually assigned groups are preserved
# Check which groups are managed by keycloak-config-cli

# In Admin Console:
# Users → select user → Groups tab
# Manually assigned groups shown
```

**Solution:**
- If groups should be there: Add to configuration
- If groups should not be there: Remove manually or explicitly set full list

---

### Cannot Remove User from Group

**Symptom:** User remains in group despite not being in configuration

**Possible cause:** Group was manually assigned, not via keycloak-config-cli

**With remote state enabled:**
- Only groups assigned by keycloak-config-cli are removed
- Manual groups remain

**Solution:**
```bash
# Option 1: Remove manually
# Admin Console → Users → john.doe → Groups → Leave group

# Option 2: Disable remote state (not recommended)
--import.remote-state.enabled=false
```

---

## Configuration Options
```bash
# Enable remote state (default) - tracks managed groups
--import.remote-state.enabled=true

# Validate configuration before import
--import.validate=true

# Enable parallel import
--import.parallel=true
```

---

## Consequences

When managing user groups in keycloak-config-cli:

1. **Omitted Groups Are Removed:** Not specifying `groups` field removes group memberships
2. **Groups Are Replaced, Not Merged:** Specifying groups sets the complete list, removing others
3. **Remote State Provides Partial Protection:** Only managed groups are removed, manual groups preserved
4. **Array Fields Behave Similarly:** roles, requiredActions, etc. also replaced when specified
5. **Complete State Required:** Must always specify full desired state for predictable results
6. **No Additive Operations:** Cannot add/remove individual groups, must specify complete list



