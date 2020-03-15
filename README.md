[![Build Status](https://travis-ci.com/adorsys/keycloak-config-cli.svg?branch=master)](https://travis-ci.com/adorsys/keycloak-config-cli) [![Maintainability](https://api.codeclimate.com/v1/badges/bd89704bfacbe1fcd215/maintainability)](https://codeclimate.com/github/adorsys/keycloak-config-cli/maintainability) [![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/master/graph/badge.svg)](https://codecov.io/gh/adorsys/keycloak-config-cli)

# keycloak-config-cli

tool to configure keycloak via json file

## Config files

The config files are based on the keycloak export files. You can use them to re-import your settings.
But keep your files as small as possible. Remove all UUIDs and all stuff which is default set by keycloak.

[moped.json](./example-config/moped.json) is a full working example file you can consider.
Other examples are located in the [test resources](./config-cli/src/test/resources/import-files).

## Compatibility matrix

| keycloak-tools | **Keycloak 4.x** | **Keycloak 6.x** | **Keycloak 7.x** | **Keycloak 8.x** | **Keycloak 9.x** |
| -------------- | :--------------: | :--------------: | :--------------: | :--------------: | :--------------: |
| **v0.4.x**     |        ✓         |        ✗         |        ✗         |        ✗         |        ✗         |
| **v0.5.x**     |        ✓         |        ✗         |        ✗         |        ✗         |        ✗         |
| **v0.6.x**     |        ✓         |        ✓         |        ✗         |        ✗         |        ✗         |
| **v0.7.x**     |        ✓         |        ✓         |        ✓         |        ✗         |        ✗         |
| **v0.8.x**     |        ✓         |        ✓         |        ✓         |        ✗         |        ✗         |
| **v1.0.x**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |
| **v1.1.x**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |
| **v1.2.x**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |
| **master**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |

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
    --keycloak.sslVerify=true \
    --keycloak.user=admin \
    --keycloak.password=admin123 \
    --import.file=./example-config/moped.json
```

### Docker

#### Docker run

```
$ docker run \
    -e KEYCLOAK_URL=http://<your keycloak host>:8080 \
    -e KEYCLOAK_ADMIN=<keycloak admin username> \
    -e KEYCLOAK_ADMIN_PASSWORD=<keycloak admin password> \
    -e WAIT_TIME_IN_SECONDS=120 \
    -e IMPORT_FORCE=false \
    -v <your config path>:/config \
    adorsys/keycloak-config-cli:latest
```
