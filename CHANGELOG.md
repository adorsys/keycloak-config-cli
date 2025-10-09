# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Fix events expiration setting in realm.json is ignored during realm import [#1230](https://github.com/adorsys/keycloak-config-cli/issues/1230)
- Fix 403 Forbidden errors in CI/CD for Keycloak 26.x [#1307](https://github.com/adorsys/keycloak-config-cli/issues/1307)

## [6.4.0] - 2025-02-21
### Added
- Allow a user's username to be updated through the config [#810](https://github.com/adorsys/keycloak-config-cli/issues/810)

## [6.3.0] - 2025-02-03
### Added
- Improve error logging for Keycloak responses  [1270](https://github.com/adorsys/keycloak-config-cli/issues/1270)

### Fixed
- Fix high level CVE (CVE-2024-38807)

### Fixed
- fix chart publish failure
### Added
- added migration guide for keycloak 25.0.1 [#1072](https://github.com/adorsys/keycloak-config-cli/issues/1072)


### Fixed
-  Fix Service Account User always triggers UPDATE USER event [#878](https://github.com/adorsys/keycloak-config-cli/issues/878)

### Added
- Publish charts with github pages [#941](https://github.com/adorsys/keycloak-config-cli/issues/941)
- Support for Keycloak 26.1
- Ignore unknown json properties from newer Keycloak versions  [#1265](https://github.com/adorsys/keycloak-config-cli/issues/1265)

### Fixed
-  Fix Initial Credentials Causes Update [819](https://github.com/adorsys/keycloak-config-cli/issues/819)


## [6.2.1] - 2024-12-05
### Fixed
- Fix ci failure actions/upload-artifact@v4 new public artifacts are no longer accessible
## [6.2.0] - 2024-12-03
### Fix Fails to delete authentication flow when it's referenced as an IdP [#868](https://github.com/adorsys/keycloak-config-cli/issues/868)
-
## Fixed
- otpPolicyAlgorithm ignored during import [#847](https://github.com/adorsys/keycloak-config-cli/issues/847)

### Added

- Added Navigation in the readme [#1187](https://github.com/adorsys/keycloak-config-cli/issues/1187)
### Added
- Improve documentation of managed resources, particularly user federations [#826](https://github.com/adorsys/keycloak-config-cli/issues/826)

- Added Navigation in the readme [#1099](https://github.com/adorsys/keycloak-config-cli/issues/1099)

### Added
- improved logging for realm retrieval errors [#1010](https://github.com/adorsys/keycloak-config-cli/issues/1010)
### Fixed
- Fix required action import handling for no-delete option [#834](https://github.com/adorsys/keycloak-config-cli/issues/834)


### Fixed
- Fix to manage Remote state import for clientscopes and scopeMappings [#1012](https://github.com/adorsys/keycloak-config-cli/issues/1012)

### Fixed
- Fixed to delete protocol mappers if not in the import[#746](https://github.com/orgs/adorsys/projects/5/views/1?pane=issue&itemId=80856370&issue=adorsys%7Ckeycloak-config-cli%7C746)

### Fixed
- Allow environment variables from existing secrets [#822](https://github.com/adorsys/keycloak-config-cli/issues/822)

### Fixed
- Fix  versioning in artifact to contain the correct keycloak version [#1097](https://github.com/adorsys/keycloak-config-cli/issues/1097)

- Updated CI to use Keycloak 26.0.5

### Fixed

- Allow executions of same provider with different configurations in Sub-Auth-Flows
- Fix enabling a realm clears the value of eventsExpiration
- Display names and icon URIs of authorization scopes are now imported alongside scope name

## [6.1.11] - 2024-10-14

- Fix env.JAVA_HOME test failures by ensuring env is set before build

## [6.1.10] - 2024-10-04


- Fixed securityContext entries in job template


- Added support for User Profile Setting: `unmanagedAttributePolicy`

- Crash after inserting more than 100 roles in realm-management authorization
  [#1090](/adorsys/keycloak-config-cli/issues/1090):

- NPE when using custom policy in AuthorizationPolicy [#1095](/adorsys/keycloak-config-cli/issues/1095):

- Fix Keycloak startup issue with admin-fine-grained-authz feature flag

## [6.1.7] - 2024-09-30

## [6.1.6] - 2024-07-26


## [6.1.5] - 2024-06-27

## [6.1.3] - 2024-06-27

## [6.1.2] - 2024-06-27

## [6.1.1] - 2024-06-27

## [6.1.0] - 2024-06-26
- Updated CI to use Keycloak 25.0.1
- Identity Providers are now updated using the name of policies, scopes and resources

### Added
- Alias docker tags without Keycloak minor/patch version

### Fixed
- Importing more than 10 subgroups into a realm

## [6.0.2] - 2024-06-17
- Restored versioning

## [6.0.1] - 2024-06-12

## [6.0.0] - 2024-06-10
- Changed Java target version and temurin to 21
- Several dependency updates
- Reassured compatibility with 19.0.3-legacy
- Updated CI to use Keycloak 24.0.5

### Breaking
- Upgrade to Spring Boot 3
    - This affects the capability of the path matcher

- Added option to calculate checksum for each import file ([#1015](https://github.com/adorsys/keycloak-config-cli/issues/1015))

## [5.12.0] - 2024-03-28
- Added support for managing message bundles

## [5.11.1] - 2024-03-12
- fixed github actions workflow permissions

## [5.11.0] - 2024-03-12
- Updated CI to use Keycloak 24.0.1
- Updated CI to use Keycloak 23.0.7
- Changed briefRepresentation from false to true (mistakenly considered full: [#25096](https://github.com/keycloak/keycloak/issues/25096))
  - Removes compatibility of Versions 23.0.0, 23.0.1, 23.0.2 and 23.0.3
- Using getGroupByPath again after being fixed ([#25111](https://github.com/keycloak/keycloak/issues/25111))

### Fixed
- Corrected name of CLI option `--import.files.locations` in docs

### Fixed
- The client policies in the configuration are applied during client import and configuration.

## [5.10.0] - 2023-12-12
- Updated CI to use Keycloak 23.0.1
- Added correct spelling of "authenticatorFlow" in all import files
- Treating default-roles-${realm} as default role even with changed description
- Supporting new group handling, also loading sub groups manually
- Supporting new user profile configuration (UPConfig)
- Removed deprecated authenticator "registration-profile-action" from tests
- Extended maven-replacer-plugin with breaking changes
- Using util classes as replacement strategy for breaking changes (GroupUtil, SubGroupUtil)

## [5.9.0] - 2023-10-13
- Updated CI to use Keycloak 22.0.4

### Added
- Support for managing `Client Authorization Policies` like other resources by configuring `import.managed.client-authorization-policies=<full|no-delete>`. This prevents deletion of remote managed policies.
- Support for managing `Client Authorization Scopes` like other resources by configuring `import.managed.client-authorization-scopes=<full|no-delete>`. This prevents deletion of remote managed scopes.

### Changed
- Fix issue [#907](/adorsys/keycloak-config-cli/issues/907):
  - add missing dependency jakarta.xml.bind-api
  - lower logstash version from 7.4 to 7.2: 7.2 is the last version to support logback 1.2, which is the version required for Spring and other components used here
## [5.8.0] - 2023-07-14

### Added
- Support for Keycloak 22

### Changed
- Migrated from Java EE to Jakarta EE
- Migrated imports of javax packages to jakarta packages
- Upgraded Spring Boot to 2.7.13

## [5.7.0] - 2023-07-14

### Changed
- Refactored support for user profile updates
- Attribute groups are now allowed in the `userProfile` property in json import. The format to import User Declarative Profile attributes (and attribute groups) has slightly changed. To migrate to the new format:
    - transform the `userProfile` property to a JSON object with two properties: `attributes` and `groups`
    - copy the JSON array of the old `userProfile` property to the new `userProfile.attributes` property
    - create a new JSON array for the `userProfile.groups` property (containing the attribute groups definitions)
    - in the end, the `userProfile` property should match the content of the "JSON editor" tab in the "Realm settings > User profile" page from the Keycloak admin console
- Add support for managing client-policies

## [5.6.1] - 2023-03-05

## [5.6.0] - 2023-03-05

### Added
- Added support for keycloak 21

### Changed
- Upgraded to latest keycloak 20 bugfix version

### Fixed
- Consider all authentication subflows during updates.

## [5.5.0] - 2022-11-12

### Added
- Added support for keycloak 20
- Realm export scripts now use the new kc.sh export command

### Removed
- Support for Keycloak 16

## [5.4.0] - 2022-11-07

### Added
- Added latest Keycloak 19.0.3 library
- Added support for managing user profiles

## [5.3.1] - 2022-08-02

### Added
- Added latest Keycloak 19.0.1 library

## [5.3.0] - 2022-07-28

### Added
- Support for Keycloak 19

### Removed
- Support for Keycloak 15

## [5.2.2] - 2022-07-25

### Added
- Added latest Keycloak 18.0.2 library

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

[Unreleased]: https://github.com/adorsys/keycloak-config-cli/compare/v6.4.0...HEAD
[6.4.0]: https://github.com/adorsys/keycloak-config-cli/compare/v6.3.0...v6.4.0
[6.3.0]: https://github.com/adorsys/keycloak-config-cli/compare/v6.2.1...v6.3.0
[6.2.1]: https://github.com/adorsys/keycloak-config-cli/compare/v6.2.0...v6.2.1
[6.2.0]: https://github.com/adorsys/keycloak-config-cli/compare/vFixed...v6.2.0
[Fixed]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.11...vFixed
[6.1.11]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.10...v6.1.11
[6.1.10]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.7...v6.1.10
[6.1.7]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.6...v6.1.7
[6.1.6]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.5...v6.1.6
[6.1.5]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.3...v6.1.5
[6.1.3]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.2...v6.1.3
[6.1.2]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.1...v6.1.2
[6.1.1]: https://github.com/adorsys/keycloak-config-cli/compare/v6.1.0...v6.1.1
[6.1.0]: https://github.com/adorsys/keycloak-config-cli/compare/v6.0.2...v6.1.0
[6.0.2]: https://github.com/adorsys/keycloak-config-cli/compare/v6.0.1...v6.0.2
[6.0.1]: https://github.com/adorsys/keycloak-config-cli/compare/v6.0.0...v6.0.1
[6.0.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.12.0...v6.0.0
[5.12.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.11.1...v5.12.0
[5.11.1]: https://github.com/adorsys/keycloak-config-cli/compare/v5.11.0...v5.11.1
[5.11.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.10.0...v5.11.0
[5.10.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.9.0...v5.10.0
[5.9.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.8.0...v5.9.0
[5.8.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.7.0...v5.8.0
[5.7.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.6.1...v5.7.0
[5.6.1]: https://github.com/adorsys/keycloak-config-cli/compare/v5.6.0...v5.6.1
[5.6.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.5.0...v5.6.0
[5.5.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.4.0...v5.5.0
[5.4.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.3.1...v5.4.0
[5.3.1]: https://github.com/adorsys/keycloak-config-cli/compare/v5.3.0...v5.3.1
[5.3.0]: https://github.com/adorsys/keycloak-config-cli/compare/v5.2.2...v5.3.0
[5.2.2]: https://github.com/adorsys/keycloak-config-cli/compare/v5.2.1...v5.2.2
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
