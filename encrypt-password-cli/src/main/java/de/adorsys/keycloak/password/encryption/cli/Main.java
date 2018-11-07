package de.adorsys.keycloak.password.encryption.cli;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;

import de.adorsys.keycloak.password.encryption.cli.util.EncryptHelper;
import de.adorsys.keycloak.password.encryption.cli.util.JWTKeyUtil;
import de.adorsys.keycloak.password.encryption.cli.util.KeycloakHelper;
import de.adorsys.keycloak.password.encryption.cli.util.PasswordPayload;

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

    private static JWTKeyUtil jwtKeyUtil;

    public static void main(String ... argv) throws MalformedURLException, ParseException, JOSEException {
        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(argv);
        main.run();
    }

    public void run() throws MalformedURLException, ParseException, JOSEException {
    	notEmpty("jwksUrl",this.jwksUrl);
    	notEmpty("password",this.password);

        jwtKeyUtil = JWTKeyUtil.builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .sizeLimit(sizeLimit)
                .build();

    	List<JWK> keys = jwtKeyUtil.getKeys(this.jwksUrl);
    	JWK jweKey = KeycloakHelper.fromSignToEnc(keys.stream().findFirst().get());
    	String payload = new PasswordPayload(this.password).toJSONObject().toJSONString();
    	String encrypt = EncryptHelper.encrypt(jweKey, payload);

    	System.out.println(encrypt.toString());
    }

	private void notEmpty(String paramName, String value) {
		if(value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException("The parameter "+paramName+" must not be null");
		}
	}
    
}