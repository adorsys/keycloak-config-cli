# Remote State Management

Remote state management ensures that configurations are tracked and only modified when necessary, enabling safe and controlled updates to Keycloak configurations without altering unmanaged resources.

Ensure your project directory has the following structure for sample demonstration locally:

```plaintext
keycloak-setup/
├── docker-compose.yml
├── import.sh
├── keycloak-config-cli.jar
└── realms/
└── state-management.json
```

Each file serves the following purpose:

- `docker-compose.yml`: Defines the Keycloak service.
- `import.sh`: Custom shell script for running the Keycloak Config CLI against our Keycloak instance.
- `keycloak-config-cli.jar`: Keycloak-config-cli is compatible with different versions of Keycloak and actively maintained.
- `realms/state-management.json`: JSON file with realm configuration.


In `docker-compose.yml`, configure the Keycloak service without a Keycloak Config CLI container, as we will be handling imports manually in this case.

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.4.0
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HOSTNAME: localhost
      KC_HTTP_PORT: "8080"
      KC_HTTP_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
      KC_HEALTH_ENABLED: "true"
    ports:
      - "8080:8080"
    volumes:
      - ./realms:/opt/keycloak/data/import:z
    command:
      - "start-dev"
```

This file configures Keycloak with essential parameters and maps `./realms` for importing configuration files.


The `import.sh` script uses the `keycloak-config-cli.jar` to apply configurations. This script will:

1. Copy the config file to the container.
2. Run the import using the CLI JAR file, with remote state management enabled.


Create `import.sh` as follows:

```bash
#!/bin/bash

# Check if a configuration file is provided
if [ -z "$1" ]; then
  echo "Usage: ./import.sh <config-file>"
  exit 1
fi

CONFIG_FILE=$1

# Run the Keycloak Config CLI tool with the updated options
java -jar keycloak-config-cli.jar \
  --keycloak.url="http://localhost:8080" \
  --keycloak.user="admin" \
  --keycloak.password="admin" \
  --import.managed.group="full" \
  --import.remote-state.enabled="true" \
  --import.files.locations="$CONFIG_FILE"

echo "Import of $CONFIG_FILE with remote-state enabled is complete."
```


Create `state-management.json` under `realms/`, which defines a Keycloak realm, clients, and roles.


Define the realm, clients, roles, and scope mappings for demonstration:

```json
{
  "realm": "master",
  "enabled": true,
  "clients": [
    {
      "clientId": "imported-client",
      "enabled": true,
      "protocol": "openid-connect",
      "fullScopeAllowed": false
    }
  ],
  "roles": {
    "realm": [
      {
        "name": "my-role",
        "description": "A test role"
      }
    ]
  },
  "clientScopes": [
    {
      "name": "custom-scope",
      "description": "Custom client scope",
      "protocol": "openid-connect",
      "attributes": {
        "include.in.token.scope": "true"
      }
    }
  ],
  "scopeMappings": [
    {
      "client": "imported-client",
      "roles": [
        "my-role"
      ]
    }
  ]
}
```

## Known Limitations

> **Source Verification:** The information below is based on [GitHub Issue #1012](https://github.com/adorsys/keycloak-config-cli/issues/1012) and related changelog entries. This is a confirmed, unresolved issue as of the latest versions.

### Issue #1012: Remote State Not Used for clientScopes and scopeMappings

**Related Issue:** [#1012](https://github.com/adorsys/keycloak-config-cli/issues/1012)

**Current Behavior:**  
When `import.remote-state.enabled=true`, remote state tracking **does not work** for `clientScopes` and `scopeMappings` resources. All `clientScopes` and `scopeMappings` not present in the JSON import files are deleted from Keycloak, even if they were created manually.

**Expected Behavior:**  
Only `clientScopes` and `scopeMappings` tracked in remote state and not present in the import should be deleted.

**Affected Versions:** All versions (reported in 5.9.0, still present)

### Affected Resources

**Not Protected by Remote State (Issue #1012):**
- `clientScopes` - Always deleted if not in import config
- `scopeMappings` - Always deleted if not in import config

**Note:** For other resource types (realms, clients, roles, groups, users), refer to the [Managed Resources](./managed-resource.md) documentation for their specific behavior with remote state enabled.

---

## Verifying Remote State Management

With remote state management enabled, Keycloak Config CLI will only modify resources it initially created, preserving custom or manually added configurations.

### Starting Keycloak

To start Keycloak, run:

```shell
docker-compose up -d
```

### Testing Remote State Protection

1. **Import initial configuration:**
   ```bash
   ./import.sh state-management.json
   ```

2. **Manually create a client in Keycloak UI** (e.g., "manual-client")

3. **Re-run the import:**
   ```bash
   ./import.sh state-management.json
   ```

4. **Verify:** "manual-client" still exists (protected by remote state)

### Testing the clientScopes/scopesMappings Limitation

1. **Import configuration with clientScopes:**
   ```bash
   ./import.sh state-management.json
   ```

2. **Manually create a ClientScope in Keycloak UI** (e.g., "manual-scope")

3. **Re-run the import:**
   ```bash
   ./import.sh state-management.json
   ```

4. **Observe:** "manual-scope" is **deleted** (not protected - Issue #1012)

---

## Workarounds for Issue #1012

### Option 1: Exclude clientScopes from Management

Don't define `clientScopes` in your configuration files. Manage them manually through Keycloak UI or Admin API.

### Option 2: Omit clientScopes from Configuration

Don't include `clientScopes` and `scopeMappings` in your configuration files if you need to preserve manually created ones:

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url="http://localhost:8080" \
  --keycloak.user="admin" \
  --keycloak.password="admin" \
  --import.remote-state.enabled="true" \
  --import.files.locations="config-without-scopes.json"
```

**Configuration without clientScopes:**
```json
{
  "realm": "master",
  "enabled": true,
  "clients": [...],
  "roles": {...}
  // Note: clientScopes and scopeMappings omitted
  // These must be managed manually through Keycloak UI or Admin API
}
```

### Option 3: Define All Scopes in Configuration

Include all clientScopes (both CLI-managed and manually created) in your configuration file:

```json
{
  "realm": "master",
  "clientScopes": [
    {
      "name": "cli-managed-scope",
      "description": "Managed by keycloak-config-cli",
      "protocol": "openid-connect"
    },
    {
      "name": "manually-created-scope",
      "description": "Originally manual, now in config",
      "protocol": "openid-connect"
    }
  ],
  "scopeMappings": [
    {
      "client": "my-client",
      "roles": ["my-role"]
    }
  ]
}
```

### Option 4: Two-Phase Import Strategy

1. **Phase 1:** Import everything except clientScopes/scopesMappings
2. **Phase 2:** Manually manage scopes through Keycloak UI or separate automation

---

## Best Practices

### When Using clientScopes

1. **Document Your Scopes:** Maintain a list of all clientScopes needed
2. **Include All Scopes in Config:** Don't mix config-managed and manual scopes
3. **Version Control:** Track all clientScopes in your configuration repository
4. **Regular Audits:** Check if scopes are unexpectedly deleted after imports

### Recommended Configuration Structure

```json
{
  "realm": "my-realm",
  "enabled": true,
  
  // 1. Define realm roles first
  "roles": {
    "realm": [
      { "name": "user" },
      { "name": "admin" }
    ],
    "client": {
      "my-client": [
        { "name": "read", "clientRole": true },
        { "name": "write", "clientRole": true }
      ]
    }
  },
  
  // 2. Define clients
  "clients": [
    {
      "clientId": "my-client",
      "enabled": true,
      "protocol": "openid-connect"
    }
  ],
  
  // 3. Define clientScopes (will replace all existing!)
  "clientScopes": [
    {
      "name": "my-scope",
      "description": "Custom scope",
      "protocol": "openid-connect",
      "attributes": {
        "include.in.token.scope": "true"
      }
    }
  ],
  
  // 4. Define scopeMappings (will replace all existing!)
  "scopeMappings": [
    {
      "clientScope": "my-scope",
      "roles": ["user"]
    },
    {
      "client": "my-client",
      "roles": ["admin"]
    }
  ]
}
```

### Import with Remote State

```bash
java -jar keycloak-config-cli.jar \
  --keycloak.url="http://localhost:8080" \
  --keycloak.user="admin" \
  --keycloak.password="admin" \
  --import.remote-state.enabled="true" \
  --import.files.locations="realm-config.json"
```

---

## Troubleshooting

### Problem: My manually created clientScopes keep disappearing

**Cause:** Issue #1012 - clientScopes are not protected by remote state

**Solutions:**
1. Add the clientScopes to your configuration file
2. Exclude clientScopes from your imports and manage them manually
3. Use Keycloak's default scopes feature instead of custom clientScopes

### Problem: Scope mappings are reset after import

**Cause:** Issue #1012 - scopeMappings are not protected by remote state

**Solutions:**
1. Define all scopeMappings in your configuration
2. Use realm-level role mappings instead of scope mappings where possible

---

## Conclusion

In this guide, we covered the basics of setting up Keycloak with Docker, creating an import script for configuration, and enabling remote state management using `keycloak-config-cli`.

**Important Takeaway:** While remote state protects most resources, be aware that `clientScopes` and `scopeMappings` are **not protected** due to Issue #1012. Plan your configuration strategy accordingly.

Feel free to reach out if you have any questions or need further assistance!