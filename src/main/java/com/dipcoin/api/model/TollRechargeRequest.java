package com.dipcoin.api.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import org.apache.commons.lang3.StringUtils;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;


@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class TollRechargeRequest extends APIRequest {

  private String bankReferenceId;
  private String tagId;
  private String type;
  private Integer dipcoinId;
  private String automaticFlag;
  private BigDecimal rechargeAmount;
  private BigDecimal availableAmount;
  @ApiModelProperty(required = true)
  private Integer cardId;
  @ApiModelProperty(required = false)
  private Integer usageType = DipcoinUsageType.TOLL.value();
  @ApiModelProperty(required = false)
  private String usageCategory;
  @ApiModelProperty(required = true)
  private String authorizationPin;
  @ApiModelProperty(required = true)
  private String currency;
  private String dipcoinTransactionId;

  @Column(name = "CustomerAccountId")
  private Integer customerAccountId;
  
  @ApiModelProperty(required = false)
  private String vehicleRegNo;
  
 
  private String initiatedFrom = APIConstants.WEB;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {

    if (StringUtils.isEmpty(String.valueOf(getRechargeAmount()))) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }

    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {

  }

}
