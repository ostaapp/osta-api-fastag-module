package com.dipcoin.api.commons;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.EmailClient;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.core.CryptoUtil.AlgoScheme;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.UserStatus;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.google.common.collect.ImmutableMap;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Component("tollEmailUtils")
public class TollEmailUtils {

  private static final Logger LOG = LogManager.getLogger(TollEmailUtils.class);

  private static final String USER_NAME = "userName";
  private static final String BODY_TEMPLATE = "bodyTemplate";
  private static final String MASKED_DIPCOIN = "maskedDipcoin";
  private static final String MASKED_PHONE = "maskedPhone";
  private static final String MASKED_LOGIN = "maskedLogin";
  private static final String BANK_NAME = "bankName";
  private static final String AMOUNT = "amount";
  private static final String DATE = "date";
  private static final String CONTACT_EMAIL = "contactEmail";
  private static final String IOSAPPLINK = "iosAppLink";
  private static final String ANDROIDAPPLINK = "androidAppLink";
  private static final String WEB_URL = "webUrl";
  private static final String APP_URL = "appUrl";


  @Autowired
  private CryptoUtil cryptoUtil;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private EmailClient emailClient;

  @Autowired
  private Configuration freemarkerConfiguration;

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;
  

  public void setHttpServletContext(HttpServletContext httpServletContext) {
    this.httpServletContext = httpServletContext;
  }

  private String render(String template, Map<String, Object> model)
      throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException,
      TemplateException {
    freemarkerConfiguration.setClassForTemplateLoading(this.getClass(), "/templates/");
    return FreeMarkerTemplateUtils.processTemplateIntoString(
        freemarkerConfiguration.getTemplate(template), ImmutableMap.copyOf(model));
  }

  protected boolean emailSend(final String emailId, Map<String, Object> model, String subject) {
    
    List<User> users = userDBService.getUsersEmailOptionalRoleAndPhone(null, null, emailId, null);
    
    for(User user:users) {
    	if(user != null && user.getStatus() == UserStatus.ACTIVE.value()  
                && user.getIsEmailVerified() == DBConstants.EmailVerified.NOT_VERIFIED.value()) {
        LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
            .message("**** EMAIL CANNOT BE SEND AS ISEMAILVERFIED IS FALSE ****....")
            .format());
        return false;
      }
    }

    try {
      model.put(CONTACT_EMAIL, applicationProperties.getContactEmail());
      model.put("ostaLogo", applicationProperties.getLogoImgPath());
      model.put("mailImg", applicationProperties.getmailImgPath()); 
      model.put("message", render((String)model.get(BODY_TEMPLATE), model));

      String htmlTemplate = "generic-template.html";
      
  
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());       
      String content = render(htmlTemplate, model);
      

     
      if (StringUtils.isEmpty(content)) {
        LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Content empty")
            .format());
      } else {
        return emailClient.sendEmail(applicationProperties.getSenderEmail(),
            Arrays.asList(emailId), subject, content.trim(), true, null,null);
      }
    } catch (Exception e) {
      LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught")
          .format(), e);
    }
     return false;
  }

  public boolean sendTollBankApprovalEmail(final TollRegistration tollRegistration,Bank bank) {

    Map<String, Object> model = new HashMap<>();
    String VehicleNo = "";
    for (TollTag tollTag : tollRegistration.getTollTag()) {
      if (Integer.parseInt(
          tollTag.getStatus()) == DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()) {
        VehicleNo += tollTag.getRegistrationNo() + ", ";

      }
    }
    VehicleNo = VehicleNo.substring(0, VehicleNo.length() - 2);

    model.put(BODY_TEMPLATE, "toll-customer-bank-approval.html");
    model.put(USER_NAME, tollRegistration.getFirstName());
    model.put("vehicleNumber", VehicleNo);
    model.put("bankName", bank.getName());
    model.put("originIp", httpServletContext.getOriginIp());
 
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "FASTAG Approval Awaiting");

  }

  public boolean sendTollCustomerTagAllotedEmail(final String originIp, final TollTag tollTag,
      final TollRegistration tollRegistration)  {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-customer-netc-approved.html");
    model.put("tagId", tollTag.getTagId());
    model.put("vehicleNumber", tollTag.getRegistrationNo());
    model.put("originIp", originIp);
  
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Tag Id Allotted");
  }

  public boolean sendTollCutomerBankApprovalEmail(final String originIp, TollTag tollTag, User user, Bank bank)  {

    Map<String, Object> model = new HashMap<>();

    model.put(BODY_TEMPLATE, "toll-customer-vendor-approval.html");
    model.put("vehicleNumber", tollTag.getRegistrationNo());
    model.put("bank", bank.getName());
    model.put("originIp", originIp);

    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(user.getEmail(), model, "FASTAG TAGID Allocation Awaiting");

  }

  public boolean sendTollCustomerBankRejectionEmail(final String originIp, TollTag tollTag,
      Bank bank, TollRegistration tollRegistration)  {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-customer-bank-rejection.html");

    model.put("vehicleNumber", tollTag.getRegistrationNo());
    model.put("rejectReason", tollTag.getRejectReason());
    model.put("remarks", tollTag.getRemarks() != null ? tollTag.getRemarks() : "Tag Rejected");
    model.put("bankName", bank.getName());
    model.put("originIp", originIp);

    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "FASTAG Application Rejected");

  }

  public boolean sendTollCustomerRegistrationEmail(String vehicleNo, Bank bank,
      TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-customer-registration-success.html");

    model.put("vehicleNumber",vehicleNo);
    model.put("bankName", bank.getName());
    model.put(WEB_URL, applicationProperties.getWebUrl());
    model.put(APP_URL, applicationProperties.getAppUrl());


    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "FASTAG registration successful.");
  }
  
  public boolean tollTagCustomerActivation(String tollTypeName,TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-customer-activation-success.html");

    model.put("tollTypeName",tollTypeName);
   
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "FASTAG Customer Activation Successful.");
  }
  
  //low balance
  public boolean tollTagLowBalance(String vehicleNo, Bank bank,
      TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-tag-low-balance.html");

    model.put("bankName",bank.getName());
   
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Tag Balance low email.");
  }
  
  //Whitelisting
  public boolean tollTagWhiteListing(String vehicleNo, Bank bank,
      TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-tag-whitelisting.html");

    model.put("bankName",bank.getName());
   
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Tag WhiteListing Email.");
  }
  
  //Blacklisting
  public boolean tollTagBlackListing(String vehicleNo, Bank bank,
      TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-tag-blacklisting.html");

    model.put("bankName",bank.getName());
   
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Tag BlackListing email.");
  }
  
  
  //Hotlisting
  public boolean tollTagHotListing(String vehicleNo, Bank bank,
      TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-tag-hotlisting.html");

    model.put("bankName",bank.getName());
   
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Tag HotListing email.");
  }
  
  //closing or replacing
  public boolean tollTagClosingOrReplacing(String vehicleNo, Bank bank,
      TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-tag-closing-or-replacing.html");

    model.put("bankName",bank.getName());
   
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Tag closing or replacing email.");
  }
  
  
  //Usage at Toll
  public boolean tollTagUsageAtToll(String vehicleNo, Bank bank,
      TollRegistration tollRegistration,BigDecimal amount,String tollPlaza,String dateTime, String txnId) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-tag-usage-at-toll.html");

    model.put("bankName",bank.getName());
    model.put("amount",amount);
    model.put("toll",tollPlaza);
    model.put("dateTime",dateTime);
    model.put("vehicleNo", vehicleNo);
    model.put("txnId",txnId);
    model.put(WEB_URL, applicationProperties.getWebUrl());
    model.put(APP_URL, applicationProperties.getAppUrl());
    
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Toll Payment At Toll Plaza "+tollPlaza);
  }
  
  //Tag Deletion
  public boolean tollTagDeletion(String vehicleNo, Bank bank,
      TollRegistration tollRegistration) {

    Map<String, Object> model = new HashMap<>();
    model.put(BODY_TEMPLATE, "toll-tag-deletion.html");

    model.put("bankName",bank.getName());
   
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("model", model).format());

    return emailSend(tollRegistration.getEmailId(), model, "Tag Balance low email.");
  }
  
  public boolean bulkTollTagDeletionStatusSheet(String attachementPath) {

	   Map<String, Object> model = new HashMap<>();

	    model.put("attachementPath", attachementPath);
	    model.put("ostaLogo", applicationProperties.getLogoImgPath());
	    model.put(CONTACT_EMAIL, applicationProperties.getContactEmail());
	    model.put(BODY_TEMPLATE, "bulkTollTagDeletionStatusSheet.html");

	    return emailSend(applicationProperties.getSendBulkTollTagDeletionStatusSheetMailTo() , model, "Bulk TollTag Delete Status email.");
	  }

  
}
