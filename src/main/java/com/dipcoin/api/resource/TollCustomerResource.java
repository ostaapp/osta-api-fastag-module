package com.dipcoin.api.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
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
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.context.annotation.Lazy;
import org.apache.commons.collections4.CollectionUtils;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.FeesAndDeposit;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TollRecharge;
import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;
import com.dipcoin.partner.toll.commons.TollErrorCodes;
import com.dipcoin.partner.toll.commons.TollHttpsServices;
import com.dipcoin.partner.toll.commons.TollSignatureGenerationServices;
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
				Arrays.asList(DBConstants.DipcoinUsageType.FEE.value(), DBConstants.DipcoinUsageType.DEPOSIT.value()))
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
				Arrays.asList(DBConstants.DipcoinUsageType.FEE.value(), DBConstants.DipcoinUsageType.DEPOSIT.value()))
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
						}

					}

					if (createFeeDipcoin && BigDecimal.ZERO
							.compareTo(feesAndDeposit.getRegistrationAmount()) < NumberUtils.INTEGER_ZERO) {

						CustomerDipcoinRequest createDcoinReq = new CustomerDipcoinRequest();

						createDcoinReq.setAmount(updateReq.getMiscCharges()
								? feesAndDeposit.getRegistrationAmount().add(feesAndDeposit.getMiscellaneousCharges())
								: feesAndDeposit.getRegistrationAmount());
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

}
