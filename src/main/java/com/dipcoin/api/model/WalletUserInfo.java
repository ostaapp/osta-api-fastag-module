package com.dipcoin.api.model;

import java.security.SecureRandom;
import com.dipcoin.api.model.APIResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
public class WalletUserInfo extends APIResponse {

  private final static SecureRandom randomGenerator = new SecureRandom();

  private String fname;
  private String lname;
  private String email;
  private String phonenum;
  private String role;
  private String walletId;
  private Integer cardId;
  
  public Integer getCardId() {
	return cardId;
  }

  public void setCardId(Integer cardId) {
	this.cardId = cardId;
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
    if (phonenum != null) {
      return "xxxxxx" + phonenum.substring(phonenum.length() - 4);
    }
    return null;
  }

  public void setPhonenum(String phonenum) {
    this.phonenum = phonenum;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getWalletId() {
    return walletId;
  }

  public void setWalletId(String walletId) {
    this.walletId = walletId;
  }

}
