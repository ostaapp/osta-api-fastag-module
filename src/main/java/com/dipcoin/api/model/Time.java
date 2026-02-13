package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Time { 

  private String serverTime;

  @XmlAttribute
  public String getServerTime() {
    return serverTime;
  }

  public void setServerTime(String serverTime) {
    this.serverTime = serverTime;
  }

}
