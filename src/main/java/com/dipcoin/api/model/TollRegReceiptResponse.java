package com.dipcoin.api.model;


import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TollRegReceiptResponse extends APIResponse {

  // Setter and Getter
  private BigDecimal registrationAmount;
  private BigDecimal depositAmount;
  private BigDecimal minimumAmount;
  private BigDecimal autoTopUpAmount;
  private BigDecimal gstAmount;
  private Integer autoTopUp;
  private BigDecimal autoTopUpThresHold;
  
  public Integer getAutoTopUp() {
    return autoTopUp;
  }
  public void setAutoTopUp(Integer autoTopUp) {
    this.autoTopUp = autoTopUp;
  }
  public BigDecimal getAutoTopUpThresHold() {
    return autoTopUpThresHold;
  }
  public void setAutoTopUpThresHold(BigDecimal autoTopUpThresHold) {
    this.autoTopUpThresHold = autoTopUpThresHold;
  }
  
  public BigDecimal getRegistrationAmount() {
    return registrationAmount;
  }
  public void setRegistrationAmount(BigDecimal registrationAmount) {
    this.registrationAmount = registrationAmount;
  }
  public BigDecimal getDepositAmount() {
    return depositAmount;
  }
  public void setDepositAmount(BigDecimal depositAmount) {
    this.depositAmount = depositAmount;
  }
  public BigDecimal getMinimumAmount() {
    return minimumAmount;
  }
  public void setMinimumAmount(BigDecimal minimumAmount) {
    this.minimumAmount = minimumAmount;
  }
  public BigDecimal getAutoTopUpAmount() {
    return autoTopUpAmount;
  }
  public void setAutoTopUpAmount(BigDecimal autoTopUpAmount) {
    this.autoTopUpAmount = autoTopUpAmount;
  }
  public BigDecimal getGstAmount() {
    return gstAmount;
  }
  public void setGstAmount(BigDecimal gstAmount) {
    this.gstAmount = gstAmount;
  }
 
}
