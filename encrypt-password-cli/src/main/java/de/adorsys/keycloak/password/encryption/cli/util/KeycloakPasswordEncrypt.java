package de.adorsys.keycloak.password.encryption.cli.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

public class KeycloakPasswordEncrypt {

    private final JWTKeyUtil jwtKeyUtil;

    public KeycloakPasswordEncrypt(JWTKeyUtil jwtKeyUtil) {
        this.jwtKeyUtil = jwtKeyUtil;
    }

    public String encryptPassword(String jwksUrl, String password) {
        try {
            return encryptPasswordWithJwe(jwksUrl, password);
        } catch (MalformedURLException | ParseException | JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String encryptPasswordWithJwe(String jwksUrl, String password) throws MalformedURLException, ParseException, JOSEException {
        List<JWK> keys = jwtKeyUtil.getKeys(jwksUrl);

        Optional<JWK> maybeKey = keys.stream().findFirst();

        if(!maybeKey.isPresent()) {
            throw new RuntimeException("Cannot find any key from jwks");
        }

        JWK jweKey = KeycloakHelper.fromSignToEnc(maybeKey.get());
        String payload = new PasswordPayload(password).toJSONObject().toJSONString();

        return EncryptHelper.encrypt(jweKey, payload);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer connectTimeout = 1000;
        private Integer readTimeout = 2000;
        private Integer sizeLimit = 1024 * 1024;

        public Builder connectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder sizeLimit(Integer sizeLimit) {
            this.sizeLimit = sizeLimit;
            return this;
        }

        public KeycloakPasswordEncrypt build() {
            JWTKeyUtil jwtKeyUtil = JWTKeyUtil.builder()
                    .connectTimeout(connectTimeout)
                    .readTimeout(readTimeout)
                    .sizeLimit(sizeLimit)
                    .build();

            return new KeycloakPasswordEncrypt(jwtKeyUtil);
        }
    }
}
