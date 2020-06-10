[![CI](https://github.com/adorsys/keycloak-config-cli/workflows/CI/badge.svg)](https://github.com/adorsys/keycloak-config-cli/actions?query=workflow%3ACI)
[![GitHub All Releases](https://img.shields.io/github/downloads/adorsys/keycloak-config-cli/total?logo=github)](https://github.com/adorsys/keycloak-config-cli/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/adorsys/keycloak-config-cli?logo=docker)](https://hub.docker.com/r/adorsys/keycloak-config-cli)
[![Maintainability](https://api.codeclimate.com/v1/badges/bd89704bfacbe1fcd215/maintainability)](https://codeclimate.com/github/adorsys/keycloak-config-cli/maintainability) [![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/master/graph/badge.svg)](https://codecov.io/gh/adorsys/keycloak-config-cli)
[![GitHub license](https://img.shields.io/github/license/adorsys/keycloak-config-cli)](https://github.com/adorsys/keycloak-config-cli/blob/master/LICENSE.txt)

# keycloak-config-cli

keycloak-config-cli is a Keycloak utility to ensure the desired configuration state for a realm based on a JSON file. The format of the JSON file based on the export realm format. Store and handle the configuration files inside git just like normal code. A Keycloak restart isn't required to apply the configuration.

## Config files

The config files are based on the keycloak export files. You can use them to re-import your settings.
But keep your files as small as possible. Remove all UUIDs and all stuff which is default set by keycloak.

[moped.json](./contrib/example-config/moped.json) is a full working example file you can consider.
Other examples are located in the [test resources](./src/test/resources/import-files).

## Supported features

See: [docs/FEATURES.md](./docs/FEATURES.md)

## Compatibility matrix

| keycloak-config-cli | **Keycloak 4.x - Keycloak 7.x** | **Keycloak 8.x - 10.x** |
| ------------------- | :-----------------------------: | :---------------------: |
| **v0.8.x**          |                ✓                |            ✗            |
| **v1.0.x - v2.0.x** |                ✗                |            ✓            |
| **master**          |                ✗                |            ✓            |

- `✓` Supported
- `✗` Not supported

## Build this project

```bash
$ mvn package
```

## Run integration tests against real keycloak

We are using [TestContainers](https://www.testcontainers.org/) in our integration tests.
To run the integration tests a configured docker environment is required.

```bash
$ mvn verify
```

## Run this project

### via Maven

Start a local keycloak on port 8080:

```bash
$ docker-compose down --remove-orphans && docker-compose up keycloak
```

before performing following command:

```bash
$ java -jar ./target/config-cli.jar \
    --keycloak.url=http://localhost:8080 \
    --keycloak.ssl-verify=true \
    --keycloak.user=admin \
    --keycloak.password=admin123 \
    --import.path=./contrib/example-config/moped.json
```

### Docker

#### Docker run

```bash
$ docker run \
    -e KEYCLOAK_URL=http://<your keycloak host>:8080 \
    -e KEYCLOAK_USER=<keycloak admin username> \
    -e KEYCLOAK_PASSWORD=<keycloak admin password> \
    -e WAIT_TIME_IN_SECONDS=120 \
    -e IMPORT_PATH=/config \
    -e IMPORT_FORCE=false \
    -v <your config path>:/config \
    adorsys/keycloak-config-cli:latest
```

#### Environment Variables

| Variable             | Description                                             | Default     |
| -------------------- | ------------------------------------------------------- | ----------- |
| WAIT_TIME_IN_SECONDS | Timeout in seconds for waiting keycloak until reachable | `120`       |
| KEYCLOAK_URL         | Keycloak Url without `/auth`                            | -           |
| KEYCLOAK_USER        | login user name                                         | `admin`     |
| KEYCLOAK_PASSWORD    | login user name                                         | -           |
| KEYCLOAK_CLIENTID    | login clientId                                          | `admin-cli` |
| KEYCLOAK_LOGINREALM  | login realm                                             | `master`    |
| KEYCLOAK_SSLVERIFY   | Verify ssl connection to keycloak                       | `true`      |
| IMPORT_PATH          | Location of config files                                | `/config`   |
| IMPORT_FORCE         | Enable force import of realm config                     | `false`     |
| IMPORT_CACHEKEY      | Cache key for importing config.                         | `default`   |

### Experimental native build

keycloak-config-cli provides _experimental_ native builds based on [GraalVM native image](https://www.graalvm.org/docs/reference-manual/native-image/).

Benefits:

- No java required
- smaller footprint (less cpu, less memory, less image size)
- Speed. Running [sample config](./contrib/example-config/moped.json) in 5 seconds. (8 seconds on normal builds)

Limitations:

- YAML based properties not supported. Use environment variable, command line parameters or old style properties.
- Some dynamic jvm features needs to be define manually in graalvm. The [list](src/main/resources/META-INF/native-image/10.0.2/reflect-config.json) isn't complete which can be result in an unexpected behavior.

It might be not production ready yet.

## Perform release

```bash
mvn -Dresume=false -DdryRun=true release:prepare
mvn -Dresume=false release:prepare
```
