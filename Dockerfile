FROM openjdk:11-jre-slim

ENV KEYCLOAK_SSL_VERIFY=true JAVA_OPTS="" IMPORT_PATH=/config

RUN set -eux; \
	apt-get update; \
	apt-get install -y --no-install-recommends \
		wget \
	; \
	rm -rf /var/lib/apt/lists/*

COPY ./target/keycloak-config-cli.jar /opt/

USER 1001

CMD exec java $JAVA_OPTS -jar /opt/keycloak-config-cli.jar
