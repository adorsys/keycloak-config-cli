#

<div class="hero-section">
  <img src="images/logo-wordmark.svg" alt="keycloak-config-cli" class="hero-wordmark">
  <span class="open-source-badge">Open Source</span>
  <h1>Keycloak Configuration as Code</h1>
  <p>Ensure the desired configuration state for your Keycloak realms using YAML or JSON files. Version-controlled, idempotent, and CI/CD-ready — no Keycloak restart required.</p>
  <p>
    <a href="guides.md" class="md-button">Get Started</a>
    <a href="https://github.com/adorsys/keycloak-config-cli" class="md-button md-button--secondary">View on GitHub</a>
  </p>
</div>

<div class="two-column">
  <div>
    <h2>Idempotent Configuration Import</h2>
    <p>keycloak-config-cli applies configuration changes rather than re-creating resources from scratch. Run it as many times as you like — only the delta between your desired state and the current Keycloak state is applied.</p>
    <p>This idempotency makes it safe to include in any automated pipeline. A deploy that has nothing to change completes instantly without side effects.</p>
  </div>
  <div>
    <div class="terminal-code">
      <div class="comment"># Run twice — second run is a no-op</div>
      <div class="command">$ docker run --rm \</div>
      <div>    -e KEYCLOAK_URL="http://keycloak:8080" \</div>
      <div>    -e KEYCLOAK_USER=admin \</div>
      <div>    -e KEYCLOAK_PASSWORD=admin123 \</div>
      <div>    -e IMPORT_FILES_LOCATIONS="/config/*" \</div>
      <div>    -v "$PWD/realms":/config \</div>
      <div>    adorsys/keycloak-config-cli:latest</div>
      <br>
      <div class="comment"># Output:</div>
      <div class="output">/ Realm 'my-realm': no changes</div>
      <div class="output">/ Client 'my-app': no changes</div>
      <div>Done in 1.2s</div>
    </div>
  </div>
</div>

<div class="two-column">
  <div>
    <div class="terminal-code">
      <div class="comment"># realm.yaml — works for any environment</div>
      <div>realm: ${env:APP_ENV}-realm</div>
      <div>enabled: true</div>
      <div>clients:</div>
      <div>  - clientId: my-app</div>
      <div>    secret: ${file:UTF-8:${env:SECRET_FILE}}</div>
      <div>    redirectUris:</div>
      <div>      - "${env:APP_URL}/*"</div>
      <br>
      <div class="comment"># JavaScript evaluation</div>
      <div>sessionTimeout: ${javascript: 2 * 60 * 60}</div>
    </div>
  </div>
  <div>
    <h2>Variable Substitution for Multi-Environment Deployments</h2>
    <p>A single set of realm config files can target dev, staging, and production by injecting environment-specific values at import time. Enable <code>import.var-substitution.enabled=true</code> and reference environment variables, files, Base64 values, system properties, URLs, DNS lookups, and more.</p>
    <p>Recursive substitution is supported: a variable can itself resolve to a file path, which is then read and substituted. Optional JavaScript evaluation enables dynamic expressions directly in your YAML.</p>
  </div>
</div>

<div class="two-column">
  <div>
    <h2>Remote State Management</h2>
    <p>keycloak-config-cli tracks which resources were created by the tool using a state annotation stored directly in Keycloak. Only resources managed by keycloak-config-cli are removed during cleanup — manual changes made in the Admin UI are never touched unexpectedly.</p>
    <p>Remote state can optionally be stored encrypted to protect sensitive configuration metadata. This gives you a safe and auditable view of exactly what your automation owns.</p>
  </div>
  <div style="text-align: center;">
    <div style="background: #f8fafc; border-radius: 12px; padding: 2rem; border: 1px solid #e2e8f0;">
      <div style="font-size: 3rem; margin-bottom: 1rem;">📋</div>
      <p style="color: #64748b; font-size: 0.9rem; margin: 0;">State tracked inside Keycloak.<br>Only managed resources are modified.</p>
    </div>
  </div>
</div>

<div class="two-column">
  <div>
    <div class="terminal-code">
      <div class="comment"># .github/workflows/keycloak.yml</div>
      <div>name: Deploy Keycloak Config</div>
      <div>on:</div>
      <div>  push:</div>
      <div>    paths: ['keycloak/**/*']</div>
      <div>jobs:</div>
      <div>  deploy:</div>
      <div>    runs-on: ubuntu-latest</div>
      <div>    steps:</div>
      <div>      - uses: actions/checkout@v4</div>
      <div>      - run: |</div>
      <div>          docker run --rm \</div>
    </div>
  </div>
  <div>
    <h2>CI/CD Pipeline Integration</h2>
    <p>keycloak-config-cli is designed to run as a step in any automated pipeline. It ships as a Docker image available on Docker Hub and Quay.io, as a Java JAR, and as a Helm chart for Kubernetes init-container patterns.</p>
    <p>GitHub Actions, GitLab CI, Jenkins, ArgoCD, and Flux all work out of the box. Pair with Docker Secrets or Kubernetes Secrets for secure credential injection.</p>
  </div>
</div>

<div class="section-header">
  <h2>Docker, Helm, and Kubernetes Ready</h2>
  <p>Images are published to Docker Hub and Quay.io on every release. Multiple tags cover every supported Keycloak version as well as an edge-build tag for testing against unreleased versions.</p>
</div>

<div style="background: #f8fafc; border-radius: 12px; padding: 2rem; margin: 2rem 0; text-align: center; border: 1px solid #e2e8f0;">
  <div style="font-size: 2.5rem; margin-bottom: 1rem;">🐳</div>
  <p style="color: #64748b; font-size: 0.9rem; margin: 0;">Docker Hub • Quay.io • Helm Chart<br>All Keycloak versions supported</p>
</div>

<div class="two-column">
  <div>
    <div class="terminal-code">
      <div class="comment"># realm.yaml — Keycloak export format</div>
      <div>realm: my-realm</div>
      <div>enabled: true</div>
      <div>loginTheme: custom</div>
      <div>clients:</div>
      <div>  - clientId: backend-api</div>
      <div>    protocol: openid-connect</div>
      <div>    bearerOnly: true</div>
      <div>roles:</div>
      <div>  realm:</div>
      <div>    - name: app-admin</div>
      <div>    - name: app-user</div>
    </div>
  </div>
  <div>
    <h2>YAML and JSON — Keycloak's Own Export Format</h2>
    <p>Configuration files use the same schema as Keycloak's native realm export. Export an existing realm from the Admin UI, trim the UUIDs and defaults, and you have a working config file immediately.</p>
    <p>Both YAML and JSON are fully supported. Ant-style glob patterns let you split configuration across multiple files by resource type, realm, or environment — loaded in a predictable order.</p>
  </div>
</div>

<div class="section-header">
  <h2>Checksum Caching and Parallel Import</h2>
  <p>keycloak-config-cli caches checksums of imported config files so unchanged files are skipped on subsequent runs, making pipelines faster in environments where only a subset of realms change between deployments.</p>
  <p>Parallel import can be enabled to speed up large configurations by processing compatible resources concurrently. Both behaviours are configurable through environment variables or CLI flags.</p>
</div>

---

## Features

<div class="feature-grid">
  <div class="feature-card">
    <div class="icon">✓</div>
    <h3>Idempotent Import</h3>
    <p>Apply once or a thousand times — only actual changes are made</p>
  </div>
  <div class="feature-card">
    <div class="icon">📄</div>
    <h3>YAML & JSON Support</h3>
    <p>Keycloak's own export format, in the language you prefer</p>
  </div>
  <div class="feature-card">
    <div class="icon">🔧</div>
    <h3>Variable Substitution</h3>
    <p>Env vars, files, Base64, URLs, DNS, system props and JavaScript</p>
  </div>
  <div class="feature-card">
    <div class="icon">🚀</div>
    <h3>CI/CD Native</h3>
    <p>GitHub Actions, GitLab CI, Jenkins, ArgoCD — all work out of the box</p>
  </div>
  <div class="feature-card">
    <div class="icon">🐳</div>
    <h3>Docker & Helm</h3>
    <p>Published to Docker Hub and Quay.io. Helm chart included</p>
  </div>
  <div class="feature-card">
    <div class="icon">⏱️</div>
    <h3>Availability Check</h3>
    <p>Wait for Keycloak to be ready before importing — perfect for init containers</p>
  </div>
  <div class="feature-card">
    <div class="icon">🏠</div>
    <h3>Remote State</h3>
    <p>Track managed resources so only automation-owned resources are removed</p>
  </div>
  <div class="feature-card">
    <div class="icon">✓</div>
    <h3>Checksum Cache</h3>
    <p>Skip unchanged files on re-runs for faster pipelines</p>
  </div>
  <div class="feature-card">
    <div class="icon">⚡</div>
    <h3>Parallel Import</h3>
    <p>Speed up large realm configurations with concurrent resource processing</p>
  </div>
  <div class="feature-card">
    <div class="icon">✓</div>
    <h3>All Keycloak Resources</h3>
    <p>Realms, clients, roles, flows, identity providers, users, groups and more</p>
  </div>
  <div class="feature-card">
    <div class="icon">⭐</div>
    <h3>Encrypted State</h3>
    <p>Optionally store remote state in encrypted format for added security</p>
  </div>
  <div class="feature-card">
    <div class="icon">🔒</div>
    <h3>Spring Boot Based</h3>
    <p>Relaxed binding, configtree secrets, profiles and Spring config support</p>
  </div>
</div>

---

<div style="text-align: center; padding: 3rem 0; color: #64748b;">
  <p>keycloak-config-cli is an open-source project by <a href="https://adorsys.com">adorsys GmbH & Co. KG</a>, used with <a href="https://keycloak.org">Keycloak</a> — a Cloud Native Computing Foundation project</p>
</div>
