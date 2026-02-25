package com.dipcoin.api.model;

import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
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
public class MerchantDipcoinRequest extends DipcoinRequest {
  @ApiModelProperty(required = true)
  private String orderId;
  @ApiModelProperty(required = true)
  private String merchantTransactionReferenceId;
  @ApiModelProperty(required = false)
  private String partnerReferenceId;
  @ApiModelProperty(required = false, notes = "Is required for App based requests")
  private String deviceHash;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (!super.validate(httpServletContext)) {
      return false;
    }
    if (StringUtils.isBlank(this.merchantTransactionReferenceId)) {
      this.setErrorCode(HeaderCode.INVALID_MERCHANT_TX_REFID);
      return false;
    }

    if (this.orderId != null && this.orderId.isEmpty()) {
      this.setErrorCode(HeaderCode.INVALID_ORDERID);
      return false;
    }

    return true;
  }
}
