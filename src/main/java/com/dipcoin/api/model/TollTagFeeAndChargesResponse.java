package com.dipcoin.api.model;


import java.math.BigDecimal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.db.services.model.TollRegistration;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TollTagFeeAndChargesResponse extends APIResponse {
  private static final Logger LOG = LogManager.getLogger(TollTagFeeAndChargesResponse.class);


  private int id;
  private int bankId;
  private String chargeCategory;
  private BigDecimal depositAmount;
  private BigDecimal minimumAmount;
  private BigDecimal minAmountMaintainForTag;
  private BigDecimal minAmountMaintainForAccount;
  private BigDecimal registrationAmount;
  private BigDecimal gstAmount;
  private String updateOn;
  private String createdOn;
  
  private BigDecimal courierCharges;

  public int getId() {
    return id;
  }

  public TollTagFeeAndChargesResponse setId(int id) {
    this.id = id;
    return this;
  }

  public int getBankId() {
    return bankId;
  }

  public TollTagFeeAndChargesResponse setBankId(int bankId) {
    this.bankId = bankId;
    return this;
  }

  public String getChargeCategory() {
    return chargeCategory;
  }

  public TollTagFeeAndChargesResponse setChargeCategory(String chargeCategory) {
    this.chargeCategory = chargeCategory;
    return this;
  }

  public BigDecimal getDepositAmount() {
    return depositAmount;
  }

  public TollTagFeeAndChargesResponse setDepositAmount(BigDecimal depositAmount) {
    this.depositAmount = depositAmount;
    return this;
  }

  public BigDecimal getMinimumAmount() {
    return minimumAmount;
  }

  public TollTagFeeAndChargesResponse setMinimumAmount(BigDecimal minimumAmount) {
    this.minimumAmount = minimumAmount;
    return this;
  }

  public BigDecimal getRegistrationAmount() {
    return registrationAmount;
  }

  public TollTagFeeAndChargesResponse setRegistrationAmount(BigDecimal registrationAmount) {
    this.registrationAmount = registrationAmount;
    return this;
  }

  public String getUpdateOn() {
    return updateOn;
  }

  public TollTagFeeAndChargesResponse setUpdateOn(String updateOn) {
    this.updateOn = updateOn;
    return this;
  }

  public String getCreatedOn() {
    return createdOn;
  }

  public TollTagFeeAndChargesResponse setCreatedOn(String createdOn) {
    this.createdOn = createdOn;
    return this;
  }

  public BigDecimal getGstAmount() {
    return gstAmount;
  }

  public TollTagFeeAndChargesResponse setGstAmount(BigDecimal gstAmount) {
    this.gstAmount = gstAmount;
    return this;
  }

  public BigDecimal getCourierCharges() {
    return courierCharges;
  }

  public TollTagFeeAndChargesResponse setCourierCharges(BigDecimal courierCharges) {
    this.courierCharges = courierCharges;
    return this;
  }

  public BigDecimal getMinAmountMaintainForTag() {
    return minAmountMaintainForTag;
  }

  public BigDecimal getMinAmountMaintainForAccount() {
    return minAmountMaintainForAccount;
  }

  public TollTagFeeAndChargesResponse setMinAmountMaintainForTag(BigDecimal minAmountMaintainForTag) {
    this.minAmountMaintainForTag = minAmountMaintainForTag;
    return this;
  }

  public void setMinAmountMaintainForAccount(BigDecimal minAmountMaintainForAccount) {
    this.minAmountMaintainForAccount = minAmountMaintainForAccount;
  }
  
}
