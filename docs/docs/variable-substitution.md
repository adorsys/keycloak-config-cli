# Variable Substitution

keycloak-config-cli supports variable substitution in configuration files. This allows you to use the same config files across different environments.

## Enabling Variable Substitution

Set `import.var-substitution.enabled=true` (disabled by default).

## Supported Variable Types

### Environment Variables
```yaml
realm: ${env:APP_ENV}-realm
```

### File Content
```yaml
secret: ${file:UTF-8:${env:SECRET_FILE}}
```

### Base64
```yaml
credentials: ${base64Decoder:SGVsbG9Xb3JsZCE=}
```

### JavaScript Evaluation
```yaml
sessionTimeout: ${javascript: 2 * 60 * 60}
```

### System Properties
```yaml
path: ${sys:user.dir}
```

### URL Content
```yaml
config: ${url:UTF-8:https://example.com/config.json}
```

## Recursive Substitution

Variables can reference other variables:
```yaml
secret: ${file:UTF-8:${env:SECRET_FILE}}
```

## Best Practices

- Use environment variables for environment-specific values
- Store secrets in files mounted as Docker/K8s secrets
- Use JavaScript for dynamic calculations
