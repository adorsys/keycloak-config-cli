---
title: Helm Chart
description: Deploy keycloak-config-cli using Helm charts
sidebar_position: 2
---

# Helm Chart

The keycloak-config-cli Helm chart deploys the tool as a Kubernetes Job that runs after Keycloak is installed or upgraded. It uses Helm hooks to automatically apply configuration as part of your deployment workflow.

## Overview

The chart creates:
- A **Kubernetes Job** that runs keycloak-config-cli
- **Secrets** for storing sensitive configuration
- **Config secrets** for realm configuration files

The Job is configured with Helm hooks to run:
- `post-install`: After initial Keycloak installation
- `post-upgrade`: After Keycloak upgrades
- `post-rollback`: After rollbacks

## Prerequisites

- Helm 3.x installed
- Kubernetes cluster with Keycloak deployed
- kubectl configured to access your cluster

## Installation

### From Source

The chart is located in the repository at `contrib/charts/keycloak-config-cli/`:

```bash
# Install from local source
helm install keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak \
  --set env.KEYCLOAK_URL=http://keycloak:8080 \
  --set secrets.KEYCLOAK_PASSWORD=admin
```

### With values file

```bash
helm install keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak \
  --values values.yaml
```

### Dry Run

Test the installation without applying:

```bash
helm install keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak \
  --dry-run --debug
```

## Configuration Reference

### Basic Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `fullnameOverride` | Override the full name of resources | `""` |
| `nameOverride` | Override the name of resources | `""` |

### Image Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `image.repository` | Docker image repository | `docker.io/adorsys/keycloak-config-cli` |
| `image.tag` | Docker image tag | `v{{ .Chart.AppVersion }}-11.0.3` |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |
| `image.pullSecrets` | List of image pull secrets | `[]` |

### Job Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `backoffLimit` | Number of retries (value of 1 = 2 tries total) | `1` |
| `annotations` | Job annotations (includes Helm hooks) | See below |

Default annotations:
```yaml
annotations:
  "helm.sh/hook": "post-install,post-upgrade,post-rollback"
  "helm.sh/hook-delete-policy": "hook-succeeded,before-hook-creation"
  "helm.sh/hook-weight": "5"
```

| Parameter | Description | Default |
|-----------|-------------|---------|
| `labels` | Additional labels for the Job | `{}` |
| `resources` | Resource limits and requests | `{}` |
| `podLabels` | Additional pod labels | `{}` |
| `podAnnotations` | Additional pod annotations | `{}` |

### Environment Variables

| Parameter | Description | Default |
|-----------|-------------|---------|
| `env` | Environment variables as key-value pairs | See below |
| `secrets` | Secret environment variables (stored in Secret) | `{}` |

Default environment variables:
```yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin
  IMPORT_PATH: /config/
```

### Secret Management

| Parameter | Description | Default |
|-----------|-------------|---------|
| `secrets` | Secret values to create and inject | `{}` |
| `existingSecret` | Name of existing secret for password | `""` |
| `existingSecretKey` | Key in existing secret for password | `password` |
| `existingSecrets` | List of existing secrets to mount | `[]` |

### Security Context

| Parameter | Description | Default |
|-----------|-------------|---------|
| `securityContext` | Pod security context | `{}` |
| `containerSecurityContext` | Container security context | `{}` |

### Configuration Files

| Parameter | Description | Default |
|-----------|-------------|---------|
| `config` | Realm configuration (inline or file) | `{}` |
| `existingConfigSecret` | Name of existing secret with config files | `""` |

### Volumes

| Parameter | Description | Default |
|-----------|-------------|---------|
| `extraVolumes` | Additional volume definitions | `""` |
| `extraVolumeMounts` | Additional volume mounts | `""` |

### Service Account

| Parameter | Description | Default |
|-----------|-------------|---------|
| `serviceAccount` | Service account name to use | Not set by default |

## Usage Examples

### Basic Installation

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

secrets:
  KEYCLOAK_PASSWORD: my-admin-password

config:
  my-realm:
    inline:
      realm: my-realm
      enabled: true
      clients:
        - clientId: my-client
          secret: my-client-secret
          redirectUris:
            - "https://myapp.example.com/*"
```

```bash
helm install keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak \
  --values values.yaml
```

### Using Existing Secrets

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

# Reference existing secret for password
existingSecret: keycloak-admin-credentials
existingSecretKey: password

# Additional existing secrets
existingSecrets:
  - name: keycloak-client-secrets
    key: client-secret
    envVar: CLIENT_SECRET

config:
  my-realm:
    inline:
      realm: my-realm
      clients:
        - clientId: my-client
          secret: $(CLIENT_SECRET)
```

### Using External Configuration Files

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

secrets:
  KEYCLOAK_PASSWORD: admin

# Reference existing secret with config files
existingConfigSecret: keycloak-realm-configs
```

### Loading Configuration from File

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

secrets:
  KEYCLOAK_PASSWORD: admin

config:
  my-realm:
    file: path/to/realm.json  # Relative to chart or values file
```

### With Resource Limits

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

secrets:
  KEYCLOAK_PASSWORD: admin

resources:
  limits:
    cpu: "500m"
    memory: "512Mi"
  requests:
    cpu: "250m"
    memory: "256Mi"

config:
  my-realm:
    inline:
      realm: my-realm
```

### With Variable Substitution

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin
  IMPORT_VAR_SUBSTITUTION_ENABLED: "true"
  REALM_NAME: production
  DOMAIN: example.com

secrets:
  KEYCLOAK_PASSWORD: admin

config:
  my-realm:
    inline:
      realm: $(REALM_NAME)
      clients:
        - clientId: my-app
          redirectUris:
            - "https://$(DOMAIN)/*"
```

### With Service Account

```yaml
# values.yaml
serviceAccount: my-service-account

env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

secrets:
  KEYCLOAK_PASSWORD: admin
```

### With Extra Volumes

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

secrets:
  KEYCLOAK_PASSWORD: admin

extraVolumes: |
  - name: custom-ca
    configMap:
      name: custom-ca-bundle

extraVolumeMounts: |
  - name: custom-ca
    mountPath: /etc/ssl/certs/custom
    readOnly: true
```

### With Security Context

```yaml
# values.yaml
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin

secrets:
  KEYCLOAK_PASSWORD: admin

securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000

containerSecurityContext:
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop:
      - ALL
```

## Environment-Specific Deployments

### Development

```yaml
# values-dev.yaml
env:
  KEYCLOAK_URL: http://keycloak-dev:8080
  KEYCLOAK_USER: admin
  IMPORT_VAR_SUBSTITUTION_ENABLED: "true"
  LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG: DEBUG

secrets:
  KEYCLOAK_PASSWORD: admin

config:
  dev-realm:
    inline:
      realm: development
      enabled: true
```

### Production

```yaml
# values-prod.yaml
env:
  KEYCLOAK_URL: https://keycloak.company.com
  KEYCLOAK_USER: admin
  IMPORT_VAR_SUBSTITUTION_ENABLED: "true"
  IMPORT_SSL_REQUIRED: all
  LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG: WARN

# Use existing secret for password
existingSecret: keycloak-admin-secret
existingSecretKey: password

# Use existing secret for config
existingConfigSecret: keycloak-realm-configs

resources:
  limits:
    cpu: "500m"
    memory: "512Mi"
  requests:
    cpu: "250m"
    memory: "256Mi"
```

Deploy with environment-specific values:

```bash
# Development
helm upgrade --install keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak-dev \
  --values values-dev.yaml

# Production
helm upgrade --install keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak-prod \
  --values values-prod.yaml
```

## Helm Operations

### Upgrade

```bash
helm upgrade keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak \
  --values values.yaml
```

### Uninstall

```bash
helm uninstall keycloak-config-cli --namespace keycloak
```

### Template (Preview)

Generate templates without installing:

```bash
helm template keycloak-config-cli ./contrib/charts/keycloak-config-cli \
  --namespace keycloak \
  --values values.yaml
```

## Troubleshooting

### Check Job Status

```bash
# Get job status
kubectl get jobs -n keycloak

# Get job details
kubectl describe job keycloak-config-cli -n keycloak

# Get job pods
kubectl get pods -n keycloak -l app.kubernetes.io/name=keycloak-config-cli
```

### View Logs

```bash
# Get the job pod
POD=$(kubectl get pods -n keycloak -l app.kubernetes.io/name=keycloak-config-cli -o jsonpath='{.items[0].metadata.name}')

# View logs
kubectl logs $POD -n keycloak
```

### Debug Failed Job

```bash
# Check job events
kubectl describe job keycloak-config-cli -n keycloak

# Check pod events
kubectl describe pod $POD -n keycloak

# Check pod logs
kubectl logs $POD -n keycloak
```

### Common Issues

1. **Job fails to connect to Keycloak**
   - Verify `KEYCLOAK_URL` is correct and reachable from the cluster
   - Check if Keycloak is running: `kubectl get pods -n keycloak`
   - Check network policies if any

2. **Authentication failed**
   - Verify `KEYCLOAK_USER` and `KEYCLOAK_PASSWORD` are correct
   - Check if the secret was created: `kubectl get secrets -n keycloak`

3. **Config files not found**
   - Verify config is defined in `values.yaml`
   - Check if the config secret was created: `kubectl get secrets -n keycloak`

4. **Job not running**
   - Check if Helm hooks are being processed
   - Verify the namespace exists

### Increase Retry Count

If the job fails due to transient issues, increase `backoffLimit`:

```yaml
# values.yaml
backoffLimit: 3  # 4 tries total
```

## Best Practices

1. **Use existing secrets** for sensitive data in production
2. **Pin image tags** to specific versions rather than `latest`
3. **Set resource limits** to prevent resource starvation
4. **Use separate namespaces** for different environments
5. **Enable variable substitution** for environment-specific configuration
6. **Test with dry-run** before deploying to production
7. **Keep configuration files in version control**

## Complete Values Example

```yaml
# values.yaml - Complete example

# Override resource names
fullnameOverride: ""
nameOverride: ""

# Image configuration
image:
  repository: docker.io/adorsys/keycloak-config-cli
  tag: "v6.5.1-11.0.3"
  pullPolicy: IfNotPresent
  pullSecrets: []

# Job configuration
backoffLimit: 1

# Additional labels
labels: {}

# Resource limits
resources:
  limits:
    cpu: "500m"
    memory: "512Mi"
  requests:
    cpu: "250m"
    memory: "256Mi"

# Environment variables
env:
  KEYCLOAK_URL: http://keycloak:8080
  KEYCLOAK_USER: admin
  IMPORT_PATH: /config/
  IMPORT_VAR_SUBSTITUTION_ENABLED: "true"

# Secrets to create
secrets:
  KEYCLOAK_PASSWORD: my-admin-password

# Or use existing secret
# existingSecret: keycloak-admin-credentials
# existingSecretKey: password

# Additional existing secrets
existingSecrets: []

# Security contexts
securityContext: {}
containerSecurityContext: {}

# Pod labels and annotations
podLabels: {}
podAnnotations: {}

# Realm configuration
config:
  my-realm:
    inline:
      realm: my-realm
      enabled: true
      clients:
        - clientId: my-app
          name: My Application
          secret: my-client-secret
          redirectUris:
            - "https://myapp.example.com/*"
          webOrigins:
            - "https://myapp.example.com"
      users:
        - username: admin-user
          enabled: true
          credentials:
            - type: password
              value: user-password

# Or use existing config secret
# existingConfigSecret: keycloak-realm-configs

# Extra volumes
extraVolumes: ""
extraVolumeMounts: ""

# Service account (optional)
# serviceAccount: my-service-account
```

## Next Steps

- [Docker Usage](../docker-helm/overview.md) - Docker and Docker Compose usage
- [Kubernetes Deployment](../docker-helm/kubernetes-deployment.md) - Advanced Kubernetes configurations
- [Variable Substitution](../variable-substitution/overview.md) - Dynamic configuration
