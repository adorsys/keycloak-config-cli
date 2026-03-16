# Configuration

Learn how to configure keycloak-config-cli for your use case.

## Import Settings

Control how keycloak-config-cli manages resources:

| Setting | Default | Description |
|---------|---------|-------------|
| `import.manage-realm` | `true` | Manage realm level settings |
| `import.manage-clients` | `true` | Manage clients |
| `import.manage-roles` | `true` | Manage roles |
| `import.manage-users` | `false` | Manage users (use with caution) |

## Remote State

Track which resources are managed by keycloak-config-cli:

```yaml
import.remote-state.enabled: true
import.remote-state.encryption-key: ${env:STATE_ENCRYPTION_KEY}
```

## Parallel Import

Speed up large configurations:

```yaml
import.parallel.enabled: true
import.parallel.threads: 10
```

## Checksum Caching

Skip unchanged files on re-runs:

```yaml
import.checksum.enabled: true
```

## Logging

Configure logging levels:

| Setting | Default | Description |
|---------|---------|-------------|
| `logging.level.root` | `info` | Root log level |
| `logging.level.http` | `info` | HTTP request logging |

## See Also

- [Variable Substitution](variable-substitution.md)
- [Docker & Helm](docker-helm.md)
