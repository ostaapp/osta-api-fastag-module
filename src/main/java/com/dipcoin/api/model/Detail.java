package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Detail {

  private String name;

  private String value;
  
  public Detail() {
    
  }
  
  public Detail(String name, String value) {
    super();
    this.name = name;
    this.value = value;
  }


  @XmlAttribute(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlAttribute(name = "value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}

