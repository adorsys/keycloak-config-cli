FROM gcr.io/distroless/java:11-debug

ENV JAVA_OPTS="" KEYCLOAK_SSL_VERIFY=true IMPORT_PATH=/config
SHELL ["/busybox/sh", "-c"]
ENTRYPOINT exec java $JAVA_OPTS -jar /app/keycloak-config-cli.jar $0 $@
COPY ./target/keycloak-config-cli.jar /app/

USER 1001
