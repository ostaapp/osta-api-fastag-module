package com.dipcoin.api.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Range;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;


@Configuration
@Setter
@Getter
public class ApplicationProperties {

  private static final Logger LOG = LogManager.getLogger(ApplicationProperties.class);

  @JsonIgnore
  @Autowired
  private PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer;

  @Value("${com.dipcoin.api.config.ApplicationProperties.app.version}")
  private String applicationVersion;

  @Value("${com.dipcoin.api.config.ApplicationProperties.app.name}")
  private String applicationName;

  @Value("#{'${com.dipcoin.api.config.ApplicationProperties.blacklistedApis}'.split(',')}")
  private List<String> blacklistedApis;
  private Set<String> blacklistedApisAsSet = new HashSet<>();

  @Value("#{'${com.dipcoin.api.config.ApplicationProperties.blacklistedHours}'.split(',')}")
  private List<String> blacklistedHours;
  private List<Range<Integer>> blacklistedHoursAsRange;

  @Value("${com.dipcoin.api.config.ApplicationProperties.uiServingHostPort}")
  private String uiServingHostPort;

  @Value("${com.dipcoin.api.config.ApplicationProperties.uploadFileToS3}")
  private boolean uploadFileToS3;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.enableSMS}")
  private boolean enableSMS;

  @Value("${com.dipcoin.api.config.ApplicationProperties.logoImgPath}")
  private String logoImgPath;

  @Value("${com.dipcoin.api.config.ApplicationProperties.mailImgPath}")
  private String mailImgPath;

  @Value("${com.dipcoin.api.config.ApplicationProperties.senderEmail}")
  private String senderEmail;

  @Value("${com.dipcoin.api.config.ApplicationProperties.contactEmail}")
  private String contactEmail;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.webUrl}")
  private String webUrl;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.appUrl}")
  private String appUrl;

  @Value("${com.dipcoin.api.config.ApplicationProperties.oauth2EncryptionKey}")
  private String oauth2EncryptionKey;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.ostaAddress}")
  private String ostaAddress;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.footerMessage}")
  private String footerMessage;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.fraudMgmt}")
  private boolean fraudMgmt;

  public String getFooterMessage() {
    return footerMessage;
  }

  public String getOstaAddress() {
    return ostaAddress;
  }

  @Value("${server.servlet.context-path}")
  private String baseApiPath;

  @Value("${com.dipcoin.api.config.ApplicationProperties.ostaRedirectUrl}")
  private String ostaRedirectUrl;

  @Value("${com.dipcoin.api.config.ApplicationProperties.deleteWallet}")
  private Boolean deleteWallet;

  @Value("${com.dipcoin.api.config.ApplicationProperties.offlineApi.job}")
  private String offlineApi;  
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.offlineApi.uploadFile}")
  private String offlineApiUploadFile;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.verifySubuserUrl}")
  private String verifySubuserUrl;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.adminVerifySubuserUrl}")
  private String adminVerifySubuserUrl;
 
  @Value("${com.dipcoin.api.config.ApplicationProperties.sdkRedirectUrl}")
  private String sdkRedirectUrl;
  
  @Value("#{'${com.dipcoin.api.config.ApplicationProperties.blacklistedApisBank}'.split(',')}")
  private List<String> blacklistedApisBank;
  private Set<String> blacklistedApisBankAsSet = new HashSet<>();

  @Value("#{'${com.dipcoin.api.config.ApplicationProperties.blacklistedHoursBank}'.split(',')}")
  private List<String> blacklistedHoursBank;
  private List<Range<Integer>> blacklistedHoursBankAsRange;

  @Value("${com.dipcoin.api.config.ApplicationProperties.androidAppLink}")
  private String androidAppLink;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.iosAppLink}")
  private String iosAppLink;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.awsSMSClient}")
  private Boolean awsSMSClient;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.runAudit}")
  private Boolean runAudit;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.fastagReconAccounting}")
  private String reconAccounting;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.generatedipcoin.numberOfRetry}")
  private int generateDipcoinRetry;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.webUrl.otp}")
  private String webOtpUrl;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.localPath}")
  private String localPath;
  
  @Value("${com.dipcoin.api.config.ApplicationProperties.sendBulkTollTagDeletionStatusSheetMailTo}")
  private String sendBulkTollTagDeletionStatusSheetMailTo;
  
  public String getAndroidAppLink() {
    return androidAppLink;
  }

  public String getIosAppLink() {
    return iosAppLink;
  }

  
  
  public String getSdkRedirectUrl() {
    return sdkRedirectUrl;
  }

  public void setSdkRedirectUrl(String sdkRedirectUrl) {
    this.sdkRedirectUrl = sdkRedirectUrl;
  }
  
  public String getVerifySubuserUrl() {
    return verifySubuserUrl;
  }

  public String getAdminVerifySubuserUrl() {
    return adminVerifySubuserUrl;
  }

  public String getOstaRedirectUrl() {
    return ostaRedirectUrl;
  }

  public String getApplicationVersion() {
    return applicationVersion;
  }

  public String getApplicationName() {
    return applicationName;
  }

  public Set<String> getBlacklistedApis() {
    return blacklistedApisAsSet;
  }

  public List<Range<Integer>> getBlacklistedHours() {
    return blacklistedHoursAsRange;
  }

  public String getUiServingHostPort() {
    return uiServingHostPort;
  }

  public boolean uploadFileToS3() {
    return uploadFileToS3;
  }
  
  public boolean enableSMS() {
    return enableSMS;
  }

  public String getLogoImgPath() {
    return uiServingHostPort.concat(logoImgPath);
  }

  public String getmailImgPath() {
    return uiServingHostPort.concat(mailImgPath);
  }

  public String getSenderEmail() {
    return senderEmail;
  }

  public String getContactEmail() {
    return contactEmail;
  }
  
  public String getWebUrl() {
	    return webUrl;
	  }

  public String getAppUrl() {
	    return appUrl;
	  }
  
  public String getOauth2EncryptionKey() {
    return oauth2EncryptionKey;
  }

  public String getBaseApiPath() {
    return baseApiPath;
  }

  public Boolean getDeleteWallet() {
    return deleteWallet;
  }

  public String getOfflineApi() {
    return offlineApi;
  }
  
  public Set<String> getBlacklistedApisBank() {
    return blacklistedApisBankAsSet;
  }

  public List<Range<Integer>> getBlacklistedHoursBank() {
    return blacklistedHoursBankAsRange;
  }

  
  /*
   * 
   */
  @PostConstruct
  public void construct() throws Exception {
    LOG.info("Constructing " + this.getClass().getSimpleName() + " ...");

    blacklistedApisAsSet.addAll(blacklistedApis);

    blacklistedHoursAsRange = new ArrayList<>();
    for (String blacklistedHour : blacklistedHours) {
      String[] split = blacklistedHour.split(":");
      blacklistedHoursAsRange
          .add(Range.closed(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
    }
    
    blacklistedApisBankAsSet.addAll(blacklistedApisBank);

    blacklistedHoursBankAsRange = new ArrayList<>();
    for (String blacklistedHour : blacklistedHoursBank) {
      String[] split = blacklistedHour.split(":");
      blacklistedHoursBankAsRange
          .add(Range.closed(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
    }
  }

  @PreDestroy
  public void cleanUp() throws Exception {
    LOG.info("Cleaning up " + this.getClass().getSimpleName() + " ...");
  }


}