Add here size of the file

```
src/main/java/de/adorsys/keycloak/config/properties/ImportConfigProperties.java:286
src/main/java/de/adorsys/keycloak/config/properties/NormalizationConfigProperties.java:84
src/main/resources/application.properties:26
```

Build command

```
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 && ./mvnw clean package -Dkeycloak.version=24.0.4 -Dkeycloak.client.version=24.0.4 -Dmaven.test.skip=true
```
