package com.dipcoin.api.model;

import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.core.CryptoUtil.SupportedEncoding;
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
public class TollRegistrationRequest extends APIRequest {
  private static final Logger LOG = LogManager.getLogger(TollRegistrationRequest.class);

  private Integer id;
  private Integer userId;
  @ApiModelProperty(required = true)
  private String firstName;
  @ApiModelProperty(required = true)
  private String lastName;
  @ApiModelProperty(required = true)
  private String gender;
  @ApiModelProperty(required = true)
  private String dob;
  @ApiModelProperty(required = true)
  private String mobileNo;
  @ApiModelProperty(required = true)
  private String emailId;
  @ApiModelProperty(required = true)
  private String address;
  @ApiModelProperty(required = true)
  private String city;
  @ApiModelProperty(required = true)
  private String state;
  @ApiModelProperty(required = true)
  private Integer pincode;
  @ApiModelProperty(required = true)
  private String idProofType;
  @ApiModelProperty(required = true)
  private String idProofNo;
  @ApiModelProperty(required = true)
  private String idProofPath;
  @ApiModelProperty(required = true)
  private String status;
  @ApiModelProperty(required = true)
  private List<TollTagRequest> vehicleList;
  @ApiModelProperty(required = true)
  private String clientTransactionId;

  @ApiModelProperty(required = true)
  private String authorizationPin;
  @ApiModelProperty(required = true)
  private String currency;

  private String ipAddress;
  private String createdDateTime;
  private String createdby;
  @ApiModelProperty(required = true)
  private Integer cardId;
  @ApiModelProperty(required = true)
  private BigDecimal autoTopUpAmount ;
  @ApiModelProperty(required = true)
  private BigDecimal autoTopUpThresHold;
  @ApiModelProperty(required = true)
  private Integer autoTopUp;
  private String accountNumber;
  private BigDecimal minimumAmount;
  private String plainTempPassword;
  private String vehicleTypeName;
  
  private Boolean miscCharges = Boolean.TRUE;
  
 
  

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    try {

    } catch (Exception e) {

    }
  }

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (StringUtils.isEmpty(getFirstName())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_FNAME);
      return false;
    }
    if (StringUtils.isEmpty(getLastName())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_LNAME);
      return false;
    }
    if (StringUtils.isEmpty(getGender())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_GENDER);
      return false;
    }
    if (StringUtils.isEmpty(getDob())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_DOB);
      return false;
    }

    if (StringUtils.isEmpty(getMobileNo()) || getMobileNo().length() != DBConstants.MOBILE_LENGTH
        || !NumberUtils.isCreatable(getMobileNo())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_MOBILE);
      return false;
    }

    if (StringUtils.isEmpty(getEmailId()) || !EmailValidator.getInstance().isValid(getEmailId())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_EMAIL);
      return false;
    }
    if (StringUtils.isEmpty(getAddress())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_ADDRESS);
      return false;
    }
    if (StringUtils.isEmpty(getCity())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_CITY);
      return false;
    }
    if (StringUtils.isEmpty(getState())) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_STATE);
      return false;
    }
    if (getPincode() == null) {
      this.setErrorCode(HeaderCode.TOLL_USER_INVALID_PINCODE);
      return false;
    }

    if (getCardId() == null) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }
    // if (StringUtils.isEmpty(getIdProofType())) {
    // this.setErrorCode(HeaderCode.TOLL_USER_INVALID_IDPROOFTYPE);
    // return false;
    // }
    // if (StringUtils.isEmpty(getIdProofNo())) {
    // this.setErrorCode(HeaderCode.TOLL_USER_INVALID_IDPROOFNO);
    // return false;
    // }
    // if (StringUtils.isEmpty(getIdProofPath())) {
    // this.setErrorCode(HeaderCode.TOLL_USER_INVALID_IDPROOFPATH);
    // return false;
    // }

    return true;
  }

}
