package com.dipcoin.api.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TollExceptionList {

  private List<TollException> tollException;

  @XmlElement(name = "Exception")
  public List<TollException> getException() {
    return tollException;
  }

  public void setException(List<TollException> tollException) {
    this.tollException = tollException;
  }
}
