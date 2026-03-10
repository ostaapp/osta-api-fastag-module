package com.dipcoin.api.model;

import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.PartnerRegisterCustomerRequest.AccountDetails;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class UserUpdateRequest extends UserRegisterRequest {

  @ApiModelProperty(required = false)
  private String oldPassword;
  @ApiModelProperty(required = false)
  private String oldPhonenum;
  @ApiModelProperty(required = false)
  private Boolean tnc;
  @ApiModelProperty(required = false)
  private String pin;
  
  @ApiModelProperty(required = true)
  private  AccountDetails accountDetails;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {

    // verify password.
    if ((!StringUtils.isEmpty(getPassword()) && StringUtils.isEmpty(getOldPassword()))) {
      this.setErrorCode(HeaderCode.USER_INVALID_PASSWORD);
      return false;
    }

    // verify pin
    if (!StringUtils.isEmpty(getPin()) && StringUtils.isEmpty(getOldPassword())) {
      this.setErrorCode(HeaderCode.USER_INVALID_PIN);
      return false;
    }

    // validate phonenum req
    if ((!StringUtils.isEmpty(getPhonenum()) && StringUtils.isEmpty(getOldPhonenum()))
        || (StringUtils.isEmpty(getPhonenum()) && !StringUtils.isEmpty(getOldPhonenum()))) {
      this.setErrorCode(HeaderCode.USER_INVALID_PHONENUM);
      return false;
    }

    return true;
  }
}
