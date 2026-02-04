package com.dipcoin.api.commons;

import java.util.Set;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import com.dipcoin.api.commons.OAuth2Utils.PartnerType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2Token {

	private ResponseType type;
	private String clientId;
	private Long expiry;
	private Set<String> scopes;
	private String originIp;
	private Integer partnerId;
	private PartnerType partnerType;
	private boolean validated;

	public OAuth2Token() {
	}

	public OAuth2Token(ResponseType type) {
		this.type = type;
	}

	public ResponseType getType() {
		return type;
	}

	public void setType(ResponseType type) {
		this.type = type;
	}

	public String getClientId() {
		return clientId;
	}

	public OAuth2Token setClientId(String clientId) {
		this.clientId = clientId;
		return this;
	}

	public Long getExpiry() {
		return expiry;
	}

	public OAuth2Token setExpiry(Long expiry) {
		this.expiry = expiry;
		return this;
	}

	public Set<String> getScopes() {
		return scopes;
	}

	public OAuth2Token setScopes(Set<String> scopes) {
		this.scopes = scopes;
		return this;
	}

	public String getOriginIp() {
		return originIp;
	}

	public OAuth2Token setOriginIp(String originIp) {
		this.originIp = originIp;
		return this;
	}

	public Integer getPartnerId() {
		return partnerId;
	}

	public OAuth2Token setPartnerId(Integer partnerId) {
		this.partnerId = partnerId;
		return this;
	}

	public PartnerType getPartnerType() {
		return partnerType;
	}

	public OAuth2Token setPartnerType(PartnerType partnerType) {
		this.partnerType = partnerType;
		return this;
	}

	public boolean isValidated() {
		return validated;
	}

	public void setValidated(boolean validated) {
		this.validated = validated;
	}
}
