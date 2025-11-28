# Contributing

Thank you for your interest in improving keycloak-config-cli! This project is community‑driven and we welcome issues, pull requests, and feedback.

Please also review our [Code of Conduct](CODE_OF_CONDUCT.md) and [Security Policy](SECURITY.md).

## Before you start

- For small fixes and documentation improvements, feel free to open a pull request directly.
- For larger changes, new features, or behavior changes, please open an issue first to discuss your proposal: https://github.com/adorsys/keycloak-config-cli/issues
  - This helps validate the approach, avoid duplicate work, and get early feedback.

## Finding something to work on

- Browse open issues and look for labels like `help wanted` or `good first issue`.
- If you’re not sure where to start, open an issue to introduce yourself and what you’re interested in.

## Questions and help

Join #keycloak-config-cli on Slack for design discussions and questions. Create an account at https://slack.cncf.io/. Direct link to the #keycloak-config-cli channel: https://cloud-native.slack.com/archives/C09SPL5G3MY

## Development setup

- Java Development Kit (JDK) — see versions supported by the project in the CI and `pom.xml`.
- Docker (for integration tests via Testcontainers).
- Maven Wrapper is included; you can use `./mvnw` without installing Maven locally.

See the following README sections:
- [Build this Project](README.md#build-this-project)
- [Run integration tests against real keycloak](README.md#run-integration-tests-against-real-keycloak)
- [Run this Project](README.md#run-this-project)

## Building and testing

Run full verification (includes unit and integration tests):

```bash
./mvnw verify
```

Notes:
- Integration tests use Testcontainers and require a working Docker environment.
- If your Docker environment has issues with internal DNS, see the hint in the README (e.g., `JUNIT_LDAP_HOST`).

## Style and quality

- Keep changes focused and minimal. Avoid unrelated refactors in the same PR.
- Follow the existing code style. The repository provides `checkstyle.xml` and license headers.
- Ensure files include the correct license header. You can use:

```bash
./mvnw license:update-file-header
```

- Prefer small, readable methods and adequate test coverage.

## Commit messages and issue linking

Write clear commit messages:

```
A brief descriptive summary

Optional body describing implementation details or rationale

Closes #1234
```

- Use "Closes #<issue>" (preferred), so GitHub auto‑links and closes the issue.
- Keep one logical change per commit when possible; we prefer a tidy history. Squash during merge is fine.

For more about linking PRs to issues, see GitHub docs: https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue

## Documentation

- Update docs when behavior, flags, environment variables, or user‑visible output changes.
- Common places to update:
  - [README.md](README.md) (quick start, usage, configuration tables, examples)
  - [DOCUMENTATION.md](DOCUMENTATION.md) and `docs/` (feature details and guides)
  - [CHANGELOG.md](CHANGELOG.md) (brief entry for user‑visible changes)

## Pull request checklist

Before you open or mark your PR ready for review, please ensure:

- You opened or referenced a corresponding issue with a clear description
- The PR scope is limited to a single change/feature/fix
- Code builds locally: `./mvnw verify`
- Tests were added/updated where appropriate (unit and/or integration)
- Documentation was updated (README/docs/CHANGELOG as needed)
- No unrelated files or formatting changes included
- Branch is rebased on `main` (use `git rebase`, not `git pull`)

## Submitting your PR

1. Fork the repository and create your branch from `main`.
2. Commit your changes using the guidelines above.
3. Rebase your branch on `main` and ensure a clean build: `./mvnw verify`.
4. Open a pull request and fill in the description, including links to the issue(s).
5. Be responsive to review comments. We may close inactive PRs after a period of no response (you can always reopen or submit a new PR later).

## Security and responsible disclosure

If you find a security vulnerability, please do not open a public issue. Follow our [Security Policy](SECURITY.md) to report it responsibly.

---

We appreciate your contributions — thank you for helping make keycloak-config-cli better!
