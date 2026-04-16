# Import Settings

This section covers the various import settings and configuration options available in keycloak-config-cli.

## Overview

keycloak-config-cli provides extensive configuration options to control how imports are processed, what gets imported, and how conflicts are resolved.

## Core Import Settings

### Basic Import Configuration

```bash
java -jar keycloak-config-cli.jar \
  --import.files=path/to/config.json \
  --import.realm=my-realm
```

### Environment Variables

| Variable | Description | Default | Example |
|-----------|-------------|---------|---------|
| `KEYCLOAK_URL` | Keycloak server URL | `http://localhost:8080/auth` | `KEYCLOAK_URL=https://keycloak.example.com/auth` |
| `KEYCLOAK_USER` | Admin username | `admin` | `KEYCLOAK_USER=import-user` |
| `KEYCLOAK_PASSWORD` | Admin password | `admin` | `KEYCLOAK_PASSWORD=secret123` |
| `KEYCLOAK_CLIENT_ID` | Client ID | `admin-cli` | `KEYCLOAK_CLIENT_ID=my-client` |
| `KEYCLOAK_CLIENT_SECRET` | Client secret | `admin-cli-secret` | `KEYCLOAK_CLIENT_SECRET=my-secret` |
| `KEYCLOAK_REALM` | Target realm | `master` | `KEYCLOAK_REALM=my-realm` |

## Import Behavior Settings

### Conflict Resolution

```bash
java -jar keycloak-config-cli.jar \
  --import.conflict-strategy=SKIP \
  --import.remove-default-roles=true \
  --import.sync-user-federation-attributes=true
```

| Setting | Description | Default | Options |
|----------|-------------|---------|--------|
| `--import.conflict-strategy` | How to handle conflicts | `SKIP` | `SKIP`, `OVERWRITE`, `MERGE` |
| `--import.remove-default-roles` | Remove default roles on import | `false` | `true`, `false` |
| `--import.sync-user-federation-attributes` | Sync federation attributes | `false` | `true`, `false` |

### State Management

```bash
java -jar keycloak-config-cli.jar \
  --import.state-file=state.json \
  --import.state-enabled=true
```

| Setting | Description | Default | Example |
|----------|-------------|---------|---------|
| `--import.state-file` | File to store import state | `null` | `state.json` |
| `--import.state-enabled` | Enable state tracking | `false` | `true`, `false` |

## Advanced Settings

### Cache Control

```bash
java -jar keycloak-config-cli.jar \
  --import.cache.enabled=false \
  --import.cache.ttl=3600
```

| Setting | Description | Default | Example |
|----------|-------------|---------|---------|
| `--import.cache.enabled` | Enable import caching | `true` | `true`, `false` |
| `--import.cache.ttl` | Cache time-to-live in seconds | `300` | `3600` |

### Parallel Processing

```bash
java -jar keycloak-config-cli.jar \
  --import.parallel.enabled=true \
  --import.parallel.threads=4
```

| Setting | Description | Default | Example |
|----------|-------------|---------|---------|
| `--import.parallel.enabled` | Enable parallel processing | `false` | `true`, `false` |
| `--import.parallel.threads` | Number of parallel threads | `2` | `4`, `8` |

### Validation Settings

```bash
java -jar keycloak-config-cli.jar \
  --import.validate=true \
  --import.validate.strict=false
```

| Setting | Description | Default | Example |
|----------|-------------|---------|---------|
| `--import.validate` | Enable validation | `true` | `true`, `false` |
| `--import.validate.strict` | Strict validation mode | `true` | `true`, `false` |

## Logging Settings

### Import Logging

```bash
java -jar keycloak-config-cli.jar \
  --import.logging.level=INFO \
  --import.logging.format=json
```

| Setting | Description | Default | Example |
|----------|-------------|---------|---------|
| `--import.logging.level` | Import log level | `INFO` | `DEBUG`, `INFO`, `WARN`, `ERROR` |
| `--import.logging.format` | Log format | `TEXT` | `JSON`, `TEXT` |
| `--import.logging.file` | Log file path | `null` | `/tmp/import.log` |

### Performance Settings

```bash
java -jar keycloak-config-cli.jar \
  --import.batch-size=100 \
  --import.throttle.delay=100
```

| Setting | Description | Default | Example |
|----------|-------------|---------|---------|
| `--import.batch-size` | Batch processing size | `50` | `100`, `200` |
| `--import.throttle.delay` | Throttle delay in ms | `0` | `100`, `500` |

## Security Settings

### SSL/TLS Configuration

```bash
java -jar keycloak-config-cli.jar \
  --import.ssl.truststore=truststore.jks \
  --import.ssl.truststore-password=changeit \
  --import.ssl.skip-hostname-verification=true
```

| Setting | Description | Default | Example |
|----------|-------------|---------|---------|
| `--import.ssl.truststore` | Truststore file | `null` | `truststore.jks` |
| `--import.ssl.truststore.password` | Truststore password | `null` | `changeit` |
| `--import.ssl.skip-hostname-verification` | Skip hostname verification | `false` | `true`, `false` |

## Examples

### Basic Import

```bash
java -jar keycloak-config-cli.jar \
  --import.files=config.json \
  --import.realm=production
```

### Advanced Import with Conflict Resolution

```bash
java -jar keycloak-config-cli.jar \
  --import.files=config.json \
  --import.conflict-strategy=MERGE \
  --import.remove-default-roles=true
```

### Import with State Management

```bash
java -jar keycloak-config-cli.jar \
  --import.files=config.json \
  --import.state-file=state.json \
  --import.state-enabled=true
```

### Production Import with Logging

```bash
java -jar keycloak-config-cli.jar \
  --import.files=config.json \
  --import.logging.level=WARN \
  --import.logging.format=json \
  --import.logging.file=/var/log/keycloak-import.log
```

## Best Practices

1. **Use Environment Variables**: Store sensitive data in environment variables
2. **Enable State Management**: Use state files for tracking changes
3. **Configure Logging**: Set appropriate log levels for production
4. **Test Conflicts**: Use `--import.validate=true` to test conflict resolution
5. **Batch Processing**: Adjust batch size based on system resources
6. **Security**: Use SSL/TLS settings for secure connections

## Related Topics

- [Configuration](../config/overview.md) - General configuration options
- [Variable Substitution](../variable-substitution/overview.md) - Dynamic configuration
- [State Management](../config/remote-state-management.md) - Advanced state tracking
- [Conflict Resolution](../config/partial-imports.md#2-conflicting-imports-from-multiple-files) - Handling import conflicts
