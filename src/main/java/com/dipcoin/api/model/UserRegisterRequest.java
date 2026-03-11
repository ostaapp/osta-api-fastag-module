package com.dipcoin.api.model;

import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.db.services.commons.DBConstants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.EmailValidator;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class UserRegisterRequest extends APIRequest {

  @ApiModelProperty(required = true)
  private String fname;
  @ApiModelProperty(required = true)
  private String lname;
  @ApiModelProperty(required = true)
  private String address;
  @ApiModelProperty(required = true)
  private String email;
  @ApiModelProperty(required = true)
  private String phonenum;
  @ApiModelProperty(required = true)
  private String password;
  @ApiModelProperty(required = true)
  private String pin;
  @ApiModelProperty(required = true)
  private Boolean tnc;
  @ApiModelProperty(required = false)
  private String source;
  @ApiModelProperty(required = false)
  private String bundleId;
  @ApiModelProperty(required = false)
  private String origin;
  @ApiModelProperty(required = false)
  private CustomerAccountRequest accountInfo;
  
  @ApiModelProperty(required = false)
  private Boolean wrapper = Boolean.FALSE;
  
  @ApiModelProperty(required = false)
  private String requestType;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (StringUtils.isEmpty(getFname())) {
      this.setErrorCode(HeaderCode.USER_INVALID_FNAME);
      return false;
    }
    if (StringUtils.isEmpty(getLname())) {
      this.setErrorCode(HeaderCode.USER_INVALID_LNAME);
      return false;
    }

    if (StringUtils.isEmpty(getPhonenum()) || getPhonenum().length() != DBConstants.MOBILE_LENGTH
        || !NumberUtils.isCreatable(getPhonenum())) {
      this.setErrorCode(HeaderCode.USER_INVALID_PHONENUM);
      return false;
    }
    
    if (StringUtils.isNumeric(getFname())) {
      this.setErrorCode(HeaderCode.USER_INVALID_FNAME);
      return false;
    }
    
    if (StringUtils.isNumeric(getLname())) {
      this.setErrorCode(HeaderCode.USER_INVALID_LNAME);
      return false;
    }

    if (StringUtils.isEmpty(getEmail()) || !EmailValidator.getInstance().isValid(getEmail())) {
      this.setErrorCode(HeaderCode.USER_INVALID_EMAIL);
      return false;
    }

    if (getPassword() != null && getPassword().isEmpty()) {
      this.setErrorCode(HeaderCode.USER_INVALID_PASSWORD);
      return false;
    }

    if (getTnc() != null && BooleanUtils.isFalse(getTnc())) {
      this.setErrorCode(HeaderCode.USER_INVALID_TNC);
      return false;
    }

    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {}

}
