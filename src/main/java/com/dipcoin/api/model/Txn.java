package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class Txn {

	private String id;

	private String note;

	private String refId;

	private String refUrl;

	private String ts;

	private String type;

	private String orgTxnId;

	private RiskScores riscScores;

	private Vehicle vehicle;

	private Resp resp;

	private TagList tagList;

	private Tag tag;

	private Error error;

	@XmlElement(name = "Error")
	public Error getError() {
		return error;
	}

	public void setError(Error error) {
		this.error = error;
	}

	private TollExceptionList tollExceptionList;

	@XmlAttribute(name = "id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlAttribute(name = "note")
	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	@XmlAttribute(name = "refId")
	public String getRefId() {
		return refId;
	}

	public void setRefId(String refId) {
		this.refId = refId;
	}

	@XmlAttribute(name = "refUrl")
	public String getRefUrl() {
		return refUrl;
	}

	public void setRefUrl(String refUrl) {
		this.refUrl = refUrl;
	}

	@XmlAttribute(name = "ts")
	public String getTs() {
		return ts;
	}

	public void setTs(String ts) {
		this.ts = ts;
	}

	@XmlAttribute(name = "type")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@XmlAttribute(name = "orgTxnId")
	public String getOrgTxnId() {
		return orgTxnId;
	}

	public void setOrgTxnId(String orgTxnId) {
		this.orgTxnId = orgTxnId;
	}

	@XmlElement(name = "RiskScores")
	public RiskScores getRiscScores() {
		return riscScores;
	}

	public void setRiscScores(RiskScores riscScores) {
		this.riscScores = riscScores;

	}

	@XmlElement(name = "Vehicle")
	public Vehicle getVehicle() {
		return vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	@XmlElement(name = "Resp")
	public Resp getResp() {
		return resp;
	}

	public void setResp(Resp resp) {
		this.resp = resp;
	}

	@XmlElement(name = "TagList")
	public TagList getTagList() {
		return tagList;
	}

	public void setTagList(TagList tagList) {
		this.tagList = tagList;
	}

	@XmlElement(name = "ExceptionList")
	public TollExceptionList getExceptionList() {
		return tollExceptionList;
	}

	public void setExceptionList(TollExceptionList tollExceptionList) {
		this.tollExceptionList = tollExceptionList;
	}

	@XmlElement(name = "Tag")
	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

}
