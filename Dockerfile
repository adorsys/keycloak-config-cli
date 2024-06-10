# Can be adjusted with docker build --build-arg RUNTIME_IMAGE=mirror.com/openjdk:21
ARG BUILDER_IMAGE=eclipse-temurin:21-jdk
ARG RUNTIME_IMAGE=eclipse-temurin:21-jre

FROM ${BUILDER_IMAGE} AS BUILDER

WORKDIR /app/

ARG KEYCLOAK_VERSION=25.0.0
ARG MAVEN_CLI_OPTS="-ntp -B"

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN ./mvnw ${MAVEN_CLI_OPTS} -q dependency:go-offline

COPY src src

RUN ./mvnw ${MAVEN_CLI_OPTS} clean package -DskipTests -Dkeycloak.version=${KEYCLOAK_VERSION} \
    -Dlicense.skipCheckLicense -Dcheckstyle.skip -Dmaven.test.skip=true -Dmaven.site.skip=true \
    -Dmaven.javadoc.skip=true -Dmaven.gitcommitid.skip=true

FROM ${RUNTIME_IMAGE}

ARG JAR=./target/keycloak-config-cli.jar
ENV JAVA_OPTS="" KEYCLOAK_SSL_VERIFY=true IMPORT_FILES_LOCATIONS=file:/config/*

# $0 represents the first CLI arg which is not inside $@
ENTRYPOINT exec java $JAVA_OPTS -jar /app/keycloak-config-cli.jar $0 $@

COPY --from=BUILDER /app/target/keycloak-config-cli.jar /app/keycloak-config-cli.jar

USER 65534
