package com.dipcoin.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TollException {

  private String excCode;

  private String lastFetchTime;

  private String desc;

  private String priority;

  private String result;

  private String errCode;

  private String totalTag;

  private String lastupdatedTime;

  private List<Tag> tag;

  @XmlElement(name = "Tag")
  public List<Tag> getTag() {
    return tag;
  }

  public void setTag(List<Tag> tag) {
    this.tag = tag;
  }

  @XmlAttribute(name = "desc")
  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  @XmlAttribute(name = "priority")
  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  @XmlAttribute(name = "result")
  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @XmlAttribute(name = "errCode")
  public String getErrCode() {
    return errCode;
  }

  public void setErrCode(String errCode) {
    this.errCode = errCode;
  }

  @XmlAttribute(name = "totalTag")
  public String getTotalTag() {
    return totalTag;
  }

  public void setTotalTag(String totalTag) {
    this.totalTag = totalTag;
  }

  @XmlAttribute(name = "lastupdatedTime")
  public String getLastupdatedTime() {
    return lastupdatedTime;
  }

  public void setLastupdatedTime(String lastupdatedTime) {
    this.lastupdatedTime = lastupdatedTime;
  }

  @XmlAttribute(name = "lastFetchTime")
  public String getLastFetchTime() {
    return lastFetchTime;
  }

  public void setLastFetchTime(String lastFetchTime) {
    this.lastFetchTime = lastFetchTime;
  }

  @XmlAttribute(name = "excCode")
  public String getExcCode() {
    return excCode;
  }

  public void setExcCode(String excCode) {
    this.excCode = excCode;
  }

}
