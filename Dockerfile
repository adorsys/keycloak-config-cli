FROM openjdk:11-jre-slim

ENV KEYCLOAK_SSLVERIFY=true JAVA_OPTS="" IMPORT_PATH=/config

COPY ./target/keycloak-config-cli.jar /opt/

USER 1001

ENTRYPOINT ["/bin/sh"]
CMD exec java $JAVA_OPTS -jar /opt/keycloak-config-cli.jar
