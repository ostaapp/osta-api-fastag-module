package com.dipcoin.api.model;

import java.math.BigDecimal;
import com.dipcoin.api.model.PartnerProcessDipcoinRequest.SubPartner;
import com.dipcoin.api.model.PartnerRegisterCustomerRequest.AccountDetails;
import com.dipcoin.api.model.PartnerRegisterCustomerRequest.UserProfile;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
@JsonInclude(Include.NON_NULL)
public class PartnerEncryptedRequest {

  private BigDecimal amount;
  private String currency;
  private String orderId;
  private String partnerTransactionReferenceId;
  private SubPartner subPartner;
  
  private UserProfile userProfile;
  @ApiModelProperty(required = true)
  private AccountDetails accountDetails;
  @ApiModelProperty(required = true)
  private String isExistingUser;
  @ApiModelProperty(required = false)
  private UserProfile updatedDetails;
  
  @ApiModelProperty(required = true)
  private String bundleId;
  
  private String redirectURI;
  @ApiModelProperty(required = false)
  private String encryptedPayload;
  private Integer status;
  private Boolean isOstaPin;
  private int featureToOpen;
  private BankInfoResponse bankInfo;
  private String source;
  
  private String role;
  private String deviceId;
  private String userChannel;
  private String tnc;


 
}
