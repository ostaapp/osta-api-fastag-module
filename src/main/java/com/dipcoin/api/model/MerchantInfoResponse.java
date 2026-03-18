package com.dipcoin.api.model;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MerchantInfoResponse extends APIResponse {

  private String activationEndDate;
  private String activationStartDate;
  private String amcFeeAmount;
  private Integer applyAmcFee;
  private Integer applyPerTranFee;
  private Integer applySetUpFee;
  private String billingAddressLine1;
  private String billingAddressLine2;
  private String billingAddressLine3;
  private String billingAddressLine4;
  private String billingCity;
  private String billingCountry;
  private String billingDistrict;
  private String billingFaxNumber;
  private String billingPinCode;
  private String billingState;
  private String boardNumber;
  private Integer businessCategory;
  private Integer businessType;
  private Integer channelType;
  private String clientTransactionId;
  private String currency;
  private String description;
  private String merchantAlias;
  private Integer emailAlertActivation;
  private String emailId;
  private Integer feeChargeMethod;
  private String IntegeregrationType;
  private String logoURL;
  private Integer mobileAppActivation;
  private Integer multiUserAccess;
  private String name;
  private Integer numberOfUsers;
  private Integer numOfEmp;
  private String officeAddressLine1;
  private String officeAddressLine2;
  private String officeAddressLine3;
  private String officeAddressLine4;
  private String officeCity;
  private String officeCountry;
  private String officeDistrict;
  private String officeFaxNumber;
  private String officeNumber;
  private String officePincode;
  private String officeState;
  private String perTranFeeAmount;
  private String partnerReferenceId; // referenceId
  private Integer settlementCycle;
  private Integer settlementType;
  private String setupFeeAmount;
  private Integer SMSAlertActivation;
  private Integer status;
  private Integer type;
  private String url;
  private Integer yearofEstablishment;
  private Integer partnerType;
  private Integer businessSegment;
  private Integer customReportUI;
  
/*
   * @Doc related fields
   */
  private String panId;
  private Boolean panUploaded;
  private String aadharCardId;
  private Boolean aadharCardUploaded;
  private String incorporationCertId;
  private Boolean incorporationCertUploaded;
  private String tanId;
  private Boolean tanUploaded;
  private String telephoneBillId;
  private Boolean telephoneBillUploaded;
  private String utilityBillId;
  private Boolean utilityBillUploded;
  private String serviceTaxId;
  private Boolean serviceTaxUploaded;
  private String bankLetterId;
  private Boolean bankLetterUploaded;
  private String vatId;
  private Boolean vatUploaded;
  private String salesTaxId;
  private Boolean salesTaxUploaded;
  private String gstId;
  private Boolean gstUploaded;
  private String sourceIdentifier;
  private List<APICustomization> apiCustomization;
  private PartnerApprovalStatus partnerApprovalStatus;
  private Integer merchantId;
  private List<BankInfoResponse> fastagBank;

  public Integer getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(Integer merchantId) {
    this.merchantId = merchantId;
  }

  public final String getActivationEndDate() {
    return activationEndDate;
  }

  public final void setActivationEndDate(String activationEndDate) {
    this.activationEndDate = activationEndDate;
  }

  public final String getActivationStartDate() {
    return activationStartDate;
  }

  public final void setActivationStartDate(String activationStartDate) {
    this.activationStartDate = activationStartDate;
  }

  public final String getAmcFeeAmount() {
    return amcFeeAmount;
  }

  public final void setAmcFeeAmount(String amcFeeAmount) {
    this.amcFeeAmount = amcFeeAmount;
  }

  public final Integer getApplyAmcFee() {
    return applyAmcFee;
  }

  public final void setApplyAmcFee(Integer applyAmcFee) {
    this.applyAmcFee = applyAmcFee;
  }

  public final Integer getApplyPerTranFee() {
    return applyPerTranFee;
  }

  public final void setApplyPerTranFee(Integer applyPerTranFee) {
    this.applyPerTranFee = applyPerTranFee;
  }

  public final Integer getApplySetUpFee() {
    return applySetUpFee;
  }

  public final void setApplySetUpFee(Integer applySetUpFee) {
    this.applySetUpFee = applySetUpFee;
  }

  public final String getBillingAddressLine1() {
    return billingAddressLine1;
  }

  public final void setBillingAddressLine1(String billingAddressLine1) {
    this.billingAddressLine1 = billingAddressLine1;
  }

  public final String getBillingAddressLine2() {
    return billingAddressLine2;
  }

  public final void setBillingAddressLine2(String billingAddressLine2) {
    this.billingAddressLine2 = billingAddressLine2;
  }

  public final String getBillingAddressLine3() {
    return billingAddressLine3;
  }

  public final void setBillingAddressLine3(String billingAddressLine3) {
    this.billingAddressLine3 = billingAddressLine3;
  }

  public final String getBillingAddressLine4() {
    return billingAddressLine4;
  }

  public final void setBillingAddressLine4(String billingAddressLine4) {
    this.billingAddressLine4 = billingAddressLine4;
  }

  public final String getBillingCity() {
    return billingCity;
  }

  public final void setBillingCity(String billingCity) {
    this.billingCity = billingCity;
  }

  public final String getBillingCountry() {
    return billingCountry;
  }

  public final void setBillingCountry(String billingCountry) {
    this.billingCountry = billingCountry;
  }

  public final String getBillingDistrict() {
    return billingDistrict;
  }

  public final void setBillingDistrict(String billingDistrict) {
    this.billingDistrict = billingDistrict;
  }

  public final String getBillingFaxNumber() {
    return billingFaxNumber;
  }

  public final void setBillingFaxNumber(String billingFaxNumber) {
    this.billingFaxNumber = billingFaxNumber;
  }

  public final String getBillingPinCode() {
    return billingPinCode;
  }

  public final void setBillingPinCode(String billingPinCode) {
    this.billingPinCode = billingPinCode;
  }

  public final String getBillingState() {
    return billingState;
  }

  public final void setBillingState(String billingState) {
    this.billingState = billingState;
  }

  public final String getBoardNumber() {
    return boardNumber;
  }

  public final void setBoardNumber(String boardNumber) {
    this.boardNumber = boardNumber;
  }

  public final Integer getBusinessCategory() {
    return businessCategory;
  }

  public final void setBusinessCategory(Integer businessCategory) {
    this.businessCategory = businessCategory;
  }

  public final Integer getBusinessType() {
    return businessType;
  }

  public final void setBusinessType(Integer businessType) {
    this.businessType = businessType;
  }

  public final Integer getChannelType() {
    return channelType;
  }

  public final void setChannelType(Integer channelType) {
    this.channelType = channelType;
  }

  public final String getClientTransactionId() {
    return clientTransactionId;
  }

  public final void setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }

  public final String getCurrency() {
    return currency;
  }

  public final void setCurrency(String currency) {
    this.currency = currency;
  }

  public final String getDescription() {
    return description;
  }

  public final void setDescription(String description) {
    this.description = description;
  }

  public final String getMerchantAlias() {
    return this.merchantAlias;
  }

  public final void setMerchantAlias(String merchantAlias) {
    this.merchantAlias = merchantAlias;
  }

  public final Integer getEmailAlertActivation() {
    return emailAlertActivation;
  }

  public final void setEmailAlertActivation(Integer emailAlertActivation) {
    this.emailAlertActivation = emailAlertActivation;
  }

  public final String getEmailId() {
    return emailId;
  }

  public final void setEmailId(String emailId) {
    this.emailId = emailId;
  }

  public final Integer getFeeChargeMethod() {
    return feeChargeMethod;
  }

  public final void setFeeChargeMethod(Integer feeChargeMethod) {
    this.feeChargeMethod = feeChargeMethod;
  }

  public final String getIntegrationType() {
    return IntegeregrationType;
  }

  public final void setIntegrationType(String IntegeregrationType) {
    this.IntegeregrationType = IntegeregrationType;
  }

  public final String getLogoURL() {
    return logoURL;
  }

  public final void setLogoURL(String logoURL) {
    this.logoURL = logoURL;
  }

  public final Integer getMobileAppActivation() {
    return mobileAppActivation;
  }

  public final void setMobileAppActivation(Integer mobileAppActivation) {
    this.mobileAppActivation = mobileAppActivation;
  }

  public final Integer getMultiUserAccess() {
    return multiUserAccess;
  }

  public final void setMultiUserAccess(Integer multiUserAccess) {
    this.multiUserAccess = multiUserAccess;
  }

  public final String getName() {
    return name;
  }

  public final void setName(String name) {
    this.name = name;
  }

  public final Integer getNumberOfUsers() {
    return numberOfUsers;
  }

  public final void setNumberOfUsers(Integer numberOfUsers) {
    this.numberOfUsers = numberOfUsers;
  }

  public final Integer getNumOfEmp() {
    return numOfEmp;
  }

  public final void setNumOfEmp(Integer numOfEmp) {
    this.numOfEmp = numOfEmp;
  }

  public final String getOfficeAddressLine1() {
    return officeAddressLine1;
  }

  public final void setOfficeAddressLine1(String officeAddressLine1) {
    this.officeAddressLine1 = officeAddressLine1;
  }

  public final String getOfficeAddressLine2() {
    return officeAddressLine2;
  }

  public final void setOfficeAddressLine2(String officeAddressLine2) {
    this.officeAddressLine2 = officeAddressLine2;
  }

  public final String getOfficeAddressLine3() {
    return officeAddressLine3;
  }

  public final void setOfficeAddressLine3(String officeAddressLine3) {
    this.officeAddressLine3 = officeAddressLine3;
  }

  public final String getOfficeAddressLine4() {
    return officeAddressLine4;
  }

  public final void setOfficeAddressLine4(String officeAddressLine4) {
    this.officeAddressLine4 = officeAddressLine4;
  }

  public final String getOfficeCity() {
    return officeCity;
  }

  public final void setOfficeCity(String officeCity) {
    this.officeCity = officeCity;
  }

  public final String getOfficeCountry() {
    return officeCountry;
  }

  public final void setOfficeCountry(String officeCountry) {
    this.officeCountry = officeCountry;
  }

  public final String getOfficeDistrict() {
    return officeDistrict;
  }

  public final void setOfficeDistrict(String officeDistrict) {
    this.officeDistrict = officeDistrict;
  }

  public final String getOfficeFaxNumber() {
    return officeFaxNumber;
  }

  public final void setOfficeFaxNumber(String officeFaxNumber) {
    this.officeFaxNumber = officeFaxNumber;
  }

  public final String getOfficeNumber() {
    return officeNumber;
  }

  public final void setOfficeNumber(String officeNumber) {
    this.officeNumber = officeNumber;
  }

  public final String getOfficePincode() {
    return officePincode;
  }

  public final void setOfficePincode(String officePincode) {
    this.officePincode = officePincode;
  }

  public final String getOfficeState() {
    return officeState;
  }

  public final void setOfficeState(String officeState) {
    this.officeState = officeState;
  }

  public final String getPerTranFeeAmount() {
    return perTranFeeAmount;
  }

  public final void setPerTranFeeAmount(String perTranFeeAmount) {
    this.perTranFeeAmount = perTranFeeAmount;
  }

  public final String getPartnerReferenceId() {
    return partnerReferenceId;
  }

  public final void setPartnerReferenceId(String partnerReferenceId) {
    this.partnerReferenceId = partnerReferenceId;
  }

  public final Integer getSettlementCycle() {
    return settlementCycle;
  }

  public final void setSettlementCycle(Integer settlementCycle) {
    this.settlementCycle = settlementCycle;
  }

  public final Integer getSettlementType() {
    return settlementType;
  }

  public final void setSettlementType(Integer settlementType) {
    this.settlementType = settlementType;
  }

  public final String getSetupFeeAmount() {
    return setupFeeAmount;
  }

  public final void setSetupFeeAmount(String setupFeeAmount) {
    this.setupFeeAmount = setupFeeAmount;
  }

  public final Integer getSMSAlertActivation() {
    return SMSAlertActivation;
  }

  public final void setSMSAlertActivation(Integer sMSAlertActivation) {
    SMSAlertActivation = sMSAlertActivation;
  }

  public final Integer getStatus() {
    return status;
  }

  public final void setStatus(Integer status) {
    this.status = status;
  }

  public final Integer getType() {
    return type;
  }

  public final void setType(Integer type) {
    this.type = type;
  }

  public final String getUrl() {
    return url;
  }

  public final void setUrl(String url) {
    this.url = url;
  }

  public final Integer getYearofEstablishment() {
    return yearofEstablishment;
  }

  public final void setYearofEstablishment(Integer yearofEstablishment) {
    this.yearofEstablishment = yearofEstablishment;
  }

  public final String getPanId() {
    return panId;
  }

  public final void setPanId(String panId) {
    this.panId = panId;
  }

  public final Boolean isPanUploaded() {
    return panUploaded;
  }

  public final void setPanUploaded(Boolean panUploaded) {
    this.panUploaded = panUploaded;
  }

  public final String getAadharCardId() {
    return aadharCardId;
  }

  public final void setAadharCardId(String aadharCardId) {
    this.aadharCardId = aadharCardId;
  }

  public final Boolean isAadharCardUploaded() {
    return aadharCardUploaded;
  }

  public final void setAadharCardUploaded(Boolean aadharCardUploaded) {
    this.aadharCardUploaded = aadharCardUploaded;
  }

  public final String getIncorporationCertId() {
    return incorporationCertId;
  }

  public final void setIncorporationCertId(String incorporationCertId) {
    this.incorporationCertId = incorporationCertId;
  }

  public final Boolean isIncorporationCertUploaded() {
    return incorporationCertUploaded;
  }

  public final void setIncorporationCertUploaded(Boolean incorporationCertUploaded) {
    this.incorporationCertUploaded = incorporationCertUploaded;
  }

  public final String getTanId() {
    return tanId;
  }

  public final void setTanId(String tanId) {
    this.tanId = tanId;
  }

  public final Boolean isTanUploaded() {
    return tanUploaded;
  }

  public final void setTanUploaded(Boolean tanUploaded) {
    this.tanUploaded = tanUploaded;
  }

  public final String getTelephoneBillId() {
    return telephoneBillId;
  }

  public final void setTelephoneBillId(String telephoneBillId) {
    this.telephoneBillId = telephoneBillId;
  }

  public final Boolean isTelephoneBillUploaded() {
    return telephoneBillUploaded;
  }

  public final void setTelephoneBillUploaded(Boolean telephoneBillUploaded) {
    this.telephoneBillUploaded = telephoneBillUploaded;
  }

  public final String getUtilityBillId() {
    return utilityBillId;
  }

  public final void setUtilityBillId(String utilityBillId) {
    this.utilityBillId = utilityBillId;
  }

  public final Boolean isUtilityBillUploded() {
    return utilityBillUploded;
  }

  public final void setUtilityBillUploded(Boolean utilityBillUploded) {
    this.utilityBillUploded = utilityBillUploded;
  }

  public final String getServiceTaxId() {
    return serviceTaxId;
  }

  public final void setServiceTaxId(String serviceTaxId) {
    this.serviceTaxId = serviceTaxId;
  }

  public final Boolean isServiceTaxUploaded() {
    return serviceTaxUploaded;
  }

  public final void setServiceTaxUploaded(Boolean serviceTaxUploaded) {
    this.serviceTaxUploaded = serviceTaxUploaded;
  }

  public final String getBankLetterId() {
    return bankLetterId;
  }

  public final void setBankLetterId(String bankLetterId) {
    this.bankLetterId = bankLetterId;
  }

  public final Boolean isBankLetterUploaded() {
    return bankLetterUploaded;
  }

  public final void setBankLetterUploaded(Boolean bankLetterUploaded) {
    this.bankLetterUploaded = bankLetterUploaded;
  }

  public final String getVatId() {
    return vatId;
  }

  public final void setVatId(String vatId) {
    this.vatId = vatId;
  }

  public final Boolean isVatUploaded() {
    return vatUploaded;
  }

  public final void setVatUploaded(Boolean vatUploaded) {
    this.vatUploaded = vatUploaded;
  }

  public final String getSalesTaxId() {
    return salesTaxId;
  }

  public final void setSalesTaxId(String salesTaxId) {
    this.salesTaxId = salesTaxId;
  }

  public final Boolean isSalesTaxUploaded() {
    return salesTaxUploaded;
  }

  public final void setSalesTaxUploaded(Boolean salesTaxUploaded) {
    this.salesTaxUploaded = salesTaxUploaded;
  }

  public String getGstId() {
    return gstId;
  }

  public void setGstId(String gstId) {
    this.gstId = gstId;
  }

  public Boolean getGstUploaded() {
    return gstUploaded;
  }

  public void setGstUploaded(Boolean gstUploaded) {
    this.gstUploaded = gstUploaded;
  }

  public PartnerApprovalStatus getPartnerApprovalStatus() {
    return partnerApprovalStatus;
  }

  public void setPartnerApprovalStatus(PartnerApprovalStatus partnerApprovalStatus) {
    this.partnerApprovalStatus = partnerApprovalStatus;
  }

  public Integer getPartnerType() {
    return partnerType;
  }

  public void setPartnerType(Integer partnerType) {
    this.partnerType = partnerType;
  }

  public Integer getBusinessSegment() {
	return businessSegment;
  }

  public void setBusinessSegment(Integer businessSegment) {
	this.businessSegment = businessSegment;
  }

  public Integer getCustomReportUI() {
	return customReportUI;
  }

  public void setCustomReportUI(Integer customReportUI) {
	 this.customReportUI = customReportUI;
  }

  public String getSourceIdentifier() {
    return sourceIdentifier;
  }

  public void setSourceIdentifier(String sourceIdentifier) {
    this.sourceIdentifier = sourceIdentifier;
  }

  public List<APICustomization> getApiCustomization() {
    return apiCustomization;
  }

  public void setApiCustomization(List<APICustomization> apiCustomization) {
    this.apiCustomization = apiCustomization;
  }
  
  public String getApiCustomizationAsString() {
    return StringUtils.join(apiCustomization, ",");
  }

  public List<BankInfoResponse> getFastagBank() {
	return fastagBank;
  }

  public void setFastagBank(List<BankInfoResponse> fastagBank) {
	this.fastagBank = fastagBank;
  }

}
