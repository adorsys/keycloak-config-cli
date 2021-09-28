FROM openjdk:17-slim

ARG JAR=./target/keycloak-config-cli.jar
ENV JAVA_OPTS="" KEYCLOAK_SSL_VERIFY=true IMPORT_PATH=file:/config
ENTRYPOINT exec java $JAVA_OPTS -jar /app/keycloak-config-cli.jar $@

USER 1001

ADD --chown=1001:0 ${JAR} /app/keycloak-config-cli.jar
