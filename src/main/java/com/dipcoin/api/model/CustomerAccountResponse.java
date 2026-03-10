package com.dipcoin.api.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import com.dipcoin.bank.services.comm.UserAuthenticationResponse;
import com.dipcoin.bank.services.comm.VerifyOTPResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.StringUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonInclude(Include.NON_NULL)
@Getter
@Setter
@ToString
public class CustomerAccountResponse extends APIResponse {
	
	private int id;
	private String bankName;
	private String dateAdded;
	private String accountNumber;
	private Integer cardId;
	private Integer isPrimaryAccount;
	private Integer status;
	private String thresholdAmount;
	private Integer typeOfMethod;
	private String encCardId;
	private Integer ostaTTLInHrs;
	private Boolean autoGenerateOsta;
	private String maskedLogin;
	private int perMonthLimit;
	private String perTransactionLimit;
	private String perDayLimit;
	private String maxCoinLimit;
	private Integer authorizationMethod;
	private Integer tollStatus;
	private Integer tagAutoTop;
	private List<APICustomization> apiCustomization;
	private BigDecimal autoTopAmount;
	private BigDecimal autoTopUpThresHold;

	private String clientTransactionId;

	private Integer isTpinEnable;
	private Integer tPinLength;
	private String alias;
	private String referenceId;

	private UserAuthenticationResponse userAuthenticationResponse;
	private String dipcoinReferenceNumber;
	private VerifyOTPResponse verifyOTPResponse;
	private Boolean ostaSideAccountVerification = Boolean.FALSE;

	private String hashedLogin;
	private List<String> authenticationMethods;

	@JsonIgnore
	private String authenticationMethodsAsString;
	
	public int getId() {
		return id;
	}
	
	public CustomerAccountResponse setId(int id) {
		this.id = id;
		return this;
	}

	public String getBankName() {
		return bankName;
	}

	public CustomerAccountResponse setBankName(String bankName) {
		this.bankName = bankName;
		return this;
	}

	public String getDateAdded() {
		return dateAdded;
	}

	public CustomerAccountResponse setDateAdded(String dateAdded) {
		this.dateAdded = dateAdded;
		return this;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public CustomerAccountResponse setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public Integer getCardId() {
		return cardId;
	}

	public CustomerAccountResponse setCardId(Integer cardId) {
		this.cardId = cardId;
		return this;
	}

	public Integer getIsPrimaryAccount() {
		return isPrimaryAccount;
	}

	public CustomerAccountResponse setIsPrimaryAccount(Integer isPrimaryAccount) {
		this.isPrimaryAccount = isPrimaryAccount;
		return this;
	}

	public Integer getStatus() {
		return status;
	}

	public CustomerAccountResponse setStatus(Integer status) {
		this.status = status;
		return this;
	}

	public String getThresholdAmount() {
		return thresholdAmount;
	}

	public CustomerAccountResponse setThresholdAmount(String thresholdAmount) {
		this.thresholdAmount = thresholdAmount;
		return this;
	}

	public Integer getTypeOfMethod() {
		return typeOfMethod;
	}

	public CustomerAccountResponse setTypeOfMethod(Integer typeOfMethod) {
		this.typeOfMethod = typeOfMethod;
		return this;
	}

	public String getEncCardId() {
		return encCardId;
	}

	public CustomerAccountResponse setEncCardId(String encCardId) {
		this.encCardId = encCardId;
		return this;
	}

	public Integer getOstaTTLInHrs() {
		return ostaTTLInHrs;
	}

	public CustomerAccountResponse setOstaTTLInHrs(Integer ostaTTLInHrs) {
		this.ostaTTLInHrs = ostaTTLInHrs;
		return this;
	}

	public Boolean getAutoGenerateOsta() {
		return autoGenerateOsta;
	}

	public CustomerAccountResponse setAutoGenerateOsta(Boolean autoGenerateOsta) {
		this.autoGenerateOsta = autoGenerateOsta;
		return this;
	}

	public String getMaskedLogin() {
		return maskedLogin;
	}

	public CustomerAccountResponse setMaskedLogin(String maskedLogin) {
		this.maskedLogin = maskedLogin;
		return this;
	}

	public int getPerMonthLimit() {
		return perMonthLimit;
	}

	public CustomerAccountResponse setPerMonthLimit(int perMonthLimit) {
		this.perMonthLimit = perMonthLimit;
		return this;
	}

	public String getPerTransactionLimit() {
		return perTransactionLimit;
	}

	public CustomerAccountResponse setPerTransactionLimit(String perTransactionLimit) {
		this.perTransactionLimit = perTransactionLimit;
		return this;
	}

	public String getPerDayLimit() {
		return perDayLimit;
	}

	public CustomerAccountResponse setPerDayLimit(String perDayLimit) {
		this.perDayLimit = perDayLimit;
		return this;
	}

	public String getMaxCoinLimit() {
		return maxCoinLimit;
	}

	public CustomerAccountResponse setMaxCoinLimit(String maxCoinLimit) {
		this.maxCoinLimit = maxCoinLimit;
		return this;
	}

	public String getClientTransactionId() {
		return clientTransactionId;
	}

	public CustomerAccountResponse setClientTransactionId(String clientTransactionId) {
		this.clientTransactionId = clientTransactionId;
		return this;
	}

	public Integer getAuthorizationMethod() {
		return authorizationMethod;
	}

	public void setAuthorizationMethod(Integer authorizationMethod) {
		this.authorizationMethod = authorizationMethod;
	}

	public Integer getTollStatus() {
		return tollStatus;
	}

	public void setTollStatus(Integer tollStatus) {
		this.tollStatus = tollStatus;
	}

	public Integer getTagAutoTop() {
		return tagAutoTop;
	}

	public CustomerAccountResponse setTagAutoTop(Integer tagAutoTop) {
		this.tagAutoTop = tagAutoTop;
		return this;
	}

	public List<APICustomization> getApiCustomization() {
		return apiCustomization;
	}

	public void setApiCustomization(List<APICustomization> apiCustomization) {
		this.apiCustomization = apiCustomization;
	}

	public Integer getIsTpinEnable() {
		return isTpinEnable;
	}

	public void setIsTpinEnable(Integer isTpinEnable) {
		this.isTpinEnable = isTpinEnable;
	}

	public Integer gettPinLength() {
		return tPinLength;
	}

	public void settPinLength(Integer tPinLength) {
		this.tPinLength = tPinLength;
	}

	public BigDecimal getAutoTopAmount() {
		return autoTopAmount;
	}

	public void setAutoTopAmount(BigDecimal autoTopAmount) {
		this.autoTopAmount = autoTopAmount;
	}

	public BigDecimal getAutoTopUpThresHold() {
		return autoTopUpThresHold;
	}

	public void setAutoTopUpThresHold(BigDecimal autoTopUpThresHold) {
		this.autoTopUpThresHold = autoTopUpThresHold;
	}

	public String getAuthenticationMethodsAsString() {
		return StringUtils.join(authenticationMethods, ",");
	}

	public void setAuthenticationMethodsAsString(String authenticationMethodsAsString) {
		if (!StringUtils.isEmpty(authenticationMethodsAsString))
			this.authenticationMethods = Arrays.asList(StringUtils.split(authenticationMethodsAsString, ","));
	}

}
