package com.dipcoin.mock.bank.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.annotations.ApiModelProperty;

public class UserData {
  private String Id;
  
  private String bankID;
  
  private String ifscCode;
  
  private String accountNumber;
  
  private String totalAmount;
  
  private String amountOnHold;
  
  private String availableAmount;
  
  private String authenticationType;
  
  private String authenticationId;
  
  private String password;
  
  private String otpCode;
  
  private boolean otpVerified;
  
  private String otpAttempt;
  
  private String otpVerifyTime;
  
  private String status;
  
  @ApiModelProperty(required = true)
  private String encryptedCredential;
  
  private static Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
  
  public String getAuthenticationType() {
    return this.authenticationType;
  }
  
  public void setAuthenticationType(String authenticationType) {
    this.authenticationType = authenticationType;
  }
  
  public String getAuthenticationId() {
    return this.authenticationId;
  }
  
  public void setAuthenticationId(String authenticationId) {
    this.authenticationId = authenticationId;
  }
  
  public String getPassword() {
    return this.password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
  
  public String getAccountNumber() {
    return this.accountNumber;
  }
  
  public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
  }
  
  public String getOtpCode() {
    return this.otpCode;
  }
  
  public void setOtpCode(String otpCode) {
    this.otpCode = otpCode;
  }
  
  public String getAvailableAmount() {
    return this.availableAmount;
  }
  
  public void setAvailableAmount(String availableAmount) {
    this.availableAmount = availableAmount;
  }
  
  public String getBankID() {
    return this.bankID;
  }
  
  public void setBankID(String bankID) {
    this.bankID = bankID;
  }
  
  public boolean isOtpVerified() {
    return this.otpVerified;
  }
  
  public void setOtpVerified(boolean otpVerified) {
    this.otpVerified = otpVerified;
  }
  
  public String getOtpAttempt() {
    return this.otpAttempt;
  }
  
  public void setOtpAttempt(String otpAttempt) {
    this.otpAttempt = otpAttempt;
  }
  
  public String toString() {
    return gson.toJson(this);
  }
  
  public final String getEncryptedCredential() {
    return this.encryptedCredential;
  }
  
  public final void setEncryptedCredential(String encryptedCredential) {
    this.encryptedCredential = encryptedCredential;
  }
  
  public String getStatus() {
    return this.status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
  
  public String getIfscCode() {
    return this.ifscCode;
  }
  
  public void setIfscCode(String ifscCode) {
    this.ifscCode = ifscCode;
  }
  
  public String getId() {
    return this.Id;
  }
  
  public void setId(String Id) {
    this.Id = Id;
  }
  
  public String getTotalAmount() {
    return this.totalAmount;
  }
  
  public void setTotalAmount(String totalAmount) {
    this.totalAmount = totalAmount;
  }
  
  public String getAmountOnHold() {
    return this.amountOnHold;
  }
  
  public void setAmountOnHold(String amountOnHold) {
    this.amountOnHold = amountOnHold;
  }
  
  public String getOtpVerifyTime() {
    return this.otpVerifyTime;
  }
  
  public void setOtpVerifyTime(String otpVerifyTime) {
    this.otpVerifyTime = otpVerifyTime;
  }
}
