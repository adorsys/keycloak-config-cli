# RedHat SSO compatibility

In general, RedHat SSO based on Keycloak. In general, keycloak-config-cli is compatible with RedHat SSO.

Some specific RH SSO version may differ from keycloak releases which introduce an incompatibility between keycloak-config-cli.

While keycloak-config-cli not officially supports RH SSO, it's possible to build keycloak-config-cli against RH SSO.

## Requirements installed on system

* Maven
* OpenJDK


## Steps

### Clone Repo
```bash
git clone https://github.com/adorsys/keycloak-config-cli.git
git checkout v3.4.0
```

### Patch pom.xml

_This step can be skip if keycloak-config-cli version 4.4.0 or higher is used._

Enrich the pom.xml with the changes from: [#583](https://github.com/adorsys/keycloak-config-cli/pull/583)

### Find correct release version for keycloak

Identify the internal version of keycloak. Check Shttps://access.redhat.com/articles/2342881

Then look at https://mvnrepository.com/artifact/org.keycloak/keycloak-core?repo=redhat-ga to find the correct Keycloak version identifier. For Keycloak 9.0.13, its 9.0.13.redhat-00006.

### Build

```bash
mvn clean package -Prh-sso -Dkeycloak.version=9.0.13.redhat-00006
```

In case there are compiler errors, then RH introduce breaking changes. But I'm not going to adjust code for such old versions. Sorry.

### Grab and test

In case the build is fine, you build is in target/keycloak-config-cli.jar.
