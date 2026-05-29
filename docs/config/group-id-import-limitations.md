# Group ID Import Limitations

When importing realm configurations that contain groups with predefined IDs, keycloak-config-cli may encounter API limitations that prevent successful imports.

Related issues: [#824](https://github.com/adorsys/keycloak-config-cli/issues/824)

## The Problem

Users often encounter this error when importing exported realm files:

```
Create method returned status Not Found (Code: 404); expected status: Created (201).
```

This occurs because:
- Keycloak's REST API doesn't allow creating groups with predefined IDs
- When groups contain an `id` field, the API attempts to create a group with that specific ID
- If the ID already exists or is invalid, the API returns 404 Not Found
- This is a Keycloak API limitation, not specific to keycloak-config-cli

## When This Happens

Common scenarios:
- **Exported realm imports**: When exporting a realm from Keycloak and re-importing it
- **Migration scenarios**: Moving configurations between environments
- **Backup restoration**: Attempting to restore from backup files
- **CI/CD pipelines**: Automated deployment with existing group configurations

## Workarounds

### Option 1: Remove ID Fields (Recommended)

Remove the `id` field from all group definitions in your import file:

```yaml
groups:
  - id: "123e4567-e89b-12d3-a456-426614174000"
    name: "developers"
    path: "/developers"
  - id: "456e7890-e89b-12d3-a456-426614174001"
    name: "admins"
    path: "/admins"

groups:
  - name: "developers"
    path: "/developers"
  - name: "admins"
    path: "/admins"
```

**Benefits:**
- Keycloak generates new IDs automatically
- Import succeeds without conflicts
- Groups are created with correct structure

### Option 2: Use Group Names for References

If you need to reference groups in other parts of your configuration, use names instead of IDs:

```yaml
users:
  - username: "john.doe"
    enabled: true
    groups:
      - "developers"
      - "admins"
```

### Option 3: Retrieve IDs After Import

If you need the group IDs for API operations, retrieve them after successful import:

#### Using curl and jq

```bash
#!/bin/bash

get_access_token() {
  echo "Requesting access token..."
  TOKEN=$(curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=admin" \
    -d "password=admin" | jq -r '.access_token')
  
  if [ -z "$TOKEN" ]; then
    echo "Failed to get access token"
    exit 1
  fi
}

retrieve_groups() {
  echo "Retrieving groups from realm..."
  GROUPS=$(curl -s -X GET "http://localhost:8080/admin/realms/your-realm/groups" \
    -H "Authorization: Bearer $TOKEN")

  if echo "$GROUPS" | jq . >/dev/null 2>&1; then
    echo "Groups retrieved successfully."
    echo "Group Name and ID mapping:"
    echo "$GROUPS" | jq -r '.[] | "\(.name): \(.id)"'
  else
    echo "Failed to retrieve groups. Please check your Keycloak URL and realm name."
    echo "Response: $GROUPS"
    exit 1
  fi
}

get_access_token
retrieve_groups
```

### Option 4: Two-Phase Import

For complex scenarios, use a two-phase approach:

1. **Phase 1**: Import groups without IDs
2. **Phase 2**: Update group properties using retrieved IDs

```yaml
groups:
  - name: "developers"
    attributes:
      department: "engineering"
      team: "backend"
  - name: "admins"
    attributes:
      department: "operations"
      team: "security"
```

After successful import, retrieve the IDs and use them for subsequent operations.

## Best Practices

1. **Avoid IDs in Exports**: When exporting realms for re-import, remove group IDs
2. **Use Name-Based References**: Reference groups by name in user assignments and role mappings
3. **Document ID Mapping**: Keep a record of group names to their auto-generated IDs
4. **Test Imports**: Always test imports in non-production environments first
5. **Backup Strategy**: Maintain backups with and without IDs

## Environment Considerations

### Development/Testing
- Remove IDs from group configurations
- Use the two-phase approach for complex setups
- Test thoroughly before production deployment

### Production
- Plan migrations carefully to avoid group ID conflicts
- Use name-based references for reliability
- Maintain documentation of group ID mappings

### CI/CD Pipelines
- Pre-process configuration files to remove group IDs
- Include validation steps to ensure ID-free configurations
- Use API calls to retrieve and store group mappings if needed

## Troubleshooting

### Common Error Messages

**404 Not Found on Group Creation**
- **Cause**: Group ID already exists or is invalid
- **Solution**: Remove ID field from configuration

**Group Reference Errors**
- **Cause**: Using IDs to reference groups in user assignments
- **Solution**: Use group names instead

**Import Partial Failures**
- **Cause**: Mixed configuration with some groups having IDs
- **Solution**: Remove all ID fields before import

### Verification Steps

1. **Check Configuration**: Ensure no group objects contain `id` fields
2. **Verify Groups**: Check Keycloak Admin Console for created groups
3. **Retrieve IDs**: Use API to get new group IDs if needed

## Additional Resources

- [Keycloak Admin API Documentation - Groups](https://www.keycloak.org/docs-api/latest/rest-api/index.html#_groups)
- [Managed Resources](managed-resource.md)
