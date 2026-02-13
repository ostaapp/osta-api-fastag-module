package com.dipcoin.api.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.io.StringReader;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.ObjectDataStore;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.Detail;
import com.dipcoin.api.model.Head;
import com.dipcoin.api.model.TollNetcDetailsRequest;
import com.dipcoin.api.model.TollNetcDetailsResponse;
import com.dipcoin.api.model.TollNetcSyncTimeResponse;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.model.TollTagFeeAndChargesResponse;
import com.dipcoin.api.model.TollTagResponse;
import com.dipcoin.api.model.Txn;
import com.dipcoin.api.model.Vehicle;
import com.dipcoin.api.model.VehicleDetails;

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
import org.springframework.context.annotation.Lazy;
import org.apache.commons.collections4.CollectionUtils;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.FeesAndDepositDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.commons.DBConstants.TollTagApprovalStatus;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.FeesAndDeposit;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;
import com.dipcoin.partner.toll.commons.TollHttpsServices;
import com.dipcoin.partner.toll.commons.TollSignatureGenerationServices;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;

public class TollCustomerResource {

	private static final Logger LOG = LogManager.getLogger(TollCustomerResource.class);

	private final static ObjectMapper objectMapper = new ObjectMapper();

	protected static boolean encryptCardId = false;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	@Autowired
	private TollDBService tollDBService;
	
	@Autowired
	private FeesAndDepositDBService feesAndDepositDBService;

	@Autowired
	private CustomerDBService customerDBService;

	@Autowired
	private BankDBService bankDBService;
	
	@Autowired
	private ObjectDataStore objectDataStore;
	
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

	public ResponseEntity reqVehicleDetails(String vrn, String vin, String lastFiveDigitsOfEngineNo, Object object,
			String bankReferenceId) {
		// TODO Auto-generated method stub
		return null;
	}

}
