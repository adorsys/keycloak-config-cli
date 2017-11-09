FROM adorsys/openjdk-jre-base:8-minideb

RUN apt-get update && apt-get install -y curl

ENV JAVA_OPTS ""

RUN mkdir -p /opt/keycloak-config-cli/configs

WORKDIR /opt/keycloak-config-cli
COPY ./target/keycloak-config-cli-*.jar .
COPY ./docker/docker-entrypoint.bash /opt/docker-entrypoint.bash
COPY ./docker/wtfc.sh /opt/wtfc.sh

VOLUME /opt/keycloak-config-cli/configs

ENTRYPOINT /opt/docker-entrypoint.bash
