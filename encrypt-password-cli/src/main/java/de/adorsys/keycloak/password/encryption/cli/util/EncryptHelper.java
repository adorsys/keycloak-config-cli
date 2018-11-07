package de.adorsys.keycloak.password.encryption.cli.util;

import org.apache.commons.codec.binary.StringUtils;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;

public class EncryptHelper {
	
	
	public static String encrypt(JWK key, String text) throws JOSEException {
		JWEEncrypter encrypter = new RSAEncrypter((RSAKey) key);
		JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A128CBC_HS256).keyID(key.getKeyID()).build();
		JWEObject jweObject = new JWEObject(header, new Payload(StringUtils.getBytesUtf8(text)));
		jweObject.encrypt(encrypter);
		String serialize = jweObject.serialize();
		return serialize;
	}
}
