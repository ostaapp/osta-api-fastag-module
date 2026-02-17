package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Merchant {

  private Lane lane;

  private Parking parking;

  private ReaderVerificationResult readerVerificationResult;

  private String id;

  private String name;

  private String geoCode;

  private String type;

  private String subtype;

  @XmlElement(name = "Lane")
  public Lane getLane() {
    return lane;
  }

  public void setLane(Lane lane) {
    this.lane = lane;
  }

  @XmlElement(name = "Parking")
  public Parking getParking() {
    return parking;
  }

  public void setParking(Parking parking) {
    this.parking = parking;
  }

  @XmlElement(name = "ReaderVerificationResult")
  public ReaderVerificationResult getReaderVerificationResult() {
    return readerVerificationResult;
  }

  public void setReaderVerificationResult(ReaderVerificationResult readerVerificationResult) {
    this.readerVerificationResult = readerVerificationResult;
  }

  @XmlAttribute
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @XmlAttribute
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @XmlAttribute
  public String getGeoCode() {
    return geoCode;
  }

  public void setGeoCode(String geoCode) {
    this.geoCode = geoCode;
  }

  @XmlAttribute
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlAttribute
  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String subtype) {
    this.subtype = subtype;
  }
}
