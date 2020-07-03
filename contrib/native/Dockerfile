FROM alpine

ENV KEYCLOAK_SSLVERIFY=true JAVA_OPTS="" IMPORT_PATH=/config

COPY ./target/keycloak-config-cli-native /opt/keycloak-config-cli

USER 1001

ENTRYPOINT ["/opt/keycloak-config-cli"]
