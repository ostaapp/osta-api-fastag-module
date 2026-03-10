package com.dipcoin.api.model;

import org.apache.commons.lang3.StringUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class PartnerUserRegisterRequest extends UserRegisterRequest {

  @ApiModelProperty(required = true)
  private String partnerReferenceId;
  @ApiModelProperty(required = true)
  private String role;
  @ApiModelProperty(required = false)
  private String phoneAlias;
  @ApiModelProperty(required = false)
  private String agentId;
  @ApiModelProperty(required = false)
  private Boolean bbpsBranchTeller = false;

  @Override
  public boolean validate(HttpServletContext httpServletContext) {

    if (!super.validate(httpServletContext)) {
      return false;
    }

    if (StringUtils.isEmpty(partnerReferenceId)) {
      this.setErrorCode(HeaderCode.PARTNER_REF_ID_NOT_PROVIDED);
      return false;
    }

    if (StringUtils.isEmpty(role)) {
      this.setErrorCode(HeaderCode.MISSING_ROLE);
      return false;
    }
    
    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {}

}
