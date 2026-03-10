package com.dipcoin.api.model;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class CustomerAccountVerifyRequest extends APIRequest {

	@ApiModelProperty(required = true)
	private Integer cardId;
	@ApiModelProperty(required = true)
	private String otp;
	@ApiModelProperty(required = false)
	private String authorizationPin;

	@JsonIgnore
	private String bankUId;
	@JsonIgnore
	private Integer methodType;
	@JsonIgnore
	private String dipcoinRefrenceNumber;
	@JsonIgnore
	private String bankTransactionRefrenceNumber;
	@JsonIgnore
	private String transactionTime;
	@ApiModelProperty(required = false)
	private String phonenum;
	@ApiModelProperty(required = false)
	private String pin;
	@ApiModelProperty(required = false)
	private String accountNumber;

	private Boolean forgotPin = Boolean.FALSE;

	@Override
	public boolean validate(HttpServletContext httpServletContext) {
		if (this.cardId == null) {
			this.setErrorCode(HeaderCode.INVALID_CARD_ID);
			return false;
		}
		if (StringUtils.isEmpty(this.otp)) {
			this.setErrorCode(HeaderCode.USER_INVALID_OTP);
			return false;
		}

		return true;
	}

	@Override
	protected void decrypt(HttpServletContext httpServletContext) {
	}

}
