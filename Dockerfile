FROM adoptopenjdk/openjdk11:alpine-jre

ENV JAVA_OPTS="" KEYCLOAK_SSL_VERIFY=true IMPORT_PATH=/config
ENTRYPOINT exec java $JAVA_OPTS -jar /app/keycloak-config-cli.jar $0 $@
COPY ./target/keycloak-config-cli.jar /app/

USER 1001
