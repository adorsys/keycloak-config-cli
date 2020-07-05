# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

### Changed

- Handle exit code in a spring native way.
- Improve error handling if keycloak returns a non 2xx http error

### Fixed

- Fix import crash if last import crashed while a temporary flow was used.

## [2.0.0-rc7] - 2020-07-03

### Breaking
The availability check in docker images based on a shell script. The functionality moved into the application now.
The availability check is disabled by default and can be re-enabled with `keycloak.availability-check.enabled=true`.

### Added

- Create, Update, Delete IdentityProviderMappers
- Support for only updating changed IdentityProviders
- Support for managed IdentityProviders

### Changed
- The availability check in docker images is off by default. Re-enable with `keycloak.availability-check.enabled`.
- `WAIT_TIME_IN_SECONDS` is replaced by `keycloak.availability-check.timeout`.

### Fixed

## [2.0.0-rc6] - 2020-06-29

### Fixed

- Do not delete authenticatorConfigs from builtin flows

## [2.0.0-rc5] - 2020-06-29

### Added

- Manage group membership of users

### Fixed

- Don't update client if protocolMappers are not changed
- Don't update clientScope if protocolMappers are not changed
- Don't update groups config if subGroups are not changed

## [2.0.0-rc4] - 2020-06-26

### Fixed

- Fixed releasing of artifacts

## [2.0.0-rc3] - 2020-06-26

### Added

- Parallel import (only some resources are supported. To enable use `--import.parallel=true`)
- Don't update client if not changed
- Don't update components config if not changed
- Don't update realm role if not changed

## [2.0.0-rc2] - 2020-06-25

### Added

- Added Helm Chart
- Support yaml as configuration import format. (`--import.file-type=yaml`)
- In some situations if Keycloak gives 400 HTTP error, pass error message from keycloak to log.
- Allow updating builtin flows and executions (keycloak allows to change some properties)
- Remove authentications config from keycloak if not defined in realm

### Changed

- Set user to 1001 in Dockerfile
- Bump Keycloak from 8.0.1 to 8.0.2
- Define jackson version in pom.xml to avoid incompatibilities between `jackson-bom` and `keycloak-config-cli` dependencies.

### Fixed

- Authentication configs in non top-level flow are not created.

## [2.0.0-rc1] - 2020-06-17

### Breaking

- `import.file` is removed. Use `import.path` instead for files and directories.
- `keycloak.migrationKey` is removed. Use `import.cache-key` instead.
- `keycloak.realm` is removed. Use `import.login-realm` to define the realm to login.
- If you have defined requiredActions, components, authentications flows or subcomponents in your realm configure, make sure you have defined all in your json files. All not defined
  actions will removed now by keycloak-config-cli unless `import.state=true` is set (default). See: [docs/MANAGED.md](docs/MANAGED.md)

### Added

- PMD for static source code analysis
- _Experimental_ GraalVM support. Run keycloak-config-cli without Java!
- Throw errors on unknown properties in config files
- Add, update and remove clientScopes (thanks @spahrson)
- Remove required actions if they not defined in import json.
- Remove components if they not defined in import json.
- Remove subcomponents if they not defined in import json.
- Remove authentication flows if they not defined in import json.
- Control behavior of purging ressource via `import.manage.<type>` property. See: [docs/MANAGED.md](docs/MANAGED.md)
- State management for `requriedActions`, `clients`, `components`

### Changed

- Reduce docker image size
- Bump SpringBoot from 2.2.7 to 2.3.1
- Bump keycloak from 10.0.0 to 10.0.2
- Used keycloak parent pom instead manage versions of 3rd party libs
- Add experimental profile for spring native builds
- Human friendly error messages instead stack traces if log level is not debug.
- SHA2 instead SHA3 is now used for config checksums
- Rename `keycloak.migrationKey` to `import.cache-key` instead.
- Rename `keycloak.realm` to `import.login-realm` instead.

### Fixed

- Updating `protocolMappers` on `clients`

### Removed

- `import.file` parameter

## [1.4.0] - 2020-04-30

### Added

- AuthenticatorConfig support (thanks @JanisPlots)
- Keycloak 10 support

### Changed

- Bump keycloak 9.0.3

### Fixed

- Fix spotbugs and sonar findings

## [1.3.1] - 2020-04-02

### Changed

- Bump Spring Boot version to 2.2.5
- Bump maven-javadoc-plugin from 3.1.1 to 3.2.0

### Fixed

- Use username filter for updating users, too.

## [1.3.0] - 2020-03-27

### Added

- Add and update groups
- Update composites in roles

### Changed

- Add copyright header to all java classes
- Bump Keycloak to 9.0.2

## [1.2.0] - 2020-03-15

### Added

- Implement migrationKey property for different config files per realm
- Implement identity providers

### Changed

- Add @SuppressWarnings("unchecked")
- Migrate to maven single module
- Use TestContainers

### Fixed

- Correct username on import

## [1.1.2] - 2020-02-25

### Changed

- Use Java 8 inside container again

## [1.1.1] - 2020-02-25

### Fixed

- Re-add Keycloak 8

## [1.1.0] - 2020-02-25

### Added

- Keycloak 9 support

### Changed

- Use Java 11 inside container
- Bump hibernate-validator from 6.0.13.Final to 6.1.0.Final

[unreleased]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0-rc7...HEAD
[2.0.0-rc7]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0-rc6...v2.0.0-rc7
[2.0.0-rc6]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0-rc5...v2.0.0-rc6
[2.0.0-rc5]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0-rc4...v2.0.0-rc5
[2.0.0-rc4]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0-rc3...v2.0.0-rc4
[2.0.0-rc3]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0-rc2...v2.0.0-rc3
[2.0.0-rc2]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0-rc1...v2.0.0-rc2
[2.0.0-rc1]: https://github.com/adorsys/keycloak-config-cli/compare/v1.4.1...v2.0.0-rc1
[1.4.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.3.1...v1.4.0
[1.3.1]: https://github.com/adorsys/keycloak-config-cli/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.4...v1.2.0
[1.1.4]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.3...v1.1.4
[1.1.3]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.2...v1.1.3
[1.1.2]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.0.0...v1.1.0
