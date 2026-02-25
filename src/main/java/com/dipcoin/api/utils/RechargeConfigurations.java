package com.dipcoin.api.utils;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.dipcoin.api.utils.RechargeConstants.ChannelCode;
import com.dipcoin.api.utils.RechargeConstants.PaymentMode;
import com.fasterxml.jackson.annotation.JsonIgnore;

@PropertySource(
    value = {"classpath:dipcoin-recharge-services.properties",
        "classpath:dipcoin-recharge-services-${spring.profiles.active}.properties"},
    ignoreResourceNotFound = true)
@Configuration
@ComponentScan("com.dipcoin")
public class RechargeConfigurations {

  @JsonIgnore
  @Autowired
  private PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.MerchantCode}")
  private String merchantCode;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.StoreCode}")
  private String storeCode;
  
  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.UserName}")
  private String userName;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.UserPass}")
  private String userPass;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.AgentId}")
  private String agentId;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.OffUsPay}")
  private String offUsPay;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.PaymentMode}")
  private String paymentMode;
  
  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.RequestTimeOut}")
  private int requestTimeOut;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.IPAddress}")
  private String ipAddress;
  
  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.ChannelCode}")
  private String channelCode;

  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.ChannelName}'.split(',')}")
  private List<String> channelName;
  
  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.ChannelNameInt}'.split(',')}")
  private List<String> channelNameInt;
  
  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.ChannelNameBankBranch}'.split(',')}")
  private List<String> channelNameBankBranch;
  
  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.ChannelNameAgt}'.split(',')}")
  private List<String> channelNameAgt;

  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.ChannelValue}'.split(',')}")
  private List<String> channelValue;

  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.PaymentName}'.split(',')}")
  private List<String> paymentName;
  
  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.PaymentNameAccountTransfer}'.split(',')}")
  private List<String> paymentNameAccountTransfer;
  
  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.PaymentNameCash}'.split(',')}")
  private List<String> paymentNameCash;

  @Value("#{'${com.dipcoin.recharge.utils.RechargeConfigurations.PaymentValue}'.split(',')}")
  private List<String> paymentValue;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.Salt}")
  private String salt;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.GetValidationUrl}")
  private String getValidationUrl;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.GetServiceUrl}")
  private String getServiceUrl;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.GetStatusUrl}")
  private String getStatusUrl;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.GetBalanceUrl}")
  private String getBalanceUrl;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.EuronetHost}")
  private String euronetHost;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.BBPSHost}")
  private String bBPSHost;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.GetBBPSUrl}")
  private String bBPSUrl;
  
  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.RechargePlanHost}")
  private String RechargePlanHost;

  @Value("${com.dipcoin.recharge.utils.RechargeConfigurations.RechargePlanUrl}")
  private String RechargePlanUrl;

  public String getEuronetHost() {
    return euronetHost;
  }

  public String getbBPSHost() {
    return bBPSHost;
  }

  public String getbBPSUrl() {
    return bBPSUrl;
  }

  public String getMerchantCode() {
    return merchantCode;
  }

  public String getStoreCode() {
    return storeCode;
  }

  public String getUserName() {
    return userName;
  }

  public String getUserPass() {
    return userPass;
  }

  public String getAgentId() {
    return agentId;
  }

  public String getOffUsPay() {
    return offUsPay;
  }

  public void setOffUsPay(String offUsPay) {
    this.offUsPay = offUsPay;
  }

  public String getPaymentMode() {
    return paymentMode;
  }

  public int getRequestTimeOut() {
    return requestTimeOut;
  }

  public String getIPAdress() {
    return ipAddress;
  }

  public String getChannelCode() {
    return channelCode;
  }

  public List<String> getChannelName() {
    return channelName;
  }

  public List<String> getChannelValue() {
    return channelValue;
  }

  public List<String> getPaymentName() {
    return paymentName;
  }

  public List<String> getPaymentValue() {
    return paymentValue;
  }

  public String getSalt() {
    return salt;
  }

  public String getGetValidationUrl() {
    return getValidationUrl;
  }

  public String getGetServiceUrl() {
    return getServiceUrl;
  }

  public String getGetStatusUrl() {
    return getStatusUrl;
  }

  public String getGetBalanceUrl() {
    return getBalanceUrl;
  }

  public String getRechargePlanHost() {
    return RechargePlanHost;
  }

  public String getRechargePlanUrl() {
    return RechargePlanUrl;
  }

  public List<String> getChannelNameInt() {
    return channelNameInt;
  }

  public List<String> getChannelNameBankBranch() {
    return channelNameBankBranch;
  }

  public List<String> getPaymentNameAccountTransfer() {
    return paymentNameAccountTransfer;
  }
  
  public List<String> getPaymentNameCash() {
    return paymentNameCash;
  }
  
  public List<String> getChannelNameAgt() {
    return channelNameAgt;
  }
  
  public  List<String> channelNames(ChannelCode channelCode) {

    switch (channelCode) {
      case INTERNET:
        return getChannelNameInt();

      case INTERNET_BANKING:
        return getChannelNameInt();

      case MOBILE:
        return getChannelName();

      case MOBILE_BANKING:
        return getChannelName();

      case BANK_BRANCH:
        return getChannelNameBankBranch();
        
      case AGENT:
        return getChannelNameAgt();
      
      default:
        return null;
    }
  }
  
  public   List<String> paymentModeName(PaymentMode paymentMode) {

    switch (paymentMode) {
      case CASH:
        return getPaymentNameCash();

      case WALLET:
        return getPaymentName();

      case ACCOUNT_TRANSFER:
        return getPaymentNameAccountTransfer();
      
      default:
        return null;
    }
  }
}
