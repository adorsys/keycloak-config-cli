package de.adorsys.keycloak.password.encryption.cli.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.nimbusds.jose.RemoteKeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;

public class JWTKeyUtil {

	private final ResourceRetriever resourceRetriever;

	private JWTKeyUtil() {
		this.resourceRetriever = null;
	}

	private JWTKeyUtil(final ResourceRetriever resourceRetriever) {
		this.resourceRetriever = resourceRetriever;
	}

	public List<JWK> getKeys(String jwksUrl) throws MalformedURLException, RemoteKeySourceException {
		RemoteJWKSet<SecurityContext> remoteJWKSet = new RemoteJWKSet<>(new URL(jwksUrl), this.resourceRetriever);
		SecurityContext context = null;
		JWKMatcher matcher = new JWKMatcher.Builder().publicOnly(true).build();
		JWKSelector jwkSelector = new JWKSelector(matcher);
		List<JWK> jwks = remoteJWKSet.get(jwkSelector, context);
		return jwks;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Integer connectTimeout;
		private Integer readTimeout;
		private Integer sizeLimit;

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

		public JWTKeyUtil build() {
			JWTKeyUtil jwtKeyUtil;

			if(connectTimeout != null && readTimeout != null && sizeLimit != null) {
				jwtKeyUtil = new JWTKeyUtil(new DefaultResourceRetriever(connectTimeout, readTimeout, sizeLimit));
			} else {
				jwtKeyUtil = new JWTKeyUtil();
			}

			return jwtKeyUtil;
		}
	}
}
