# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

We now also consider auth flows referenced by post-broker login flow Identity Provider configurations for flow in-use checks.

## [5.2.1] - 2022-06-20

### Added
- Added latest Keycloak 18.0.1 library

## [5.2.0] - 2022-05-06

### Added
- Added Keycloak 18 support

## [5.1.0] - 2022-04-08

### Added

- Dump realm configuration on trace log level

## [5.0.0] - 2022-03-25

### Notes

A lot of import properties are added over the years. this major release of keycloak will reorder all properties. You will find a translation table below.

### Added

- Support for managing fine-grained authorization rules with placeholders to reference identity providers by alias, realm role by name and groups by full path

### Changed

- Docker base images changed from `openjdk` to `eclipse-temurin`
- Refactored import properties.
    - `import.force=true` -> `import.cache.enabled=false`
    - `import.cache-key` -> `import.cache.key`
    - `import.path` -> `import.files.locations`
    - `import.hidden-files` -> `import.files.include-hidden-files`
    - `import.exclude` -> `import.files.excludes`
    - `import.file-type` -> removed
    - `import.state` -> `import.remote-state.enabled`
    - `import.state-encryption-key` -> `import.remote-state.encryption-key`
    - `import.state-encryption-salt` -> `import.remote-state.encryption-salt`
    - `import.var-substitution` -> `import.var-substitution.enabled`
    - `import.var-substitution-in-variables` -> `import.var-substitution.nested`
    - `import.var-substitution-undefined-throws-exceptions` -> `import.var-substitution.undefined-is-error`
    - `import.var-substitution-prefix` -> `import.var-substitution.prefix`
    - `import.var-substitution-suffix` -> `import.var-substitution.suffix`
    - `import.remove-default-role-from-user` -> `import.behaviors.remove-default-role-from-user`
    - `import.skip-attributes-for-federated-user` -> `import.behaviors.skip-attributes-for-federated-user`
    - `import.sync-user-federation` -> `import.behaviors.sync-user-federation`
- Changed loading of directories
    - `path/to/dir` -> `path/to/dir/*`
- Changed loading of zip files
    - `path/to/file.zip` -> `zip:file:path/to/file.zip!**/*`

### Fixed

- import path contains `..`

### Removed

- Java 8 Support
- `customImport` property in json import.
- Directory import. Use `dir/*` instead `dir/`
- Support for zip files from http locations
- `import.file-type`. Import files will always be parsed with YAML parser. JSON files are YAML compatible.

## [4.9.0] - 2022-03-21

### Added

- Support for managing fine-grained authorization rules with placeholders to reference identity providers by alias, realm role by name and groups by full path

### Changes

- Remove `v` prefix docker image tags.

## [4.8.1] - 2022-03-09

### Fixed

- Docker Image for Keycloak 14, 15, 16 contains the version for Keycloak 17

## [4.8.0] - 2022-03-06

### Added

- Support for managing `Client Authorization Resources` like other resources by configuring `import.managed.client-authorization-resources=<full|no-delete>`. This prevents deletion of remote managed resources.
- Support for managing fine granted authorization rules with placeholders to reference clients by client id.

### Changes

- Compile keycloak-config-cli inside docker build to avoid the requirement to run maven before

### Fixed

- Manage  `Client Authorization` without define a `clientId` in import realm.

## [4.7.0] - 2022-02-14

### Added

- Added Keycloak 17 support, drop Keycloak 13 support
- Allow spring boot properties in string substitution.
- Supports YAML anchors in realm import file

### Fixed

- 404 not found, if roles have nested composites

## [4.6.1] - 2022-01-17

### Fixed

- NoClassDefFoundError: org/apache/commons/lang3/StringUtils if IMPORT_VARSUBSTITUTION=true

## [4.6.0] - 2022-01-16

### Added

- Support logout with confidential client if grant_type=password is used.
- Make read and connect timeout of Resteasy client configurable (defaults stay the same as before)

### Changes

- Add `--import.validate` flag to disable pre validation checks inside keycloak-config-cli.
- Change maven wrapper to official one (https://maven.apache.org/wrapper/)

### Fixed

- Skip logout if grant_type=client_credentials is used

## [4.5.0] - 2021-12-19

### Added

- Added Keycloak 16 support, drop Keycloak 12 support
- Support for multiple realm definitions inside one YAML file.
- Workaround for creating client authorization resources, if a username is defined an owner through `owner.name`. Keycloak [excepts](https://github.com/keycloak/keycloak/blob/bfce612641a70e106b20b136431f0e4046b5c37f/server-spi-private/src/main/java/org/keycloak/models/utils/RepresentationToModel.java#L2647-L2649) `owner.id` here instead `owner.name`. See [#589](https://github.com/adorsys/keycloak-config-cli/pull/589)

## [4.4.0] - 2021-12-04

### Added

- Cookie Management for http client to support clustered environments with cookie based sticky sessions
- Raise an exception, if authenticator is defined for a basic-flow execution
- Support for managing `Client Scope Mappings` like other resources by configuring `import.managed.client-scope-mapping=<full|no-delete>`.
- Configuration profile for RedHat's maven repository to fetch RH SSO compatible keycloak versions

### Changes

- Use java 17 as default and use docker image `openjdk:17-slim`

### Fixed

- Stale client level roles assignment on a user, if the client is not present in the `clientRoles` JSON object in the config file.
  The Keycloak default client roles (e.g. realm-management) will remain untouched though.

## [4.3.0] - 2021-09-28

### Added

- Docker Images for arm64
- Managed realm level `defaultDefaultClientScopes` and `defaultOptionalClientScopes`

### Changes

- Introduce maven wrapper (`./mvnw`) to easy access maven for non developers

## [4.2.0] - 2021-08-09

### Added

- Support initial user password (only set doing user creation). See [docs/FEATURE.md](docs/FEATURES.md) for more information.
- Flag `import.skip-attributes-for-federated-user` to set user attributes to `null` for federated users. Defaults to `false`.
- Validate composite client roles

### Fixed

- Update subComponents if config of parent is equal

## [4.1.0] - 2021-07-31

### Added

- Keycloak 15 support
- Print a warning if local keycloak-config-cli and keycloak are incompatible.
- Terminate admin-cli session through `logout` REST endpoint

### Fixed

- Realm attributes in configuration file overwrite the realm's state when the realm is updated.
- Custom realm attributes not updatable.

### Removed

- Keycloak 11 support

## [4.0.1] - 2021-06-19

### Changed

- Set `import.var-substitution-prefix=$(` and `import.var-substitution-suffix=)` as default to prevent incompatibility with keycloak variables. This
  change forgotten in release 4.0.0.

## [4.0.0] - 2021-06-18

### Breaking

- New keycloak support policy: keycloak-config-cli will officially support the 4 latest keycloak versions. In the future, if a new keycloak version is
  out, the oldest version will be removed without bump the major version of keycloak-config-cli
- New defaults: `import.var-substitution-prefix=$(` and `import.var-substitution-suffix=)` to prevent incompatibility with keycloak variables.
  TL;DR: If you import file containers variables like `${env:USERNAME}`, you have to replace them with `$(env:USERNAME)`.

### Added

- JSON logging
- Support Keycloak 14
- User federation can be automatically synchronized with `import.sync-user-federation` set to `true`
- New flag `import.remove-default-role-from-user`. Default to `false`.
  Keycloak 13 attach a default role named `default-role-$REALM` that contains some defaults from any user.
  Previously keycloak-config-cli remove that default role, if the role not defined inside the import json.
  The flag prevents keycloak-config-cli from exclude `default-roles-$REALM` from removal logic. This results that it's not longer possible to explicit
  remove the role from a user, if this flag set to `true`.

### Fixed

- Exclude `default-roles-$REALM` from user realm role removal

### Removed

- Support Keycloak 9
- Support Keycloak 10

## [3.4.0] - 2021-05-12

### Added

- Support for Keycloak 13
  _Note_: If you get an error like `client already exists` or `java.lang.IllegalStateException: Session/EntityManager is closed`, it's not an error in keycloak-config-cli. See https://issues.redhat.com/browse/KEYCLOAK-18035
- Define custom var substitution prefix and suffix through `import.var-substitution-prefix` and `import.var-substitution-suffix`.
  This prevents conflicts with keycloak builtin variables.
  Default to `${` and `}` and will be changed to `$(` and `)`. in keycloak-config-cli 4.0.
- News image tag call `edge-build` that compile keycloak-config-cli run runtime. This useful to run keycloak-config-cli against unsupported keycloak versions.
- Keycloak images additionally pushed to quay.io

### Fixed

- Versions specific images of keycloak-config-cli are not exists with keycloak version variations.

## [3.3.1] - 2021-05-04

### Fixed

- 409 Conflict on importing client role that already exists but not in state.

## [3.3.0] - 2021-04-24

### Added

- Do not reset eventsEnable if missing in import
- Client secrets mapping on the client scopes with the `clientScopeMappings`.

### Fixed

- Undetermined treatment of a client without the client id specified.
- Provisioning of a client with service account enabled when the `registrationEmailAsUsername` flag for the realm is set to `true`.

## [3.2.0] - 2021-03-12

### Added

- Support for `defaultGroups`

### Changed

- Using adoptopenjdk/openjdk11:alpine-jre as base image instead openjdk to reduce image footprint and vulnerabilities.

## [3.1.3] - 2021-03-08

### Fixed

- Add `v` prefix to docker images (restore breaking change)

## [3.1.2] - 2021-03-08

### Fixed

- Docker builds inside release pipeline
- 400 Bad Request while deleting a used client scope

## [3.1.1] - 2021-03-07

### Changed

- Bump keycloak from 12.0.3 to 12.0.4

### Fixed

- Forbidden error while create a new realm with a keycloak service account.
- Do not try to remove effective user roles

## [3.1.0] - 2021-02-18

### Added

- `wget` inside docker container
- If `keycloak.grant-type` is set to `client_credentials` the tool can use client_id and client_secret for obtaining its OAuth tokens (
  default: `password`)
- The `keycloak.client-secret` can now be set for confidential OAuth clients (and it's required for the `client_credentials` flow together with
  an `keycloak.client-id` referring an OAuth client which supports the client_credentials OAuth flow).
- import.path accepts now zip files and remote locations (http)

### Changed

- Default development branch renamed from `master` to `main`
- The docker tag `master` has been renamed to `edge`
- Bump keycloak from 12.0.2 to 12.0.3

### Fixed

- Cleanup old authenticator configs
- Ordering and execution flow authentication config if multiple execution have the same authenticator.

## [3.0.0] - 2021-01-20

### Breaking

- keycloak-config-cli does not auto append `/auth/` to the keycloak path.
- Role and Clients are `fully managed` now. See: [docs/MANAGED.md](docs/MANAGED.md). _Take care while upgrade exist keycloak instances_. This upgrade should be tested carefully on existing instances. If `import.state` is enabled, only roles and clients created by keycloak-config-cli will be deleted. Set `--import.managed.role=no-delete` and `--import.managed.client=no-delete` will restore the keycloak-config-cli v2.x behavior.

### Added

- Support for Keycloak 12.0.1

### Changed

- Set `import.managed.role` and `import.managed.client` to `full` as default
- Remove experimental native builds
- Update to Resteasy to 4.5.8.Final

### Fixed

### Removed

- Support for Keycloak 8
- Auto append `/auth/` url

## [2.6.3] - 2020-12-09

### Changed

- Update Spring Boot to 2.4.0

### Fixed

- On client import `defaultClientScopes` and `optionalClientScopes` are ignored on existing clients.
- Prevent 409 Conflict error with users if "email as username" is enabled

## [2.6.2] - 2020-11-18

### Fixed

- On client import `defaultClientScopes` and `optionalClientScopes` are ignored if referenced scope does not exist before import.

## [2.6.1] - 2020-11-17

### Fixed

- Pipeline related error inside release
  process. [GitHub Blog](https://github.blog/changelog/2020-10-01-github-actions-deprecating-set-env-and-add-path-commands/)

## [2.6.0] - 2020-11-17

### Added

- If `import.state-encryption-key` is set, the state will be stored in encrypted format.
- If 'import.var-substitution-in-variables' is set to false var substitution in variables is disabled (default: true)
- If 'import.var-substitution-undefined-throws-exceptions' is set to false unknown variables will be ignored (default: true)

### Changed

- Pre validate client with authorization settings
- Update to Keycloak 11.0.3

### Fixed

- Calculate import checksum after variable substitution
- Ignore the id from imports for builtin flows and identityProviderMappers if resource already exists
- Fix [KEYCLOAK-16082](https://issues.redhat.com/browse/KEYCLOAK-16082)
- Can't manage user membership of subgroups

## [2.5.0] - 2020-10-19

### Added

- Roles are fully managed now and could be deleted if absent from import (disabled by default)
- Clients are fully managed now and could be deleted if absent from import (disabled by default)
- client scope mapping can be managed through keycloak-config-cli

### Changed

- **DEPRECATION:** Auto append `/auth` in server url.

### Fixed

- Required action providerId and alias can be different now
- ProviderId of required actions can be updated now

## [2.4.0] - 2020-10-05

### Added

- Builds are now reproducible.
- Provide checksums of prebuild artifacts.
- `import.var-substitution=true` to enable substitution of environment variables or system properties. (default: false)
- Multiple file formats could be detected by file ending
- HTTP Proxies now supported. Use `-Dhttp.proxyHost` and `-Dhttp.proxyHost` to specify proxy settings.

### Fixed

- On directory import, the order of files is consistent now. (default ordered)
- Allow custom sub paths of keycloak.

## [2.3.0] - 2020-09-22

### Added

- Allow loading Presentations (like RealmRepresentation) externally.
  See [docs](https://github.com/adorsys/keycloak-config-cli/blob/main/contrib/custom-representations/README.md) for more information.
- Update flow descriptions form builtin flows

### Changed

- Update to Keycloak 11.0.2
- Update to Resteasy to 3.13.1.Final

### Fixed

- Fix update `authenticationFlowBindingOverrides` on clients [issue-170](https://github.com/adorsys/keycloak-config-cli/issues/170)
- Fix creation clientScopes with protocolMappers [issue-183](https://github.com/adorsys/keycloak-config-cli/issues/183)
- Fix could not update default clientScopes with protocolMappers [issue-183](https://github.com/adorsys/keycloak-config-cli/issues/183)

## [2.2.0] - 2020-08-07

### Added

- Add support for clients with fine-grained authorization

## [2.1.0] - 2020-07-23

### Added

- Keycloak 11 support

### Changed

- Implement checkstyle to ensure consistent coding style.

### Fixed

- Subflow requirement forced to ‘DISABLED’ when importing multiple subflows

## [2.0.2] - 2020-07-15

### Fixed

- Realm creation with an idp and custom auth flow results into a 500 HTTP error

## [2.0.1] - 2020-07-09

### Fixed

- Incorrect Docker entrypoint. Thanks to jBouyoud.

## [2.0.0] - 2020-07-05

### Breaking

- The availability check in docker images based on a shell script. The functionality moved into the application now.
- The availability check is disabled by default and can be re-enabled with `keycloak.availability-check.enabled=true`.
- `import.file` is removed. Use `import.path` instead for files and directories.
- `keycloak.migrationKey` is removed. Use `import.cache-key` instead.
- `keycloak.realm` is removed. Use `import.login-realm` to define the realm to login.
- If you have defined requiredActions, components, authentications flows or subcomponents in your realm configure, make sure you have defined all in
  your json files. All not defined actions will remove now by keycloak-config-cli unless `import.state=true` is set (default).
  See: [docs/MANAGED.md](docs/MANAGED.md)

### Added

- Create, Update, Delete IdentityProviderMappers
- Support for only updating changed IdentityProviders
- Support for managed IdentityProviders
- Manage group membership of users
- Parallel import (only some resources are supported. To enable use `--import.parallel=true`)
- Don't update client if not changed
- Don't update components config if not changed
- Don't update realm role if not changed
- Added Helm Chart
- Support yaml as configuration import format. (`--import.file-type=yaml`)
- In some situations if Keycloak gives 400 HTTP error, pass error message from keycloak to log.
- Allow updating builtin flows and executions (keycloak allows to change some properties)
- Remove authentications config from keycloak if not defined in realm
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

- Handle exit code in a spring native way.
- Improve error handling if keycloak returns a non 2xx http error
- The availability check in docker images is off by default. Re-enable with `keycloak.availability-check.enabled`.
- `WAIT_TIME_IN_SECONDS` is replaced by `keycloak.availability-check.timeout`.
- Set user to 1001 in Dockerfile
- Bump Keycloak from 8.0.1 to 8.0.2
- Define jackson version in pom.xml to avoid incompatibilities between `jackson-bom` and `keycloak-config-cli` dependencies.
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

- Fix import crash if last import crashed while a temporary flow was used.
- Do not delete authenticatorConfigs from builtin flows
- Don't update client if protocolMappers are not changed
- Don't update clientScope if protocolMappers are not changed
- Don't update groups config if subGroups are not changed
- Authentication configs in non-top-level flow are not created.
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

## 1.1.0 - 2020-02-25

### Added

- Keycloak 9 support

### Changed

- Use Java 11 inside container
- Bump hibernate-validator from 6.0.13.Final to 6.1.0.Final

<!-- @formatter:off -->

[Unreleased]: https://github.com/adorsys/keycloak-config-cli/compare/v5.2.1...HEAD
[5.2.1]: https://github.com/adorsys/keycloak-config-cli/compare/v5.2.0...v5.2.1
[5.2.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.1.0...v5.2.0
[5.1.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.0.0...v5.1.0
[5.0.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.9.0...v5.0.0
[4.9.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.8.1...v4.9.0
[4.8.1]: https://github.com/adorsys/keycloak-config-cli/compare/v4.8.0...v4.8.1
[4.8.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.7.0...v4.8.0
[4.7.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.6.1...v4.7.0
[4.6.1]: https://github.com/adorsys/keycloak-config-cli/compare/v4.6.0...v4.6.1
[4.6.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.5.0...v4.6.0
[4.5.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.4.0...v4.5.0
[4.4.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.3.0...v4.4.0
[4.3.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.2.0...v4.3.0
[4.2.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.1.0...v4.2.0
[4.1.0]: https://github.com/adorsys/keycloak-config-cli/compare/v4.0.1...v4.1.0
[4.0.1]: https://github.com/adorsys/keycloak-config-cli/compare/v4.0.0...v4.0.1
[4.0.0]: https://github.com/adorsys/keycloak-config-cli/compare/v3.4.0...v4.0.0
[3.4.0]: https://github.com/adorsys/keycloak-config-cli/compare/v3.3.1...v3.4.0
[3.3.1]: https://github.com/adorsys/keycloak-config-cli/compare/v3.3.0...v3.3.1
[3.3.0]: https://github.com/adorsys/keycloak-config-cli/compare/v3.2.0...v3.3.0
[3.2.0]: https://github.com/adorsys/keycloak-config-cli/compare/v3.1.3...v3.2.0
[3.1.3]: https://github.com/adorsys/keycloak-config-cli/compare/v3.1.2...v3.1.3
[3.1.2]: https://github.com/adorsys/keycloak-config-cli/compare/v3.1.1...v3.1.2
[3.1.1]: https://github.com/adorsys/keycloak-config-cli/compare/v3.1.0...v3.1.1
[3.1.0]: https://github.com/adorsys/keycloak-config-cli/compare/v3.0.0...v3.1.0
[3.0.0]: https://github.com/adorsys/keycloak-config-cli/compare/v2.6.3...v3.0.0
[2.6.3]: https://github.com/adorsys/keycloak-config-cli/compare/v2.6.2...v2.6.3
[2.6.2]: https://github.com/adorsys/keycloak-config-cli/compare/v2.6.1...v2.6.2
[2.6.1]: https://github.com/adorsys/keycloak-config-cli/compare/v2.6.0...v2.6.1
[2.6.0]: https://github.com/adorsys/keycloak-config-cli/compare/v2.5.0...v2.6.0
[2.5.0]: https://github.com/adorsys/keycloak-config-cli/compare/v2.4.0...v2.5.0
[2.4.0]: https://github.com/adorsys/keycloak-config-cli/compare/v2.3.0...v2.4.0
[2.3.0]: https://github.com/adorsys/keycloak-config-cli/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/adorsys/keycloak-config-cli/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.2...v2.1.0
[2.0.2]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/adorsys/keycloak-config-cli/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.4.0...v2.0.0
[1.4.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.3.1...v1.4.0
[1.3.1]: https://github.com/adorsys/keycloak-config-cli/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.2...v1.2.0
[1.1.2]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.1...v1.1.2
[1.1.1]: https://github.com/adorsys/keycloak-config-cli/compare/v1.1.0...v1.1.1
