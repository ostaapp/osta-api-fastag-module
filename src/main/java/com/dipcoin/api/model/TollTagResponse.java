package com.dipcoin.api.model;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.db.services.model.TollRegistration;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
@Setter
@Getter
public class TollTagResponse extends APIResponse {
  private static final Logger LOG = LogManager.getLogger(TollTagResponse.class);

  private int tagDetailsId;
  private String approvalFlag;
  private int approvedBy;
  private String approvedDateTime;
  private String approvedIPAddress;
  private String autoTopUp;
  private String category;
  private String excCode;
  private String expiryDate;
  private String extraColumn1;
  private String extraColumn2;
  private String extraColumn3;
  private String isCommercial;
  private String issueDate;
  private String rawResponse;
  private BigDecimal rechargeAmount;
  private String registeredState;
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
  private String serialNumber;
  public String getSerialNumber() {
	return serialNumber;
}

public void setSerialNumber(String serialNumber) {
	this.serialNumber = serialNumber;
}

private String type;
  private String vendorIPAddress;
  private int vendorUpdateBy;
  private String vendorUpdateDateTime;
  private TollRegistration tollRegistration;

  private String clientTransactionId;
  private int bankId;
  private int customerAccountId;
  private int cardId;

  private int id;  
  private BigDecimal availableAmount;
  
  private String excCodeUpdateTime;
  
  private String updateExceptionErrorCode;
 
  private String addTagResponse;

  private String RCImage;
  
  private String addTagErrorCode;
  
  private String updateExceptionResponse;
  
  private BigDecimal depositAmount;
  
  private BigDecimal minimumAmount;
  
  private BigDecimal registrationAmount;  
  
  private String createdDateTime;
  
  private Integer categoryId;
  
  private String accountNumber;
  
  private BigDecimal autoTopUpAmount;
  
  private BigDecimal thresHoldAmount;
  
  private Integer autoTopUpflag;

  private String maker;
  
  private String checker;
  
  private BigDecimal ostaAmount;
  
  private String courierCompany;
  
  private String trackingId;
  
  private Integer miscCharges;
  
  private String tollTagReferenceId;
  
  private int walletFlag;
  
  private String issuerBankName;
  
  private int vinVrnFlag;
  
  // Setter and getter

  public int getTagDetailsId() {
    return tagDetailsId;
  }

  public TollRegistration getTollRegistration() {
    return tollRegistration;
  }

  public void setTollRegistration(TollRegistration tollRegistration) {
    this.tollRegistration = tollRegistration;
  }

  public String getClientTransactionId() {
    return clientTransactionId;
  }

  public void setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }

  public void setTagDetailsId(int tagDetailsId) {
    this.tagDetailsId = tagDetailsId;
  }

  public String getApprovalFlag() {
    return approvalFlag;
  }

  public void setApprovalFlag(String approvalFlag) {
    this.approvalFlag = approvalFlag;
  }

  public int getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(int approvedBy) {
    this.approvedBy = approvedBy;
  }

  public String getApprovedDateTime() {
    return approvedDateTime;
  }

  public void setApprovedDateTime(String approvedDateTime) {
    this.approvedDateTime = approvedDateTime;
  }

  public String getApprovedIPAddress() {
    return approvedIPAddress;
  }

  public void setApprovedIPAddress(String approvedIPAddress) {
    this.approvedIPAddress = approvedIPAddress;
  }

  public String getAutoTopUp() {
    return autoTopUp;
  }

  public void setAutoTopUp(String autoTopUp) {
    this.autoTopUp = autoTopUp;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getExcCode() {
    return excCode;
  }

  public void setExcCode(String excCode) {
    this.excCode = excCode;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
  }

  public String getExtraColumn1() {
    return extraColumn1;
  }

  public void setExtraColumn1(String extraColumn1) {
    this.extraColumn1 = extraColumn1;
  }

  public String getExtraColumn2() {
    return extraColumn2;
  }

  public void setExtraColumn2(String extraColumn2) {
    this.extraColumn2 = extraColumn2;
  }

  public String getExtraColumn3() {
    return extraColumn3;
  }

  public void setExtraColumn3(String extraColumn3) {
    this.extraColumn3 = extraColumn3;
  }

  public String getIsCommercial() {
    return isCommercial;
  }

  public void setIsCommercial(String isCommercial) {
    this.isCommercial = isCommercial;
  }

  public String getIssueDate() {
    return issueDate;
  }

  public void setIssueDate(String issueDate) {
    this.issueDate = issueDate;
  }

  public String getRawResponse() {
    return rawResponse;
  }

  public void setRawResponse(String rawResponse) {
    this.rawResponse = rawResponse;
  }



  public String getRegisteredState() {
    return registeredState;
  }

  public void setRegisteredState(String registeredState) {
    this.registeredState = registeredState;
  }

  public String getRegistrationNo() {
    return registrationNo;
  }

  public void setRegistrationNo(String registrationNo) {
    this.registrationNo = registrationNo;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }

  public String getRemarks() {
    return remarks;
  }

  public void setRemarks(String remarks) {
    this.remarks = remarks;
  }

  public String getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(String requestTime) {
    this.requestTime = requestTime;
  }

  public String getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(String responseCode) {
    this.responseCode = responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public void setResponseMessage(String responseMessage) {
    this.responseMessage = responseMessage;
  }

  public String getResponseTime() {
    return responseTime;
  }

  public void setResponseTime(String responseTime) {
    this.responseTime = responseTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getTagId() {
    return tagId;
  }

  public void setTagId(String tagId) {
    this.tagId = tagId;
  }

  public String getTid() {
    return tid;
  }

  public void setTid(String tid) {
    this.tid = tid;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getVendorIPAddress() {
    return vendorIPAddress;
  }

  public void setVendorIPAddress(String vendorIPAddress) {
    this.vendorIPAddress = vendorIPAddress;
  }

  public String getVendorUpdateDateTime() {
    return vendorUpdateDateTime;
  }

  public void setVendorUpdateDateTime(String vendorUpdateDateTime) {
    this.vendorUpdateDateTime = vendorUpdateDateTime;
  }

  public TollRegistration settingTollTagResponse(TollRegistration tollRegistration) {

    TollRegistration tollObject = new TollRegistration();

    tollObject.setId(tollRegistration.getId());
    tollObject.setFirstName(tollRegistration.getFirstName());
    tollObject.setLastName(tollRegistration.getLastName());
    tollObject.setStatus(tollRegistration.getStatus());

    return tollObject;

  }

  public int getCustomerAccountId() {
    return customerAccountId;
  }

  public void setCustomerAccountId(int customerAccountId) {
    this.customerAccountId = customerAccountId;
  }

  public int getBankId() {
    return bankId;
  }

  public void setBankId(int bankId) {
    this.bankId = bankId;
  }

  public int getCardId() {
    return cardId;
  }

  public void setCardId(int cardId) {
    this.cardId = cardId;
  }
  
  

}