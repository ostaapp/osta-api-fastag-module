package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "etc:ReqSyncTime")
public class TollNetcSyncTimeRequest {

	private Head head;

	@XmlAttribute(name = "xmlns:etc")
	private String etc = "http://npci.org/etc/schema/";

	@XmlElement(name = "Head")
	public Head getHead() {
		return head;
	}

	public void setHead(Head head) {
		this.head = head;
	}

}
