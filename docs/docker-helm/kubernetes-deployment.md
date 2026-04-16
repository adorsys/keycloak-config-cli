---
title: Kubernetes Deployment
description: Advanced Kubernetes deployment configurations for keycloak-config-cli
sidebar_position: 3
---

# Kubernetes Deployment

Deploy keycloak-config-cli on Kubernetes with advanced configurations for production environments.

## Prerequisites

- Kubernetes cluster (v1.20+)
- kubectl configured
- Appropriate RBAC permissions

## Basic Kubernetes Deployment

### Deployment Manifest

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config
  labels:
    app: keycloak-config-cli
    version: v1
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak-config-cli
  template:
    metadata:
      labels:
        app: keycloak-config-cli
        version: v1
    spec:
      containers:
        - name: keycloak-config-cli
          image: adorsys/keycloak-config-cli:latest
          imagePullPolicy: IfNotPresent
          env:
            - name: KEYCLOAK_URL
              value: "https://keycloak.company.com"
            - name: KEYCLOAK_USER
              valueFrom:
                secretKeyRef:
                  name: keycloak-credentials
                  key: username
            - name: KEYCLOAK_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: keycloak-credentials
                  key: password
            - name: REALM_NAME
              value: "production-realm"
            - name: IMPORT_VAR_SUBSTITUTION_ENABLED
              value: "true"
          volumeMounts:
            - name: config-volume
              mountPath: /config
              readOnly: true
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
      volumes:
        - name: config-volume
          configMap:
            name: keycloak-config
      restartPolicy: Always
```

### ConfigMap for Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: keycloak-config
  namespace: keycloak-config
data:
  realm.json: |
    {
      "realm": "${REALM_NAME}",
      "enabled": true,
      "displayName": "Production Realm",
      "users": [
        {
          "username": "${ADMIN_USER}",
          "enabled": true,
          "credentials": [
            {
              "type": "password",
              "value": "${ADMIN_PASSWORD}",
              "temporary": false
            }
          ]
        }
      ]
    }
  clients.json: |
    {
      "clients": [
        {
          "clientId": "${CLIENT_ID}",
          "name": "Application Client",
          "enabled": true,
          "publicClient": true,
          "redirectUris": ["${REDIRECT_URI}"]
        }
      ]
    }
```

### Secret for Credentials

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: keycloak-credentials
  namespace: keycloak-config
type: Opaque
data:
  username: <base64-encoded-username>
  password: <base64-encoded-password>
```

## Advanced Configurations

### Job-Based Deployment

Run as a Kubernetes Job for one-time configuration:

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: keycloak-config-job
  namespace: keycloak-config
spec:
  backoffLimit: 3
  completions: 1
  parallelism: 1
  template:
    spec:
      containers:
        - name: keycloak-config-cli
          image: adorsys/keycloak-config-cli:latest
          env:
            - name: KEYCLOAK_URL
              value: "https://keycloak.company.com"
            - name: KEYCLOAK_USER
              valueFrom:
                secretKeyRef:
                  name: keycloak-credentials
                  key: username
            - name: KEYCLOAK_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: keycloak-credentials
                  key: password
          volumeMounts:
            - name: config-volume
              mountPath: /config
          command: ["java", "-jar", "/app/keycloak-config-cli.jar"]
          args: ["--import.files=realm.json,clients.json"]
      volumes:
        - name: config-volume
          configMap:
            name: keycloak-config
      restartPolicy: Never
```

### CronJob for Scheduled Updates

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: keycloak-config-cron
  namespace: keycloak-config
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: keycloak-config-cli
              image: adorsys/keycloak-config-cli:latest
              env:
                - name: KEYCLOAK_URL
                  value: "https://keycloak.company.com"
                - name: KEYCLOAK_USER
                  valueFrom:
                    secretKeyRef:
                      name: keycloak-credentials
                      key: username
                - name: KEYCLOAK_PASSWORD
                  valueFrom:
                    secretKeyRef:
                      name: keycloak-credentials
                      key: password
              volumeMounts:
                - name: config-volume
                  mountPath: /config
              command: ["java", "-jar", "/app/keycloak-config-cli.jar"]
              args: ["--import.files=realm.json"]
          volumes:
            - name: config-volume
              configMap:
                name: keycloak-config
          restartPolicy: Never
  successfulJobsHistoryLimit: 3
  failedJobsHistoryLimit: 1
```

## Multi-Environment Setup

### Namespace Configuration

```yaml
# development-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: keycloak-config-dev
  labels:
    environment: development
---
# production-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: keycloak-config-prod
  labels:
    environment: production
```

### Environment-Specific Deployment

```yaml
# keycloak-config-dev.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config-dev
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak-config-cli-dev
  template:
    metadata:
      labels:
        app: keycloak-config-cli-dev
    spec:
      containers:
        - name: keycloak-config-cli
          image: adorsys/keycloak-config-cli:latest
          env:
            - name: KEYCLOAK_URL
              value: "http://keycloak-dev:8080"
            - name: KEYCLOAK_USER
              value: "admin"
            - name: KEYCLOAK_PASSWORD
              value: "admin"
            - name: REALM_NAME
              value: "development"
            - name: LOGGING_LEVEL_DE_ADORSYS_KEYCLOAK_CONFIG
              value: "DEBUG"
            - name: IMPORT_DRY_RUN
              value: "true"
          volumeMounts:
            - name: config-volume
              mountPath: /config
          resources:
            requests:
              memory: "128Mi"
              cpu: "100m"
            limits:
              memory: "256Mi"
              cpu: "200m"
      volumes:
        - name: config-volume
          configMap:
            name: keycloak-config-dev
```

## Security Configuration

### Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: keycloak-config-netpol
  namespace: keycloak-config
spec:
  podSelector:
    matchLabels:
      app: keycloak-config-cli
  policyTypes:
    - Egress
  egress:
    - to:
        - namespaceSelector:
            matchLabels:
              name: keycloak
      ports:
        - protocol: TCP
          port: 8080
    - to: []
      ports:
        - protocol: TCP
          port: 443
        - protocol: TCP
          port: 53
        - protocol: UDP
          port: 53
```

### Pod Security Policies

```yaml
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: keycloak-config-psp
spec:
  privileged: false
  allowPrivilegeEscalation: false
  requiredDropCapabilities:
    - ALL
  volumes:
    - 'configMap'
    - 'emptyDir'
    - 'projected'
    - 'secret'
    - 'downwardAPI'
    - 'persistentVolumeClaim'
  runAsUser:
    rule: 'MustRunAsNonRoot'
  seLinux:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

### RBAC Configuration

```yaml
# Service Account
apiVersion: v1
kind: ServiceAccount
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789012:role/keycloak-config-cli
---
# Role
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config
rules:
  - apiGroups: [""]
    resources: ["configmaps", "secrets"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["pods"]
    verbs: ["get", "list", "watch"]
---
# Role Binding
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config
subjects:
  - kind: ServiceAccount
    name: keycloak-config-cli
    namespace: keycloak-config
roleRef:
  kind: Role
  name: keycloak-config-cli
  apiGroup: rbac.authorization.k8s.io
```

## Monitoring and Logging

### Service Monitor for Prometheus

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config
  labels:
    app: keycloak-config-cli
    release: prometheus
spec:
  selector:
    matchLabels:
      app: keycloak-config-cli
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
      scrapeTimeout: 10s
```

### Logging Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: keycloak-config-logging
  namespace: keycloak-config
data:
  logback-spring.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/app/logs/keycloak-config.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/app/logs/keycloak-config.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
            <appender-ref ref="FILE"/>
        </root>
    </configuration>
```

## Autoscaling

### Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: keycloak-config-cli-hpa
  namespace: keycloak-config
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: keycloak-config-cli
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 80
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
      selectPolicy: Max
```

## Deployment Strategies

### Rolling Update

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: keycloak-config-cli
  template:
    metadata:
      labels:
        app: keycloak-config-cli
    spec:
      containers:
        - name: keycloak-config-cli
          image: adorsys/keycloak-config-cli:latest
          # ... rest of configuration
```

### Canary Deployment

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: keycloak-config-cli
  namespace: keycloak-config
spec:
  replicas: 3
  strategy:
    canary:
      steps:
        - setWeight: 20
        - pause: {}
        - setWeight: 40
        - pause: {}
        - setWeight: 60
        - pause: {}
        - setWeight: 80
        - pause: {}
      canaryService: keycloak-config-cli-canary
      stableService: keycloak-config-cli
  selector:
    matchLabels:
      app: keycloak-config-cli
  template:
    metadata:
      labels:
        app: keycloak-config-cli
    spec:
      containers:
        - name: keycloak-config-cli
          image: adorsys/keycloak-config-cli:latest
          # ... rest of configuration
```

## Troubleshooting

### Common Issues

1. **Pod Pending**: Check resource requests and limits
   ```bash
   kubectl describe pod -l app=keycloak-config-cli
   ```

2. **Image Pull Errors**: Verify image repository access
   ```bash
   kubectl get events --namespace keycloak-config
   ```

3. **Permission Issues**: Check RBAC configuration
   ```bash
   kubectl auth can-i create configmap --namespace keycloak-config
   ```

### Debug Commands

```bash
# Check pod status
kubectl get pods --namespace keycloak-config

# View pod logs
kubectl logs -f deployment/keycloak-config-cli --namespace keycloak-config

# Describe pod
kubectl describe pod -l app=keycloak-config-cli --namespace keycloak-config

# Check events
kubectl get events --namespace keycloak-config --sort-by='.lastTimestamp'

# Port forward for testing
kubectl port-forward deployment/keycloak-config-cli 8080:8080 --namespace keycloak-config
```

## Best Practices

1. **Resource Management**: Set appropriate requests and limits
2. **Security**: Use least-privilege RBAC and NetworkPolicies
3. **Monitoring**: Implement comprehensive monitoring and logging
4. **Backup**: Regular backups of configurations and secrets
5. **Testing**: Test deployments in staging first
6. **Documentation**: Maintain clear documentation of configurations

## Next Steps

- [Helm Chart](helm-chart.md) - Simplified Helm deployment
- [Docker Usage](docker-usage.md) - Docker and Docker Compose usage
- [Configuration](../config/overview.md) - General configuration options
