package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "etc:RespPay")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TollResPayResponse {


  @XmlAttribute(name = "xmlns:etc")
  private String etc = "http://npci.org/etc/schema/";

  private Head head;

  private Meta meta;

  private Txn txn;

  private Resp resp;

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

  @XmlElement(name = "Resp")
  public Resp getResp() {
    return resp;
  }

  public void setResp(Resp resp) {
    this.resp = resp;
  }

  @XmlElement(name = "Meta")
  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }
}
