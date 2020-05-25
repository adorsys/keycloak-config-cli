FROM openjdk:11-jre

ENV KEYCLOAK_SSLVERIFY=true JAVA_OPTS="" IMPORT_PATH=/config

COPY ./target/keycloak-config-cli.jar /opt/
COPY ./docker/root/ /

ENTRYPOINT ["/usr/local/bin/config-cli"]
CMD exec java $JAVA_OPTS -jar /opt/keycloak-config-cli.jar
