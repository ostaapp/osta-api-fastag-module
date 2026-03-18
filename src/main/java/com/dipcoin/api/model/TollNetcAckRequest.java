package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "etc:Ack")
public class TollNetcAckRequest {

	@XmlAttribute(name = "xmlns:etc")
	private String etc = "http://npci.org/etc/schema/";
	
	private String api;
	private String reqMsgId;
	private String err;
	private String ts;
	
	@XmlAttribute(name = "api")
	public String getApi() {
		return api;
	}
	public void setApi(String api) {
		this.api = api;
	}
	
	@XmlAttribute(name = "reqMsgId")
	public String getReqMsgId() {
		return reqMsgId;
	}
	public void setReqMsgId(String reqMsgId) {
		this.reqMsgId = reqMsgId;
	}
	
	@XmlAttribute(name = "err")
	public String getErr() {
		return err;
	}
	public void setErr(String err) {
		this.err = err;
	}
	
	@XmlAttribute(name = "ts")
	public String getTs() {
		return ts;
	}
	public void setTs(String ts) {
		this.ts = ts;
	}
	
}

