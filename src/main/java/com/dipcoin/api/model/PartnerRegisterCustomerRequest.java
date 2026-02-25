package com.dipcoin.api.model;


import com.dipcoin.api.filter.HttpServletContext;
//import com.dipcoin.partner.paymentGateway.model.Payer.Phone;
//import com.dipcoin.partner.paymentGateway.model.Payer.Phone.PhoneNumber;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class PartnerRegisterCustomerRequest extends APIRequest  {
      private  UserProfile userProfile;
	  @ApiModelProperty(required = true)
	  private  AccountDetails accountDetails;
	  @ApiModelProperty(required = true)
	  private String isExistingUser;
	  @ApiModelProperty(required = true)
	  private String bundleId;
	  @ApiModelProperty(required = true)
      private  UserProfile updatedDetails;
	  @ApiModelProperty(required = false)
	  private String redirectURI;
	  @ApiModelProperty(required = false)
	  private String encryptedPayload;
	  private Integer status;
	  private Boolean isOstaPin;
	  private int featureToOpen;
	  private BankInfoResponse bankInfo;
	  private String source;
	  
	  private String role;
	  private String deviceId;
	  private String userChannel;
	  private String tnc;
	  private String requestType;
	  
	  //private String uniqueId;
	  
	  //private String partnerTransactionRefId;
	  
	  //private String requestType;
	  
	  @JsonInclude(Include.NON_NULL)
	  @NoArgsConstructor
	  @Accessors(chain = true)
	  @Setter
	  @Getter
	  public static class AccountDetails {

	      @ApiModelProperty(required = true)
	      private String accountId; 
	      @ApiModelProperty(required = true)
	      private String bankReferenceId;
	      @ApiModelProperty(required = false)
          private String cif;
	    
	    
	  }
	  

	  
	  @JsonInclude(Include.NON_NULL)
      @NoArgsConstructor
      @Accessors(chain = true)
      @Setter
      @Getter
      public static class UserProfile {
          private String lastName;
          @ApiModelProperty(required = true)
          private String firstName;  
          @ApiModelProperty(required = true)
          private String mobile;
          @ApiModelProperty(required = true)
          private String email;
          @ApiModelProperty(required = true)
          private String tnc;
          @ApiModelProperty(required = false)
          private String tPINFlag;
          @ApiModelProperty(required = false)
          private String tPINLength;
          @ApiModelProperty(required = false)
          private String isTpinEnabled;
          @ApiModelProperty(required = false)
          private String tPIN;
          @ApiModelProperty(required = false)
          private int isSsoEnabled;
          @ApiModelProperty(required = false)
          private String redirectTo;
          
                   
          //lombok not creating getter setter for 
          //tpin and tpinLenghth as the work is not camelcase
          public String gettPINFlag() {
            return tPINFlag;
          }
          public void settPINFlag(String tPINFlag) {
            this.tPINFlag = tPINFlag;
          }
          public String gettPINLength() {
            return tPINLength;
          }
          public void settPINLength(String tPINLength) {
            this.tPINLength = tPINLength;
          }
          public String gettPIN() {
            return tPIN;
          }

          public void settPIN(String tPIN) {
            this.tPIN = tPIN;
          }
                
      }
	@Override
	public boolean validate(HttpServletContext httpServletContext) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	protected void decrypt(HttpServletContext httpServletContext) {
		// TODO Auto-generated method stub
		
	}

}
