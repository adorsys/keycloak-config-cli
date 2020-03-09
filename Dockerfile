FROM adorsys/java:11

COPY ./target/keycloak-config-cli.jar .
COPY ./docker/root/ /

ENTRYPOINT ["/usr/local/bin/config-cli"]
