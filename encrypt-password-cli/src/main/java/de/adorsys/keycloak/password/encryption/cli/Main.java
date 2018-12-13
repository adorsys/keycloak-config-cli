package de.adorsys.keycloak.password.encryption.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import de.adorsys.keycloak.password.encryption.cli.util.KeycloakPasswordEncrypt;

public class Main {
    @Parameter(names={"--well-known-url", "-w"})
    private String wellKnownUrl;
    
    @Parameter(names={"--jwks-url", "-j"})
    private String jwksUrl;

    @Parameter(names={"--password", "-p"})
    private String password;

    @Parameter(names={"--enc", "-e"})
    private String enc;

    @Parameter(names={"--alg", "-ap"})
    private String alg;

    @Parameter(names={"--connectTimeout", "-ct"})
    private Integer connectTimeout;

    @Parameter(names={"--readTimeout", "-rt"})
    private Integer readTimeout;

    @Parameter(names={"--sizeLimit", "-sl"})
    private Integer sizeLimit;

    @Parameter(names = "--help", help = true)
    private boolean help;

    public static void main(String ... argv) {
        Main main = new Main();

        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(argv);

        main.printEncryptedPassword();
    }

    public void printEncryptedPassword() {
        notEmpty("jwksUrl",this.jwksUrl);
        notEmpty("password",this.password);

        KeycloakPasswordEncrypt keycloakPasswordEncrypt = KeycloakPasswordEncrypt.builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .sizeLimit(sizeLimit)
                .build();

        String encrypt = keycloakPasswordEncrypt.encryptPassword(this.jwksUrl, this.password);

        System.out.println(encrypt);
    }

    private void notEmpty(String paramName, String value) {
        if(value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException("The parameter "+paramName+" must not be null");
        }
    }
}
