/**
 * 
 */
package com.dipcoin.api.resource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APICustomization;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.BankInfoResponse;
import com.dipcoin.api.model.ChargebackResponse;
import com.dipcoin.api.model.MerchantDipcoinRequest;
import com.dipcoin.api.model.MerchantInfoResponse;
//import com.dipcoin.api.model.MerchantRegisterRequest;
import com.dipcoin.api.model.MerchantTagResponse;
//import com.dipcoin.api.model.MerchantUpdateRequest;
import com.dipcoin.api.model.Pagination;
import com.dipcoin.api.model.PartnerApprovalStatus;
import com.dipcoin.api.model.PartnerPaymentRequest;
//import com.dipcoin.api.model.PaymentDetailsResponse;
import com.dipcoin.api.model.UploadedDocResponse;
import com.dipcoin.api.model.UserDeviceInfoResponse;
import com.dipcoin.api.model.UserInfoResponse;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.ObjectDataStore;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.commons.qrcode.QRCodeUtils;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.PartnerDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BankStatus;
import com.dipcoin.db.services.commons.DBConstants.BankTollStatus;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantAmountPaid;
import com.dipcoin.db.services.commons.DBConstants.MerchantDocStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantDocType;
import com.dipcoin.db.services.commons.DBConstants.MerchantStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantType;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalFields;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalLevels;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.UserStatus;
import com.dipcoin.db.services.dao.TollTagDao;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankAccount;
import com.dipcoin.db.services.model.Chargeback;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.MerchantDoc;
import com.dipcoin.db.services.model.MerchantOnboard;
import com.dipcoin.db.services.model.PartnerApproval;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.db.services.model.UserDevice;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.db.services.PartnerAccountDBService;
import com.dipcoin.partner.db.services.PartnerEntityDBService;
import com.dipcoin.partner.db.services.PartnerTransactionDBService;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerTransactionRequestType;
import com.dipcoin.partner.db.services.model.Partner;
import com.dipcoin.partner.db.services.model.PartnerAccount;
import com.dipcoin.partner.db.services.model.PartnerTransaction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
@Component("merchantResource")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class MerchantResource extends PartnerResource {

  private static final Logger LOG = LogManager.getLogger(MerchantResource.class);
  private final static ObjectMapper objectMapper = new ObjectMapper();
  private static final TimeZone dateTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
  public static final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss aa");
  
  @Autowired
  private UserDBService userDBService;

  @Autowired
  private MerchantDBService merchantDBService;

//  @Autowired
//  private PartnerApprovalHelper partnerApprovalHelper;

  @Autowired
  private CryptoUtil cryptoUtil;

//  @Autowired
//  private ObjectDataStore objectDataStore;
//
//  @Autowired
//  private ApplicationProperties applicationProperties;

  @Autowired
  private ChargebackResource chargebackResource;

//  @Autowired
//  MiscellaneousResource miscellaneousResource;
  
//  @Autowired
//  private PartnerEntityDBService partnerEntityDBService;
//  
//  @Autowired
//  private PartnerAccountDBService partnerAccountDBService;
  
  @Autowired
  private PartnerDBService partnerDBService;

  @Autowired
  private BankDBService bankDBService;
  
//  @Autowired
//  private EmailUtils emailUtils;
//
//  @Autowired
//  private SmsClient smsClient;
//  
//  @Autowired
//  private UserUtil userUtil;
  
//  @Autowired
//  private NotificationResource notificationResource;
  
  @Autowired
  private TollDBService tollDBService;
  
//  @Autowired
//  private PartnerTransactionDBService partnerTransactionDBService;
  
  @Autowired
  TollTagDao tollTagDao;
  

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

	
	 /*
	   * Get Merchant Info
	   */
	  public ResponseEntity getMerchant(final User user, final Integer merchantId) throws Exception {
		  return getMerchant(user, merchantDBService.getMerchant(merchantId));
	  }

	  public ResponseEntity getMerchant(final User user, final Merchant merchant) throws Exception {

	    if (user != null && merchant != null) {
	      if ((!this.userDBService.isMerchantSuperAdmin(user)
	          && !this.userDBService.isMerchantAdmin(user)
	    		  && !this.userDBService.isMerchantTransactor(user))
	          && (!this.userDBService.isBrontooSuperAdmin(user)
	              && !this.userDBService.isBrontooAdmin(user))) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
	      }
	      MerchantInfoResponse info = new MerchantInfoResponse();
	      MerchantOnboard merchantOnboard =
	          this.merchantDBService.getMerchantOnboardByReferenceId(merchant.getReferenceId());
	      if (merchantOnboard == null) {
	        info.addHeaderCode(HeaderCode.INTERNAL_ERROR);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(info);
	      }

	      PartnerApprovalStatus partnerApprovalStatus =
	          this.getPartnerApproval(merchant, null);
	      if (partnerApprovalStatus != null) {
	        info.setPartnerApprovalStatus(partnerApprovalStatus);
	      }
	      
	      List<Bank> fastagBanks = bankDBService.getBankByTollStatusAndStatus(BankTollStatus.ACTIVE.value(), BankStatus.ACTIVE.value());
	      if(CollectionUtils.isEmpty(fastagBanks)) {
	    	  info.addHeaderCode(HeaderCode.TOLL_BANK_STATUS_NOT_ACTIVE);
	          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(info);
	      }
	      List<Bank> merchantFastagBank = null;
	      if(StringUtils.isNotEmpty(merchant.getBankId())) {
	    	    merchantFastagBank = this.bankDBService.findBanksByIds(
	   				Arrays.stream(merchant.getBankId().split(",")).map(Integer::parseInt).collect(Collectors.toList()),
	   				BankStatus.ACTIVE.value(), BankTollStatus.ACTIVE.value());
	      }   
			
	      populateMerchantInfoResponse(merchant, merchantOnboard, info, fastagBanks, merchantFastagBank);

	      return ResponseEntity.ok(info);
	    }

	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
	  }
	  
	  public PartnerApprovalStatus getPartnerApproval(Merchant merchant, Bank bank) throws Exception {

		    PartnerApprovalStatus partnerApprovalStatus = new PartnerApprovalStatus();
		    PartnerApproval partnerApproval = this.partnerDBService.getPartnerApproval(merchant, bank);
		    if (partnerApproval != null) {
		      partnerApprovalStatus =
		          objectMapper.readValue(partnerApproval.getApprovalLevels(), PartnerApprovalStatus.class);
		    }
		    return partnerApprovalStatus;
		  }

		

	public ResponseEntity getMerchantDipcoinRequestQRCode(final User user, final Merchant merchant,
			final MerchantDipcoinRequest createReq, Integer width, Integer height, String image) throws Exception {

		ResponseEntity response = getMerchantDipcoinRequest(user, merchant, createReq);
		if (response.getStatusCode() != HttpStatus.OK) {
			return response;
		}

		final byte[] data = QRCodeUtils.generateQRCode(response.getBody(), width, height, "merchant_osta_request",
				image);

		return APIUtils.generateMultiPartResponse(data, "merchant_osta_request." + image);

	}

	public ResponseEntity getMerchantDipcoinRequest(final User user, final Merchant merchant,
			final MerchantDipcoinRequest createReq) throws Exception {

		if (!this.userDBService.merchantRepresentative(user) || !this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}
		if (createReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching User Device")
				.data("user", user.getId()).format());
		List<UserDevice> devices = userDBService.asyncGetUserDevicesByTypes(user.getId(), null).get();
		if (!CollectionUtils.isEmpty(devices)) {
			// @TODO - choose UserDevice associated with merchant only.
			UserDevice device = devices.get(0);

			UserDeviceInfoResponse info = new UserDeviceInfoResponse();
			info.setDeviceType(device.getDeviceType());
			info.setImeiNo(device.getImeiNo());
			info.setRegistrationToken(device.getRegistrationToken());

			// @TODO - Move this encryption to per merchant basis
			// @NOTE - For decryption refer to CustomerDipcoinResource.processDipcoin
			createReq.setDeviceHash(APIUtils.encryptUserDeviceInfo(cryptoUtil, info));
		}
		createReq.setPartnerReferenceId(merchant.getReferenceId());

		return ResponseEntity.ok(createReq);
	}
	
	 public ResponseEntity getMerchantAmount(User user, Integer merchantId) {
		 
		 Merchant merchant = merchantDBService.getMerchant(merchantId);
			if (null == user || null == merchant) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
			}
			
			MerchantTagResponse merchantTagResponse = new MerchantTagResponse();
		
			BigDecimal totalAmount =  BigDecimal.ZERO;
			
			BigDecimal registrationAmount = this.tollDBService.sumOfMerchantRegistrationAmount(
					Integer.valueOf(merchant.getId()), Arrays.asList(MerchantAmountPaid.PENDING_BOTH_AMOUNT.value()));

			BigDecimal minimumAmount = this.tollDBService.sumOfMerchantMinimumAmount(Integer.valueOf(merchant.getId()),
					Arrays.asList(MerchantAmountPaid.PENDING_BOTH_AMOUNT.value()));
			
			
			Long countOfTags = this.tollDBService.countOfMerchantTags(Integer.valueOf(merchant.getId()),
					Arrays.asList(MerchantAmountPaid.PENDING_BOTH_AMOUNT.value()));
			
			if(null != registrationAmount && null != countOfTags) {
				
				totalAmount = registrationAmount.add(minimumAmount);
				
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("CHECK AMOUNT")
						.data("totalAmount", totalAmount).data("registrationAmount", registrationAmount)
						.data("minimumAmount", minimumAmount).data("merchantId", merchant.getId()).format());
			}
			
			merchantTagResponse.setTotalRegisteredTagCount(countOfTags);
			merchantTagResponse.setTotalAmount(totalAmount != null?totalAmount.doubleValue():0);
			merchantTagResponse.setTotalMinimumAmount(minimumAmount != null ? minimumAmount.doubleValue():0);
			merchantTagResponse.setTotalRegistrationAmount( registrationAmount != null ? registrationAmount.doubleValue():0);
			
			return ResponseEntity.ok().body(merchantTagResponse);
		}
	 
	 
	  /*
	   * 
	   */
	  public ResponseEntity getMerchantDocs(final User user, final Merchant merchant) {
	    // verify user is active and a merchant admin
	    if ((!this.userDBService.isActive(user)) || ((!this.userDBService.isMerchantSuperAdmin(user))
	        && (!this.userDBService.isMerchantAdmin(user))
	        && (!this.userDBService.isBrontooSuperAdmin(user))
	        && (!this.userDBService.isBrontooAdmin(user)))) {
	      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
	    }

	    final List<MerchantDoc> docs = this.merchantDBService.getMerchantDocs(merchant.getId(), null);
	    if (CollectionUtils.isEmpty(docs)) {
	      return ResponseEntity.status(HttpStatus.OK)
	          .body(APIResponse.error(HeaderCode.FILE_DOESNT_EXIST));
	    }

	    final Map<Integer, User> userLookup =
	        APIUtils.getUserLookup(userDBService.getBankMerchantUsers(user.getBankMerchantId(), null));
	    List<APIResponse> responses = new LinkedList<>();
	    for (MerchantDoc doc : docs) {
	      UploadedDocResponse response = new UploadedDocResponse();
	      response.setId(doc.getDocId());
	      response.setLastUpdated(Long.parseLong(doc.getLastUpdatedDate()));
	      response.setStatus(doc.getStatus());
	      response.setType(doc.getDocType());

	      if (userLookup.containsKey(doc.getUploadedBy())) {
	        UserInfoResponse uploadedBy = new UserInfoResponse();
	        User admin = userLookup.get(doc.getUploadedBy());
	        uploadedBy.setFname(admin.getFirstName());
	        uploadedBy.setLname(admin.getLastName());
	        uploadedBy.setRole(admin.getRole());
	        response.setUploadedBy(uploadedBy);
	      }

	      responses.add(response);
	    }

	    return ResponseEntity.ok(responses);
	  }
	  
	  
	  public ResponseEntity getOnboardFeesForMerchant(final User user, Merchant merchant)
		      throws Exception, APIException {
		    if (user == null && merchant == null) {
		      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		          .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		    }
		    if ((!this.userDBService.isMerchantSuperAdmin(user)
		        && !this.userDBService.isMerchantAdmin(user))
		        && (!this.userDBService.isBrontooSuperAdmin(user)
		            && !this.userDBService.isBrontooAdmin(user))) {

		      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
		          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		    }
		    MerchantInfoResponse info = new MerchantInfoResponse();
		    MerchantOnboard merchantOnboard = null;
		    
		      merchant = this.merchantDBService.getMerchant(merchant.getId());
		      if (merchant == null) {
		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		      }

		      merchantOnboard = this.merchantDBService.getMerchantOnboardByReferenceId(merchant.getReferenceId());
		      if (merchantOnboard == null) {
		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		            .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		      }
		      info.setSetupFeeAmount(merchantOnboard.getSetupFeeAmount().toString());
		      info.setAmcFeeAmount(merchantOnboard.getAMCFeeAmount().toString());
		      info.setPerTranFeeAmount(merchantOnboard.getPerTranFeeAmount().toString());
		 
		    return ResponseEntity.ok(info);
		  }

//	  /*
//	   * Accept payment details for merchant.
//	   */
//	  public ResponseEntity addPaymentDetails(User user, Merchant merchant,
//	      PartnerPaymentRequest request) throws APIException, Exception {
//
//	    if (user == null || merchant == null) {
//	      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//	          .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
//	    }
//
//	    if (!this.userDBService.isActive(user)) {
//	      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//	          .body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
//	    }
//
//	    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("UserId", user.getId())
//	        .format());
//
//	    if (!(this.userDBService.isMerchantAdmin(user)
//	        || this.userDBService.isMerchantSuperAdmin(user)
//	        || this.userDBService.isBrontooEditor(user)
//	        || this.userDBService.isBrontooSuperAdmin(user)
//	        || this.userDBService.isBrontooAdmin(user))) {
//	      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
//	    }
//
//	    return addPaymentDetails(user, null, merchant, request);
//	  }

	
	 /*
	   * 
	   */
		public static void populateMerchantInfoResponse(final Merchant merchant, final MerchantOnboard merchantOnboard,
				MerchantInfoResponse response, List<Bank> fastagBank, List<Bank> merchantFastagBank)
				throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException,
				JsonMappingException, JsonProcessingException {

			if (merchant != null && merchantOnboard != null) {

				if (response == null)
					response = new MerchantInfoResponse();

				response.setActivationEndDate(merchantOnboard.getActivationEndDate());
				response.setActivationStartDate(merchantOnboard.getActivationStartDate());
				response.setAmcFeeAmount(merchantOnboard.getAMCFeeAmount().toString());
				response.setApplyAmcFee(merchantOnboard.getApplyAMCFee());
				response.setApplyPerTranFee(merchantOnboard.getApplyPerTranFee());
				response.setApplySetUpFee(merchantOnboard.getApplySetUpFee());
				response.setBillingAddressLine1(merchantOnboard.getBillingAddressLine1());
				response.setBillingAddressLine2(merchantOnboard.getBillingAddressLine2());
				response.setBillingAddressLine3(merchantOnboard.getBillingAddressLine3());
				response.setBillingAddressLine4(merchantOnboard.getBillingAddressLine4());
				response.setBillingCity(merchantOnboard.getBillingCity());
				response.setBillingCountry(merchantOnboard.getBillingCountry());
				response.setBillingDistrict(merchantOnboard.getBillingDistrict());
				response.setBillingFaxNumber(merchantOnboard.getBillingFaxNumber());
				response.setBillingPinCode(merchantOnboard.getBillingPinCode());
				response.setBillingState(merchantOnboard.getBillingState());
				response.setBoardNumber(merchantOnboard.getBoardNumber());
				response.setBusinessCategory(merchantOnboard.getBusinessCategory());
				response.setBusinessType(merchantOnboard.getBusinessType());
				response.setChannelType(merchantOnboard.getChannelType());
				// response.setClientTransactionId(merchantOnboard.getClientTransactionId());
				response.setCurrency(merchantOnboard.getCurrency());
				response.setDescription(merchantOnboard.getDescription());
				response.setMerchantAlias(merchantOnboard.getAlias());
				response.setEmailAlertActivation(merchantOnboard.getEmailAlertActivation());
				response.setEmailId(merchantOnboard.getEmailId());
				response.setFeeChargeMethod(merchantOnboard.getFeeChargeMethod());
				response.setIntegrationType(merchantOnboard.getIntegrationType());
				response.setLogoURL(merchantOnboard.getLogoURL());
				response.setMobileAppActivation(merchantOnboard.getMobileAppActivation());
				response.setMultiUserAccess(merchantOnboard.getMultiUserAccess());
				response.setName(merchantOnboard.getName());
				response.setNumberOfUsers(merchantOnboard.getNumberOfUsers());
				response.setNumOfEmp(merchantOnboard.getNumOfEmp());
				response.setOfficeAddressLine1(merchantOnboard.getOfficeAddressLine1());
				response.setOfficeAddressLine2(merchantOnboard.getOfficeAddressLine2());
				response.setOfficeAddressLine3(merchantOnboard.getOfficeAddressLine3());
				response.setOfficeAddressLine4(merchantOnboard.getOfficeAddressLine4());
				response.setOfficeCity(merchantOnboard.getOfficeCity());
				response.setOfficeCountry(merchantOnboard.getOfficeCountry());
				response.setOfficeDistrict(merchantOnboard.getOfficeDistrict());
				response.setOfficeFaxNumber(merchantOnboard.getOfficeFaxNumber());
				response.setOfficeNumber(merchantOnboard.getOfficeNumber());
				response.setOfficePincode(merchantOnboard.getOfficePincode());
				response.setOfficeState(merchantOnboard.getOfficeState());
				response.setPerTranFeeAmount(merchantOnboard.getPerTranFeeAmount().toString());
				response.setPartnerReferenceId(merchantOnboard.getReferenceId());
				response.setSettlementCycle(merchantOnboard.getSettlementCycle());
				response.setSettlementType(merchantOnboard.getSettlementType());
				response.setSetupFeeAmount(merchantOnboard.getSetupFeeAmount().toString());
				response.setSMSAlertActivation(merchantOnboard.getSMSAlertActivation());
				response.setStatus(merchant.getStatus());
				response.setType(merchantOnboard.getType());
				response.setUrl(merchantOnboard.getUrl());
				response.setYearofEstablishment(merchantOnboard.getYearofEstablishment());
				response.setPartnerType(merchantOnboard.getPartnerType());
				response.setBusinessSegment(merchantOnboard.getBusinessSegment());
				response.setCustomReportUI(merchantOnboard.getCustomReportUI());
				if (!StringUtils.isBlank(merchantOnboard.getApiCustomization())) {
					Object alpha = objectMapper.readValue(merchantOnboard.getApiCustomization(), Object.class);
					response.setApiCustomization((List<APICustomization>) alpha);

				}

				if (!CollectionUtils.isEmpty(fastagBank)) {
					ArrayList<BankInfoResponse> bankInfoResponseList = new ArrayList<>();
					
					for (Bank bank : fastagBank) {
						boolean matchFound = false;
						if (!CollectionUtils.isEmpty(merchantFastagBank)) {
							for (Bank merchantBank : merchantFastagBank) {
								if(merchantBank.getId() == bank.getId()) {
									matchFound =true;
								}						
							}						
							if(matchFound) {
								BankInfoResponse bankInfoResponse = new BankInfoResponse();
								bankInfoResponse.setName(bank.getName());
								bankInfoResponse.setPartnerReferenceId(bank.getReferenceId());
								bankInfoResponse
										.setMerchantTollBankFlag( true);
								bankInfoResponseList.add(bankInfoResponse);
							} else {
								BankInfoResponse bankInfoResponse = new BankInfoResponse();
								bankInfoResponse.setName(bank.getName());
								bankInfoResponse.setPartnerReferenceId(bank.getReferenceId());
								bankInfoResponse
										.setMerchantTollBankFlag(false);
								bankInfoResponseList.add(bankInfoResponse);
							}

						} else {

							BankInfoResponse bankInfoResponse = new BankInfoResponse();
							bankInfoResponse.setName(bank.getName());
							bankInfoResponse.setPartnerReferenceId(bank.getReferenceId());
							bankInfoResponse.setMerchantTollBankFlag(false);
							bankInfoResponseList.add(bankInfoResponse);
						}
					}
					response.setFastagBank(bankInfoResponseList);
				}

			}
		}
		
		 public ResponseEntity getChargeBackTransactions(User user, String partnerRefId, Integer status,
			      Long startDate, Long endDate, String partner, Integer start, Integer count) {

			    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
			        .data("MerchnatUserId :-", user.getId()).format());

			    ChargebackResponse chargebackResponse = new ChargebackResponse();
			    List<Chargeback> chargebackTransaction = new ArrayList<Chargeback>();

			    if (!this.userDBService.isMerchantUser(user) && !this.userDBService.isMerchantSuperAdmin(user)
			        && !this.userDBService.isMerchantAdmin(user)) {
			      LOG.error(HeaderCode.USER_UNAUTHORIZED);

			      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
			    }
			    Merchant merchant = this.merchantDBService.getMerchant(user.getBankMerchantId());

			    if (merchant == null) {
			      LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
			          .data("Merchnat :-", HttpStatus.BAD_REQUEST).format());
			      chargebackResponse.addHeaderCode(HeaderCode.INVALID_REQUEST);

			      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(chargebackResponse);
			    }

			    chargebackTransaction = chargebackResource.fetchChargebackTransaction(merchant.getId(), null, status,
			        startDate, endDate, start, count);

			    chargebackResponse.setChargebackTransaction(chargebackTransaction);
			    return ResponseEntity.ok(chargebackResponse);

			  }

}
