FROM adorsys/java:8

WORKDIR /tmp/keycloak-config-cli
VOLUME /tmp/keycloak-config-cli/configs

COPY ./config-cli/target/config-cli.jar ./keycloak-config-cli.jar

COPY ./encrypt-password-cli/target/encrypt-password-cli.jar /usr/local/share/encrypt-password-for-keycloak.jar
COPY ./docker/jwe-cli.sh /usr/local/bin/jwe-cli
COPY ./docker/config-cli.sh /usr/local/bin/config-cli
COPY ./docker/wtfc.sh /usr/local/bin/wtfc

ENTRYPOINT ["/bin/bash"]
