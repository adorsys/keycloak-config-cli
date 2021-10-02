FROM openjdk:17-slim

ARG JAR=./target/keycloak-config-cli.jar
ENV JAVA_OPTS="" KEYCLOAK_SSL_VERIFY=true IMPORT_PATH=file:/config

# $0 represents the first CLI arg which is not inside $@
ENTRYPOINT exec java $JAVA_OPTS -jar /app/keycloak-config-cli.jar $0 $@

USER 1001

ADD --chown=0:0 ${JAR} /app/keycloak-config-cli.jar
