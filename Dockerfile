FROM adorsys/java:11

WORKDIR /tmp/keycloak-config-cli
VOLUME /tmp/keycloak-config-cli/configs

COPY ./target/config-cli.jar ./keycloak-config-cli.jar
COPY ./docker/root/ /

ENTRYPOINT ["/bin/bash"]
