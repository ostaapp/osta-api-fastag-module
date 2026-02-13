package com.dipcoin.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Vehicle {

  private List<VehicleDetails> vehicleDetails;

  private String tagId;

  private String TID;

  private String avc;

  private String wim;

  private String vehicleRegNo;

  private String errCode;
  
  private String engineNo;
  
  private String vinNumber;
  
  private String vin;
  
  private String vrn;
  
  private String last5digitofengineno;

  @XmlAttribute(name = "tagId")
  public String getTagId() {
    return tagId;
  }

  public void setTagId(String tagId) {
    this.tagId = tagId;
  }

  @XmlAttribute(name = "TID")
  public String getTID() {
    return this.TID;
  }

  public void setTID(String tID) {
    this.TID = tID;
  }

  @XmlAttribute(name = "avc")
  public String getAvc() {
    return avc;
  }

  public void setAvc(String avc) {
    this.avc = avc;
  }

  @XmlAttribute
  public String getWim() {
    return wim;
  }

  public void setWim(String wim) {
    this.wim = wim;
  }

  @XmlElement(name = "VehicleDetails")
  public List<VehicleDetails> getVehicleDetails() {
    return vehicleDetails;
  }

  public void setVehicleDetails(List<VehicleDetails> vehicleDetails) {
    this.vehicleDetails = vehicleDetails;
  }

  @XmlAttribute(name = "vehicleRegNo")
  public String getVehicleRegNo() {
    return vehicleRegNo;
  }

  public void setVehicleRegNo(String vehicleRegNo) {
    this.vehicleRegNo = vehicleRegNo;
  }

  @XmlAttribute
  public String getErrCode() {
    return errCode;
  }

  public void setErrCode(String errCode) {
    this.errCode = errCode;
  }

	@XmlAttribute(name = "ENGINENO")
	public String getEngineNo() {
		return engineNo;
	}

	public void setEngineNo(String engineNo) {
		this.engineNo = engineNo;
	}

	@XmlAttribute(name = "vin")
	public String getVinNumber() {
		return vinNumber;
	}

	public void setVinNumber(String vinNumber) {
		this.vinNumber = vinNumber;
	} 

	@XmlAttribute(name = "VIN")
	public String getVin() {
		return vin;
	}

	public void setVin(String vin) {
		this.vin = vin;
	}

	@XmlAttribute(name = "vrn")
	public String getVrn() {
		return vrn;
	}

	public void setVrn(String vrn) {
		this.vrn = vrn;
	}

	@XmlAttribute(name = "last5digitofengineno")
	public String getLast5digitofengineno() {
		return last5digitofengineno;
	}

	public void setLast5digitofengineno(String last5digitofengineno) {
		this.last5digitofengineno = last5digitofengineno;
	}
	
	

}