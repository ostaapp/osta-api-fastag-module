package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class ReaderVerificationResult {

  private String tsRead;

  private String signData;

  private String signAuth;

  private String epcVerified;

  private String procRestrictionResult;

  private String vehicleAuth;

  private String publicKeyCVV;

  private String txnCounter;

  private String txnStatus;

  @XmlAttribute
  public String getTsRead() {
    return tsRead;
  }


  public void setTsRead(String tsRead) {
    this.tsRead = tsRead;
  }

  @XmlAttribute
  public String getSignData() {
    return signData;
  }

  public void setSignData(String signData) {
    this.signData = signData;
  }

  @XmlAttribute
  public String getSignAuth() {
    return signAuth;
  }

  public void setSignAuth(String signAuth) {
    this.signAuth = signAuth;
  }

  @XmlAttribute
  public String getEpcVerified() {
    return epcVerified;
  }

  public void setEpcVerified(String epcVerified) {
    this.epcVerified = epcVerified;
  }

  @XmlAttribute
  public String getProcRestrictionResult() {
    return procRestrictionResult;
  }

  public void setProcRestrictionResult(String procRestrictionResult) {
    this.procRestrictionResult = procRestrictionResult;
  }

  @XmlAttribute
  public String getVehicleAuth() {
    return vehicleAuth;
  }

  public void setVehicleAuth(String vehicleAuth) {
    this.vehicleAuth = vehicleAuth;
  }

  @XmlAttribute
  public String getPublicKeyCVV() {
    return publicKeyCVV;
  }

  public void setPublicKeyCVV(String publicKeyCVV) {
    this.publicKeyCVV = publicKeyCVV;
  }

  @XmlAttribute
  public String getTxnCounter() {
    return txnCounter;
  }

  public void setTxnCounter(String txnCounter) {
    this.txnCounter = txnCounter;
  }

  @XmlAttribute
  public String getTxnStatus() {
    return txnStatus;
  }

  public void setTxnStatus(String txnStatus) {
    this.txnStatus = txnStatus;
  }

}
