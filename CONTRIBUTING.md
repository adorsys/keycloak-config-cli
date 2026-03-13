# Contributing

Please read and follow our **[Code of Conduct](CODE_OF_CONDUCT.md)**.

Thank you for your interest in improving keycloak-config-cli. This guide is intended to take you from **clone** to a **merged PR**, even if you are new to the codebase.

If you found a security vulnerability, please do **not** open a public issue. Follow our [Security Policy](SECURITY.md).

## Where to ask questions

- For usage/configuration questions, please use **Discussions (Q&A)**.
- For bug reports and feature requests, open an **Issue**.
- For design discussions, join **#keycloak-config-cli** on Slack:
  - Create an account: https://slack.cncf.io/
  - Channel: https://cloud-native.slack.com/archives/C09SPL5G3MY

## Finding something to work on

- Start with issues labeled `good first issue` or `help wanted`.
- If you want to work on something bigger, open an issue first to align on the approach.

## Local development setup (macOS / Linux / Windows)

### Prerequisites

- **Git**
- **JDK**: see supported versions in CI and `pom.xml`
- **Docker**: required for integration tests (Testcontainers)

Notes:
- Maven Wrapper is included; you do not need Maven installed.

### Clone

```bash
git clone https://github.com/adorsys/keycloak-config-cli.git
cd keycloak-config-cli
```

### Build

macOS / Linux:

```bash
./mvnw -v
./mvnw -DskipTests package
```

Windows (PowerShell):

```powershell
./mvnw.cmd -v
./mvnw.cmd -DskipTests package
```

### Run unit + integration tests

Full verification (recommended before opening a PR):

macOS / Linux:

```bash
./mvnw verify
```

Windows:

```powershell
./mvnw.cmd verify
```

Troubleshooting:
- Integration tests use Testcontainers and require a working Docker environment.
- If your Docker environment has issues with internal DNS, see the README hints (e.g. `JUNIT_LDAP_HOST`).

### Run against a real Keycloak (manual testing)

This project is often validated by running the built JAR against a Keycloak instance.

See:
- [Run this Project](README.md#run-this-project)
- [Docker](README.md#docker)
- [Helm](README.md#helm)

## Architecture overview (high level)

keycloak-config-cli is a Spring Boot application that reads JSON/YAML import files (Keycloak export format), performs optional normalization and variable substitution, and then applies the desired state to Keycloak through the Admin REST API.

At a high level:

```text
import files -> KeycloakImportProvider -> RealmImportService
  -> *ImportService classes (clients/users/groups/roles/flows/...)
  -> repositories (Admin API calls)
  -> state/checksum services (idempotency + "managed" behavior)
```

Key code locations:
- `src/main/java/de/adorsys/keycloak/config/provider`: import file loading + variable substitution
- `src/main/java/de/adorsys/keycloak/config/service`: orchestrates imports per realm (e.g. `RealmImportService`)
- `src/main/java/de/adorsys/keycloak/config/repository`: Admin API access wrappers
- `src/main/java/de/adorsys/keycloak/config/service/state`: state tracking used for managed/no-delete/remote-state behaviors
- `src/test/...`: unit tests + integration tests (Testcontainers)

## Code style and quality

### Checkstyle

Checkstyle runs as part of Maven builds.

```bash
./mvnw checkstyle:check
```

### License headers

If you add new source files, ensure they include the correct license header. You can apply headers via:

```bash
./mvnw license:update-file-header
```

### Keep PRs focused

- Keep changes minimal and avoid unrelated refactors.
- Prefer small, readable methods.
- Add tests where behavior changes.

## Git workflow and PR process

### Branch naming

Use a descriptive name. Examples:

- `fix/<short-description>`
- `feat/<short-description>`
- `docs/<short-description>`

### Commit messages

Use clear commit messages. If applicable, link issues.

For PR descriptions and the reviewer checklist, use the PR template.

```text
Short summary

Optional details

Closes #1234
```

### What reviewers expect

- What changed and why
- How you tested (unit/integration/manual)
- Tests updated/added where appropriate
- Docs updated (README / `DOCUMENTATION.md` / `CHANGELOG.md`) for user-facing changes

### Review SLA

Maintainers aim to respond within **48 hours** on weekdays. If you have not heard back, please leave a short comment to bump.

## Documentation updates

If you change behavior, flags, environment variables, or user-visible output, please update:

- `README.md`
- `DOCUMENTATION.md` and `docs/`
- `CHANGELOG.md`

---

Thank you for contributing!
