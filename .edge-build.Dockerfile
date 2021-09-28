FROM openjdk:17-slim

ENV KEYCLOAK_VERSION="" MAVEN_CLI_OPTS="-B -ntp" IMPORT_PATH=file:/config

USER 1001

WORKDIR /app/

COPY src src
COPY .mvn .mvn
COPY mvnw pom.xml .env ./

ENTRYPOINT env KEYCLOAK_VERSION=${KEYCLOAK_VERSION:-$(grep KEYCLOAK_VERSION .env | cut -d= -f2)} ./mvnw ${MAVEN_CLI_OPTS} spring-boot:run \
  -Dkeycloak.version=\${KEYCLOAK_VERSION} -DskipTests -Dlicense.skipCheckLicense -Dcheckstyle.skip -Dmaven.test.skip=true -Dmaven.site.skip=true \
  -Dmaven.javadoc.skip=true -Dmaven.gitcommitid.skip=true
