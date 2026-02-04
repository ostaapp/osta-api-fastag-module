package com.dipcoin.api.commons;

import java.io.IOException;
import java.util.Optional;
import org.apache.oltu.oauth2.as.issuer.ValueGenerator;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OAuth2TokenGenerator implements ValueGenerator {

	private final static ObjectMapper mapper = new ObjectMapper();

	private OAuth2Token token;

	private OAuth2TokenGenerator(OAuth2Token token) {
		this.token = token;
	}

	public static OAuth2TokenGenerator of(OAuth2Token token) {
		return new OAuth2TokenGenerator(token);
	}

	@Override
	public String generateValue() throws OAuthSystemException {
		try {
			return mapper.writeValueAsString(token);
		} catch (JsonProcessingException e) {
			throw new OAuthSystemException("Failed to serialized access token", e);
		}
	}

	@Override
	public String generateValue(String param) throws OAuthSystemException {
		return generateValue();
	}

	public static Optional<OAuth2Token> parse(String token) {
		try {
			return Optional.of(mapper.readValue(token, OAuth2Token.class));
		} catch (IOException e) {
		}

		return Optional.empty();
	}
}
