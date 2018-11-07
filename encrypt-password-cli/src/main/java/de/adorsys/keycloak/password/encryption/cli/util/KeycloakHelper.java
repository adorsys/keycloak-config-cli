package de.adorsys.keycloak.password.encryption.cli.util;

import java.text.ParseException;

import com.nimbusds.jose.jwk.JWK;

import net.minidev.json.JSONObject;

public class KeycloakHelper {
	
	/**
	 * Change a Signature key to an Encryption key.
	 * @param signKey
	 * @return
	 * @throws ParseException 
	 */
	public static JWK fromSignToEnc(JWK signKey) throws ParseException {
		if(signKey == null) throw new IllegalArgumentException("Sign key should not be null");
		JSONObject jsonObject = signKey.toJSONObject();
		jsonObject.put("use", "enc");
		jsonObject.put("alg", "RSA-OAEP");
		return JWK.parse(jsonObject);
	}
}
