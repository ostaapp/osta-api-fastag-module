package com.dipcoin.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import com.dipcoin.partner.toll.model.Head;
//import com.dipcoin.partner.toll.model.Merchant;
//import com.dipcoin.partner.toll.model.Meta;
//import com.dipcoin.partner.toll.model.Payee;
//import com.dipcoin.partner.toll.model.Payer;
//import com.dipcoin.partner.toll.model.Txn;
//import com.dipcoin.partner.toll.model.Vehicle;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;

@XmlRootElement(name = "NETCAdvice", namespace = "http://npci.org/etc/schema/")
@XmlAccessorType(XmlAccessType.PROPERTY)
@JsonInclude(Include.NON_NULL)
public class VahanInfoRequest extends APIResponse{

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


	
	/*
	 * private static final Logger LOG =
	 * LogManager.getLogger(VahanInfoResponse.class);
	 * 
	 * private String tagId; private String regNumber; private String vin; private
	 * String engineNo; private String gvw; private String nationalPermit; private
	 * String nationalPermitStartDate; private String nationalPermitEndDate; private
	 * String stageCarriagePermit; private String stageCarriagePermitStartDate;
	 * private String stageCarriagePermitEndDate; private String
	 * contractCarriagePermit; private String contractCarriagePermitStartDate;
	 * private String contractCarriagePermitEndDate; private String
	 * privateServiceVehiclePermit; private String
	 * privateServiceVehiclePermitStartDate; private String
	 * privateServiceVehiclePermitEndDate; private String allIndiaTouristPermit;
	 * private String allIndiaTouristPermitStartDate; private String
	 * allIndiaTouristPermitEndDate; private String goodsPermit; private String
	 * goodsPermitStartDate; private String goodsPermitEndDate; private String
	 * temporaryPermit; private String temporaryPermitStartDate; private String
	 * temporaryPermitEndDate; private String specialPermit; private String
	 * specialPermitStartDate; private String specialPermitEndDate; private String
	 * errorCode; public String getTagId() { return tagId; } public void
	 * setTagId(String tagId) { this.tagId = tagId; } public String getRegNumber() {
	 * return regNumber; } public void setRegNumber(String regNumber) {
	 * this.regNumber = regNumber; } public String getVin() { return vin; } public
	 * void setVin(String vin) { this.vin = vin; } public String getEngineNo() {
	 * return engineNo; } public void setEngineNo(String engineNo) { this.engineNo =
	 * engineNo; } public String getGvw() { return gvw; } public void setGvw(String
	 * gvw) { this.gvw = gvw; } public String getNationalPermit() { return
	 * nationalPermit; } public void setNationalPermit(String nationalPermit) {
	 * this.nationalPermit = nationalPermit; } public String
	 * getNationalPermitStartDate() { return nationalPermitStartDate; } public void
	 * setNationalPermitStartDate(String nationalPermitStartDate) {
	 * this.nationalPermitStartDate = nationalPermitStartDate; } public String
	 * getNationalPermitEndDate() { return nationalPermitEndDate; } public void
	 * setNationalPermitEndDate(String nationalPermitEndDate) {
	 * this.nationalPermitEndDate = nationalPermitEndDate; } public String
	 * getStageCarriagePermit() { return stageCarriagePermit; } public void
	 * setStageCarriagePermit(String stageCarriagePermit) { this.stageCarriagePermit
	 * = stageCarriagePermit; } public String getStageCarriagePermitStartDate() {
	 * return stageCarriagePermitStartDate; } public void
	 * setStageCarriagePermitStartDate(String stageCarriagePermitStartDate) {
	 * this.stageCarriagePermitStartDate = stageCarriagePermitStartDate; } public
	 * String getStageCarriagePermitEndDate() { return stageCarriagePermitEndDate; }
	 * public void setStageCarriagePermitEndDate(String stageCarriagePermitEndDate)
	 * { this.stageCarriagePermitEndDate = stageCarriagePermitEndDate; } public
	 * String getContractCarriagePermit() { return contractCarriagePermit; } public
	 * void setContractCarriagePermit(String contractCarriagePermit) {
	 * this.contractCarriagePermit = contractCarriagePermit; } public String
	 * getContractCarriagePermitStartDate() { return
	 * contractCarriagePermitStartDate; } public void
	 * setContractCarriagePermitStartDate(String contractCarriagePermitStartDate) {
	 * this.contractCarriagePermitStartDate = contractCarriagePermitStartDate; }
	 * public String getContractCarriagePermitEndDate() { return
	 * contractCarriagePermitEndDate; } public void
	 * setContractCarriagePermitEndDate(String contractCarriagePermitEndDate) {
	 * this.contractCarriagePermitEndDate = contractCarriagePermitEndDate; } public
	 * String getPrivateServiceVehiclePermit() { return privateServiceVehiclePermit;
	 * } public void setPrivateServiceVehiclePermit(String
	 * privateServiceVehiclePermit) { this.privateServiceVehiclePermit =
	 * privateServiceVehiclePermit; } public String
	 * getPrivateServiceVehiclePermitStartDate() { return
	 * privateServiceVehiclePermitStartDate; } public void
	 * setPrivateServiceVehiclePermitStartDate(String
	 * privateServiceVehiclePermitStartDate) {
	 * this.privateServiceVehiclePermitStartDate =
	 * privateServiceVehiclePermitStartDate; } public String
	 * getPrivateServiceVehiclePermitEndDate() { return
	 * privateServiceVehiclePermitEndDate; } public void
	 * setPrivateServiceVehiclePermitEndDate(String
	 * privateServiceVehiclePermitEndDate) { this.privateServiceVehiclePermitEndDate
	 * = privateServiceVehiclePermitEndDate; } public String
	 * getAllIndiaTouristPermit() { return allIndiaTouristPermit; } public void
	 * setAllIndiaTouristPermit(String allIndiaTouristPermit) {
	 * this.allIndiaTouristPermit = allIndiaTouristPermit; } public String
	 * getAllIndiaTouristPermitStartDate() { return allIndiaTouristPermitStartDate;
	 * } public void setAllIndiaTouristPermitStartDate(String
	 * allIndiaTouristPermitStartDate) { this.allIndiaTouristPermitStartDate =
	 * allIndiaTouristPermitStartDate; } public String
	 * getAllIndiaTouristPermitEndDate() { return allIndiaTouristPermitEndDate; }
	 * public void setAllIndiaTouristPermitEndDate(String
	 * allIndiaTouristPermitEndDate) { this.allIndiaTouristPermitEndDate =
	 * allIndiaTouristPermitEndDate; } public String getGoodsPermit() { return
	 * goodsPermit; } public void setGoodsPermit(String goodsPermit) {
	 * this.goodsPermit = goodsPermit; } public String getGoodsPermitStartDate() {
	 * return goodsPermitStartDate; } public void setGoodsPermitStartDate(String
	 * goodsPermitStartDate) { this.goodsPermitStartDate = goodsPermitStartDate; }
	 * public String getGoodsPermitEndDate() { return goodsPermitEndDate; } public
	 * void setGoodsPermitEndDate(String goodsPermitEndDate) {
	 * this.goodsPermitEndDate = goodsPermitEndDate; } public String
	 * getTemporaryPermit() { return temporaryPermit; } public void
	 * setTemporaryPermit(String temporaryPermit) { this.temporaryPermit =
	 * temporaryPermit; } public String getTemporaryPermitStartDate() { return
	 * temporaryPermitStartDate; } public void setTemporaryPermitStartDate(String
	 * temporaryPermitStartDate) { this.temporaryPermitStartDate =
	 * temporaryPermitStartDate; } public String getTemporaryPermitEndDate() {
	 * return temporaryPermitEndDate; } public void setTemporaryPermitEndDate(String
	 * temporaryPermitEndDate) { this.temporaryPermitEndDate =
	 * temporaryPermitEndDate; } public String getSpecialPermit() { return
	 * specialPermit; } public void setSpecialPermit(String specialPermit) {
	 * this.specialPermit = specialPermit; } public String
	 * getSpecialPermitStartDate() { return specialPermitStartDate; } public void
	 * setSpecialPermitStartDate(String specialPermitStartDate) {
	 * this.specialPermitStartDate = specialPermitStartDate; } public String
	 * getSpecialPermitEndDate() { return specialPermitEndDate; } public void
	 * setSpecialPermitEndDate(String specialPermitEndDate) {
	 * this.specialPermitEndDate = specialPermitEndDate; } public String
	 * getErrorCode() { return errorCode; } public void setErrorCode(String
	 * errorCode) { this.errorCode = errorCode; } public static Logger getLog() {
	 * return LOG; }
	 */
  
  
}
 
