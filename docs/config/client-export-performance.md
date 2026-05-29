# Client Export Performance with Many Clients

When running `keycloak-config-cli` against a Keycloak realm containing thousands of clients (e.g., dynamically generated or self-registered clients), the configuration update process can become extremely slow (taking 5+ minutes) or fail entirely due to timeouts. Understanding why this happens and how to structure your imports is essential for maintaining fast and reliable deployments.

Related issues: [#1104](https://github.com/adorsys/keycloak-config-cli/issues/1104), [#1178](https://github.com/adorsys/keycloak-config-cli/issues/1178)  
Upstream Keycloak issue: [keycloak/keycloak#35233](https://github.com/keycloak/keycloak/issues/35233)

---

## The Problem

Users managing large-scale Keycloak environments with thousands of clients often encounter the following:
- Keycloak configuration updates take a long time to start or complete.
- Keycloak servers experience high CPU/memory usage or database bottlenecks during imports.
- The import process fails with database transaction timeouts (`TransactionTimeoutException`).
- Large-scale environments with dynamically registered clients are difficult to manage with a single configuration file.

---

## Why It Happens

At the start of the configuration import process, `keycloak-config-cli` needs to understand the existing state of the realm. Specifically:

1. **Scope Mapping Inspection**: The `ScopeMappingImportService` needs to inspect existing client scopes and scope mappings to determine what updates or deletions are necessary.
2. **Keycloak Partial Export**: To gather this information, `keycloak-config-cli` requests a partial export from Keycloak that includes clients and their configurations.
3. **Keycloak-Side Bottleneck**: On the Keycloak server, exporting thousands of clients (along with their roles, protocols, and mappers) requires performing heavy database queries. Keycloak's export API is not optimized for high-volume client setups, leading to transaction timeouts or extreme latency.

---

## Workarounds & Solutions

Depending on your use case, there are several ways to address or bypass this performance bottleneck:

### 1. Skip Client Export (Exclude Client Configurations)

If you are only updating realm settings, authentication flows, roles, or users, and do **not** need to manage clients via `keycloak-config-cli`:

* **Action**: Ensure your configuration files do **not** contain any client-related declarations (such as the `clients` or `clientScopes` blocks).
* **Behavior**: When `keycloak-config-cli` detects that no client configuration is specified in the input file, it automatically skips the client export phase. This bypasses the Keycloak database query and speeds up the import significantly.

#### Example: Realm Settings Only (Fast Import)
```yaml
realm: "myrealm"
enabled: true
displayName: "My Fast Realm"
# No clients: [] or clientScopes: [] defined here
```

---

### 2. Segment Client Configurations

If you only want to manage a small subset of clients (e.g., 40-50 managed infrastructure clients) while ignoring thousands of dynamically generated clients:

* **Action**: Define only the managed clients in your configuration files and rely on `keycloak-config-cli`'s partial import capability.
* **Caution**: Because `keycloak-config-cli` will still need to export all clients during the import of a client configuration block, this will still trigger the slow export process. To minimize this, combine it with Keycloak-side performance tuning (see below) or separate your client configuration updates into less frequent, dedicated maintenance windows.

---

### 3. Keycloak Server Tuning

If you must manage client configurations and cannot avoid the export, you can tune your Keycloak and Quarkus settings to prevent transaction timeouts:

#### Extend Transaction Timeouts
Increase the default transaction timeout settings in Quarkus/Keycloak to allow the export process more time to complete.

* Set the environment variable:
  ```bash
  QUARKUS_TRANSACTION_MANAGER_DEFAULT_TRANSACTION_TIMEOUT=300
  ```
  *(or set in `conf/keycloak.conf`)*
  ```properties
  quarkus.transaction-manager.default-transaction-timeout=300
  ```

#### Database Optimization
- Ensure your Keycloak database has adequate CPU and RAM allocated.
- Analyze database query logs and ensure indexes are properly set, especially on client-related tables (e.g., `client`, `client_attributes`, `scope_mapping`).

---

### 4. Cleanup of Dynamic/Self-Registered Clients

If your Keycloak realm accumulates thousands of dynamic clients (e.g., from developer sandboxes or old sessions), implement an automated cleanup job:
* Query the Keycloak REST API periodically to identify and delete expired or inactive clients.
* Keeping the total client count low prevents performance issues from developing in the first place.

---

## Best Practices

1. **Separate Client Configs from Realm Configs**: Keep your main realm configuration files free of `clients` blocks. Update realm settings and client settings using separate `keycloak-config-cli` runs.
2. **Run Client Updates Separately**: Only run configuration files containing the `clients` section when you actually need to apply changes to clients.
3. **Use Remote State**: Ensure `import.remote-state.enabled=true` (default) is active, so only the explicitly managed clients are tracked and modified.
4. **Monitor Keycloak Resource Usage**: During config imports on large realms, monitor database connections and Keycloak memory/CPU limits to prevent outages.
