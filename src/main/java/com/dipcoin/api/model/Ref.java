package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Ref {

  private String type;

  private String addr;

  private String accType;

  private String settAmount;

  private String settCurrency;

  private String approvalNum;

  private String errCode;

  private String avalBal;

  private String ledgerBal;

  private String maskedAccountNumber;

  private String customerName;

  @XmlAttribute
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlAttribute
  public String getAddr() {
    return addr;
  }

  public void setAddr(String addr) {
    this.addr = addr;
  }

  @XmlAttribute
  public String getAccType() {
    return accType;
  }

  public void setAccType(String accType) {
    this.accType = accType;
  }

  @XmlAttribute
  public String getSettAmount() {
    return settAmount;
  }

  public void setSettAmount(String settAmount) {
    this.settAmount = settAmount;
  }

  @XmlAttribute
  public String getSettCurrency() {
    return settCurrency;
  }

  public void setSettCurrency(String settCurrency) {
    this.settCurrency = settCurrency;
  }

  @XmlAttribute
  public String getApprovalNum() {
    return approvalNum;
  }

  public void setApprovalNum(String approvalNum) {
    this.approvalNum = approvalNum;
  }

  @XmlAttribute
  public String getErrCode() {
    return errCode;
  }

  public void setErrCode(String errCode) {
    this.errCode = errCode;
  }

  @XmlAttribute
  public String getAvalBal() {
    return avalBal;
  }

  public void setAvalBal(String avalBal) {
    this.avalBal = avalBal;
  }

  @XmlAttribute
  public String getLedgerBal() {
    return ledgerBal;
  }

  public void setLedgerBal(String ledgerBal) {
    this.ledgerBal = ledgerBal;
  }

  @XmlAttribute
  public String getMaskedAccountNumber() {
    return maskedAccountNumber;
  }

  public void setMaskedAccountNumber(String maskedAccountNumber) {
    this.maskedAccountNumber = maskedAccountNumber;
  }

  @XmlAttribute
  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

}
