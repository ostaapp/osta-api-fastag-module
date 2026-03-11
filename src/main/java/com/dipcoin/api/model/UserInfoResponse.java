package com.dipcoin.api.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class UserInfoResponse extends APIResponse {

  private int userId;
  private String fname;
  private String lname;
  private String email;
  private String phonenum;
  private String phonetype;
  private String secretQue;
  private Integer tnc;
  private Integer status;
  private String clientTransactionId;
  private String role;
  private String registrationDate;
  private Integer expertise;
  private String agentId;
  private String phoneAlias;
  

  private MerchantInfoResponse merchantInfo;
  private BankInfoResponse bankInfo;
  private UserInfoResponse creatorInfo;
  
  private String cardId;
  private String tpinLength;
  private String note;
  private UserPreferencesResponse userPreferences;
  
  private List<APIResponse> apiResponse;
  private Pagination pagination;

  public Pagination getPagination() {
    return pagination;
  }

  public List<APIResponse> getApiResponse() {
    return apiResponse;
  }

  public void setApiResponse(List<APIResponse> apiResponse) {
    this.apiResponse = apiResponse;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  public String getFname() {
    return fname;
  }

  public void setFname(String fname) {
    this.fname = fname;
  }

  public String getLname() {
    return lname;
  }

  public void setLname(String lname) {
    this.lname = lname;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhonenum() {
    // @TODO Mask phone num appropriately
//    if (phonenum != null) {
//      return "xxxxxx" + phonenum.substring(phonenum.length() - 4);
//    }
    return phonenum;
  }

  public void setPhonenum(String phonenum) {
    this.phonenum = phonenum;
  }

  public String getPhonetype() {
    return phonetype;
  }

  public void setPhonetype(String phonetype) {
    this.phonetype = phonetype;
  }

  public String getSecretQue() {
    return secretQue;
  }

  public void setSecretQue(String secretQue) {
    this.secretQue = secretQue;
  }

  public final Integer getStatus() {
    return status;
  }

  public Integer getTnc() {
    return tnc;
  }

  public void setTnc(Integer tnc) {
    this.tnc = tnc;
  }

  public final void setStatus(Integer status) {
    this.status = status;
  }

  public final String getClientTransactionId() {
    return clientTransactionId;
  }

  public final void setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }

  public MerchantInfoResponse getMerchantInfo() {
    return merchantInfo;
  }

  public void setMerchantInfo(MerchantInfoResponse merchantInfo) {
    this.merchantInfo = merchantInfo;
  }

  public BankInfoResponse getBankInfo() {
    return bankInfo;
  }

  public void setBankInfo(BankInfoResponse bankInfo) {
    this.bankInfo = bankInfo;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(String registrationDate) {
    this.registrationDate = registrationDate;
  }

  public Integer getExpertise() {
    return expertise;
  }

  public void setExpertise(Integer expertise) {
    this.expertise = expertise;
  }

  public UserInfoResponse getCreatorInfo() {
    return creatorInfo;
  }

  public void setCreatorInfo(UserInfoResponse creatorInfo) {
    if (creatorInfo != null) {
      // @NOTE - set explicit fields to null to avoid loop
      creatorInfo.setCreatorInfo(null);
      creatorInfo.setBankInfo(null);
      creatorInfo.setMerchantInfo(null);
    }

    this.creatorInfo = creatorInfo;
  }

  public UserPreferencesResponse getUserPreferences() {
    return userPreferences;
  }

  public void setUserPreferences(UserPreferencesResponse userPreferences) {
    this.userPreferences = userPreferences;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getPhoneAlias() {
    return phoneAlias;
  }

  public void setPhoneAlias(String phoneAlias) {
    this.phoneAlias = phoneAlias;
  }

  public String getCardId() {
    return cardId;
  }

  public void setCardId(String cardId) {
    this.cardId = cardId;
  }

  public String getTpinLength() {
    return tpinLength;
  }

  public void setTpinLength(String tpinLength) {
    this.tpinLength = tpinLength;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
  
  public int getUserId() {
	return userId;
  }
  
  public void setUserId(int userId) {
	  this.userId = userId;
  }
}
