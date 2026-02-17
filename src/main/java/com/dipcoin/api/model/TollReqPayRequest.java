											package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ReqPay", namespace = "http://npci.org/etc/schema/")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TollReqPayRequest {

  private Head head;

  private Meta meta;

  private Txn txn;

  private Merchant merchant;

  private Vehicle vehicle;

  private Payer payer;

  private Payee payee;

  @XmlElement(name = "Head")
  public Head getHead() {
    return head;
  }

  public void setHead(Head head) {
    this.head = head;
  }

  @XmlElement(name = "Txn")
  public Txn getTxn() {
    return txn;
  }

  public void setTxn(Txn txn) {
    this.txn = txn;
  }

  @XmlElement(name = "Meta")
  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  @XmlElement(name = "Payer")
  public Payer getPayer() {
    return payer;
  }

  public void setPayer(Payer payer) {
    this.payer = payer;
  }

  @XmlElement(name = "Payee")
  public Payee getPayee() {
    return payee;
  }

  public void setPayee(Payee payee) {
    this.payee = payee;
  }

  @XmlElement(name = "Merchant")
  public Merchant getMerchant() {
    return merchant;
  }

  public void setMerchant(Merchant merchant) {
    this.merchant = merchant;
  }

  @XmlElement(name = "Vehicle")
  public Vehicle getVehicle() {
    return vehicle;
  }

  public void setVehicle(Vehicle vehicle) {
    this.vehicle = vehicle;
  }

}
