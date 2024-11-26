## Helm Chart

The Helm chart for keycloak-config-cli is available via GitHub Pages. To use it,

1. Add the Helm repository:
```shell
helm repo add keycloak-config-cli https://adorsys.github.io/keycloak-config-cli/charts
```


2. Update your local Helm chart repository cache:
```shell
helm repo update
```
3. Install the chart:
```shell
helm install keycloak-config-cli keycloak-config-cli/keycloak-config-cli
```


