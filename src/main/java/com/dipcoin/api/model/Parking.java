package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Parking {

  private String floor;

  private String zone;

  private String slotId;

  private String readerId;

  @XmlAttribute
  public String getFloor() {
    return floor;
  }

  public void setFloor(String floor) {
    this.floor = floor;
  }

  @XmlAttribute
  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  @XmlAttribute
  public String getSlotId() {
    return slotId;
  }

  public void setSlotId(String slotId) {
    this.slotId = slotId;
  }

  @XmlAttribute
  public String getReaderId() {
    return readerId;
  }

  public void setReaderId(String readerId) {
    this.readerId = readerId;
  }

}
