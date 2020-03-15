FROM openjdk:11-jre

ENV KEYCLOAK_SSL_VERIFY=true

COPY ./target/keycloak-config-cli.jar /opt/
COPY ./docker/root/ /

ENTRYPOINT ["/usr/local/bin/config-cli"]
