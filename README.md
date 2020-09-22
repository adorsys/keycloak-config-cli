[![CI](https://github.com/adorsys/keycloak-config-cli/workflows/CI/badge.svg)](https://github.com/adorsys/keycloak-config-cli/actions?query=workflow%3ACI)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/adorsys/keycloak-config-cli?logo=github&sort=semver)](https://github.com/adorsys/keycloak-config-cli/releases/latest)
[![GitHub All Releases](https://img.shields.io/github/downloads/adorsys/keycloak-config-cli/total?logo=github)](https://github.com/adorsys/keycloak-config-cli/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/adorsys/keycloak-config-cli?logo=docker)](https://hub.docker.com/r/adorsys/keycloak-config-cli)
[![Maintainability](https://api.codeclimate.com/v1/badges/bd89704bfacbe1fcd215/maintainability)](https://codeclimate.com/github/adorsys/keycloak-config-cli/maintainability) [![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/master/graph/badge.svg)](https://codecov.io/gh/adorsys/keycloak-config-cli)
[![GitHub license](https://img.shields.io/github/license/adorsys/keycloak-config-cli)](https://github.com/adorsys/keycloak-config-cli/blob/master/LICENSE.txt)

# keycloak-config-cli

keycloak-config-cli is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON/YAML file. The format of the JSON/YAML file based on the export realm format. Store and handle the configuration files inside git just like normal code. A Keycloak restart isn't required to apply the configuration.

## Config files

The config files are based on the keycloak export files. You can use them to re-import your settings.
But keep your files as small as possible. Remove all UUIDs and all stuff which is default set by keycloak.

[moped.json](./contrib/example-config/moped.json) is a full working example file you can consider.
Other examples are located in the [test resources](./src/test/resources/import-files).

## Supported features

See: [docs/FEATURES.md](./docs/FEATURES.md)

## Compatibility matrix

| keycloak-config-cli | **Keycloak 4.x - Keycloak 7.x** | **Keycloak 8.x - 11.x** |
| ------------------- | :-----------------------------: | :---------------------: |
| **v0.8.x**          |                ✓                |            ✗            |
| **v1.0.x - v2.2.x** |                ✗                |            ✓            |
| **master**          |                ✗                |            ✓            |

- `✓` Supported
- `✗` Not supported

## Build this project

```shell script
mvn package
```

## Run integration tests against real keycloak

We are using [TestContainers](https://www.testcontainers.org/) in our integration tests.
To run the integration tests a configured docker environment is required.

```shell script
mvn verify
```

## Run this project

### via Maven

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
    --import.path=./contrib/example-config/moped.json
```

### Docker

The docker tag `master` points to the latest commit on the master branch.

#### Docker run

For docker `-e` you have to replace dots with underscores.

```shell script
docker run \
    -e keycloak_url=http://<your keycloak host>:8080 \
    -e keycloak_user=<keycloak admin username> \
    -e keycloak_password=<keycloak admin password> \
    -e keycloak_availability-check_enabled=true \
    -e keycloak_availability-check_timeout=120s \
    -e import_path=/config \
    -e import_force=false \
    -v <your config path>:/config \
    adorsys/keycloak-config-cli:latest
```

### Helm

We provide a helm chart [here](./contrib/charts/keycloak-config-cli).

Since it make no sense to deploy keycloak-config-cli as standalone application, you could
add it as dependency to your chart deployment.

Checkout helm docs about [chart dependencies](https://helm.sh/docs/topics/charts/#chart-dependencies)!

#### CLI option / Environment Variables

|  CLI / ENV Variable                 | Description                                                                       | Default     |
| ----------------------------------- | --------------------------------------------------------------------------------- | ----------- |
| keycloak.url                        | Keycloak Url without `/auth`                                                      | -           |
| keycloak.user                       | login user name                                                                   | `admin`     |
| keycloak.password                   | login user name                                                                   | -           |
| keycloak.client-id                  | login clientId                                                                    | `admin-cli` |
| keycloak.login-realm                | login realm                                                                       | `master`    |
| keycloak.ssl-verify                 | Verify ssl connection to keycloak                                                 | `true`      |
| keycloak.availability-check.enabled | Wait until keycloak is available                                                  | `false`     |
| keycloak.availability-check.timeout | Wait timeout for keycloak availability check                                      | `120s`      |
| import.path                         | Location of config files (if location is a directory, all files will be imported) | `/config`   |
| import.force                        | Enable force import of realm config                                               | `false`     |
| import.cache-key                    | Cache key for importing config.                                                   | `default`   |
| import.state                        | Enable state management. Purge only resources managed by kecloak-config-cli.      | `true`      |
| import.file-type                    | Format of the configuration import file. Allowed values: JSON/YAML                | `json`      |
| import.parallel                     | Enable parallel import of certain resources                                       | `false`     |

See [application.properties](src/main/resources/application.properties) for all available settings.

Take a look at [spring relax binding](https://github.com/spring-projects/spring-boot/wiki/Relaxed-Binding-2.0) if you need
alternative spellings. For docker `-e` you have to replace dots with underscores.

### Experimental native build

keycloak-config-cli provides _experimental_ native builds based on [GraalVM native image](https://www.graalvm.org/docs/reference-manual/native-image/). While native builds configs provided for multiple versions of keycloak, only the latest version will be tested and released as artifact.

Benefits:

- No java required
- smaller footprint (less cpu, less memory, less image size)
- Speed. Running [sample config](./contrib/example-config/moped.json) in 5 seconds. (8 seconds on normal builds)

Limitations:

- Some dynamic jvm features needs to be define manually in graalvm. The [list](src/main/resources/META-INF/native-image/10.0.2/reflect-config.json) isn't complete which can be result in an unexpected behavior.

## Perform release

Create release via [maven release plugin](https://maven.apache.org/maven-release/maven-release-plugin/examples/prepare-release.html):

```shell script
mvn -Dresume=false release:prepare release:clean
git push --follow-tags
```
