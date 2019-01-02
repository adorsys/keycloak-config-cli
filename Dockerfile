FROM adorsys/java:8

WORKDIR /tmp/keycloak-config-cli
VOLUME /tmp/keycloak-config-cli/configs

COPY ./config-cli/target/config-cli.jar ./keycloak-config-cli.jar
COPY ./encrypt-password-cli/target/encrypt-password-cli.jar /usr/local/share/encrypt-password-for-keycloak.jar
COPY ./docker/root/ /

ENTRYPOINT ["/bin/bash"]
