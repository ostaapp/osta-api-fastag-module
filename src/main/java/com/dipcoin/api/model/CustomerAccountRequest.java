package com.dipcoin.api.model;

import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class CustomerAccountRequest extends APIRequest {
	private static final Logger LOG = LogManager.getLogger(CustomerAccountRequest.class);

	@JsonIgnore
	private String login;
	@JsonIgnore
	private String credential;
	@JsonIgnore
	private String maskedLogin;
	@JsonIgnore
	private String maskedAccountNumber;
	// @JsonIgnore
	private String hashedLogin;

	@ApiModelProperty(required = true)
	private String encryptedLogin;
	@ApiModelProperty(required = false)
	private String accountNumber;
	@ApiModelProperty(required = true)
	private String encryptedCredential;
	@ApiModelProperty(required = false)
	private Integer isPrimaryAccount;
	@ApiModelProperty(required = true)
	private String bankReferenceId;
	@ApiModelProperty(required = false)
	private Integer methodType;
	@ApiModelProperty(required = true)
	private BigDecimal thresholdAmount;
	@ApiModelProperty(required = true)
	private Integer ostaTTLInHrs;
	@ApiModelProperty(required = false)
	private Boolean autoGenerateOsta = true;
	@ApiModelProperty(required = false)
	private BigDecimal maxCoinLimit;
	@ApiModelProperty(required = false)
	private String authorizationPin;
	@ApiModelProperty(required = true)
	private String kycNumber;
	@ApiModelProperty(required = true)
	private Boolean doMask;
	@ApiModelProperty(required = false)
	private Integer accountFlag;
	@ApiModelProperty(required = false)
	private String mobileNumber;
	@ApiModelProperty(required = false)
	private String cardExpiryDate;
	@JsonIgnore
	private String dipcoinRefrenceNumber;
	@JsonIgnore
	private String bankTransactionRefrenceNumber;
	@JsonIgnore
	private String transactionTime;

	@ApiModelProperty(required = true)
	private int isTpinEnabled;
	@ApiModelProperty(required = true)
	private int tPinLength;
	@ApiModelProperty(required = true)
	private String tPin;

	@ApiModelProperty(required = false)
	private String encryptedPin;
	@ApiModelProperty(required = false)
	private String bankCifNo;
	@ApiModelProperty(required = false)
	private String tpinSalt;

	@ApiModelProperty(required = false)
	private String phonenum;

	@ApiModelProperty(required = false)
	private Integer typeOfMethod = -1;

	private Boolean forgotPin = Boolean.FALSE;

	@ApiModelProperty(required = false)
	private String otp;

	private boolean isValidStatus() {
		if (this.isPrimaryAccount != null) {
			return (this.isPrimaryAccount == BooleanStatus.YES.value()
					|| this.isPrimaryAccount == BooleanStatus.NO.value());
		} else {
			this.isPrimaryAccount = BooleanStatus.NO.value();
		}

		return true;
	}

	private boolean isValidMethod() {
		if (this.methodType != null) {
			for (CustomerAccountMethodType type : CustomerAccountMethodType.values()) {
				if (this.methodType == type.value())
					return true;
			}
		}

		return false;
	}

	@Override
	public boolean validate(HttpServletContext httpServletContext) {

		if (!isDecrypted()) {
			decrypt(httpServletContext);
			if (!isDecrypted())
				return false;
		}

		if (!isValidStatus() || !isValidMethod())
			return false;

		if (CustomerAccountMethodType.BANK_INB.value() == this.methodType
				&& !NumberUtils.isDigits(getAccountNumber())) {
			this.setErrorCode(HeaderCode.INVALID_ACCOUNT_TYPE);
			return false;
		}

		if (CustomerAccountMethodType.BANK_DEBITCVV.value() == this.methodType) {
			if (StringUtils.isEmpty(this.mobileNumber) || StringUtils.isBlank(this.cardExpiryDate)) {
				this.setErrorCode(HeaderCode.MISSING_INVALID_INFO);
				return false;
			}
		}

		if (getAccountNumber() != null && (getAccountNumber().length() < DBConstants.ACCOUNT_NUMBER_MIN
				|| getAccountNumber().length() > DBConstants.ACCOUNT_NUMBER_MAX)) {
			this.setErrorCode(HeaderCode.INVALID_ACCOUNT_NUMBER_SIZE);
			return false;
		}

		if (this.thresholdAmount == null)
			this.thresholdAmount = DBConstants.DIPCOIN_THRESHOLD_MIN_AMOUNT;
		else if (this.thresholdAmount.compareTo(DBConstants.DIPCOIN_THRESHOLD_MIN_AMOUNT) == -1
				|| this.thresholdAmount.compareTo(DBConstants.DIPCOIN_THRESHOLD_MAX_AMOUNT) == 1)
			return false;

		if (this.ostaTTLInHrs != null && this.ostaTTLInHrs <= 0)
			return false;

		if (!this.methodType.equals(DBConstants.CustomerAccountMethodType.WALLET_INB.value())) {

			if (this.ostaTTLInHrs == null || this.ostaTTLInHrs > DBConstants.DIPCOIN_MAX_TTL_HRS)
				this.ostaTTLInHrs = DBConstants.DIPCOIN_MAX_TTL_HRS;
		}

		if (!this.forgotPin && StringUtils.isEmpty(this.bankReferenceId)) {
			this.setErrorCode(HeaderCode.INVALID_BANK_REF_ID);
			return false;
		}

		if (this.thresholdAmount == null) {
			this.setErrorCode(HeaderCode.OSTA_THRESHOLD_BREACHED);
			return false;
		}

		if (CustomerAccountMethodType.BANK_ACCOUNTOTP.value() != this.methodType
				&& CustomerAccountMethodType.ADDED_BY_BRANCH.value() != this.methodType) {
			if (StringUtils.isEmpty(this.login)) {
				this.setErrorCode(HeaderCode.INVALID_USER_LOGIN);
				return false;
			}
			if (StringUtils.isEmpty(this.credential)) {
				this.setErrorCode(HeaderCode.INVALID_USER_CREDENTIAL);
				return false;
			}
		} else if (StringUtils.isEmpty(this.getAccountNumber())) {
			return false;
		}

		return true;
	}

	@Override
	protected void decrypt(HttpServletContext httpServletContext) {
		try {

			if (CustomerAccountMethodType.BANK_ACCOUNTOTP.value() == this.methodType
					|| CustomerAccountMethodType.ADDED_BY_BRANCH.value() == this.methodType) {
				this.hashedLogin = Utils.getHashedLogin(StringUtils.stripStart(accountNumber, "0"));

				if (!StringUtils.isEmpty(this.accountNumber)) {
					this.maskedAccountNumber = this.doMask ? Utils.getMaskedAccountNumber(this.accountNumber)
							: this.accountNumber;
				}

				this.maskedLogin = this.maskedAccountNumber;
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.data("accountNumber", this.accountNumber).data("doMask", this.doMask)
						.data("maskedAccountNumber", maskedAccountNumber).data("hashedLogin", hashedLogin).format());
			}

			if (this.encryptedLogin != null && this.encryptedCredential != null && this.methodType != null) {
				byte[] skey = Base64.decodeBase64(getEncryptionSecretKey());
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("login", login).format());

				if (this.getMethodType() == DBConstants.CustomerAccountMethodType.WALLET_INB.value()
						|| this.getMethodType() == DBConstants.CustomerAccountMethodType.BANK_SDK.value()
						|| this.forgotPin) {
					this.login = encryptedLogin;
				} else {
					this.login = new String(this.getCryptoUtil().decrypt(Base64.decodeBase64(encryptedLogin),
							getEncryptionAlgo(), skey, skey), StandardCharsets.UTF_8).trim();
				}

				if (!StringUtils.isEmpty(this.accountNumber)) {
					this.maskedAccountNumber = this.doMask ? Utils.getMaskedAccountNumber(this.accountNumber)
							: this.accountNumber;
				}
				if (!StringUtils.isEmpty(this.login)) {
					this.maskedLogin = Utils.getMaskedLogin(this.login, this.methodType);
					this.hashedLogin = Utils.getHashedLogin(StringUtils.stripStart(login, "0"));
				}

				if (this.getMethodType() == DBConstants.CustomerAccountMethodType.WALLET_INB.value()
						|| this.getMethodType() == DBConstants.CustomerAccountMethodType.BANK_SDK.value()
						|| this.forgotPin) {
					this.credential = encryptedCredential;
				} else {
					this.credential = new String(this.getCryptoUtil().decrypt(Base64.decodeBase64(encryptedCredential),
							getEncryptionAlgo(), skey, skey), StandardCharsets.UTF_8).trim();
				}
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("login", login)
						.data("credential", credential).data("maskedLogin", maskedLogin)
						.data("hashedLogin", hashedLogin).format());
			}
			setDecrypted(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			setDecrypted(false);
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception Caught").format(), e);
		}
	}

}
