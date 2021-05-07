FROM maven:3-openjdk-11-slim

ENV KEYCLOAK_VERSION=""
ENV MAVEN_CLI_OPTS="-B -ntp"
ENV IMPORT_PATH=/config

USER 1001

WORKDIR /app/

COPY src src
COPY pom.xml .env ./

ENTRYPOINT env KEYCLOAK_VERSION=${KEYCLOAK_VERSION:-$(grep KEYCLOAK_VERSION .env | cut -d= -f2)} mvn ${MAVEN_CLI_OPTS} spring-boot:run \
  -Dkeycloak.version=\${KEYCLOAK_VERSION} -DskipTests -Dlicense.skipCheckLicense -Dcheckstyle.skip -Dmaven.test.skip=true -Dmaven.site.skip=true \
  -Dmaven.javadoc.skip=true -Dmaven.gitcommitid.skip=true
