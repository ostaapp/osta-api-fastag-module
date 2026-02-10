package com.dipcoin.api.model;

import java.util.List;
import com.dipcoin.db.services.model.TollTag;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TollRegistrationResponse extends APIResponse {

	private Integer id;
	private Integer userId;
	private String firstName;
	private String lastName;
	private String gender;
	private String dob;
	private String mobileNo;
	private String emailId;
	private String address;
	private String city;
	private String state;
	private Integer pincode;
	private String idProofType;
	private String idProofNo;
	private String idProofPath;
	private String clientTransactionId;
	private String iPAddress;
	private List<TollTagResponse> vehicleList;
	private String createdDateTime;
	private String status;
	private String bankName;
	private String createdby;
	private Integer cardId;
	private String gstAmount;
	private String walletBankReferenceId;
	private int isEverLoggedIn;

	// Setter and Getter

	public int getIsEverLoggedIn() {
		return isEverLoggedIn;
	}

	public TollRegistrationResponse setIsEverLoggedIn(int isEverLoggedIn) {
		this.isEverLoggedIn = isEverLoggedIn;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public TollRegistrationResponse setStatus(String status) {
		this.status = status;
		return this;
	}

	public Integer getId() {
		return id;
	}

	public List<TollTagResponse> getVehicleList() {
		return vehicleList;
	}

	public void setVehicleList(List<TollTagResponse> vehicleList) {
		this.vehicleList = vehicleList;
	}

	public TollRegistrationResponse setId(Integer id) {
		this.id = id;
		return this;
	}

	public Integer getUserId() {
		return userId;
	}

	public TollRegistrationResponse setUserId(Integer userId) {
		this.userId = userId;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public TollRegistrationResponse setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public TollRegistrationResponse setLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getGender() {
		return gender;
	}

	public TollRegistrationResponse setGender(String gender) {
		this.gender = gender;
		return this;
	}

	public String getDob() {
		return dob;
	}

	public TollRegistrationResponse setDob(String dob) {
		this.dob = dob;
		return this;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public TollRegistrationResponse setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
		return this;
	}

	public String getEmailId() {
		return emailId;
	}

	public TollRegistrationResponse setEmailId(String emailId) {
		this.emailId = emailId;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public TollRegistrationResponse setAddress(String address) {
		this.address = address;
		return this;
	}

	public String getCity() {
		return city;
	}

	public TollRegistrationResponse setCity(String city) {
		this.city = city;
		return this;
	}

	public String getState() {
		return state;
	}

	public TollRegistrationResponse setState(String state) {
		this.state = state;
		return this;
	}

	public Integer getPincode() {
		return pincode;
	}

	public TollRegistrationResponse setPincode(Integer pincode) {
		this.pincode = pincode;
		return this;
	}

	public String getIdProofType() {
		return idProofType;
	}

	public TollRegistrationResponse setIdProofType(String idProofType) {
		this.idProofType = idProofType;
		return this;
	}

	public String getIdProofNo() {
		return idProofNo;
	}

	public TollRegistrationResponse setIdProofNo(String idProofNo) {
		this.idProofNo = idProofNo;
		return this;
	}

	public String getIdProofPath() {
		return idProofPath;
	}

	public TollRegistrationResponse setIdProofPath(String idProofPath) {
		this.idProofPath = idProofPath;
		return this;
	}

	public String getClientTransactionId() {
		return clientTransactionId;
	}

	public TollRegistrationResponse setClientTransactionId(String clientTransactionId) {
		this.clientTransactionId = clientTransactionId;
		return this;
	}

	public String getiPAddress() {
		return iPAddress;
	}

	public TollRegistrationResponse setiPAddress(String iPAddress) {
		this.iPAddress = iPAddress;
		return this;
	}

	public String getCreatedDateTime() {
		return createdDateTime;
	}

	public TollRegistrationResponse setCreatedDateTime(String createdDateTime) {
		this.createdDateTime = createdDateTime;
		return this;
	}

	public String getCreatedby() {
		return createdby;
	}

	public TollRegistrationResponse setCreatedby(String createdby) {
		this.createdby = createdby;
		return this;
	}

	public Integer getCardId() {
		return cardId;
	}

	public TollRegistrationResponse setCardId(Integer cardId) {
		this.cardId = cardId;
		return this;
	}

	public String getWalletBankReferenceId() {
		return walletBankReferenceId;
	}

	public TollRegistrationResponse setWalletBankReferenceId(String walletBankReferenceId) {
		this.walletBankReferenceId = walletBankReferenceId;
		return this;
	}

}
