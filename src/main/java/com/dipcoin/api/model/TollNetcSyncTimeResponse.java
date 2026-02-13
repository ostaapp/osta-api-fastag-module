package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ReqSyncTime", namespace = "http://npci.org/etc/schema/")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TollNetcSyncTimeResponse extends APIResponse {

	private Head head;

	@XmlElement(name = "Head")
	public Head getHead() {
		return head;
	}

	public void setHead(Head head) {
		this.head = head;
	}

	private Resp resp;

	@XmlElement(name = "Resp")
	public Resp getResp() {
		return resp;
	}

	public void setResp(Resp resp) {
		this.resp = resp;
	}

}
