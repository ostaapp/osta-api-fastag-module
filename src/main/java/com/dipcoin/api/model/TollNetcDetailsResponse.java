package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.dipcoin.api.filter.HttpServletContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "RespDetails", namespace = "http://npci.org/etc/schema/")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class TollNetcDetailsResponse extends APIRequest {

	@Override
	public boolean validate(HttpServletContext httpServletContext) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void decrypt(HttpServletContext httpServletContext) {
		// TODO Auto-generated method stub

	}

	private Head head;

	private Txn txn;

	@XmlElement(name = "Head")
	public Head getHead() {
		return head;
	}

	public void setHead(Head head) {
		this.head = head;
	}

	@XmlElement(name = "Txn")
	public Txn getTxn() {
		return txn;
	}

	public void setTxn(Txn txn) {
		this.txn = txn;
	}

}
