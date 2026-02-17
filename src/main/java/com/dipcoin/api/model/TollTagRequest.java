package com.dipcoin.api.model;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
//import com.dipcoin.api.model.ServerIntegrationRequest.Api;
import com.dipcoin.bank.services.client.BankClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@ApiModel
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class TollTagRequest extends APIRequest {

  private static final Logger LOG = LogManager.getLogger(TollTagRequest.class);

  private Integer id;
  private Integer tollRegisterationId;
  private String approvalFlag;
  private String approvedBy;
  private String approvedDateTime;
  private String approvedIPAddress;
  private String autoTopUp;
  @ApiModelProperty(required = true)
  private String category;
  private String excCode;
  private String expiryDate;
  private String iin;
  private String serialNumber;
  private String operation;
  private Long startTime;
  private Long endTime;
  @ApiModelProperty(required = true)
  private String isCommercial;
  private String issueDate;
  private String rawResponse;
  private BigDecimal rechargeAmount;
  @ApiModelProperty(required = true)
  private String registeredState;
  @ApiModelProperty(required = true)
  private String registrationNo;
  private String rejectReason;
  private String remarks;
  private String requestTime;
  private String responseCode;
  private String responseMessage;
  private String responseTime;
  private String status;
  private String tagId;
  private String tid;
  private String type;
  private String vendorIPAddress;
  private String vendorUpdateBy;
  private String vendorUpdateDateTime;
  @ApiModelProperty(required = true)
  private String RCImage;
  private Integer categoryId;  
  private String excCodeAck;
  private String addTagResponse;
  private String addTagErrorCode;
  private String tollTagReferenceId;
  private String bankReferenceId;
  private Integer visibilityStatus;
  private Integer updateReq;
  private Integer  followedCount;
  private int transactionFileType;
  private String emailId;
  private String dispatchLocation;
  private String address;
  private int vinVrnFlag=NumberUtils.INTEGER_ZERO;
  private String zone;
  private String region;  
  private String tempPassword;
  private String vin;
  private String engineNo;
  private String registeredVehicle;
  private String tagDescriptor;
  private String vehicleDescriptor;
  private String nationalPermit;
  private String permitExpiryDate;
  private String stateShortCode;
  private Boolean isForceClose;
  private String version;



  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    try {

    } catch (Exception e) {

    }
  }

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (getId() == null) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (getTollRegisterationId() == null) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getType())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }

    if (StringUtils.isEmpty(getIsCommercial())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getCategory())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getRegistrationNo())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getRegisteredState())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getExcCode())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getExcCode())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getExcCode())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getAutoTopUp())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    if (StringUtils.isEmpty(getStatus())) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    return true;
  }

}
