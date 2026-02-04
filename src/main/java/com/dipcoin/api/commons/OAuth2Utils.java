package com.dipcoin.api.commons;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dipcoin.api.model.APIResponse;
import com.google.common.collect.Sets;

public class OAuth2Utils {

	public static final String OAUTH2_INVALID_CLIENT_DESCRIPTION = "Client authentication failed (e.g., unknown client, no client authentication included, or unsupported authentication method).";

	private static final String ACCESS_SCOPE_DELIMITER = ",";

	public static Set<String> parseAccessScopes(String scopes) {
		if (StringUtils.isEmpty(scopes))
			return Sets.newHashSet();

		return Sets.newHashSet(scopes.split(ACCESS_SCOPE_DELIMITER));
	}

	public enum OAuthService {
		CUSTOMER_DIPCOIN("customer:osta"), MERCHANT_DIPCOIN("merchant:osta");

		private static final Map<String, List<String>> ACCESS_SCOPE_MAPPINGS = new HashMap<>();
		static {
			ACCESS_SCOPE_MAPPINGS.put(CUSTOMER_DIPCOIN.value(), Arrays.asList("oauth2/customer/osta"));
			ACCESS_SCOPE_MAPPINGS.put(MERCHANT_DIPCOIN.value(),
					Arrays.asList("oauth2/merchant/osta", "oauth2/merchant/osta/qrcode"));
		}

		private String service;

		private OAuthService(String s) {
			this.service = s;
		}

		public String value() {
			return this.service;
		}

		public static boolean validate(String service, String path) {
			if (!ACCESS_SCOPE_MAPPINGS.containsKey(service))
				return false;

			return ACCESS_SCOPE_MAPPINGS.get(service).contains(path);
		}
	}

	public static List<String> MERCHANT_ACCESS_SCOPES = new LinkedList<>(
			Arrays.asList(OAuthService.CUSTOMER_DIPCOIN.value(), OAuthService.MERCHANT_DIPCOIN.value()));

	public static enum PartnerType {
		MERCHANT, BANK;

		public static PartnerType of(String type) {
			if (MERCHANT.name().equalsIgnoreCase(type))
				return MERCHANT;
			else if (BANK.name().equalsIgnoreCase(type))
				return BANK;

			return null;
		}
	}

	public static ResponseEntity errorResponse(OAuthProblemException e) throws OAuthSystemException {
		OAuthResponse response = OAuthASResponse.errorResponse(e.getResponseStatus()).setError(e.getError())
				.setErrorDescription(e.getDescription()).buildJSONMessage();
		return ResponseEntity.status(response.getResponseStatus()).body(response.getBody());
	}

	public static ResponseEntity<String> errorResponse(HttpStatus responseCode, String errorCode, String description)
			throws OAuthSystemException {
		OAuthResponse response = OAuthASResponse.errorResponse(responseCode.value()).setError(errorCode)
				.setErrorDescription(description).buildJSONMessage();
		return ResponseEntity.status(response.getResponseStatus()).body(response.getBody());
	}

	public static ResponseEntity apiResponse(List<HeaderCode> headerCode, HttpStatus responseCode)
			throws OAuthSystemException {
		return ResponseEntity.status(responseCode).body(APIResponse.errors(headerCode));
	}

}
