package com.dipcoin.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Resp {

  private Ref ref;

  private String merchantId;

  private String ts;

  private String result;

  private String respCode;

  private String totReqCnt;

  private String sucessReqCnt;

  private Vehicle vehicle;

  private List<Tag> tag;
  
  private Time time;

  private List<TollException> tollException;

  @XmlElement(name = "Exception")
  public List<TollException> getTollException() {
    return tollException;
  }

  public void setTollException(List<TollException> tollException) {
    this.tollException = tollException;
  }

  @XmlAttribute(name = "merchantId")
  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  @XmlAttribute(name = "ts")
  public String getTs() {
    return ts;
  }

  public void setTs(String ts) {
    this.ts = ts;
  }

  @XmlAttribute(name = "result")
  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @XmlAttribute(name = "respCode")
  public String getRespCode() {
    return respCode;
  }

  public void setRespCode(String respCode) {
    this.respCode = respCode;
  }

  @XmlElement(name = "Ref")
  public Ref getRef() {
    return ref;
  }

  public void setRef(Ref ref) {
    this.ref = ref;
  }

  @XmlAttribute(name = "totReqCnt")
  public String getTotReqCnt() {
    return totReqCnt;
  }

  public void setTotReqCnt(String totReqCnt) {
    this.totReqCnt = totReqCnt;
  }

  @XmlAttribute(name = "successReqCnt")
  public String getSucessReqCnt() {
    return sucessReqCnt;
  }

  public void setSucessReqCnt(String sucessReqCnt) {
    this.sucessReqCnt = sucessReqCnt;
  }

  @XmlElement(name = "Vehicle")
  public Vehicle getVehicle() {
    return vehicle;
  }

  public void setVehicle(Vehicle vehicle) {
    this.vehicle = vehicle;
  }

  @XmlElement(name = "Tag")
  public List<Tag> getTag() {
    return tag;
  }

  public void setTag(List<Tag> tag) {
    this.tag = tag;
  }

  @XmlElement(name = "Time")
  public Time getTime() {
    return time;
  }

  public void setTime(Time time) {
    this.time = time;
  }

}

