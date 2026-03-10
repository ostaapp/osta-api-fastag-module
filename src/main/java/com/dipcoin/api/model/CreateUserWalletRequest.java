package com.dipcoin.api.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.EmailValidator;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.db.services.commons.DBConstants;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class CreateUserWalletRequest extends APIRequest {

  @ApiModelProperty(required = true)
  private String fname;
  @ApiModelProperty(required = true)
  private String lname;
  @ApiModelProperty(required = true)
  private String email;
  @ApiModelProperty(required = true)
  private String phonenum;
  @ApiModelProperty(required = true)
  private String role;


  @SuppressWarnings("unused")
  private String password;

  @SuppressWarnings("unused")
  private String pin;

  @SuppressWarnings("unused")
  private Boolean tnc;

  private boolean createUser;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (StringUtils.isEmpty(this.fname)) {
      this.setErrorCode(HeaderCode.USER_INVALID_FNAME);
      return false;
    }
    if (StringUtils.isEmpty(this.lname)) {
      this.setErrorCode(HeaderCode.USER_INVALID_LNAME);
      return false;
    }

    if (!(!StringUtils.isBlank(this.phonenum)
        && !(this.phonenum.length() != DBConstants.MOBILE_LENGTH)
        && NumberUtils.isCreatable(this.phonenum) && !this.phonenum.contains(" "))) {
      this.setErrorCode(HeaderCode.USER_INVALID_PHONENUM);
      return false;
    }

    if (!(!StringUtils.isBlank(this.email) && EmailValidator.getInstance().isValid(this.email)
        && !this.email.contains(" "))) {
      this.setErrorCode(HeaderCode.USER_INVALID_EMAIL);
      return false;
    }

    if (this.createUser) {
      if (!(!StringUtils.isBlank(this.role) && !this.role.contains(" "))) {
        this.setErrorCode(HeaderCode.USER_INVALID_ROLE);
        return false;
      }
    }

    if (this.createUser) {
      if (!APIConstants.CUSTOMER.equals(this.role)) {
        if (!APIConstants.MERCHANT.equals(this.role)) {
          this.setErrorCode(HeaderCode.USER_INVALID_ROLE);
          return false;
        }
      }
    }
    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {}

}
