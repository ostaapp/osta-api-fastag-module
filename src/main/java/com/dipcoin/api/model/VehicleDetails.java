package com.dipcoin.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class VehicleDetails {

  private List<Detail> Detail;
  
  //commenting bcoz according to new change req of vahan this will be received in detail list
  private String tagId;

  @XmlElement(name = "Detail")
  public List<Detail> getDetail() {
    return Detail;
  }

  public void setDetail(List<Detail> detail) {
    Detail = detail;
  }

	@XmlAttribute(name = "tagid")
	public String getTagId() {
		return tagId;
	}

	public void setTagId(String tagId) {
		this.tagId = tagId;
	}
  
  

}
