package com.dipcoin.api.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.amqp.TollReqpayListener;
import com.dipcoin.amqp.TollReqpayRabbitMQRequest;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.TollEmailUtils;
import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.filter.HttpServletContext.ClientFeatureFlags;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.Detail;
import com.dipcoin.api.model.Head;
import com.dipcoin.api.model.PartnerProcessDipcoinRequest;
import com.dipcoin.api.model.PartnerProcessDipcoinRequest.SubPartner;
import com.dipcoin.api.model.Ref;
import com.dipcoin.api.model.ReqVehicleDetailResponse;
import com.dipcoin.api.model.Resp;
import com.dipcoin.api.model.RespVehicleDetails;
import com.dipcoin.api.model.RiskScores;
import com.dipcoin.api.model.Score;
import com.dipcoin.api.model.Tag;
import com.dipcoin.api.model.TagList;
import com.dipcoin.api.model.TollException;
import com.dipcoin.api.model.TollGetExceptionListResponse;
import com.dipcoin.api.model.TollNetcAckRequest;
import com.dipcoin.api.model.TollNetcSyncTimeResponse;
import com.dipcoin.api.model.TollRechargeRequest;
import com.dipcoin.api.model.TollReqPayRequest;
import com.dipcoin.api.model.TollResPayResponse;
import com.dipcoin.api.model.TollTagUpdateRequest;
import com.dipcoin.api.model.TollTagUpdateResponse;
import com.dipcoin.api.model.TopUpDetails;
import com.dipcoin.api.model.Txn;
import com.dipcoin.api.model.VahanInfoRequest;
import com.dipcoin.api.model.VehicleDetails;
import com.dipcoin.api.utils.TollMetricRegistry;
//import com.dipcoin.api.model.VahanInfoRequest;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.FeesAndDepositDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TagNPCIApprovalStatusDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.TollRechargeDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.VahanInfoService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionSettlementDone;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.commons.DBConstants.TagDeliveryType;
import com.dipcoin.db.services.commons.DBConstants.TollTagApprovalStatus;
import com.dipcoin.db.services.commons.DBConstants.TollTagExcCodeStatus;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.VehicleCategoryDetail;
import com.dipcoin.db.services.commons.DBConstants.VehicleType;
import com.dipcoin.db.services.commons.DBConstants.VinVrn;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TagNPCIApprovalStatus;
import com.dipcoin.db.services.model.TollRecharge;
import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.db.services.model.VahanInfo;
import com.dipcoin.db.services.model.VahanInfoAudit;
//import com.dipcoin.metrics.TollMetricRegistry;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollConstant.TransactionInitiator;
import com.dipcoin.partner.toll.commons.TollErrorCodes;
import com.dipcoin.partner.toll.commons.TollHttpsServices;
import com.dipcoin.partner.toll.commons.TollSignatureGenerationServices;
import com.dipcoin.partner.toll.commons.TollSignatureVerificationServices;
//import com.dipcoin.partner.toll.commons.TollProperties;
//import com.dipcoin.partner.toll.model.Detail;
//import com.dipcoin.partner.toll.model.Head;
//import com.dipcoin.partner.toll.model.Ref;
//import com.dipcoin.partner.toll.model.Resp;
//import com.dipcoin.partner.toll.model.RespVehicleDetails;
//import com.dipcoin.partner.toll.model.RiskScores;
//import com.dipcoin.partner.toll.model.Score;
//import com.dipcoin.partner.toll.model.Tag;
//import com.dipcoin.partner.toll.model.TagList;
//import com.dipcoin.partner.toll.model.TollException;
//import com.dipcoin.partner.toll.model.TollNetcAckRequest;
//import com.dipcoin.partner.toll.model.TollReqPayRequest;
//import com.dipcoin.partner.toll.model.TollResPayResponse;
//import com.dipcoin.partner.toll.model.TollTagUpdateRequest;
//import com.dipcoin.partner.toll.model.Txn;
//import com.dipcoin.partner.toll.model.VehicleDetails;
//import com.dipcoin.partner.toll.services.TollHttpsServices;
//import com.dipcoin.partner.toll.services.TollSignatureGenerationServices;
//import com.dipcoin.partner.toll.services.TollSignatureVerificationServices;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("tollServicesNetcResource")
public class TollNetcResource {
  private static final Logger LOG = LogManager.getLogger(TollNetcResource.class);
  private final static ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private PartnerDipcoinResource partnerDipcoinResource;

  @Autowired
  private MerchantDBService merchantDBService;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private TollDBService tollDBService;

  @Autowired
  private DipcoinDBService dipcoinDBService;

  @Autowired
  private TollProperties tollProperties;

  @Autowired
  private TollHttpsServices tollHttpsServices;

  @Autowired
  private RabbitTemplate tollReqpayRabbitTemplate;

  @Autowired
  private TollSignatureGenerationServices tollSignatureGenerationServices;

  @Autowired
  private TollSignatureVerificationServices tollSignatureVerificationServices;

  @Autowired
  private FeesAndDepositDBService feesAndDepositDBService;

  @Autowired
  private TollRechargeDBService tollRechargeDBService;

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  private TollRechargeResource tollRechargeResource;

  @Autowired
  private BrontooResource brontooResource;

  @Autowired
  private RedissonClient redissonclient;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContexts;

  @Autowired
  private TollEmailUtils tollEmailUtils;

  @Autowired
  private SmsClient smsClient;

  @Autowired
  private CustomerDBService customerDBService;

  @Autowired
  private EncryptionResource encryptionResource;

  @Autowired
  private Environment environment;

  @Autowired
  @Qualifier("com.dipcoin.metrics.TollMetricRegistry")
  private TollMetricRegistry tollMetricRegistry;

  @Autowired
  private NotificationResource notificationResource;

  @Autowired
  private ApplicationProperties applicationProperties;
  
  @Autowired
  private APIUtils aPIUtils;
  
  @Autowired
  private BankAPIServices bankAPIServices; 
  
  @Autowired
  private TollBankResource tollBankResource;
  
  @Autowired
  private VahanInfoService vahanInfoService;
  
  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;
  
  @Autowired
  private EmailUtils emailUtils;
  
  @Autowired
  private TagNPCIApprovalStatusDBService tagNPCIApprovalStatusDBService;
  
//  private Map<String, Object> map = new HashMap<>();

  public ResponseEntity netcRequest(String tollReqPay) throws Exception {

    TollReqpayRabbitMQRequest tollReq = new TollReqpayRabbitMQRequest();

    tollReq.setTraceId(httpServletContexts.getTraceId());
    tollReq.setTollReqPay(tollReqPay);

    tollReqpayRabbitTemplate.convertAndSend(TollReqpayListener.EXCHANGE,
        TollReqpayListener.ROUTINGKEY, tollReq);

    return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
  }

  //call by rabbitmq
  public void netcRequestPay(String tollReqPay, String traceId) {
    HttpServletContext httpServletContext = null;
    RLock lock = null;
    try {

      Map<String, Object> map = requestPay(tollReqPay, traceId, new HashMap<String, Object>());
      lock = (RLock) map.get("lock");
      if (map.containsKey("netcReqPay") && map.containsKey("tollTag")) {

        TollReqPayRequest netcReqPay = (TollReqPayRequest) map.get("netcReqPay");
        TollTag tollTag = (TollTag) map.get("tollTag");

        BigDecimal totalMinimumAmount = (BigDecimal) map.get("totalMinimumAmount");
        httpServletContext = (HttpServletContext) map.get("httpServletContext");

        ResponseEntity dipcoinResponse = (ResponseEntity) map.get("dipcoinResponse");

        this.tollEmailUtils.setHttpServletContext(httpServletContext);

        Dipcoin dcoin = dipcoinDBService.findDipcoin(tollTag.getCustomerAccountId(),
            DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());

        try {
          if (dcoin != null) {

            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("AutoGenerated Dcoin after process").data("Amount", dcoin.getAmount())
                .format());

            if (!Hibernate.isInitialized(dcoin.getCustomerAccount())) {
              Hibernate.initialize(dcoin.getCustomerAccount());
            }

            TopUpDetails topUpDetails = null;
            if (StringUtils.isNoneBlank(tollTag.getTopUpDetails())) {
              try {
                topUpDetails =
                    objectMapper.readValue(tollTag.getTopUpDetails(), TopUpDetails.class);
              } catch (Exception e) {
                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Exception Caught while converting json").format(), e);
              }
            } else if(StringUtils.isNoneBlank(dcoin.getCustomerAccount().getTopUpDetails())) {

              try {
                topUpDetails = objectMapper.readValue(dcoin.getCustomerAccount().getTopUpDetails(),
                    TopUpDetails.class);
              } catch (Exception e) {
                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Exception Caught while converting json").format(), e);
              }
            }

            if (topUpDetails != null && topUpDetails.getAutoTopUp() == NumberUtils.INTEGER_ONE
                && (dcoin.getAmount()
                    .compareTo(topUpDetails.getAutoTopUpThresHold()) <= NumberUtils.INTEGER_ZERO
                    || tollTag.getAvailableAmount()
                    .compareTo(tollTag.getMinimumAmount()) < NumberUtils.INTEGER_ZERO)) {

              autoTopUp(dcoin.getCustomerAccount(), netcReqPay, httpServletContext, topUpDetails);

            } else {

              if (totalMinimumAmount.compareTo(dcoin.getAmount()) >= NumberUtils.INTEGER_ZERO) {
                topUpDetails = new TopUpDetails();
                topUpDetails.setAutoTopUpAmount(
                    totalMinimumAmount.subtract(dcoin.getAmount()).add(BigDecimal.ONE));
                autoTopUp(dcoin.getCustomerAccount(), netcReqPay, httpServletContext, topUpDetails);
              }

            }

          } else {

            CustomerAccount customerAccount =
                customerDBService.getAccountById(tollTag.getCustomerAccountId());

            if (customerAccount == null) {
              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("Customer Account not found").format());


            } else {

              TopUpDetails topUpDetails = null;
              
              if (StringUtils.isNoneBlank(tollTag.getTopUpDetails())) {
                try {
                  topUpDetails =
                      objectMapper.readValue(tollTag.getTopUpDetails(), TopUpDetails.class);
                } catch (Exception e) {
                  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Exception Caught while converting json").format(), e);
                }
              } else if (StringUtils.isNoneBlank(customerAccount.getTopUpDetails())) {
                try {
                  topUpDetails =
                      objectMapper.readValue(customerAccount.getTopUpDetails(), TopUpDetails.class);
                } catch (Exception e) {
                  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Exception Caught while converting json").format(), e);
                }
              }

              if (topUpDetails != null && topUpDetails.getAutoTopUp() == NumberUtils.INTEGER_ONE) {

                autoTopUp(customerAccount, netcReqPay, httpServletContext, topUpDetails);

              } else {
                topUpDetails = new TopUpDetails();
                topUpDetails.setAutoTopUpAmount(totalMinimumAmount.add(BigDecimal.ONE));
                autoTopUp(customerAccount, netcReqPay, httpServletContext, topUpDetails);

              }



            }
          }
        } catch (Exception e) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Exception caught while Topup").format(), e);

        }
        tollTag = tollDBService.findTollCustomersByTagId(netcReqPay.getVehicle().getTagId());

        if (tollTag == null) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("TollTag not found").format());
          tollTag = (TollTag) map.get("tollTag");
        }


        if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)) {
        	try {
                Bank bank = bankDBService.getBank(tollTag.getWalletBankId()>NumberUtils.INTEGER_ZERO ? tollTag.getWalletBankId() : tollTag.getBankId());

                if (bank != null) {
        	boolean updateLowBalance = true;
//            if (StringUtils.isNotBlank(bankAPIServices.getBankProperties(bank.getReferenceId()).getNotCallBankServer())) {
//            	
//         	   SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
//         	   String dateString = dateFormat.format(new Date(System.currentTimeMillis()));
//         	   	
//         	   	LocalTime serverTime = LocalTime.parse( dateString ) ; 	   	
//         	   	
//         	   	if(serverTime.isAfter( LocalTime.parse(bankAPIServices.getBankProperties(bank.getReferenceId()).getNotCallBankServer().trim().split("-")[NumberUtils.INTEGER_ZERO]) ) 
//             	   	&& serverTime.isBefore( LocalTime.parse( bankAPIServices.getBankProperties(bank.getReferenceId()).getNotCallBankServer().trim().split("-")[NumberUtils.INTEGER_ONE]))) {
//         	   	  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//         	   			.message("Bank server connection is prohibited because of time constraint so not updating low balance at netc")
//         	   			.data("serverTime", serverTime).format());
//         	   	updateLowBalance=false;
//         	   	}
//            }
            
            if(updateLowBalance) {
        	
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Toll tag is now in low balance so updating the tag at netc").format());


          
              // success sms
              if (!applicationProperties.getAwsSMSClient()
                  && !smsClient.sendSms(
                      tollTag.getTollRegistration().getMobileNo(), Templates.TollTagLowBalance
                          .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                      true)) {
                LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Failed to send SMS")
                    .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
              }

              if (applicationProperties.getAwsSMSClient()) {

                NotificationRequestContext notificationRequestContext =
                    new NotificationRequestContext();
                notificationRequestContext.setTraceId(httpServletContext.getTraceId());
                if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                    Templates.TollTagLowBalance
                        .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                    httpServletContext.getClientFeatureFlags().smsEnabled(),
                    notificationRequestContext)) {

                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Failed to send SMS")
                      .data("phone", tollTag.getTollRegistration().getMobileNo()).format());

                }
              }

              // Email
              if (!this.tollEmailUtils.tollTagLowBalance(tollTag.getRegistrationNo(), bank,
                  tollTag.getTollRegistration())) {
                LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Failed to send email to set tag low balance.")
                    .data("user email", tollTag.getTollRegistration().getEmailId()).format());
              }
            }
                }
          } catch (Exception e) {
            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Exception in sending email & sms to Customer.").format());
          }

          try {

            brontooResource.setHttpServletContext(httpServletContext);
            brontooResource.updateExceptionList(tollTag, TollConstant.ADD_OP, null);
          } catch (Exception e) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Exception caught while updating exception code").format(), e);
          }

        }
      
        String excCode = tollTag.getExcCode();
        
        if (excCode.equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST)
            || excCode.equalsIgnoreCase(TollConstant.EXC_CODE_HOTLIST)
            || excCode.equalsIgnoreCase(TollConstant.EXC_CODE_CLOSED_OR_REPLACED)) {
          
          try {
            Bank bank = bankDBService.getBank(
                tollTag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? tollTag.getWalletBankId()
                    : tollTag.getBankId());
            
            if (bank != null) {
             
              if(excCode.equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST)) {

                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Toll tag is now in low black listing so updating the tag at netc")
                    .format());

                if (!applicationProperties.getAwsSMSClient()
                    && !smsClient.sendSms(
                        tollTag.getTollRegistration().getMobileNo(), Templates.TollTagBlackListing
                            .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                        true)) {
                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Failed to send SMS")
                      .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                }

                if (applicationProperties.getAwsSMSClient()) {

                  NotificationRequestContext notificationRequestContext =
                      new NotificationRequestContext();
                  notificationRequestContext.setTraceId(httpServletContext.getTraceId());
                  if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                      Templates.TollTagBlackListing
                          .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                      httpServletContext.getClientFeatureFlags().smsEnabled(),
                      notificationRequestContext)) {

                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                        .message("Failed to send SMS")
                        .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                  }
                }
                // Email
                if (!this.tollEmailUtils.tollTagBlackListing(tollTag.getRegistrationNo(), bank,
                    tollTag.getTollRegistration())) {
                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Failed to send email to Blacklisting of tag.")
                      .data("user email", tollTag.getTollRegistration().getEmailId()).format());
                }
              }

              else if (excCode.equalsIgnoreCase(TollConstant.EXC_CODE_HOTLIST)) {

                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Toll tag is hot listing so updating the tag at netc")
                    .format());

                if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(
                    tollTag.getTollRegistration().getMobileNo(), Templates.TollTagHotListing
                        .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                    true)) {
                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Failed to send SMS")
                      .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                }

                if (applicationProperties.getAwsSMSClient()) {

                  NotificationRequestContext notificationRequestContext =
                      new NotificationRequestContext();
                  notificationRequestContext.setTraceId(httpServletContext.getTraceId());
                  if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                      Templates.TollTagHotListing
                          .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                      httpServletContext.getClientFeatureFlags().smsEnabled(),
                      notificationRequestContext)) {

                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                        .message("Failed to send SMS")
                        .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                  }
                }
                // Email
                if (!this.tollEmailUtils.tollTagHotListing(tollTag.getRegistrationNo(), bank,
                    tollTag.getTollRegistration())) {
                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Failed to send email to Hotlisting of tag.")
                      .data("user email", tollTag.getTollRegistration().getEmailId()).format());
                }
              }
              
              else {
                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Toll tag is closing or replacing so updating the tag at netc")
                    .format());
                
                if (!applicationProperties.getAwsSMSClient()
                    && !smsClient.sendSms(
                        tollTag.getTollRegistration().getMobileNo(), Templates.TollTagClosingOrReplacing
                            .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                        true)) {
                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Failed to send SMS")
                      .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                }

                if (applicationProperties.getAwsSMSClient()) {

                  NotificationRequestContext notificationRequestContext =
                      new NotificationRequestContext();
                  notificationRequestContext.setTraceId(httpServletContext.getTraceId());
                  if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                      Templates.TollTagClosingOrReplacing
                          .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                      httpServletContext.getClientFeatureFlags().smsEnabled(),
                      notificationRequestContext)) {

                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                        .message("Failed to send SMS")
                        .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                  }
                }
                // Email
                if (!this.tollEmailUtils.tollTagClosingOrReplacing(tollTag.getRegistrationNo(), bank,
                    tollTag.getTollRegistration())) {
                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Failed to send email to closing or replacing of tag.")
                      .data("user email", tollTag.getTollRegistration().getEmailId()).format());
                }
              }
            }
            
          } catch (Exception e) {
            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Exception in sending email & sms to Customer.").format());
          }
          brontooResource.setHttpServletContext(httpServletContext);
          brontooResource.updateExceptionList(tollTag, TollConstant.ADD_OP, null);
        }      
      }
    } catch (Exception e) {
      LOG.debug(LogFormatter.instance(traceId).message("Exception Caught").format(), e);
    } finally {
      if (lock != null) {
        try {
          lock.unlock();
          LOG.debug(LogFormatter.instance(traceId).message("Lock unlocked").format());
        } catch (Exception ex) {
          LOG.debug(LogFormatter.instance(traceId).message("Exception Caught while unlocking Lock")
              .format(), ex);
        }
      }
    }
    LOG.debug(LogFormatter.instance(traceId).message("Reqpay Completed").format());
  }


  @Transactional(rollbackFor = {Exception.class, APIException.class},
      propagation = Propagation.REQUIRES_NEW)
  public ResponseEntity autoTopUp(CustomerAccount cAccount, TollReqPayRequest netcReqPay,
      HttpServletContext httpServletContext, TopUpDetails topUpDetails)
      throws APIException, Exception {

    try {
      TollRechargeRequest rechargeReq = new TollRechargeRequest();

      LOG.debug(
          LogFormatter.instance(httpServletContext.getTraceId()).message("TopUp started").format());
      this.tollEmailUtils.setHttpServletContext(httpServletContext);
      User user = cAccount.getUser();

      if (Hibernate.isInitialized(cAccount.getUser())) {

        Hibernate.initialize(cAccount.getUser());
        user = cAccount.getUser();

      }

      if (!encryptionResource.initUserEncryption(user, APIConstants.SESSION_TIMEOUT)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Failed to reset user encryption key").format());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
      }

      rechargeReq.setAuthorizationPin(APIConstants.PIN);
      rechargeReq.setCardId(cAccount.getUserCardId());
      rechargeReq.setCurrency(netcReqPay.getPayer().getAmount().getCurr());
      rechargeReq.setRechargeAmount(topUpDetails.getAutoTopUpAmount());
      rechargeReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
      rechargeReq.setType("NETC");
      rechargeReq.setAutomaticFlag("false");
      rechargeReq.setTagId(netcReqPay.getVehicle().getTagId());
      rechargeReq.setInitiatedFrom(TransactionInitiator.NPCI.value());

      tollRechargeResource.setHttpServletContext(httpServletContext);
      tollRechargeResource.getUserEventResource().setHttpServletContext(httpServletContext);
      tollRechargeResource.getBrontooResource().setHttpServletContext(httpServletContext);
      tollRechargeResource.getCustomerDipcoinResource().setHttpServletContext(httpServletContext);
      tollRechargeResource.getEncryptionResource().setHttpServletContext(httpServletContext);

      ResponseEntity tollRechargeResponse = tollRechargeResource.tollRechargeCreateOsta(
          cAccount.getUser(), rechargeReq, netcReqPay.getTxn().getId(), null);

      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Topup Completed")
          .format());

      return tollRechargeResponse;
    } catch (Exception e) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Exception caught while Top up").format(), e);
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(APIResponse.error(HeaderCode.TOLL_RECHARGE_FAILURE));
  }

  /*
   * any if condition which is related to osta system not netc request validation it should not
   * return anything as this is a deemed success request
   * 
   */
  @Transactional(rollbackFor = {Exception.class, APIException.class},
      propagation = Propagation.REQUIRES_NEW)
  public Map<String, Object> requestPay(String tollReqPay, String traceId, Map<String, Object> map) {

    PartnerProcessDipcoinRequest dcoinReq = new PartnerProcessDipcoinRequest();
    TollReqPayRequest netcReqPay = null;
    Bank bank = null;
    TollTag tollTag = null;
    List<Merchant> merchants = null;
    Merchant merchant = null;
    RLock lock = null;
    ResponseEntity dipcoinResponse = null;

    HttpServletContext httpServletContext = HttpServletContext.instance();
    httpServletContext.setTraceId(traceId);
    httpServletContext.setOriginIp("cm.npci.org.in");
    httpServletContext.setClientFeatureFlags(ClientFeatureFlags.instance());
    httpServletContext.setClientTransactionId(traceId);

    User user = null;
    try {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("TollRequestPay String => " + tollReqPay).format());
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .data("httpServletContext.getClientFeatureFlags() is",
              httpServletContext.getClientFeatureFlags())
          .data("OriginIp is", httpServletContext.getOriginIp())
          .data("httpServletContext.getClientFeatureFlags() is",
              httpServletContext.getClientFeatureFlags())
          .data("smsEnabled is", httpServletContext.getClientFeatureFlags().smsEnabled())
          .data("notificationEnabled is",
              httpServletContext.getClientFeatureFlags().notificationEnabled())
          .format());

      //convert xml body to java obj
      netcReqPay = JAXB.unmarshal(new StringReader(tollReqPay), TollReqPayRequest.class);

      for (Detail bankDetail : netcReqPay.getVehicle().getVehicleDetails()
          .get(NumberUtils.INTEGER_ZERO).getDetail()) {
        if (bankDetail.getName().equalsIgnoreCase(TollConstant.BANK_ID)) {
          bank = bankDBService.getBankByIin(bankDetail.getValue());

          if (bank == null) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Bank IIN Not Present").data("BankId", bankDetail.getValue()).format());
            bank = new Bank();
            bank.setOrgId(netcReqPay.getHead().getOrgId());
            sendResponsePay(httpServletContext, netcReqPay,
                TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                    .code(),
                null);
            addDtxEntry(tollTag, netcReqPay, httpServletContext, "Bank IIN Not Present", null,
                user);

            tollMetricRegistry.numberOfFailedTollTransaction().increment();
            return map;
          }
        }
      }

      //fetch vehicle number from req body
      for (Detail bankDetail : netcReqPay.getVehicle().getVehicleDetails()
          .get(NumberUtils.INTEGER_ZERO).getDetail()) {
        if (bankDetail.getName().equalsIgnoreCase(TollConstant.REGNUMBER)) {

          if (bankDetail.getValue().length() > 20) {
            try {
              byte[] decryptedData =
                  decrypt(
                      tollSignatureVerificationServices
                          .getPrivateKey(httpServletContext.getTraceId(), bank.getOrgId()),
                      Base64.getDecoder().decode(bankDetail.getValue()));
              netcReqPay.getVehicle().setVehicleRegNo(new String(decryptedData));
            } catch (Exception e) {
              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("vehicle number decryption error").format(), e);
            }
          } else {
            netcReqPay.getVehicle().setVehicleRegNo(bankDetail.getValue());
          }

        }

      }
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("tollReqPayRequest data").data("tagId", netcReqPay.getVehicle().getTagId())
          .data("txnId", netcReqPay.getTxn().getId()).format());

      merchants = merchantDBService
          .asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();

      if (CollectionUtils.isEmpty(merchants)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("merchant not found")
            .data("errorcode sent",
                TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                    .code())
            .format());

        sendResponsePay(httpServletContext, netcReqPay,
            TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                .code(),
            bank);
        addDtxEntry(tollTag, netcReqPay, httpServletContext, "Merchant not found", null, user);
        tollMetricRegistry.numberOfFailedTollTransaction().increment();
        return map;

      }

      merchant = merchants.get(NumberUtils.INTEGER_ZERO);

      //on production only else part should run
      if (environment != null
          && !(new HashSet<>(Arrays.asList(ArrayUtils.isNotEmpty(environment.getActiveProfiles())
              ? environment.getActiveProfiles()
              : environment.getDefaultProfiles())).contains("prod"))
          && StringUtils.isNoneBlank(netcReqPay.getPayee().getTxnInitiator())
          && TollConstant.TransactionInitiator.OSTA.value()
              .equalsIgnoreCase(netcReqPay.getPayee().getTxnInitiator())) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .data("Txn Initiator ", netcReqPay.getPayee().getTxnInitiator()).format());
        tollSignatureVerificationServices.verifySignature(httpServletContext.getTraceId(),
            tollReqPay, bank.getOrgId());
      } else {        
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .data("Txn Initiator ", TollConstant.TransactionInitiator.NPCI.value()).format());
        tollSignatureVerificationServices.verifySignature(httpServletContext.getTraceId(),
            tollReqPay, "npci");
      }

      if (!TollSignatureVerificationServices.verificationResult && tollProperties.isConnectNpci()) {

        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message(TollErrorCodes.errorCode_INCORRECT_SIGNATURE_AUTHENTICATION_VALUE.message())
            .format());

        sendResponsePay(httpServletContext, netcReqPay,
            TollErrorCodes.errorCode_INCORRECT_SIGNATURE_AUTHENTICATION_VALUE.code(), bank);
        addDtxEntry(tollTag, netcReqPay, httpServletContext,
            TollErrorCodes.errorCode_INCORRECT_SIGNATURE_AUTHENTICATION_VALUE.message(),
            merchants.get(0), user);
        tollMetricRegistry.numberOfFailedTollTransaction().increment();
        return map;
      }

      try {
        tollTag = tollDBService.findByTollTagOrTid(netcReqPay.getVehicle().getTagId(),
            netcReqPay.getVehicle().getTID());
      } catch (Exception e) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("found non unique entry while fetching tollTag using tagId and Tid").format(),
            e);
      }
      if (tollTag == null) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("TagId Is Not Present In Osta System")
            .data("errorcode sent", TollErrorCodes.Error_code_UNREGISTERED_TAGS.code()).format());
		if (tollProperties.isConnectNpci()) {

			sendResponsePay(httpServletContext, netcReqPay, TollErrorCodes.Error_code_UNREGISTERED_TAGS.code(), bank);
			addDtxEntry(tollTag, netcReqPay, httpServletContext, TollErrorCodes.Error_code_UNREGISTERED_TAGS.message(),
					merchants.get(0), user);

			TollTag newTag = new TollTag();

			Epc epc = tollDBService.asyncFindEpcByTagId(netcReqPay.getVehicle().getTagId()).get();
			if (epc == null) {
				newTag.setTagId(netcReqPay.getVehicle().getTagId());
				newTag.setTid(netcReqPay.getVehicle().getTID());
			} else {
				newTag.setTagId(epc.getRfidTag());
				newTag.setTid(epc.getTid());
			}
			String dummyVehicleNumber = TollConstant.getRandomVehicleNumber();
			newTag.setExcCode(String.valueOf(TollTagExcCodeStatus.LOW_BALANCE.value()));
			newTag.setRegistrationNo(dummyVehicleNumber);
			newTag.setCategory(VehicleCategoryDetail.VC4.name());
			newTag.setIsCommercial(VehicleType.NOT_COMMERCIAL.value());

			try {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Unregistered TagId addition into npci mapper during reqpay")
						.data("errorcode sent", TollErrorCodes.Error_code_UNREGISTERED_TAGS.code()).format());
				TollTagUpdateResponse tollTagUpdateResponse = this.uploadTheTollListToNETC(httpServletContext,
						Arrays.asList(newTag), TollConstant.ADD_OP, bank, merchant);
				if (tollTagUpdateResponse == null) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("tollTagUpdateResponse is null").format());
				}

				List<HeaderCode> headerCodes = new ArrayList<>();
				boolean callUpdateNpci = false;

				for (TollTagUpdateResponse.Txn.Resp.Tag tag : tollTagUpdateResponse.getTxn().getResp().getTag()) {

					String[] errCodes = tag.getErrCode().split(",");
					String[] respCodes = tollTagUpdateResponse.getTxn().getResp().getRespCode().split(",");
					for (HeaderCode headerCode : HeaderCode.values()) {
						for (String errCode : errCodes) {

							if (headerCode.code().equalsIgnoreCase("N-" + errCode)) {
								headerCodes.add(headerCode);

							}
						}
						for (String respCode : respCodes) {

							if (headerCode.code().equalsIgnoreCase("N-" + respCode)) {
								headerCodes.add(headerCode);

							}
						}
					}

					for (String errCode : errCodes) {

						if (HeaderCode.VEHICLE_REGNO_OR_VIN_OR_ENGINENUMBER_ALREADY_REGISTERED_WITH_SOME_OTHER_TAGID
								.code().equalsIgnoreCase("N-" + errCode)) {

							callUpdateNpci = true;
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message(
									HeaderCode.VEHICLE_REGNO_OR_VIN_OR_ENGINENUMBER_ALREADY_REGISTERED_WITH_SOME_OTHER_TAGID
											.message() + StringUtils.EMPTY + newTag.getRegistrationNo())
									.format());
						}
					}

					if (callUpdateNpci) {
						dummyVehicleNumber = TollConstant.getRandomVehicleNumber();
						newTag.setRegistrationNo(dummyVehicleNumber);
						tollTagUpdateResponse = this.uploadTheTollListToNETC(httpServletContext, Arrays.asList(newTag),
								TollConstant.UPDATE_OP, bank, merchant);

						if (tollTagUpdateResponse == null) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("tollTagUpdateResponse is null").format());
						}
					}
				}
			} catch (Exception e) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Exception Caught while registering Unregistered tag in npci").format(), e);
			}
			if(epc != null) {
				epc.setDummyRegistrationNo(dummyVehicleNumber);
				tollDBService.saveEpc(epc);

			}
			
		}
        tollMetricRegistry.numberOfFailedTollTransaction().increment();
        return map;

      }
      
     

      List<TollTag> tollTags =
          tollDBService.asyncFindTollTagsByCustomerAccountId(tollTag.getCustomerAccountId()).get();
      BigDecimal totalMinimumAmount = BigDecimal.ZERO;
      for (TollTag tolltag : tollTags) {
        if (DBConstants.TollTagApprovalStatus.ACTIVE.value() == Integer
            .parseInt(tolltag.getStatus())
            || DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer
                .valueOf(tolltag.getStatus())
            || DBConstants.TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value() == Integer
                .valueOf(tolltag.getStatus())) {
          totalMinimumAmount = totalMinimumAmount.add(tolltag.getMinimumAmount());
        }
      }

      map.put("totalMinimumAmount", totalMinimumAmount);

      List<Integer> userId = new ArrayList<Integer>();
      String errorCodes =
          TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
              .code();

      userId.add(tollTag.getTollRegistration().getUserId());

      List<User> users = userDBService.asyncGetUsersByIds(userId).get();

      if (CollectionUtils.isEmpty(users)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("User Not Found")
            .format());

        sendResponsePay(httpServletContext, netcReqPay,
            TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                .code(),
            bank);
        addDtxEntry(tollTag, netcReqPay, httpServletContext, "User Not Found", merchants.get(0),
            user);
        tollMetricRegistry.numberOfFailedTollTransaction().increment();
        return map;

      }
      user = users.get(0);

      lock = redissonclient.getFairLock(String.valueOf(tollTag.getCustomerAccountId()));
      lock.lock(60, TimeUnit.SECONDS);
      
      Dipcoin dcoin = dipcoinDBService.findDipcoin(tollTag.getCustomerAccountId(),
          DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());
      
      if (dcoin == null) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Dipcoin Not Found").format());

        TopUpDetails topUpDetails = new TopUpDetails();

        topUpDetails.setAutoTopUpAmount(netcReqPay.getPayer().getAmount().getValue()
            .add(totalMinimumAmount).add(BigDecimal.ONE));

        CustomerAccount customerAccount =
            customerDBService.getAccountById(tollTag.getCustomerAccountId());

        if (customerAccount == null) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Customer Account not found").format());

          sendResponsePay(httpServletContext, netcReqPay,
              TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                  .code(),
              bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext, "Customer Account Not Found",
              merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;
        }

        autoTopUp(customerAccount, netcReqPay, httpServletContext, topUpDetails);

        tollTag = tollDBService.findTollCustomersByTagId(netcReqPay.getVehicle().getTagId());

        if (tollTag == null) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("TagId Is Not Present In Osta System")
              .data("errorcode sent", TollErrorCodes.Error_code_UNREGISTERED_TAGS.code()).format());
          if (tollProperties.isConnectNpci())

            sendResponsePay(httpServletContext, netcReqPay,
                TollErrorCodes.Error_code_UNREGISTERED_TAGS.code(), bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              TollErrorCodes.Error_code_UNREGISTERED_TAGS.message(), merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;

        }

        dcoin = dipcoinDBService.findDipcoin(tollTag.getCustomerAccountId(),
            DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());

        if (dcoin == null) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Dipcoin Not Found").format());
          
          tollTag.setExcCode(TollConstant.EXC_CODE_LOWBALANCE_LIST);
          tollTag.setExcCodeUpdateTime(String.valueOf(DateTime.now().getMillis()));          
          tollTag = tollDBService.updateTollTag(tollTag);
          
          httpServletContext.setClientTransactionId(netcReqPay.getTxn().getId());
          map.put("tollTag", tollTag);
          map.put("netcReqPay", netcReqPay);
          map.put("httpServletContext", httpServletContext);
          
          sendResponsePay(httpServletContext, netcReqPay,
              TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                  .code(),
              bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext, "Dipcoin Not Found",
              merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;

        }



      }

      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Resource Is Locked For").data("Dipcoin Id", dcoin.getId()).format());

      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Reqpay Type Is")
          .data("Type", netcReqPay.getTxn().getType()).format());

      if (TollConstant.REQPAY_TYPE_CREDIT.equalsIgnoreCase(netcReqPay.getTxn().getType())
          || TollConstant.REQPAY_TYPE_CREDIT_ADVICE
              .equalsIgnoreCase(netcReqPay.getTxn().getType())) {

        if (netcReqPay.getPayer().getAmount().getValue().compareTo(BigDecimal.valueOf(0)) < 0
            && TollConstant.REQPAY_TYPE_CREDIT.equalsIgnoreCase(netcReqPay.getTxn().getType())) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message(TollErrorCodes.errorcode_NEGATIVE_CREDIT_REQUEST.message())
              .data("Amount", netcReqPay.getPayer().getAmount().getValue()).format());
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              TollErrorCodes.errorcode_NEGATIVE_CREDIT_REQUEST.message(), merchants.get(0), user);
          errorCodes = TollErrorCodes.errorcode_NEGATIVE_CREDIT_REQUEST.code();
        } else {
          errorCodes = TollErrorCodes.Error_code_SUCCESS.code();
          creditCustomerAccount(netcReqPay, merchants.get(0), httpServletContext, tollTag, user);

        }

      } else if ((TollConstant.REQPAY_TYPE_DEBIT_ADVICE
          .equalsIgnoreCase(netcReqPay.getTxn().getType())
          || TollConstant.REQPAY_TYPE_DEBIT.equalsIgnoreCase(netcReqPay.getTxn().getType()))) {

        // validate reqpay and set error codes
        errorCodes =
            validateReqPay(errorCodes, netcReqPay, dcoin, tollTag, httpServletContext, bank);

        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("totalMinimumAmount = " + totalMinimumAmount).format());
        if (errorCodes.equalsIgnoreCase(TollErrorCodes.Error_code_INVALID_TAG_SIGNATURE.code())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message(TollErrorCodes.Error_code_INVALID_TAG_SIGNATURE.code()).format());

          sendResponsePay(httpServletContext, netcReqPay, errorCodes, bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              TollErrorCodes.Error_code_INVALID_TAG_SIGNATURE.message(), merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;
        } else if (errorCodes.equalsIgnoreCase(TollErrorCodes.errorcode_BLACKLISTED_TAG.code())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message(TollErrorCodes.errorcode_BLACKLISTED_TAG.message()).format());

          sendResponsePay(httpServletContext, netcReqPay, errorCodes, bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              TollErrorCodes.errorcode_BLACKLISTED_TAG.message(), merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;
        }else if (errorCodes.equalsIgnoreCase(TollErrorCodes.errorcode_HOTLISTED_TAG.code())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message(TollErrorCodes.errorcode_HOTLISTED_TAG.message()).format());

          sendResponsePay(httpServletContext, netcReqPay, errorCodes, bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              TollErrorCodes.errorcode_HOTLISTED_TAG.message(), merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;
        } else if (errorCodes
            .equalsIgnoreCase(TollErrorCodes.errorcode_NEGATIVE_DEBIT_REQUEST.code())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message(TollErrorCodes.errorcode_NEGATIVE_DEBIT_REQUEST.code()).format());

          sendResponsePay(httpServletContext, netcReqPay,
              TollErrorCodes.errorcode_NEGATIVE_DEBIT_REQUEST.code(), bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              TollErrorCodes.errorcode_NEGATIVE_DEBIT_REQUEST.message(), merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;
        } else if (errorCodes.equalsIgnoreCase(
            TollErrorCodes.Error_code_CONDITION_APPLIED_READERREADTIME_TIME_OF_UPDATING_TAG_ID_IN_LOW_BAL_EX_LIST_20_MINUTES_TAG_IN_LOW_BALANCE_EXCEPTION_LIST
                .code())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message(
              TollErrorCodes.Error_code_CONDITION_APPLIED_READERREADTIME_TIME_OF_UPDATING_TAG_ID_IN_LOW_BAL_EX_LIST_20_MINUTES_TAG_IN_LOW_BALANCE_EXCEPTION_LIST
                  .code())
              .format());

          sendResponsePay(httpServletContext, netcReqPay,
              TollErrorCodes.Error_code_CONDITION_APPLIED_READERREADTIME_TIME_OF_UPDATING_TAG_ID_IN_LOW_BAL_EX_LIST_20_MINUTES_TAG_IN_LOW_BALANCE_EXCEPTION_LIST
                  .code(),
              bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              TollErrorCodes.Error_code_CONDITION_APPLIED_READERREADTIME_TIME_OF_UPDATING_TAG_ID_IN_LOW_BAL_EX_LIST_20_MINUTES_TAG_IN_LOW_BALANCE_EXCEPTION_LIST
                  .message(),
              merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;
        }

        // if osta
        if (netcReqPay.getPayer().getAmount().getValue()
            .compareTo(dcoin.getAmount().subtract(totalMinimumAmount)
                .add(tollTag.getMinimumAmount())) > NumberUtils.INTEGER_ZERO) {

          TopUpDetails topUpDetails = new TopUpDetails();

          if (!Hibernate.isInitialized(dcoin.getCustomerAccount())) {
            Hibernate.initialize(dcoin.getCustomerAccount());
          }

          topUpDetails.setAutoTopUpAmount(netcReqPay.getPayer().getAmount().getValue()
              .subtract(dcoin.getAmount().subtract(totalMinimumAmount)).add(BigDecimal.ONE));

          autoTopUp(dcoin.getCustomerAccount(), netcReqPay, httpServletContext, topUpDetails);

          tollTag = tollDBService.findTollCustomersByTagId(netcReqPay.getVehicle().getTagId());

          if (tollTag == null) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("TagId Is Not Present In Osta System")
                .data("errorcode sent", TollErrorCodes.Error_code_UNREGISTERED_TAGS.code())
                .format());
            if (tollProperties.isConnectNpci())

              sendResponsePay(httpServletContext, netcReqPay,
                  TollErrorCodes.Error_code_UNREGISTERED_TAGS.code(), bank);
            addDtxEntry(tollTag, netcReqPay, httpServletContext,
                TollErrorCodes.Error_code_UNREGISTERED_TAGS.message(), merchants.get(0), user);
            tollMetricRegistry.numberOfFailedTollTransaction().increment();
            return map;

          }

          dcoin = dipcoinDBService.findDipcoin(tollTag.getCustomerAccountId(),
              DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());
          
          if (dcoin == null) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Dipcoin Not Found").format());
            
            tollTag.setExcCode(TollConstant.EXC_CODE_LOWBALANCE_LIST);
            tollTag.setExcCodeUpdateTime(String.valueOf(DateTime.now().getMillis()));          
            tollTag = tollDBService.updateTollTag(tollTag);
            
            httpServletContext.setClientTransactionId(netcReqPay.getTxn().getId());
            map.put("tollTag", tollTag);
            map.put("netcReqPay", netcReqPay);
            map.put("httpServletContext", httpServletContext);

            sendResponsePay(httpServletContext, netcReqPay,
                TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                    .code(),
                bank);
            addDtxEntry(tollTag, netcReqPay, httpServletContext, "Dipcoin Not Found",
                merchants.get(0), user);
            tollMetricRegistry.numberOfFailedTollTransaction().increment();
            return map;

          }

        }

        if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)
            && netcReqPay.getPayer().getAmount().getValue()
                .compareTo(tollTag.getAvailableAmount()) > NumberUtils.INTEGER_ZERO) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Txn amount is greater than tag available amount").format());
          
          httpServletContext.setClientTransactionId(netcReqPay.getTxn().getId());
          map.put("tollTag", tollTag);
          map.put("netcReqPay", netcReqPay);
          map.put("httpServletContext", httpServletContext);

          sendResponsePay(httpServletContext, netcReqPay,
              TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                  .code(),
              bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              "Txn amount is greater than tag available amount", merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;
        } else if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)
            && netcReqPay.getPayer().getAmount().getValue()
                .compareTo(tollTag.getAvailableAmount()) <= NumberUtils.INTEGER_ZERO) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message(
                  "Tag is already in Low Balance List but time of addition is less than 20 min")
              .format());

          if (tollTag.getAvailableAmount().subtract(netcReqPay.getPayer().getAmount().getValue())
              .compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO) {
            tollTag.setAvailableAmount(tollTag.getAvailableAmount()
                .subtract(netcReqPay.getPayer().getAmount().getValue()));
          } else {
            tollTag.setAvailableAmount(BigDecimal.ZERO);
          }

          tollTag = tollDBService.updateTollTag(tollTag);

        } else if (!tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)
            && totalMinimumAmount.compareTo(dcoin.getAmount().subtract(
                netcReqPay.getPayer().getAmount().getValue())) >= NumberUtils.INTEGER_ZERO) {


          if (dcoin.getAmount().compareTo(totalMinimumAmount) >= NumberUtils.INTEGER_ZERO
              && netcReqPay.getPayer().getAmount().getValue()
                  .compareTo(dcoin.getAmount().subtract(totalMinimumAmount
                      .subtract(tollTag.getMinimumAmount()))) > NumberUtils.INTEGER_ZERO) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Txn amount is greater than tag available amount").format());
            
            tollTag.setExcCode(TollConstant.EXC_CODE_LOWBALANCE_LIST);
            tollTag.setExcCodeUpdateTime(String.valueOf(DateTime.now().getMillis()));
            tollTag = tollDBService.updateTollTag(tollTag);
            
            httpServletContext.setClientTransactionId(netcReqPay.getTxn().getId());
            map.put("tollTag", tollTag);
            map.put("netcReqPay", netcReqPay);
            map.put("httpServletContext", httpServletContext);

            sendResponsePay(httpServletContext, netcReqPay,
                TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                    .code(),
                bank);
            addDtxEntry(tollTag, netcReqPay, httpServletContext,
                "Txn amount is greater than tag available amount", merchants.get(0), user);
            tollMetricRegistry.numberOfFailedTollTransaction().increment();
            return map;
          }

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Tag is Getting added in Low Balance List").format());

          if (dcoin.getAmount().compareTo(totalMinimumAmount) < NumberUtils.INTEGER_ZERO) {
            if (tollTag.getAvailableAmount().subtract(netcReqPay.getPayer().getAmount().getValue())
                .compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO) {
              tollTag.setAvailableAmount(tollTag.getAvailableAmount()
                  .subtract(netcReqPay.getPayer().getAmount().getValue()));
            } else {
              tollTag.setAvailableAmount(BigDecimal.ZERO);
            }
          } else {

            if (tollTag.getAvailableAmount()
                .subtract(totalMinimumAmount.subtract(
                    dcoin.getAmount().subtract(netcReqPay.getPayer().getAmount().getValue())))
                .compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO) {
              tollTag.setAvailableAmount(
                  tollTag.getAvailableAmount().subtract(totalMinimumAmount.subtract(
                      dcoin.getAmount().subtract(netcReqPay.getPayer().getAmount().getValue()))));
            } else {
              tollTag.setAvailableAmount(BigDecimal.ZERO);
            }
          }


          tollTag.setExcCode(TollConstant.EXC_CODE_LOWBALANCE_LIST);
          tollTag.setExcCodeUpdateTime(String.valueOf(DateTime.now().getMillis()));
          tollTag = tollDBService.updateTollTag(tollTag);

        }

        httpServletContext.setClientTransactionId(netcReqPay.getTxn().getId());
        dcoinReq.setOsta(user.getPhone().concat(dcoin.getCoin()));
        dcoinReq.setAmount(netcReqPay.getPayer().getAmount().getValue());
        dcoinReq.setCurrency(netcReqPay.getPayer().getAmount().getCurr());
        dcoinReq.setOrderId(
            netcReqPay.getVehicle().getTagId() + DateTime.now(DateTimeZone.UTC).getMillis());
        dcoinReq.setRawRequest(objectMapper.writeValueAsString(netcReqPay));
        dcoinReq.setPartnerTransactionReferenceId(netcReqPay.getTxn().getId());
        SubPartner subPartner = new SubPartner();
        subPartner.setTollLocation(netcReqPay.getMerchant().getName());
        subPartner.setTollLaneDirection(netcReqPay.getMerchant().getLane().getDirection());
        subPartner.setVehicleNumber(netcReqPay.getVehicle().getVehicleRegNo());
        dcoinReq.setSubPartner(subPartner);

        List<String> roleList = new ArrayList<>();
        roleList.add(DBConstants.UserRoles.MERCHANT_INTERNAL.value());



        List<User> merchantUsers =
            this.userDBService.asyncGetBankMerchantUsers(merchants.get(0).getId(), roleList).get();

        if (CollectionUtils.isEmpty(merchantUsers)) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("merchantUser is null").format());

          sendResponsePay(httpServletContext, netcReqPay,
              TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                  .code(),
              bank);
          addDtxEntry(tollTag, netcReqPay, httpServletContext, "Merchant User Not Found",
              merchants.get(0), user);
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
          return map;

        }

        User activeMerchantUser = null;
        for (User merchantUser : merchantUsers) {
          if (merchantUser.getStatus() == DBConstants.UserStatus.ACTIVE.value()) {
            activeMerchantUser = merchantUser;
            break;
          }
        }


        // To check clone txn AND SET ERROR CODES
        List<DipcoinTransaction> dTxs = dipcoinDBService.findByPartnerReferenceIdAndOrderId(
            merchants.get(0).getReferenceId(), netcReqPay.getVehicle().getTagId(),
            DBConstants.TransactionSource.TOLL.value(), NumberUtils.INTEGER_ONE, NumberUtils.INTEGER_ONE);
        
        if (CollectionUtils.isNotEmpty(dTxs)) {
          TollReqPayRequest tollReqPayRequest = null;
          for (DipcoinTransaction dTx : dTxs) {
            
              try {
                tollReqPayRequest =
                    objectMapper.readValue(dTx.getPartnerRawRequest(), TollReqPayRequest.class);
              } catch (Exception e) {

                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Reqpay clone txn Exception").format());
              }
              break;      

          }

          if (tollReqPayRequest != null) {

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);

            Date currentTxnDate = simpleDateFormat
                .parse(netcReqPay.getMerchant().getReaderVerificationResult().getTsRead());
            Date prevTxnDate = simpleDateFormat
                .parse(tollReqPayRequest.getMerchant().getReaderVerificationResult().getTsRead());

            Long cloneTxnTimeDiff = currentTxnDate.getTime() - prevTxnDate.getTime();
            if (tollReqPayRequest.getMerchant().getLane().getDirection()
                .equalsIgnoreCase(netcReqPay.getMerchant().getLane().getDirection())
                && cloneTxnTimeDiff < TollConstant.NEAR_TIME) {
              if (cloneTxnTimeDiff < NumberUtils.INTEGER_ZERO) {
                netcReqPay = tollReqPayRequest;
              }

              errorCodes =
                  TollErrorCodes.errorCode_CLONED_TRANSACTION_AS_PER_THE_DEFINITION_OF_NEAR_TIME
                      .code();
              sendResponsePay(httpServletContext, netcReqPay,
                  TollErrorCodes.errorCode_CLONED_TRANSACTION_AS_PER_THE_DEFINITION_OF_NEAR_TIME
                      .code(),
                  bank);
              addDtxEntry(tollTag, netcReqPay, httpServletContext,
                  TollErrorCodes.errorCode_CLONED_TRANSACTION_AS_PER_THE_DEFINITION_OF_NEAR_TIME
                      .message(),
                  merchants.get(0), user);
              tollMetricRegistry.numberOfFailedTollTransaction().increment();
              return map;
            }
          }
        }

        try {
          if ((netcReqPay.getTxn().getType().equalsIgnoreCase(TollConstant.REQPAY_TYPE_DEBIT)
              || netcReqPay.getTxn().getType()
                  .equalsIgnoreCase(TollConstant.REQPAY_TYPE_DEBIT_ADVICE))) {
            partnerDipcoinResource.setHttpServletContext(httpServletContext);
            partnerDipcoinResource.getEmailUtils().setHttpServletContext(httpServletContext);
            partnerDipcoinResource.getPartnerEncDecResource()
                .setHttpServletContext(httpServletContext);
            partnerDipcoinResource.getUserEventResource().setHttpServletContext(httpServletContext);
            dipcoinResponse =
                partnerDipcoinResource.processCustomerDipcoin(activeMerchantUser, merchants.get(0),
                    dcoinReq, TransactionSource.TOLL, Optional.empty(), Boolean.FALSE);
          }
        } catch (Exception e) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Exception Caught while processing osta").format(), e);

        }

        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .data("dipcoinResponse", dipcoinResponse).format());

        if (dipcoinResponse == null
            || dipcoinResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()
                && netcReqPay.getPayer().getAmount().getValue().compareTo(dcoin.getAmount()) < 0) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message(
              TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                  .message())
              .data("Amount", netcReqPay.getPayer().getAmount().getValue()).format());
          errorCodes =
              TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
                  .code();
          addDtxEntry(tollTag, netcReqPay, httpServletContext,
              "DipcoinResponse is null or DipcoinResponse status is greater than 400",
              merchants.get(0), user);
        } else {
          tollMetricRegistry.tollAmountUsed()
              .increment(netcReqPay.getPayer().getAmount().getValue().doubleValue());
        }
        
        map.put("tollTag", tollTag);
        map.put("netcReqPay", netcReqPay);
        map.put("httpServletContext", httpServletContext);
        map.put("dipcoinResponse", dipcoinResponse);

        if (dipcoinResponse != null
            && dipcoinResponse.getStatusCodeValue() < HttpStatus.BAD_REQUEST.value()) {
          tollMetricRegistry.numberOfTollTransaction().increment();



        } else {
          tollMetricRegistry.numberOfFailedTollTransaction().increment();
        }

      } else if (TollConstant.REQPAY_TYPE_NON_FIN.equalsIgnoreCase(netcReqPay.getTxn().getType())) {
        errorCodes = TollErrorCodes.Error_code_SUCCESS.code();
        addDtxEntry(tollTag, netcReqPay, httpServletContext, TollConstant.REQPAY_TYPE_NON_FIN,
            merchants.get(0), user);
      }


      if (tollProperties.isConnectNpci()) {

        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("final errorCodes to be send").data("errorCodes", errorCodes).format());

        sendResponsePay(httpServletContext, netcReqPay, errorCodes, bank);


      }
      
      if (TollConstant.REQPAY_TYPE_DEBIT_ADVICE
              .equalsIgnoreCase(netcReqPay.getTxn().getType())
              || TollConstant.REQPAY_TYPE_DEBIT.equalsIgnoreCase(netcReqPay.getTxn().getType())) {
      
      try {
          
          String[] fastagTxnDateTime= netcReqPay.getMerchant().getReaderVerificationResult().getTsRead().split("T");
          Dipcoin dcoinn = dipcoinDBService.findDipcoin(tollTag.getCustomerAccountId(),
              DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());
          
			/*
			 * // success sms if (!applicationProperties.getAwsSMSClient() &&
			 * !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
			 * Templates.TollTagUsageAtToll.format(tollTag.getRegistrationNo(),
			 * bank.getAlias() == null ? bank.getName() : bank.getAlias(),
			 * netcReqPay.getPayer().getAmount().getValue(),
			 * netcReqPay.getMerchant().getName() + StringUtils.SPACE +
			 * netcReqPay.getMerchant().getLane().getDirection(), fastagTxnDateTime[0],
			 * fastagTxnDateTime[1], netcReqPay.getTxn().getId()), true)) {
			 * 
			 * LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
			 * .message("Failed to send SMS") .data("phone",
			 * tollTag.getTollRegistration().getMobileNo()) .data("Bank name",
			 * bank.getAlias() == null ? bank.getName() : bank.getAlias())
			 * .data("Payer Name", netcReqPay.getPayer().getName()) .data("Merchant Name",
			 * netcReqPay.getMerchant().getName()).data("Date & Time",
			 * netcReqPay.getMerchant().getReaderVerificationResult().getTsRead())
			 * .format()); }
			 */
          
//       // success sms
//          if (!applicationProperties.getAwsSMSClient()
//              && !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
//                  Templates.TagUsageAtToll.format(tollTag.getRegistrationNo(),
//                      bank.getAlias() == null ? bank.getName() : bank.getAlias(),
//                      netcReqPay.getPayer().getAmount().getValue(),
//                      netcReqPay.getMerchant().getName() + StringUtils.SPACE
//                          + netcReqPay.getMerchant().getLane().getDirection(),
//                          netcReqPay.getMerchant().getGeoCode(),
//                          fastagTxnDateTime[0], fastagTxnDateTime[1], netcReqPay.getTxn().getId()), true)) {
//
//            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
//                .message("Failed to send SMS")
//                .data("phone", tollTag.getTollRegistration().getMobileNo())
//                .data("Bank name", bank.getAlias() == null ? bank.getName() : bank.getAlias())
//                .data("Payer Name", netcReqPay.getPayer().getName())
//                .data("Merchant Name", netcReqPay.getMerchant().getName()).data("Date & Time",
//                    netcReqPay.getMerchant().getReaderVerificationResult().getTsRead())
//                .format());
//          }
       // success sms
          if (!applicationProperties.getAwsSMSClient()
              && !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
                  Templates.TagUsageAtTollNew.format(tollTag.getRegistrationNo(),
                      bank.getAlias() == null ? bank.getName() : bank.getAlias(),
                      netcReqPay.getPayer().getAmount().getValue(),
                      netcReqPay.getMerchant().getName() + StringUtils.SPACE
                          + netcReqPay.getMerchant().getLane().getDirection(),
                          fastagTxnDateTime[0], fastagTxnDateTime[1]), true)) {

            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Failed to send SMS")
                .data("phone", tollTag.getTollRegistration().getMobileNo())
                .data("Bank name", bank.getAlias() == null ? bank.getName() : bank.getAlias())
                .data("Payer Name", netcReqPay.getPayer().getName())
                .data("Merchant Name", netcReqPay.getMerchant().getName()).data("Date & Time",
                    netcReqPay.getMerchant().getReaderVerificationResult().getTsRead())
                .format());
          }


			/*
			 * if (applicationProperties.getAwsSMSClient()) {
			 * 
			 * NotificationRequestContext notificationRequestContext = new
			 * NotificationRequestContext();
			 * notificationRequestContext.setTraceId(httpServletContext.getTraceId()); if
			 * (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
			 * Templates.TollTagUsageAtToll.format(tollTag.getRegistrationNo(),
			 * bank.getAlias() == null ? bank.getName() : bank.getAlias(),
			 * netcReqPay.getPayer().getAmount().getValue(),
			 * netcReqPay.getMerchant().getName() + StringUtils.SPACE +
			 * netcReqPay.getMerchant().getLane().getDirection(), fastagTxnDateTime[0],
			 * fastagTxnDateTime[1], netcReqPay.getTxn().getId()),
			 * httpServletContext.getClientFeatureFlags().smsEnabled(),
			 * notificationRequestContext)) {
			 * 
			 * LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
			 * .message("Failed to send SMS") .data("phone",
			 * tollTag.getTollRegistration().getMobileNo()) .data("Bank name",
			 * bank.getAlias() == null ? bank.getName() : bank.getAlias())
			 * .data("Payer Name", netcReqPay.getPayer().getName()) .data("Merchant Name",
			 * netcReqPay.getMerchant().getName()).data("Date & Time",
			 * netcReqPay.getMerchant().getReaderVerificationResult().getTsRead())
			 * .format());
			 * 
			 * } }
			 */
          
          if (applicationProperties.getAwsSMSClient()) {

              NotificationRequestContext notificationRequestContext =
                  new NotificationRequestContext();
              notificationRequestContext.setTraceId(httpServletContext.getTraceId());
              if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                  Templates.TagUsageAtToll.format(tollTag.getRegistrationNo(),
                      bank.getAlias() == null ? bank.getName() : bank.getAlias(),
                      netcReqPay.getPayer().getAmount().getValue(),
                      netcReqPay.getMerchant().getName() + StringUtils.SPACE
                          + netcReqPay.getMerchant().getLane().getDirection(),
                          netcReqPay.getMerchant().getGeoCode(),
                          fastagTxnDateTime[0], fastagTxnDateTime[1], netcReqPay.getTxn().getId()),
                  httpServletContext.getClientFeatureFlags().smsEnabled(),
                  notificationRequestContext)) {

                LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Failed to send SMS")
                    .data("phone", tollTag.getTollRegistration().getMobileNo())
                    .data("Bank name", bank.getAlias() == null ? bank.getName() : bank.getAlias())
                    .data("Payer Name", netcReqPay.getPayer().getName())
                    .data("Merchant Name", netcReqPay.getMerchant().getName()).data("Date & Time",
                        netcReqPay.getMerchant().getReaderVerificationResult().getTsRead())
                    .format());

              }
            }
          // Email

          this.tollEmailUtils.setHttpServletContext(httpServletContext);
          if (!this.tollEmailUtils.tollTagUsageAtToll(tollTag.getRegistrationNo(), bank, 
              tollTag.getTollRegistration(), netcReqPay.getPayer().getAmount().getValue(),
              netcReqPay.getMerchant().getName() + StringUtils.SPACE
                  + netcReqPay.getMerchant().getLane().getDirection(),
                  netcReqPay.getMerchant().getReaderVerificationResult().getTsRead().replaceAll("T", StringUtils.SPACE), netcReqPay.getTxn().getId())) {
            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Failed to send email for toll tag usage")
                .data("user email", tollTag.getTollRegistration().getEmailId()).format());
          }

        } catch (Exception e) {
          LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Exception in sending email & sms to Customer.").format(), e);
        }

      }
    } catch (Exception e) {
      LOG.debug(LogFormatter.instance(traceId).message("Exception Caught").format(), e);
      sendResponsePay(httpServletContext, netcReqPay,
          TollErrorCodes.errorCode_TRANSACTION_STATUS_IS_SET_TO_FAILED_AND_AMOUNT_IS_GREATER_THAN_ZERO
              .code(),
          bank);
      addDtxEntry(tollTag, netcReqPay, httpServletContext, "Exception Occurred", merchant, user);

    }
    map.put("lock", lock);
    return map;
  }

  public TollRecharge populateTollRecharge(TollTag tollTag, HttpServletContext httpServletContext,
      TollReqPayRequest netcReqPay, Dipcoin dcoin_obj, DipcoinTransaction dTx,
      TollRecharge tollRecharge_current_active, TollRecharge lastTagRecharge) {

    TollRecharge tollRecharge = new TollRecharge();
    tollRecharge.setClientTransactionId(netcReqPay.getTxn().getId());
    tollRecharge.setRechargeAmount(BigDecimal.ZERO);
    tollRecharge.setDipcoinId(dcoin_obj.getId());
    tollRecharge.setTagId(tollTag.getTagId());
    tollRecharge.setVehicleNo(tollTag.getRegistrationNo());
    tollRecharge.setRechargeBy(dcoin_obj.getUser().getId());
    tollRecharge.setRechargeTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
    tollRecharge.setIPAddress(httpServletContext.getOriginIp());
    tollRecharge.setAvailableAmount(
        netcReqPay.getTxn().getType().equalsIgnoreCase(TollConstant.REQPAY_TYPE_DEBIT)
            || netcReqPay.getTxn().getType().equalsIgnoreCase(TollConstant.REQPAY_TYPE_DEBIT_ADVICE)
                ? lastTagRecharge.getAvailableAmount()
                    .subtract(netcReqPay.getPayer().getAmount().getValue())
                : tollRecharge_current_active.getAvailableAmount()
                    .add(netcReqPay.getPayer().getAmount().getValue()));
    tollRecharge.setUsedAmount(netcReqPay.getPayer().getAmount().getValue());
    tollRecharge.setStatus(String.valueOf(DBConstants.TollRechargeStatus.ACTIVE.value()));
    tollRecharge.setDipcoinTransactionRefId(dTx.getDipcoinTransactionRefId());
    tollRecharge.setReferenceId(tollRecharge_current_active.getId());
    tollRecharge.setCustomerAccountId(lastTagRecharge.getCustomerAccountId());
    return tollRecharge;
  }

  public boolean sendResponsePay(HttpServletContext httpServletContext,
      TollReqPayRequest netcReqPay, String errorCodes, Bank bank) {

    try {


      if (!tollProperties.isConnectNpci()) {
        return false;
      }

      TollResPayResponse tollResPayResponse = new TollResPayResponse();

      Head head = new Head();

      Txn txn = new Txn();

      RiskScores riskScores = new RiskScores();

      Score score = new Score();

      Resp resp = new Resp();

      Ref ref = new Ref();

      List<Score> scoreList = new ArrayList<>();

      head.setMsgId(netcReqPay.getHead().getMsgId());
      head.setOrgId(bank != null ? bank.getOrgId() : netcReqPay.getHead().getOrgId());
      head.setTs(netcReqPay.getHead().getTs());
      head.setVer(netcReqPay.getHead().getVer());

      txn.setId(netcReqPay.getTxn().getId());
      txn.setNote(netcReqPay.getTxn().getNote());
      txn.setOrgTxnId(netcReqPay.getTxn().getOrgTxnId());
      txn.setRefId(netcReqPay.getTxn().getRefId());
      txn.setRefUrl(netcReqPay.getTxn().getRefUrl());
      txn.setTs(netcReqPay.getTxn().getTs());
      txn.setType(netcReqPay.getTxn().getType());



      if (netcReqPay.getTxn().getRiscScores() != null
          && CollectionUtils.isNotEmpty(netcReqPay.getTxn().getRiscScores().getScore())) {
        for (int index = 0; index < netcReqPay.getTxn().getRiscScores().getScore()
            .size(); index++) {

          score
              .setProvider(netcReqPay.getTxn().getRiscScores().getScore().get(index).getProvider());
          score.setType(netcReqPay.getTxn().getRiscScores().getScore().get(index).getType());
          score.setValue(netcReqPay.getTxn().getRiscScores().getScore().get(index).getValue());
          scoreList.add(score);

        }
      }

      riskScores.setScore(scoreList);

      txn.setRiscScores(riskScores);

      resp.setMerchantId(netcReqPay.getMerchant().getId());
      resp.setRespCode(TollConstant.SUCCESS_RESPONSE);
      resp.setResult(TollConstant.RESULT_ACCEPTED);
      resp.setTs(netcReqPay.getTxn().getTs());

      ref.setAccType(TollConstant.EMPTY);
      ref.setAddr(netcReqPay.getPayer().getAddr());
      ref.setApprovalNum(TollConstant.APPROVAL_NUMBER);
      ref.setAvalBal(TollConstant.EMPTY);
      ref.setCustomerName(TollConstant.EMPTY);

      // this is used at the time of compliance test check by npci
      if (!Boolean.parseBoolean(tollProperties.getComplianceTestCheck400())) {
        ref.setErrCode(errorCodes);
      }
      ref.setLedgerBal(TollConstant.EMPTY);
      ref.setMaskedAccountNumber(TollConstant.EMPTY);
      ref.setSettAmount(String.valueOf(netcReqPay.getPayer().getAmount().getValue()));
      ref.setSettCurrency(netcReqPay.getPayer().getAmount().getCurr());
      ref.setType(TollConstant.PAYER_REF_TYPE);

      resp.setRef(ref);

      tollResPayResponse.setHead(head);
      if (netcReqPay.getMeta() != null) {
        tollResPayResponse.setMeta(netcReqPay.getMeta());
      }
      tollResPayResponse.setTxn(txn);
      tollResPayResponse.setResp(resp);

      // Create JAXB Context
      JAXBContext jaxbContext = JAXBContext.newInstance(TollResPayResponse.class);

      // Create Marshaller
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

      // Required formatting??
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

      // Print XML String to Console
      StringWriter sw = new StringWriter();

      // Write XML to StringWriter
      jaxbMarshaller.marshal(tollResPayResponse, sw);

      // Verify XML Content
      String postData = sw.toString();

      ByteArrayInputStream byteArrayInputStream =
          new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

      ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices
          .signatureGenerationServices(byteArrayInputStream, httpServletContext.getTraceId(),
              bank != null ? bank.getOrgId() : netcReqPay.getHead().getOrgId().toLowerCase());

      String responseData = null;
      
    //NPCI Active Active Setup Phase2 changes
      String ipAddress = null;
	try {
		ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}        
      if(StringUtils.isEmpty(ipAddress)) {
    	  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
    	            .message("******* NPCI Server is Down ******").format());
  		return false;

      }
      int port = this.tollProperties.getNetcHealthCheckPort();
	  String endPoint = this.tollProperties.getResponsePayServiceUrl();
      String url = "https://" + ipAddress + ":" + port + endPoint;
    
      try {
        TollHttpsServices.bankIin = bank != null ? bank.getIin() : StringUtils.EMPTY;

        // this is used at the time of compliance test check by npci
        if (Boolean.parseBoolean(tollProperties.getComplianceTestCheck408())) {
          Thread.sleep(70000);
        }
        
		/*
		 * responseData =
		 * tollHttpsServices.send(tollProperties.getResponsePayServiceUrl(),
		 * httpServletContext.getTraceId(), byteArrayOutputStream);
		 */
        
        responseData = tollHttpsServices.send(url,
                httpServletContext.getTraceId(), byteArrayOutputStream);
        
        TollHttpsServices.bankIin = StringUtils.EMPTY;
      } catch (Exception e) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Exception Caught while calling tollHttpsServices").format(), e);
      }

    } catch (JAXBException e) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Exception Caught in send Response pay method").format(), e);
    }
    return true;
  }

  public String validateReqPay(String errorCodes, TollReqPayRequest netcReqPay, Dipcoin dcoin,
      TollTag tollTag, HttpServletContext httpServletContext, Bank bank) {
    try {


      boolean avcValid = false;
      boolean mapperVCValid = false;


//      if (StringUtils
//          .isNotBlank(netcReqPay.getMerchant().getReaderVerificationResult().getSignData())) {
//
//        try {
//          if (!tollSignatureVerificationServices.verifyEcdsaSignedData(
//              netcReqPay.getVehicle().getTID().concat(netcReqPay.getVehicle().getTagId()),
//              netcReqPay.getMerchant().getReaderVerificationResult().getSignData(),
//              tollSignatureVerificationServices.getPublicKey(httpServletContext.getTraceId(),
//                  bank.getAlias()))
//              &&
//              
//              !tollSignatureVerificationServices.verifyEcdsaSignedData(
//                  netcReqPay.getVehicle().getTagId().concat(netcReqPay.getVehicle().getTID()),
//                  netcReqPay.getMerchant().getReaderVerificationResult().getSignData(),
//                  tollSignatureVerificationServices.getPublicKey(httpServletContext.getTraceId(),
//                      bank.getAlias()))) {
//            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                .message("Tag sign verification").data("verificationResult", false)
//                .data("error code sent ", TollErrorCodes.Error_code_INVALID_TAG_SIGNATURE.code())
//                .format());
//
//            tollTag.setExcCode(TollConstant.EXC_CODE_BLACKLIST);
//            tollTag.setExcCodeUpdateTime(String.valueOf(DateTime.now().getMillis()));
//            tollTag = tollDBService.updateTollTag(tollTag);
//            return TollErrorCodes.Error_code_INVALID_TAG_SIGNATURE.code();
//          }
//        } catch (Exception e) {
//          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//              .message("Tag sign verification exception")
//              .data("errorCode set ", TollErrorCodes.Error_code_INVALID_TAG_SIGNATURE.code())
//              .format());
//          return TollErrorCodes.Error_code_INVALID_TAG_SIGNATURE.code();
//        }
//      }


      for (String vehicleAvc : TollConstant.VEHICLE_AVC) {
        if (netcReqPay.getVehicle().getAvc().equalsIgnoreCase(vehicleAvc)) {
          avcValid = true;
        }

        for (Detail detail : netcReqPay.getVehicle().getVehicleDetails()
            .get(NumberUtils.INTEGER_ZERO).getDetail()) {
          if (detail.getName().equalsIgnoreCase(TollConstant.VEHICLECLASS)
              && detail.getValue().equalsIgnoreCase(vehicleAvc)) {
            mapperVCValid = true;
          }
        }
      }

      if (!avcValid && mapperVCValid) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Avc Is Not valid but mapper vc is valid")
            .data("avc", netcReqPay.getVehicle().getAvc()).format());
        return TollErrorCodes.errorCode_AVC_IS_INVALID_BUT_MAPPER_VC_IS_VALID.code();
      }

      if (avcValid && mapperVCValid
          && !netcReqPay.getVehicle().getAvc().equals(tollTag.getCategory())) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("tollTag avc")
            .data("tollTag Category", tollTag.getCategory()).format());
        return TollErrorCodes.Error_code_VEHICLE_CLASS_AVC_IS_NOT_EQUAL_TO_MAPPER_VEHICLE_CLASS_
            .code();
      }

      SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
      Date txnTsDate;

      txnTsDate = simpleDateFormat.parse(netcReqPay.getTxn().getTs());
      Date readerTsDate = simpleDateFormat
          .parse(netcReqPay.getMerchant().getReaderVerificationResult().getTsRead());
      Long readerTsTimeDiff = txnTsDate.getTime() - readerTsDate.getTime();

      if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)
          && ((txnTsDate.getTime() - Long
              .parseLong(tollTag.getExcCodeUpdateTime()) >= TollConstant.EXCCODE_UPDATE_DIFFERENCE))
          && (readerTsTimeDiff / 60000) < NumberUtils.INTEGER_ONE) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("ExcCode  update time").data("ExcCode", tollTag.getExcCode())
            .data("update time", tollTag.getExcCodeUpdateTime()).format());



        try {

          // success sms
          if (!applicationProperties.getAwsSMSClient()
              && !smsClient
                  .sendSms(
                      tollTag.getTollRegistration().getMobileNo(), Templates.TollTagLowBalance
                          .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                      true)) {
            LOG.error(
                LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
                    .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
          }

          if (applicationProperties.getAwsSMSClient()) {

            NotificationRequestContext notificationRequestContext =
                new NotificationRequestContext();
            notificationRequestContext.setTraceId(httpServletContext.getTraceId());
            if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                Templates.TollTagLowBalance
                    .format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
                httpServletContext.getClientFeatureFlags().smsEnabled(),
                notificationRequestContext)) {

              LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("Failed to send SMS")
                  .data("phone", tollTag.getTollRegistration().getMobileNo()).format());

            }
          }

          // Email
          this.tollEmailUtils.setHttpServletContext(httpServletContext);
          if (!this.tollEmailUtils.tollTagLowBalance(tollTag.getRegistrationNo(), bank,
              tollTag.getTollRegistration())) {
            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Failed to send email to set tag low balance.")
                .data("user email", tollTag.getTollRegistration().getEmailId()).format());
          }

        } catch (Exception e) {
          LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Exception in sending email & sms to Customer.").format());
        }

        return TollErrorCodes.Error_code_CONDITION_APPLIED_READERREADTIME_TIME_OF_UPDATING_TAG_ID_IN_LOW_BAL_EX_LIST_20_MINUTES_TAG_IN_LOW_BALANCE_EXCEPTION_LIST
            .code();
      }


      if (readerTsTimeDiff >= TollConstant.TXN_TS_DIFFERENCE
          && !tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("TXN Time Diff")
            .data("txn time diff in min", readerTsTimeDiff / 60000)
            .data("expected ref error code",
                TollErrorCodes.Error_code_TRANSACTION_WITH_ACQ_LIABILITY_BUT_CUSTOMER_IS_HAVING_SUFFICIENT_BALANCE_IN_TAG_LINKED_ACCOUNT
                    .code())
            .data("sent ref error code ",
                TollErrorCodes.Error_code_TRANSACTION_WITH_ACQ_LIABILITY_BUT_CUSTOMER_IS_HAVING_SUFFICIENT_BALANCE_IN_TAG_LINKED_ACCOUNT
                    .code())
            .format());
        return TollErrorCodes.Error_code_TRANSACTION_WITH_ACQ_LIABILITY_BUT_CUSTOMER_IS_HAVING_SUFFICIENT_BALANCE_IN_TAG_LINKED_ACCOUNT
            .code();
      }

      if (readerTsTimeDiff >= TollConstant.TXN_TS_DIFFERENCE
          && tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("dcoin.getAmount more than fastag osta amount")
            .data("getAmount", dcoin.getAmount())
            .data("txn time diff in min", readerTsTimeDiff / 60000).format());
        return TollErrorCodes.Error_code_TRANSACTION_WITH_ACQ_LIABILITY_BUT_CUSTOMER_IS_NOT_HAVING_SUFFICIENT_BALANCE_IN_TAG_LINKED_ACCOUNT
            .code();

      }

      if (netcReqPay.getMerchant().getReaderVerificationResult().getSignAuth()
          .equalsIgnoreCase(TollConstant.SIGN_AUTH_UNKNOWN)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("SIGN AUTH UNKNOWN")
            .data("sign auth", netcReqPay.getMerchant().getReaderVerificationResult().getSignAuth())
            .format());
      }

      if (readerTsTimeDiff <= TollConstant.TXN_TS_DIFFERENCE) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("TXN TIME DIFF")
            .data("txn reader time diff in min ", readerTsTimeDiff / 60000).format());
      }

      // if (StringUtils.isEmpty(netcReqPay.getPayer().getName())
      // || !netcReqPay.getPayer().getName().toUpperCase().replaceAll("\\s", "")
      // .equals(tollTag.getTollRegistration().getFirstName()
      // .concat(tollTag.getTollRegistration().getLastName()).toUpperCase()
      // .replaceAll("\\s", ""))) {
      // LOG.debug(
      // LogFormatter.instance(httpServletContext.getTraceId()).message("PAYER NAME invalid")
      // .data("npci req payer name", netcReqPay.getPayer().getName())
      // .data("osta payer name", tollTag.getTollRegistration().getFirstName()
      // .concat(tollTag.getTollRegistration().getLastName()))
      // .format());
      // }

      // if (StringUtils.isEmpty(netcReqPay.getPayee().getName())
      // || !netcReqPay.getPayee().getName().toUpperCase().replaceAll("\\s", "")
      // .equals(netcReqPay.getMerchant().getName().toUpperCase().replaceAll("\\s", ""))) {
      // LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
      // .message("PAYEE NAME invalid").data("merchant name", netcReqPay.getMerchant().getName())
      // .data("payee name", netcReqPay.getPayee().getName()).format());
      // }

      if (!netcReqPay.getVehicle().getWim().isEmpty()) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("WIM NOT EMPTY")
            .data("Wim", netcReqPay.getVehicle().getWim()).format());
      }

      // if (StringUtils.isEmpty(netcReqPay.getMerchant().getGeoCode())
      // || !netcReqPay.getMerchant().getGeoCode().contains(",")) {
      // LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
      // .message("GEOCODE IS INVALID").format());
      // } else {
      // String[] geocode = netcReqPay.getMerchant().getGeoCode().split(",");
      // if (!NumberUtils.isParsable(geocode[0]) || !NumberUtils.isParsable(geocode[1])
      // || geocode[0].split("\\.")[0].length() > 2 || geocode[0].split("\\.")[1].length() > 4
      // || geocode[1].split("\\.")[0].length() > 2 || geocode[1].split("\\.")[1].length() > 4) {
      //
      // LOG.debug(
      // LogFormatter.instance(httpServletContext.getTraceId()).message("GEOCODE IS INVALID")
      // .data("latitude", geocode[0]).data("longitude", geocode[1]).format());
      //
      // }
      // }

      if (netcReqPay.getMerchant().getLane().getDirection().isEmpty()
          || netcReqPay.getMerchant().getLane().getDirection().length() > 2) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("LANE DIRECTION INVALID")
            .data("lane direction", netcReqPay.getMerchant().getLane().getDirection()).format());
      }

      if (netcReqPay.getPayer().getAmount().getValue()
          .compareTo(BigDecimal.valueOf(2000)) > NumberUtils.INTEGER_ZERO) {

        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Payer Amount Greater than 2000")
            .data("Amount", netcReqPay.getPayer().getAmount().getValue()).format());
        return TollErrorCodes.errorCode_TRANSACTION_AMOUNT.code();
      }


      if (netcReqPay.getPayer().getAmount().getValue()
          .compareTo(BigDecimal.valueOf(0)) < NumberUtils.INTEGER_ZERO
          && netcReqPay.getTxn().getType().equalsIgnoreCase(TollConstant.REQPAY_TYPE_DEBIT)) {
        LOG.debug(
            LogFormatter.instance(httpServletContext.getTraceId()).message("Negative Debit Request")
                .data("Amount", netcReqPay.getPayer().getAmount().getValue()).format());

        return TollErrorCodes.errorcode_NEGATIVE_DEBIT_REQUEST.code();
      }


      Map<String, String> detailMap = new HashMap<String, String>();

      for (Detail detail : netcReqPay.getVehicle().getVehicleDetails().get(NumberUtils.INTEGER_ZERO)
          .getDetail()) {
        detailMap.put(detail.getName(), detail.getValue());
      }

      if (detailMap.get(TollConstant.EXCCODE).equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST)
          || tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Blacklisted tag")
            .data("errorCode", TollErrorCodes.errorcode_BLACKLISTED_TAG.code()).format());

        return TollErrorCodes.errorcode_BLACKLISTED_TAG.code();
      }
      
      if (detailMap.get(TollConstant.EXCCODE).equalsIgnoreCase(TollConstant.EXC_CODE_HOTLIST)
          || tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_HOTLIST)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Hotlisted tag")
            .data("errorCode", TollErrorCodes.errorcode_HOTLISTED_TAG.code()).format());

        return TollErrorCodes.errorcode_HOTLISTED_TAG.code();
      }

      if (detailMap.get(TollConstant.EXCCODE)
          .equalsIgnoreCase(TollConstant.EXC_CODE_CLOSED_OR_REPLACED)
          || tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_CLOSED_OR_REPLACED)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Closed Or Replaced tag")
            .data("errorCode", TollErrorCodes.errorcode_CLOSED_OR_REPLACED_TAG.code()).format());

        return TollErrorCodes.errorcode_CLOSED_OR_REPLACED_TAG.code();
      }

      if (!checkVehicleRegistrationNo(
          Base64.getDecoder().decode(detailMap.get(TollConstant.REGNUMBER)),
          tollTag.getRegistrationNo(), bank, httpServletContext)) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("invalid vehicle registration number")
            .data("errorCode", TollErrorCodes.Error_code_INVALID_VEHICLE_REGISTRATION_NUMBER.code())
            .format());
        return TollErrorCodes.Error_code_INVALID_VEHICLE_REGISTRATION_NUMBER.code();
      }


    } catch (Exception e) {
      e.printStackTrace();
    }

    return TollErrorCodes.Error_code_SUCCESS.code();

  }


  public boolean creditCustomerAccount(TollReqPayRequest netcReqPay, Merchant merchant,
      HttpServletContext httpServletContext, TollTag tollTag, User user) {
    try {

      LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Credit customer Account using Merchant Cancellation for")
          .data("partner txn reference id", netcReqPay.getTxn().getOrgTxnId()).format());

      List<DipcoinTransaction> dTxs = dipcoinDBService.asyncGetMerchantTransactions(merchant,
          Arrays.asList(netcReqPay.getTxn().getOrgTxnId()), null, null).get();


      BigDecimal cancelAmount = netcReqPay.getPayer().getAmount().getValue();
      if (CollectionUtils.isNotEmpty(dTxs)) {

        LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Original DTX Entry").data("dtx Amount ", dTxs.get(0).getAmount())
            .data("dtx Id ", dTxs.get(0).getId()).format());

        // run for one time only
        for (DipcoinTransaction dtx : dTxs) {

          List<DipcoinTransaction> dcoinTxs =
              dipcoinDBService.asyncFindByOrderId(dtx.getOrderId()).get();

          if (CollectionUtils.isNotEmpty(dcoinTxs)) {

            for (DipcoinTransaction dcoinTx : dcoinTxs) {
              if (dcoinTx.getType() == DBConstants.DipcoinTransactionType.CANCELLED_BY_MERCHANT
                  .value()
                  || dcoinTx
                      .getType() == DBConstants.DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT
                          .value()) {

                cancelAmount = cancelAmount.add(dcoinTx.getAmount());
              }

            }

            if (cancelAmount.compareTo(dtx.getAmount()) <= NumberUtils.INTEGER_ZERO) {

              Dipcoin dipcoin = dipcoinDBService.asyncGetById(dtx.getDipcoinId(), false).get();

              DipcoinTransaction dTx = new DipcoinTransaction();
              dTx.setType(netcReqPay.getPayer().getAmount().getValue()
                  .compareTo(dtx.getAmount()) < NumberUtils.INTEGER_ZERO
                      ? DBConstants.DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT.value()
                      : DBConstants.DipcoinTransactionType.CANCELLED_BY_MERCHANT.value());
              dTx.setDipcoinTransactionRefId(CoreUtils.generateDipcoinToMerchantReferenceNumber());
              dTx.setUpdateDate(String.valueOf(DateTime.now().getMillis()));
              dTx.setRequestTime(String.valueOf(DateTime.now().getMillis()));
              dTx.setResponseTime(String.valueOf(DateTime.now().getMillis()));
              dTx.setAmount(netcReqPay.getPayer().getAmount().getValue());
              dTx.setDipcoinId(dtx.getDipcoinId());
              dTx.setComments("Credited Successfully");
              dTx.setCustomerAccountId(dtx.getCustomerAccountId());
              dTx.setDCResponseCode(dtx.getDCResponseCode());
              dTx.setDCResponseDesc("Credited Successfully");
              dTx.setGeoLocation(dtx.getGeoLocation());
              dTx.setIPAddress(dtx.getIPAddress());
              dTx.setOrderId(dtx.getOrderId());
              dTx.setUser(dtx.getUser());
              dTx.setPartnerRawRequest(objectMapper.writeValueAsString(netcReqPay));
              dTx.setPartnerReferenceId(dtx.getPartnerReferenceId());
              dTx.setPartnerTransactionReferenceId(netcReqPay.getTxn().getId());
              dTx.setSettlementDone(DipcoinTransactionSettlementDone.DEFAULT.value());
              dTx.setSource(dtx.getSource());
              dTx.setStatus(dtx.getStatus());
              if (dipcoinDBService.addTransaction(dTx) == null) {
                LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message(
                    "Merchant cancellation entry not added in Dtx for crediting amount to customer account ")
                    .format());
              }

              dipcoin.setStatus(DipcoinStatus.PROCESSED_DISPUTED.value());
              if (dipcoinDBService.updateCoin(dipcoin) == null) {
                LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Fail to Update Dipcoin.").format());
              }

            } else {
              addDtxEntry(tollTag, netcReqPay, httpServletContext,
                  "Credit Amount Is Greater Than Debited Amount", merchant, user);
            }

          } else {
            addDtxEntry(tollTag, netcReqPay, httpServletContext, "Debited entry Not Found",
                merchant, user);
          }
          break;
        }
      }
    } catch (Exception e) {
      LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught")
          .format(), e);
    }
    return true;
  }

  public boolean checkVehicleRegistrationNo(byte[] encryptedData, String vehicleRegistrationNo,
      Bank bank, HttpServletContext httpServletContext) throws Exception {


    byte[] decryptedData = decrypt(tollSignatureVerificationServices
        .getPrivateKey(httpServletContext.getTraceId(), bank.getOrgId()), encryptedData);

    LOG.debug(
        LogFormatter.instance(httpServletContext.getTraceId()).message("Decrypted Vehicle Number")
            .data("Vehicle Number", new String(decryptedData)).format());

    if (vehicleRegistrationNo.equalsIgnoreCase(new String(decryptedData))) {
      return true;
    }
    return false;
  }

  public byte[] decrypt(PrivateKey key, byte[] inputData) throws Exception {

    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, key);

    byte[] decryptedBytes = cipher.doFinal(inputData);

    return decryptedBytes;
  }


  public String encrypt(PublicKey key, byte[] inputData) {
    try {
      Cipher rsa;
      rsa = Cipher.getInstance("RSA");
      rsa.init(Cipher.ENCRYPT_MODE, key);
      byte[] utf8 = rsa.doFinal(inputData);
      return Base64.getEncoder().encodeToString(utf8);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public boolean addDtxEntry(TollTag tollTag, TollReqPayRequest tollReqPayRequest,
      HttpServletContext httpServletContext, String message, Merchant merchant, User user) {

    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("Adding DtxEntry for " + message).format());

    try {
      DipcoinTransaction dTx = new DipcoinTransaction();

      dTx.setCustomerAccountId(
          tollTag != null ? tollTag.getCustomerAccountId() : NumberUtils.INTEGER_ZERO);
      dTx.setAmount(tollReqPayRequest != null ? tollReqPayRequest.getPayer().getAmount().getValue()
          : BigDecimal.ZERO);
      dTx.setComments(message.length() > 50 ? message.substring(0, 50) : message);
      dTx.setDCResponseCode(String.valueOf(HttpStatus.BAD_REQUEST.value()));
      dTx.setDCResponseDesc(message);
      dTx.setDipcoinId(NumberUtils.INTEGER_ZERO);
      dTx.setDipcoinTransactionRefId(CoreUtils.generateDipcoinToMerchantReferenceNumber());
      dTx.setIPAddress(httpServletContext.getOriginIp());
      dTx.setOrderId(
          tollReqPayRequest != null
              ? tollReqPayRequest.getVehicle().getTagId()
                  + String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis())
              : null);
      dTx.setPartnerRawRequest(
          tollReqPayRequest != null ? objectMapper.writeValueAsString(tollReqPayRequest) : null);
      dTx.setPartnerReferenceId(merchant != null ? merchant.getReferenceId() : null);
      dTx.setPartnerTransactionReferenceId(
          tollReqPayRequest != null ? tollReqPayRequest.getTxn().getId() : null);
      dTx.setRequestTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
      dTx.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
      dTx.setSettlementDone(DipcoinTransactionSettlementDone.DEFAULT.value());
      dTx.setSource(DBConstants.TransactionSource.TOLL.value());
      dTx.setStatus(DBConstants.DipcoinTransactionsStatus.SUCCESS.value());
      dTx.setType(message.equalsIgnoreCase(TollErrorCodes.errorCode_CLONED_TRANSACTION_AS_PER_THE_DEFINITION_OF_NEAR_TIME
              .message()) ? DipcoinTransactionType.CLONE_TRANSACTION.value()
      : tollReqPayRequest != null && TollConstant.REQPAY_TYPE_NON_FIN.equalsIgnoreCase(tollReqPayRequest.getTxn().getType()) 
      ? DipcoinTransactionType.NON_FIN.value()
    		  : DipcoinTransactionType.DEEMED_ACCEPTED.value());
      dTx.setUpdateDate(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
      dTx.setUsageDetails(
          tollReqPayRequest != null
              ? tollReqPayRequest.getVehicle().getVehicleRegNo() + ", "
                  + tollReqPayRequest.getMerchant().getName() + "," + " Lane Direction "
                  + tollReqPayRequest.getMerchant().getLane().getDirection()
              : null);
      dTx.setUser(user);


      if (merchant != null) {

        List<DipcoinTransaction> dtXs =
            dipcoinDBService.getPartnerTransactions(merchant.getReferenceId(),
                Arrays.asList(tollReqPayRequest.getTxn().getId()), null, null, null, null);
        if (CollectionUtils.isNotEmpty(dtXs)) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Found Dtx Entry So Not Added").data("dtXs Id", dtXs.get(0).getId())
              .data("Dtx TxnId", dtXs.get(0).getPartnerTransactionReferenceId()).format());
          return true;
        }
      }
      if (dipcoinDBService.addTransaction(dTx) == null) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Failed To Add dtx Entry For Failed or Exception in Reqpay Txn")
            .data("Dtx Id", dTx.getId()).data("Dtx TxnId", dTx.getPartnerTransactionReferenceId())
            .format());
      }

      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Added Dtx Entry For " + message).data("Dtx Id", dTx.getId())
          .data("Dtx TxnId", dTx.getPartnerTransactionReferenceId()).format());

    } catch (Exception e) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Exception caught while adding failed DTx Entry").format(), e);
    }
    return true;
  }
  
  public TollTagUpdateResponse uploadTheTollListToNETC(final HttpServletContext httpServletContext,
			List<TollTag> tollTagRequests, String operation, Bank bank, Merchant merchant)
			throws APIException, Exception {

		// For the time being return true response.
	    TollTagUpdateResponse tollTagUpdateResponse = new TollTagUpdateResponse();
		TollTagUpdateRequest tollTagUpdateRequest = new TollTagUpdateRequest();
		String refUrl = StringUtils.EMPTY;

		TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();
		if (tollProperties.isConnectNpci()) {
			brontooResource.setHttpServletContext(httpServletContext);
			ResponseEntity responseEntity = this.brontooResource.syncTime(bank);
			if (responseEntity.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				return null;
			}
			tollNetcSyncTimeResponse = (TollNetcSyncTimeResponse) responseEntity.getBody();
		}

		String[] bankInfos = tollProperties.getBankInfo().split(",");

		for (String bankInfo : bankInfos) {
			String[] info = bankInfo.split("~");
			if (info[2].equalsIgnoreCase(bank.getIin())) {
				refUrl = info[3];
			}
		}

		Head head = new Head();
		Txn txn = new Txn();
		TagList tagList = new TagList();
		List<Tag> tags = new ArrayList<>();

		// TODO after NETC
		head.setVer(TollConstant.VERSION);
		SimpleDateFormat tsformatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
		Date date = tsformatter.parse(tollNetcSyncTimeResponse.getResp().getTs());
		head.setTs(tollNetcSyncTimeResponse.getResp().getTs());

		head.setOrgId(bank.getOrgId());

		SimpleDateFormat msgformatter = new SimpleDateFormat(TollConstant.MSG_DATE_FORMAT);

		head.setMsgId(bank.getOrgId() + msgformatter.format(date).toUpperCase());

		SimpleDateFormat issueDateformatter = new SimpleDateFormat(TollConstant.TAG_ISSUE_DATE_FORMAT);

		for (TollTag tollTagRequest : tollTagRequests) {

			// Catgory mapping ================
			// we can add rest vehicle in else if --accordingly.
			String mappedVehicleClass = StringUtils.EMPTY;
			if (TollConstant.VEHICLE_CLASS_6.equalsIgnoreCase(tollTagRequest.getCategory())) {
				mappedVehicleClass = tollProperties.getMappingOfVehicleVC6();
			} else {
				mappedVehicleClass = tollTagRequest.getCategory();
			}
			// =====================
			List<Detail> detailList = new ArrayList<>();
			Tag tag = new Tag();
			Detail detail = new Detail();

			if (operation.equalsIgnoreCase(TollConstant.ADD_OP)) {

				detail.setName(TollConstant.TID);
				detail.setValue(tollTagRequest.getTid());
				detailList.add(detail);

				detail = new Detail();
				detail.setName(TollConstant.ISSUEDATE);
				detail.setValue(issueDateformatter.format(date));
				detailList.add(detail);

				detail = new Detail();
				detail.setName(TollConstant.EXCCODE);
				detail.setValue(tollTagRequest.getExcCode());
				detailList.add(detail);
			}

			detail = new Detail();
			detail.setName(TollConstant.VEHICLECLASS);
			detail.setValue(mappedVehicleClass);
			detailList.add(detail);

			detail = new Detail();
			detail.setName(TollConstant.REGNUMBER);
			detail.setValue(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
					? tollTagRequest.getRegistrationNo()
					: (tollTagRequest.getVinNumber().length() <= 20 ? tollTagRequest.getVinNumber()
							: tollTagRequest.getVinNumber().substring(0, 19)));
			detailList.add(detail);

			detail = new Detail();
			detail.setName(TollConstant.COMVEHICLE);
			detail.setValue(tollTagRequest.getIsCommercial());
			detailList.add(detail);

			tag.setDetail(detailList);
			tag.setOp(operation);
			tag.setTagId(tollTagRequest.getTagId());
			tag.setSeqNum(String.valueOf(tollTagRequests.indexOf(tollTagRequest) + NumberUtils.INTEGER_ONE));
			tag.setType(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
					? (operation.equalsIgnoreCase(TollConstant.ADD_OP) ? operation : TollConstant.UPDATE_OP)
					: tollTagRequest.getVinNumber());

			tags.add(tag);

		}
		tagList.setTag(tags);
		txn.setTagList(tagList);

		String dipcoinReferenceNumber = CoreUtils.randomAlphaString(22);

		txn.setId(dipcoinReferenceNumber);
		txn.setNote(operation);
		txn.setOrgTxnId(TollConstant.EMPTY);
		txn.setRefId(TollConstant.EMPTY);
		txn.setRefUrl(refUrl);
		txn.setType(TollConstant.MANAGE_TXN_TYPE);
		txn.setTs(tollNetcSyncTimeResponse.getResp().getTs());
		tollTagUpdateRequest.setHead(head);
		tollTagUpdateRequest.setTxn(txn);

		// Create JAXB Context
		JAXBContext jaxbContext = JAXBContext.newInstance(TollTagUpdateRequest.class);

		// Create Marshaller
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// Required formatting??
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// Print XML String to Console
		StringWriter sw = new StringWriter();

		// Write XML to StringWriter
		jaxbMarshaller.marshal(tollTagUpdateRequest, sw);

		// Verify XML Content
		String postData = sw.toString();

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

		ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices
				.signatureGenerationServices(byteArrayInputStream, httpServletContext.getTraceId(), bank.getOrgId());
		String responseData = null;
		
		//NPCI Active Active Setup Phase2 changes
	      String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());        
	      if(StringUtils.isEmpty(ipAddress)) {
	    	  
	    	  tollTagUpdateResponse.addHeaderCode(HeaderCode.NETC_NPCI_SERVER_DOWN);
	    	  return tollTagUpdateResponse;

	      }
	      int port = this.tollProperties.getNetcHealthCheckPort();
		  String endPoint = this.tollProperties.getManageTagsUrl();
	      String url = "https://" + ipAddress + ":" + port + endPoint;
	    
		try {
			TollHttpsServices.bankIin = bank.getIin();
			
			/*
			 * responseData = tollHttpsServices.send(tollProperties.getManageTagsUrl(),
			 * httpServletContext.getTraceId(), byteArrayOutputStream);
			 */
			
			responseData = tollHttpsServices.send(url, httpServletContext.getTraceId(),
					byteArrayOutputStream);


			TollHttpsServices.bankIin = StringUtils.EMPTY;
			if (responseData == null) {
				return null;
			}

		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught").format(), e);
		}

		/*
		 * TollTagUpdateResponse tollTagUpdateResponse = JAXB.unmarshal(new
		 * StringReader(responseData), TollTagUpdateResponse.class);
		 */
	    tollTagUpdateResponse = JAXB.unmarshal(new StringReader(responseData),
				TollTagUpdateResponse.class);

		return tollTagUpdateResponse;

	}
  
  
  
	public void exceptionList(final String exceptionListResponse, String traceId) {

		if (StringUtils.isEmpty(exceptionListResponse)) {
			LOG.debug(LogFormatter.instance(traceId).message("Response Is empty").format());
			return;
		}

		TollGetExceptionListResponse tollGetExceptionListResponse = JAXB
				.unmarshal(new StringReader(exceptionListResponse), TollGetExceptionListResponse.class);

		LOG.debug(LogFormatter.instance(traceId).message("Exception List")
				.data("size", tollGetExceptionListResponse.getTxn().getResp().getTollException().size())
				.data("Tagsize",
						tollGetExceptionListResponse.getTxn().getResp().getTollException().get(0).getTag().size())
				.data("data", tollGetExceptionListResponse.getTxn().getResp().getTollException().get(0).getTag().get(0))
				.format());

		Integer count = 3000;
		Integer start = 0;

		while (true) {
			LOG.debug(LogFormatter.instance(traceId).message("Exception list updation").data("start", start)
					.data("count", count).format());
			List<TollTag> tollTags = tollDBService.findAll(count, start);
			if (CollectionUtils.isEmpty(tollTags)) {
				LOG.debug(LogFormatter.instance(traceId).message("End of Exception List traversal").format());
				break;
			}
			start += tollTags.size();

			Map<String, TollTag> mp = new HashMap<>();

			for (TollTag tollTag : tollTags) {
				if (StringUtils.isNotBlank(tollTag.getTagId())) {
					mp.put(tollTag.getTagId(), tollTag);
				}
			}

			for (TollException tollException : tollGetExceptionListResponse.getTxn().getResp().getTollException()) {

				for (Tag tag : tollException.getTag()) {
					TollTag tollTag = mp.get(tag.getTagId());
					if (tollTag != null) {
						LOG.debug(LogFormatter.instance(traceId).message("Exception List updated")
								.data("tagId", tollTag.getTagId()).format());
						tollTag.setExcCode(tollException.getExcCode());
						tollTag.setExcCodeUpdateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
						tollDBService.updateTollTag(tollTag);
					}
				}
			}
			mp.clear();
		}

	}
	

	public ResponseEntity setvahanResponse(String vahanResponse, int type) {

      LOG.debug(LogFormatter.instance().message("Vahan Response from NPCI server ").format());
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .data("vahanResponse", vahanResponse).format());

      try {

          /*
           * tollSignatureVerificationServices.verifySignature(httpServletContext.
           * getTraceId(), vahanResponse, "npci");
           * 
           * if (!TollSignatureVerificationServices.verificationResult &&
           * tollProperties.isConnectNpci()) {
           * 
           * LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
           * .message(TollErrorCodes.errorCode_INCORRECT_SIGNATURE_AUTHENTICATION_VALUE.
           * message()).format());
           * 
           * return ResponseEntity.status(HttpStatus.BAD_REQUEST)
           * .body(APIResponse.error(HeaderCode.SIGNATURE_IS__INVALID)); }
           */
        
        
          VahanInfoRequest vahanInfoRequest = JAXB.unmarshal(new StringReader(vahanResponse), VahanInfoRequest.class);

          if (vahanInfoRequest == null) {
              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("response is null")
                      .data("vahanInfoResponse", vahanInfoRequest).format());
          }
          
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("response is not null")
                  .data("vahanInfoResponse", vahanInfoRequest).format());

          VahanInfo vahanInfo = new VahanInfo();
          VahanInfoAudit vahanInfoAudit = new VahanInfoAudit();
          TollTag tollTag = new TollTag();
          Boolean isForceClose = false;
          
          if(vahanInfoRequest.getTxn().getType().equals(TollConstant.NETCResponseType.FORCE_CLOSE.type())) {
              isForceClose = true;
          }
          
          if(TollConstant.NETCResponseType.DECLINE.code() == type) {
              
              tollTag = tollDBService.findTollCustomersByTagId(vahanInfoRequest.getTxn().getError().getTagId());
              tollTag.setAddTagResponse(vahanResponse);
              vahanInfo.setErrorCode(vahanInfoRequest.getTxn().getError().getErrCode());
              vahanInfo.setTollTagId(tollTag.getId());
              vahanInfo.setTagId(vahanInfoRequest.getTxn().getError().getTagId());
              
          }
          else {


            int index = NumberUtils.INTEGER_ZERO;

            if (type == 0 && vahanInfoRequest.getTxn().getType()
                .equals(TollConstant.NETCResponseType.NOTIFICATION.type())
            /* TollConstant.NETCResponseType.NOTIFICATION.value() == type */ ) {
              index = NumberUtils.INTEGER_ONE;
            }

            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .data("Type is request: ", vahanInfoRequest.getTxn().getType()).format());

            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .data("Type is equal: ", vahanInfoRequest.getTxn().getType()
                    .equals(TollConstant.NETCResponseType.FORCE_CLOSE.type()))
                .format());
            // for new change req of vahan
            if (vahanInfoRequest.getTxn().getType()
                .equals(TollConstant.NETCResponseType.FORCE_CLOSE.type())) {
              tollTag = tollDBService.findTollCustomersByTagId(
                  vahanInfoRequest.getTxn().getTag().getImpactedTagId());
              tollTag.setIsForceClose(DBConstants.IsForceCloseStatus.FORCECLOSETAG.value());
              this.tollDBService.updateTollTag(tollTag);
              
            } else {
              tollTag = tollDBService.findTollCustomersByTagId(vahanInfoRequest.getTxn()
                  .getVehicle().getVehicleDetails().get(index).getTagId());
            }
            
            if (vahanInfoRequest.getTxn().getType()
                    .equals(TollConstant.NETCResponseType.FORCE_CLOSE.type())) {
                  tollTag.setExcCode(vahanInfoRequest.getTxn().getTag().getExcCode());
                }

                // if notification is sent for force close then hit ack to npci
                if (vahanInfoRequest.getTxn().getType()
                    .equals(TollConstant.NETCResponseType.FORCE_CLOSE.type())) {
                  
                  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Before Updating force closed in db").format());
                  
                  if(this.tollDBService.updateTollTag(tollTag)==null) {
                    
                    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                        .message("Failed to update TollTag").format());
                  }

                  ResponseEntity ackResponse =
                      this.sendAckToNpci(tollTag.getBankId());
                  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("Ack Response sent to NPCI in xml: " + ackResponse.getBody()).format());

                  return ResponseEntity.status(ackResponse.getStatusCode()).body(ackResponse.getBody());
                }
            
            if (!isForceClose) {
              /*
               * tollTag =
               * tollDBService.findTollCustomersByTagId(vahanInfoRequest.getTxn().getVehicle()
               * .getVehicleDetails().get(index).getDetail().get(0).getValue());
               */
              if (tollTag == null) {
                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("TollTag for given data not found").data("TagId", vahanInfoRequest
                        .getTxn().getVehicle().getVehicleDetails().get(index).getTagId())
                    .format());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HeaderCode.TOLL_TAGS_NOT_FOUND));
              }


              for (Detail detail : vahanInfoRequest.getTxn().getVehicle().getVehicleDetails()
                  .get(index).getDetail()) {

                if (detail.getName().equalsIgnoreCase(TollConstant.REGNUMBER)) {
                  vahanInfo.setRegistrationNo(detail.getValue());
                }

                if (detail.getName().equalsIgnoreCase(TollConstant.VIN)) {
                  vahanInfo.setVin(detail.getValue());
                }

                if (detail.getName().equalsIgnoreCase(TollConstant.ENGINENO)) {
                  vahanInfo.setEngineNo(detail.getValue());
                }

              }

              vahanInfo.setTagId(vahanInfoRequest.getTxn().getVehicle().getVehicleDetails()
                  .get(index).getTagId());
              // vahanInfo.setTagId(vahanInfoRequest.getTxn().getVehicle().getVehicleDetails().get(index).getDetail().get(0).getValue());

              vahanInfo.setTollTagId(tollTag.getId());


              // vahanInfo.setCreatedOn(new Date());
              vahanInfo.setType(type);


              LOG.debug(
                  LogFormatter.instance().message("vahanInfo " + vahanInfo.toString()).format());

              // vahanInfo = vahanInfoService.save(vahanInfo);

              vahanInfoAudit.setVahanInfo(vahanInfo);
              vahanInfoAudit.setVahanInfoXmlResponse(vahanResponse.getBytes());

              if (vahanInfoRequest.getTxn().getType()
                  .equals(TollConstant.NETCResponseType.NOTIFICATION.type())) {
                tollTag.setVinNumber(vahanInfoRequest.getTxn().getVehicle().getVin());
                tollTag.setEngineNo(vahanInfoRequest.getTxn().getVehicle().getEngineNo());
              }

              // vahanInfoAudit = vahanInfoService.save(vahanInfoAudit);

              LOG.debug(LogFormatter.instance()
                  .message("vahanInfoAudit " + vahanInfoAudit.toString()).format());

              vahanInfo = this.saveVahanInfo(vahanInfo, vahanInfoAudit, tollTag);

              if (vahanInfo == null) {
                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Failed to save data in vahanInfo table")
                    .data("vahanInfoAudit", vahanInfoAudit).format());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HeaderCode.FAILED_TO_ADD_NETC_RESPONSE));
              }
            }


          }
      
                  
          /*
           * TollTag tollTag =
           * tollDBService.findTollCustomersByTagId(vahanInfoResponse.getTagId());
           * 
           * 
           * VahanInfo vahanInfo = new VahanInfo();
           * 
           * // set the response data vahanInfo.setTollTagId(tollTag.getId());
           * vahanInfo.setTagId(vahanInfoResponse.getTagId()); vahanInfo.setType(type);
           * vahanInfo.setRegistrationNo(vahanInfoResponse.getRegNumber());
           * vahanInfo.setVin(vahanInfoResponse.getVin());
           * vahanInfo.setEngineNo(vahanInfoResponse.getEngineNo());
           * vahanInfo.setErrorCode(vahanInfoResponse.getErrorCode());
           */
          // vahanInfo = this.vahanInfoService.save(vahanInfo);

          
          /*
           * if (vahanInfo == null) {
           * LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
           * .message("Failed to dump vahan Response Data").data("vahanInfo",
           * vahanInfo).format()); return ResponseEntity.status(HttpStatus.BAD_REQUEST)
           * .body(APIResponse.error(HeaderCode.FAILED_TO_ADD_NETC_RESPONSE)); }
           */

       /*         
            // Create JAXB Context 
            JAXBContext jaxbContext = JAXBContext.newInstance(VahanInfo.class);
            
            // Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            
            // Required formatting??
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            
            // Print XML String to Console 
            StringWriter sw = new StringWriter();
            
            // Write XML to StringWriter 
            jaxbMarshaller.marshal(vahanInfoRequest, sw);
            
            // Verify XML Content 
            String postData = sw.toString();
           

           ByteArrayInputStream byteArrayInputStream =
           new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

           ByteArrayOutputStream byteArrayOutputStream =
           tollSignatureGenerationServices.signatureGenerationServices(byteArrayInputStream,
           httpServletContext.getTraceId(), null);

       */
          // set vahan_info_audit data
//         VahanInfoAudit vahanInfoAudit = new VahanInfoAudit();
        //  vahanInfoAudit.setVahanInfoXmlResponse(vahanResponse.getBytes());
          

          
          
        

          return ResponseEntity.ok(HttpStatus.OK);
      } catch (Exception e) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("Exception Caught while receiving vahan response").format(), e);

          return ResponseEntity.ok(HttpStatus.INTERNAL_SERVER_ERROR);
      }
  }
	@Transactional
	public VahanInfo saveVahanInfo(VahanInfo vahanInfo, VahanInfoAudit vahanInfoAudit, TollTag tollTag) {

		vahanInfo = this.vahanInfoService.save(vahanInfo);
		vahanInfoAudit.setVahanInfo(vahanInfo);
		vahanInfoAudit = this.vahanInfoService.save(vahanInfoAudit);
		tollTag = this.tollDBService.updateTollTag(tollTag);
				
//		if(tollTag != null) {
//		tollTag = this.tollDBService.updateTollTag(tollTag);
//		}		
		return vahanInfo;
	}

	public ResponseEntity reqVehicleDetailsResponse(String respVehicleDetails) {

		String traceId = httpServletContext.getTraceId();

		if (StringUtils.isEmpty(respVehicleDetails)) {
			LOG.debug(LogFormatter.instance(traceId).message("Response Is empty").format());
			return null;
		}

		TollTag tollTag = new TollTag();
		ReqVehicleDetailResponse reqVehicleDetailResp = new ReqVehicleDetailResponse();

		RespVehicleDetails reqVehicleDetailsResp = JAXB.unmarshal(new StringReader(respVehicleDetails),
				RespVehicleDetails.class);
		
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("")
            .data("reqVehicleDetailsResp", reqVehicleDetailsResp).format());

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Txn Id")
				.data("Txn Id", reqVehicleDetailsResp.getTxn().getId()).format());

		if (!reqVehicleDetailsResp.getTxn().getResp().getVehicle().getErrCode().isEmpty()
				&& !reqVehicleDetailsResp.getTxn().getResp().getVehicle()
						.getErrCode().equals(TollErrorCodes.Error_code_SUCCESS.code())) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Received Failure Response from NPCI for RespVehicleDetails")
					.data("Response:", reqVehicleDetailsResp).format());

			return ResponseEntity.status(HttpStatus.ACCEPTED).body(HeaderCode.RECEIVED_FAILURE_RESPONSE_FROM_NPCI);

		}

		if (reqVehicleDetailsResp.getTxn().getResp().getVehicle().getVehicleDetails() != null) {

			for (VehicleDetails vehicleDetails : reqVehicleDetailsResp.getTxn().getResp().getVehicle()
					.getVehicleDetails()) {

				for (Detail detail : vehicleDetails.getDetail()) {

					String name = detail.getName();
					String value = detail.getValue();

					switch (name) {
					case "TAGID":
						reqVehicleDetailResp.setTAGID(value);
						break;
					case "REGNUMBER":
						reqVehicleDetailResp.setREGNUMBER(value);
						break;
					case "TID":
						reqVehicleDetailResp.setTID(value);
						break;
					case "VIN":
						reqVehicleDetailResp.setVIN(value);
						break;
					case "ENGINENO":
						reqVehicleDetailResp.setENGINENO(value);
						break;
					case "STATE":
						reqVehicleDetailResp.setSTATE(value);
						break;
					case "VEHICLEDESCRIPTOR":
						reqVehicleDetailResp.setVEHICLEDESCRIPTOR(value);
						break;
					case "REGISTERED_VEHICLE":
						reqVehicleDetailResp.setREGISTERED_VEHICLE(value);
						break;
					case "MAKER_DESCR":
						reqVehicleDetailResp.setMAKER_DESCR(value);
						break;
					case "MONTH_YEAR":
						reqVehicleDetailResp.setMONTH_YEAR(value);
						break;
					case "NUMBER_OF_AXLES":
						reqVehicleDetailResp.setNUMBER_OF_AXLES(value);
						break;
					case "F_AXLE_DESCP":
						reqVehicleDetailResp.setF_AXLE_DESCP(value);
						break;
					case "R_AXLE_DESCP":
						reqVehicleDetailResp.setR_AXLE_DESCP(value);
						break;
					case "T_AXLE_DESCP":
						reqVehicleDetailResp.setT_AXLE_DESCP(value);
						break;
					case "O_AXLE_DESCP":
						reqVehicleDetailResp.setO_AXLE_DESCP(value);
						break;
					case "F_AXLE_WEIGHT":
						reqVehicleDetailResp.setF_AXLE_WEIGHT(value);
						break;
					case "R_AXLE_WEIGHT":
						reqVehicleDetailResp.setR_AXLE_WEIGHT(value);
						break;
					case "T_AXLE_WEIGHT":
						reqVehicleDetailResp.setT_AXLE_WEIGHT(value);
						break;
					case "O_AXLE_WEIGHT":
						reqVehicleDetailResp.setO_AXLE_WEIGHT(value);
						break;
					case "MAKE_MODEL":
						reqVehicleDetailResp.setMAKE_MODEL(value);
						break;
					case "NUMBER_OF_SEATS":
						reqVehicleDetailResp.setNUMBER_OF_SEATS(value);
						break;
					case "COLOR":
						reqVehicleDetailResp.setCOLOR(value);
						break;
					case "FUEL_DESCR":
						reqVehicleDetailResp.setFUEL_DESCR(value);
						break;
					case "UNLD_WT":
						reqVehicleDetailResp.setUNLD_WT(value);
						break;
					case "GVW":
						reqVehicleDetailResp.setGVW(value);
						break;
					case "NATIONAL_PERMIT":
						reqVehicleDetailResp.setNATIONAL_PERMIT(value);
						break;
					case "NATIONAL_PERMIT_START_DATE":
						reqVehicleDetailResp.setNATIONAL_PERMIT_START_DATE(value);
						break;
					case "NATIONAL_PERMIT_END_DATE":
						reqVehicleDetailResp.setNATIONAL_PERMIT_END_DATE(value);
						break;
					case "ALL_INDIA_TOURIST_PERMIT":
						reqVehicleDetailResp.setALL_INDIA_TOURIST_PERMIT(value);
						break;
					case "ALL_INDIA_TOURIST_PERMIT_START_DATE":
						reqVehicleDetailResp.setALL_INDIA_TOURIST_PERMIT_START_DATE(value);
						break;
					case "ALL_INDIA_TOURIST_PERMIT_END_DATE":
						reqVehicleDetailResp.setALL_INDIA_TOURIST_PERMIT_END_DATE(value);
						break;
					default:
						break;

					}

				}
			}
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("")
            .data("reqVehicleDetailsResp", reqVehicleDetailsResp).format());
		
		tollTag = this.tollDBService.findByTollTagOrTid(reqVehicleDetailResp.getTAGID(), reqVehicleDetailResp.getTID());

		if(tollTag != null) {
		  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("")
	            .data("reqVehicleDetailsResp", reqVehicleDetailsResp).format());
		tollTag.setEngineNo(reqVehicleDetailResp.getENGINENO());
		tollTag.setNationalPermit(reqVehicleDetailResp.getNATIONAL_PERMIT());
		tollTag.setPermitExpiryDate(reqVehicleDetailResp.getNATIONAL_PERMIT_END_DATE());
		tollTag.setRegisteredVehicle(reqVehicleDetailResp.getREGISTERED_VEHICLE());
		tollTag.setVehicleDescriptor(reqVehicleDetailResp.getVEHICLEDESCRIPTOR());
		tollTag.setVinNumber(reqVehicleDetailResp.getVIN());

		if (this.tollDBService.updateTollTag(tollTag) == null) {
		  
		  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("")
	            .data("reqVehicleDetailsResp", reqVehicleDetailsResp).format());
		  
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.FAILED_TO_UPDATE_TOLLTAG_DETAILS));

		}
		}else {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("TollTag entry for given tagId and Tid not found to update")
					.data("TagID:", reqVehicleDetailResp.getTAGID())
					.data("TID",reqVehicleDetailResp.getTID()).format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HeaderCode.BAD_REQUEST);

		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("")
            .data("reqVehicleDetailsResp", reqVehicleDetailsResp).format());

		//////
		return ResponseEntity.ok(null);

	}
	
    
public ResponseEntity sendAckToNpci(int bankId) {
		
		String refUrl = StringUtils.EMPTY;
	    TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	    
	    
	    Bank bank = bankDBService.getBank(bankId);   
	    if (bank == null) {
	      LOG.debug(
	          LogFormatter.instance(httpServletContext.getTraceId()).message("bank is null").format());
	      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	          .body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
	    }

	    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	        .message("calling toll sync time").format());
	      String xmlData = null;

	    try {

	      ResponseEntity responseEntity = this.brontooResource.syncTime(bank);
	      if (responseEntity.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
	        return responseEntity;
	      }
	      tollNetcSyncTimeResponse = (TollNetcSyncTimeResponse) responseEntity.getBody();
	      
	    
	    String[] bankInfos = tollProperties.getBankInfo().split(",");

	      for (String bankInfo : bankInfos) {
	        String[] info = bankInfo.split("~");
	        if (info[2].equalsIgnoreCase(bank.getIin())) {
	          refUrl = info[3];
	        }
	      }

	      TollNetcAckRequest ackRequest = new TollNetcAckRequest();
	      
	      SimpleDateFormat formatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
	      Date date = formatter.parse(tollNetcSyncTimeResponse.getResp().getTs());
	      ackRequest.setTs(tollNetcSyncTimeResponse.getResp().getTs());
	      ackRequest.setReqMsgId(bank.getOrgId() + formatter.format(date).toUpperCase());
	      ackRequest.setApi("");
	      ackRequest.setErr("");
	      
	   // Create JAXB Context
	      JAXBContext jaxbContext = JAXBContext.newInstance(TollNetcAckRequest.class);

	      // Create Marshaller
	      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

	      // Required formatting??
	      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

	      // Print XML String to Console
	      StringWriter sw = new StringWriter();

	      // Write XML to StringWriter
	      jaxbMarshaller.marshal(ackRequest, sw);

	      // Verify XML Content
	      String postData = sw.toString();

	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
      	          .message("POST DATA : " +postData).format());
	      
	      ByteArrayInputStream byteArrayInputStream =
	          new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

	       byteArrayOutputStream =
	          tollSignatureGenerationServices.signatureGenerationServices(byteArrayInputStream,
	              httpServletContext.getTraceId(), bank.getOrgId());
	      
	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
      	          .message("byteArrayOutputStream : " +byteArrayOutputStream).format());
	      
	       xmlData = byteArrayOutputStream.toString(StandardCharsets.UTF_8);

	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
      	          .message("xmlData : " +xmlData).format());
	      
	      //String responseData = null;
	      
	     //NPCI Active Active Setup Phase2 changes
	     /* String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());        
	      if(StringUtils.isEmpty(ipAddress)) {

	      	LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	      	          .message("********** NPCI Server is DOWN **********").format());
	  		return null;

	      }

	      int port = this.tollProperties.getNetcHealthCheckPort();

	      if (tollProperties.isConnectNpci()) {
	          TollHttpsServices.bankIin = bank.getIin();
	          
	          String endPoint =  this.tollProperties.getAckUrl();
	          
	          String url = "https://" + ipAddress + ":" + port + endPoint;
	          
	          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	      	          .message("URL Formed: " +url).format());
	        	
	          responseData =
	              tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);
	         }
*/	      
	      
	    }catch(Exception e) {
	    	e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(HeaderCode.INTERNAL_ERROR);

	    }
	return ResponseEntity.status(HttpStatus.ACCEPTED).body(xmlData);
	    
	}

	
	
    public ResponseEntity respMngTagEntriesResponse(String respMngTagEntries) {

      try {

        String traceId = httpServletContext.getTraceId();

        if (StringUtils.isEmpty(respMngTagEntries)) {
          LOG.debug(LogFormatter.instance(traceId).message("Response Is empty")
              .data("respMngTagEntries", respMngTagEntries).format());
          return null;
        }

        LOG.debug(LogFormatter.instance(traceId).message("Response")
            .data("respMngTagEntries", respMngTagEntries).format());

        RespVehicleDetails respMngTagResp =
            JAXB.unmarshal(new StringReader(respMngTagEntries), RespVehicleDetails.class);

        LOG.debug(LogFormatter.instance(traceId).message("Response")
            .data("respMngTagResp", respMngTagResp).format());
        
        TagNPCIApprovalStatus tagNPCIApprovalStatus = null;
        TollTag tolltag = null;

        if (respMngTagResp.getTxn().getNote().equals(TollConstant.ADD_OP)) {

          tagNPCIApprovalStatus = this.tagNPCIApprovalStatusDBService
              .getTagNPCIApprovalStatusByTxnId(respMngTagResp.getTxn().getId());
          
          LOG.debug(LogFormatter.instance(traceId).message("tagNPCIApprovalStatus")
              .data("tagNPCIApprovalStatus", tagNPCIApprovalStatus).format());

          tolltag = this.tollDBService.findTollTagById(tagNPCIApprovalStatus.getTollTagId());
          
        } else {
          
          tolltag = this.tollDBService.findTollCustomersByTagId(
              respMngTagResp.getTxn().getResp().getTag().get(0).getTagId());
        }
        
        TollRegistration tollRegistration = tolltag.getTollRegistration();

        List<Merchant> merchants = merchantDBService
            .asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();
        List<String> roleList = new ArrayList<>();
        roleList.add(DBConstants.UserRoles.MERCHANT_INTERNAL.value());

        if (CollectionUtils.isEmpty(merchants)) {
          // this.tollBankResource.removeLock(userProfileLock, serialNumberLock);
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("merchant is null").format());
          // response.addHeaderCode(HeaderCode.MERCHANT_DOESNT_EXIST);
          return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
        }

        boolean callUpdateNpci = false;

        String[] errCodes =
            respMngTagResp.getTxn().getResp().getTag().get(0).getErrCode().split(",");

        for (String errCode : errCodes) {

          if (HeaderCode.TAGID_ALREADY_PRESENT_IN_DATABASE.code()
              .equalsIgnoreCase("N-" + errCode)) {

            callUpdateNpci = true;
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("tagId is already present in db hence calling tag Updation api "
                    + tolltag.getTagId())
                .format());
          }
        }

        Bank bank = this.bankDBService.getBankByOrgId(respMngTagResp.getHead().getOrgId());

        if (callUpdateNpci) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .data("callUpdateNpci", callUpdateNpci).format());

          TollTagUpdateResponse respMngTagResponse =
              this.tollBankResource.asyncUploadTheTollListToNETCManageTag(httpServletContext,
                  Arrays.asList(tolltag), TollConstant.UPDATE_OP, bank, merchants.get(0),null);

          if (respMngTagResponse == null) {

            if (this.tollDBService.updateTollTag(tolltag) == null) {
              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("tollTag Not Updated").format());
            }
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("tollTagUpdateResponse is null").format());
            // this.tollBankResource.removeLock(userProfileLock, serialNumberLock);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);

          }
        }

        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .data("callUpdateNpci", callUpdateNpci).format());

        tolltag.setAddTagResponse(objectMapper.writeValueAsString(respMngTagResp));
        tolltag.setAddTagErrorCode(respMngTagResp.getTxn().getResp().getTag().get(0).getErrCode());
        
        if (respMngTagResp.getTxn().getNote().equals(TollConstant.ADD_OP)) {

          tagNPCIApprovalStatus = this.tagNPCIApprovalStatusDBService
              .getTagNPCIApprovalStatusByTxnId(respMngTagResp.getTxn().getId());

          tagNPCIApprovalStatus.setAddTagErrorCode(respMngTagResp.getTxn().getResp().getTag().get(0).getErrCode());       
          tagNPCIApprovalStatus.setRawResponse(objectMapper.writeValueAsString(respMngTagResp));
         
        } 

        if (!respMngTagResp.getTxn().getResp().getTag().get(0).getErrCode()
            .equals(TollErrorCodes.Error_code_SUCCESS.code())
            && !HeaderCode.VEHICLE_REGNO_OR_VIN_OR_ENGINENUMBER_ALREADY_REGISTERED_WITH_SOME_OTHER_TAGID
                .code().contains(respMngTagResp.getTxn().getResp().getTag().get(0).getErrCode())
            && !respMngTagResp.getTxn().getResp().getTag().get(0).getResult()
                .equalsIgnoreCase(TollErrorCodes.Error_code_SUCCESS.message())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("tolltag")
              .data("tolltag", tolltag).format());
          tollRegistration
              .setStatus(String.valueOf(DBConstants.TollRegistrationStatus.DEFAULT.value()));
          tolltag.setTollRegistration(tollRegistration);
          
          this.tollDBService.updateTollTag(tolltag);
          if (respMngTagResp.getTxn().getNote().equals(TollConstant.ADD_OP)) {
            this.tagNPCIApprovalStatusDBService.updateTagNPCIApprovalStatus(tagNPCIApprovalStatus);
          }
          return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);

        }
        
        if (respMngTagResp.getTxn().getNote().equals(TollConstant.ADD_OP)) {

//          tolltag = this.tollDBService.findTollTagById(tagNPCIApprovalStatus.getTollTagId());
          
          tolltag.setSerialNumber(tagNPCIApprovalStatus.getSerialNumber());
          tolltag.setTagId(tagNPCIApprovalStatus.getTagId());
          tolltag.setTid(tagNPCIApprovalStatus.getTid());
          
          tagNPCIApprovalStatus.setAddTagErrorCode(respMngTagResp.getTxn().getResp().getTag().get(0).getErrCode());       
          tagNPCIApprovalStatus.setRawResponse(objectMapper.writeValueAsString(respMngTagResp));
         
        } 

        boolean ostaProcessed = true;
        boolean ostaDoesnotExist = false;

        if (tolltag.getRegistrationAmount().compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO
            || tolltag.getDepositAmount().compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO
            || tolltag.getMiscCharges() == TagDeliveryType.COURIER.value()) {

          List<Dipcoin> dcoins = dipcoinDBService.asyncFindDipcoin(tolltag.getCustomerAccountId(),
              Arrays.asList(DBConstants.DipcoinUsageType.DEPOSIT.value(),
                  DBConstants.DipcoinUsageType.FEE.value()))
              .get();

          if (CollectionUtils.isEmpty(dcoins)) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("deposit and fees amount not available")
                .data("Customer Account Id", tolltag.getCustomerAccountId()).format());

            ostaDoesnotExist = true;
          } else {
            List<Integer> userId = new ArrayList<Integer>();
            userId.add(tolltag.getTollRegistration().getUserId());
            List<User> dcoinUser = userDBService.asyncGetUsersByIds(userId).get();
            ResponseEntity processDcoinResponse = null;

            for (Dipcoin dcoin : dcoins) {
              if (StringUtils.isNotBlank(dcoin.getUsageCategory())
                  && dcoin.getUsageCategory().equalsIgnoreCase(tolltag.getRegistrationNo())
                  && DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {
                PartnerProcessDipcoinRequest dcoinReq = new PartnerProcessDipcoinRequest();
                dcoinReq.setOsta(dcoinUser.get(0).getPhone().concat(dcoin.getCoin()));
                dcoinReq.setAmount(dcoin.getAmount());
                dcoinReq.setCurrency(TollConstant.INR_CURRENCY);
                dcoinReq.setOrderId(
                    dcoin.getUsageCategory() + DateTime.now(DateTimeZone.UTC).getMillis());
                dcoinReq.setPartnerTransactionReferenceId(Utils.constructPartnerReferenceId(bank));
                SubPartner subPartner = new SubPartner();
                subPartner.setName(DBConstants.DipcoinUsageType.FEE.value() == dcoin.getUsageType()
                    ? "Toll Registration Fee"
                    : "Toll Registration Deposit");
                dcoinReq.setSubPartner(subPartner);

                try {

                  User user = this.userDBService
                      .getUsersByIds(Arrays.asList(tolltag.getApprovedBy())).get(0);

                  processDcoinResponse = partnerDipcoinResource.processCustomerDipcoin(user, bank,
                      dcoinReq, TransactionSource.WEB, Boolean.FALSE);

                } catch (Exception e) {
                  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("deposit and fees not processed").format());

                  ostaProcessed = false;
                }

                if (HttpStatus.BAD_REQUEST.value() <= processDcoinResponse.getStatusCodeValue()) {
                  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                      .message("deposit and fees not processed").format());

                  ostaProcessed = false;
                }
              }

            }

            if (null == processDcoinResponse) {
              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("deposit and fees not processed").format());

              ostaProcessed = false;
            }
          }
        }


        List<User> createdByUsers =
            this.userDBService.getUsersByIds(Arrays.asList(tollRegistration.getCreatedBy()));
        User createdByUser = createdByUsers.get(0);
        // Check the role of createdbyUser
        // if Bank --> Status = Active OR Customer = CUSTOMER_ACTIVATION_PENDING
//        if (tollRegistration != null
//            && !createdByUser.getRole().equals(DBConstants.UserRoles.CUSTOMER.value())) {
//          tolltag.setStatus(String.valueOf(TollTagApprovalStatus.ACTIVE.value()));
//        } else {
//          tolltag
//              .setStatus(String.valueOf(TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value()));
//        }
        
        tolltag.setStatus(String.valueOf(TollTagApprovalStatus.ACTIVE.value()));

        if (!ostaProcessed) {
          tolltag.setStatus(String.valueOf(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()));
        }

        Epc epc = this.tollDBService.findEpcBySerialNumber(tolltag.getSerialNumber());

        tolltag.setApprovedDateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
        tolltag.setVendorUpdateDateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
        tollRegistration
            .setStatus(String.valueOf(DBConstants.TollRegistrationStatus.ACTIVE.value()));
        tolltag.setTollRegistration(tollRegistration);
        // update the TollTag details.
        
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Before tolltag Updation")
            .data("tolltag", tolltag).format());
        
        TollTag tollTagResponse = this.tollDBService.updateTollTag(tolltag);
        
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("After tolltag Updation")
            .data("tollTagResponse", tollTagResponse).format());
        
        if (respMngTagResp.getTxn().getNote().equals(TollConstant.ADD_OP)) {
          this.tagNPCIApprovalStatusDBService.updateTagNPCIApprovalStatus(tagNPCIApprovalStatus);
        }
        
        // After the Success From Tag Vendor NETC APPROVAL CALL and CREATE OSTA
        if (tollTagResponse == null) {
          // TODO Revert processed dipcoin
          // this.tollBankResource.removeLock(userProfileLock, serialNumberLock);
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("updatetollTag is null").format());
          // response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
          return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
        }


        epc.setStatus(DBConstants.EpcStatus.USED.value());
        epc = this.tollDBService.saveEpc(epc);
        if (epc == null) {
          LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Epc Not Updated").format());
        }

        String originIp = tolltag.getApprovedIPAddress();
        if (!ostaProcessed) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Dipcoin not active").format());
          return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
        } else if (ostaDoesnotExist) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Dipcoin does not exist").format());
          // this.tollBankResource.removeLock(userProfileLock, serialNumberLock);
          // response.addHeaderCode(HeaderCode.DIPCOIN_DOESNT_EXIST);
          return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
        } 
          try {
            // success Email
            if (!emailUtils.sendTollCustomerTagAllotedEmail(originIp, tolltag, tollRegistration,
                epc.getSerialNumber())) {
              LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("Failed to send email to user")
                  .data("user email", tollRegistration.getEmailId()).format());
            }

            // success message
            if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(
                tollRegistration.getMobileNo(),
                Templates.TollTagBankApproval.format(tolltag.getRegistrationNo(), bank.getAlias()),
                true)) {
              LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                  .message("Failed to send SMS").data("phone", tollRegistration.getMobileNo())
                  .format());
            }

            if (applicationProperties.getAwsSMSClient()) {

              NotificationRequestContext notificationRequestContext =
                  new NotificationRequestContext();
              notificationRequestContext.setTraceId(httpServletContext.getTraceId());
              if (!notificationResource.sendSms(tolltag.getRegistrationNo(),
                  Templates.TollTagBankApproval.format(tolltag.getRegistrationNo(),
                      bank.getAlias()),
                  httpServletContext.getClientFeatureFlags().smsEnabled(),
                  notificationRequestContext)) {

                LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Failed to send SMS").data("phone", tollRegistration.getMobileNo())
                    .format());

              }
            }

          } catch (Exception e) {
            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Exception While Sending Email To Bank For Fastag Approval").format());

          }
        

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
      } catch (Exception e) {

        LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception Caught")
            .format());
        e.printStackTrace();
      }
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);

    }  
}
