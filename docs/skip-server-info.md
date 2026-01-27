# Skip Server Info

In modern Keycloak versions (e.g. 26.4.0+), the global `/admin/serverinfo` endpoint is restricted to users with administrative roles in the `master` realm.

When authenticating against a non-master realm (using `--keycloak.login-realm`), the authenticated user typically does not have these permissions. This causes the `keycloak-config-cli` to fail by default as it tries to fetch the server version for compatibility checks.

## Usage

To authenticate against a non-master realm, you should enable `keycloak.skip-server-info=true`.

```bash
java -jar ./target/keycloak-config-cli.jar \
    --keycloak.url=http://localhost:8080 \
    --keycloak.login-realm=my-realm \
    --keycloak.user=my-user \
    --keycloak.password=my-password \
    --keycloak.skip-server-info=true \
    --import.files.locations=...
```

## Consequences

When `keycloak.skip-server-info` is enabled:

1.  **Version Fetching is skipped**: `keycloak-config-cli` will not attempt to call `/admin/serverinfo`.
2.  **Version Fallback**: The tool defaults to version `unknown` unless `--keycloak.version` (or `KEYCLOAK_VERSION`) is explicitly provided.
3.  **Alternative Health Check**: An alternative health check (authenticating against the login realm) is performed to verify Keycloak connectivity.
4.  **Implicit Compatibility**: Some version-specific compatibility checks might be skipped or behave differently. It is recommended to provide the explicit version via `--keycloak.version` if you know it.
