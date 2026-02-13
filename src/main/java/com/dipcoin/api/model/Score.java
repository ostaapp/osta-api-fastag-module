package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Score {

  private String provider;

  private String type;

  private String value;

  @XmlAttribute
  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  @XmlAttribute
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlAttribute
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
