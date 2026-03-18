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
@XmlRootElement(name = "NotificationDeclineMngTagEntry", namespace = "http://npci.org/etc/schema/")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class NETCDeclineResponse {
  
  private Head head;

  private Txn txn;

  private Vehicle vehicle;

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

  @XmlElement(name = "Vehicle")
  public Vehicle getVehicle() {
    return vehicle;
  }

  public void setVehicle(Vehicle vehicle) {
    this.vehicle = vehicle;
  }


}
