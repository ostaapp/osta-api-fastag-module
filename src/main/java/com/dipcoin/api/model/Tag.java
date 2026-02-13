package com.dipcoin.api.model;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Tag {

  private String op;

  private String tagId;

  private String seqNum;

  private String type;

  private List<Detail> detail;

  private String result;

  private String errCode;

  private String excCode;

  private String updateTime;
  

//force close notification
  private String impactedTagId;
  private String issuerIIN;
  private String newTagId;
  private String regNo;
  private String activityTs;
//*****************************//

  public String getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(String updateTime) {
    this.updateTime = updateTime;
  }

  @XmlElement(name = "Detail")
  public List<Detail> getDetail() {
    return detail;
  }

  public void setDetail(List<Detail> detail) {
    this.detail = detail;
  }

  @XmlAttribute(name = "op")
  public String getOp() {
    return op;
  }

  public void setOp(String op) {
    this.op = op;
  }

  @XmlAttribute(name = "tagId")
  public String getTagId() {
    return tagId;
  }

  public void setTagId(String tagId) {
    this.tagId = tagId;
  }

  @XmlAttribute(name = "seqNum")
  public String getSeqNum() {
    return seqNum;
  }

  public void setSeqNum(String seqNum) {
    this.seqNum = seqNum;
  }

  @XmlAttribute(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @XmlAttribute
  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @XmlAttribute
  public String getErrCode() {
    return errCode;
  }

  public void setErrCode(String errCode) {
    this.errCode = errCode;
  }

  @XmlAttribute
  public String getExcCode() {
    return excCode;
  }

  public void setExcCode(String excCode) {
    this.excCode = excCode;
  }
  
  @XmlAttribute(name = "impactedTagId")
	public String getImpactedTagId() {
		return impactedTagId;
	}

	public void setImpactedTagId(String impactedTagId) {
		this.impactedTagId = impactedTagId;
	}

	@XmlAttribute(name = "issuerIIN")
	public String getIssuerIIN() {
		return issuerIIN;
	}

	public void setIssuerIIN(String issuerIIN) {
		this.issuerIIN = issuerIIN;
	}
	
	@XmlAttribute(name = "newTagId")
	public String getNewTagId() {
		return newTagId;
	}

	public void setNewTagId(String newTagId) {
		this.newTagId = newTagId;
	}

	@XmlAttribute(name = "regNo")
	public String getRegNo() {
		return regNo;
	}

	public void setRegNo(String regNo) {
		this.regNo = regNo;
	}
	@XmlAttribute(name = "activityTs")
	public String getActivityTs() {
		return activityTs;
	}

	public void setActivityTs(String activityTs) {
		this.activityTs = activityTs;
	}



}
