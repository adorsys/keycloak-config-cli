FROM openjdk:11-jre-slim

ENV KEYCLOAK_SSLVERIFY=true JAVA_OPTS="" IMPORT_PATH=/config

RUN apt-get update && apt-get install --no-install-recommends -y curl  && rm -rf /var/lib/apt/lists/*

COPY ./target/keycloak-config-cli.jar /opt/
COPY ./docker/root/ /

ENTRYPOINT ["/usr/local/bin/config-cli"]
CMD exec java $JAVA_OPTS -jar /opt/keycloak-config-cli.jar

USER 1001
