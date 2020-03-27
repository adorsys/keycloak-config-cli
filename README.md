[![CI](https://github.com/adorsys/keycloak-config-cli/workflows/CI/badge.svg)](https://github.com/adorsys/keycloak-config-cli/actions?query=workflow%3ACI)
[![GitHub All Releases](https://img.shields.io/github/downloads/adorsys/keycloak-config-cli/total?logo=github)](https://github.com/adorsys/keycloak-config-cli/releases)
[![Docker Pulls](https://img.shields.io/docker/pulls/adorsys/keycloak-config-cli?logo=docker)](https://hub.docker.com/r/adorsys/keycloak-config-cli)
[![Maintainability](https://api.codeclimate.com/v1/badges/bd89704bfacbe1fcd215/maintainability)](https://codeclimate.com/github/adorsys/keycloak-config-cli/maintainability) [![codecov](https://codecov.io/gh/adorsys/keycloak-config-cli/branch/master/graph/badge.svg)](https://codecov.io/gh/adorsys/keycloak-config-cli)
[![GitHub license](https://img.shields.io/github/license/adorsys/keycloak-config-cli)](https://github.com/adorsys/keycloak-config-cli/blob/master/LICENSE.txt)

# keycloak-config-cli

tool to configure keycloak via json file

## Config files

The config files are based on the keycloak export files. You can use them to re-import your settings.
But keep your files as small as possible. Remove all UUIDs and all stuff which is default set by keycloak.

[moped.json](./contrib/example-config/moped.json) is a full working example file you can consider.
Other examples are located in the [test resources](./src/test/resources/import-files).

## Supported features

| Feature                                    | Since | Description                                                                                              |
| ------------------------------------------ | ----- | -------------------------------------------------------------------------------------------------------- |
| Create clients                             | 1.0.0 | Create client configuration while creating or updating realms                                            |
| Update clients                             | 1.0.0 | Update client configuration while updating realms                                                        |
| Add roles                                  | 1.0.0 | Add roles while creating or updating realms                                                              |
| Update roles                               | 1.0.0 | Update role properties while updating realms                                                             |
| Add composites to roles                    | 1.3.0 | Add role with realm-level and client-level composite roles while creating or updating realms             |
| Add composites to roles                    | 1.3.0 | Add realm-level and client-level composite roles to existing role while creating or updating realms      |
| Remove composites from roles               | 1.3.0 | Remove realm-level and client-level composite roles from existing role while creating or updating realms |
| Add users                                  | 1.0.0 | Add users (inclusive password!) while creating or updating realms                                        |
| Add users with roles                       | 1.0.0 | Add users with realm-level and client-level roles while creating or updating realms                      |
| Update users                               | 1.0.0 | Update user properties (inclusive password!) while updating realms                                       |
| Add role to user                           | 1.0.0 | Add realm-level and client-level roles to user while updating realm                                      |
| Remove role from user                      | 1.0.0 | Remove realm-level or client-level roles from user while updating realm                                  |
| Add authentication flows and executions    | 1.0.0 | Add authentication flows and executions while creating or updating realms                                |
| Update authentication flows and executions | 1.0.0 | Update authentication flow properties and executions while updating realms                               |
| Add components                             | 1.0.0 | Add components while creating or updating realms                                                         |
| Update components                          | 1.0.0 | Update components properties while updating realms                                                       |
| Update sub-components                      | 1.0.0 | Add sub-components properties while creating or updating realms                                          |
| Add groups                                 | 1.3.0 | Add groups (inclusive subgroups!) to realm while creating or updating realms                             |
| Update groups                              | 1.3.0 | Update existing group properties and attributes while creating or updating realms                        |
| Remove groups                              | 1.3.0 | Remove existing groups while updating realms                                                             |
| Add/Remove group attributes                | 1.3.0 | Add or remove group attributes in existing groups while updating realms                                  |
| Add/Remove group roles                     | 1.3.0 | Add or remove roles to/from existing groups while updating realms                                        |
| Update/Remove subgroups                    | 1.3.0 | Like groups, subgroups may also be added/updated and removed while updating realms                       |
| Add scope-mappings                         | 1.0.0 | Add scope-mappings while creating or updating realms                                                     |
| Add roles to scope-mappings                | 1.0.0 | Add roles to existing scope-mappings while updating realms                                               |
| Remove roles from scope-mappings           | 1.0.0 | Remove roles from existing scope-mappings while updating realms                                          |
| Add required-actions                       | 1.0.0 | Add required-actions while creating or updating realms                                                   |
| Update required-actions                    | 1.0.0 | Update properties of existing required-actions while updating realms                                     |
| Update identity providers                  | 1.2.0 | Update properties of existing identity providers while updating realms                                   |

## Compatibility matrix

| keycloak-tools | **Keycloak 4.x** | **Keycloak 6.x** | **Keycloak 7.x** | **Keycloak 8.x** | **Keycloak 9.x** |
| -------------- | :--------------: | :--------------: | :--------------: | :--------------: | :--------------: |
| **v0.8.x**     |        ✓         |        ✓         |        ✓         |        ✗         |        ✗         |
| **v1.0.x**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |
| **v1.1.x**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |
| **v1.2.x**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |
| **v1.3.x**     |        ✗         |        ✗         |        ✗         |        ✓         |        ✓         |
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
    --import.file=./contrib/example-config/moped.json
```

### Docker

#### Docker run

```bash
$ docker run \
    -e KEYCLOAK_URL=http://<your keycloak host>:8080 \
    -e KEYCLOAK_ADMIN=<keycloak admin username> \
    -e KEYCLOAK_ADMIN_PASSWORD=<keycloak admin password> \
    -e WAIT_TIME_IN_SECONDS=120 \
    -e IMPORT_FORCE=false \
    -v <your config path>:/config \
    adorsys/keycloak-config-cli:latest
```

## Perform release

```bash
mvn -Dresume=false -DdryRun=true release:prepare
mvn -Dresume=false release:prepare
```
