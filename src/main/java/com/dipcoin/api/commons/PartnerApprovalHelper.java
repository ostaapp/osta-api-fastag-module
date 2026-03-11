package com.dipcoin.api.commons;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.PartnerApprovalStatus;
import com.dipcoin.api.resource.MerchantResource;
import com.dipcoin.api.resource.NotificationResource;
import com.dipcoin.api.resource.UserLoginResource;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.PartnerDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BankStatus;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantStatus;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalFields;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalLevels;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.UserStatus;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankAccount;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.MerchantOnboard;
import com.dipcoin.db.services.model.PartnerApproval;
import com.dipcoin.db.services.model.PartnerCredential;
import com.dipcoin.db.services.model.User;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.db.services.PartnerAccountDBService;
//import com.dipcoin.partner.db.services.PartnerEntityDBService;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerAccountStatus;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerBankAccountCodes;
import com.dipcoin.partner.db.services.model.Partner;
import com.dipcoin.partner.db.services.model.PartnerAccount;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("partnerApprovalHelper")
@Transactional(rollbackFor = {Exception.class, APIException.class},
propagation = Propagation.REQUIRES_NEW)
public class PartnerApprovalHelper {
  private static ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger LOG = LogManager.getLogger(UserLoginResource.class);

  @Autowired
  private PartnerDBService partnerDBService;

  @Autowired
  private MerchantDBService merchantDBService;

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private EmailUtils emailUtils;

  @Autowired
  private SmsClient smsClient;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

//  @Autowired
//  private OfflineJobClient offlineJobClient;

  @Autowired
  private ApplicationProperties applicationProperties;
//  
//  @Autowired
//  private PartnerEntityDBService partnerEntityDBService;
  
  @Autowired
  private PartnerAccountDBService partnerAccountDBService;
  
  @Autowired
  private NotificationResource notificationResource;
  
  /*
   * Add and set lead, form, doc, bank account, payment, integration
   */
  public PartnerApprovalStatus submitPartnerApproval(User user, Merchant merchant,
      MerchantOnboard merchantOnboard, Bank bank, PartnerApprovalFields submitType)
      throws Exception, APIException {

    Integer userId = 0;
    boolean isPartnerApprovalData = false;
    if (user != null)
      userId = user.getId();
    PartnerApprovalStatus partnerApprovalStatus = new PartnerApprovalStatus();
    partnerApprovalStatus.setApproval(PartnerApprovalLevels.NOT_STARTED.ordinal(), userId,
        PartnerApprovalLevels.NOT_STARTED.name());
    PartnerApproval partnerApproval = this.partnerDBService.getPartnerApproval(merchant, bank);
    if (partnerApproval != null) {
      partnerApprovalStatus =
          objectMapper.readValue(partnerApproval.getApprovalLevels(), PartnerApprovalStatus.class);
      partnerApprovalStatus.setApproval(PartnerApprovalLevels.SUBMITTED.ordinal(), userId,
          PartnerApprovalLevels.SUBMITTED.name());
      isPartnerApprovalData = true;
    } else {
      setPartnerApprovalStatus(partnerApprovalStatus);
      partnerApproval = new PartnerApproval();
      partnerApproval.setMerchant(merchant);
      partnerApproval.setBank(bank);
    }
    if (PartnerApprovalFields.LEAD.equals(submitType)) {
      partnerApprovalStatus.getApproval().setStatus(PartnerApprovalLevels.APPROVED.ordinal());
      partnerApprovalStatus.getApproval().setDescription(PartnerApprovalLevels.APPROVED.name());
      partnerApprovalStatus.setLead(partnerApprovalStatus.getApproval());
    }

    setPartnerApprovalStatus(partnerApprovalStatus, submitType);

    partnerApproval.setApprovalLevels(objectMapper.writeValueAsString(partnerApprovalStatus));
    if (isPartnerApprovalData)
      partnerApproval = this.partnerDBService.updatePartnerApproval(partnerApproval);
    else
      partnerApproval = this.partnerDBService.addPartnerApproval(partnerApproval);

    if (partnerApproval != null) {

      if ((partnerApprovalStatus.getLead().getStatus() == PartnerApprovalLevels.APPROVED.ordinal())

          && (partnerApprovalStatus.getForm().getStatus() == PartnerApprovalLevels.SUBMITTED
              .ordinal()
              || partnerApprovalStatus.getForm().getStatus() == PartnerApprovalLevels.APPROVED
                  .ordinal())

          && (partnerApprovalStatus.getDocument().getStatus() == PartnerApprovalLevels.SUBMITTED
              .ordinal()
              || partnerApprovalStatus.getDocument().getStatus() == PartnerApprovalLevels.APPROVED
                  .ordinal())

          && (partnerApprovalStatus.getBankAccount().getStatus() == PartnerApprovalLevels.SUBMITTED
              .ordinal()
              || partnerApprovalStatus.getBankAccount()
                  .getStatus() == PartnerApprovalLevels.APPROVED.ordinal())

          && (partnerApprovalStatus.getPayment().getStatus() == PartnerApprovalLevels.SUBMITTED
              .ordinal()
              || partnerApprovalStatus.getPayment().getStatus() == PartnerApprovalLevels.APPROVED
                  .ordinal())

          && (partnerApprovalStatus.getIntegration().getStatus() == PartnerApprovalLevels.SUBMITTED
              .ordinal()
              || partnerApprovalStatus.getIntegration()
                  .getStatus() == PartnerApprovalLevels.APPROVED.ordinal())) {
        if(submitType !=null && !PartnerApprovalFields.LEAD.equals(submitType)) {
        if (merchantOnboard != null) {
          merchantOnboard.setStatus(MerchantStatus.SUBMITTED.value());

          if (this.merchantDBService.updateMerchantOnboard(merchantOnboard) == null) {
            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
                APIResponse.error(HeaderCode.INTERNAL_ERROR));
          }
        } else {
          bank.setStatus(BankStatus.SUBMITTED.value());
          if (this.bankDBService.updateBank(bank) == null) {
            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
                APIResponse.error(HeaderCode.INTERNAL_ERROR));
          }
        }
      }
    }
   }
    return partnerApprovalStatus;
  }

  /*
   * delete partner bank account / document
   */
  public PartnerApprovalStatus deletePartnerApproval(User user, Merchant merchant, Bank bank,
      PartnerApprovalFields submitType) throws Exception, APIException {

    PartnerApprovalStatus partnerApprovalStatus = new PartnerApprovalStatus();
    PartnerApproval partnerApproval = this.partnerDBService.getPartnerApproval(merchant, bank);
    if (partnerApproval != null) {
      partnerApprovalStatus =
          objectMapper.readValue(partnerApproval.getApprovalLevels(), PartnerApprovalStatus.class);

      // check if bank account then check if number of accounts in table, if present then submitted
      // else not started
      partnerApprovalStatus.setApproval(PartnerApprovalLevels.NOT_STARTED.ordinal(), user.getId(),
          PartnerApprovalLevels.NOT_STARTED.name());

      setPartnerApprovalStatus(partnerApprovalStatus, submitType);

      partnerApproval.setApprovalLevels(objectMapper.writeValueAsString(partnerApprovalStatus));
      partnerApproval = partnerDBService.updatePartnerApproval(partnerApproval);

      if (partnerApproval != null) {
        if (merchant != null) {
          // if a merchant deletes their bank account we need to update status with resp merchant
          // table and merchantOnboard table
          if (MerchantStatus.ACTIVE.equals(merchant.getStatus())) {
            merchant.setStatus(MerchantStatus.INACTIVE.value());
            if (this.merchantDBService.updateMerchant(merchant) == null) {
              throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
                  APIResponse.error(HeaderCode.INTERNAL_ERROR));
            }
          }
          MerchantOnboard updateMerchantOnboard =
              this.merchantDBService.getMerchantOnboardByReferenceId(merchant.getReferenceId());
          updateMerchantOnboard.setStatus(MerchantStatus.INACTIVE.value());
          if (this.merchantDBService.updateMerchantOnboard(updateMerchantOnboard) == null) {
            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
                APIResponse.error(HeaderCode.INTERNAL_ERROR));
          }
        } else {
          bank.setStatus(BankStatus.INACTIVE.value());
          if (this.bankDBService.updateBank(bank) == null) {
            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
                APIResponse.error(HeaderCode.INTERNAL_ERROR));
          }
        }
      }
    }
    return partnerApprovalStatus;

  }

//  @SuppressWarnings("null")
//public void verifyPartner(User user, Merchant merchant, Bank bank,
//      PartnerApprovalStatus updateReq, PartnerApprovalLevels status)
//      throws Exception, APIException {
//
//    String now = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
//    Boolean isPartnerApprovalData = false;
//    boolean sendEmail = false;
//    MerchantOnboard updateMerchantOnboard = null;
//    PartnerApprovalStatus partnerApprovalStatus = new PartnerApprovalStatus();
//    PartnerApproval partnerApproval = this.partnerDBService.getPartnerApproval(merchant, bank);
//
//    if (partnerApproval != null) {
//      partnerApprovalStatus =
//          objectMapper.readValue(partnerApproval.getApprovalLevels(), PartnerApprovalStatus.class);
//      isPartnerApprovalData = true;
//    } else {
//      partnerApprovalStatus.setApproval(PartnerApprovalLevels.NOT_STARTED.ordinal(), null,
//          PartnerApprovalLevels.NOT_STARTED.name());
//
//      setPartnerApprovalStatus(partnerApprovalStatus);
//
//      partnerApproval = new PartnerApproval();
//      if (merchant != null)
//        partnerApproval.setMerchant(merchant);
//      else
//        partnerApproval.setBank(bank);
//    }
//
//    if (updateReq != null) {
//      if (updateReq.getForm() != null) {
//        partnerApprovalStatus.setApproval(updateReq.getForm().getStatus(), user.getId(),
//            updateReq.getForm().getDescription());
//        partnerApprovalStatus.setForm(partnerApprovalStatus.getApproval());
//      } else if (updateReq.getDocument() != null) {
//        partnerApprovalStatus.setApproval(updateReq.getDocument().getStatus(), user.getId(),
//            updateReq.getDocument().getDescription());
//        partnerApprovalStatus.setDocument(partnerApprovalStatus.getApproval());
//      } else if (updateReq.getBankAccount() != null) {
//        partnerApprovalStatus.setApproval(updateReq.getBankAccount().getStatus(), user.getId(),
//            updateReq.getBankAccount().getDescription());
//        partnerApprovalStatus.setBankAccount(partnerApprovalStatus.getApproval());
//      } else if (updateReq.getPayment() != null) {
//        partnerApprovalStatus.setApproval(updateReq.getPayment().getStatus(), user.getId(),
//            updateReq.getPayment().getDescription());
//        partnerApprovalStatus.setPayment(partnerApprovalStatus.getApproval());
//      } else if (updateReq.getIntegration() != null) {
//        partnerApprovalStatus.setApproval(updateReq.getIntegration().getStatus(), user.getId(),
//            updateReq.getIntegration().getDescription());
//        partnerApprovalStatus.setIntegration(partnerApprovalStatus.getApproval());
//      }
//    }
//
//    partnerApproval.setApprovalLevels(objectMapper.writeValueAsString(partnerApprovalStatus));
//    if (isPartnerApprovalData)
//      partnerApproval = this.partnerDBService.updatePartnerApproval(partnerApproval);
//    else
//      partnerApproval = this.partnerDBService.addPartnerApproval(partnerApproval);
//
//    if (partnerApproval != null) {
//
//      Bank updateBank = null;
//      Merchant updateMerchant = null;
//      if (merchant != null) {
//
//        updateMerchantOnboard =
//            this.merchantDBService.getMerchantOnboardByReferenceId(merchant.getReferenceId());
//        if (updateMerchantOnboard == null) {
//          throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//              APIResponse.error(HeaderCode.INTERNAL_ERROR));
//        }
//        updateMerchantOnboard.setUpdateTime(now);
//        updateMerchantOnboard.setUser(user);
//
//        if (PartnerApprovalLevels.APPROVED.equals(status)) {
//          String yearFromNow =
//              String.valueOf(DateTime.now(DateTimeZone.UTC).plusYears(1).getMillis());
//
//          List<Integer> partnerStatus =
//              Arrays.asList(MerchantStatus.DEACTIVATED.value(), MerchantStatus.DELETED.value());
//
//          updateMerchantOnboard.setStatus(MerchantStatus.ACTIVE.value());
//
//          if (merchant.getActivationEndDate() == null
//              || Long.parseLong(merchant.getActivationEndDate()) < Long.parseLong(now)
//              || partnerStatus.contains(merchant.getStatus())) {
//
//            updateMerchantOnboard.setActivationStartDate(now);
//            updateMerchantOnboard.setActivationEndDate(yearFromNow);
//            sendEmail = true;
//          }
//          updateInternalUser(user, merchant, null);
//          //Check for whether the Merchant details are present in PartnerCredential table
//          PartnerCredential credential =  partnerDBService.getCredentialByClientId(merchant.getReferenceId());
//          if(credential != null) {
//            credential.setStatus(DBConstants.PartnerCredentialStatus.ACTIVE.value());
//            credential = partnerDBService.updateCredential(credential);
//            if (credential == null) {
//              throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//                  APIResponse.error(HeaderCode.FAILED_TO_ADD_OR_UPDATE_CREDENTIALS));
//            }
//          }
//        } else {
//          updateMerchantOnboard.setStatus(MerchantStatus.REVIEW_IN_PROGRESS.value());
//        }
//
//        if (updateMerchantOnboard
//            .getBusinessSegment() == DBConstants.MerchantBusinessSegment.TYPE_NA.value()
//            || updateMerchantOnboard.getPartnerType() == DBConstants.MerchantPartnerType.TYPE_NA
//                .value()
//            || updateMerchantOnboard
//                .getCustomReportUI() == DBConstants.MerchantCustomReportUI.TYPE_NA.value()) {
//          throw new APIException(HttpStatus.BAD_REQUEST,
//              APIResponse.error(HeaderCode.MISSING_SELECTION_OF_CERTAIN_PARAMETERS));
//        }
//        
//        updateMerchantOnboard = this.merchantDBService.updateMerchantOnboard(updateMerchantOnboard);
//        if (updateMerchantOnboard == null) {
//          throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//              APIResponse.error(HeaderCode.INTERNAL_ERROR));
//        }
//
//        if (DBConstants.MerchantStatus.ACTIVE.equals(updateMerchantOnboard.getStatus())) {
//          // Copy Merchantonboard to merchant
//          MerchantResource.updateMerchantTable(updateMerchantOnboard, merchant);
//          updateMerchant = this.merchantDBService.updateMerchant(merchant);
//          if (updateMerchant == null) {
//            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//                APIResponse.error(HeaderCode.INTERNAL_ERROR));
//          }
//
//          List<User> merchantSuperAdmins = this.userDBService.getBankMerchantUsers(merchant.getId(),
//              Arrays.asList(UserRoles.MERCHANT_SUPERADMIN.value()));
//
//          if (CollectionUtils.isEmpty(merchantSuperAdmins))
//            return;
//
//          //Populating Partner Table in Partner Api system
//              
//          if (DBConstants.MerchantPartnerType.PARTNER.value() == updateMerchant.getPartnerType()
//              || DBConstants.MerchantPartnerType.PARTNER_ORGANISATION.value() == updateMerchant
//                  .getPartnerType()
//              || DBConstants.MerchantPartnerType.PARTNER_AGGREGATOR.value() == updateMerchant
//                  .getPartnerType()
//              || DBConstants.MerchantPartnerType.INTERNAL_PARTNER.value() == updateMerchant
//                  .getPartnerType()
//              || DBConstants.MerchantPartnerType.PARTNER_WHOLESALER.value() == updateMerchant
//                  .getPartnerType()
//              || DBConstants.MerchantPartnerType.PARTNER_RETAILER.value() == updateMerchant
//                  .getPartnerType()) {
//
//
//            Partner partner = new Partner();
//            
//        	partner = this.partnerEntityDBService.getPartner(updateMerchant.getReferenceId());
//        	 
//        	if(partner == null) {
//            partner.setType(
//                com.dipcoin.partner.db.services.commons.DBConstants.PartnerType.PARENT.value());
//            partner.setReferenceId(updateMerchant.getReferenceId());
//            partner.setEmail(updateMerchant.getEmailId());
//            partner.setName(updateMerchant.getName());
//            partner.setPhone(updateMerchant.getOfficeNumber());
//            partner.setStatus(
//                com.dipcoin.partner.db.services.commons.DBConstants.PartnerStatus.ACTIVE.value());
//            partner.setUpdateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
//
//            partner = this.partnerEntityDBService.addPartner(partner);
//
//            if (partner == null) {
//              throw new APIException(HttpStatus.BAD_REQUEST,
//                  APIResponse.error(HeaderCode.FAILED_TO_ADD_PARTNER_DETAILS));
//            }
//        	}
//            // Check if the Merchant has an Bank Account added if so, then add corresponding entries
//            // in Partner Account table
//
//            BankAccount bankaccount =
//                this.bankDBService.getPrimaryMerchantBankAccount(updateMerchant.getId());
//
//            if (bankaccount != null
//                && DBConstants.BankAccountStatus.ACTIVE.equals(bankaccount.getStatus())) {
//              LOG.info("Merchant Bank Accounts details are fetched " + bankaccount);
//
//              addPartnerAccounts(bankaccount, null, partner,
//                  PartnerBankAccountCodes.PARTNER_ACCOUNT.value());
//              addPartnerAccounts(bankaccount, null, partner,
//                  PartnerBankAccountCodes.PARTNER_PRINCIPAL_ACCOUNT.value());
//
//              List<PartnerAccount> partnerAccounts =
//                  this.partnerAccountDBService.getPartnerAccountByCodes(
//                      Arrays.asList(PartnerBankAccountCodes.BRONTOO_COMMISSION.value(),
//                          PartnerBankAccountCodes.BRONTOO_GST.value(),
//                          PartnerBankAccountCodes.BRONTOO_SETTLEMENT_ACCOUNT.value()));
//              if (!CollectionUtils.isEmpty(partnerAccounts)) {
//                for (PartnerAccount partnerAccount : partnerAccounts) {
//                  if (PartnerBankAccountCodes.BRONTOO_COMMISSION.value() == partnerAccount
//                      .getCode()) {
//                    PartnerAccount pAccount = this.partnerAccountDBService.findByPartnerIdAndCode(
//                        partner.getId(), PartnerBankAccountCodes.BRONTOO_COMMISSION.value());
//                    if (pAccount != null) {
//                      continue;
//                    }
//                  } else if (PartnerBankAccountCodes.BRONTOO_GST.value() == partnerAccount
//                      .getCode()) {
//                    PartnerAccount pAccount = this.partnerAccountDBService.findByPartnerIdAndCode(
//                        partner.getId(), PartnerBankAccountCodes.BRONTOO_GST.value());
//                    if (pAccount != null) {
//                      continue;
//                    }
//                  } else if (PartnerBankAccountCodes.BRONTOO_SETTLEMENT_ACCOUNT
//                      .value() == partnerAccount.getCode()) {
//                    PartnerAccount pAccount =
//                        this.partnerAccountDBService.findByPartnerIdAndCode(partner.getId(),
//                            PartnerBankAccountCodes.BRONTOO_SETTLEMENT_ACCOUNT.value());
//                    if (pAccount != null) {
//                      continue;
//                    }
//                  }
//                  addPartnerAccounts(null, partnerAccount, partner, partnerAccount.getCode());
//                }
//              }
//            }
//          }                     
//          for (User merchantUser : merchantSuperAdmins) {
//            if (sendEmail && merchantUser.getIsEmailVerified() == BooleanStatus.YES.value()
//                && !emailUtils.sendMerchantApprovalEmail(merchantUser)) {
//              LOG.error("Failed to send email to user " + user.getId());
//            }
//
//            if (!applicationProperties.getAwsSMSClient() && 
//                !smsClient.sendSms(merchantUser.getPhone(), Templates.MerchantApproval.format(),
//                httpServletContext.getClientFeatureFlags().smsEnabled())) {
//              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                  .message("Failed to send SMS").data("phone", merchantUser.getPhone())
//                  .data("template", Templates.MerchantApproval.format()).format());
//            }
//            
//            if (applicationProperties.getAwsSMSClient()) {
//
//              NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
//              notificationRequestContext.setTraceId(httpServletContext.getTraceId());
//              if (!notificationResource.sendSms(merchantUser.getPhone(),
//                  Templates.MerchantApproval.format(),
//                  httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {
//                
//                LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                    .message("Failed to send SMS").data("phone", merchantUser.getPhone())
//                    .data("template", Templates.MerchantApproval.format()).format());
//
//              }
//            }
//          }
//      }
//      }
//
//      else {
//        bank.setUpdateTime(now);
//        bank.setUser(user);
//      
//     // virtual bank user is our internal customer for BBPS at Branch
//     // adding it for add customer account
//        
//     User virtualBankUser = null;
//     
//     // if bank's api customization contains bbps @ branch the virtual bank user must be added
//     if (PartnerApprovalLevels.IN_REVIEW.equals(status)) {
//       if (StringUtils.isNotEmpty(bank.getApiCustomization()) && (bank.getApiCustomization()
//           .contains(DBConstants.ApiCustomization.BRANCH_RECHARGE_BILLPAYMENTS_BASIC.toString()) || bank.getApiCustomization()
//               .contains(DBConstants.ApiCustomization.BRANCH_RECHARGE_BILLPAYMENTS_FUND_TRANSFER.toString()))) {
//         virtualBankUser = updateVirtualBankUser(user, bank);
//       }
//     }
//        if (PartnerApprovalLevels.APPROVED.equals(status)) {
//          
//          String yearFromNow =
//              String.valueOf(DateTime.now(DateTimeZone.UTC).plusYears(1).getMillis());
//
//          List<Integer> partnerStatus =
//              Arrays.asList(BankStatus.DEACTIVATED.value(), BankStatus.DELETED.value());
//
//          bank.setStatus(BankStatus.ACTIVE.value());
//
//          if (bank.getActivationEndDate() == null
//              || Long.parseLong(bank.getActivationEndDate()) < Long.parseLong(now)
//              || partnerStatus.contains(bank.getStatus())) {
//
//            bank.setActivationStartDate(now);
//            bank.setActivationEndDate(yearFromNow);
//          }
//
//          updateInternalUser(user, null, bank);
//         
//          PartnerCredential credential =  partnerDBService.getCredentialByClientId(bank.getReferenceId());
//          if(credential != null) {
//            credential.setStatus(DBConstants.PartnerCredentialStatus.ACTIVE.value());
//            
//            credential = partnerDBService.updateCredential(credential);
//            if (credential == null) {
//              throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//                  APIResponse.error(HeaderCode.FAILED_TO_ADD_OR_UPDATE_CREDENTIALS));
//            }
//          }
//          
//        } else {
//          bank.setStatus(BankStatus.REVIEW_IN_PROGRESS.value());
//        }
//
//        updateBank = this.bankDBService.asyncUpdateBank(bank).get();
//        if (updateBank == null) {
//          throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//              APIResponse.error(HeaderCode.INTERNAL_ERROR));
//        }  
//
//      String traceId = httpServletContext.getTraceId();
//      // when merchant or bank is getting active then we call offline job to make a folder on sftp
//      // area.
//      // as of now offline code not merged yet so command
//      // if (updateMerchant != null &&
//      // DBConstants.MerchantStatus.ACTIVE.equals(updateMerchant.getStatus())) {
//      // offlineJobClient.initPartnerSetupRequest(updateMerchant, null, traceId);
//      // }
//      //
//      // if (updateBank != null && DBConstants.BankStatus.ACTIVE.equals(updateBank.getStatus())) {
//      // offlineJobClient.initPartnerSetupRequest(null, updateBank, traceId);
//      // }
//
//      } 
//  }
//  }
  

  /*
   * 
   * 
   */

  public PartnerApprovalStatus getPartnerApproval(Merchant merchant, Bank bank) throws Exception {

    PartnerApprovalStatus partnerApprovalStatus = new PartnerApprovalStatus();
    PartnerApproval partnerApproval = this.partnerDBService.getPartnerApproval(merchant, bank);
    if (partnerApproval != null) {
      partnerApprovalStatus =
          objectMapper.readValue(partnerApproval.getApprovalLevels(), PartnerApprovalStatus.class);
    }
    return partnerApprovalStatus;
  }

//  protected void updateInternalUser(User user, Merchant merchant, Bank bank)
//      throws APIException, Exception {
//
//    // init internal user
//    User internalUser =
//        APIUtils.populatePartnerInternalUser(httpServletContext, user, merchant, bank);
//    if (internalUser == null) {
//      throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//          APIResponse.error(HeaderCode.INTERNAL_ERROR));
//    }
//    // fetch existing internal user
//    List<User> existingInternalUsers = this.userDBService.getUsersByRoles(internalUser.getEmail(),
//        Arrays.asList(internalUser.getRole()), null);
//    User existingInternalUser =
//        !CollectionUtils.isEmpty(existingInternalUsers) ? existingInternalUsers.get(0) : null;
//
//    // If internal user already exists, set values and update
//    // If not add new internal user created
//    if (existingInternalUser != null) {
//      internalUser = existingInternalUser;
//      internalUser.setStatus(UserStatus.ACTIVE.value());
//      internalUser.setUpdateDate(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
//    }
//    if (this.userDBService.updateUser(internalUser) == null) {
//      throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//          APIResponse.error(HeaderCode.INTERNAL_ERROR));
//    }
//  }

  protected void setPartnerApprovalStatus(PartnerApprovalStatus partnerApprovalStatus) {

    partnerApprovalStatus.setLead(partnerApprovalStatus.getApproval());
    partnerApprovalStatus.setForm(partnerApprovalStatus.getApproval());
    partnerApprovalStatus.setDocument(partnerApprovalStatus.getApproval());
    partnerApprovalStatus.setBankAccount(partnerApprovalStatus.getApproval());
    partnerApprovalStatus.setPayment(partnerApprovalStatus.getApproval());
    partnerApprovalStatus.setIntegration(partnerApprovalStatus.getApproval());

  }

  protected void setPartnerApprovalStatus(PartnerApprovalStatus partnerApprovalStatus,
      PartnerApprovalFields submitType) {

    if (PartnerApprovalFields.FORM.equals(submitType))
      partnerApprovalStatus.setForm(partnerApprovalStatus.getApproval());
    if (PartnerApprovalFields.DOCUMENT.equals(submitType))
      partnerApprovalStatus.setDocument(partnerApprovalStatus.getApproval());
    if (PartnerApprovalFields.BANK_ACCOUNT.equals(submitType))
      partnerApprovalStatus.setBankAccount(partnerApprovalStatus.getApproval());
    if (PartnerApprovalFields.PAYMENT.equals(submitType))
      partnerApprovalStatus.setPayment(partnerApprovalStatus.getApproval());
    if (PartnerApprovalFields.INTEGRATION.equals(submitType))
      partnerApprovalStatus.setIntegration(partnerApprovalStatus.getApproval());
  }
  
//  public User updateVirtualBankUser(User user, Bank bank)
//      throws APIException, Exception {
//
//    // init internal user
//    User virtualBankUser =
//        APIUtils.populateVirtualBankUser(httpServletContext, user, bank);
//    if (virtualBankUser == null) {
//      throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//          APIResponse.error(HeaderCode.INTERNAL_ERROR));
//    }
//    // fetch existing internal user
//    List<User> existingInternalUsers = this.userDBService.getUsersByRoles(virtualBankUser.getEmail(),
//        Arrays.asList(virtualBankUser.getRole()), null);
//    User existingInternalUser =
//        !CollectionUtils.isEmpty(existingInternalUsers) ? existingInternalUsers.get(0) : null;
//
//    // If internal user already exists, set values and update
//    // If not add new internal user created
//    if (existingInternalUser != null) {
//      virtualBankUser = existingInternalUser;
//      virtualBankUser.setStatus(UserStatus.ACTIVE.value());
//      virtualBankUser.setUpdateDate(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
//    }
//    
//    if (this.userDBService.updateUser(virtualBankUser) == null) {
//      throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//          APIResponse.error(HeaderCode.INTERNAL_ERROR));
//    }
//    
//    return virtualBankUser;
//  }
  
  public Bank getBank(String bankRefId) {
    return this.bankDBService.getBank(bankRefId);
  }
  
  public void addPartnerAccounts(BankAccount bankaccount, PartnerAccount  partnerAccount, Partner partner, int code) throws APIException {
    
    PartnerAccount account = new PartnerAccount();
    account.setAccountHolderName(bankaccount != null ? bankaccount.getAccountHolderName() : partnerAccount.getAccountHolderName());
    account.setAccountNumber(bankaccount != null ? bankaccount.getAccountNumber() : partnerAccount.getAccountNumber());
    account.setAccountType(bankaccount != null ? bankaccount.getAccountType() : partnerAccount.getAccountType());
    account.setIFSCCode(bankaccount != null ? bankaccount.getIFSCCode() : partnerAccount.getIFSCCode());
    account.setIsPrimaryAccount(bankaccount != null ? bankaccount.getIsPrimaryAccount() : partnerAccount.getIsPrimaryAccount());
    account.setPartner(partner);
    account.setParentPartnerId(partner.getId());
    account.setStatus(PartnerAccountStatus.ACTIVE.value());
    account.setCode(code);
    account.setAmountBalance(BigDecimal.ZERO);
    account.setAmountOnHold(BigDecimal.ZERO);
    account.setAmountToRefund(BigDecimal.ZERO);
    account.setAmountNonReconciled(BigDecimal.ZERO);
    account.setAmountPenalty(BigDecimal.ZERO);
    account.setAmountSettlement(BigDecimal.ZERO);

    PartnerAccount partneraccount = this.partnerAccountDBService.addPartnerAccount(account);

    if (partneraccount == null) {
      throw new APIException(HttpStatus.BAD_REQUEST,
          APIResponse.error(HeaderCode.FAILED_TO_ADD_PARTNER_ACCOUNT_DETAILS));
    }
  }
}
