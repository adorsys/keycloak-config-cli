# keycloak-tools

This project contains tools used to automate keycloak's deployment process.

## Submodules

| folder | description |
|--------|-------------|
| config-cli | tool to configure keycloak via yml files |
| encrypt-password-cli | tool to encrypt a password     |

### Docker

#### Docker run

```
$ docker run -e KEYCLOAK_URL=http://<your keycloak host>:8080 \
             -e KEYCLOAK_ADMIN=<keycloak admin username> \
             -e KEYCLOAK_ADMIN_PASSWORD=<keycloak admin password> \
             -e KEYCLOAK_ENCRYPT_ADMIN_PASSWORD=<true/false> \
             -e JWKS_CONNECT_TIMEOUT=250 \
             -e JWKS_READ_TIMEOUT=250 \
             -e JWKS_SIZE_LIMIT=51200 \
             -v <your config path>:/opt/keycloak-config-cli/configs \
             adorsys/keycloak-config-cli:latest \
             config-cli
```

#### Docker-compose

```
version: '3.1'
services:
  keycloak:
    image: jboss/keycloak:4.4.0.Final
    environment:
      KEYCLOAK_USER: <keycloak admin username>
      KEYCLOAK_PASSWORD: <keycloak admin password>
    ports:
    - "8080:8080"
    networks:
    - my_network
    command:
    - "-b"
    - "0.0.0.0"
    - "--debug"
  keycloak_config:
    image: adorsys/keycloak-config-cli:latest
    depends_on:
    - keycloak
    links:
    - keycloak
    volumes:
    - <your config path>:/tmp/keycloak-config-cli/configs
    environment:
    - KEYCLOAK_URL=http://<your keycloak host>:8080/auth
    - KEYCLOAK_ADMIN=<keycloak admin username>
    - KEYCLOAK_ADMIN_PASSWORD=<keycloak admin password>
    - KEYCLOAK_ENCRYPT_ADMIN_PASSWORD=<true/false>
    - JWKS_CONNECT_TIMEOUT=250
    - JWKS_READ_TIMEOUT=250
    - JWKS_SIZE_LIMIT=51200
    depends_on:
    - keycloak
    networks:
    - my_network
    command: config-cli

networks:
  my_network:

```

### Within maven

```
$ ./config.sh http://<your keycloak host>:8080/auth
```

where `<your keycloak host>` is the hostname of your local docker-host. This could also be `localhost` if the docker where you run keycloak is available on localhost.
