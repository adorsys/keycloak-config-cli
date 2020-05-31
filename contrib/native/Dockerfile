FROM alpine

ENV KEYCLOAK_SSLVERIFY=true JAVA_OPTS="" IMPORT_PATH=/config

RUN apk add --no-cache bash curl

COPY ./target/keycloak-config-cli-native /opt/keycloak-config-cli
COPY ./docker/root/ /

ENTRYPOINT ["/usr/local/bin/config-cli"]
CMD /opt/keycloak-config-cli
