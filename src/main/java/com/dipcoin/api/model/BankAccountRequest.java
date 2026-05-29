package com.dipcoin.api.model;

import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantAccountType;
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
public class BankAccountRequest extends APIRequest {

  @ApiModelProperty(required = true)
  private String accountHolderName;
  @ApiModelProperty(required = true)
  private String accountNumber;
  @ApiModelProperty(required = false)
  private Integer isPrimaryAccount;
  @ApiModelProperty(required = false)
  private Integer accountType;//related to bank account codes
  @ApiModelProperty(required = true)
  private String ifscCode;
  @ApiModelProperty(required = false)
  private Integer bankAccountType; //related to BankAccountType in DBConstants
  

  private boolean isValidStatus() {
    if (this.isPrimaryAccount != null) {
      return (this.isPrimaryAccount == BooleanStatus.YES.value()
          || this.isPrimaryAccount == BooleanStatus.NO.value());
    } else {
      this.isPrimaryAccount = BooleanStatus.NO.value();
    }

    return true;
  }

  private boolean isValidType() {
    if (this.bankAccountType != null) {
      for (MerchantAccountType type : MerchantAccountType.values()) {
        if (this.bankAccountType == type.value())
          return true;
      }
    }

    return false;
  }

  @Override
  public boolean validate(HttpServletContext httpServletContext) {

    if (!isValidStatus()) {
      this.setErrorCode(HeaderCode.INVALID_ACCOUNT_STATUS);
      return false;
    }
//    if (!isValidType()) {
//      this.setErrorCode(HeaderCode.INVALID_ACCOUNT_TYPE);
//      return false;
//    }
    if (!StringUtils.isNotEmpty(this.accountHolderName)) {
      this.setErrorCode(HeaderCode.ACCOUNT_HOLDER_NAME_NOT_CORRECT);
      return false;
    }
    if (!StringUtils.isNotEmpty(this.accountNumber)) {
      this.setErrorCode(HeaderCode.ACCOUNT_NUMBER_NOT_CORRECT);
      return false;
    }
    if (!StringUtils.isNotEmpty(this.ifscCode)) {
      this.setErrorCode(HeaderCode.IFSC_CODE_NOT_CORRECT);
      return false;
    }

    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {}

}
