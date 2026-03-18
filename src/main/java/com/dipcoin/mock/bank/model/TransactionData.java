package com.dipcoin.mock.bank.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.math.BigDecimal;

public class TransactionData {
  private String dipcoinAccountId;
  
  private String bankID;
  
  private String dipcoinRefNumber;
  
  private String transactionReferenceNumber;
  
  private String transactionType;
  
  private String transactionTypeDesc;
  
  private String transactionReqTime;
  
  private String transactionResTime;
  
  private String attempt;
  
  private String transactionStatus;
  
  private String transactionDesc;
  
  private String rawRequest;
  
  private BigDecimal amount;
  
  private String cbsJournal;
  
  public String getDipcoinRefNumber() {
    return this.dipcoinRefNumber;
  }
  
  public void setDipcoinRefNumber(String dipcoinRefNumber) {
    this.dipcoinRefNumber = dipcoinRefNumber;
  }
  
  public String getTransactionReqTime() {
    return this.transactionReqTime;
  }
  
  public void setTransactionReqTime(String transactionReqTime) {
    this.transactionReqTime = transactionReqTime;
  }
  
  public String getTransactionResTime() {
    return this.transactionResTime;
  }
  
  public void setTransactionResTime(String transactionResTime) {
    this.transactionResTime = transactionResTime;
  }
  
  public String getAttempt() {
    return this.attempt;
  }
  
  public void setAttempt(String attempt) {
    this.attempt = attempt;
  }
  
  public String getTransactionStatus() {
    return this.transactionStatus;
  }
  
  public void setTransactionStatus(String transactionStatus) {
    this.transactionStatus = transactionStatus;
  }
  
  public String getTransactionDesc() {
    return this.transactionDesc;
  }
  
  public void setTransactionDesc(String transactionDesc) {
    this.transactionDesc = transactionDesc;
  }
  
  private static Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
  
  public String getTransactionType() {
    return this.transactionType;
  }
  
  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }
  
  public String getTransactionReferenceNumber() {
    return this.transactionReferenceNumber;
  }
  
  public void setTransactionReferenceNumber(String transactionReferenceNumber) {
    this.transactionReferenceNumber = transactionReferenceNumber;
  }
  
  public String getTransactionTypeDesc() {
    return this.transactionTypeDesc;
  }
  
  public void setTransactionTypeDesc(String transactionTypeDesc) {
    this.transactionTypeDesc = transactionTypeDesc;
  }
  
  public String getRawRequest() {
    return this.rawRequest;
  }
  
  public void setRawRequest(String rawRequest) {
    this.rawRequest = rawRequest;
  }
  
  public String getDipcoinAccountId() {
    return this.dipcoinAccountId;
  }
  
  public void setDipcoinAccountId(String dipcoinAccountId) {
    this.dipcoinAccountId = dipcoinAccountId;
  }
  
  public String getBankID() {
    return this.bankID;
  }
  
  public void setBankID(String bankID) {
    this.bankID = bankID;
  }
  
  public BigDecimal getAmount() {
    return this.amount;
  }
  
  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
  
  public String getCbsJournal() {
    return this.cbsJournal;
  }
  
  public void setCbsJournal(String cbsJournal) {
    this.cbsJournal = cbsJournal;
  }
  
  public String toString() {
    return gson.toJson(this);
  }
}
