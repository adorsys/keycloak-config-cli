[![CI](https://github.com/adorsys/keycloak-config-cli/workflows/CI/badge.svg)](https://github.com/adorsys/keycloak-config-cli/actions?query=workflow%3ACI)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/adorsys/keycloak-config-cli?logo=github&sort=semver)](https://github.com/adorsys/keycloak-config-cli/releases/latest)
[![GitHub All Releases](https://img.shields.io/github/downloads/adorsys/keycloak-config-cli/total?logo=github)](https://github.com/adorsys/keycloak-config-cli/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/adorsys/keycloak-config-cli?logo=docker)](https://hub.docker.com/r/adorsys/keycloak-config-cli)
[![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/main/graph/badge.svg)](https://codecov.io/gh/adorsys/keycloak-config-cli)
[![GitHub license](https://img.shields.io/github/license/adorsys/keycloak-config-cli)](https://github.com/adorsys/keycloak-config-cli/blob/main/LICENSE.txt)

# keycloak-config-cli

keycloak-config-cli is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file. The format of the JSON/YAML file based on the export realm format. Store and handle the configuration files inside git just like normal code. A Keycloak restart isn't required to apply the configuration.

# Config files

The config files are based on the keycloak export files. You can use them to re-import your settings.
But keep your files as small as possible. Remove all UUIDs and all stuff which is default set by keycloak.

[moped.json](./contrib/example-config/moped.json) is a full working example file you can consider.
Other examples are located in the [test resources](./src/test/resources/import-files).

## Variable Substitution

keycloak-config-cli supports variable substitution of config files. This could be enabled by `import.var-substitution=true` (**disabled by default**).
Use substitutions like

```
Base64 Decoder:        ${base64Decoder:SGVsbG9Xb3JsZCE=}
Base64 Encoder:        ${base64Encoder:HelloWorld!}
Java Constant:         ${const:java.awt.event.KeyEvent.VK_ESCAPE}
Date:                  ${date:yyyy-MM-dd}
DNS:                   ${dns:address|apache.org}
Environment Variable:  ${env:USERNAME}
File Content:          ${file:UTF-8:src/test/resources/document.properties}
Java:                  ${java:version}
Localhost:             ${localhost:canonical-name}
Properties File:       ${properties:src/test/resources/document.properties::mykey}
Resource Bundle:       ${resourceBundle:org.example.testResourceBundleLookup:mykey}
Script:                ${script:javascript:3 + 4}
System Property:       ${sys:user.dir}
URL Decoder:           ${urlDecoder:Hello%20World%21}
URL Encoder:           ${urlEncoder:Hello World!}
URL Content (HTTP):    ${url:UTF-8:http://www.apache.org}
URL Content (HTTPS):   ${url:UTF-8:https://www.apache.org}
URL Content (File):    ${url:UTF-8:file:///${sys:user.dir}/src/test/resources/document.properties}
XML XPath:             ${xml:src/test/resources/document.xml:/root/path/to/node}
```

to replace the values with java system properties or environment variables. Recursive variable replacement like `${file:UTF-8:${env:KEYCLOAK_PASSWORD_FILE}}` is enabled by default if `import.var-substitution` is set to `true`.

The variable substitution is running before the json parser gets executed. This allows json structures or complex values.

See [Apache Common StringSubstitutor documentation](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html) for more information and advanced usage.

**Note**: Since variable substitution is a part of the keycloak-config-cli, it's done locally. This means, the environment variables need to be availible where keycloak-config-cli is executed.

# Supported features

See: [docs/FEATURES.md](./docs/FEATURES.md)

# Compatibility matrix

| keycloak-config-cli | **Keycloak 4 - 7** | **Keycloak 8** | **Keycloak 9 - 11** | **Keycloak 12** |
| ------------------- | :----------------: | :------------: | :-----------------: | :-------------: |
| **v0.8.x**          |         ✓          |       ✗        |          ✗          |        ✗        |
| **v1.0.x - v2.6.x** |         ✗          |       ✓        |          ✓          |        ✗        |
| **v3.0.x - v3.0.x** |         ✗          |       ✗        |          ✓          |        ✓        |
| **main**            |         ✗          |       ✗        |          ✓          |        ✓        |

- `✓` Supported
- `✗` Not supported

# Build this project

```shell script
mvn package
```

# Run integration tests against real keycloak

We are using [TestContainers](https://www.testcontainers.org/) in our integration tests.
To run the integration tests a configured docker environment is required.

```shell script
mvn verify
```

# Run this project

## via Maven

Start a local keycloak on port 8080:

```shell script
docker-compose down --remove-orphans && docker-compose up keycloak
```

before performing following command:

```shell script
java -jar ./target/keycloak-config-cli.jar \
    --keycloak.url=http://localhost:8080/auth \
    --keycloak.ssl-verify=true \
    --keycloak.user=admin \
    --keycloak.password=admin123 \
    # default usage: local files
    --import.path=./contrib/example-config/moped.json

    # alternative usage: local directory
    # --import.path=./contrib/example-config-directory/
    # alternative usage: remote file
    # --import.path=http://hostname/moped.json
    # alternative usage: remote resource and basic auth
    # --import.path=http://user:pass@hostname/moped.json
```

## Docker

The docker tag `latest` points to the latest available release while `edge` points to the latest commit on the main branch.

### Docker run

For docker `-e` you have to replace dots with underscores.

```shell script
docker run \
    -e KEYCLOAK_URL=http://<your keycloak host>:8080/auth \
    -e KEYCLOAK_USER=<keycloak admin username> \
    -e KEYCLOAK_PASSWORD=<keycloak admin password> \
    -e KEYCLOAK_AVAILABILITYCHECK_ENABLED=true \
    -e KEYCLOAK_AVAILABILITYCHECK_TIMEOUT=120s \
    -e IMPORT_PATH=/config \
    -e IMPORT_FORCE=false \
    -v <your config path>:/config \
    adorsys/keycloak-config-cli:latest
```

## Helm

We provide a helm chart [here](./contrib/charts/keycloak-config-cli).

Since it make no sense to deploy keycloak-config-cli as standalone application, you could
add it as dependency to your chart deployment.

Checkout helm docs about [chart dependencies](https://helm.sh/docs/topics/charts/#chart-dependencies)!

# CLI option / Environment Variables

| CLI Option                            | ENV Variable                       | Description                                                                       | Default     | Docs                                                                                                                            |
| ------------------------------------- | ---------------------------------- | --------------------------------------------------------------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------- |
| --keycloak.url                        | KEYCLOAK_URL                       | Keycloak URL including web context. Format: `scheme://hostname:port/web-context`. | -           |                                                                                                                                 |
| --keycloak.user                       | KEYCLOAK_USER                      | login user name                                                                   | `admin`     |                                                                                                                                 |
| --keycloak.password                   | KEYCLOAK_PASSWORD                  | login user password                                                               | -           |                                                                                                                                 |
| --keycloak.client-id                  | KEYCLOAK_CLIENTID                  | login clientId                                                                    | `admin-cli` |                                                                                                                                 |
| --keycloak.client-secret              | KEYCLOAK_CLIENTSECRET              | login client secret                                                               | -           |                                                                                                                                 |
| --keycloak.grant-type                 | KEYCLOAK_GRANTTYPE                 | login grant_type                                                                  | `password`  |                                                                                                                                 |
| --keycloak.login-realm                | KEYCLOAK_LOGINREALM                | login realm                                                                       | `master`    |                                                                                                                                 |
| --keycloak.ssl-verify                 | KEYCLOAK_SSLVERIFY                 | Verify ssl connection to keycloak                                                 | `true`      |                                                                                                                                 |
| --keycloak.http-proxy                 | KEYCLOAK_HTTPPROXY                 | Connect to Keycloak via HTTP Proxy. Format: `scheme://hostname:port`              | -           |                                                                                                                                 |
| --keycloak.availability-check.enabled | KEYCLOAK_AVAILABILITYCHECK_ENABLED | Wait until Keycloak is available                                                  | `false`     |                                                                                                                                 |
| --keycloak.availability-check.timeout | KEYCLOAK_AVAILABILITYCHECK_TIMEOUT | Wait timeout for keycloak availability check                                      | `120s`      |                                                                                                                                 |
| --import.path                         | IMPORT_PATH                        | Location of config files (if location is a directory, all files will be imported) | `/config`   | [Spring ResourceLoader](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#resources-resourceloader) |
| --import.var-substitution             | IMPORT_VARSUBSTITUTION             | Enable variable substitution config files                                         | `false`     |                                                                                                                                 |
| --import.force                        | IMPORT_FORCE                       | Enable force import of realm config                                               | `false`     |                                                                                                                                 |
| --import.cache-key                    | IMPORT_CACHEKEY                    | Cache key for importing config.                                                   | `default`   |                                                                                                                                 |
| --import.state                        | IMPORT_STATE                       | Enable state management. Purge only resources managed by kecloak-config-cli. S.   | `true`      | [MANAGED.md](docs/MANAGED.md)                                                                                                   |
| --import.state-encryption-key         | IMPORT_STATEENCRYPTIONKEY          | Enables state in encrypted format. If unset, state will be stored in plain        | -           |                                                                                                                                 |
| --import.file-type                    | IMPORT_FILETYPE                    | Format of the configuration import file. Allowed values: AUTO,JSON,YAML           | `auto`      |                                                                                                                                 |
| --import.parallel                     | IMPORT_PARALLEL                    | Enable parallel import of certain resources                                       | `false`     |                                                                                                                                 |

See [application.properties](src/main/resources/application.properties) for all available settings.

For docker `-e` you have to remove hyphens and replace dots with underscores.

Take a look at [spring relax binding](https://github.com/spring-projects/spring-boot/wiki/Relaxed-Binding-2.0) if you need
alternative spellings.

## Configure properties values through files

_Available since keycloak-config-cli 2.6.3._

By define an environment variable `SPRING_CONFIG_IMPORT=configtree:/run/secrets/`, the values of properties can be provided via files instead of plain
environment variable values.

Example: To configure the property `keycloak.password` in this case, the file should be in `/run/secrets/keycloak.password`.

The configuration and secret support in Docker Swarm is a perfect match for this use case.

Checkout the [spring docs](https://docs.spring.io/spring-boot/docs/2.4.2/reference/html/spring-boot-features.html#boot-features-external-config-files-configtree)
to get more information about the configuration trees feature in spring boot.

# Perform release

Create release via [maven release plugin](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html):

```shell script
mvn -Dresume=false release:prepare release:clean
git push --follow-tags
```
