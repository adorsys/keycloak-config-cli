[![CI](https://github.com/adorsys/keycloak-config-cli/workflows/CI/badge.svg)](https://github.com/adorsys/keycloak-config-cli/actions?query=workflow%3ACI)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/adorsys/keycloak-config-cli?logo=github&sort=semver)](https://github.com/adorsys/keycloak-config-cli/releases/latest)
[![GitHub All Releases](https://img.shields.io/github/downloads/adorsys/keycloak-config-cli/total?logo=github)](https://github.com/adorsys/keycloak-config-cli/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/adorsys/keycloak-config-cli?logo=docker)](https://hub.docker.com/r/adorsys/keycloak-config-cli)
[![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/main/graph/badge.svg)](https://codecov.io/gh/adorsys/keycloak-config-cli)
[![GitHub license](https://img.shields.io/github/license/adorsys/keycloak-config-cli)](https://github.com/adorsys/keycloak-config-cli/blob/main/LICENSE.txt)

# Table of Contents

- [Config Files](#config-files)
- [Variable Substitution](#variable-substitution)
- [Logging](#logging)
- [Supported Features](#supported-features)
- [Compatibility with Keycloak](#compatibility-with-keycloak)
- [Build this Project](#build-this-project)
- [Run Integration Tests](#run-integration-tests)
- [Run this Project](#run-this-project)
- [Docker](#docker)
- [Helm](#helm)
- [Configuration](#configuration)
- [Perform Release](#perform-release)
- [Commercial Support](#commercial-support)

# keycloak-config-cli

keycloak-config-cli is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file. The format of the JSON/YAML file based on the export realm format. Store and handle the configuration files inside git just like normal code. A Keycloak restart isn't required to apply the configuration.

# Config files

The config files are based on the keycloak export files. You can use them to re-import your settings. But keep your files as small as possible. Remove all UUIDs and all stuff which is default set by keycloak.

[moped.json](./contrib/example-config/moped.json) is a full working example file you can consider. Other examples are located in the [test resources](./src/test/resources/import-files).

## Variable Substitution

keycloak-config-cli supports variable substitution of config files. This could be enabled by `import.var-substitution.enabled=true` (**disabled by default**).

Variables exposed by spring boot (through configtree or [external configuration](https://docs.spring.io/spring-boot/docs/2.5.0/reference/htmlsingle/#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables)) can be accessed by `$(property.name)`.

In additional, the string substitution support multiple prefixes for different approaches

```
Base64 Decoder:        $(base64Decoder:SGVsbG9Xb3JsZCE=)
Base64 Encoder:        $(base64Encoder:HelloWorld!)
Java Constant:         $(const:java.awt.event.KeyEvent.VK_ESCAPE)
Date:                  $(date:yyyy-MM-dd)
DNS:                   $(dns:address|apache.org)
Environment Variable:  $(env:USERNAME)
File Content:          $(file:UTF-8:src/test/resources/document.properties)
Java:                  $(java:version)
Localhost:             $(localhost:canonical-name)
Properties File:       $(properties:src/test/resources/document.properties::mykey)
Resource Bundle:       $(resourceBundle:org.example.testResourceBundleLookup:mykey)
Script:                $(script:javascript:3 + 4)
System Property:       $(sys:user.dir)
URL Decoder:           $(urlDecoder:Hello%20World%21)
URL Encoder:           $(urlEncoder:Hello World!)
URL Content (HTTP):    $(url:UTF-8:http://www.apache.org)
URL Content (HTTPS):   $(url:UTF-8:https://www.apache.org)
URL Content (File):    $(url:UTF-8:file:///$(sys:user.dir)/src/test/resources/document.properties)
XML XPath:             $(xml:src/test/resources/document.xml:/root/path/to/node)
```

to replace the values with java system properties or environment variables. Recursive variable replacement like `$(file:UTF-8:$(env:KEYCLOAK_PASSWORD_FILE))` is enabled by default if `import.var-substitution.enabled` is set to `true`.

The variable substitution is running before the json parser gets executed. This allows json structures or complex values.

See [Apache Common `StringSubstitutor` documentation](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) for more information and advanced usage.

**Note**: Since variable substitution is a part of the keycloak-config-cli, it's done locally. This means, the environment variables need to be available where keycloak-config-cli is executed.

If `import.var-substitution.prefix=${` and `import.var-substitution.suffix=}` (default in keycloak-config-cli 3.x) is set, then keycloak builtin variables like `${role_uma_authorization}` needs to be escaped by `$${role_uma_authorization}`.

# Logging

## JSON logging support

keycloak-config-cli supports logging in JSON format. To enable, set `SPRING_PROFILES_ACTIVE=json-log`.

## Log level

| CLI Option                          | ENV Variable                    | Description                                                                          | Default                       |
|-------------------------------------|---------------------------------|--------------------------------------------------------------------------------------|-------------------------------|
| --logging.level.root                | LOGGING_LEVEL_ROOT              | define the root log level                                                            | `info`                        |
| --logging.level.keycloak-config-cli | LOGGING_LEVEL_KEYCLOAKCONFIGCLI | log level of keycloak-config-cli components                                          | value of `logging.level.root` |
| --logging.level.http                | LOGGING_LEVEL_HTTP              | log level http requests between keycloak-config-cli and Keycloak                     | value of `logging.level.root` |
| --logging.level.realm-config        | LOGGING_LEVEL_REALMCONFIG       | if set to trace, the realm config including **sensitive information** will be logged | value of `logging.level.root` |

# Supported features

See: [docs/FEATURES.md](./docs/FEATURES.md)

# Compatibility with keycloak

Since keycloak-config-cli 4.0 will support the latest 4 releases of keycloak, if possible.
There are some exceptions:

- keycloak-config-cli will try the keep an extended support for [RH-SSO](https://access.redhat.com/articles/2342881)
- keycloak-config-cli will cut the support if keycloak introduces some breaking changes

# Build this project

keycloak-config-cli using [maven](https://maven.apache.org/index.html) to build and test keycloak-config-cli.
In case maven is not installed on your system, the [`mvnw`](https://github.com/takari/maven-wrapper) command will download maven for you.

Further development requirements
- Java Development Kit (JDK)
- Docker Desktop or an alternative replacement (e.g Rancher Desktop)

Before running `mvn verify`, you have to set the JAVA_HOME environment variable to prevent some test failures.

```shell
./mvnw verify

# Windows only
mvnw.cmd verify
```

If your are working with a Docker Desktop replacement, some of the Integrationtests can fail due to internal DNS Lookups (host.docker.internal is not reachable).
In this case the host can be replaced by a property.

```shell script
mvn verify -DJUNIT_LDAP_HOST=an.alternate.host.or.ip
```

# Run integration tests against real keycloak

We are using [TestContainers](https://www.testcontainers.org/) in our integration tests. To run the integration tests a configured docker environment
is required.

```shell script
./mvnw verify

# Windows only
mvnw.cmd verify
```

# Run this project

Start a local keycloak on port 8080:

```shell script
docker-compose down --remove-orphans && docker-compose up keycloak
```

before performing following command:

```shell script
java -jar ./target/keycloak-config-cli.jar \
    --keycloak.url=http://localhost:8080 \
    --keycloak.ssl-verify=true \
    --keycloak.user=admin \
    --keycloak.password=admin123 \
    --import.files.locations=./contrib/example-config/moped.json
```

## Docker

A docker images is available at [DockerHub](https://hub.docker.com/r/adorsys/keycloak-config-cli) (docker.io/adorsys/keycloak-config-cli)
and [quay.io](https://quay.io/repository/adorsys/keycloak-config-cli) (quay.io/adorsys/keycloak-config-cli)

Available docker tags

| Tag            | Description                                                                                                   |
|----------------|---------------------------------------------------------------------------------------------------------------|
| `latest`       | latest available release of keycloak-config-cli which is built against the latest supported Keycloak release. |
| `latest-x.y.z` | latest available release of keycloak-config-cli which is built against the Keycloak version `x.y.z`.          |
| `edge`         | latest commit on the main branch and which is built against the latest supported Keycloak release.            |
| `a.b.c`        | keycloak-config-cli version `a.b.c` which is built against the latest supported Keycloak release.             |
| `a.b.c-x.y.z`  | keycloak-config-cli version `a.b.c` which is built against the Keycloak version `x.y.z`.                      |
| `maven`        | See below                                                                                                     |

Additionally, the tag `maven` contains the source code and compile keycloak-config-cli at runtime. This has the advantage to keycloak-config-cli with
Keycloak versions, that not official supported., e.g.:

```bash
docker run --rm -ti -v $PWD:/config/ -eKEYCLOAK_VERSION=23.0.1 -eMAVEN_CLI_OPTS="-B -ntp -q" adorsys/keycloak-config-cli:edge-build
```

### Docker run

For docker `-e` you have to replace dots with underscores.

```shell script
docker run \
    -e KEYCLOAK_URL="http://<your keycloak host>:8080/" \
    -e KEYCLOAK_USER="<keycloak admin username>" \
    -e KEYCLOAK_PASSWORD="<keycloak admin password>" \
    -e KEYCLOAK_AVAILABILITYCHECK_ENABLED=true \
    -e KEYCLOAK_AVAILABILITYCHECK_TIMEOUT=120s \
    -e IMPORT_FILES_LOCATIONS='/config/*' \
    -v <your config path>:/config \
    adorsys/keycloak-config-cli:latest
```

### Docker build

You can build an own docker image by running

```shell
docker build -t keycloak-config-cli .
```

## Helm

We provide a helm chart [here](./contrib/charts/keycloak-config-cli).

Since it makes no sense to deploy keycloak-config-cli as standalone application, you could add it as dependency to your chart deployment.

Checkout helm docs about [chart dependencies](https://helm.sh/docs/topics/charts/#chart-dependencies)!

# Configuration

## CLI option / Environment Variables

### Keycloak options

| CLI Option                            | ENV Variable                         | Description                                                                       | Default     | Docs                                                                                             |
|---------------------------------------|--------------------------------------|-----------------------------------------------------------------------------------|-------------|--------------------------------------------------------------------------------------------------|
| --keycloak.url                        | `KEYCLOAK_URL`                       | Keycloak URL including web context. Format: `scheme://hostname:port/web-context`. | -           |                                                                                                  |
| --keycloak.user                       | `KEYCLOAK_USER`                      | login user name                                                                   | `admin`     |                                                                                                  |
| --keycloak.password                   | `KEYCLOAK_PASSWORD`                  | login user password                                                               | -           |                                                                                                  |
| --keycloak.client-id                  | `KEYCLOAK_CLIENTID`                  | login clientId                                                                    | `admin-cli` |                                                                                                  |
| --keycloak.client-secret              | `KEYCLOAK_CLIENTSECRET`              | login client secret                                                               | -           |                                                                                                  |
| --keycloak.grant-type                 | `KEYCLOAK_GRANTTYPE`                 | login grant_type                                                                  | `password`  |                                                                                                  |
| --keycloak.login-realm                | `KEYCLOAK_LOGINREALM`                | login realm                                                                       | `master`    |                                                                                                  |
| --keycloak.ssl-verify                 | `KEYCLOAK_SSLVERIFY`                 | Verify ssl connection to keycloak                                                 | `true`      |                                                                                                  |
| --keycloak.http-proxy                 | `KEYCLOAK_HTTPPROXY`                 | Connect to Keycloak via HTTP Proxy. Format: `scheme://hostname:port`              | -           |                                                                                                  |
| --keycloak.connect-timeout            | `KEYCLOAK_CONNECTTIMEOUT`            | Connection timeout                                                                | `10s`       |                                                                                                  |
| --keycloak.read-timeout               | `KEYCLOAK_READTIMEOUT`               | Read timeout                                                                      | `10s`       | configured as [Java Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) |
| --keycloak.availability-check.enabled | `KEYCLOAK_AVAILABILITYCHECK_ENABLED` | Wait until Keycloak is available                                                  | `false`     | configured as [Java Duration](https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html) |
| --keycloak.availability-check.timeout | `KEYCLOAK_AVAILABILITYCHECK_TIMEOUT` | Wait timeout for keycloak availability check                                      | `120s`      |                                                                                                  |

### Import options

| CLI Option                                            | ENV Variable                                       | Description                                                                                                                                                                                                                                                                                                                                                                                                                        | Default    | Docs                          |
|-------------------------------------------------------|----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|-------------------------------|
| --import.validate                                     | `IMPORT_VALIDATE`                                  | Validate configuration settings                                                                                                                                                                                                                                                                                                                                                                                                    | `false`    |                               |
| --import.parallel                                     | `IMPORT_PARALLEL`                                  | Enable parallel import of certain resources                                                                                                                                                                                                                                                                                                                                                                                        | `false`    |                               |
| --import.files.locations                              | `IMPORT_FILES_LOCATIONS`                           | Location of config files (URL, file path, or Ant-style pattern)                                                                                                                                                                                                                                                                                                                                                                    | -          | [IMPORT.md](docs/IMPORT.md)   |
| --import.files.include-hidden-files                   | `IMPORT_FILES_INCLUDE_HIDDEN_FILES`                | Includes files that marked as hidden                                                                                                                                                                                                                                                                                                                                                                                               | `false`    |                               |
| --import.files.excludes                               | `IMPORT_FILES_EXCLUDES`                            | Exclude files with Ant-style pattern                                                                                                                                                                                                                                                                                                                                                                                               | -          |                               |
| --import.cache.enabled                                | `IMPORT_CACHE_ENABLED`                             | Enable caching of import file locations                                                                                                                                                                                                                                                                                                                                                                                            | `true`     |                               |
| --import.cache.key                                    | `IMPORT_CACHE_KEY`                                 | Cache key for importing config.                                                                                                                                                                                                                                                                                                                                                                                                    | `default`  |                               |
| --import.remote-state.enabled                         | `IMPORT_REMOTESTATE_ENABLED`                       | Enable remote state management. Purge only resources managed by keycloak-config-cli.                                                                                                                                                                                                                                                                                                                                               | `true`     | [MANAGED.md](docs/MANAGED.md) |
| --import.remote-state.encryption-key                  | `IMPORT_REMOTESTATE_ENCRYPTIONKEY`                 | Enables remote state in encrypted format. If unset, state will be stored in plain                                                                                                                                                                                                                                                                                                                                                  | -          |                               |
| --import.var-substitution.enabled                     | `IMPORT_VARSUBSTITUTION_ENABLED`                   | Enable variable substitution config files                                                                                                                                                                                                                                                                                                                                                                                          | `false`    |                               |
| --import.var-substitution.nested                      | `IMPORT_VARSUBSTITUTION_NESTED`                    | Expand variables in variables.                                                                                                                                                                                                                                                                                                                                                                                                     | `true`     |                               |
| --import.var-substitution.undefined-is-error          | `IMPORT_VARSUBSTITUTION_UNDEFINEDISTERROR`         | Raise exceptions, if variables are not defined.                                                                                                                                                                                                                                                                                                                                                                                    | `true`     |                               |
| --import.var-substitution.prefix                      | `IMPORT_VARSUBSTITUTION_PREFIX`                    | Configure the variable prefix, if `import.var-substitution.enabled` is `true`.                                                                                                                                                                                                                                                                                                                                                     | `$(`       |                               |
| --import.var-substitution.suffix                      | `IMPORT_VARSUBSTITUTION_SUFFIX`                    | Configure the variable suffix, if `import.var-substitution.enabled` is `true`.                                                                                                                                                                                                                                                                                                                                                     | `)`        |                               |
| --import.behaviors.sync-user-federation               | `IMPORT_BEHAVIORS_SYNC_USER_FEDERATION`            | Enable the synchronization of user federation.                                                                                                                                                                                                                                                                                                                                                                                     | `false`    |                               |
| --import.behaviors.remove-default-role-from-user      | `IMPORT_BEHAVIORS_REMOVEDEFAULTROLEFROMUSER`       | The default setting of this flag prevents keycloak-config-cli from removing `default-roles-$REALM`, even if its not defined in the import json. To make keycloak-config-cli able to remove the `default-role-$REALM`, `import.remove-default-role-from-user` must be set to true. In conclusion, you have to add the `default-role-$REALM` to the realm import on certain users, if you want not remove the `default-role-$REALM`. | `false`    |                               |
| --import.behaviors.skip-attributes-for-federated-user | `IMPORT_BEHAVIORS_SKIP_ATTRIBUTESFORFEDERATEDUSER` | Set attributes to null for federated users to avoid read only conflicts                                                                                                                                                                                                                                                                                                                                                            | `false`    |                               |
| --import.behaviors.checksum-with-cache-key            | `IMPORT_BEHAVIORS_CHECKSUM_WITH_CACHE_KEY`         | Use cache key to store the checksum, if set to `false` a checksum for each import file is stored                                                                                                                                                                                                                                                                                                                                   | `true`     |                               |
| --import.behaviors.checksum-changed                   | `IMPORT_BEHAVIORS_CHECKSUM_CHANGED`                | Defines the behavior if the checksum of an imported file has changed. Set to `fail` when import should be aborted, `continue` reimport and update the checksum.                                                                                                                                                                                                                                                                    | `continue` |                               |

## Spring boot options

| CLI Option               | ENV Variable             | Description                             | Default | Docs                                                                                                                                                                      |
|--------------------------|--------------------------|-----------------------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| --spring.profiles.active | `SPRING_PROFILES_ACTIVE` | enable spring profiles. comma separated | `-`     | [Set the Active Spring Profiles](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.properties-and-configuration.set-active-spring-profiles) |
| --spring.config.import   | `SPRING_CONFIG_IMPORT`   | See below                               | `info`  | [Configure properties values through files](#configure-properties-values-through-files)                                                                                   |
| --logging.level.root     | `LOGGING_LEVEL_ROOT`     | define the root log level               | `info`  | [Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.logging)                                                                        |
| --debug                  | `DEBUG`                  | enables debug mode of spring boot       | `false` |                                                                                                                                                                           |

See [application.properties](src/main/resources/application.properties) for all available settings.

For docker `-e` you have to remove hyphens and replace dots with underscores.

Take a look at [spring relax binding](https://github.com/spring-projects/spring-boot/wiki/Relaxed-Binding-2.0) or
[binding from Environment Variables](https://docs.spring.io/spring-boot/docs/2.6.0/reference/htmlsingle/#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables)
if you need alternative spellings.

## Configure properties values through files

By define an environment variable `SPRING_CONFIG_IMPORT=configtree:/run/secrets/`, the values of properties can be provided via files instead of plain
environment variable values.

Example: To configure the property `keycloak.password` in this case, the file should be in `/run/secrets/keycloak.password`.

The configuration and secret support in Docker Swarm is a perfect match for this use case.

Checkout the [spring docs](https://docs.spring.io/spring-boot/docs/2.6.0/reference/htmlsingle/#features.external-config.files.configtree)
to get more information about the configuration trees feature in spring boot.

# Perform release

Building releases requires gpg signing.

Example to create and add a key to yout git config on MacOS

```shell script
brew install gnupg
gpg --version
gpg --full-generate-key
# follow instructions
gpg --list-keys
gpg --list-secret-keys --keyid-format=short
# check the 8 digit code eg "ssb   xxxxxxx/E51442F5 2022-01-01 [X]"
git config --global user.signingkey E51442F5
```

Finally add the key to your Github account under Settings -> SSH and GPG keys -> New GPG key

Create release via [maven release plugin](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html):

```shell script
./mvnw -Dresume=false release:prepare release:clean
git push --follow-tags
```

# Commercial support

Checkout https://adorsys.com/en/products/keycloak-config-cli/ for commercial support.
