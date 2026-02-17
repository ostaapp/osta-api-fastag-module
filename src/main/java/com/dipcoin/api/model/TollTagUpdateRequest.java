package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
//import com.dipcoin.partner.toll.model.Head;
//import com.dipcoin.partner.toll.model.Txn;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "etc:ReqMngTagEntries")

public class TollTagUpdateRequest {

  private Head head;

  private Txn txn;

  @XmlAttribute(name = "xmlns:etc")
  private String etc = "http://npci.org/etc/schema/";

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

}
