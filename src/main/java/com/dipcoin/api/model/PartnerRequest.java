package com.dipcoin.api.model;

import java.math.BigDecimal;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.PartnerProcessDipcoinRequest.SubPartner;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionsStatus;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class PartnerRequest extends APIRequest {

  @ApiModelProperty(required = false)
  private BigDecimal amount;
  @ApiModelProperty(required = false)
  private String currency;
  @ApiModelProperty(required = false)
  private String orderId;
  @ApiModelProperty(required = false)
  private String partnerTransactionReferenceId;
  @ApiModelProperty(required = false)
  private SubPartner subPartner;
  @ApiModelProperty(required = false)
  private String redirectURI;
  @ApiModelProperty(required = false)
  private String encryptedPayload;
  @ApiModelProperty(required = false)
  private String merchantName;
  @ApiModelProperty(required = false)
  private String partnerRefrenceId;
  @ApiModelProperty(required = false)
  private String deviceHash;
  @ApiModelProperty(required = false)
  private String logoUrl;
  @ApiModelProperty(required = false)
  private Integer status = DipcoinTransactionsStatus.STATUS_NA.value();
  

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub
  }


}
