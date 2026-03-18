package com.dipcoin.api.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;

import javax.crypto.Cipher;
import javax.ws.rs.core.Response.Status;
import org.redisson.api.RedissonClient;

import java.io.StringReader;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.db.services.commons.Utils;

import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.ObjectDataStore;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.TollEmailUtils;
import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.CustomerDipcoinRequest;
import com.dipcoin.api.model.CustomerDipcoinResponse;
import com.dipcoin.api.model.Detail;
import com.dipcoin.api.model.Head;
import com.dipcoin.api.model.Pagination;
import com.dipcoin.api.model.ReqVehicleDetailResponse;
import com.dipcoin.api.model.ReqVehicleDetailsRequest;
import com.dipcoin.api.model.TollNetcDetailsRequest;
import com.dipcoin.api.model.TollNetcDetailsResponse;
import com.dipcoin.api.model.TollNetcSyncTimeResponse;
import com.dipcoin.api.model.TollRechargeReceipt;
import com.dipcoin.api.model.TollRechargeRequest;
import com.dipcoin.api.model.TollRechargeResponse;
import com.dipcoin.api.model.TollRegReceiptResponse;
import com.dipcoin.api.model.TollRegistrationRequest;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.model.TollTagFeeAndChargesResponse;
import com.dipcoin.api.model.TollTagRequest;
import com.dipcoin.api.model.TollTagResponse;
import com.dipcoin.api.model.TollTagUpdateResponse;
import com.dipcoin.api.model.TopUpDetails;
import com.dipcoin.api.model.Txn;
import com.dipcoin.api.model.Vehicle;
import com.dipcoin.api.model.VehicleDetails;
import com.dipcoin.api.utils.TollMetricRegistry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.apache.commons.collections4.CollectionUtils;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.FeesAndDepositDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.TollRechargeDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionSettlementDone;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.MerchantAmountPaid;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.commons.DBConstants.TagDeliveryType;
import com.dipcoin.db.services.commons.DBConstants.TollRegistrationStatus;
import com.dipcoin.db.services.commons.DBConstants.TollTagApprovalStatus;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.VinVrn;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.FeesAndDeposit;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TollRecharge;
import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.toll.commons.TollAquirerFunctionCodes;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;
import com.dipcoin.partner.toll.commons.TollErrorCodes;
import com.dipcoin.partner.toll.commons.TollHttpsServices;
import com.dipcoin.partner.toll.commons.TollIssuerFunctionCodes;
import com.dipcoin.partner.toll.commons.TollReasonCodes;
import com.dipcoin.partner.toll.commons.TollSignatureGenerationServices;
import com.dipcoin.partner.toll.commons.TollSignatureVerificationServices;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component("tollServiceResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class TollCustomerResource {

	private static final Logger LOG = LogManager.getLogger(TollCustomerResource.class);

	private final static ObjectMapper objectMapper = new ObjectMapper();

	protected static boolean encryptCardId = false;

	private Map<String, String> vehicleRegistrationNo = new HashMap<>();

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;
	
	@Autowired
	private TollSignatureVerificationServices tollSignatureVerificationServices;
	
	@Autowired
	private EncryptionResource encryptionResource;

	@Autowired
	@Qualifier("com.dipcoin.metrics.TollMetricRegistry")
	private TollMetricRegistry tollMetricRegistry;

	@Autowired
	private TollDBService tollDBService;

	@Autowired
	private DipcoinDBService coinDBService;

	@Autowired
	private FeesAndDepositDBService feesAndDepositDBService;

	@Autowired
	private CustomerDBService customerDBService;

	@Autowired
	private BankDBService bankDBService;

	@Autowired
	private TollEmailUtils tollEmailUtils;

	@Autowired
	private ApplicationProperties applicationProperties;

	@Autowired
	private ObjectDataStore objectDataStore;

	@Autowired
	private NotificationResource notificationResource;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private MerchantDBService merchantDBService;

	@Autowired
	private TollProperties tollProperties;

	@Autowired
	private BrontooResource brontooResource;

	@Autowired
	private TollSignatureGenerationServices tollSignatureGenerationServices;

	@Autowired
	private TollHttpsServices tollHttpsServices;

	@Autowired
	private CustomerDipcoinResource customerDipcoinResource;

	@Autowired
	private TollBankResource tollServiceBankResource;

	@Autowired
	private TollRechargeResource tollRechargeResource;

	@Autowired
	private RedissonClient redissonclient;

	@Autowired
	private SmsClient smsClient;

	private RLock vehicleNumberLock;
	
	@Autowired
	TollRechargeDBService tollRechargeDBService;
	

	/*
	 * Getting tollCustomer Details of basis of USERID OR BANKCODE OR TAGID.
	 */
	public ResponseEntity getTollCustomerDetails(final User user, final String status, final String tagId)
			throws Exception, APIException {

		if (user != null) {
			// Getting the all Register user

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("list based on user id")
					.data("id", user.getId()).format());

			List<TollRegistration> listofTollCutomers = this.tollDBService
					.findTollCustomersByUserId((Integer) user.getId());

			List<APIResponse> listresponses = new ArrayList<APIResponse>();
			if (!CollectionUtils.isEmpty(listofTollCutomers)) {

				for (int i = 0; i < listofTollCutomers.size(); i++) {

					TollRegistration registration = listofTollCutomers.get(i);

					List<CustomerAccount> customerAccount = customerDBService.getAccounts(user.getId());

					if (!CollectionUtils.isEmpty(customerAccount)) {
						for (CustomerAccount customerAccountobj : customerAccount) {
							for (int j = 0; j < registration.getTollTag().size(); j++) {
								if (customerAccountobj.getBank().getId() == registration.getTollTag().get(j).getBankId()
										&& customerAccountobj.getId() == registration.getTollTag().get(j)
												.getCustomerAccountId()) {
									TollRegistrationResponse responseObj = new TollRegistrationResponse();
									responseObj.setBankName(customerAccountobj.getBank().getName());
									responseObj.setCardId(customerAccountobj.getUserCardId());

									if (customerAccountobj.getWalletBankId() != NumberUtils.INTEGER_ZERO) {
										Bank bank = bankDBService.getBank(customerAccountobj.getWalletBankId());
										if (bank != null) {
											responseObj.setWalletBankReferenceId(bank.getReferenceId());
										}
									}

									populateTollCustomersResponse(user, registration,
											Arrays.asList(registration.getTollTag().get(j)), responseObj);
									listresponses.add(responseObj);
								}
							}
						}

						// Getting the Bank Details
					}

				}
				return ResponseEntity.status(HttpStatus.OK).body(listresponses);

			}
			return ResponseEntity.status(HttpStatus.OK).body(listresponses);
		}

		if (tagId != null) {
			// Getting the all Register user
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("list based on tag id")
					.data("id", tagId).format());

			TollTag tollTag = this.tollDBService.findTollCustomersByTagId(tagId);
			if (tollTag != null) {
				TollTagResponse responseObj = new TollTagResponse();

				List<CustomerAccount> customerAccount = customerDBService.getAccounts(user.getId());

				if (!CollectionUtils.isEmpty(customerAccount)) {
					for (CustomerAccount customerAccountobj : customerAccount) {
						if (customerAccountobj.getBank().getId() == tollTag.getBankId()
								&& customerAccountobj.getId() == tollTag.getCustomerAccountId()) {
							responseObj.setCardId(customerAccountobj.getUserCardId());
						}
					}
					Bank issuerBank = bankDBService
							.getBank(tollTag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? tollTag.getWalletBankId()
									: tollTag.getBankId());

					populateTollTagDetailsResponse(user, tollTag, responseObj, issuerBank);

					return ResponseEntity.status(HttpStatus.OK).body(responseObj);
				}

				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to fetch Customer Account.").data("user id", user.getId()).format());

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.FAILED_FETCHING_CUSTOMER_ACCOUNT));

			}

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_DOESNT_EXIST));
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(APIResponse.error(HeaderCode.TAGID_IS_NOT_PRESENT_IN_DATABASE));

	}

	/*
	 * Getting the Response for Toll Customers According to BankCode
	 */
	public void populateTollCustomersResponse(final User user, final TollRegistration registration,
			final List<TollTag> tollTag, TollRegistrationResponse response) throws APIException {

		List<TollTagResponse> tollTagresponselist = new ArrayList<>();

		if (registration != null) {
			for (TollTag tolltag : tollTag) {
				TollTagResponse tollTagresponse = new TollTagResponse();
				Bank issuerBank = bankDBService
						.getBank(tolltag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? tolltag.getWalletBankId()
								: tolltag.getBankId());
				populateTollTagDetailsResponse(user, tolltag, tollTagresponse, issuerBank);
				// attaching tagId to serial Number
				Epc epc = tollDBService.findEpcByTagId(tolltag.getTagId());
				if (epc != null) {
					tollTagresponse.setTagId(epc.getSerialNumber());
				}
				tollTagresponselist.add(tollTagresponse);
			}

			response.setId(registration.getId()).setFirstName(registration.getFirstName())
					.setLastName(registration.getLastName()).setEmailId(registration.getEmailId())
					.setDob(registration.getDob()).setAddress(registration.getAddress())
					.setMobileNo(registration.getMobileNo()).setCity(registration.getCity())
					.setState(registration.getState()).setPincode(registration.getPincode())
					.setUserId(registration.getUserId()).setIdProofNo(registration.getIdProofNumber())
					.setIdProofType(registration.getIdProofType()).setCardId(response.getCardId())
					.setGender(registration.getGender()).setStatus(registration.getStatus())
					.setVehicleList(tollTagresponselist);

		}
	}

	/*
	 * Setting the Response for TollTagDetails
	 */
	public static void populateTollTagDetailsResponse(final User user, final TollTag tollTag, TollTagResponse response,
			Bank bank) throws APIException {

		if (tollTag != null) {
			response.setId(tollTag.getId());
			response.setTagDetailsId(tollTag.getId());
			response.setTagId(tollTag.getTagId());
			response.setTid(tollTag.getTid());
			response.setSerialNumber(tollTag.getSerialNumber());
			response.setBankId(tollTag.getBankId());
			response.setCustomerAccountId(tollTag.getCustomerAccountId());
			response.setIsCommercial(tollTag.getIsCommercial());
			response.setRegistrationNo(tollTag.getRegistrationNo());
			response.setRegisteredState(tollTag.getRegisteredState());
			response.setStatus(tollTag.getStatus());
			response.setRCImage(tollTag.getRCImage());
			response.setApprovalFlag(tollTag.getApprovalFlag());
			response.setApprovedBy(tollTag.getApprovedBy());
			response.setResponseTime(tollTag.getResponseTime());
			response.setApprovedDateTime(tollTag.getApprovedDateTime());
			response.setApprovedIPAddress(tollTag.getApprovedIPAddress());
			response.setAutoTopUp(tollTag.getAutoTopUp());
			response.setAddTagErrorCode(tollTag.getAddTagErrorCode());
			response.setAddTagResponse(tollTag.getAddTagResponse());
			response.setAvailableAmount(tollTag.getAvailableAmount());
			response.setCategory(tollTag.getCategory());
			response.setCategoryId(tollTag.getCategoryId());
			response.setCreatedDateTime(tollTag.getCreatedDateTime());
			response.setDepositAmount(tollTag.getDepositAmount());
			response.setExcCode(tollTag.getExcCode());
			response.setExcCodeUpdateTime(tollTag.getExcCodeUpdateTime());
			response.setExpiryDate(tollTag.getExpiryDate());
			response.setIssueDate(tollTag.getIssueDate());
			response.setMinimumAmount(tollTag.getMinimumAmount());
			response.setRechargeAmount(tollTag.getRechargeAmount());
			response.setRejectReason(tollTag.getRejectReason());
			response.setRemarks(tollTag.getRemarks());
			response.setType(tollTag.getType());
			response.setVendorIPAddress(tollTag.getVendorIPAddress());
			response.setUpdateExceptionErrorCode(tollTag.getUpdateExceptionErrorCode());
			response.setVendorUpdateBy(tollTag.getVendorUpdateBy());
			response.setUpdateExceptionResponse(tollTag.getUpdateExceptionResponse());
			response.setVendorUpdateDateTime(tollTag.getVendorUpdateDateTime());
			response.setCourierCompany(tollTag.getCourierCompany());
			response.setTrackingId(tollTag.getTrackingId());
			response.setWalletFlag(tollTag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? NumberUtils.INTEGER_ONE
					: NumberUtils.INTEGER_ZERO);
			response.setIssuerBankName(bank.getName());
		}
	}

	/*
	 * Get Toll Customer Document
	 */
	public ResponseEntity getTollCustomerDoc(final User user, final String clientTransactionId, final String docPath)
			throws APIException, IOException {

		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		if (docPath == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		String filePath = docPath;
		String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching file " + filePath).format());

		InputStream fileInputStream = null;
		fileInputStream = this.objectDataStore.get(filePath);

		if (fileInputStream == null)
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.FILE_DOESNT_EXIST));

		return APIUtils.generateMultiPartResponse(fileInputStream, fileName);

	}

	/*
	 * Getting tollCustomer Details of basis of USERID OR BANKCODE OR TAGID.
	 */
	public ResponseEntity getTollVehicleCharges(final User user, final String cardId, final String vehicleClass,
			final Integer registrationType) throws Exception, APIException {

		TollRegistrationResponse response = new TollRegistrationResponse();
		List<APIResponse> listresponses = new ArrayList<APIResponse>();
		TollTagFeeAndChargesResponse responseCharges = new TollTagFeeAndChargesResponse();

		if ((Integer) user.getId() != null) {
			// Getting the all Register user

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get the Vehicle Charges.")
					.data("cardId", cardId).data("vehicleClass", vehicleClass).format());

			if (StringUtils.isBlank(cardId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.MISSING_USER_CARDID));
			}

			if (StringUtils.isBlank(vehicleClass)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.MISSING_VEHICLE_CLASS));
			}

			// =========Get the Bank from CardID

			if (!this.userDBService.isCustomer(user)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
			}

			CustomerAccount customerAccount = this.customerDBService.getAccount(user.getId(), Integer.parseInt(cardId));

			// Card Id is not found
			if (customerAccount == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
			}

			// Check the UserId and DipcoinCreated User Id is Same
			if (user.getId() != customerAccount.getUser().getId()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
			}

			if (customerAccount.getBank() == null
					|| (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()
							&& customerAccount.getWalletBankId() == NumberUtils.INTEGER_ZERO)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
			}

			// ==========

			List<Integer> bankList = new ArrayList<Integer>();
			Set<String> chargeCategorys = new HashSet<String>();

			bankList.add(customerAccount.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()
					? customerAccount.getWalletBankId()
					: customerAccount.getBank().getId());
			chargeCategorys.add(vehicleClass);

			// @TODO WRITE THE CODE FOR FETCHING.
			List<FeesAndDeposit> feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(bankList,
					chargeCategorys);

			// Data check.
			if (CollectionUtils.isEmpty(feesAndDeposits)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(APIResponse.error(HeaderCode.BANK_CHARGES_NOT_AVAILABLE));
			}

			// Minimium amount calculation
			List<TollTag> tollTags = tollDBService.asyncFindTollTagsByCustomerAccountId(customerAccount.getId()).get();
			BigDecimal totalMinimumAmount = BigDecimal.ZERO;
			for (TollTag tolltag : tollTags) {
				if (DBConstants.TollTagApprovalStatus.ACTIVE.value() == Integer.parseInt(tolltag.getStatus())
						|| DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer
								.valueOf(tolltag.getStatus())
						|| DBConstants.TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value() == Integer
								.valueOf(tolltag.getStatus())) {
					totalMinimumAmount = totalMinimumAmount.add(tolltag.getMinimumAmount());
				}
			}
			// Setter at Account Level
			responseCharges.setMinAmountMaintainForAccount(totalMinimumAmount);

			try {

				for (int i = 0; i < feesAndDeposits.size(); i++) {

					responseCharges
							.setGstAmount(getGstAmountCalculation(RegistrationType.IHMCL.value() == registrationType
									? feesAndDeposits.get(i).getIHMCLRegistrationAmount()
									: feesAndDeposits.get(i).getRegistrationAmount()));
					populateTollChargesResponse(user, feesAndDeposits.get(i), responseCharges, registrationType);
					listresponses.add(responseCharges);
				}

			} catch (Exception e) {
				LOG.error("Failed to set values for toll tag charges and Vehicle class " + feesAndDeposits);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}

			// Construct the Response

		}

		response.addHeaderCode(HeaderCode.SUCCESS);
		return ResponseEntity.status(HttpStatus.OK).body(listresponses);

	}

	public BigDecimal getGstAmountCalculation(BigDecimal amount) {

		// BigDecimal gstAmount = amount.multiply(APIConstants.GST_TAX);
		BigDecimal plusAmount = BigDecimal.valueOf(100).add(APIConstants.GST);

		BigDecimal divideamount = APIConstants.GST.divide(plusAmount, 6, RoundingMode.HALF_UP);

		BigDecimal gstAmount = divideamount.multiply(amount);
		// round(0.1224) i.e 0.12
		BigDecimal registrationGstCalculation = gstAmount.setScale(NumberUtils.INTEGER_TWO, RoundingMode.HALF_UP);
		return registrationGstCalculation;
	}

	/*
	 * Getting the Response for Toll Customers According to BankCode
	 */
	public void populateTollChargesResponse(final User user, final FeesAndDeposit feesAndDeposit,
			TollTagFeeAndChargesResponse response, final Integer registrationType) throws APIException {

		BigDecimal registrationAmount = BigDecimal.ZERO;
		if (feesAndDeposit != null) {

			if (RegistrationType.IHMCL.value() == registrationType) {
				registrationAmount = feesAndDeposit.getIHMCLRegistrationAmount()
						.subtract(getGstAmountCalculation(feesAndDeposit.getIHMCLRegistrationAmount()));
			} else {
				registrationAmount = feesAndDeposit.getRegistrationAmount()
						.subtract(getGstAmountCalculation(feesAndDeposit.getRegistrationAmount()));
			}

			BigDecimal miscGst = getGstAmountCalculation(feesAndDeposit.getMiscellaneousCharges());
			BigDecimal courierCharges = feesAndDeposit.getMiscellaneousCharges().subtract(miscGst);

			response.setBankId(feesAndDeposit.getBankId()).setChargeCategory(feesAndDeposit.getChargeCategory())
					.setDepositAmount(feesAndDeposit.getDepositAmount())
					.setMinimumAmount(feesAndDeposit.getMinimumAmount())
					.setMinAmountMaintainForTag(feesAndDeposit.getMinimumAmount())
					.setGstAmount(response.getGstAmount().add(miscGst)).setCourierCharges(courierCharges)
					.setRegistrationAmount(registrationAmount).setCreatedOn(feesAndDeposit.getCreatedOn())
					.setUpdateOn(feesAndDeposit.getUpdateOn());
		}
	}

	public ResponseEntity vehicleVerification(final User user, final String clientTransactionId,
			String vehicleRegistrationNo, String vehicleClass, String tagId, String tid, Integer cardId,
			Integer registrationType) throws APIException, Exception {

		TollNetcDetailsRequest tollNetcDetailsRequest = new TollNetcDetailsRequest();
		TollTagResponse tollTagResponse = new TollTagResponse();
		String refUrl = StringUtils.EMPTY;
		try {
			// Checking for the clientTransactionID is null
			if (clientTransactionId == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
			}

			if (!userDBService.isCustomer(user)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
			}
			if (!this.userDBService.isActive(user)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
			}

			CustomerAccount custAccount = customerDBService.getAccount(user.getId(), cardId, false);

			if (custAccount == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.INVALID_CARD_ID));
			}

			Bank bank = custAccount.getBank();
			if (custAccount.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()) {
				if (custAccount.getWalletBankId() == NumberUtils.INTEGER_ZERO) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
				}
				bank = bankDBService.getBank(custAccount.getWalletBankId());
				if (bank == null) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
				}

			}

			List<Merchant> merchants = merchantDBService
					.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();

			if (CollectionUtils.isEmpty(merchants)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.MERCHANT_DOESNT_EXIST));
			}

			if (!tollProperties.isConnectNpci()) {
				List<TollTag> tollTags = tollDBService
						.findTollTagByRegistrationNo(Arrays.asList(vehicleRegistrationNo));
				String excCode = StringUtils.EMPTY;
				if (CollectionUtils.isNotEmpty(tollTags)) {

					for (TollTag tollTag : tollTags) {
						if (!(TollTagApprovalStatus.INACTIVE.value() == Integer.parseInt(tollTag.getStatus())
								|| TollTagApprovalStatus.BANK_REJECTED.value() == Integer.parseInt(tollTag.getStatus()))
								|| TollConstant.EXC_CODE_BLACKLIST.equalsIgnoreCase(tollTag.getExcCode())
								|| TollConstant.EXC_CODE_HOTLIST.equalsIgnoreCase(tollTag.getExcCode())) {

							tollTagResponse.addHeaderCode(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST);
							tollTagResponse.setExcCode(TollConstant.EXC_CODE_BLACKLIST);
							return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);
						}
					}

					for (TollTag tollTag : tollTags) {
						excCode = tollTag.getExcCode() + "," + excCode;
					}

					tollTagResponse.addHeaderCode(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST);
					tollTagResponse.setExcCode(excCode);
					return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);
				}

				tollTagResponse.addHeaderCode(HeaderCode.TOLL_VEHICLE_REG_NO_NOT_PRESENT);
				return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);

			}

			LOG.debug(
					LogFormatter.instance(httpServletContext.getTraceId()).message("calling toll sync time").format());

			TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();

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

			Txn txn = new Txn();

			Vehicle vehicle = new Vehicle();

			Head head = new Head();
			// head.setVer(TollConstant.VERSION);
			head.setVer(TollConstant.VER);
			SimpleDateFormat formatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
			Date date = formatter.parse(tollNetcSyncTimeResponse.getResp().getTs());
			head.setTs(tollNetcSyncTimeResponse.getResp().getTs());
			txn.setTs(tollNetcSyncTimeResponse.getResp().getTs());
			head.setOrgId(bank.getOrgId());

			formatter = new SimpleDateFormat(TollConstant.MSG_DATE_FORMAT);
			date = new Date(System.currentTimeMillis());
			head.setMsgId(bank.getOrgId() + formatter.format(date).toUpperCase());

			vehicle.setAvc(
					StringUtils.isNotBlank(vehicleClass) || vehicleClass != null ? vehicleClass : TollConstant.EMPTY);
			vehicle.setTagId(StringUtils.isNotBlank(tagId) || tagId != null ? tagId : TollConstant.EMPTY);
			vehicle.setTID(StringUtils.isNotBlank(tid) || tid != null ? tid : TollConstant.EMPTY);
			vehicle.setVehicleRegNo(StringUtils.isNotBlank(vehicleRegistrationNo) || vehicleRegistrationNo != null
					? vehicleRegistrationNo
					: TollConstant.EMPTY);

			String dipcoinReferenceNumber = CoreUtils.randomAlphaString(22);

			txn.setId(dipcoinReferenceNumber);
			txn.setNote(TollConstant.REQUEST_DETAILS_NOTE);
			txn.setOrgTxnId(bank.getOrgId() + dipcoinReferenceNumber);
			txn.setRefId(dipcoinReferenceNumber);
			txn.setRefUrl(refUrl);
			txn.setType(TollConstant.REQUEST_DETAILS_TYPE);
			txn.setVehicle(vehicle);

			tollNetcDetailsRequest.setHead(head);
			tollNetcDetailsRequest.setTxn(txn);

			// Create JAXB Context
			JAXBContext jaxbContext = JAXBContext.newInstance(TollNetcDetailsRequest.class);

			// Create Marshaller
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

			// Required formatting??
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			// Print XML String to Console
			StringWriter sw = new StringWriter();

			// Write XML to StringWriter
			jaxbMarshaller.marshal(tollNetcDetailsRequest, sw);

			// Verify XML Content
			String postData = sw.toString();

			ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
					postData.getBytes(StandardCharsets.UTF_8));

			ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices.signatureGenerationServices(
					byteArrayInputStream, httpServletContext.getTraceId(), bank.getOrgId());
			String responseData = null;

			// NPCI Active Active Setup Phase2 changes
			String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
			if (StringUtils.isEmpty(ipAddress)) {

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.NETC_NPCI_SERVER_DOWN));

			}
			int port = this.tollProperties.getNetcHealthCheckPort();

			try {
				if (tollProperties.isConnectNpci()) {
					TollHttpsServices.bankIin = bank.getIin();
					/*
					 * String url = "";
					 * 
					 * switch (registrationType) { case 0: url =
					 * tollProperties.getRequestDetailUrl(); break; case 1: url =
					 * tollProperties.getRequestDetailIHMCL(); break; default: url =
					 * tollProperties.getRequestDetailUrl(); break; }
					 */

					String endPoint = "";

					switch (registrationType) {
					case 0:
						endPoint = tollProperties.getRequestDetailUrl();
						break;
					case 1:
						endPoint = tollProperties.getRequestDetailIHMCL();
						break;
					default:
						endPoint = tollProperties.getRequestDetailUrl();
						break;
					}

					String url = "https://" + ipAddress + ":" + port + endPoint;
					responseData = tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);

					TollHttpsServices.bankIin = StringUtils.EMPTY;

					if (responseData == null) {
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
								.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
					}
				}
			} catch (Exception e) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Exception Caught while calling tollHttpsServices").format(), e);
			}

			TollNetcDetailsResponse tollNetcDetailsResponse = JAXB.unmarshal(new StringReader(responseData),
					TollNetcDetailsResponse.class);

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Txn Id")
					.data("Txn Id", tollNetcDetailsResponse.getTxn().getId()).format());

			List<HeaderCode> headerCodes = new ArrayList<>();
			String[] errCodes = tollNetcDetailsResponse.getTxn().getResp().getVehicle().getErrCode().split(",");
			String[] respCodes = tollNetcDetailsResponse.getTxn().getResp().getRespCode().split(",");

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

			if (registrationType == RegistrationType.DEFAULT.value()) {
				String excCode = StringUtils.EMPTY;
				if (tollNetcDetailsResponse.getTxn().getResp().getVehicle().getVehicleDetails() != null
						&& CollectionUtils.isNotEmpty(
								tollNetcDetailsResponse.getTxn().getResp().getVehicle().getVehicleDetails())) {

					for (VehicleDetails vehicleDetail : tollNetcDetailsResponse.getTxn().getResp().getVehicle()
							.getVehicleDetails()) {

						for (Detail detail : vehicleDetail.getDetail()) {

							if (detail.getName().equalsIgnoreCase(TollConstant.EXCCODE)) {
								excCode = excCode + detail.getValue() + ",";

							}

						}
					}
					if (StringUtils.isNotBlank(excCode)) {
						excCode = excCode.substring(NumberUtils.INTEGER_ZERO,
								excCode.length() - NumberUtils.INTEGER_ONE);
						tollTagResponse.setExcCode(excCode);
					}
				}

				tollTagResponse.addHeaderCodes(headerCodes);

				return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);

			} else if (registrationType == RegistrationType.IHMCL.value() && !HeaderCode.VEHICLE_REG_NO_NOT_IN_DB.code()
					.contains(tollNetcDetailsResponse.getTxn().getResp().getVehicle().getErrCode())) {

				return ResponseEntity.status(HttpStatus.OK).body(tollNetcDetailsResponse);

			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.errors(headerCodes));

		} catch (Exception ex) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("vehicle verification" + ex)
					.format());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));

	}

	public ResponseEntity reqVehicleDetails(String vrn, String vin, String last5digitofenginNumber, Integer bankId,
			String bankReferenceId) {

		TollNetcDetailsResponse tollNetcDetailsResponse = new TollNetcDetailsResponse();
		ReqVehicleDetailResponse reqVehicleDetailResp = new ReqVehicleDetailResponse();
		String responseData = null;
		String last5digitofengineno = null;

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("vrn" + vrn)
				.data("last5digitofengineno", last5digitofenginNumber).data("vin", vin).format());

		if (StringUtils.isNotEmpty(last5digitofenginNumber) && last5digitofenginNumber.length() > 5) {
			last5digitofengineno = last5digitofenginNumber.substring(last5digitofenginNumber.length() - 5);

		} else {
			last5digitofengineno = last5digitofenginNumber;
		}

		String refUrl = StringUtils.EMPTY;
		Bank bank = new Bank();
		if (StringUtils.isNotEmpty(bankReferenceId)) {
			bank = bankDBService.getBank(bankReferenceId);
		} else {
			bank = bankDBService.getBank(bankId);
		}
		if (bank == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("bank is null").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("calling toll sync time").format());
		try {
			TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();

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
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.data("last5digitofengineno is not blank", last5digitofengineno).format());
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("vin is not blank ", vin).format());

			responseData = this.callNETCForReqVehicleDetails(vrn, vin, last5digitofengineno, bank, refUrl,
					tollNetcSyncTimeResponse);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseData);
	}

	/*
	 * reVehicleDetails 1.1 This API is called by Issuer Bank to get the details of
	 * registered Tag ID using VIN & last5digitofengineno OR using VRN &
	 * last5digitofengineno. Privilege: Initiated by Issuer Bank
	 */
	public String callNETCForReqVehicleDetails(String vrn, String vinNumber, String last5digitofengineno, Bank bank,
			String refUrl, TollNetcSyncTimeResponse tollNetcSyncTimeResponse) throws Exception {

		TollNetcSyncTimeResponse tollNetcSyncTimeResponse1 = new TollNetcSyncTimeResponse();

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("vrn" + vrn)
				.data("last5digitofengineno", last5digitofengineno).data("vinNumber", vinNumber).format());

		if (tollNetcSyncTimeResponse == null) {
			tollNetcSyncTimeResponse1 = new TollNetcSyncTimeResponse();

			ResponseEntity responseEntity = this.brontooResource.syncTime(bank);
			if (responseEntity.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				return null;
			}
			tollNetcSyncTimeResponse1 = (TollNetcSyncTimeResponse) responseEntity.getBody();

			tollNetcSyncTimeResponse = tollNetcSyncTimeResponse1;
		}

		if (refUrl == null) {
			String[] bankInfos = tollProperties.getBankInfo().split(",");

			for (String bankInfo : bankInfos) {
				String[] info = bankInfo.split("~");
				if (info[2].equalsIgnoreCase(bank.getIin())) {
					refUrl = info[3];
				}
			}

		}

		Vehicle vehicle = new Vehicle();

		if (StringUtils.isNotBlank(vrn) && StringUtils.isNotBlank(last5digitofengineno)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("vrn" + vrn)
					.data("last5digitofengineno", last5digitofengineno).format());
			vehicle.setVrn(vrn);
			vehicle.setLast5digitofengineno(last5digitofengineno);
		} else if (StringUtils.isNotBlank(vinNumber) && StringUtils.isNotBlank(last5digitofengineno)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("vinNumber" + vinNumber)
					.data("last5digitofengineno", last5digitofengineno).format());
			vehicle.setVinNumber(vinNumber.toUpperCase());
			vehicle.setLast5digitofengineno(last5digitofengineno);
		} else if (StringUtils.isNotBlank(vinNumber)) {
			vehicle.setVinNumber(vinNumber.toUpperCase());
		}

		String dipcoinReferenceNumber = CoreUtils.randomAlphaString(22);
		String txnId = "";

		Txn txn = new Txn();

		Head head = new Head();
		// head.setVer(TollConstant.VERSION);
		head.setVer(TollConstant.VER);
		SimpleDateFormat formatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
		Date date = formatter.parse(tollNetcSyncTimeResponse.getResp().getTs());
		head.setTs(tollNetcSyncTimeResponse.getResp().getTs());
		txn.setTs(tollNetcSyncTimeResponse.getResp().getTs());
		head.setOrgId(bank.getOrgId());

		formatter = new SimpleDateFormat(TollConstant.MSG_DATE_FORMAT);
		date = new Date(System.currentTimeMillis());
		head.setMsgId(bank.getOrgId() + formatter.format(date).toUpperCase());

		txn.setId(dipcoinReferenceNumber);
		txnId = dipcoinReferenceNumber;
		txn.setNote(TollConstant.REQUEST_DETAILS_NOTE);
		txn.setOrgTxnId(bank.getOrgId() + dipcoinReferenceNumber);
		txn.setRefId(dipcoinReferenceNumber);
		txn.setRefUrl(refUrl);
		txn.setType(TollConstant.REQUEST_DETAILS_TYPE);
		txn.setVehicle(vehicle);

		ReqVehicleDetailsRequest reqVehicleDetailsRequest = new ReqVehicleDetailsRequest();

		reqVehicleDetailsRequest.setHead(head);
		reqVehicleDetailsRequest.setTxn(txn);

		// Create JAXB Context
		JAXBContext jaxbContext = JAXBContext.newInstance(ReqVehicleDetailsRequest.class);

		// Create Marshaller
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// Required formatting??
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// Print XML String to Console
		StringWriter sw = new StringWriter();

		// Write XML to StringWriter
		jaxbMarshaller.marshal(reqVehicleDetailsRequest, sw);

		// Verify XML Content
		String postData = sw.toString();

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

		ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices
				.signatureGenerationServices(byteArrayInputStream, httpServletContext.getTraceId(), bank.getOrgId());
		String responseData = null;

		// NPCI Active Active Setup Phase2 changes
		String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
		if (StringUtils.isEmpty(ipAddress)) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("********** NPCI Server is DOWN **********").format());
			return null;

		}
		int port = this.tollProperties.getNetcHealthCheckPort();

		try {
			if (tollProperties.isConnectNpci()) {
				TollHttpsServices.bankIin = bank.getIin();
				/*
				 * String url = "";
				 * 
				 * switch (regType) { case 0: url = tollProperties.getRequestDetailUrl(); break;
				 * case 1: url = tollProperties.getRequestDetailIHMCL(); break; default: url =
				 * tollProperties.getRequestDetailUrl(); break; }
				 */

				String endPoint = tollProperties.getReqVehicleDetailsUrl();

				/*
				 * switch (regType) { case 0: endPoint = tollProperties.getRequestDetailUrl();
				 * break; case 1: endPoint = tollProperties.getRequestDetailIHMCL(); break;
				 * default: endPoint = tollProperties.getRequestDetailUrl(); break; }
				 */

				String url = "https://" + ipAddress + ":" + port + endPoint;

				responseData = tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);

				return responseData;
				// return txnId;
			}
		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Exception Caught while calling tollHttpsServices").format(), e);
		}
		return responseData;

	}

	// Customer Vehicle Verification Status
	public ResponseEntity customerVehicleVerificationStatus(User user, String vehicleRegistrationNo, String tagId,
			String tid, String serialNumber, Integer regType, Integer cardId, boolean flag)
			throws InterruptedException, ExecutionException {

		if (user == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));
		}

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (StringUtils.isBlank(vehicleRegistrationNo)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
		}

		if (!tollProperties.isConnectNpci()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_API_NOT_ALLOWED));
		}

		CustomerAccount custAccount = customerDBService.getAccount(user.getId(), cardId, false);

		if (custAccount == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.INVALID_CARD_ID));
		}

		Bank bank = custAccount.getBank();

		if (bank == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		return brontooResource.getVehicleInfo(user, vehicleRegistrationNo, tagId, tid, regType, bank.getReferenceId()
		/* flag */);

		/*
		 * return brontooResource.getVehicleInfo(user, vehicleRegistrationNo, tagId,
		 * tid, null, vin, vrn, last5digitofengineno, regType, bank.getReferenceId());
		 */
	}

	public ResponseEntity addAndUpdateTollCustomer(User user, User bankUser, Bank bank, final MultipartFile[] rcDoc,
			final MultipartFile idProof, final String request, final Integer registrationType,
			final String clientTransactionId) throws Exception, APIException {

		TollRegistrationResponse response = new TollRegistrationResponse();
		List<FeesAndDeposit> feesAndDeposits = null;

		ResponseEntity responseEntity = null;
		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		if (!userDBService.isCustomer(user) && bank == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}
		if (!this.userDBService.isActive(user) && bank == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		response.setClientTransactionId(clientTransactionId);

		if (request == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
		}

		MultipartFile filepart = idProof;
		if (null == filepart || NumberUtils.INTEGER_ZERO == rcDoc.length) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_IMAGE_MISSING));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("filepart").data("filepart", filepart)
				.format());

		LOG.debug(
				LogFormatter.instance(httpServletContext.getTraceId()).message("rcDoc").data("rcDoc", rcDoc).format());

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("TollRegistrationRequest")
				.data("request", request).format());
		final TollRegistrationRequest createReq = new ObjectMapper().readValue(request, TollRegistrationRequest.class);

		// If request is called from bank
		if (bank != null) {
			// Get the List

			List<CustomerAccount> existingAccounts = this.customerDBService.asyncGetAccounts(user.getId()).get();
			if (CollectionUtils.isEmpty(existingAccounts)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.MISSING_USER_CARDID.message()).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.MISSING_USER_CARDID));
			}

			for (CustomerAccount account : existingAccounts) {
				if (((StringUtils.isNotEmpty(account.getHashedAccountNumber()) && account.getHashedAccountNumber()
						.equals(Utils.getHashedLogin(StringUtils.stripStart(createReq.getAccountNumber(), "0"))))
						|| (StringUtils.isNotEmpty(account.getHashedLogin()) && account.getHashedLogin().equals(
								Utils.getHashedLogin(StringUtils.stripStart(createReq.getAccountNumber(), "0")))))
						&& CustomerAccountStatus.ACTIVE.value() == account.getStatus()) {
					createReq.setCardId(account.getUserCardId());
					break;
				}
			}

			// Fetch the registered User.from bank
			List<User> users = this.userDBService.getUsers(createReq.getMobileNo(), 0,
					Arrays.asList(UserRoles.CUSTOMER.value()), DBConstants.UserStatus.ACTIVE.value());
			// overide the bankuser object to
			// registered customer user object.
			user = users.get(0);

			// Seeting the Autorization Pin
			createReq.setAuthorizationPin(APIConstants.PIN);

		}

		CustomerAccount customerAccount = customerDBService.getAccount(user.getId(), createReq.getCardId());
		if (customerAccount == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.MISSING_INVALID_INFO.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));

		}

		// pass lien debit credit method
		/*
		 * ResponseEntity lienDebitRes = this.lienDebitCredit(customerAccount, request);
		 * 
		 * if (HttpStatus.BAD_REQUEST.value() <= lienDebitRes.getStatusCodeValue()) {
		 * return lienDebitRes; }
		 */

		TollRegistration tollRegistration = null;

		if (createReq.getId() != null && createReq.getId() != NumberUtils.INTEGER_ZERO) {
			TollRegistration tollReg = this.tollDBService.findById(createReq.getId());

			if (null == tollReg) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
			}
			if (isProfileChanged(tollReg, createReq)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Profile has been Changed")
						.format());
				createReq.setId(NumberUtils.INTEGER_ZERO);
				if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {
					responseEntity = getFeesAndDeposits(user, createReq, clientTransactionId, customerAccount,
							registrationType);
					if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
						if (this.vehicleNumberLock != null) {
							try {
								this.vehicleNumberLock.unlock();
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("vehicleNumberLock unlocked").format());
							} catch (Exception ex) {
								LOG.debug(
										LogFormatter.instance(httpServletContext.getTraceId())
												.message("Exception Caught while unlocking vehicleNumberLock").format(),
										ex);
							}
						}
						return responseEntity;
					}
					feesAndDeposits = (List<FeesAndDeposit>) responseEntity.getBody();
				}

			} else {

				createReq.setStatus(tollReg.getStatus());
				if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {
					vehicleRegistrationNo.clear();
					responseEntity = getFeesAndDeposits(user, createReq, clientTransactionId, customerAccount, tollReg,
							registrationType);
					if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
						if (this.vehicleNumberLock != null) {
							try {
								this.vehicleNumberLock.unlock();
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("vehicleNumberLock unlocked").format());
							} catch (Exception ex) {
								LOG.debug(
										LogFormatter.instance(httpServletContext.getTraceId())
												.message("Exception Caught while unlocking vehicleNumberLock").format(),
										ex);
							}
						}
						return responseEntity;
					}
					feesAndDeposits = (List<FeesAndDeposit>) responseEntity.getBody();
				}
			}

		} else {
			if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {

				responseEntity = getFeesAndDeposits(user, createReq, clientTransactionId, customerAccount,
						registrationType);
				if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
					if (this.vehicleNumberLock != null) {
						try {
							this.vehicleNumberLock.unlock();
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("vehicleNumberLock unlocked").format());
						} catch (Exception ex) {
							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Exception Caught while unlocking vehicleNumberLock").format(),
									ex);
						}
					}
					return responseEntity;
				}
				feesAndDeposits = (List<FeesAndDeposit>) responseEntity.getBody();
			}
		}

		if (createReq.getId() != NumberUtils.INTEGER_ZERO && createReq.getId() != null) {
			if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {
				// commenting because of proxy issue.

				responseEntity = createFeesandDepositOsta(createReq, user, bankUser, customerAccount, feesAndDeposits,
						registrationType, clientTransactionId);
				if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
					if (this.vehicleNumberLock != null) {
						try {
							this.vehicleNumberLock.unlock();
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("vehicleNumberLock unlocked").format());
						} catch (Exception ex) {
							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Exception Caught while unlocking vehicleNumberLock").format(),
									ex);
						}
					}

					return responseEntity;
				}
			} else {
				feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(
						Arrays.asList(customerAccount.getWalletBankId()),
						new HashSet<String>(Arrays.asList(createReq.getVehicleList().get(0).getCategory())));

				if (CollectionUtils.isEmpty(feesAndDeposits)) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE.message()).format());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));

				}
			}
			// Call for populating the request for save
			TollRegistration registerationSaveObj = populateForSaveAndUpdateTollCustomer(user, bankUser, createReq,
					clientTransactionId, customerAccount, feesAndDeposits, registrationType, null);

			// update the registration details.
			tollRegistration = this.tollDBService.asyncUpdate(registerationSaveObj).get();

			if (null == tollRegistration) {
				if (this.vehicleNumberLock != null) {
					try {
						this.vehicleNumberLock.unlock();
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("vehicleNumberLock unlocked").format());
					} catch (Exception ex) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Exception Caught while unlocking vehicleNumberLock").format(), ex);
					}
				}

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}

			setFilePath(tollRegistration, user, filepart, createReq, rcDoc);

			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Updated").format());

			vehicleRegistrationNo.clear();
			response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		} else {
			if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {

				// commenting because of proxy issue
				responseEntity = createFeesandDepositOsta(user, bankUser, bank, customerAccount, feesAndDeposits,
						createReq, registrationType, clientTransactionId);
				if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {

					if (this.vehicleNumberLock != null) {
						try {
							this.vehicleNumberLock.unlock();
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("vehicleNumberLock unlocked").format());
						} catch (Exception ex) {
							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Exception Caught while unlocking vehicleNumberLock").format(),
									ex);
						}
					}

					return responseEntity;
				}
			} else {
				feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(
						Arrays.asList(customerAccount.getWalletBankId()),
						new HashSet<String>(Arrays.asList(createReq.getVehicleList().get(0).getCategory())));

				if (CollectionUtils.isEmpty(feesAndDeposits)) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE.message()).format());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));

				}
			}

			// Call for populating the request for save
			TollRegistration registerationSaveObj = populateForSaveAndUpdateTollCustomer(user, bankUser, createReq,
					clientTransactionId, customerAccount, feesAndDeposits, registrationType, null);

			tollRegistration = tollDBService.asyncSave(registerationSaveObj).get();

			if (null == tollRegistration) {

				if (this.vehicleNumberLock != null) {
					try {
						this.vehicleNumberLock.unlock();
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("vehicleNumberLock unlocked").format());
					} catch (Exception ex) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Exception Caught while unlocking vehicleNumberLock").format(), ex);
					}
				}
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}

			setFilePath(tollRegistration, user, filepart, createReq, rcDoc);

			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Created").format());
			populateTollCustomerResponse(user, createReq, response);
			response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		}

		try {

			StringBuilder tags = new StringBuilder();
			for (TollTag tolltag : tollRegistration.getTollTag()) {
				if (tollRegistration.getTollTag().size() > 1) {
					tags.append(",").append(tolltag.getRegistrationNo());
				} else {
					tags.append(tolltag.getRegistrationNo());
				}
			}

			// success sms
			if (!smsClient.sendSms(tollRegistration.getMobileNo(),
					Templates.TollTagRegistration.format(tags.toString(),
							customerAccount.getBank().getAlias() == null ? customerAccount.getBank().getName()
									: customerAccount.getBank().getAlias()),
					true)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
						.data("phone", tollRegistration.getMobileNo()).format());
			}

			if (applicationProperties.getAwsSMSClient()) {

				NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
				notificationRequestContext.setTraceId(httpServletContext.getTraceId());
				if (!notificationResource.sendSms(tollRegistration.getMobileNo(),
						Templates.TollTagRegistration.format(tags.toString(),
								customerAccount.getBank().getAlias() == null ? customerAccount.getBank().getName()
										: customerAccount.getBank().getAlias()),
						httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

					LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", tollRegistration.getMobileNo()).format());

				}
			}

			// Email
			if (!this.tollEmailUtils.sendTollCustomerRegistrationEmail(tags.toString(), customerAccount.getBank(),
					tollRegistration)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to send email to Cutomer for Registration success.")
						.data("user email", tollRegistration.getEmailId()).format());
			}

		} catch (Exception exception) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Exception in sending email & sms to Customer." + exception).format());
		} finally {

			if (this.vehicleNumberLock != null) {
				try {
					this.vehicleNumberLock.unlock();
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("vehicleNumberLock unlocked").format());
				} catch (Exception ex) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Exception Caught while unlocking vehicleNumberLock").format(), ex);
				}
			}
		}
		// Adding the count to metrix
		tollMetricRegistry.numberOfTagsApplied().increment();

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	public boolean isProfileChanged(TollRegistration existingTollProfileDetails, TollRegistrationRequest createReq) {

		if (!existingTollProfileDetails.getDob().equalsIgnoreCase(createReq.getDob())
				|| !existingTollProfileDetails.getEmailId().equalsIgnoreCase(createReq.getEmailId())
				|| !existingTollProfileDetails.getFirstName().equalsIgnoreCase(createReq.getFirstName())
				|| !existingTollProfileDetails.getLastName().equalsIgnoreCase(createReq.getLastName())
				|| !existingTollProfileDetails.getGender().equalsIgnoreCase(createReq.getGender())
				|| !existingTollProfileDetails.getMobileNo().equalsIgnoreCase(createReq.getMobileNo())
				|| existingTollProfileDetails.getPincode() != createReq.getPincode()
				|| !existingTollProfileDetails.getIdProofNumber().equalsIgnoreCase(createReq.getIdProofNo()))
			return true;
		return false;
	}

	/*
	 * Fees and deposit Register toll Collection Customer
	 */
	public ResponseEntity getFeesAndDeposits(final User user, final TollRegistrationRequest createReq,
			final String clientTransactionId, final CustomerAccount customerAccount, final Integer registrationType)
			throws Exception, APIException {

		String originIp = httpServletContext.getOriginIp();

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", createReq).format());
		TollRegistrationResponse response = new TollRegistrationResponse();

		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		// checking for the request null
		if (createReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		// validate request
		if (!createReq.validate(httpServletContext)) {
			response.addHeaderCodes(createReq.getErrorCodes());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		if (!(this.userDBService.isCustomer(user) || this.userDBService.isBankSuperAdmin(user))) {
			response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (customerAccount == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.CUSTOMER_ACCOUNT_DOESNT_EXIST));
		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get User Accounts for Toll")
				.data("UserId", user.getId()).data("Status", user.getStatus()).format());

		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();

		List<String> roleList = new ArrayList<>();
		roleList.add(DBConstants.UserRoles.MERCHANT_INTERNAL.value());

		if (CollectionUtils.isEmpty(merchants)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("merchant is null").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MERCHANT_DOESNT_EXIST));
		}

		response.setClientTransactionId(clientTransactionId);
		List<Integer> bankList = new ArrayList<Integer>();
		Set<String> chargeCategoryList = new HashSet<String>();

		bankList.add(customerAccount.getBank().getId());

		// @TODO WRITE THE CODE FOR FETCHING.

		RKeys rkeys = redissonclient.getKeys();

		for (TollTagRequest tollTagRequest : createReq.getVehicleList()) {

			Iterable<String> serialNumberRkeys = rkeys.getKeysByPattern(tollTagRequest.getRegistrationNo());

			if (IterableUtils.size(serialNumberRkeys) > NumberUtils.INTEGER_ZERO) {
				Iterator<String> rkey = serialNumberRkeys.iterator();
				while (rkey.hasNext()) {

					String key = rkey.next();
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.TOLL_TAG_VEHICLE_NUMBER_REGISTRATON_IN_PROGRESS_TRY_LATER.message())
							.data("key", key).format());
					if (key.equalsIgnoreCase(tollTagRequest.getRegistrationNo())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("vehicle number key pattern already present in redis").format());

						response.addHeaderCode(HeaderCode.TOLL_TAG_VEHICLE_NUMBER_REGISTRATON_IN_PROGRESS_TRY_LATER);
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
					}
				}
			}
		}

		vehicleRegistrationNo.clear();
		for (TollTagRequest tollTagRequest : createReq.getVehicleList()) {

			this.vehicleNumberLock = redissonclient.getFairLock(
					StringUtils.isEmpty(tollTagRequest.getRegistrationNo()) ? "" : tollTagRequest.getRegistrationNo());
			this.vehicleNumberLock.lock(120, TimeUnit.SECONDS);
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Locking Vehicle number " + tollTagRequest.getRegistrationNo()).format());
			chargeCategoryList.add(tollTagRequest.getCategory());

			vehicleRegistrationNo.put(tollTagRequest.getRegistrationNo().replaceAll(" ", "").trim(),
					tollTagRequest.getCategory());

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching TollTags")
					.data("Registratin No", tollTagRequest.getRegistrationNo().replaceAll(" ", "").trim())
					.data("Bank Id", customerAccount.getBank().getId()).format());

			List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNoAndBankId(
					tollTagRequest.getRegistrationNo().replaceAll(" ", "").trim(), customerAccount.getBank().getId());

			if (CollectionUtils.isNotEmpty(tollTags)) {
				for (TollTag tolltag : tollTags) {
					if (!(DBConstants.TollTagApprovalStatus.INACTIVE.value() == Integer.valueOf(tolltag.getStatus())
							|| DBConstants.TollTagApprovalStatus.BANK_REJECTED.value() == Integer
									.valueOf(tolltag.getStatus()))) {

						// failed metric
						tollMetricRegistry.tagApplicationFailed().increment();
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("VehicleNumber is already active").format());

						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body(APIResponse.error(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST));
					}
				}
			}
		}

		for (Map.Entry<String, String> vehicleInfo : vehicleRegistrationNo.entrySet()) {
			if (tollProperties.isConnectNpci()) {

				// Extra validation when registration of IHMCL customers.
				if (registrationType == RegistrationType.IHMCL.value()) {

					ResponseEntity vehicleResponse = vehicleVerification(user, clientTransactionId,
							vehicleInfo.getKey(), vehicleInfo.getValue(), null, null, createReq.getCardId(),
							registrationType);
					if (vehicleResponse != null
							&& (vehicleResponse.getStatusCodeValue() == Status.BAD_REQUEST.getStatusCode())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("VehicleNumber is already present at npci").format());
						return vehicleResponse;
					}

					// set the TagId and TID in the request.
					TollNetcDetailsResponse tollNetcDetailsResponse = (TollNetcDetailsResponse) vehicleResponse
							.getBody();

					ResponseEntity validationResponse = this.vehicleDetailsValidation(createReq, registrationType,
							vehicleInfo.getKey(), tollNetcDetailsResponse);

					if (validationResponse != null
							&& (validationResponse.getStatusCodeValue() == Status.BAD_REQUEST.getStatusCode())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Vehicle validation failed. ").format());
						return validationResponse;
					}
				}
			}
		}

		vehicleRegistrationNo.clear();

		List<FeesAndDeposit> feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(bankList,
				chargeCategoryList);

		// Data check.
		if (CollectionUtils.isEmpty(feesAndDeposits)) {

			// failed metric
			tollMetricRegistry.tagApplicationFailed().increment();

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Toll Deposit And Fee Amount Not Available.").data("Category list ", chargeCategoryList)
					.format());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));

		}

		// Validation for Auto-top threshold amount +++++++++++++++++++++++++
		BigDecimal minimumAmountDefault = BigDecimal.ZERO;
		BigDecimal totalMinimumAmount = BigDecimal.ZERO;
		for (TollTagRequest tollTagReq : createReq.getVehicleList()) {
			for (FeesAndDeposit feesAndDeposit : feesAndDeposits) {
				if (feesAndDeposit.getChargeCategory().equalsIgnoreCase(tollTagReq.getCategory())
						&& feesAndDeposit.getBankId() == customerAccount.getBank().getId()) {
					minimumAmountDefault = feesAndDeposit.getMinimumAmount();
				}
			}
		}

		totalMinimumAmount = totalMinimumAmount.add(minimumAmountDefault);

		List<TollTag> tollTags = tollDBService.asyncFindTollTagsByCustomerAccountId(customerAccount.getId()).get();
		for (TollTag tolltag : tollTags) {
			if (DBConstants.TollTagApprovalStatus.ACTIVE.value() == Integer.parseInt(tolltag.getStatus())
					|| DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer
							.valueOf(tolltag.getStatus())
					|| DBConstants.TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value() == Integer
							.valueOf(tolltag.getStatus())) {
				totalMinimumAmount = totalMinimumAmount.add(tolltag.getMinimumAmount());
			}
		}

		TopUpDetails existingTopUpDetails = null;
		if (StringUtils.isNoneBlank(customerAccount.getTopUpDetails())) {
			existingTopUpDetails = objectMapper.readValue(customerAccount.getTopUpDetails(), TopUpDetails.class);

			if (createReq.getAutoTopUpThresHold() == null) {
				if (totalMinimumAmount
						.compareTo(existingTopUpDetails.getAutoTopUpThresHold()) > NumberUtils.INTEGER_MINUS_ONE) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.TOLL_THRESHOLD_AMOUNT_INCORRECT));
				}
			}
		}

		// register metric
		tollMetricRegistry.tagApplicationSuccess().increment();

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("feesAndDeposits object returned")
				.format());

		// response setup for the request
		return ResponseEntity.status(HttpStatus.OK).body(feesAndDeposits);

	}

	public boolean setFilePath(TollRegistration tollRegistration, User user, MultipartFile filepart,
			TollRegistrationRequest createReq, final MultipartFile[] rcDoc) throws APIException, Exception {
		String destinationPath = processUploadedFiles(filepart, user, httpServletContext, tollRegistration.getId());
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll idproof path set")
				.data("destinationPath", destinationPath).format());
		if (destinationPath == null) {
			throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.FILE_UPLOAD_FAIL));
		}
		tollRegistration.setIdProofImage(destinationPath);

		int index = 0;

		for (MultipartFile rcImage : rcDoc) {

			destinationPath = processUploadedFiles(rcImage, user, httpServletContext,
					tollRegistration.getTollTag().get(index).getId());
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll RcImage path set")
					.data("destinationPath", destinationPath).format());

			if (destinationPath != null) {

				tollRegistration.getTollTag().get(index).setRCImage(destinationPath);
			} else {

				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
						APIResponse.error(HeaderCode.FILE_UPLOAD_FAIL));
			}
			index++;
		}
		// update image path in db
		tollRegistration = this.tollDBService.update(tollRegistration);
		if (null == tollRegistration) {
			throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		return true;
	}

	/*
	 * Method for processing the uploaded file
	 */
	public String processUploadedFiles(final MultipartFile filePart, final User user,
			HttpServletContext httpServletContext, final Integer id) throws Exception, APIException {

		String destinationPath = null;
		String folderName = filePart.getName().equalsIgnoreCase("idproof") ? "tollRegistrationId_" : "tollTagId_";
		String docType = filePart.getContentType().split("/")[1];
		// get input stream of file
		InputStream fileInputStream = filePart.getInputStream();
		// Check active profile whether to upload to /tmp or aws s3
		if (applicationProperties.uploadFileToS3()) {
			//
			// // Setting upload location path in AWS S3 based on merchant type
			String uploadLocation = APIConstants.CUSTOMER_TOLL_PATH + "userId_" + user.getId() + "/" + folderName + id
					+ "/" + filePart.getName() + "/" + filePart.getOriginalFilename();

			// set destination path for uploading file in S3 bucket
			destinationPath = uploadLocation;

			// getting file content length
			long contentLength = filePart.getSize();

			if (!this.objectDataStore.put(destinationPath, fileInputStream, contentLength)) {

				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to upload file to S3 bucket.").format());
				// info.addHeaderCode(HeaderCode.FILE_UPLOAD_FAIL);
				destinationPath = null;
			}

		}

		else {

			destinationPath = new StringBuffer(APIConstants.SERVER_UPLOAD_LOCATION_FOLDER)
					.append("tollcustomer_userId_").append(user.getId()).append("_" + folderName).append(id).append("_")
					.append(filePart.getName()).append("_").append(filePart.getOriginalFilename()).toString();

			if (!objectDataStore.put(destinationPath, fileInputStream, 0)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to store at temp")
						.format());
				// info.addHeaderCode(HeaderCode.FILE_UPLOAD_FAIL);
				destinationPath = null;

			}

		}
		return destinationPath;
	}

	/*
	 * Getting the Response for Toll Customer After Registration request.
	 */
	public static void populateTollCustomerResponse(final User user, final TollRegistrationRequest request,
			TollRegistrationResponse response) throws APIException {

		response.setFirstName(request.getFirstName()).setLastName(request.getLastName()).setGender(request.getGender())
				.setDob(request.getDob()).setMobileNo(request.getMobileNo()).setEmailId(request.getEmailId());

	}

	/*
	 * Fees and Deposits Updating TollRegistration Details of the toll Collection
	 * Customer
	 */

	public ResponseEntity getFeesAndDeposits(final User user, final TollRegistrationRequest updateReq,
			final String clientTransactionId, final CustomerAccount customerAccount, final TollRegistration tollReg,
			Integer registrationType) throws Exception, APIException {

		String originIp = httpServletContext.getOriginIp();

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", updateReq).format());

		TollRegistrationResponse response = new TollRegistrationResponse();

		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));

		}

		// checking for the request null
		if (updateReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		// validate request
		if (!updateReq.validate(httpServletContext)) {
			response.addHeaderCodes(updateReq.getErrorCodes());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// Checking the Registration status Check

		if (TollRegistrationStatus.INACTIVE.equals(updateReq.getStatus())) {
			response.addHeaderCode(HeaderCode.TOLL_INACTIVE_ACCOUNT_UPDATION);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

		}

		response.setClientTransactionId(clientTransactionId);
		List<Integer> bankList = new ArrayList<Integer>();
		Set<String> chargeCategoryList = new HashSet<String>();

		bankList.add(customerAccount.getBank().getId());

		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();
		List<String> roleList = new ArrayList<>();
		roleList.add(DBConstants.UserRoles.MERCHANT_INTERNAL.value());

		if (CollectionUtils.isEmpty(merchants)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("merchant is null").format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MERCHANT_DOESNT_EXIST));
		}

		RKeys rkeys = redissonclient.getKeys();

		for (TollTagRequest updateTollTagRequest : updateReq.getVehicleList()) {

			Iterable<String> serialNumberRkeys = rkeys.getKeysByPattern(updateTollTagRequest.getRegistrationNo());

			if (IterableUtils.size(serialNumberRkeys) > NumberUtils.INTEGER_ZERO) {
				Iterator<String> rkey = serialNumberRkeys.iterator();
				while (rkey.hasNext()) {

					String key = rkey.next();
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.TOLL_TAG_VEHICLE_NUMBER_REGISTRATON_IN_PROGRESS_TRY_LATER.message())
							.data("key", key).format());
					if (key.equalsIgnoreCase(updateTollTagRequest.getRegistrationNo())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("vehicle number key pattern already present in redis").format());

						response.addHeaderCode(HeaderCode.TOLL_TAG_VEHICLE_NUMBER_REGISTRATON_IN_PROGRESS_TRY_LATER);
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
					}
				}
			}
		}

		boolean foundVehicleNumber = false;// vehicle which has not been registered in list

		for (TollTagRequest updateTollTagRequest : updateReq.getVehicleList()) {
			// added check below to avoid null pointer
			this.vehicleNumberLock = redissonclient
					.getFairLock(StringUtils.isEmpty(updateTollTagRequest.getRegistrationNo()) ? ""
							: updateTollTagRequest.getRegistrationNo());
			this.vehicleNumberLock.lock(120, TimeUnit.SECONDS);
			for (TollTag tollTag : tollReg.getTollTag()) {
				if (!(DBConstants.TollTagApprovalStatus.INACTIVE.value() == Integer.valueOf(tollTag.getStatus())
						|| DBConstants.TollTagApprovalStatus.BANK_REJECTED.value() == Integer
								.valueOf(tollTag.getStatus()))
						&& tollTag.getRegistrationNo().replaceAll(" ", "").trim().equalsIgnoreCase(
								updateTollTagRequest.getRegistrationNo().replaceAll(" ", "").trim())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("foundVehicleNumber")
							.data("updateTollTagRequest", updateTollTagRequest.getRegistrationNo())
							.data("tollTag", tollTag.getRegistrationNo()).format());
					foundVehicleNumber = true;

				}
			}
			if (!foundVehicleNumber) {
				chargeCategoryList.add(updateTollTagRequest.getCategory());
				vehicleRegistrationNo.put(updateTollTagRequest.getRegistrationNo().replaceAll(" ", "").trim(),
						updateTollTagRequest.getCategory());
			}
		}

		if (vehicleRegistrationNo.isEmpty()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("No new vehicle no found in the request.").format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST));

		}

		for (Map.Entry<String, String> vehicleInfo : vehicleRegistrationNo.entrySet()) {

			List<TollTag> tollTags = tollDBService
					.findTollTagByRegistrationNo(Arrays.asList(vehicleInfo.getKey().replaceAll(" ", "").trim()));
			if (CollectionUtils.isNotEmpty(tollTags)) {
				for (TollTag tolltag : tollTags) {
					if (!(DBConstants.TollTagApprovalStatus.INACTIVE.value() == Integer.valueOf(tolltag.getStatus())
							|| DBConstants.TollTagApprovalStatus.BANK_REJECTED.value() == Integer
									.valueOf(tolltag.getStatus()))) {

						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("foundVehicleNumber is active").format());
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body(APIResponse.error(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST));
					}
				}
			}

			if (tollProperties.isConnectNpci()) {

				// Extra validation when registration of IHMCL customers.
				if (registrationType == RegistrationType.IHMCL.value()) {

					ResponseEntity vehicleResponse = vehicleVerification(user, clientTransactionId,
							vehicleInfo.getKey(), vehicleInfo.getValue(), null, null, updateReq.getCardId(),
							registrationType);
					if (vehicleResponse != null
							&& (vehicleResponse.getStatusCodeValue() == Status.BAD_REQUEST.getStatusCode())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("VehicleNumber is already present at npci").format());
						return vehicleResponse;
					}

					// set the TagId and TID in the request.
					TollNetcDetailsResponse tollNetcDetailsResponse = (TollNetcDetailsResponse) vehicleResponse
							.getBody();

					ResponseEntity validationResponse = this.vehicleDetailsValidation(updateReq, registrationType,
							vehicleInfo.getKey(), tollNetcDetailsResponse);

					if (validationResponse != null
							&& (validationResponse.getStatusCodeValue() == Status.BAD_REQUEST.getStatusCode())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Vehicle validation failed. ").format());
						return validationResponse;
					}
				}
			}
		}

		List<FeesAndDeposit> feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(bankList,
				chargeCategoryList);

		// Data check.
		if (CollectionUtils.isEmpty(feesAndDeposits)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("feesAndDeposits object is null")
					.format());
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));
		}

		// Validation for Auto-top threshold amount +++++++++++++++++++++++++
		BigDecimal minimumAmountDefault = BigDecimal.ZERO;
		BigDecimal totalMinimumAmount = BigDecimal.ZERO;

		for (Map.Entry<String, String> vehicleInfo : vehicleRegistrationNo.entrySet()) {

			for (FeesAndDeposit feesAndDeposit : feesAndDeposits) {
				if (feesAndDeposit.getChargeCategory().equalsIgnoreCase(vehicleInfo.getValue())
						&& feesAndDeposit.getBankId() == customerAccount.getBank().getId()) {
					minimumAmountDefault = minimumAmountDefault.add(feesAndDeposit.getMinimumAmount());
				}
			}
		}
		totalMinimumAmount = totalMinimumAmount.add(minimumAmountDefault);

		List<TollTag> tollTags = tollDBService.asyncFindTollTagsByCustomerAccountId(customerAccount.getId()).get();
		for (TollTag tolltag : tollTags) {
			if (DBConstants.TollTagApprovalStatus.ACTIVE.value() == Integer.parseInt(tolltag.getStatus())
					|| DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer
							.valueOf(tolltag.getStatus())
					|| DBConstants.TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value() == Integer
							.valueOf(tolltag.getStatus())) {
				totalMinimumAmount = totalMinimumAmount.add(tolltag.getMinimumAmount());
			}
		}

		TopUpDetails existingTopUpDetails = null;
		if (StringUtils.isNoneBlank(customerAccount.getTopUpDetails())) {
			existingTopUpDetails = objectMapper.readValue(customerAccount.getTopUpDetails(), TopUpDetails.class);

			if (updateReq.getAutoTopUpThresHold() == null) {
				if (totalMinimumAmount
						.compareTo(existingTopUpDetails.getAutoTopUpThresHold()) > NumberUtils.INTEGER_MINUS_ONE) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.TOLL_THRESHOLD_AMOUNT_INCORRECT));
				}
			}
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("feesAndDeposits object returned")
				.format());

		// POPULATING THE RESPONSE.
		return ResponseEntity.status(HttpStatus.OK).body(feesAndDeposits);

	}

	/*
	 * NPCI Vehicle response validation.
	 */
	public ResponseEntity vehicleDetailsValidation(TollRegistrationRequest createReq, Integer registrationType,
			String vehicelRegNo, TollNetcDetailsResponse tollNetcDetailsResponse) throws Exception, APIException {

		Boolean isdataMismatch = false;

		if (registrationType == RegistrationType.IHMCL.value()) {

			for (TollTagRequest tollTagRequest : createReq.getVehicleList()) {

				if (tollTagRequest.getRegistrationNo().equals(vehicelRegNo)) {

					// Gets the vehicle deatilas form the NPCI API.
					List<Detail> details = tollNetcDetailsResponse.getTxn().getResp().getVehicle().getVehicleDetails()
							.get(NumberUtils.INTEGER_ZERO).getDetail();

					// Comparing the Value of request with NPCI.
					for (Detail detail : details) {
						if (TollConstant.TID.equalsIgnoreCase(detail.getName())) {
							tollTagRequest.setTid(detail.getValue());
						} else if (TollConstant.TAGID.equalsIgnoreCase(detail.getName())) {
							tollTagRequest.setTagId(detail.getValue());
						} else if (TollConstant.VEHICLECLASS.equalsIgnoreCase(detail.getName())) {

							if (!(tollTagRequest.getCategory()).equalsIgnoreCase(detail.getValue())) {
								isdataMismatch = true;
							}
							tollTagRequest.setCategory(detail.getValue());

						} else if (TollConstant.REGNUMBER.equalsIgnoreCase(detail.getName())) {

							if (!tollTagRequest.getRegistrationNo().equalsIgnoreCase(detail.getValue())) {
								isdataMismatch = true;
							}
							tollTagRequest.setRegistrationNo(detail.getValue());

						} else if (TollConstant.EXCCODE.equalsIgnoreCase(detail.getName())) {
							tollTagRequest.setExcCode(detail.getValue());
						} else if (TollConstant.COMVEHICLE.equalsIgnoreCase(detail.getName())) {
							if (!tollTagRequest.getIsCommercial().equalsIgnoreCase(detail.getValue())) {
								isdataMismatch = true;
							}
							tollTagRequest.setIsCommercial(detail.getValue());
						}
					}
				}
			}

			if (isdataMismatch) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TOLL_IHMCL_USER_VEHICLE_VERIFICATION_FAILED));
			}

			return ResponseEntity.status(HttpStatus.OK).body(createReq);
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));

	}

	/*
	 * For Populating the Request or Add and Update.
	 */
	public TollRegistration populateForSaveAndUpdateTollCustomer(final User user, User bankUser,
			final TollRegistrationRequest request, String clientTransactionId, CustomerAccount customerAccount,
			List<FeesAndDeposit> feesAndDeposits, Integer registrationType, User merchantUser)
			throws JsonProcessingException {

		TollRegistration registration = new TollRegistration();
		if (request.getId() != null) {

			registration.setId(request.getId());
			registration.setStatus(StringUtils.isNotBlank(request.getStatus()) ? request.getStatus()
					: String.valueOf(TollRegistrationStatus.DEFAULT.value()));
		} else {
			registration.setStatus(String.valueOf(TollRegistrationStatus.DEFAULT.value()));
		}

		if (registrationType == RegistrationType.IHMCL.value()) {
			registration.setId(NumberUtils.INTEGER_ZERO);
			registration.setStatus(String.valueOf(TollRegistrationStatus.ACTIVE.value()));
		}

		registration.setAddress(request.getAddress());
		registration.setCity(request.getCity());
		registration.setClientTransactionId(clientTransactionId);
		registration.setFirstName(request.getFirstName());
		registration.setLastName(request.getLastName());
		registration.setMobileNo(request.getMobileNo());
		registration.setEmailId(request.getEmailId());
		registration.setGender(request.getGender());
		registration.setDob(request.getDob());
		registration.setState(request.getState());
		registration.setPincode(request.getPincode());
		registration.setIdProofNumber(request.getIdProofNo());
		registration.setIdProofType(request.getIdProofType());
		registration.setIdProofImage(APIConstants.CUSTOMER_TOLL_PATH);
		registration.setUserId(user.getId());
		registration.setIpAddress(httpServletContext.getOriginIp());
		registration.setCreatedDateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
		registration.setCreatedBy(
				bankUser != null ? bankUser.getId() : merchantUser != null ? merchantUser.getId() : user.getId());

		List<TollTag> tollTaglist = new ArrayList<TollTag>();

		for (int index = 0; index < request.getVehicleList().size(); index++) {

			TollTag tollTag = new TollTag();

			if (request.getVehicleList().get(index).getId() != null) {

				tollTag.setId(request.getVehicleList().get(index).getId());
				tollTag.setStatus(request.getVehicleList().get(index).getStatus());
			} else if (registrationType == RegistrationType.IHMCL.value()) {

				// in this tagid and tid is allocated by IHMCL
				tollTag.setStatus(String.valueOf(DBConstants.TollTagApprovalStatus.ACTIVE.value()));
				tollTag.setTagId(request.getVehicleList().get(index).getTagId());
				tollTag.setTid(request.getVehicleList().get(index).getTid());

				for (FeesAndDeposit feesAndDeposit : feesAndDeposits) {
					if (feesAndDeposit.getChargeCategory().equals(request.getVehicleList().get(index).getCategory())
							|| feesAndDeposit.getBankId() == customerAccount.getBank().getId()) {
						tollTag.setAvailableAmount(feesAndDeposit.getMinimumAmount());
						tollTag.setRegistrationAmount(feesAndDeposit.getRegistrationAmount());
						tollTag.setMinimumAmount(feesAndDeposit.getMinimumAmount());
						tollTag.setDepositAmount(feesAndDeposit.getDepositAmount());
					}

				}

			} else {
				tollTag.setStatus(customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()
						? String.valueOf(DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value())
						: merchantUser != null
								? String.valueOf(DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value())
								: String.valueOf(DBConstants.TollTagApprovalStatus.PAYMENT_PENDING.value()));

				for (FeesAndDeposit feesAndDeposit : feesAndDeposits) {
					if (feesAndDeposit.getChargeCategory().equals(request.getVehicleList().get(index).getCategory())
							|| feesAndDeposit.getBankId() == customerAccount.getBank().getId()) {
						tollTag.setAvailableAmount(feesAndDeposit.getMinimumAmount());
						tollTag.setRegistrationAmount(feesAndDeposit.getRegistrationAmount());
						tollTag.setMinimumAmount(feesAndDeposit.getMinimumAmount());
						tollTag.setDepositAmount(feesAndDeposit.getDepositAmount());
					}

				}

			}

			tollTag.setRegistrationNo(
					request.getVehicleList().get(index).getRegistrationNo().replaceAll(" ", "").trim());
			tollTag.setCategory(request.getVehicleList().get(index).getCategory());
			tollTag.setCategoryId(request.getVehicleList().get(index).getCategoryId());
			tollTag.setType(request.getVehicleList().get(index).getType());
			tollTag.setIsCommercial(request.getVehicleList().get(index).getIsCommercial());
			tollTag.setRegisteredState(request.getVehicleList().get(index).getRegisteredState());
			tollTag.setEngineNo(request.getVehicleList().get(index).getEngineNo().toUpperCase());
			tollTag.setRegisteredVehicle(request.getVehicleList().get(index).getRegisteredVehicle());
			tollTag.setTagDescriptor(request.getVehicleList().get(index).getTagDescriptor());
			tollTag.setState(request.getVehicleList().get(index).getStateShortCode());
			tollTag.setVehicleDescriptor(request.getVehicleList().get(index).getVehicleDescriptor());

			if (!request.getVehicleList().get(index).getNationalPermit().isEmpty()) {
				tollTag.setNationalPermit(request.getVehicleList().get(index).getNationalPermit());
			}

			if (!request.getVehicleList().get(index).getPermitExpiryDate().isEmpty()) {
				tollTag.setPermitExpiryDate(request.getVehicleList().get(index).getPermitExpiryDate());
			}

			if (registrationType == RegistrationType.IHMCL.value()) {
				tollTag.setExcCode(TollConstant.EXC_CODE_ACTIVE);
			} else {
				tollTag.setExcCode(request.getVehicleList().get(index).getExcCode()
						.equalsIgnoreCase(String.valueOf(NumberUtils.INTEGER_ZERO)) ? TollConstant.EXC_CODE_ACTIVE
								: request.getVehicleList().get(index).getExcCode());
			}

			if (StringUtils.isNotBlank(request.getVehicleList().get(index).getExcCodeAck())) {
				tollTag.setExcCodeAcknowledge("T");
			}

			if (VinVrn.VIN_CHACIS.value() == request.getVehicleList().get(index).getVinVrnFlag()) {
				tollTag.setVinNumber(request.getVehicleList().get(index).getRegistrationNo().toUpperCase());
				tollTag.setVinVrnFlag(VinVrn.VIN_CHACIS.value());
			}

			if (!request.getVehicleList().get(index).getVin().isEmpty()) {
				tollTag.setVinNumber(request.getVehicleList().get(index).getVin());
			}

			tollTag.setAutoTopUp(request.getVehicleList().get(index).getAutoTopUp());
			tollTag.setCustomerAccountId(customerAccount.getId());
			tollTag.setBankId(customerAccount.getBank().getId());
			tollTag.setWalletBankId(customerAccount.getWalletBankId());
			tollTag.setRCImage(APIConstants.CUSTOMER_TOLL_PATH);
			tollTag.setAccountNumber(
					StringUtils.isBlank(request.getAccountNumber()) ? null : request.getAccountNumber());
			tollTag.setCreatedDateTime(String.valueOf(System.currentTimeMillis()));
			tollTag.setTollRegistration(registration);
			tollTag.setMiscCharges(
					request.getMiscCharges() ? TagDeliveryType.COURIER.value() : TagDeliveryType.HAND_DELIVERY.value());
			if (request.getAutoTopUpAmount() != null) {
				TopUpDetails topUpDetails = new TopUpDetails();
				topUpDetails.setAutoTopUp(request.getAutoTopUp());
				topUpDetails.setAutoTopUpAmount(request.getAutoTopUpAmount());
				topUpDetails.setAutoTopUpThresHold(request.getAutoTopUpThresHold());
				// minimumant is not set here
				tollTag.setTopUpDetails(objectMapper.writeValueAsString(topUpDetails));
			}

			if (merchantUser != null) {
				tollTag.setMerchantAmountPaid(MerchantAmountPaid.PENDING_BOTH_AMOUNT.value());
				tollTag.setMerchantId(merchantUser.getBankMerchantId());
			}

			tollTaglist.add(tollTag);

		}

		registration.setTollTag(tollTaglist);

		return registration;
	}

	// create Regitration request

	public ResponseEntity createFeesandDepositOsta(User user, User bankUser, Bank bank, CustomerAccount customerAccount,
			List<FeesAndDeposit> feesAndDeposits, TollRegistrationRequest createReq, Integer registrationType,
			final String clientTransactionId) throws Exception, APIException {

		List<Dipcoin> dCoins = coinDBService.asyncFindDipcoin(customerAccount.getId(),
				Arrays.asList(DBConstants.DipcoinUsageType.FEE.value(), DBConstants.DipcoinUsageType.DEPOSIT.value(),
						DBConstants.DipcoinUsageType.TOLL.value()))
				.get();

		boolean createFeeDipcoin = true;
		boolean createDepositDipcoin = true;
		boolean createMinimumAmountDipcoin = true;
		ResponseEntity registrationResponse = null;
		ResponseEntity depositResponse = null;
		ResponseEntity minimumResponse = null;
		ResponseEntity manageTagResponse = null;

		for (TollTagRequest tollTagRequest : createReq.getVehicleList()) {
			for (FeesAndDeposit feesAndDeposit : feesAndDeposits) {
				if ((tollTagRequest.getCategory()).equalsIgnoreCase(feesAndDeposit.getChargeCategory())) {

					if (CollectionUtils.isNotEmpty(dCoins)) {

						for (Dipcoin dcoin : dCoins) {
							if (tollTagRequest.getRegistrationNo().equalsIgnoreCase(dcoin.getUsageCategory())
									&& DBConstants.DipcoinUsageType.FEE.value() == dcoin.getUsageType()
									&& DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {
								createFeeDipcoin = false;
							}
							if (tollTagRequest.getRegistrationNo().equalsIgnoreCase(dcoin.getUsageCategory())
									&& DBConstants.DipcoinUsageType.DEPOSIT.value() == dcoin.getUsageType()
									&& DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {
								createDepositDipcoin = false;
							}
							if (tollTagRequest.getRegistrationNo().equalsIgnoreCase(dcoin.getUsageCategory())
									&& DBConstants.DipcoinUsageType.TOLL.value() == dcoin.getUsageType()
									&& DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {
								createMinimumAmountDipcoin = false;
							}
						}
					}

					if (createFeeDipcoin && (BigDecimal.ZERO
							.compareTo(feesAndDeposit.getRegistrationAmount()) < NumberUtils.INTEGER_ZERO
							|| createReq.getMiscCharges())) {

						CustomerDipcoinRequest createDcoinReq = new CustomerDipcoinRequest();

						createDcoinReq.setAmount(createReq.getMiscCharges()
								? feesAndDeposit.getRegistrationAmount().add(feesAndDeposit.getMiscellaneousCharges())
								: feesAndDeposit.getRegistrationAmount());
						createDcoinReq.setAccountId(customerAccount.getId());
						createDcoinReq.setCardId(customerAccount.getUserCardId());
						createDcoinReq.setCurrency(createReq.getCurrency());
						createDcoinReq.setAuthorizationPin(createReq.getAuthorizationPin());
						createDcoinReq.setUsageType(DBConstants.DipcoinUsageType.FEE.value());
						createDcoinReq.setUsageCategory(tollTagRequest.getRegistrationNo().replaceAll(" ", ""));
						createDcoinReq.setEncryptDipcoin(false);
						createDcoinReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);

						// for Bank SDK whose SSO is enabled, then no need of Osta Pin or TPin
						// validation
						if (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()
								&& customerAccount.getBank().getIsSsoEnabled() == BooleanStatus.YES.value()) {
							createDcoinReq.setValidateAuthorizationPin(false);
						}

						try {

							registrationResponse = customerDipcoinResource.createDipcoin(user, createDcoinReq, false);

						} catch (APIException e) {

							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Toll Registration Amount Dipcoin Generation Exception").format(),
									e);
							registrationResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {

							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Toll Registration Amount Dipcoin Generation Exception").format(),
									e);
							registrationResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (registrationResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
							return registrationResponse;
						}

					}

					if (createDepositDipcoin && BigDecimal.ZERO
							.compareTo(feesAndDeposit.getDepositAmount()) < NumberUtils.INTEGER_ZERO) {

						CustomerDipcoinRequest createDepositDcoinReq = new CustomerDipcoinRequest();

						createDepositDcoinReq.setAmount(feesAndDeposit.getDepositAmount());
						createDepositDcoinReq.setAccountId(customerAccount.getId());
						createDepositDcoinReq.setCardId(customerAccount.getUserCardId());
						createDepositDcoinReq.setCurrency(createReq.getCurrency());
						createDepositDcoinReq.setAuthorizationPin(createReq.getAuthorizationPin());
						createDepositDcoinReq.setUsageType(DBConstants.DipcoinUsageType.DEPOSIT.value());
						createDepositDcoinReq.setUsageCategory(tollTagRequest.getRegistrationNo().replaceAll(" ", ""));
						createDepositDcoinReq.setEncryptDipcoin(false);
						createDepositDcoinReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);

						// for Bank SDK whose SSO is enabled, then no need of Osta Pin or TPin
						// validation
						if (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()
								&& customerAccount.getBank().getIsSsoEnabled() == BooleanStatus.YES.value()) {
							createDepositDcoinReq.setValidateAuthorizationPin(false);
						}

						try {

							depositResponse = customerDipcoinResource.createDipcoin(user, createDepositDcoinReq, false);

						} catch (APIException e) {

							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Deposit Amount Dipcoin Generation Exception").format(), e);
							depositResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Deposit Amount Dipcoin Generation Exception").format(), e);
							depositResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (depositResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
							if (registrationResponse != null) {

								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) registrationResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							return depositResponse;
						}

					}

					if (createMinimumAmountDipcoin && BigDecimal.ZERO
							.compareTo(feesAndDeposit.getMinimumAmount()) < NumberUtils.INTEGER_ZERO) {

						TollRechargeRequest rechargeReq = new TollRechargeRequest();
						rechargeReq.setAuthorizationPin(createReq.getAuthorizationPin());
						rechargeReq.setCustomerAccountId(customerAccount.getId());
						rechargeReq.setCardId(customerAccount.getUserCardId());
						rechargeReq.setCurrency(createReq.getCurrency());

						BigDecimal rechargeAmount = feesAndDeposit.getMinimumAmount();
						if ((registrationType == RegistrationType.IHMCL.value() || bankUser != null)
								&& createReq.getAutoTopUpAmount() != null && BigDecimal.ZERO
										.compareTo(createReq.getAutoTopUpAmount()) < NumberUtils.INTEGER_ZERO) {

							rechargeAmount = rechargeAmount.add(createReq.getAutoTopUpAmount());
						}

						rechargeReq.setRechargeAmount(rechargeAmount);
						rechargeReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
						rechargeReq.setType("NETC");
						rechargeReq.setAutomaticFlag("false");
						rechargeReq.setUsageCategory(tollTagRequest.getRegistrationNo().replaceAll(" ", ""));

						try {
							minimumResponse = tollRechargeResource.tollRechargeCreateOsta(user, rechargeReq,
									clientTransactionId, null);
						} catch (APIException e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Minimum Amount Dipcoin Generation Exception").format(), e);
							minimumResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Minimum Amount Dipcoin Generation Exception").format(), e);
							minimumResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (minimumResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {

							if (registrationResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Registration Amount").format());

								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) registrationResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							if (depositResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Deposit Amount").format());

								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) depositResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							return minimumResponse;
						}
					}

				}
			}

			if (registrationType == RegistrationType.IHMCL.value()) {

				// Activated the ihmcl Customer.
				TollRegistration registeration = populateForSaveAndUpdateTollCustomer(user, null, createReq,
						clientTransactionId, customerAccount, feesAndDeposits, registrationType, null);

				for (TollTag tollTag : registeration.getTollTag()) {

					if (tollTag.getRegistrationNo().equalsIgnoreCase(tollTagRequest.getRegistrationNo())) {

						try {
							manageTagResponse = this.customerTollTagActivation(user, null, tollTag, registrationType);
						} catch (APIException e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Ihmcl Activation Failed").format(), e);
							manageTagResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Ihmcl Activation Failed").format(), e);
							manageTagResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (HttpStatus.BAD_REQUEST.value() <= manageTagResponse.getStatusCodeValue()) {

							if (registrationResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Registration Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) registrationResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							if (depositResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Deposit Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) depositResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							if (minimumResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Minimum Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) minimumResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}
							return manageTagResponse;
						}
					}
				}
			}
		}
		return ResponseEntity.status(HttpStatus.OK).body(new APIResponse().addHeaderCode(HeaderCode.SUCCESS));
	}

	// update Regitration request

	public ResponseEntity createFeesandDepositOsta(TollRegistrationRequest updateReq, User user, User bankUser,
			CustomerAccount customerAccount, List<FeesAndDeposit> feesAndDeposits, Integer registrationType,
			final String clientTransactionId) throws Exception, APIException {

		List<Dipcoin> dCoins = coinDBService.asyncFindDipcoin(customerAccount.getId(),
				Arrays.asList(DBConstants.DipcoinUsageType.FEE.value(), DBConstants.DipcoinUsageType.DEPOSIT.value(),
						DBConstants.DipcoinUsageType.TOLL.value()))
				.get();

		boolean createFeeDipcoin = true;
		boolean createDepositDipcoin = true;
		boolean createMinimumAmountDipcoin = true;
		ResponseEntity registrationResponse = null;
		ResponseEntity depositResponse = null;
		ResponseEntity minimumResponse = null;
		ResponseEntity manageTagResponse = null;

		for (Map.Entry<String, String> vehicleInfo : vehicleRegistrationNo.entrySet()) {
			for (FeesAndDeposit feesAndDeposit : feesAndDeposits) {
				if (vehicleInfo.getValue().equalsIgnoreCase(feesAndDeposit.getChargeCategory())) {

					if (CollectionUtils.isNotEmpty(dCoins)) {

						for (Dipcoin dcoin : dCoins) {
							if (vehicleInfo.getKey().equalsIgnoreCase(dcoin.getUsageCategory())
									&& DBConstants.DipcoinUsageType.FEE.value() == dcoin.getUsageType()
									&& DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {
								createFeeDipcoin = false;
							}
							if (vehicleInfo.getKey().equalsIgnoreCase(dcoin.getUsageCategory())
									&& DBConstants.DipcoinUsageType.DEPOSIT.value() == dcoin.getUsageType()
									&& DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {
								createDepositDipcoin = false;
							}
							if (vehicleInfo.getKey().equalsIgnoreCase(dcoin.getUsageCategory())
									&& DBConstants.DipcoinUsageType.TOLL.value() == dcoin.getUsageType()
									&& DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {
								createMinimumAmountDipcoin = false;
							}
						}

					}

					if (createFeeDipcoin && BigDecimal.ZERO
							.compareTo(feesAndDeposit.getRegistrationAmount()) < NumberUtils.INTEGER_ZERO) {

						CustomerDipcoinRequest createDcoinReq = new CustomerDipcoinRequest();

						createDcoinReq.setAmount(updateReq.getMiscCharges()
								? feesAndDeposit.getRegistrationAmount().add(feesAndDeposit.getMiscellaneousCharges())
								: feesAndDeposit.getRegistrationAmount());
						createDcoinReq.setAccountId(customerAccount.getId());
						createDcoinReq.setCardId(customerAccount.getUserCardId());
						createDcoinReq.setCurrency(updateReq.getCurrency());
						createDcoinReq.setAuthorizationPin(updateReq.getAuthorizationPin());
						createDcoinReq.setUsageType(DBConstants.DipcoinUsageType.FEE.value());
						createDcoinReq.setUsageCategory(vehicleInfo.getKey());
						createDcoinReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);
						createDcoinReq.setEncryptDipcoin(false);

						// for Bank SDK whose SSO is enabled, then no need of Osta Pin or TPin
						// validation
						if (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()
								&& customerAccount.getBank().getIsSsoEnabled() == BooleanStatus.YES.value()) {
							createDcoinReq.setValidateAuthorizationPin(false);
						}

						try {

							registrationResponse = customerDipcoinResource.createDipcoin(user, createDcoinReq, false);

						} catch (APIException e) {

							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Toll Registration Amount Dipcoin Generation Exception").format(),
									e);
							registrationResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {
							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Toll Registration Amount Dipcoin Generation Exception").format(),
									e);
							registrationResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (registrationResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
							return registrationResponse;
						}
					}

					if (createDepositDipcoin && BigDecimal.ZERO
							.compareTo(feesAndDeposit.getDepositAmount()) < NumberUtils.INTEGER_ZERO) {

						CustomerDipcoinRequest createDepositDcoinReq = new CustomerDipcoinRequest();

						createDepositDcoinReq.setAmount(feesAndDeposit.getDepositAmount());
						createDepositDcoinReq.setAccountId(customerAccount.getId());
						createDepositDcoinReq.setCardId(customerAccount.getUserCardId());
						createDepositDcoinReq.setCurrency(updateReq.getCurrency());
						createDepositDcoinReq.setAuthorizationPin(updateReq.getAuthorizationPin());
						createDepositDcoinReq.setUsageType(DBConstants.DipcoinUsageType.DEPOSIT.value());
						createDepositDcoinReq.setUsageCategory(vehicleInfo.getKey());
						createDepositDcoinReq.setEncryptDipcoin(false);
						createDepositDcoinReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);

						// for Bank SDK whose SSO is enabled, then no need of Osta Pin or TPin
						// validation
						if (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()
								&& customerAccount.getBank().getIsSsoEnabled() == BooleanStatus.YES.value()) {
							createDepositDcoinReq.setValidateAuthorizationPin(false);
						}

						try {

							depositResponse = customerDipcoinResource.createDipcoin(user, createDepositDcoinReq, false);

						} catch (APIException e) {

							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Deposit Amount Dipcoin Generation Exception").format(), e);
							depositResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Deposit Amount Dipcoin Generation Exception").format(), e);
							depositResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (depositResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
							if (registrationResponse != null) {
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) registrationResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							return depositResponse;
						}
					}

					if (createMinimumAmountDipcoin && BigDecimal.ZERO
							.compareTo(feesAndDeposit.getMinimumAmount()) < NumberUtils.INTEGER_ZERO) {

						TollRechargeRequest rechargeReq = new TollRechargeRequest();
						rechargeReq.setAuthorizationPin(updateReq.getAuthorizationPin());
						rechargeReq.setCustomerAccountId(customerAccount.getId());
						rechargeReq.setCardId(customerAccount.getUserCardId());
						rechargeReq.setCurrency(updateReq.getCurrency());

						BigDecimal rechargeAmount = feesAndDeposit.getMinimumAmount();
						if ((registrationType == RegistrationType.IHMCL.value() || bankUser != null)
								&& updateReq.getAutoTopUpAmount() != null && BigDecimal.ZERO
										.compareTo(updateReq.getAutoTopUpAmount()) < NumberUtils.INTEGER_ZERO) {

							rechargeAmount = rechargeAmount.add(updateReq.getAutoTopUpAmount());
						}

						rechargeReq.setRechargeAmount(rechargeAmount);
						rechargeReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
						rechargeReq.setType("NETC");
						rechargeReq.setAutomaticFlag("false");
						rechargeReq.setUsageCategory(vehicleInfo.getKey());

						try {

							minimumResponse = tollRechargeResource.tollRechargeCreateOsta(user, rechargeReq,
									clientTransactionId, null);

						} catch (APIException e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Minimum Amount Dipcoin Generation Exception").format(), e);
							minimumResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Minimum Amount Dipcoin Generation Exception").format(), e);
							minimumResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (minimumResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
							if (registrationResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Registration Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) registrationResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							if (depositResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Deposit Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) depositResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}
							return minimumResponse;
						}
					}
				}
			}

			// Activate IHMCL Customer.
			if (registrationType == RegistrationType.IHMCL.value()) {
				TollRegistration registeration = populateForSaveAndUpdateTollCustomer(user, null, updateReq,
						clientTransactionId, customerAccount, feesAndDeposits, registrationType, null);

				for (TollTag tollTag : registeration.getTollTag()) {

					if (tollTag.getRegistrationNo().equalsIgnoreCase(vehicleInfo.getValue())) {

						try {
							manageTagResponse = this.customerTollTagActivation(user, null, tollTag, registrationType);
						} catch (APIException e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Ihmcl Activation Failed").format(), e);
							manageTagResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
						} catch (Exception e) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Toll Ihmcl Activation Failed").format(), e);
							manageTagResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
						}

						if (HttpStatus.BAD_REQUEST.value() <= manageTagResponse.getStatusCodeValue()) {

							if (registrationResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Registration Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) registrationResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							if (depositResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Deposit Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) depositResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							if (minimumResponse != null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Reverting Minimum Amount").format());
								CustomerDipcoinResponse cdResponse = (CustomerDipcoinResponse) minimumResponse
										.getBody();

								customerDipcoinResource.deleteDipcoin(user, cdResponse.getOsta(), false);

							}

							return manageTagResponse;
						}
					}
				}
			}
		}

		return ResponseEntity.status(HttpStatus.OK).body(new APIResponse().addHeaderCode(HeaderCode.SUCCESS));
	}

	/*
	 * Activate Toll Tag from Customer.
	 */
	public ResponseEntity customerTollTagActivation(final User user, final String encTtid, final TollTag tolltag,
			final int regType) throws Exception, APIException {

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		String decTtid = encTtid;

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Activate TollTag")
				.data("UserId", user.getId()).data("enc", encTtid).data("tolltag", tolltag).format());

		TollTagResponse response = new TollTagResponse();

		TollTag tollTag = RegistrationType.DEFAULT.value() == regType
				? this.tollDBService.findTollTagById(Integer.parseInt(decTtid))
				: tolltag;

		if (tollTag == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_ACCOUNT_DOESNT_EXIST));
		}

		// Making the Deep Object to map the Error Response in tollTag Table

		TollTag tollTagDeepCopy = RegistrationType.DEFAULT.value() == regType
				? objectMapper.readValue(objectMapper.writeValueAsString(tollTag), TollTag.class)
				: null;

		// Get Merchant
		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();

		if (CollectionUtils.isEmpty(merchants)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("merchant is null").format());
			response.addHeaderCode(HeaderCode.MERCHANT_DOESNT_EXIST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// Get Bank
		Bank bank = bankDBService.getBank(tollTag.getBankId());

		if (bank == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank is null")
					.data("BankId", tollTag.getBankId()).format());
			response.addHeaderCode(HeaderCode.BANK_DOESNT_EXISTS);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// Updating the NETC Services

		if (tollProperties.isConnectNpci()) {
			TollTagUpdateResponse tollTagUpdateResponse = this.tollServiceBankResource.uploadTheTollListToNETC(
					httpServletContext, Arrays.asList(tollTag), TollConstant.ADD_OP, bank, merchants.get(0));
			if (tollTagUpdateResponse == null) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("TollTagUpdateResponse is null").format());
				response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			TollTagUpdateResponse tollTagUpdateResp = objectMapper
					.readValue(objectMapper.writeValueAsString(tollTagUpdateResponse), TollTagUpdateResponse.class);
			tollTagUpdateResp.getTxn().getResp().getTag().clear();

			List<HeaderCode> headerCodes = new ArrayList<>();
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

				tollTagUpdateResp.getTxn().getResp().setTag(Arrays.asList(tag));

				tollTag.setAddTagResponse(objectMapper.writeValueAsString(tollTagUpdateResp));
				tollTag.setAddTagErrorCode(tag.getErrCode());
				if (tollTagDeepCopy != null) {
					tollTagDeepCopy.setAddTagResponse(objectMapper.writeValueAsString(tollTagUpdateResp));
					tollTagDeepCopy.setAddTagErrorCode(tag.getErrCode());
				}

			}

			if (tollTagUpdateResponse != null
					&& !tollTagUpdateResponse.getTxn().getResp().getTag().get(0).getErrCode()
							.equals(TollErrorCodes.Error_code_SUCCESS.code())
					&& !HeaderCode.VEHICLE_REGNO_OR_VIN_OR_ENGINENUMBER_ALREADY_REGISTERED_WITH_SOME_OTHER_TAGID.code()
							.contains(tollTagUpdateResponse.getTxn().getResp().getTag().get(0).getErrCode())
					&& tollTagUpdateResponse.getTxn().getResp().getTag().get(0).getResult()
							.equalsIgnoreCase(TollErrorCodes.Error_code_SUCCESS.message())) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("tollTagDeepCopy")
						.data("tollTagDeepCopy", tollTagDeepCopy).format());

				if (tollTagDeepCopy != null) {
					TollRegistration tollRegistration = tollTag.getTollRegistration();
					tollRegistration.setStatus(String.valueOf(DBConstants.TollRegistrationStatus.DEFAULT.value()));
					tollTagDeepCopy.setTollRegistration(tollRegistration);

					this.tollDBService.updateTollTag(tollTagDeepCopy);
				}
				headerCodes.add(HeaderCode.TOLL_PREVIOUSLY_ISSUED_TAG_IS_INVALID);
				response.addHeaderCodes(headerCodes);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

			}
		}

		if (RegistrationType.IHMCL.value() != regType) {
			// Set Toll Tag as Active
			tollTag.setStatus(String.valueOf(TollTagApprovalStatus.ACTIVE.value()));

			// Updating the Toll Tag
			TollTag updatetollTag = this.tollDBService.updateTollTag(tollTag);

			if (updatetollTag == null) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("updatetollTag is null")
						.format());
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.TOLL_TAG_DOESNT_EXIST));
			}

			// Populating the TollTag Id and Status
			response.setTagDetailsId(updatetollTag.getId());
			response.setStatus(updatetollTag.getStatus());
		}

		try {

			// success sms
			if (!applicationProperties.getAwsSMSClient()
					&& !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
							Templates.TollTagCustomerActivation.format(APIConstants.TOLL_TYPE_NAME), true)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
						.data("phone", tollTag.getTollRegistration().getMobileNo()).format());
			}

			if (applicationProperties.getAwsSMSClient()) {

				NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
				notificationRequestContext.setTraceId(httpServletContext.getTraceId());
				if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
						Templates.TollTagCustomerActivation.format(APIConstants.TOLL_TYPE_NAME),
						httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

					LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", tollTag.getTollRegistration().getMobileNo()).format());

				}
			}
			// Email
			if (!this.tollEmailUtils.tollTagCustomerActivation(APIConstants.TOLL_TYPE_NAME,
					tollTag.getTollRegistration())) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to send email to customer after activation.")
						.data("user email", tollTag.getTollRegistration().getEmailId()).format());
			}

		} catch (Exception e) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Exception in sending email & sms to customer after activation.").format());
		}

		// Adding the count to metrix
		tollMetricRegistry.numberOfTagsActivatedByCustomer().increment();

		response.addHeaderCode(HeaderCode.TOLL_TAG_ACTIVATED);
		return ResponseEntity.status(HttpStatus.OK).body(response);

	}
	
	/*
	 * Add And Update without MULTIPART toll Collection Customer setting idProof and
	 * rcDoc from the resource for APK call.
	 */
	public ResponseEntity addAndUpdateTollCustomer(User user, User bankUser, Bank bank, final String request,
			final Integer registrationType, final String clientTransactionId) throws Exception, APIException {

		TollRegistrationResponse response = new TollRegistrationResponse();
		List<FeesAndDeposit> feesAndDeposits = null;

		ResponseEntity responseEntity = null;
		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		if (!userDBService.isCustomer(user) && bank == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}
		if (!this.userDBService.isActive(user) && bank == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		response.setClientTransactionId(clientTransactionId);

		if (request == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
		}

		// set idProof and rcDoc from the resource for APK call.

		MultipartFile idProof = setIdProof();

		MultipartFile[] rcDoc = setRcDoc();

		MultipartFile filepart = idProof;
		if (null == filepart || NumberUtils.INTEGER_ZERO == rcDoc.length) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_IMAGE_MISSING));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("filepart").data("filepart", filepart)
				.format());

		LOG.debug(
				LogFormatter.instance(httpServletContext.getTraceId()).message("rcDoc").data("rcDoc", rcDoc).format());

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("TollRegistrationRequest")
				.data("request", request).format());
		final TollRegistrationRequest createReq = new ObjectMapper().readValue(request, TollRegistrationRequest.class);

		// If request is called from bank
		if (bank != null) {
			// Get the List

			List<CustomerAccount> existingAccounts = this.customerDBService.asyncGetAccounts(user.getId()).get();
			if (CollectionUtils.isEmpty(existingAccounts)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.MISSING_USER_CARDID.message()).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.MISSING_USER_CARDID));
			}

			for (CustomerAccount account : existingAccounts) {
				if (((StringUtils.isNotEmpty(account.getHashedAccountNumber()) && account.getHashedAccountNumber()
						.equals(Utils.getHashedLogin(StringUtils.stripStart(createReq.getAccountNumber(), "0"))))
						|| (StringUtils.isNotEmpty(account.getHashedLogin()) && account.getHashedLogin().equals(
								Utils.getHashedLogin(StringUtils.stripStart(createReq.getAccountNumber(), "0")))))
						&& CustomerAccountStatus.ACTIVE.value() == account.getStatus()) {
					createReq.setCardId(account.getUserCardId());
					break;
				}
			}

			// Fetch the registered User.from bank
			List<User> users = this.userDBService.getUsers(createReq.getMobileNo(), 0,
					Arrays.asList(UserRoles.CUSTOMER.value()), DBConstants.UserStatus.ACTIVE.value());
			// overide the bankuser object to
			// registered customer user object.
			user = users.get(0);

			// Seeting the Autorization Pin
			createReq.setAuthorizationPin(APIConstants.PIN);

		}

		CustomerAccount customerAccount = customerDBService.getAccount(user.getId(), createReq.getCardId());
		if (customerAccount == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.MISSING_INVALID_INFO.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));

		}

		// pass lien debit credit method
		/*
		 * ResponseEntity lienDebitRes = this.lienDebitCredit(customerAccount, request);
		 * 
		 * if (HttpStatus.BAD_REQUEST.value() <= lienDebitRes.getStatusCodeValue()) {
		 * return lienDebitRes; }
		 */

		TollRegistration tollRegistration = null;

		if (createReq.getId() != null && createReq.getId() != NumberUtils.INTEGER_ZERO) {
			TollRegistration tollReg = this.tollDBService.findById(createReq.getId());

			if (null == tollReg) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
			}
			if (isProfileChanged(tollReg, createReq)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Profile has been Changed")
						.format());
				createReq.setId(NumberUtils.INTEGER_ZERO);
				if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {
					responseEntity = getFeesAndDeposits(user, createReq, clientTransactionId, customerAccount,
							registrationType);
					if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
						if (this.vehicleNumberLock != null) {
							try {
								this.vehicleNumberLock.unlock();
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("vehicleNumberLock unlocked").format());
							} catch (Exception ex) {
								LOG.debug(
										LogFormatter.instance(httpServletContext.getTraceId())
												.message("Exception Caught while unlocking vehicleNumberLock").format(),
										ex);
							}
						}
						return responseEntity;
					}
					feesAndDeposits = (List<FeesAndDeposit>) responseEntity.getBody();
				}

			} else {

				createReq.setStatus(tollReg.getStatus());
				if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {
					vehicleRegistrationNo.clear();
					responseEntity = getFeesAndDeposits(user, createReq, clientTransactionId, customerAccount, tollReg,
							registrationType);
					if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
						if (this.vehicleNumberLock != null) {
							try {
								this.vehicleNumberLock.unlock();
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("vehicleNumberLock unlocked").format());
							} catch (Exception ex) {
								LOG.debug(
										LogFormatter.instance(httpServletContext.getTraceId())
												.message("Exception Caught while unlocking vehicleNumberLock").format(),
										ex);
							}
						}
						return responseEntity;
					}
					feesAndDeposits = (List<FeesAndDeposit>) responseEntity.getBody();
				}
			}

		} else {
			if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {

				responseEntity = getFeesAndDeposits(user, createReq, clientTransactionId, customerAccount,
						registrationType);
				if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
					if (this.vehicleNumberLock != null) {
						try {
							this.vehicleNumberLock.unlock();
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("vehicleNumberLock unlocked").format());
						} catch (Exception ex) {
							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Exception Caught while unlocking vehicleNumberLock").format(),
									ex);
						}
					}
					return responseEntity;
				}
				feesAndDeposits = (List<FeesAndDeposit>) responseEntity.getBody();
			}
		}

		if (createReq.getId() != NumberUtils.INTEGER_ZERO && createReq.getId() != null) {
			if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {
				// commenting because of proxy issue.

				responseEntity = createFeesandDepositOsta(createReq, user, bankUser, customerAccount, feesAndDeposits,
						registrationType, clientTransactionId);
				if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {
					if (this.vehicleNumberLock != null) {
						try {
							this.vehicleNumberLock.unlock();
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("vehicleNumberLock unlocked").format());
						} catch (Exception ex) {
							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Exception Caught while unlocking vehicleNumberLock").format(),
									ex);
						}
					}

					return responseEntity;
				}
			} else {
				feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(
						Arrays.asList(customerAccount.getWalletBankId()),
						new HashSet<String>(Arrays.asList(createReq.getVehicleList().get(0).getCategory())));

				if (CollectionUtils.isEmpty(feesAndDeposits)) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE.message()).format());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));

				}
			}
			// Call for populating the request for save
			TollRegistration registerationSaveObj = populateForSaveAndUpdateTollCustomer(user, bankUser, createReq,
					clientTransactionId, customerAccount, feesAndDeposits, registrationType, null);

			// update the registration details.
			tollRegistration = this.tollDBService.asyncUpdate(registerationSaveObj).get();

			if (null == tollRegistration) {
				if (this.vehicleNumberLock != null) {
					try {
						this.vehicleNumberLock.unlock();
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("vehicleNumberLock unlocked").format());
					} catch (Exception ex) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Exception Caught while unlocking vehicleNumberLock").format(), ex);
					}
				}

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}

			setFilePath(tollRegistration, user, filepart, createReq, rcDoc);

			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Updated").format());

			vehicleRegistrationNo.clear();
			response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		} else {
			if (customerAccount.getTypeOfMethod() != CustomerAccountMethodType.WALLET_INB.value()) {

				// commenting because of proxy issue
				responseEntity = createFeesandDepositOsta(user, bankUser, bank, customerAccount, feesAndDeposits,
						createReq, registrationType, clientTransactionId);
				if (HttpStatus.BAD_REQUEST.value() <= responseEntity.getStatusCodeValue()) {

					if (this.vehicleNumberLock != null) {
						try {
							this.vehicleNumberLock.unlock();
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("vehicleNumberLock unlocked").format());
						} catch (Exception ex) {
							LOG.debug(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("Exception Caught while unlocking vehicleNumberLock").format(),
									ex);
						}
					}

					return responseEntity;
				}
			} else {
				feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(
						Arrays.asList(customerAccount.getWalletBankId()),
						new HashSet<String>(Arrays.asList(createReq.getVehicleList().get(0).getCategory())));

				if (CollectionUtils.isEmpty(feesAndDeposits)) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE.message()).format());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));

				}
			}

			// Call for populating the request for save
			TollRegistration registerationSaveObj = populateForSaveAndUpdateTollCustomer(user, bankUser, createReq,
					clientTransactionId, customerAccount, feesAndDeposits, registrationType, null);

			tollRegistration = tollDBService.asyncSave(registerationSaveObj).get();

			if (null == tollRegistration) {

				if (this.vehicleNumberLock != null) {
					try {
						this.vehicleNumberLock.unlock();
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("vehicleNumberLock unlocked").format());
					} catch (Exception ex) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Exception Caught while unlocking vehicleNumberLock").format(), ex);
					}
				}
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}

			setFilePath(tollRegistration, user, filepart, createReq, rcDoc);

			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Created").format());
			populateTollCustomerResponse(user, createReq, response);
			response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		}

		try {

			StringBuilder tags = new StringBuilder();
			for (TollTag tolltag : tollRegistration.getTollTag()) {
				if (tollRegistration.getTollTag().size() > 1) {
					tags.append(",").append(tolltag.getRegistrationNo());
				} else {
					tags.append(tolltag.getRegistrationNo());
				}
			}

			// success sms
			if (!smsClient.sendSms(tollRegistration.getMobileNo(),
					Templates.TollTagRegistration.format(tags.toString(),
							customerAccount.getBank().getAlias() == null ? customerAccount.getBank().getName()
									: customerAccount.getBank().getAlias()),
					true)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
						.data("phone", tollRegistration.getMobileNo()).format());
			}

			if (applicationProperties.getAwsSMSClient()) {

				NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
				notificationRequestContext.setTraceId(httpServletContext.getTraceId());
				if (!notificationResource.sendSms(tollRegistration.getMobileNo(),
						Templates.TollTagRegistration.format(tags.toString(),
								customerAccount.getBank().getAlias() == null ? customerAccount.getBank().getName()
										: customerAccount.getBank().getAlias()),
						httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

					LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", tollRegistration.getMobileNo()).format());

				}
			}

			// Email
			if (!this.tollEmailUtils.sendTollCustomerRegistrationEmail(tags.toString(), customerAccount.getBank(),
					tollRegistration)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to send email to Cutomer for Registration success.")
						.data("user email", tollRegistration.getEmailId()).format());
			}

		} catch (Exception exception) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Exception in sending email & sms to Customer." + exception).format());
		} finally {

			if (this.vehicleNumberLock != null) {
				try {
					this.vehicleNumberLock.unlock();
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("vehicleNumberLock unlocked").format());
				} catch (Exception ex) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Exception Caught while unlocking vehicleNumberLock").format(), ex);
				}
			}
		}
		// Adding the count to metrix
		tollMetricRegistry.numberOfTagsApplied().increment();

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}
	
	private MultipartFile[] setRcDoc() {
		try {
	        // Load rcDoc.png from resources
	        ClassPathResource resource = new ClassPathResource("RCImage.png");
	        InputStream inputStream = resource.getInputStream();

	        MultipartFile rcFile = new MockMultipartFile(
	                "rcDoc",                        // form field name
	                resource.getFilename(),         // original filename
	                "image/png",                    // content type
	                inputStream                     // file content
	        );

	        return new MultipartFile[]{ rcFile };
	    } catch (IOException e) {
	        throw new RuntimeException("Failed to load rcDoc.png from resources", e);
	    }
	}

	private MultipartFile setIdProof() {
		try {
	        // Load idProof.png from resources
	        ClassPathResource resource = new ClassPathResource("idProof.png");
	        InputStream inputStream = resource.getInputStream();

	        return new MockMultipartFile(
	                "idProof",                      // form field name
	                resource.getFilename(),         // original filename
	                "image/png",                    // content type
	                inputStream                     // file content
	        );
	    } catch (IOException e) {
	        throw new RuntimeException("Failed to load idProof.png from resources", e);
	    }
	}

	/*
	 * Delete the Toll Customer
	 */

	public ResponseEntity deleteTollCustomerAccount(final User user, final String encTcid)
			throws Exception, APIException {

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		// Decrypt the Toll customer id
		String decTcid = encryptCardId ? encryptionResource.decrypt(user, null, null, encTcid) : encTcid;
		if (StringUtils.isBlank(decTcid)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.INVALID_ENCRYPTED_DATA));
		}
		Integer tcid = Integer.parseInt(decTcid);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Delete TollCustomer Account")
				.data("UserId", user.getId()).data("TollId", tcid).format());
		TollRegistrationResponse response = new TollRegistrationResponse();

		TollRegistration tollCustomerAccount = this.tollDBService.findById(tcid);

		if (tollCustomerAccount == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_ACCOUNT_DOESNT_EXIST));
		}

		// Updating the tollCustomer to Deactivate
		tollCustomerAccount.setStatus(String.valueOf(TollRegistrationStatus.INACTIVE.value()));

		List<TollTag> listTollTag = new ArrayList<>();

		for (int i = 0; i < tollCustomerAccount.getTollTag().size(); i++) {

			TollTag tollTag = tollCustomerAccount.getTollTag().get(i);

			tollTag.setStatus(String.valueOf(TollTagApprovalStatus.INACTIVE.value()));
			tollTag.setExcCode(TollConstant.EXC_CODE_CLOSED_OR_REPLACED);

			if (tollProperties.isConnectNpci()) {
				try {
					brontooResource.updateExceptionList(tollTag, TollConstant.ADD_OP, null);
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("update exccode exception")
							.format());
				}

			}

			listTollTag.add(tollTag);

		}

		tollCustomerAccount.setTollTag(listTollTag);

		// Updating the Toll Customer
		TollRegistration updateregistrationTollCustomer = this.tollDBService.update(tollCustomerAccount);

		if (updateregistrationTollCustomer == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));

		}
		response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_INACTIVE);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

	}

	public ResponseEntity getTagRechargeReport(User user, Long startTime, Long endTime, Integer start, Integer count,
			String vehicleNumber) throws InterruptedException, ExecutionException {

		TollRechargeResponse response = new TollRechargeResponse();
		if (user != null) {
			if (!(this.userDBService.isCustomer(user) || this.userDBService.isBankSuperAdmin(user))) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("User is neither a customer nor  a bankSuper admin").format());
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
			}

			if (!this.userDBService.isActive(user)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("User not Active").format());
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
			}
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("startTime", startTime)
					.data("endTime", endTime).data("start", start).data("count", count)
					.data("Vehicle Number", vehicleNumber).format());

			List<TollRechargeReceipt> tollRechargeReceipts = new LinkedList<>();
			List<TollRecharge> tollTagRechargeReport = this.tollRechargeDBService
					.findTollRechargeByUserIdAndVehicleNumber(user.getId(), start, count, startTime, endTime,
							vehicleNumber);

			if (CollectionUtils.isEmpty(tollTagRechargeReport)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Data not found").format());
				return ResponseEntity.status(HttpStatus.OK).body(tollRechargeReceipts);
			}
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Data found")
					.data("list Size", tollTagRechargeReport.size()).format());

			for (TollRecharge tollRecharge : tollTagRechargeReport) {

				TollRechargeReceipt tollRechargeReceipt = new TollRechargeReceipt();
				tollRechargeReceipt.setAvailableAmount(String.valueOf(tollRecharge.getAvailableAmount()));
				tollRechargeReceipt.setDateAndTime(tollRecharge.getRechargeTime());
				tollRechargeReceipt.setVehicleNumber(tollRecharge.getVehicleNo());
				tollRechargeReceipt.setStatus(tollRecharge.getStatus());
				tollRechargeReceipt.setOstaTransactionRefId(tollRecharge.getDipcoinTransactionRefId());
				tollRechargeReceipt.setRechargeAmount(tollRecharge.getRechargeAmount());
				TollTag tag = null;

				// check if tollRecharge.getTagId() is serial number
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Data found")
						.data("tollRecharge.getTagId()", tollRecharge.getTagId()).format());
				if (!StringUtils.isEmpty(tollRecharge.getTagId()) && tollRecharge.getTagId().contains("-")) {

					Epc epc = tollDBService.asyncFindEpcBySerialNumber(tollRecharge.getTagId()).get();
					if (epc != null) {
						tag = tollDBService.asyncFindTollCustomersByTagId(epc.getRfidTag()).get();
					} else {
						tag = this.tollDBService.findTollTagByCustomerAccountIdAndTagIdAndRegistrationNo(
								tollRecharge.getCustomerAccountId(), tollRecharge.getTagId(),
								tollRecharge.getVehicleNo());
					}
				}

				if (tag != null) {

					if (StringUtils.isEmpty(tag.getAccountNumber())) {
						CustomerAccount customerAccount = customerDBService
								.getAccountById(tollRecharge.getCustomerAccountId());
						tollRechargeReceipt.setAccountNumber(
								customerAccount != null ? customerAccount.getRawAccountNumber() : null);
					} else {
						tollRechargeReceipt.setAccountNumber(tag.getAccountNumber());
					}
				}

				tollRechargeReceipts.add(tollRechargeReceipt);

			}
			// set pagination info
			Pagination pagination = new Pagination();
			pagination.setStartRange(startTime);
			pagination.setEndRange(endTime);
			pagination.setScanCompleted(tollRechargeReceipts.size() < count);
			pagination.setTotal(tollRechargeReceipts.size());
			if (!pagination.getScanCompleted()) {
				pagination.setStart(start + count);
			}

			response.setPagination(pagination);
			response.setTollRechargeResponse(tollRechargeReceipts);

			return ResponseEntity.status(HttpStatus.OK).body(response);
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));
	}

	/*
	 * Delete the Toll Tag
	 */
	public ResponseEntity deactivateTollTagByCustomer(final User user, final String encTtid)
			throws Exception, APIException {

		httpServletContext.setClientTransactionId(CoreUtils.randomAlphaString(8));

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		// Decrypt the Toll customer id
		// String decTtid = encryptCardId ? encryptionResource.decrypt(user, null, null,
		// encTtid) : encTtid;
		String decTtid = encTtid;
		if (StringUtils.isBlank(decTtid)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.INVALID_ENCRYPTED_DATA));
		}
		Integer ttid = Integer.parseInt(decTtid);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Deactivate TollTag Account")
				.data("UserId", user.getId()).data("TollTagId", decTtid).format());
		TollTagResponse response = new TollTagResponse();

		TollTag tollTagDetails = this.tollDBService.findTollTagById(ttid);

		if (tollTagDetails == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_ACCOUNT_DOESNT_EXIST));
		}

		if (tollTagDetails.getExcCode().equalsIgnoreCase(DBConstants.TollTagExcCodeStatus.CLOSED_OR_REPLACED.value())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TAG_IS_CLOSED_OR_REPLACED));
		}

		if (!tollTagDetails.getExcCode().equalsIgnoreCase(DBConstants.TollTagExcCodeStatus.ACTIVE.value())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.FAILED_TO_DELETE_TAG));
		}

		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();

		if (CollectionUtils.isEmpty(merchants)) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Merchant not present").format());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.MERCHANT_DOESNT_EXIST));

		}

		List<User> customerusers = userDBService
				.getUsersByIds(Arrays.asList(tollTagDetails.getTollRegistration().getUserId()));

		if (CollectionUtils.isEmpty(customerusers)) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("customer user not present")
					.data("User id", tollTagDetails.getTollRegistration().getUserId()).format());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));

		}

		Dipcoin dipcoin = coinDBService.findDipcoin(tollTagDetails.getCustomerAccountId(),
				DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());

		if (dipcoin == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Dipcoin Not Present").format());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.DIPCOIN_DOESNT_EXIST));
		}

		if (!Hibernate.isInitialized(dipcoin.getCustomerAccount())) {
			Hibernate.initialize(dipcoin.getCustomerAccount());
		}

		BigDecimal initialAmount = dipcoin.getAmount();
		BigDecimal minimumAmount = tollTagDetails.getAvailableAmount();

		tollTagDetails.setStatus(String.valueOf(TollTagApprovalStatus.INACTIVE.value()));
		tollTagDetails.setExcCode(TollConstant.EXC_CODE_CLOSED_OR_REPLACED);

		// Updating the Toll Tag
		TollTag updatetollTag = this.tollDBService.updateTollTag(tollTagDetails);

		if (updatetollTag == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_DOESNT_EXIST));
		}

		Bank bank = bankDBService.asyncGetBank(updatetollTag.getBankId()).get();

		if (bank == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank not present")
					.data("Bank Id", updatetollTag.getBankId()).format());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}
		List<DipcoinTransaction> dipcoinTransactions = new ArrayList<>();
		if (updatetollTag.getRegistrationAmount().compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO) {

			// Check the Fee AND Deposit Osta is inn Processed state or not.
			dipcoinTransactions = coinDBService.findByPartnerReferenceIdAndOrderId(bank.getReferenceId(),
					updatetollTag.getRegistrationNo(), DBConstants.TransactionSource.WEB.value(), null, null);

			if (CollectionUtils.isEmpty(dipcoinTransactions)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("No Fee Deposit dTxns found for the Tag.")
						.data("Vehicle registration no :", updatetollTag.getRegistrationNo()).data("TollTagId", decTtid)
						.format());
			}
		}

		// Check the Process Osta is inn Processed state or not.
		List<DipcoinTransaction> dTxs = coinDBService.findByPartnerReferenceIdAndOrderId(
				merchants.get(0).getReferenceId(), updatetollTag.getTagId(), DBConstants.TransactionSource.TOLL.value(),
				null, null);

		for (DipcoinTransaction dTx : dTxs) {
			dipcoinTransactions.add(dTx);
		}

		if (CollectionUtils.isNotEmpty(dipcoinTransactions)) {
			boolean isSettlementNotDone = false;
			for (DipcoinTransaction dipcoinTransaction : dipcoinTransactions) {
				// Checking that of any of the DTxns are in Settlement not done state.
				// If i found anyone also i terminate and would not allow to release osta.

				if (dipcoinTransaction.getType() == DipcoinTransactionType.DEEMED_ACCEPTED_PROCESSED.value()
						|| dipcoinTransaction.getType() == DipcoinTransactionType.NON_FIN.value()) {
					continue;
				}

				else if (dipcoinTransaction.getSettlementDone() != DipcoinTransactionSettlementDone.SETTLEMENT_SUCCESS
						.value()) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("There is dTxn with the Tag with Settlement NOT DONE state.")
							.data("dTxn OrderId:", dipcoinTransaction.getOrderId())
							.data("SettlementDone flag", dipcoinTransaction.getSettlementDone()).format());
					isSettlementNotDone = true;
					break;

				}
			}

			if (isSettlementNotDone) {
				throw new APIException(HttpStatus.BAD_REQUEST,
						APIResponse.error(HeaderCode.TOLLTAG_SETTLEMENT_PENDING));
			}
		}

		List<TollTag> tollTags = tollDBService
				.findTollTagsByCustomerAccountId(Arrays.asList(updatetollTag.getCustomerAccountId()));

		boolean tollTagEnabled = false;
		for (TollTag tolltag : tollTags) {
			if (updatetollTag.getId() != tolltag.getId()
					&& (DBConstants.TollTagApprovalStatus.ACTIVE.value() == Integer.valueOf(tolltag.getStatus())
							|| DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer
									.valueOf(tolltag.getStatus())
							|| DBConstants.TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value() == Integer
									.valueOf(tolltag.getStatus()))) {
				tollTagEnabled = true;

			}
		}

		// Adding Request To release Osta.
		ResponseEntity customerDipcoinResponse = customerDipcoinResource.deleteDipcoin(customerusers.get(0),
				customerusers.get(0).getPhone().concat(dipcoin.getCoin()), false);

		if (HttpStatus.BAD_REQUEST.value() <= customerDipcoinResponse.getStatusCodeValue()) {
			throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		if (tollTagEnabled
				&& initialAmount.subtract(minimumAmount).compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO) {

			CustomerDipcoinRequest createReq = new CustomerDipcoinRequest();

			// Adding Request To create Osta.
			if (updatetollTag.getTollRegistration().getUserId() != updatetollTag.getTollRegistration().getCreatedBy()
					&& StringUtils.isNotBlank(updatetollTag.getTopUpDetails())) {

				try {
					TopUpDetails topUpDetails = objectMapper.readValue(updatetollTag.getTopUpDetails(),
							TopUpDetails.class);
					if (updatetollTag.getAvailableAmount().add(topUpDetails.getAutoTopUpAmount())
							.compareTo(initialAmount) < NumberUtils.INTEGER_ZERO) {
						createReq.setAmount(initialAmount
								.subtract(updatetollTag.getAvailableAmount().add(topUpDetails.getAutoTopUpAmount())));
					} else {
						createReq.setAmount(BigDecimal.ZERO);
					}

				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("exception caught so so setting lien mark amount to zero").format(), e);
					createReq.setAmount(BigDecimal.ZERO);
				}

			} else {
				createReq.setAmount(initialAmount.subtract(updatetollTag.getAvailableAmount()));
			}
			createReq.setCardId(dipcoin.getCustomerAccount().getUserCardId());
			createReq.setCurrency(TollConstant.INR_CURRENCY);
			createReq.setAuthorizationPin(APIConstants.PIN);
			createReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
			createReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);
			createReq.setEncryptDipcoin(false);

			ResponseEntity createDcoinResponse = customerDipcoinResource.createDipcoin(customerusers.get(0), createReq,
					false);

		}

		// fetch dipcoin whose usageType = 8 ie deposit only then call this method
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching Deposit dipcoin").format());
		List<Dipcoin> depositDipcoins = coinDBService.asyncFindDipcoin(tollTagDetails.getCustomerAccountId(),
				Arrays.asList(DBConstants.DipcoinUsageType.DEPOSIT.value())).get();

		if (CollectionUtils.isNotEmpty(depositDipcoins)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Deposit dipcoin found.. calling revertDeposit method").format());
			revertDeposit(bank, updatetollTag);
		}

		// Populating the TollTag Id and Status
		response.setTagDetailsId(updatetollTag.getId());
		response.setStatus(updatetollTag.getStatus());

		try {

			if (tollProperties.isConnectNpci()) {
				try {
					brontooResource.updateExceptionList(updatetollTag, TollConstant.ADD_OP, null);
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("update exccode exception")
							.format());
				}
			}

			// success sms
			if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(
					tollTagDetails.getTollRegistration().getMobileNo(),
					Templates.TollTagDeletion.format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
					true)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
						.data("phone", tollTagDetails.getTollRegistration().getMobileNo()).format());
			}

			if (applicationProperties.getAwsSMSClient()) {

				NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
				notificationRequestContext.setTraceId(httpServletContext.getTraceId());
				if (!notificationResource.sendSms(tollTagDetails.getTollRegistration().getMobileNo(),
						Templates.TollTagDeletion.format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
						httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

					LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", tollTagDetails.getTollRegistration().getMobileNo()).format());

				}
			}
			// Email
			if (!this.tollEmailUtils.tollTagDeletion(updatetollTag.getTollRegistration().getMobileNo(), bank,
					updatetollTag.getTollRegistration())) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to send email to customer after activation.")
						.data("user email", updatetollTag.getTollRegistration().getEmailId()).format());
			}

		} catch (Exception e) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Exception in sending email & sms to customer after activation.").format());
		}

		response.addHeaderCode(HeaderCode.TOLL_TAG_INACTIVE);
		return ResponseEntity.status(HttpStatus.OK).body(response);

	}
	
	public boolean revertDeposit(Bank bank, TollTag tollTag) throws Exception {
		List<DipcoinTransaction> dTxs = coinDBService.asyncFindByPartnerReferenceIdAndOrderId(bank.getReferenceId(),
				tollTag.getRegistrationNo(), null, null, null).get();
		BigDecimal cancelAmount = BigDecimal.ZERO;
		if (CollectionUtils.isNotEmpty(dTxs)) {

			// run for one time only
			for (DipcoinTransaction dtx : dTxs) {

				Dipcoin dCoin = coinDBService.asyncGetById(dtx.getDipcoinId(), false).get();

				if (dtx.getType() != DBConstants.DipcoinTransactionType.CANCELLED_BY_MERCHANT.value()
						&& (dCoin.getUsageType() == DBConstants.DipcoinUsageType.DEPOSIT.value())
						&& dCoin.getUsageCategory().equalsIgnoreCase(tollTag.getRegistrationNo())) {

					Dipcoin dipcoin = coinDBService.asyncGetById(dtx.getDipcoinId(), false).get();

					DipcoinTransaction dTx = new DipcoinTransaction();
					dTx.setType(DBConstants.DipcoinTransactionType.CANCELLED_BY_MERCHANT.value());
					dTx.setDipcoinTransactionRefId(CoreUtils.generateDipcoinToMerchantReferenceNumber());
					dTx.setUpdateDate(String.valueOf(DateTime.now().getMillis()));
					dTx.setRequestTime(String.valueOf(DateTime.now().getMillis()));
					dTx.setResponseTime(String.valueOf(DateTime.now().getMillis()));
					dTx.setAmount(dtx.getAmount());
					dTx.setDipcoinId(dtx.getDipcoinId());
					dTx.setComments(dtx.getComments());
					dTx.setCustomerAccountId(dtx.getCustomerAccountId());
					dTx.setDCResponseCode(dtx.getDCResponseCode());
					dTx.setDCResponseDesc(dtx.getDCResponseDesc());
					dTx.setGeoLocation(dtx.getGeoLocation());
					dTx.setIPAddress(dtx.getIPAddress());
					dTx.setOrderId(dtx.getOrderId());
					dTx.setUser(dtx.getUser());
					dTx.setPartnerReferenceId(dtx.getPartnerReferenceId());
					dTx.setPartnerTransactionReferenceId(Utils.constructPartnerReferenceId(bank));
					dTx.setSettlementDone(DipcoinTransactionSettlementDone.DEFAULT.value());
					dTx.setSource(dtx.getSource());
					dTx.setStatus(dtx.getStatus());
					coinDBService.addTransaction(dTx);

					dipcoin.setStatus(DipcoinStatus.PROCESSED_DISPUTED.value());
					if (coinDBService.updateCoin(dipcoin) == null) {
						LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Fail to Update Dipcoin.").format());
					}
					break;
				}
			}

		}
		return true;
	}

	/*
	 * Delete the Toll Osta
	 */
	public ResponseEntity deleteTollOsta(final User user, final String cardId) throws Exception, APIException {

		List<TollTagResponse> responselist = new ArrayList<TollTagResponse>();
		TollTagResponse response = new TollTagResponse();

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Deleting TollOsta Account")
				.data("UserId", user.getId()).data("CardId", cardId).format());

		CustomerAccount customerAccount = this.customerDBService.getAccount(user.getId(), Integer.parseInt(cardId));

		// Card Id is not found
		if (customerAccount == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
		}

		// Check the UserId and DipcoinCreated User Id is Same
		if (user.getId() != customerAccount.getUser().getId()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();

		if (CollectionUtils.isEmpty(merchants)) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Merchant not present").format());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.MERCHANT_DOESNT_EXIST));

		}

		Dipcoin dipcoin = coinDBService.findDipcoin(customerAccount.getId(), DBConstants.DipcoinStatus.ACTIVE.value(),
				DBConstants.DipcoinUsageType.TOLL.value());

		if (dipcoin == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Dipcoin Not Present").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.DIPCOIN_DOESNT_EXIST));
		}

		if (!Hibernate.isInitialized(dipcoin.getCustomerAccount())) {
			Hibernate.initialize(dipcoin.getCustomerAccount());
		}

		if (Hibernate.isInitialized(customerAccount.getBank())) {
			Hibernate.initialize(customerAccount.getBank());
		}

		Bank bank = customerAccount.getBank();

		if (bank == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank not present").format());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		List<DipcoinTransaction> dipcoinTransactions = coinDBService
				.asyncGetTransactionsByCustomerAccountIds(Arrays.asList(customerAccount.getId()),
						Arrays.asList(DBConstants.DipcoinTransactionType.COMPLETELY_USED.value(),
								DBConstants.DipcoinTransactionType.PARTIALLY_USED.value()),
						null, null, null, null)
				.get();

		Iterator<DipcoinTransaction> itr = dipcoinTransactions.iterator();
		boolean isSettlementNotDone = false;
		while (itr.hasNext()) {
			DipcoinTransaction dipcoinTransaction = itr.next();
			if ((dipcoinTransaction.getPartnerReferenceId().equalsIgnoreCase(bank.getReferenceId())
					|| dipcoinTransaction.getPartnerReferenceId().equalsIgnoreCase(merchants.get(0).getReferenceId()))
					&& dipcoinTransaction.getStatus() == DBConstants.DipcoinTransactionsStatus.SUCCESS.value()
					&& dipcoinTransaction.getSettlementDone() != DipcoinTransactionSettlementDone.SETTLEMENT_SUCCESS
							.value()) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("There is dTxn with the Tag with Settlement NOT DONE state.")
						.data("dTxn OrderId:", dipcoinTransaction.getOrderId())
						.data("SettlementDone flag", dipcoinTransaction.getSettlementDone()).format());
				isSettlementNotDone = true;
				break;
			}
		}

		if (isSettlementNotDone) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLLTAG_SETTLEMENT_PENDING));
		}

		// Adding Request To release Osta.
		ResponseEntity customerDipcoinResponse = customerDipcoinResource.deleteDipcoin(user,
				user.getPhone().concat(dipcoin.getCoin()), false);

		if (HttpStatus.BAD_REQUEST.value() <= customerDipcoinResponse.getStatusCodeValue()) {
			return customerDipcoinResponse;
		}

		List<TollTag> tollTags = tollDBService.findTollTagsByCustomerAccountId(Arrays.asList(customerAccount.getId()));

		Iterator<TollTag> tag = tollTags.iterator();

		while (tag.hasNext()) {
			TollTag tolltag = tag.next();

			tolltag.setStatus(String.valueOf(DBConstants.TollTagApprovalStatus.INACTIVE.value()));
			tolltag.setExcCode(TollConstant.EXC_CODE_LOWBALANCE_LIST);
			TollTag tollTag = tollDBService.updateTollTag(tolltag);
			if (tollTag == null) {
				throw new APIException(HttpStatus.BAD_REQUEST,
						APIResponse.error(HeaderCode.TOLL_USER_DETAILS_CANNOT_UPDATE));
			}
			if (tollProperties.isConnectNpci()) {
				try {
					ResponseEntity responseEntity = brontooResource.updateExceptionList(tollTag, TollConstant.ADD_OP,
							null);
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("update exccode exception")
							.format());
				}

			}

			revertDeposit(bank, tolltag);

			// Populating the TollTag Id and Status
			response.setRegistrationNo(tolltag.getRegistrationNo());
			response.setStatus(tolltag.getStatus());
			responselist.add(response);
		}

		for (TollTag tolltag : tollTags) {

			try {
				// success sms
				if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(
						tolltag.getTollRegistration().getMobileNo(),
						Templates.TollTagDeletion.format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
						true)) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", tolltag.getTollRegistration().getMobileNo()).format());
				}

				if (applicationProperties.getAwsSMSClient()) {

					NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
					notificationRequestContext.setTraceId(httpServletContext.getTraceId());
					if (!notificationResource.sendSms(tolltag.getTollRegistration().getMobileNo(),
							Templates.TollTagDeletion
									.format(bank.getAlias() == null ? bank.getName() : bank.getAlias()),
							httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

						LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
								.data("phone", tolltag.getTollRegistration().getMobileNo()).format());

					}
				}

				// Email
				if (!this.tollEmailUtils.tollTagDeletion(tolltag.getTollRegistration().getMobileNo(), bank,
						tolltag.getTollRegistration())) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to send email to customer after activation.")
							.data("user email", tolltag.getTollRegistration().getEmailId()).format());
				}

			} catch (Exception e) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Exception in sending email & sms to customer after activation.").format());
			}
		}

		response.addHeaderCode(HeaderCode.TOLL_TAG_INACTIVE);
		return ResponseEntity.status(HttpStatus.OK).body(responselist);

	}

	public ResponseEntity getIhmclBank(String bankShortCode) throws Exception {

		if (StringUtils.isNotBlank(bankShortCode)) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Fetching ihmcl bank encrypted data").format());
			return getEncrypted(bankShortCode);
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching ihmcl bank list").format());

		List<HashMap<String, String>> banklist = new ArrayList<>();

		String[] banks = tollProperties.getBankInfo().split(",");

		for (String bank : banks) {
			HashMap<String, String> bankmap = new HashMap<>();
			String[] bankInfo = bank.split("~");
			bankmap.put("name", bankInfo[0].trim());
			bankmap.put("shortCode", bankInfo[1].trim());
			banklist.add(bankmap);
		}

		return ResponseEntity.status(HttpStatus.OK).body(banklist);
	}
	
	public ResponseEntity getEncrypted(String bankShortCode) throws Exception {

		HashMap<String, String> response = new HashMap<String, String>();
		String date = StringUtils.EMPTY;
		String bankData = StringUtils.EMPTY;
		SimpleDateFormat monthlyPassFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();

		// fetching npci server time based on bank short code(orgId) for environment
		// uat,prod
		if (tollProperties.isConnectNpci()) {
			String[] bankInfos = tollProperties.getBankInfo().split(",");
			Bank bank = new Bank();
			for (String bankInfo : bankInfos) {
				String[] info = bankInfo.split("~");
				if (info[1].equalsIgnoreCase(bankShortCode)) {
					bank.setOrgId(info[1]);
					bank.setIin(info[2]);
				}
			}
			ResponseEntity responseEntity = this.brontooResource.syncTime(bank);

			if (responseEntity.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Sync Time bad request")
						.format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
			}

			tollNetcSyncTimeResponse = (TollNetcSyncTimeResponse) responseEntity.getBody();
			SimpleDateFormat tsformatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
			Date npciServerDate = tsformatter.parse(tollNetcSyncTimeResponse.getResp().getTs());
			date = monthlyPassFormatter.format(npciServerDate);
			bankData = bankShortCode.toUpperCase() + "~" + date;
		} else {
			date = monthlyPassFormatter.format(new Date());
			bankData = bankShortCode.toUpperCase() + "~" + date;

		}

		PublicKey publickey = tollSignatureVerificationServices.getPublicKey(httpServletContext.getTraceId(),
				"monthlypass");
		if (publickey == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("publickey not found").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.BANK_CERTIFICATE_IS_NOT_FOUND));
		}

		String encryptedData = encrypt(publickey, bankData.getBytes());
		if (encryptedData == null) {
			LOG.debug(
					LogFormatter.instance(httpServletContext.getTraceId()).message("encryptedData not found").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.ENCRYPTION_INVALID));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Monthly Pass Encryption")
				.data("bankData", bankData).data("encrypted data", encryptedData).format());

		response.put("bankData", encryptedData);

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}
	
	public String encrypt(PublicKey key, byte[] inputData) {
		try {
			Cipher rsa;
			//rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			rsa = Cipher.getInstance("RSA");
			rsa.init(Cipher.ENCRYPT_MODE, key);
			byte[] utf8 = rsa.doFinal(inputData);
			return Base64.getEncoder().encodeToString(utf8);
		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Encryption Exception").format(),
					e);
		}
		return null;
	}

	public ResponseEntity getDisputeOptions(final User user, Integer preRequisiteCode, Integer functionCodes,
			boolean fetchAll) throws Exception, APIException {

		if (!this.userDBService.isCustomer(user) && (!this.userDBService.isBankSuperAdmin(user)
				&& !this.userDBService.isBankAdmin(user) && !this.userDBService.isBankInternalUser(user)
				&& !this.userDBService.brontooRepresentative(user) && !this.userDBService.isBankViewer(user))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (functionCodes != null || fetchAll) {
			return getDisputeCodes(functionCodes, fetchAll);
		}

		return getDisputeOptions(preRequisiteCode);

	}
	
	public ResponseEntity getDisputeOptions(Integer preRequisiteCode) {

		List<HashMap<String, Object>> response = new ArrayList<>();

		if (preRequisiteCode == null) {
			preRequisiteCode = TollIssuerFunctionCodes.SETTELED_TXN.getFunctionCode();
		}

		for (TollIssuerFunctionCodes tollIssuerFunctionCode : TollIssuerFunctionCodes.values()) {

			String[] tollIssuerPreRequisiteCodes = tollIssuerFunctionCode.getPreRequisite().split(",");

			for (String tollIssuerPreRequisiteCode : tollIssuerPreRequisiteCodes) {

				if (tollIssuerPreRequisiteCode.equalsIgnoreCase(String.valueOf(preRequisiteCode))) {
					popullateTollIssuerFunctionCodeResponse(tollIssuerFunctionCode, response);
				}
			}

		}

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}
	
	public ResponseEntity getDisputeCodes(Integer functionCodes, boolean fetchAll) {
		List<HashMap<String, Object>> response = new ArrayList<>();

		if (fetchAll) {

			for (TollAquirerFunctionCodes tollAquirerFunctionCode : TollAquirerFunctionCodes.values()) {

				popullateTollAquirerFunctionCodesResponse(tollAquirerFunctionCode, response);
			}
			for (TollIssuerFunctionCodes tollIssuerFunctionCode : TollIssuerFunctionCodes.values()) {

				popullateTollIssuerFunctionCodeResponse(tollIssuerFunctionCode, response);
			}

		} else {

			for (TollIssuerFunctionCodes tollIssuerFunctionCode : TollIssuerFunctionCodes.values()) {
				if (tollIssuerFunctionCode.getFunctionCode() == functionCodes && !fetchAll) {
					popullateTollIssuerFunctionCodeResponse(tollIssuerFunctionCode, response);
				}
			}

			for (TollAquirerFunctionCodes tollAquirerFunctionCode : TollAquirerFunctionCodes.values()) {
				if (tollAquirerFunctionCode.getFunctionCode() == functionCodes && !fetchAll) {
					popullateTollAquirerFunctionCodesResponse(tollAquirerFunctionCode, response);
				}
			}

		}

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}
	
	public List<HashMap<String, Object>> popullateTollAquirerFunctionCodesResponse(
			TollAquirerFunctionCodes tollAquirerFunctionCode, List<HashMap<String, Object>> response) {

		HashMap<String, Object> functionCodesData = new HashMap<>();
		List<HashMap<String, Object>> reasons = new ArrayList<>();
		functionCodesData.put("functionCode", tollAquirerFunctionCode.getFunctionCode());
		functionCodesData.put("disputeName", tollAquirerFunctionCode.getDisputeName());
		functionCodesData.put("tat", tollAquirerFunctionCode.getTat());

		for (TollReasonCodes tollReasonCode : TollReasonCodes.values()) {
			String[] tollReasonfunctionCodes = tollReasonCode.getFunctionCodes().split(",");

			for (String tollReasonfunctionCode : tollReasonfunctionCodes) {

				if (tollReasonfunctionCode
						.equalsIgnoreCase(String.valueOf(tollAquirerFunctionCode.getFunctionCode()))) {

					HashMap<String, Object> reasonCodesData = new HashMap<>();
					reasonCodesData.put("reasonCode", tollReasonCode.getReasonCode());
					reasonCodesData.put("description", tollReasonCode.getDescription());
					reasons.add(reasonCodesData);

				}
			}
		}
		functionCodesData.put("reasons", reasons);
		response.add(functionCodesData);

		return response;
	}
	
	public List<HashMap<String, Object>> popullateTollIssuerFunctionCodeResponse(
			TollIssuerFunctionCodes tollIssuerFunctionCode, List<HashMap<String, Object>> response) {

		HashMap<String, Object> functionCodesData = new HashMap<>();
		List<HashMap<String, Object>> reasons = new ArrayList<>();
		functionCodesData.put("functionCode", tollIssuerFunctionCode.getFunctionCode());
		functionCodesData.put("disputeName", tollIssuerFunctionCode.getDisputeName());
		functionCodesData.put("tat", tollIssuerFunctionCode.getTat());

		for (TollReasonCodes tollReasonCode : TollReasonCodes.values()) {
			String[] tollReasonfunctionCodes = tollReasonCode.getFunctionCodes().split(",");

			for (String tollReasonfunctionCode : tollReasonfunctionCodes) {

				if (tollReasonfunctionCode.equalsIgnoreCase(String.valueOf(tollIssuerFunctionCode.getFunctionCode()))) {

					HashMap<String, Object> reasonCodesData = new HashMap<>();
					reasonCodesData.put("reasonCode", tollReasonCode.getReasonCode());
					reasonCodesData.put("description", tollReasonCode.getDescription());
					reasons.add(reasonCodesData);

				}
			}
		}
		functionCodesData.put("reasons", reasons);
		response.add(functionCodesData);

		return response;
	}

	public ResponseEntity getSummationOfMinimumAmount(final User user, final Integer userCardId)
			throws InterruptedException, ExecutionException, JsonMappingException, JsonProcessingException {

		TollRegReceiptResponse response = new TollRegReceiptResponse();

		if (user == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));
		}

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (userCardId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_USER_CARDID));
		}

		// From the UserCardId , fetch CustomerAccount table details.
		CustomerAccount customerAccount = this.customerDBService.getAccount(user.getId(), userCardId);

		if (customerAccount == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.BANK_ACCOUNT_DOESNT_EXISTS));
		}

		// Fetch the CustomerAccountId and fetch the TollTag details.
		BigDecimal minAmount = this.tollDBService.sumOfMinimumAmount(customerAccount.getId(),
				Arrays.asList(Integer.valueOf(TollTagApprovalStatus.ACTIVE.value()).toString(),
						Integer.valueOf(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()).toString(),
						Integer.valueOf(TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value()).toString()));

		TopUpDetails topUpDetails = new TopUpDetails();

		// If already data is present in CustomerAccount.TopUpDetails
		if (!StringUtils.isEmpty(customerAccount.getTopUpDetails())) {
			topUpDetails = objectMapper.readValue(customerAccount.getTopUpDetails(), TopUpDetails.class);
		}

		response.setAutoTopUp(topUpDetails.getAutoTopUp());
		response.setAutoTopUpAmount(topUpDetails.getAutoTopUpAmount());
		response.setAutoTopUpThresHold(topUpDetails.getAutoTopUpThresHold());
		response.setMinimumAmount(minAmount != null ? minAmount : topUpDetails.getMinimumAmount());

		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	public ResponseEntity addTopUpDetails(final User user, final TollRegistrationRequest addDetails,
			final String serialNumber) throws InterruptedException, ExecutionException, JsonProcessingException {

		if (user == null || StringUtils.isEmpty(serialNumber)) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));
		}

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		// checking for the request null
		if (addDetails == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}
		Epc epc = tollDBService.findEpcBySerialNumber(serialNumber);

		if (epc == null) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_SERIAL_NUMBER_NOT_CORRECT));
		}
		TollTag tag = tollDBService.findTollCustomersByTagId(epc.getRfidTag());

		if (tag == null) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.DESTINATION_TAG_IS_MISSING));
		}
		// Fetch the CustomerAccount with help of Customer AccountId
		CustomerAccount account = this.customerDBService.getAccountById(tag.getCustomerAccountId());

		if (account == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.FAILED_FETCHING_CUSTOMER_ACCOUNT));
		}

		TopUpDetails topUpDetails = new TopUpDetails();

		topUpDetails.setAutoTopUp(addDetails.getAutoTopUp());

		topUpDetails.setAutoTopUpAmount(addDetails.getAutoTopUpAmount());

		topUpDetails.setAutoTopUpThresHold(addDetails.getAutoTopUpThresHold());

		topUpDetails.setMinimumAmount(addDetails.getMinimumAmount());

		tag.setTopUpDetails(objectMapper.writeValueAsString(topUpDetails));

		TollTag updatedTollTag = tollDBService.updateTollTag(tag);

		if (updatedTollTag == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.DATA_UPDATE_FAILED));
		} else {
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.error(HeaderCode.DATA_UPDATE_SUCCESS));
		}
	}

	/*
	 * Get Bank Transactions for Toll
	 */
	public ResponseEntity getTollTransactions(User user, Long startTime, Long endTime, Integer start, Integer count,
			String vehicleNumber) throws Exception {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
			.message("getTollTransactions - Method started")
			.data("UserId", user != null ? user.getId() : null)
			.data("StartTime", startTime)
			.data("EndTime", endTime)
			.data("Start", start)
			.data("Count", count)
			.data("VehicleNumber", vehicleNumber)
			.format());

		if (user == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - User is null")
				.format());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));
		}

		if (!this.userDBService.isActive(user)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - User is not active")
				.data("UserId", user.getId())
				.data("UserStatus", user.getStatus())
				.format());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		List<String> vehicleNumbers = new ArrayList<>();
		if (StringUtils.isNotEmpty(vehicleNumber)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - Using provided vehicle number")
				.data("VehicleNumber", vehicleNumber)
				.format());
			vehicleNumbers.add(vehicleNumber);
		} else {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - Fetching toll registrations for user")
				.data("UserId", user.getId())
				.format());

			List<TollRegistration> tollRegs = tollDBService.findTollCustomersByUserId(user.getId());

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - Toll registrations fetched")
				.data("UserId", user.getId())
				.data("TollRegistrationsCount", tollRegs != null ? tollRegs.size() : 0)
				.format());

			if (CollectionUtils.isEmpty(tollRegs)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("getTollTransactions - No toll registrations found for user")
					.data("UserId", user.getId())
					.format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}

			int totalTollTags = 0;
			for (TollRegistration tollReg : tollRegs) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("getTollTransactions - Processing toll registration")
					.data("TollRegistrationId", tollReg.getId())
					.data("TollTagsCount", tollReg.getTollTag() != null ? tollReg.getTollTag().size() : 0)
					.format());
				
				if (tollReg.getTollTag() != null) {
					totalTollTags += tollReg.getTollTag().size();
					for (TollTag tollTag : tollReg.getTollTag()) {
						vehicleNumbers.add(tollTag.getRegistrationNo());
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("getTollTransactions - Added vehicle number")
							.data("TollTagId", tollTag.getId())
							.data("RegistrationNo", tollTag.getRegistrationNo())
							.format());
					}
				}
			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - Vehicle numbers extraction completed")
				.data("TotalTollRegistrations", tollRegs.size())
				.data("TotalTollTags", totalTollTags)
				.data("VehicleNumbersCount", vehicleNumbers.size())
				.format());
		}

		if (CollectionUtils.isEmpty(vehicleNumbers)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - No vehicle numbers found")
				.data("UserId", user.getId())
				.format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
			.message("getTollTransactions - Fetching toll tags by registration numbers")
			.data("VehicleNumbers", vehicleNumbers)
			.format());

		List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNo(vehicleNumbers);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
			.message("getTollTransactions - Toll tags fetched")
			.data("VehicleNumbersCount", vehicleNumbers.size())
			.data("TollTagsCount", tollTags != null ? tollTags.size() : 0)
			.format());

		if (CollectionUtils.isEmpty(tollTags)) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - No toll tags found for vehicle numbers")
				.data("UserId", user.getId())
				.data("VehicleNumbers", vehicleNumbers)
				.format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
		}

		Bank bank = bankDBService.getBank(tollTags.get(tollTags.size() - 1).getBankId());

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
			.message("getTollTransactions - Bank fetched")
			.data("BankId", tollTags.get(tollTags.size() - 1).getBankId())
			.data("BankExists", bank != null)
			.format());

		if (bank == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("getTollTransactions - Bank not found")
				.data("BankId", tollTags.get(tollTags.size() - 1).getBankId())
				.format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
			.message("getTollTransactions - Calling tollServiceBankResource.getTollTransactions")
			.data("UserId", user.getId())
			.data("BankId", bank.getId())
			.data("VehicleNumbersCount", vehicleNumbers.size())
			.format());

		return tollServiceBankResource.getTollTransactions(user, bank, startTime, endTime, start, count, null,
				vehicleNumbers, null);
	}

	public ResponseEntity updateRegistrationNumberAndAddImages(User user, final String vinNumber,
			final String serialNumber, final String registrationNumber, final MultipartFile rcImg,
			final MultipartFile vehicleImg) throws Exception, APIException {

		ResponseEntity responseEntity = null;

		if (user == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));
		}

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (null == vehicleImg || rcImg == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_IMAGE_MISSING));
		}

		List<TollTag> tollTags = tollDBService.findVinNumberAndSerialNumberAndStatusAndVinVrnFlag(vinNumber,
				serialNumber, String.valueOf(DBConstants.TollTagApprovalStatus.ACTIVE.value()),
				DBConstants.VinVrn.VIN_CHACIS.value());
		if (tollTags.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.WRONG_VIN_NUMBER_AND_SERIAL_NUMBER_INPUT));
		}

		TollTag tollTag = tollTags.get(0);

		if (!StringUtils.isEmpty(tollTag.getUpdatedDocs())
				&& tollTag.getVinDocVerify() == DBConstants.VinDocVerify.DEFAULT.value()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.DOCUMENTS_PENDING_FROM_BANK));
		} else if (!StringUtils.isEmpty(tollTag.getUpdatedDocs())
				&& tollTag.getVinDocVerify() == DBConstants.VinDocVerify.REJECTED.value()) {
			tollTag.setVinDocVerify(DBConstants.VinDocVerify.DEFAULT.value());
		} else if (!StringUtils.isEmpty(tollTag.getUpdatedDocs())
				&& tollTag.getVinDocVerify() == DBConstants.VinDocVerify.VERIFIED.value()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.ALREADY_VERIFIED_THE_DETAILS));
		}

		tollTag.setVinToVrn(registrationNumber);

		// Save vehicleImg to tollTag
		String vehicleImgDestinationPath = saveImageToTollTag(tollTag, vehicleImg);
		tollTag.setUpdatedDocs(vehicleImgDestinationPath);

		// Save rcImg to tollTag
		String rcImgDestinationPath = saveImageToTollTag(tollTag, rcImg);
		tollTag.setUpdatedDocs(rcImgDestinationPath);

		tollTag.setUpdatedDocs(vehicleImgDestinationPath + "," + rcImgDestinationPath);

		tollDBService.updateTollTag(tollTag);

		return ResponseEntity.status(HttpStatus.OK).body(APIResponse.error(HeaderCode.SUCCESS));
	}
	
	private String saveImageToTollTag(TollTag tollTag, MultipartFile image) throws APIException, Exception {
		String destinationPath = null;

		String docType = image.getContentType().split("/")[1];
		String folderName = "tollTagId_" + tollTag.getId();
		String fileName = image.getOriginalFilename();

		InputStream fileInputStream = image.getInputStream();

		// Check active profile whether to upload to /tmp or aws s3
		if (applicationProperties.uploadFileToS3()) {
//            // Setting upload location path in AWS S3 based on merchant type
			String uploadLocation = APIConstants.CUSTOMER_TOLL_PATH + folderName + "/" + fileName;
			destinationPath = uploadLocation;

			// getting file content length
			long contentLength = image.getSize();

			if (!this.objectDataStore.put(destinationPath, fileInputStream, contentLength)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to upload file to S3 bucket.").format());
				destinationPath = null;
			}
		} else {
			destinationPath = APIConstants.SERVER_UPLOAD_LOCATION_FOLDER + "tollcustomer_userId_" + tollTag.getId()
					+ "_" + folderName + "/" + fileName;
			if (!objectDataStore.put(destinationPath, fileInputStream, 0)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to store at temp")
						.format());
				destinationPath = null;
			}
		}

		return destinationPath;
	}

	public ResponseEntity walletRecharge(User user, final TollRegistrationRequest createReq, int type)
			throws APIException, Exception {

		CustomerAccount customerAccount = customerDBService.getAccount(user.getId(), createReq.getCardId());

		if (customerAccount == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.CUSTOMER_ACCOUNT_DOESNT_EXIST_OR_CUSTOMER_NOT_LINKED_TO_THE_BANK.message())
					.format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
					APIResponse.error(HeaderCode.CUSTOMER_ACCOUNT_DOESNT_EXIST_OR_CUSTOMER_NOT_LINKED_TO_THE_BANK));

		}

		List<FeesAndDeposit> feesAndDeposits = feesAndDepositDBService.findByBankIdAndChargeCategory(
				Arrays.asList(customerAccount.getWalletBankId()),
				new HashSet<String>(Arrays.asList(createReq.getVehicleList().get(0).getCategory())));

		if (CollectionUtils.isEmpty(feesAndDeposits)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));

		}

		if (Hibernate.isInitialized(customerAccount.getBank())) {
			Hibernate.initialize(customerAccount.getBank());
		}

		createReq.setAuthorizationPin(APIConstants.PIN);
		ResponseEntity response = createFeesandDepositOsta(user, null, customerAccount.getBank(), customerAccount,
				feesAndDeposits, createReq, type, httpServletContext.getTraceId());

		if (response.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
			return response;
		}

		List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNoAndBankIdAndStatus(
				createReq.getVehicleList().get(0).getRegistrationNo(), customerAccount.getBank().getId(),
				String.valueOf(TollTagApprovalStatus.PAYMENT_PENDING.value()));
		tollTags.get(0).setStatus(String.valueOf(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()));
		tollDBService.updateTollTag(tollTags.get(0));

		return response;

	}

}
