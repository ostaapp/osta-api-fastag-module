package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Payee {


  private String addr;

  private String name;

  private String type;
  
  private String txnInitiator;

  @XmlAttribute
  public String getAddr() {
    return addr;
  }

  public void setAddr(String addr) {
    this.addr = addr;
  }

  @XmlAttribute
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlAttribute
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
  
  @XmlAttribute(name = "txnInitiator")
  public String getTxnInitiator() {
    return txnInitiator;
  }

  
  public void setTxnInitiator(String txnInitiator) {
    this.txnInitiator = txnInitiator;
  }

}
