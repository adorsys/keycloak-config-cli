# Downloads

Get keycloak-config-cli for your platform.

<div class="download-grid">
  <a href="https://hub.docker.com/r/adorsys/keycloak-config-cli" class="download-card">
    <h3>Docker Hub</h3>
    <p>Official Docker images for all supported Keycloak versions</p>
    <div class="link">adorsys/keycloak-config-cli:latest →</div>
  </a>
  
  <a href="https://quay.io/repository/adorsys/keycloak-config-cli" class="download-card">
    <h3>Quay.io</h3>
    <p>Alternative container registry with same images</p>
    <div class="link">quay.io/adorsys/keycloak-config-cli:latest →</div>
  </a>
  
  <a href="https://github.com/adorsys/keycloak-config-cli/releases" class="download-card">
    <h3>GitHub Releases</h3>
    <p>JAR files and release notes for each version</p>
    <div class="link">v6.5.0 (Latest) →</div>
  </a>
  
  <a href="helm-chart/" class="download-card">
    <h3>Helm Chart</h3>
    <p>Kubernetes Helm chart for init-container deployment</p>
    <div class="link">contrib/charts/keycloak-config-cli →</div>
  </a>
</div>

---

## Version Compatibility

<table class="version-table">
  <thead>
    <tr>
      <th>Keycloak</th>
      <th>CLI Version</th>
      <th>Status</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>26.x</td>
      <td>6.5.x</td>
      <td><span class="version-badge latest">Latest</span></td>
    </tr>
    <tr>
      <td>26.x</td>
      <td>6.4.x</td>
      <td><span class="version-badge supported">Supported</span></td>
    </tr>
    <tr>
      <td>25.x</td>
      <td>6.3.x</td>
      <td><span class="version-badge supported">Supported</span></td>
    </tr>
    <tr>
      <td>24.x</td>
      <td>6.2.x</td>
      <td><span class="version-badge supported">Supported</span></td>
    </tr>
    <tr>
      <td>23.x</td>
      <td>6.1.x</td>
      <td><span class="version-badge supported">Supported</span></td>
    </tr>
    <tr>
      <td>22.x</td>
      <td>6.0.x</td>
      <td><span class="version-badge lts">LTS</span></td>
    </tr>
  </tbody>
</table>

---

## Installation

### Docker

```bash
# Latest version
docker pull adorsys/keycloak-config-cli:latest

# Specific version
docker pull adorsys/keycloak-config-cli:v6.5.0
```

### Java JAR

Download from [GitHub Releases](https://github.com/adorsys/keycloak-config-cli/releases).

### Helm

```bash
helm repo add adorsys https://adorsys.github.io/keycloak-config-cli
helm install keycloak-config-cli adorsys/keycloak-config-cli
```

---

## Release Notes

See the full [changelog on GitHub](https://github.com/adorsys/keycloak-config-cli/blob/main/CHANGELOG.md).
