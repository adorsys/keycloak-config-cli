# Can be adjusted with docker build --build-arg RUNTIME_IMAGE=mirror.com/openjdk:17
ARG BUILDER_IMAGE=openjdk:17
ARG RUNTIME_IMAGE=openjdk:17-slim

FROM ${BUILDER_IMAGE} AS BUILDER

ARG KEYCLOAK_VERSION=17.0.0

COPY . .

RUN ./mvnw -ntp -B clean package -DskipTests -Dkeycloak.version=${KEYCLOAK_VERSION} \
    -Dlicense.skipCheckLicense -Dcheckstyle.skip -Dmaven.test.skip=true -Dmaven.site.skip=true \
    -Dmaven.javadoc.skip=true -Dmaven.gitcommitid.skip=true

FROM ${RUNTIME_IMAGE}

ARG JAR=./target/keycloak-config-cli.jar
ENV JAVA_OPTS="" KEYCLOAK_SSL_VERIFY=true IMPORT_PATH=file:/config

# $0 represents the first CLI arg which is not inside $@
ENTRYPOINT exec java $JAVA_OPTS -jar /app/keycloak-config-cli.jar $0 $@

COPY --from=BUILDER /target/keycloak-config-cli.jar /app/keycloak-config-cli.jar

USER 65534
