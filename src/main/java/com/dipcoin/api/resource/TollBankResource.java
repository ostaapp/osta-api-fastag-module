package com.dipcoin.api.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIConstants.DateFormatter;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.TollEmailUtils;
import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.CustomerDipcoinRequest;
import com.dipcoin.api.model.Detail;
import com.dipcoin.api.model.Head;
import com.dipcoin.api.model.Pagination;
import com.dipcoin.api.model.PartnerProcessDipcoinRequest;
import com.dipcoin.api.model.PartnerProcessDipcoinRequest.SubPartner;
import com.dipcoin.api.model.ReqVehicleDetailResponse;
import com.dipcoin.api.model.ReqVehicleDetailsRequest;
import com.dipcoin.api.model.RespVehicleDetails;
import com.dipcoin.api.model.Tag;
import com.dipcoin.api.model.TagList;
import com.dipcoin.api.model.TollNetcDetailsResponse;
import com.dipcoin.api.model.TollNetcSyncTimeResponse;
import com.dipcoin.api.model.TollRechargeRequest;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.model.TollReqPayRequest;
import com.dipcoin.api.model.TollTagCountResponse;
import com.dipcoin.api.model.TollTagFeeAndChargesResponse;
import com.dipcoin.api.model.TollTagRequest;
import com.dipcoin.api.model.TollTagResponse;
import com.dipcoin.api.model.TollTagUpdateRequest;
import com.dipcoin.api.model.TollTagUpdateResponse;
import com.dipcoin.api.model.TollTagWihVinDetail;
import com.dipcoin.api.model.TollTagsWihVinDetail;
import com.dipcoin.api.model.TollTransactionsReport;
import com.dipcoin.api.model.TollTransactionsReportResponse;
import com.dipcoin.api.model.TopUpDetails;
import com.dipcoin.api.model.Txn;
import com.dipcoin.api.model.Vehicle;
import com.dipcoin.api.utils.TollMetricRegistry;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.db.services.BankBranchDetailsService;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.FeesAndDepositDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TagNPCIApprovalStatusDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BankTagStatus;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.EpcVisibilityStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.commons.DBConstants.TagDeliveryType;
import com.dipcoin.db.services.commons.DBConstants.TollTagApprovalStatus;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.VinVrn;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankBranchDetails;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.FeesAndDeposit;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TagNPCIApprovalStatus;
import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;
import com.dipcoin.partner.toll.commons.TollErrorCodes;
import com.dipcoin.partner.toll.commons.TollHttpsServices;
import com.dipcoin.partner.toll.commons.TollSignatureGenerationServices;
import com.dipcoin.partner.toll.commons.VehicleCategories;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("tollServiceBankResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class TollBankResource {
	private static final Logger LOG = LogManager.getLogger(TollBankResource.class);
	private final static ObjectMapper objectMapper = new ObjectMapper();
	private static final TimeZone dateTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
	public static final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
	public static final DateFormat format = new SimpleDateFormat("HH:mm:ss");

	public static final DateFormat txnAndReconDateformat = new SimpleDateFormat(
			DateFormatter.DD_MM_YYYY_HH_MM_SS.value());

	protected static boolean encryptCardId = false;

	@Autowired
	private TollHttpsServices tollHttpsServices;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private EmailUtils emailUtils;

	@Autowired
	private BankDBService bankDBService;

	@Autowired
	private SmsClient smsClient;

	@Autowired
	TollSignatureGenerationServices tollSignatureGenerationServices;

	@Autowired
	private TollDBService tollDBService;

	@Autowired
	private MerchantDBService merchantDBService;

	@Autowired
	private DipcoinDBService dipcoinDBService;

	@Autowired
	private PartnerDipcoinResource partnerDipcoinResource;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	@Autowired
	private TollProperties tollProperties;

	@Autowired
	private BankAPIServices bankAPIServices;

	@Autowired
	private CustomerDBService customerDBService;

	@Autowired
	private BrontooResource brontooResource;

	@Autowired
	private TollRechargeResource tollRechargeResource;

	@Autowired
//	@Qualifier("com.dipcoin.metrics.TollMetricRegistry")
	private TollMetricRegistry tollMetricRegistry;

	@Autowired
	private FeesAndDepositDBService feesAndDepositDBService;

	@Autowired
	private RedissonClient redissonclient;

	@Autowired
	private NotificationResource notificationResource;

	@Autowired
	private ApplicationProperties applicationProperties;

	@Autowired
	private BankBranchDetailsService bankBranchDetailsService;

	@Autowired
	private Environment environment;

	@Autowired
	private TagNPCIApprovalStatusDBService tagNPCIApprovalStatusDBService;
	
	@Autowired
	private TollCustomerResource tollServicesResource;
	
	@Autowired
	private CustomerDipcoinResource customerDipcoinResource;
	
	@Autowired
	private TollEmailUtils tollEmailUtils;
			
//	public BrontooResource getBrontooResource() {
//	    return brontooResource;
//	  }
//
//	public BrontooResource getBrontooResource() {
//		return brontooResource;
//	}

	/*
	 * 
	 */
	public ResponseEntity getTagsRejectedPendingCounts(final User user, final String bankReferenceId)

			throws Exception, APIException {

		TollTagCountResponse response = new TollTagCountResponse();
		if (user == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		if ((!this.userDBService.isBankAdmin(user) && !this.userDBService.isBankSuperAdmin(user))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (bankReferenceId.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
		}

		Bank bank = this.bankDBService.getBank(bankReferenceId);

		if (bank == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		Long pendingTagCount = this.tollDBService.countOfTags(bank.getId(),
				Arrays.asList(Integer.valueOf(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()).toString()));
		response.setTagApprovalPendingCounts(pendingTagCount);

		Long rejectedTagCount = this.tollDBService.countOfTags(bank.getId(),
				Arrays.asList(Integer.valueOf(TollTagApprovalStatus.BANK_REJECTED.value()).toString()));
		response.setTagRejectedCounts(rejectedTagCount);

		return ResponseEntity.ok(response);
	}
	
	// Updating Address and Vehicle Number By Bank
	public ResponseEntity updateAddressAndVehicleNoByBank(User user, Bank bank, TollTagRequest updateReq,
			String clientTransactionId) {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", updateReq).format());

		TollTagResponse response = new TollTagResponse();

		if (user == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (!this.userDBService.isBankSuperAdmin(user) && !userDBService.isBankTransactor(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		// checking for the request null
		if (updateReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
		}

		response.setClientTransactionId(clientTransactionId);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Tag By Id")
				.data("Id", updateReq.getId()).format());

		TollTag tollTag = this.tollDBService.findTollTagById(updateReq.getId());
		// update vehicle no and address in TollTagTable
		if (!StringUtils.isEmpty(updateReq.getRegistrationNo())) {

			if (tollTag == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TOLL_TAG_DOESNT_EXIST));

			}
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Tag By Id")
					.data("Toll Tag", tollTag).format());

			tollTag.setRegistrationNo(updateReq.getRegistrationNo());

			tollTag = this.tollDBService.updateTollTag(tollTag);

			if (tollTag == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.FAILED_TO_UPDATE_TOLLTAG_DETAILS));
			}
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Tag By Id")
					.data("Updated Toll Tag", tollTag).format());

		}
		if (!StringUtils.isEmpty(updateReq.getAddress())) {
			TollRegistration tollRegistration = tollTag.getTollRegistration();
			if (tollRegistration == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.FAILED_TO_FETCH_TOLL_REGISTRATION));
			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Address By Id")
					.data("Toll Registration", tollRegistration).format());

			tollRegistration.setAddress(updateReq.getAddress());

			tollRegistration = this.tollDBService.update(tollRegistration);
			if (tollRegistration == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.FAILED_TO_UPDATE_TOLL_REGISTRATION));
			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Address By Id")
					.data("Updated Toll Registration", tollRegistration).format());
		}

		return ResponseEntity.ok(response);
	}

	/*
	 * Getting tollCustomer Details of basis of BankReferenceID.
	 */
	public ResponseEntity getTollCustomerDetails(final User user, final String bankReferenceId, final String status,
			Long startTime, Long endTime, Integer start, Integer count, String vehicleNumber, String accountNumber,
			String serialNumber, String branchCode, String phone) throws Exception, APIException {

		TollRegistrationResponse response = new TollRegistrationResponse();

		if (!this.userDBService.bankRepresentative(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}
		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (bankReferenceId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_BANK_REFERENCE));
		}

		// Get the BankId with the BankReferenceID
		Bank bank = bankDBService.getBank(bankReferenceId);
		if (bank == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}
		Integer bankId = bank.getId();

		if (phone != null || branchCode != null) {
			return filterTollTags(user, bank, bankId, phone, branchCode, status, startTime, endTime, start, count);
		}

		// Getting All the list based on just BankId where status is not required
		if (bankId != null && startTime != null) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Get All Toll Customer list based on just BankId").data("bankId", bankId)
					.data("Status", status).format());

			// Getting the all Register user
			List<TollTag> tollTags;
			if (vehicleNumber != null) {
				tollTags = this.tollDBService.findTollCustomersByBankId(bankId, status, start, count, startTime,
						endTime, vehicleNumber);
			} else if (accountNumber != null) {
				tollTags = this.tollDBService.findTollCustomersByBankIdAndAccountNumber(bankId, status, start, count,
						startTime, endTime, accountNumber);
				if (CollectionUtils.isEmpty(tollTags)) {
					// Look up account number in customerAccount table
					List<CustomerAccount> customerAccounts = this.customerDBService
							.findCustomerAccountByBankUID(accountNumber);
					if (!CollectionUtils.isEmpty(customerAccounts)) {
						List<Integer> customerAccountIds = new ArrayList<>();
						for (CustomerAccount customerAccount : customerAccounts) {
							customerAccountIds.add(customerAccount.getId());
						}
						tollTags = this.tollDBService.findTollCustomersByCustomerAccountId(bankId, status, start, count,
								startTime, endTime, customerAccountIds);
					}
				}
			} else {
				tollTags = this.tollDBService.findTollCustomersByBankIdAndSerialNumber(bankId, status, start, count,
						startTime, endTime, serialNumber);
				if (CollectionUtils.isEmpty(tollTags)) {
					// Look up account number in customerAccount table
					Epc epc = this.tollDBService.getBySerialNumber(serialNumber);
					if (epc != null) {
						String epcRfIdTag = epc.getRfidTag();
						tollTags = this.tollDBService.getEpcByTagId(bankId, status, start, count, startTime, endTime,
								epcRfIdTag);
					}
				}
			}

			if (!CollectionUtils.isEmpty(tollTags)) {

				List<APIResponse> listresponses = new LinkedList<APIResponse>();
				for (TollTag tollTag : tollTags) {

					TollRegistrationResponse responseObj = new TollRegistrationResponse();

					populateTollCustomersResponse(user, tollTag.getTollRegistration(), Arrays.asList(tollTag),
							responseObj);

					// Add Bank Name
					responseObj.setBankName(bank.getName());

					listresponses.add(responseObj);
				}
				// set pagination info
				Pagination pagination = new Pagination();
				pagination.setStartRange(startTime);
				pagination.setEndRange(endTime);
				pagination.setScanCompleted(listresponses.size() < count);
				pagination.setTotal(listresponses.size());
				if (!pagination.getScanCompleted()) {
					pagination.setStart(start + count);
				}

				Map<String, Object> tagResponses = new HashMap<>();

				tagResponses.put("tags", listresponses);
				tagResponses.put("pagination", pagination);

				return ResponseEntity.status(HttpStatus.OK).body(tagResponses);

			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_VEHICLE_REG_NO_NOT_PRESENT));

		}

		// Getting the List based on the BankId and Status : For Bank Login
		if (bankId != null && status != null && startTime == null) {
			// Bank would not be seeing the list apart from it status
			if (TollTagApprovalStatus.BANK_APPROVAL_PENDING.equals(status)) {
				response.addHeaderCode(HeaderCode.BAD_REQUEST);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Get All Toll Customer list based on BankId and Status").data("bankId", bankId)
					.data("Status", status).format());

			List<TollTag> listTollTagByStatus = this.tollDBService.findTollTagByBankIdAndStatus(bankId, status);

			if (!CollectionUtils.isEmpty(listTollTagByStatus)) {
				List<APIResponse> listresponses = new LinkedList<APIResponse>();
				for (TollTag tag : listTollTagByStatus) {

					TollTagResponse responseObj = new TollTagResponse();

					Dipcoin dipcoin = dipcoinDBService.findDipcoin(tag.getCustomerAccountId(),
							DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());

					List<User> makerUser = userDBService
							.getUsersByIds(Arrays.asList(tag.getTollRegistration().getCreatedBy()));
					List<User> checkerUser = userDBService.getUsersByIds(Arrays.asList(tag.getApprovedBy()));
					User maker = null;
					User checker = null;
					if (CollectionUtils.isNotEmpty(makerUser)) {
						maker = makerUser.get(NumberUtils.INTEGER_ZERO);
					}

					if (CollectionUtils.isNotEmpty(checkerUser)) {
						checker = checkerUser.get(NumberUtils.INTEGER_ZERO);
					}

					Bank issuerbank = bankDBService.getBank(
							tag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? tag.getWalletBankId() : tag.getBankId());

					populateTollTagDetailsResponse(user, tag, responseObj, dipcoin, maker, checker, issuerbank);

					listresponses.add(responseObj);
				}
				return ResponseEntity.status(HttpStatus.OK).body(listresponses);

			}
			response.addHeaderCode(HeaderCode.TOLL_ACCOUNT_DOESNT_EXIST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_INVALID));

	}

	/**
	 * Get the Toll Tag Charges Specific to the Tag.
	 * 
	 * @param user
	 * @param encTtid
	 * @param miscCharges
	 * @return
	 * @throws Exception
	 * @throws APIException
	 */
	public ResponseEntity getTollTagCharges(final User user, final String encTtid) throws Exception, APIException {

		String originIp = httpServletContext.getOriginIp();

		TollTagFeeAndChargesResponse responseCharges = new TollTagFeeAndChargesResponse();

		if (user != null && encTtid != null) {

			if (!(this.userDBService.bankRepresentative(user) || this.userDBService.brontooRepresentative(user))) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
			}

			String decTtid = encTtid;
			if (StringUtils.isBlank(decTtid)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.INVALID_ENCRYPTED_DATA));
			}
			Integer ttid = Integer.parseInt(decTtid);

			// Get the Toll Tag By Id
			TollTag tollTag = this.tollDBService.findTollTagById(ttid);

			if (tollTag == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TOLL_ACCOUNT_DOESNT_EXIST));
			}

			List<FeesAndDeposit> feesAndDeposit = feesAndDepositDBService
					.findByBankIdAndChargeCategory(
							Arrays.asList(
									tollTag.getWalletBankId() != NumberUtils.INTEGER_ZERO ? tollTag.getWalletBankId()
											: tollTag.getBankId()),
							new HashSet<>(Arrays.asList(tollTag.getCategory())));
			if (CollectionUtils.isEmpty(feesAndDeposit)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("feesAndDeposit empty")
						.data("Bank Id", tollTag.getBankId()).data("Category", tollTag.getCategory()).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));
			}

			Boolean miscCharges = DBConstants.TollTagMiscCharges.TRUE.value()
					.equals(Integer.toString(tollTag.getMiscCharges()));

			BigDecimal miscGst = miscCharges
					? getGstAmountCalculation(feesAndDeposit.get(NumberUtils.INTEGER_ZERO).getMiscellaneousCharges())
					: BigDecimal.ZERO;
			BigDecimal courierCharges = miscCharges
					? feesAndDeposit.get(NumberUtils.INTEGER_ZERO).getMiscellaneousCharges().subtract(miscGst)
					: BigDecimal.ZERO;

			// Set the Charges and Gst Amount.

			responseCharges.setBankId(tollTag.getBankId()).setChargeCategory(tollTag.getCategory())
					.setDepositAmount(tollTag.getDepositAmount()).setMinimumAmount(tollTag.getMinimumAmount())
					.setGstAmount(getGstAmountCalculation(tollTag.getRegistrationAmount()).add(miscGst))
					.setCourierCharges(courierCharges).setRegistrationAmount(tollTag.getRegistrationAmount()
							.subtract(getGstAmountCalculation(tollTag.getRegistrationAmount())));

			return ResponseEntity.status(HttpStatus.OK).body(responseCharges);
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));

	}

	/**
	 * Get the gstAmount Calculation for the Amount
	 * 
	 * @param amount
	 * @return
	 */
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
	 * Updating Toll Customer by Bank.
	 */
	public ResponseEntity updateTollCustomerByVendor(final User user, final Bank bank, final TollTagRequest updateReq,
			final String clientTransactionId) throws Exception, APIException {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", updateReq).format());

		String originIp = httpServletContext.getOriginIp();

		TollTagResponse response = new TollTagResponse();
		if (!(UserRoles.bankUserRoles().contains(user.getRole())
				|| UserRoles.merchantUserRoles().contains(user.getRole()))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}
		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		// checking for the request null
		if (updateReq == null) {
			response.addHeaderCode(HeaderCode.BAD_REQUEST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		response.setClientTransactionId(clientTransactionId);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Tag By Id")
				.data("Id", updateReq.getId()).format());

		RKeys rkeys = redissonclient.getKeys();

		Iterable<String> profileRkeys = rkeys.getKeysByPattern("userProfileTagLock" + updateReq.getId());

		if (IterableUtils.size(profileRkeys) > NumberUtils.INTEGER_ZERO) {
			Iterator<String> rkey = profileRkeys.iterator();
			while (rkey.hasNext()) {

				String key = rkey.next();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("key pattern in redis")
						.data("key", key).format());
				if (key.equalsIgnoreCase("userProfileTagLock" + updateReq.getId())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("key pattern already present in redis").format());

					response.addHeaderCode(HeaderCode.TOLL_TAG_PROFILE_ALREADY_SELECTED_FOR_APPROVAL);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}

		}

		Iterable<String> serialNumberRkeys = rkeys.getKeysByPattern(updateReq.getSerialNumber());

		if (IterableUtils.size(serialNumberRkeys) > NumberUtils.INTEGER_ZERO) {
			Iterator<String> rkey = serialNumberRkeys.iterator();
			while (rkey.hasNext()) {

				String key = rkey.next();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("serial number key pattern in redis").data("key", key).format());
				if (key.equalsIgnoreCase(updateReq.getSerialNumber())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("serial number key pattern already present in redis").format());

					response.addHeaderCode(HeaderCode.TOLL_TAG_SERIAL_NUMBER_ALREADY_SELECTED_FOR_APPROVAL);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}

		}

		RLock userProfileLock = redissonclient.getFairLock(String.valueOf("userProfileTagLock" + updateReq.getId()));
		if (userProfileLock.isLocked()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("key pattern already present in redis").format());

			response.addHeaderCode(HeaderCode.TOLL_TAG_PROFILE_ALREADY_SELECTED_FOR_APPROVAL);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		userProfileLock.lock(60, TimeUnit.SECONDS);
		RLock serialNumberLock = redissonclient.getFairLock(String.valueOf(updateReq.getSerialNumber()));

		if (serialNumberLock.isLocked()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("serial number key pattern already present in redis").format());

			response.addHeaderCode(HeaderCode.TOLL_TAG_SERIAL_NUMBER_ALREADY_SELECTED_FOR_APPROVAL);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		serialNumberLock.lock(60, TimeUnit.SECONDS);

		Epc epc = this.tollDBService.findEpcBySerialNumber(updateReq.getSerialNumber());

		if (epc == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("EPC Serial Number not linked with iin").data("iin", updateReq.getIin())
					.data("serialNumber", updateReq.getSerialNumber()).format());
			removeLock(userProfileLock, serialNumberLock);

			response.addHeaderCode(HeaderCode.IIN_FROM_TAG_ID_DOES_N0T_MATCH_PAYER_IIN);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		if (new HashSet<>(
				Arrays.asList(ArrayUtils.isNotEmpty(environment.getActiveProfiles()) ? environment.getActiveProfiles()
						: environment.getDefaultProfiles()))
				.contains("prod") && (StringUtils.isEmpty(epc.getTid()) || StringUtils.isEmpty(epc.getSignData()))) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("EPC Tid is Empty").format());

			removeLock(userProfileLock, serialNumberLock);

			response.addHeaderCode(HeaderCode.TOLL_TAG_SERIAL_NUMBER_NOT_APPROVED_YET);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		TollRegistration tollRegistration = null;

		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();
		List<String> roleList = new ArrayList<>();
		roleList.add(DBConstants.UserRoles.MERCHANT_INTERNAL.value());

		if (CollectionUtils.isEmpty(merchants)) {
			removeLock(userProfileLock, serialNumberLock);
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("merchant is null").format());
			response.addHeaderCode(HeaderCode.MERCHANT_DOESNT_EXIST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		TollTag tollTag = this.tollDBService.findTollTagById(updateReq.getId());

		if (tollTag == null) {
			removeLock(userProfileLock, serialNumberLock);
			response.addHeaderCode(HeaderCode.TOLL_TAG_DOESNT_EXIST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		List<TollTag> tollTagByRegistrationNoAndBankId = this.tollDBService
				.findTollTagByRegistrationNoAndBankId(tollTag.getRegistrationNo(), bank.getId());

		if (!CollectionUtils.isEmpty(tollTagByRegistrationNoAndBankId)) {
			for (TollTag t : tollTagByRegistrationNoAndBankId) {
				if (t.getStatus().equals(String.valueOf(TollTagApprovalStatus.ACTIVE.value()))) {
					response.addHeaderCode("TC-1000", "Active tolltag already present");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}
		}

		if (TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() != Integer.parseInt(tollTag.getStatus())) {
			removeLock(userProfileLock, serialNumberLock);
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Tag is already activated")
					.format());
			response.addHeaderCode(HeaderCode.TOLL_TAG_PROFILE_ALREADY_ACTIVATED);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		TollTag tollTagDeepCopy = objectMapper.readValue(objectMapper.writeValueAsString(tollTag), TollTag.class);

		TollTag tollTagCloneCopy = objectMapper.readValue(objectMapper.writeValueAsString(tollTag), TollTag.class);

		// it should get called before setter of tollTag obj other wise this will never
		// give null obj
		TollTag tolltag = tollDBService.findTollCustomersByTagId(epc.getRfidTag());
		TagNPCIApprovalStatus tagNPCIApprovalStatus = new TagNPCIApprovalStatus();

		boolean isTagIdBlank = false;
		tollTagCloneCopy.setVendorIPAddress(originIp);
		if (StringUtils.isBlank(tollTag.getTagId()) && StringUtils.isBlank(tollTag.getTid())) {
			isTagIdBlank = true;
			tollTagCloneCopy.setTagId(epc.getRfidTag());

			tagNPCIApprovalStatus.setTagId(epc.getRfidTag());

			if (new HashSet<>(Arrays
					.asList(ArrayUtils.isNotEmpty(environment.getActiveProfiles()) ? environment.getActiveProfiles()
							: environment.getDefaultProfiles()))
					.contains("prod")) {
				tollTagCloneCopy.setTid(epc.getTid());

				tagNPCIApprovalStatus.setTid(epc.getTid());
			} else {
				if (new HashSet<>(Arrays
						.asList(ArrayUtils.isNotEmpty(environment.getActiveProfiles()) ? environment.getActiveProfiles()
								: environment.getDefaultProfiles()))
						.contains("uat")) {
					tollTagCloneCopy.setTid(epc.getTid());
					tagNPCIApprovalStatus.setTid(epc.getTid());
				} else {
					tollTagCloneCopy.setTid(epc.getRfidTag());
					tagNPCIApprovalStatus.setTagId(epc.getRfidTag());
				}
			}

			tollTagCloneCopy.setSerialNumber(epc.getSerialNumber());
			tagNPCIApprovalStatus.setSerialNumber(epc.getSerialNumber());

			// If EPC VisibilityStatus=1 thenset TrackingId as BankCode-HandDelivery
			if (EpcVisibilityStatus.NOTSHOW.value() == epc.getVisibilityStatus()) {
				tollTagCloneCopy.setTrackingId(
						tollTag.getTollRegistration().getDob() + "-" + TollConstant.TOLLTAG_HAND_DELIVERY);
			}

		} else if (tollTag.getMiscCharges() == TagDeliveryType.HAND_DELIVERY.value()
				&& !tollTag.getTagId().equals(epc.getRfidTag())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("incoming serial  number does not matched with assigned serial number ")
					.data("incoming serial number", epc.getSerialNumber())
					.data("already assigned serial number", tollTag.getSerialNumber()).format());

			removeLock(userProfileLock, serialNumberLock);

			response.addHeaderCode("TC-999",
					"Enter this -> " + tollTag.getSerialNumber() + " Serial Number as this "
							+ "vehicle has already been assigned this -> " + tollTag.getSerialNumber()
							+ "Serial Number on previous approval");

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		if (/* isTagIdBlank && */ tolltag != null) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("tag already in db for Id " + tolltag.getId()).format());

			removeLock(userProfileLock, serialNumberLock);

			response.addHeaderCode(HeaderCode.TOLL_SERIAL_NUMBER_USED);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Tag").data("tollTag", tollTag)
				.format());
		tollRegistration = tollTag.getTollRegistration();

		if (tollTag.getMiscCharges() != TagDeliveryType.HAND_DELIVERY.value()
				&& epc.getStatus() == DBConstants.EpcStatus.USED.value()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.TOLL_SERIAL_NUMBER_USED.message()).format());

			removeLock(userProfileLock, serialNumberLock);

			response.addHeaderCode(HeaderCode.TOLL_SERIAL_NUMBER_USED);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// Validation of the Created of the Tag cannot be approver at Bank end.
		if (tollRegistration != null && user.getId() == tollRegistration.getCreatedBy()
				&& !UserRoles.merchantUserRoles().contains(user.getRole())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Attempt to appprove tag by the creator").format());

			removeLock(userProfileLock, serialNumberLock);

			response.addHeaderCode(HeaderCode.TOLL_ISSUER_CANNOT_BE_APPROVER);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		List<User> createdByUsers = this.userDBService.getUsersByIds(Arrays.asList(tollRegistration.getCreatedBy()));

		if (CollectionUtils.isEmpty(createdByUsers)) {
			LOG.debug(
					LogFormatter.instance(httpServletContext.getTraceId()).message("Created user not found").format());

			removeLock(userProfileLock, serialNumberLock);
			throw new APIException(HttpStatus.BAD_REQUEST, APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		User createdByUser = createdByUsers.get(0);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("isTagIdBlank", isTagIdBlank)
				.data("isConnectNpci", tollProperties.isConnectNpci()).format());

		if (tollProperties.isConnectNpci() && isTagIdBlank) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("BEFORE CALLING MANAGE TAG")
					.format());

			if (StringUtils.isEmpty(tollTagCloneCopy.getEngineNo())
					&& StringUtils.isEmpty(tollTagCloneCopy.getVinNumber())) {
				// call reqVehicleDetails api first get the vahan related details and then hit
				// manageTag api with those details
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("hitting  MANAGE TAG 1.0")
						.format());

				String last5DigitsOfEngineNo = null;
				if (StringUtils.isNotEmpty(tollTagCloneCopy.getEngineNo())
						&& tollTagCloneCopy.getEngineNo().length() > 5) {
					last5DigitsOfEngineNo = tollTagCloneCopy.getEngineNo()
							.substring(tollTagCloneCopy.getEngineNo().length() - 5);
				}
				// manage tag api version 1.0 commenting as we will be using manage tag version
				// 1.1 for vahan integration
				/*
				 * TollTagUpdateResponse tollTagUpdateResponse =
				 * uploadTheTollListToNETC(httpServletContext, Arrays.asList(tollTagCloneCopy),
				 * TollConstant.ADD_OP, bank, merchants.get(0));
				 */

				TollTagUpdateResponse tollTagUpdateResponse = uploadTheTollListToNETCManageTag(httpServletContext,
						Arrays.asList(tollTagCloneCopy), TollConstant.ADD_OP, bank, merchants.get(0));

				if (tollTagUpdateResponse == null) {
					tollTagDeepCopy.setTollRegistration(tollRegistration);
					if (this.tollDBService.updateTollTag(tollTagDeepCopy) == null) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("tollTagDeepCopy Not Updated").format());
					}
					removeLock(userProfileLock, serialNumberLock);
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("tollTagUpdateResponse is null").format());
					response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
				}
				TollTagUpdateResponse tollTagUpdateResp = objectMapper
						.readValue(objectMapper.writeValueAsString(tollTagUpdateResponse), TollTagUpdateResponse.class);
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
						if (HeaderCode.TAGID_ALREADY_PRESENT_IN_DATABASE.code().equalsIgnoreCase("N-" + errCode)) {
							callUpdateNpci = true;
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message(
									"tagId not in db hence calling tag Updation api " + tollTagCloneCopy.getTagId())
									.format());
						}
					}

					if (callUpdateNpci) {
						// manage tag api version 1.0 commenting as we will be using manage tag version
						// 1.1 for vahan integration
						/*
						 * TollTagUpdateResponse tollTagUpdateResponse =
						 * uploadTheTollListToNETC(httpServletContext, Arrays.asList(tollTagCloneCopy),
						 * TollConstant.ADD_OP, bank, merchants.get(0));
						 */

						tollTagUpdateResponse = uploadTheTollListToNETCManageTag(httpServletContext,
								Arrays.asList(tollTagCloneCopy), TollConstant.UPDATE_OP, bank, merchants.get(0));

						if (tollTagUpdateResponse == null) {
							if (this.tollDBService.updateTollTag(tollTagDeepCopy) == null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("tollTagDeepCopy Not Updated").format());
							}
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("tollTagUpdateResponse is null").format());
							removeLock(userProfileLock, serialNumberLock);
							response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
							return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
						}
						tollTagUpdateResp = objectMapper.readValue(
								objectMapper.writeValueAsString(tollTagUpdateResponse), TollTagUpdateResponse.class);
						for (TollTagUpdateResponse.Txn.Resp.Tag tags : tollTagUpdateResponse.getTxn().getResp()
								.getTag()) {
							tag = tags;

						}
						/*
						 * ResponseEntity netcRequestDetailsResponse =
						 * brontooResource.getVehicleInfo(user, null, tollTagCloneCopy.getTagId(), null,
						 * RegistrationType.DEFAULT.value(), bank.getReferenceId(), false);
						 * 
						 * 
						 * if(HttpStatus.BAD_REQUEST.value() >
						 * netcRequestDetailsResponse.getStatusCodeValue()) { TollTagResponse
						 * tollTagResponse = (TollTagResponse) netcRequestDetailsResponse.getBody();
						 * String excCodes[] = tollTagResponse.getExcCode().split(","); for(String
						 * excCode : excCodes) {
						 * if(TollConstant.EXC_CODE_LOWBALANCE_LIST.equalsIgnoreCase(excCode)) {
						 * tollTagCloneCopy.setExcCode(TollConstant.EXC_CODE_LOWBALANCE_LIST);
						 * tollTagCloneCopy.setTollRegistration(tollRegistration);
						 * brontooResource.updateExceptionList(tollTagCloneCopy, TollConstant.REMOVE_OP,
						 * null); }
						 * 
						 * } }
						 */
					}
					tollTagUpdateResp.getTxn().getResp().getTag().clear();
					tollTagUpdateResp.getTxn().getResp().setTag(Arrays.asList(tag));
					tollTagCloneCopy.setAddTagResponse(objectMapper.writeValueAsString(tollTagUpdateResp));
					tollTagCloneCopy.setAddTagErrorCode(tag.getErrCode());
					tollTagDeepCopy.setAddTagResponse(objectMapper.writeValueAsString(tollTagUpdateResp));
					tollTagDeepCopy.setAddTagErrorCode(tag.getErrCode());
				}
				if (tollTagUpdateResponse != null
						&& !tollTagUpdateResponse.getTxn().getResp().getTag().get(0).getErrCode()
								.equals(TollErrorCodes.Error_code_SUCCESS.code())
						&& !HeaderCode.VEHICLE_REGNO_OR_VIN_OR_ENGINENUMBER_ALREADY_REGISTERED_WITH_SOME_OTHER_TAGID
								.code().contains(tollTagUpdateResponse.getTxn().getResp().getTag().get(0).getErrCode())
						&& !tollTagUpdateResponse.getTxn().getResp().getTag().get(0).getResult()
								.equalsIgnoreCase(TollErrorCodes.Error_code_SUCCESS.message())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("tollTagDeepCopy")
							.data("tollTagDeepCopy", tollTagDeepCopy).format());
					tollRegistration.setStatus(String.valueOf(DBConstants.TollRegistrationStatus.DEFAULT.value()));
					tollTagDeepCopy.setTollRegistration(tollRegistration);
					this.tollDBService.updateTollTag(tollTagDeepCopy);
					removeLock(userProfileLock, serialNumberLock);
					response.addHeaderCodes(headerCodes);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				boolean ostaProcessed = true;
				boolean ostaDoesnotExist = false;
				if (tollTag.getRegistrationAmount().compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO
						|| tollTag.getDepositAmount().compareTo(BigDecimal.ZERO) > NumberUtils.INTEGER_ZERO
						|| tollTag.getMiscCharges() == TagDeliveryType.COURIER.value()) {
					List<Dipcoin> dcoins = dipcoinDBService.asyncFindDipcoin(tollTag.getCustomerAccountId(),
							Arrays.asList(DBConstants.DipcoinUsageType.DEPOSIT.value(),
									DBConstants.DipcoinUsageType.FEE.value()))
							.get();
					if (CollectionUtils.isEmpty(dcoins)) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("deposit and fees amount not available")
								.data("Customer Account Id", tollTag.getCustomerAccountId()).format());
						ostaDoesnotExist = true;
					} else {
						List<Integer> userId = new ArrayList<Integer>();
						userId.add(tollTag.getTollRegistration().getUserId());
						List<User> dcoinUser = userDBService.asyncGetUsersByIds(userId).get();
						ResponseEntity processDcoinResponse = null;
						for (Dipcoin dcoin : dcoins) {
							if (StringUtils.isNotBlank(dcoin.getUsageCategory())
									&& dcoin.getUsageCategory().equalsIgnoreCase(tollTag.getRegistrationNo())
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
				// Check if the BankProperties ---> tollTagApproval == true then Bank will
				// Active
				if (bankAPIServices.getBankProperties(bank.getReferenceId()).isTollTagApprovalSet()) {
					tollTagCloneCopy.setStatus(String.valueOf(TollTagApprovalStatus.ACTIVE.value()));
				} else {
					// Check the role of createdbyUser
					// if Bank --> Status = Active OR Customer = CUSTOMER_ACTIVATION_PENDING
					if (tollRegistration != null
							&& !createdByUser.getRole().equals(DBConstants.UserRoles.CUSTOMER.value())) {
						tollTagCloneCopy.setStatus(String.valueOf(TollTagApprovalStatus.ACTIVE.value()));
					} else {
						tollTagCloneCopy
								.setStatus(String.valueOf(TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value()));
					}
				}
				if (!ostaProcessed) {
					tollTagCloneCopy.setStatus(String.valueOf(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()));
				}
				tollTagCloneCopy.setVendorUpdateBy(user.getId());
				tollTagCloneCopy.setApprovedBy(user.getId());
				tollTagCloneCopy.setApprovedDateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
				tollTagCloneCopy.setApprovedIPAddress(originIp);
				tollTagCloneCopy.setVendorUpdateDateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
				tollRegistration.setStatus(String.valueOf(DBConstants.TollRegistrationStatus.ACTIVE.value()));
				tollTagCloneCopy.setTollRegistration(tollRegistration);
				// update the TollTag details.
				TollTag tollTagResponse = this.tollDBService.updateTollTag(tollTagCloneCopy);
				// After the Success From Tag Vendor NETC APPROVAL CALL and CREATE OSTA
				if (tollTagResponse == null) {
					// TODO Revert processed dipcoin
					removeLock(userProfileLock, serialNumberLock);
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("updatetollTag is null")
							.format());
					response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
				}
				if (isTagIdBlank) {
					epc.setStatus(DBConstants.EpcStatus.USED.value());
					epc = this.tollDBService.saveEpc(epc);
					if (epc == null) {
						removeLock(userProfileLock, serialNumberLock);
						LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Epc Not Updated")
								.format());
					}
				}
				if (!ostaProcessed) {
					removeLock(userProfileLock, serialNumberLock);
					response.addHeaderCode(HeaderCode.DIPCOIN_NOT_ACTIVE);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				} else if (ostaDoesnotExist) {
					removeLock(userProfileLock, serialNumberLock);
					response.addHeaderCode(HeaderCode.DIPCOIN_DOESNT_EXIST);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				} else {
					try {
						// success Email
						if (!emailUtils.sendTollCustomerTagAllotedEmail(originIp, tollTag, tollRegistration,
								epc.getSerialNumber())) {
							LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
									.message("Failed to send email to user")
									.data("user email", tollRegistration.getEmailId()).format());
						}
						// success message
						if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(
								tollRegistration.getMobileNo(),
								Templates.TollTagBankApproval.format(tollTag.getRegistrationNo(), bank.getAlias()),
								true)) {
							LOG.error(
									LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
											.data("phone", tollRegistration.getMobileNo()).format());
						}
						if (applicationProperties.getAwsSMSClient()) {
							NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
							notificationRequestContext.setTraceId(httpServletContext.getTraceId());
							if (!notificationResource.sendSms(tollTag.getRegistrationNo(),
									Templates.TollTagBankApproval.format(tollTag.getRegistrationNo(), bank.getAlias()),
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
				}
				response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BANK_APRROVED);
				return ResponseEntity.status(HttpStatus.OK).body(response);

			} else {
				// new method

				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("hitting  MANAGE TAG 1.1")
						.format());

				String last5DigitsOfEngineNo = null;
				if (StringUtils.isNotEmpty(tollTagCloneCopy.getEngineNo())
						&& tollTagCloneCopy.getEngineNo().length() > 5) {
					last5DigitsOfEngineNo = tollTagCloneCopy.getEngineNo()
							.substring(tollTagCloneCopy.getEngineNo().length() - 5);

					tagNPCIApprovalStatus.setSerialNumber(epc.getSerialNumber());
					tagNPCIApprovalStatus.setTollTagId(tollTag.getId());
					tagNPCIApprovalStatus.setApprovalTime(String.valueOf(System.currentTimeMillis()));

					TollTagUpdateResponse tollTagUpdateResponse = asyncUploadTheTollListToNETCManageTag(
							httpServletContext, Arrays.asList(tollTagCloneCopy), TollConstant.ADD_OP, bank,
							merchants.get(0), tagNPCIApprovalStatus);
					if (tollTagUpdateResponse == null) {
						tollTagDeepCopy.setTollRegistration(tollRegistration);
						if (this.tollDBService.updateTollTag(tollTagDeepCopy) == null) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("tollTagDeepCopy Not Updated").format());
						}
						removeLock(userProfileLock, serialNumberLock);
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("tollTagUpdateResponse is null").format());
						response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

					}

				}

			}
			tollTagCloneCopy.setVendorUpdateBy(user.getId());
			tollTagCloneCopy.setApprovedBy(user.getId());
			tollTagCloneCopy.setApprovedIPAddress(originIp);
			tollTagCloneCopy.setTollRegistration(tollRegistration);

			// This is done because these data should not get set before NPCI response other
			// wise tag will get wasted.
			tollTagCloneCopy.setTid(null);
			tollTagCloneCopy.setTagId(null);
			tollTagCloneCopy.setSerialNumber(null);

			if (this.tollDBService.updateTollTag(tollTagCloneCopy) == null) {
				LOG.debug(
						LogFormatter.instance(httpServletContext.getTraceId()).message("tollTag Not Updated").format());
			}

//	        if (isTagIdBlank) {
//	          epc.setStatus(DBConstants.EpcStatus.PENDING.value());
//	          epc = this.tollDBService.saveEpc(epc);
//	          if (epc == null) {
//	            removeLock(userProfileLock, serialNumberLock);
//	            LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
//	                .message("Epc Not Updated").format());
//	          }
//	        }
			return ResponseEntity.status(HttpStatus.OK).body(response);
		}

		tollTagCloneCopy.setVendorUpdateBy(user.getId());

		tollTagCloneCopy.setTollRegistration(tollRegistration);

		// This is done because these data should not get set before NPCI response other
		// wise tag will get wasted.
		LocalDateTime now = LocalDateTime.now();
		if (tollTag.getTagId() == null) {

			tollTagCloneCopy.setTagId(now.toString());
		}
		if (tollTag.getTid() == null) {
			tollTagCloneCopy.setTid(now.toString());
		}

		tollTagCloneCopy.setStatus("1");

		String tollTagUpdateRespJson = "{" + "\"head\": {" + "    \"ver\": \"1.1\","
				+ "    \"ts\": \"2024-03-21T16:16:12\"," + "    \"orgId\": \"brio\"," + "    \"msgId\": \"brio21MAR24\""
				+ "}," + "\"txn\": {" + "    \"id\": \"BAOIWUHFCYR0PGTEQWANEP\"," + "    \"note\": \"ADD\","
				+ "    \"refId\": \"\"," + "    \"refUrl\": \"https://192.168.6.190/\","
				+ "    \"ts\": \"2024-03-21T16:16:12\"," + "    \"type\": \"ManageTag\"," + "    \"resp\": {"
				+ "        \"ref\": null," + "        \"merchantId\": null,"
				+ "        \"ts\": \"2024-03-21T16:16:17\"," + "        \"result\": \"SUCCESS\","
				+ "        \"respCode\": \"000\"," + "        \"tag\": [" + "            {"
				+ "                \"op\": \"ADD\"," + "                \"tagId\": \"34161FA8202F874608000318\","
				+ "                \"seqNum\": \"1\"," + "                \"result\": \"SUCCESS\","
				+ "                \"errCode\": \"240\"" + "            }" + "        ]" + "    }" + "}" + "}";

		tollTagCloneCopy.setAddTagResponse(tollTagUpdateRespJson);

		tollTagCloneCopy.setSerialNumber(updateReq.getSerialNumber());

		this.tollDBService.updateTollTag(tollTagCloneCopy);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("NOT APPROVED").format());

		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	public void removeLock(RLock userProfileLock, RLock serialNumberLock) {
		if (userProfileLock != null) {
			try {
				userProfileLock.unlock();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("user Profile unlocked")
						.format());
			} catch (Exception ex) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Exception Caught while unlocking Lock").format(), ex);
			}
		}
		if (serialNumberLock != null) {
			try {
				serialNumberLock.unlock();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("serial Number unlocked")
						.format());
			} catch (Exception ex) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Exception Caught while unlocking Lock").format(), ex);
			}
		}
	}

	public TollTagUpdateResponse uploadTheTollListToNETCManageTag(final HttpServletContext httpServletContext,
			List<TollTag> tollTagRequests, String operation, Bank bank,
			Merchant merchant /* ,ReqVehicleDetailResponse reqVehicleDetailResponse */) throws APIException, Exception {

		// For the time being return true response.

		TollTagUpdateRequest tollTagUpdateRequest = new TollTagUpdateRequest();
		String refUrl = StringUtils.EMPTY;
		TollTagUpdateResponse tollTagUpdateResponse = new TollTagUpdateResponse();

		TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();
		if (tollProperties.isConnectNpci()) {
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
		// head.setVer(TollConstant.VER);
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

			if (operation.equalsIgnoreCase(TollConstant.ADD_OP)) {

				detailList.add(new Detail(TollConstant.TID, tollTagRequest.getTid()));

				detailList.add(new Detail(TollConstant.ISSUEDATE, issueDateformatter.format(date)));

				detailList.add(new Detail(TollConstant.EXCCODE, tollTagRequest.getExcCode()));

				// Registered or unregistered vehicle at RTO should be T|F

				// ******** Commented By Murad, If RegNum is not empty in NETC Request then set
				// T (told by Sanskruti)
				// detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE,
				// tollTagRequest.getRegisteredVehicle()));
				// ********

				// detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE,
				// "T"/*tollTagRequest.getRegisteredVehicle()*/));

				// detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE, "T"));

				// state should be short code
				// detailList.add(new Detail(TollConstant.STATE, tollTagRequest.getState()));
				// detailList.add(new Detail(TollConstant.STATE,
				// "MH"/*tollTagRequest.getState()*/));

				// should be NEW_TAG|REPLACEMENT_TAG
				// detailList.add(new Detail(TollConstant.TAGDESCRIPTOR,
				// tollTagRequest.getTagDescriptor()));
				// detailList.add(new Detail(TollConstant.TAGDESCRIPTOR,
				// "REPLACEMENT_TAG"/*tollTagRequest.getTagDescriptor()*/));

			}

			detailList.add(new Detail(TollConstant.VEHICLECLASS, mappedVehicleClass));

			/*
			 * detail = new Detail(); detail.setName(TollConstant.REGNUMBER);
			 * detail.setValue(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() ==
			 * tollTagRequest.getVinVrnFlag() ? tollTagRequest.getRegistrationNo() :
			 * (tollTagRequest.getVinNumber().length() <= 20 ? tollTagRequest.getVinNumber()
			 * : tollTagRequest.getVinNumber().substring(0, 19))); detailList.add(detail);
			 */

			detailList.add(new Detail(TollConstant.REGNUMBER,
					VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
							? tollTagRequest.getRegistrationNo()
							: (tollTagRequest.getVinNumber().length() <= 20 ? tollTagRequest.getVinNumber()
									: tollTagRequest.getVinNumber().substring(0, 20))));

			// for managetag 1.1 registrationNo is optional and vin is mandatory that is why
			// added this check

			// commented by Murad -> if we don't have reg no then pass starting 20 digit of
			// VIN (Sanskruti Mane)
//			if(/*tollTagRequest.getVinNumber().isEmpty()*/ tollTagRequest.getVinVrnFlag() != DBConstants.VinVrn.VIN_CHACIS.value()) {
//				detailList.add(new Detail(TollConstant.REGNUMBER,VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
//			              ? tollTagRequest.getRegistrationNo()
//			              : (tollTagRequest.getVinNumber().length() <= 10 ? tollTagRequest.getVinNumber()
//			                      : tollTagRequest.getVinNumber().substring(0, 10))));					
//			}else {
//				detailList.add(new Detail(TollConstant.REGNUMBER, ""));
//			}

			// detailList.add(new Detail(TollConstant.VIN, tollTagRequest.getVinNumber()));
			// detailList.add(new Detail(TollConstant.VIN,
			// "5KL8M8ED6B0108371"/*tollTagRequest.getVinNumber()*/));

			// detailList.add(new Detail(TollConstant.ENGINENO,
			// tollTagRequest.getEngineNo()));
			// detailList.add(new Detail(TollConstant.ENGINENO,
			// "JH4G589HGB54842158"/*tollTagRequest.getEngineNo()*/));

			// PETROL|DIESEL|ELECTRIC|HYBRID|CNG
			// detailList.add(new Detail(TollConstant.VEHICLEDESCRIPTOR,
			// tollTagRequest.getVehicleDescriptor()));
			// detailList.add(new Detail(TollConstant.VEHICLEDESCRIPTOR,
			// reqVehicleDetailResponse.getVEHICLEDESCRIPTOR()));

			// T|F
			// detailList.add(new Detail(TollConstant.NATIONALPERMIT,
			// tollTagRequest.getNationalPermit()));
			// detailList.add(new Detail(TollConstant.NATIONALPERMIT,
			// "T"/*tollTagRequest.getNationalPermit()*/));

			// if national permit is false then there will be no permit expiry date
//			if(tollTagRequest.getPermitExpiryDate() != null && !tollTagRequest.getPermitExpiryDate().isEmpty()){
//				detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE, tollTagRequest.getPermitExpiryDate()));
//			}else {
//			    detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE, ""));
//			}

			// detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE,
			// "15-12-2028"/*String.valueOf(tollTagRequest.getPermitExpiryDate())*/));

			detailList.add(new Detail(TollConstant.COMVEHICLE, tollTagRequest.getIsCommercial()));

			tag.setDetail(detailList);
			tag.setOp(operation);
			tag.setTagId(tollTagRequest.getTagId());
			tag.setSeqNum(String.valueOf(tollTagRequests.indexOf(tollTagRequest) + NumberUtils.INTEGER_ONE));
//			tag.setType(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
//					? (operation.equalsIgnoreCase(TollConstant.ADD_OP) ? operation : TollConstant.UPDATE_OP)
//					: tollTagRequest.getVinNumber());
			tag.setType(tollTagRequest.getRegistrationNo());
			tag.setType(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
					? tollTagRequest.getRegistrationNo()
					: (tollTagRequest.getVinNumber().length() <= 20 ? tollTagRequest.getVinNumber()
							: tollTagRequest.getVinNumber().substring(0, 20)));
			// tag.setType("");

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

		// NPCI Active Active Setup Phase2 changes
		String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
		if (StringUtils.isEmpty(ipAddress)) {

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

			responseData = tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);

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
		tollTagUpdateResponse = JAXB.unmarshal(new StringReader(responseData), TollTagUpdateResponse.class);

		return tollTagUpdateResponse;

	}

	public ResponseEntity fetchApprovalstatus(String serialNumber) throws Exception {

		if (serialNumber == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("SerialNumber is empty").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));

		}
		TollTagResponse response = new TollTagResponse();

		TollTag tolltag = this.tollDBService.findTollTagBySerialNumber(serialNumber);

		LOG.debug(LogFormatter.instance().data("tollTag", tolltag).format());

		RespVehicleDetails respMngTagResp = objectMapper.readValue(tolltag.getAddTagResponse(),
				RespVehicleDetails.class);

		LOG.debug(LogFormatter.instance().data("respMngTagResp", respMngTagResp).format());

		String[] errCodes = null;

		if (respMngTagResp.getTxn().getResp().getTag().get(0).getResult().equalsIgnoreCase("SUCCESS")) {

			TollTagFeeAndChargesResponse responseCharges = new TollTagFeeAndChargesResponse();

			List<FeesAndDeposit> feesAndDeposit = feesAndDepositDBService
					.findByBankIdAndChargeCategory(
							Arrays.asList(
									tolltag.getWalletBankId() != NumberUtils.INTEGER_ZERO ? tolltag.getWalletBankId()
											: tolltag.getBankId()),
							new HashSet<>(Arrays.asList(tolltag.getCategory())));
			if (CollectionUtils.isEmpty(feesAndDeposit)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("feesAndDeposit empty")
						.data("Bank Id", tolltag.getBankId()).data("Category", tolltag.getCategory()).format());

			}
			Boolean miscCharges = DBConstants.TollTagMiscCharges.TRUE.value()
					.equals(Integer.toString(tolltag.getMiscCharges()));

			BigDecimal miscGst = miscCharges
					? getGstAmountCalculation(feesAndDeposit.get(NumberUtils.INTEGER_ZERO).getMiscellaneousCharges())
					: BigDecimal.ZERO;
			BigDecimal courierCharges = miscCharges
					? feesAndDeposit.get(NumberUtils.INTEGER_ZERO).getMiscellaneousCharges().subtract(miscGst)
					: BigDecimal.ZERO;

			responseCharges.setBankId(tolltag.getBankId()).setChargeCategory(tolltag.getCategory())
					.setDepositAmount(tolltag.getDepositAmount()).setMinimumAmount(tolltag.getMinimumAmount())
					.setGstAmount(getGstAmountCalculation(tolltag.getRegistrationAmount()).add(miscGst))
					.setCourierCharges(courierCharges).setRegistrationAmount(tolltag.getRegistrationAmount()
							.subtract(getGstAmountCalculation(tolltag.getRegistrationAmount())));

			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Tag Result is Success").format());
			return ResponseEntity.status(HttpStatus.OK).body(responseCharges);
		} else {
			try {
				List<HeaderCode> headerCodes = new ArrayList<>();

				// Iterate over each tag in the response
				for (Tag tag : respMngTagResp.getTxn().getResp().getTag()) {

					// Split error codes and response codes
					errCodes = tag.getErrCode().split(",");

					String respCodes = respMngTagResp.getTxn().getResp().getRespCode();

					// Iterate over each error code and add corresponding HeaderCode to the list
					for (String errCode : errCodes) {
						for (HeaderCode headerCode : HeaderCode.values()) {
							if (headerCode.code().equalsIgnoreCase("N-" + errCode.trim())) {
								headerCodes.add(headerCode);
							}
						}
					}
				}

				response.addHeaderCodes(headerCodes);

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
			} catch (Exception e) {
				// Catching exceptions thrown while handling error codes
				// LOG.error("Error occurred while processing error codes: " + e.getMessage()).;
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Error occurred while processing error codes: ").data("Error", errCodes).format());
				throw new Exception("Error occurred while processing error codes: " + e.getMessage());
			}
		}
	}

	/**
	 * Get the Toll Tag Charges Specific to the Tag.
	 * 
	 * @param user
	 * @param miscCharges
	 * @param encTtid
	 * @return
	 * @throws Exception
	 * @throws APIException
	 */
	public ResponseEntity getTollTagChargesByCategory(final User user, final Bank bank, final String category,
			final Integer registrationType, Boolean miscCharges, final String accountNumber, final String cardId)
			throws Exception, APIException {

		TollRegistrationResponse response = new TollRegistrationResponse();
		List<APIResponse> listresponses = new ArrayList<APIResponse>();
		TollTagFeeAndChargesResponse responseCharges = new TollTagFeeAndChargesResponse();

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get the Vehicle Charges.")
				.data("category", category).data("Bank", bank.getId()).format());

		Set<String> hashSet = new HashSet();
		hashSet.add(category);

		// @TODO WRITE THE CODE FOR FETCHING.
		List<FeesAndDeposit> feesAndDeposits = feesAndDepositDBService
				.findByBankIdAndChargeCategory(Arrays.asList(bank.getId()), hashSet);

		// Data check.
		if (CollectionUtils.isEmpty(feesAndDeposits)) {
			LOG.error(
					LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to fetch fees And Deposits")
							.data("Bank Id", bank.getId()).data("hashSet", hashSet).format());
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(APIResponse.error(HeaderCode.BANK_CHARGES_NOT_AVAILABLE));
		}

		// Minimium amount calculation
		if (StringUtils.isNoneBlank(accountNumber)) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).data("AccountNumber", accountNumber)
					.format());

			List<TollTag> tollTags = tollDBService.findTollTagsByAccountNumber(accountNumber);

			LOG.error(
					LogFormatter.instance(httpServletContext.getTraceId()).data("tollTags", tollTags.size()).format());

			// If two CustomerAccountFound then this logic will not execute as it ambigous
			// two say.
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
		}

		if (StringUtils.isNoneBlank(cardId)) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).data("AccountNumber", accountNumber)
					.format());

			List<TollTag> tollTags = tollDBService
					.findTollTagsByCustomerAccountId(Arrays.asList(Integer.parseInt(cardId)));

			LOG.error(
					LogFormatter.instance(httpServletContext.getTraceId()).data("tollTags", tollTags.size()).format());

			// If two CustomerAccountFound then this logic will not execute as it ambigous
			// two say.
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
		}

		try {

			for (int i = 0; i < feesAndDeposits.size(); i++) {
				responseCharges.setGstAmount(getGstAmountCalculation(RegistrationType.IHMCL.value() == registrationType
						? feesAndDeposits.get(i).getIHMCLRegistrationAmount()
						: feesAndDeposits.get(i).getRegistrationAmount()));
				populateTollChargesResponse(user, feesAndDeposits.get(i), responseCharges, registrationType,
						miscCharges);
				listresponses.add(responseCharges);
			}

		} catch (Exception e) {
			LOG.error("Failed to set values for toll tag charges and Vehicle class " + feesAndDeposits);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		response.addHeaderCode(HeaderCode.SUCCESS);
		return ResponseEntity.status(HttpStatus.OK).body(listresponses);

	}

	/*
	 * Getting the Response for Toll Customers According to BankCode
	 */
	public void populateTollChargesResponse(final User user, final FeesAndDeposit feesAndDeposit,
			TollTagFeeAndChargesResponse response, final Integer registrationType, Boolean miscCharges)
			throws APIException {

		BigDecimal registrationAmount = BigDecimal.ZERO;
		if (feesAndDeposit != null) {

			if (RegistrationType.IHMCL.value() == registrationType) {
				registrationAmount = feesAndDeposit.getIHMCLRegistrationAmount()
						.subtract(getGstAmountCalculation(feesAndDeposit.getIHMCLRegistrationAmount()));
			} else {
				registrationAmount = feesAndDeposit.getRegistrationAmount()
						.subtract(getGstAmountCalculation(feesAndDeposit.getRegistrationAmount()));
			}

			BigDecimal miscGst = miscCharges ? getGstAmountCalculation(feesAndDeposit.getMiscellaneousCharges())
					: BigDecimal.ZERO;
			BigDecimal courierCharges = miscCharges ? feesAndDeposit.getMiscellaneousCharges().subtract(miscGst)
					: BigDecimal.ZERO;

			response.setBankId(feesAndDeposit.getBankId()).setChargeCategory(feesAndDeposit.getChargeCategory())
					.setDepositAmount(feesAndDeposit.getDepositAmount())
					.setMinimumAmount(feesAndDeposit.getMinimumAmount())
					.setMinAmountMaintainForTag(feesAndDeposit.getMinimumAmount())
					.setGstAmount(response.getGstAmount().add(miscGst)).setRegistrationAmount(registrationAmount)
					.setCourierCharges(courierCharges).setCreatedOn(feesAndDeposit.getCreatedOn())
					.setUpdateOn(feesAndDeposit.getUpdateOn());
		}
	}

	public ResponseEntity vehicleVerification(User user, String vehicleRegistrationNo, String tagId, String tid,
			Integer regType, String bankReferenceId) {
		// Check for the Request form bank
		TollTagResponse tollTagResponse = new TollTagResponse();
		if (!(UserRoles.bankUserRoles().contains(user.getRole())
				|| UserRoles.merchantUserRoles().contains(user.getRole()))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (!tollProperties.isConnectNpci()) {
			List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNo(Arrays.asList(vehicleRegistrationNo));
			String excCode = StringUtils.EMPTY;
			if (CollectionUtils.isNotEmpty(tollTags)) {

				for (TollTag tollTag : tollTags) {
					if (!(TollTagApprovalStatus.INACTIVE.value() == Integer.parseInt(tollTag.getStatus())
							|| TollTagApprovalStatus.BANK_REJECTED.value() == Integer.parseInt(tollTag.getStatus()))
							|| TollConstant.EXC_CODE_BLACKLIST.equalsIgnoreCase(tollTag.getExcCode())
							|| TollConstant.EXC_CODE_HOTLIST.equalsIgnoreCase(tollTag.getExcCode())) {

						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Vehicle already present").data("TollTag Status:", tollTag.getStatus())
								.data("TollTag Exccode", tollTag.getExcCode()).format());

						tollTagResponse.addHeaderCode(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST);
						tollTagResponse.setExcCode(TollConstant.EXC_CODE_BLACKLIST);

						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Vehicle already present")
								.data("TollTagResponse Exccode", tollTagResponse.getExcCode()).format());

						return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);
					}
				}

				for (TollTag tollTag : tollTags) {
					excCode = tollTag.getExcCode() + "," + excCode;
				}

				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Vehicle already present")
						.data("TollTag Exccode", excCode).format());

				tollTagResponse.addHeaderCode(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST);
				tollTagResponse.setExcCode(excCode);
				return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);
			}

			tollTagResponse.addHeaderCode(HeaderCode.TOLL_VEHICLE_REG_NO_NOT_PRESENT);
			return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);

		}

		return brontooResource.getVehicleInfo(user, vehicleRegistrationNo, tagId, tid, regType, bankReferenceId);

		// return brontooResource.getVehicleInfo(user, vehicleRegistrationNo, tagId,
		// tid, engineNo, vinNumber, vrn, last5digitofengineno, regType,
		// bankReferenceId);
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

// Bank Vehicle Verification Status
public ResponseEntity bankVehicleVerification(User user, String vehicleRegistrationNo,
    String tagId, String tid, String serialNumber) throws APIException, Exception {

  if (!(this.userDBService.isBankSuperAdmin(user) || this.userDBService.isBankAdmin(user)
      || this.userDBService.isBankTransactor(user) || this.userDBService.isBankViewer(user))) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
  }

  if (!this.userDBService.isActive(user)) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
  }

  if (StringUtils.isBlank(vehicleRegistrationNo)) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
  }


  if (!tollProperties.isConnectNpci()) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(APIResponse.error(HeaderCode.TOLL_API_NOT_ALLOWED));
  }



  Bank bank = bankDBService.getBank(user.getBankMerchantId());

  if (bank == null) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
  }


  List<TollTag> tollTags =
      tollDBService.findTollTagByRegistrationNoAndBankId(vehicleRegistrationNo, bank.getId());


  if (CollectionUtils.isEmpty(tollTags)) {
    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .body(APIResponse.error(HeaderCode.TOLL_VEHICLE_REG_NO_NOT_ASSOCIATED_WITH_BANK));
  }

	return brontooResource.getVehicleInfo(user, vehicleRegistrationNo, tagId, tid, RegistrationType.DEFAULT.value(),
			bank.getReferenceId() /*,true*/);

 // return brontooResource.getVehicleInfo(user, vehicleRegistrationNo, tagId, tid, null, vin, vrn, last5digitofengineno, RegistrationType.DEFAULT.value(), bank.getReferenceId());
}



public ResponseEntity<?> fetchTollTagsWithVin(User user, String branchCode , String clientTransactionId, 
		Long startTime, Long endTime, Integer start, Integer count , String serialNumber) {

  TollTagsWihVinDetail response = new TollTagsWihVinDetail();

  if (clientTransactionId == null) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
  }

  if (!this.userDBService.bankRepresentative(user)) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
  }

  if (!this.userDBService.isActive(user)) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
  }
  
	/* to solve prod issue
	 * BankBranchDetails bankBranchDetail =
	 * this.bankBranchDetailsService.findBankBranchDetailsByBranchCode(branchCode);
	 * if (bankBranchDetail == null) { return
	 * ResponseEntity.status(HttpStatus.BAD_REQUEST)
	 * .body(APIResponse.error(HeaderCode.INVALID_BRANCH_CODE)); }
	 */

  List<TollTagWihVinDetail> tollTagWihVinDetails = new ArrayList<>();
  
	List<TollTag> tollTags = null;


	if (StringUtils.isNotEmpty(serialNumber) && serialNumber != null) {

          LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
              .message("in serialNumber")
              .data("serialNumber", serialNumber).format());

          tollTags = this.tollDBService
              .findByBankIdAndVinVrnFlagAndStatusAndApprovedDateTimeBetweenAndSerialNumber(
                  httpServletContext.getBank().getId(), VinVrn.VIN_CHACIS.value(),
                  String.valueOf(TollTagApprovalStatus.ACTIVE.value()), String.valueOf(startTime),
                  String.valueOf(endTime), start, count, serialNumber);

        } else if(StringUtils.isNotEmpty(branchCode) && branchCode != null){

          LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
              .message("in branchCode")
              .data("branchCode", branchCode).format());

          tollTags = this.tollDBService
              .findByBankIdAndVinVrnFlagAndStatusAndApprovedDateTimeBetweenAndTollRegistrationDob(
                  start, count, httpServletContext.getBank().getId(), VinVrn.VIN_CHACIS.value(),
                  TollTagApprovalStatus.ACTIVE.value(), String.valueOf(startTime),
                  String.valueOf(endTime), branchCode);
        }

  if (CollectionUtils.isEmpty(tollTags)) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(APIResponse.error(HeaderCode.TOLL_TAGS_NOT_FOUND));
  }

  for (TollTag tollTag : tollTags) {

    TollRegistration tollRegistration = tollTag.getTollRegistration();

    TollTagWihVinDetail tollTagWihVinDetail = new TollTagWihVinDetail();
    tollTagWihVinDetail.setFirstName(tollRegistration.getFirstName());
    tollTagWihVinDetail.setLastName(tollRegistration.getLastName());
    tollTagWihVinDetail.setMobileNo(tollRegistration.getMobileNo());
    tollTagWihVinDetail.setSerialNumber(tollTag.getSerialNumber());
    tollTagWihVinDetail.setVin(tollTag.getVinNumber());

    // if documents not uploaded
    if (StringUtils.isEmpty(tollTag.getUpdatedDocs())) {
      tollTagWihVinDetail.setIsDocumentUploaded(BooleanStatus.NO.value());
      tollTagWihVinDetail.setIsDocumentVerified(BooleanStatus.NO.value());
      tollTagWihVinDetail.setVrn("");
      tollTagWihVinDetail.setRcImage("");
      tollTagWihVinDetail.setVehicleImage("");
    }

    else {
      tollTagWihVinDetail.setIsDocumentUploaded(BooleanStatus.YES.value());
      tollTagWihVinDetail.setVrn(tollTag.getVinToVrn());

      // updatedDocs = "image1path,image2path" example
      // image1path --> rc image , image2path --> vehicle image
      String[] uploadedDocuments = tollTag.getUpdatedDocs().trim().split(",");

      if (ArrayUtils.isNotEmpty(uploadedDocuments)) {
        tollTagWihVinDetail.setRcImage(uploadedDocuments[0].trim());
        tollTagWihVinDetail.setVehicleImage(uploadedDocuments[1].trim());

		/*
		 * if (tollTag.getVinDocVerify() == VinDocVerify.VERIFIED.value()) {
		 * tollTagWihVinDetail.setIsDocumentVerified(BooleanStatus.YES.value()); }
		 * 
		 * else { tollTagWihVinDetail.setIsDocumentVerified(BooleanStatus.NO.value()); }
		 */
        
        tollTagWihVinDetail.setIsDocumentVerified(tollTag.getVinDocVerify());
      }
    }

    // vinVrnFlag --> if flag indicates vin that means vrn is not updated to NETC
    // this will always be NO here
    // Note : vinVrnFlag --> if flag indicates vrn that means TollTag would not get fetched here
    // in TollTagWihVinDetail
    if(tollTag.getRegistrationNo().equals(tollTag.getVinToVrn())){
    	
    	tollTagWihVinDetail.setIsVrnUpdatedWithNETC(BooleanStatus.YES.value());
    }
    else {
    	tollTagWihVinDetail.setIsVrnUpdatedWithNETC(BooleanStatus.NO.value());

    }

    LocalDate todaysDate = LocalDate.now();
    LocalDate tagApprovalDate =
        Instant.ofEpochMilli(Long.valueOf(tollTag.getApprovedDateTime()))
            .atZone(ZoneId.systemDefault()).toLocalDate();
    long diffInDays = ChronoUnit.DAYS.between(tagApprovalDate, todaysDate);

    tollTagWihVinDetail.setNoOfDaysFromDateOfTagApproval(diffInDays);

    tollTagWihVinDetails.add(tollTagWihVinDetail);

  }

  // setting pagination info
  Pagination pagination = new Pagination();
  pagination.setStartRange(startTime);
  pagination.setEndRange(endTime);
  pagination.setScanCompleted(tollTags.size() < count);
  pagination.setTotal(tollTags.size());
  if (!pagination.getScanCompleted()) {
    pagination.setStart(start + count);
  }

  response.setTollTagWihVinDetails(tollTagWihVinDetails);
  response.setPagination(pagination);

  return ResponseEntity.status(HttpStatus.OK).body(response);
}



public ResponseEntity getTagRechargeReport(User bankUser, Bank bank, Long startTime, Long endTime, Integer start,
		Integer count, String vehicleNumber) throws InterruptedException, ExecutionException {

	if (this.userDBService.isBankAdmin(bankUser)) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
	}

	if (!this.userDBService.bankRepresentative(bankUser)) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
	}

	if (!this.bankDBService.isActive(bank)) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
	}

	return tollServicesResource.getTagRechargeReport(httpServletContext.getUser(), startTime, endTime, start, count,
			vehicleNumber);
}

// ###########################################################################################################################
	

//get Dipcoin transaction
	@SuppressWarnings("unlikely-arg-type")
	public List<TollTransactionsReport> populateDipcoinTransactionInfo(final TollTag tag,
			Map<String, DipcoinTransaction> dtxLookUp, Map<String, BankTransaction> btxLookUp,
			Map<String, Dipcoin> dCoinLookUp, final List<String> vehicleNumber, final String startDateValue,
			final String endDateValue) {
		List<TollTransactionsReport> transactionsResponse = new ArrayList<>();
		for (Map.Entry<String, DipcoinTransaction> entry : dtxLookUp.entrySet()) {
			TollTransactionsReport response = new TollTransactionsReport();

			try {
				try {
					response.setDipcoinStatus(dCoinLookUp.get(entry.getKey()).getStatus());
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("dipcoin status set exception for dtx" + entry.getKey()).format());
				}

				TollReqPayRequest tollReqPayRequest = null;
				try {
					tollReqPayRequest = objectMapper.readValue(entry.getValue().getPartnerRawRequest(),
							TollReqPayRequest.class);
				} catch (Exception e) {

					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Reqpay json Conversion errror").format());
				}

				String[] fastagTxnDateTime = tollReqPayRequest != null
						? tollReqPayRequest.getMerchant().getReaderVerificationResult().getTsRead().split("T")
						: new String[] { StringUtils.EMPTY, StringUtils.EMPTY };

				try {
					response.setTransactionDate(StringUtils.isNotEmpty(fastagTxnDateTime[0]) ? fastagTxnDateTime[0]
							: txnAndReconDateformat.format(new Date(Long.parseLong(entry.getValue().getRequestTime())))
									.split(StringUtils.SPACE)[0]);
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("exception while converting epoch to date")
							.data("TxnRequestTime", tollReqPayRequest.getMerchant().getReaderVerificationResult()
									.getTsRead().split("T"))
							.format());
					response.setTransactionDate(StringUtils.EMPTY);
				}

				try {
					response.setTransactionTime(StringUtils.isNotEmpty(fastagTxnDateTime[1]) ? fastagTxnDateTime[1]
							: txnAndReconDateformat.format(new Date(Long.parseLong(entry.getValue().getRequestTime())))
									.split(StringUtils.SPACE)[1]);
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("exception while converting epoch to date")
							.data("TxnRequestTime", tollReqPayRequest.getMerchant().getReaderVerificationResult()
									.getTsRead().split("T"))
							.format());
					response.setTransactionTime(StringUtils.EMPTY);
				}

				try {
					response.setTransactionId(
							entry.getValue().getPartnerTransactionReferenceId().contains("-")
									? entry.getValue().getPartnerTransactionReferenceId().split("-")[0]
											+ StringUtils.SPACE + "Debit Adjustment"
									: entry.getValue().getPartnerTransactionReferenceId());
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("exception while inserting in pdf")
							.data("TxnId", entry.getValue().getPartnerTransactionReferenceId()).format());
					response.setTransactionId(StringUtils.EMPTY);
				}

				try {
					response.setOstaTransactionReferenceId(entry.getValue().getDipcoinTransactionRefId());
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("exception while inserting in pdf")
							.data("TxnId", entry.getValue().getPartnerTransactionReferenceId()).format());
					response.setOstaTransactionReferenceId(StringUtils.EMPTY);
				}

				// Splitting DipcoinTransactionUsageDetails
				try {

					if (tollReqPayRequest != null) {
						response.setTollPlazaName(tollReqPayRequest.getMerchant().getName());

					} else {
						String[] usageDetails = entry.getValue().getUsageDetails().split(",");

						if (usageDetails.length >= 2) {
							response.setTollPlazaName(usageDetails[usageDetails.length - 2].trim());
						} else {
							response.setTollPlazaName(entry.getValue().getUsageDetails());
						}
					}

				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("exception while adding plaza name")
							.data("usagedetails", entry.getValue().getUsageDetails()).format());
					response.setTollPlazaName(StringUtils.EMPTY);
				}

				try {
					response.setTollAmount(String.valueOf(entry.getValue().getAmount()));
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("exception").format());
					response.setTollAmount(String.valueOf(BigDecimal.ZERO));
				}

				try {

					if (tollReqPayRequest != null) {
						response.setLaneDirection(tollReqPayRequest.getMerchant().getLane().getDirection());
					} else {
						response.setLaneDirection(entry.getValue().getUsageDetails()
								.substring(entry.getValue().getUsageDetails().trim().length() - 1));
					}
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("exception while adding lane direction")
							.data("usagedetails", entry.getValue().getUsageDetails()).format());
					response.setLaneDirection(StringUtils.EMPTY);
				}

				try {
					response.setReconDate(btxLookUp.get(entry.getKey()) != null ? txnAndReconDateformat
							.format(new Date(Long.parseLong(btxLookUp.get(entry.getKey()).getRequestTime())))
							.split(StringUtils.SPACE)[0] : StringUtils.EMPTY);
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("exception")
							.data("bTx.getRequestTime", btxLookUp.get(entry.getKey()).getRequestTime()).format());
					response.setReconDate(StringUtils.EMPTY);
				}

				try {
					response.setReconTime(btxLookUp.get(entry.getKey()) != null ? txnAndReconDateformat
							.format(new Date(Long.parseLong(btxLookUp.get(entry.getKey()).getRequestTime())))
							.split(StringUtils.SPACE)[1] : StringUtils.EMPTY);
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("exception")
							.data("bTx.getRequestTime", btxLookUp.get(entry.getKey()).getRequestTime()).format());
					response.setReconTime(StringUtils.EMPTY);
				}

			} catch (Exception e) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("exception caught").format(),
						e);
			}
			transactionsResponse.add(response);
		}
		return transactionsResponse;
	}




	
	public ResponseEntity filterTollTags(final User user,Bank bank,Integer bankId, final String phone, final String branchCode,
			final String status, Long startTime, Long endTime, Integer start, Integer count)throws Exception, APIException 
	{
		TollRegistrationResponse response = new TollRegistrationResponse();
		List<APIResponse> listresponses = new LinkedList<APIResponse>();
		List<TollTag> listOfTollTag = new ArrayList<>();

		if (bankId != null && phone != null) {
			listOfTollTag = this.tollDBService
					.getTollTagByBankIdAndTollRegistrationCreatedByAndStatusOrderByIdDesc(bankId, user.getId(), status);
		} else if (bankId != null && branchCode != null) {
			// branchCode is Dob in TollRegistration Table

			BankBranchDetails bankBranchDetail = this.bankBranchDetailsService
					.findBankBranchDetailsByBranchCode(branchCode);
			if (bankBranchDetail == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.INVALID_BRANCH_CODE));
			}
			List<TollRegistration> tollRegistration = this.tollDBService.getBybranchCode(branchCode);
			List<TollRegistration> tollRegistrationIds = new ArrayList<>();

			for (TollRegistration registration : tollRegistration) {
				tollRegistrationIds.add(registration);
			}

			listOfTollTag = this.tollDBService.getByBankIdAndTollRegistrationAndStatus(bankId, tollRegistrationIds,
					status);

		}
//		listOfTollTag = this.tollDBService.getByBankIdAndTollRegistrationDobAndStatus(bankId, branchCode, status);
		if (!CollectionUtils.isEmpty(listOfTollTag)) {
			for (TollTag tollTag : listOfTollTag) {

				TollRegistrationResponse responseObj = new TollRegistrationResponse();

				populateTollCustomersResponse(user, tollTag.getTollRegistration(), Arrays.asList(tollTag), responseObj);

				// Add Bank Name
				responseObj.setBankName(bank.getName());

				listresponses.add(responseObj);
			}

			// set pagination info
			Pagination pagination = new Pagination();
			pagination.setStartRange(startTime);
			pagination.setEndRange(endTime);
			pagination.setScanCompleted(listresponses.size() < count);
			pagination.setTotal(listresponses.size());
			if (!pagination.getScanCompleted()) {
				pagination.setStart(start + count);
			}

			Map<String, Object> tagResponses = new HashMap<>();

			tagResponses.put("tags", listresponses);
			tagResponses.put("pagination", pagination);

			if (!tagResponses.isEmpty()) {
				return ResponseEntity.status(HttpStatus.OK).body(tagResponses);
			}
		}
		response.addHeaderCode(HeaderCode.DATA_NOT_FOUND);
		return ResponseEntity.status(HttpStatus.OK).body(response);
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

				try {
					// Get the Auto-top field in the response and
					if (tolltag.getCustomerAccountId() != 0) {
						CustomerAccount customerAccount = this.customerDBService
								.getAccountById(tolltag.getCustomerAccountId());
						if (customerAccount != null) {
							TopUpDetails topUpDetails = null;
							if (StringUtils.isNoneBlank(customerAccount.getTopUpDetails())) {
								topUpDetails = objectMapper.readValue(customerAccount.getTopUpDetails(),
										TopUpDetails.class);

								if (topUpDetails != null) {

									tollTagresponse.setAutoTopUpAmount(topUpDetails.getAutoTopUpAmount());
									tollTagresponse.setThresHoldAmount(topUpDetails.getAutoTopUpThresHold());
									tollTagresponse.setAutoTopUpflag(topUpDetails.getAutoTopUp());
								}

							}
						}
					}
				} catch (Exception e) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Error in adding response date of top-up details.").format());
				}

				// populate the Toll Tag Details.

				Dipcoin dipcoin = dipcoinDBService.findDipcoin(tolltag.getCustomerAccountId(),
						DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());
				Bank bank = bankDBService
						.getBank(tolltag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? tolltag.getWalletBankId()
								: tolltag.getBankId());
				List<User> makerUser = userDBService
						.getUsersByIds(Arrays.asList(tolltag.getTollRegistration().getCreatedBy()));
				List<User> checkerUser = userDBService.getUsersByIds(Arrays.asList(tolltag.getApprovedBy()));
				User maker = null;
				User checker = null;
				if (CollectionUtils.isNotEmpty(makerUser)) {
					maker = makerUser.get(NumberUtils.INTEGER_ZERO);
				}

				if (CollectionUtils.isNotEmpty(checkerUser)) {
					checker = checkerUser.get(NumberUtils.INTEGER_ZERO);
				}

				populateTollTagDetailsResponse(user, tolltag, tollTagresponse, dipcoin, maker, checker, bank);

				// attaching tagId to serial Number
				Epc epc = tollDBService.findEpcByTagId(tolltag.getTagId());
				if (epc != null) {
					tollTagresponse.setTagId(epc.getRfidTag());
					tollTagresponse.setSerialNumber(epc.getSerialNumber());
				}
				tollTagresponselist.add(tollTagresponse);
			}
			User tollUser = userDBService.getUsersByIds(Arrays.asList(registration.getUserId())).get(0);
			response.setId(registration.getId()).setFirstName(registration.getFirstName())
					.setLastName(registration.getLastName()).setEmailId(registration.getEmailId())
					.setDob(registration.getDob()).setAddress(registration.getAddress())
					.setMobileNo(registration.getMobileNo()).setCity(registration.getCity())
					.setState(registration.getState()).setPincode(registration.getPincode())
					.setUserId(registration.getUserId()).setIdProofNo(registration.getIdProofNumber())
					.setIdProofType(registration.getIdProofType()).setGender(registration.getGender())
					.setIdProofPath(registration.getIdProofImage())
					.setIsEverLoggedIn(tollUser.getPin() == null ? NumberUtils.INTEGER_ZERO : NumberUtils.INTEGER_ONE)
					.setVehicleList(tollTagresponselist);

		}
	}

	/*
	 * Setting the Response for TollTagDetails
	 */
	public static void populateTollTagDetailsResponse(final User user, final TollTag tollTag, TollTagResponse response,
			Dipcoin dipcoin, User maker, User checker, Bank bank) throws APIException {

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
			response.setType(tollTag.getType());
			response.setVendorIPAddress(tollTag.getVendorIPAddress());
			response.setUpdateExceptionErrorCode(tollTag.getUpdateExceptionErrorCode());
			response.setVendorUpdateBy(tollTag.getVendorUpdateBy());
			response.setUpdateExceptionResponse(tollTag.getUpdateExceptionResponse());
			response.setVendorUpdateDateTime(tollTag.getVendorUpdateDateTime());
			response.setAccountNumber(
					StringUtils.isBlank(tollTag.getAccountNumber()) ? null : tollTag.getAccountNumber());
			response.setTollRegistration(response.settingTollTagResponse(tollTag.getTollRegistration()));
			response.setOstaAmount(dipcoin != null ? dipcoin.getAmount() : BigDecimal.ZERO);
			response.setRemarks(tollTag.getRemarks());
			response.setCourierCompany(tollTag.getCourierCompany());
			response.setTrackingId(tollTag.getTrackingId());
			response.setMiscCharges(tollTag.getMiscCharges());
			response.setWalletFlag(tollTag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? NumberUtils.INTEGER_ONE
					: NumberUtils.INTEGER_ZERO);
			response.setIssuerBankName(bank.getName());
			response.setVinVrnFlag(tollTag.getVinVrnFlag());
			if (StringUtils.isNotBlank(tollTag.getTopUpDetails())) {

				try {
					TopUpDetails topUpDetails = objectMapper.readValue(tollTag.getTopUpDetails(), TopUpDetails.class);
					response.setAutoTopUpAmount(topUpDetails.getAutoTopUpAmount());
					response.setThresHoldAmount(topUpDetails.getAutoTopUpThresHold());
					response.setAutoTopUpflag(topUpDetails.getAutoTopUp());
				} catch (Exception e) {
					LOG.error(LogFormatter.instance().message("Exception while parsing").format(), e);
				}
			}

			if (maker != null && DBConstants.UserRoles.CUSTOMER.value().equalsIgnoreCase(maker.getRole())) {
				response.setMaker(maker.getFirstName() + StringUtils.SPACE + maker.getLastName() + StringUtils.SPACE
						+ "(" + APIConstants.CUSTOMER + ")");
			} else if (maker != null && UserRoles.bankUserRoles().contains(user.getRole())) {
				response.setMaker(
						maker != null
								? maker.getFirstName() + StringUtils.SPACE + maker.getLastName() + StringUtils.SPACE
										+ "(" + APIConstants.BANK + ")"
								: StringUtils.EMPTY);
			} else {
				response.setMaker(
						maker != null
								? maker.getFirstName() + StringUtils.SPACE + maker.getLastName() + StringUtils.SPACE
										+ "(" + APIConstants.MERCHANT + ")"
								: StringUtils.EMPTY);
			}
			response.setChecker(checker != null ? checker.getFirstName() + StringUtils.SPACE + checker.getLastName()
					: StringUtils.EMPTY);

		}
	}

	public TollTagUpdateResponse asyncUploadTheTollListToNETCManageTag(final HttpServletContext httpServletContext,
			List<TollTag> tollTagRequests, String operation, Bank bank, Merchant merchant,
			TagNPCIApprovalStatus tagNPCIApprovalStatus) throws APIException, Exception {

		// For the time being return true response.

		TollTagUpdateRequest tollTagUpdateRequest = new TollTagUpdateRequest();
		String refUrl = StringUtils.EMPTY;
		TollTagUpdateResponse tollTagUpdateResponse = new TollTagUpdateResponse();

		TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();
		if (tollProperties.isConnectNpci()) {
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
		// head.setVer(TollConstant.VER);
		head.setVer(TollConstant.VER);
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

			if (operation.equalsIgnoreCase(TollConstant.ADD_OP)) {

				detailList.add(new Detail(TollConstant.TID, tollTagRequest.getTid()));

				detailList.add(new Detail(TollConstant.ISSUEDATE, issueDateformatter.format(date)));

				detailList.add(new Detail(TollConstant.EXCCODE, tollTagRequest.getExcCode()));

				// Registered or unregistered vehicle at RTO should be T|F

				// ******** Commented By Murad, If RegNum is not empty in NETC Request then set
				// T (told by Sanskruti)
				// detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE,
				// tollTagRequest.getRegisteredVehicle()));
				// ********

				// detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE,
				// "T"/*tollTagRequest.getRegisteredVehicle()*/));

				detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE, "T"));

				// state should be short code
				detailList.add(new Detail(TollConstant.STATE, tollTagRequest.getState()));
				// detailList.add(new Detail(TollConstant.STATE,
				// "MH"/*tollTagRequest.getState()*/));

				// should be NEW_TAG|REPLACEMENT_TAG
				detailList.add(new Detail(TollConstant.TAGDESCRIPTOR, tollTagRequest.getTagDescriptor()));
				// detailList.add(new Detail(TollConstant.TAGDESCRIPTOR,
				// "REPLACEMENT_TAG"/*tollTagRequest.getTagDescriptor()*/));

			}

			detailList.add(new Detail(TollConstant.VEHICLECLASS, mappedVehicleClass));

			/*
			 * detail = new Detail(); detail.setName(TollConstant.REGNUMBER);
			 * detail.setValue(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() ==
			 * tollTagRequest.getVinVrnFlag() ? tollTagRequest.getRegistrationNo() :
			 * (tollTagRequest.getVinNumber().length() <= 20 ? tollTagRequest.getVinNumber()
			 * : tollTagRequest.getVinNumber().substring(0, 19))); detailList.add(detail);
			 */

			detailList.add(new Detail(TollConstant.REGNUMBER,
					VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
							? tollTagRequest.getRegistrationNo()
							: (tollTagRequest.getVinNumber().length() <= 20 ? tollTagRequest.getVinNumber()
									: tollTagRequest.getVinNumber().substring(0, 20))));

			// for managetag 1.1 registrationNo is optional and vin is mandatory that is why
			// added this check

			// commented by Murad -> if we don't have reg no then pass starting 20 digit of
			// VIN (Sanskruti Mane)
//	          if(/*tollTagRequest.getVinNumber().isEmpty()*/ tollTagRequest.getVinVrnFlag() != DBConstants.VinVrn.VIN_CHACIS.value()) {
//	              detailList.add(new Detail(TollConstant.REGNUMBER,VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
//	                        ? tollTagRequest.getRegistrationNo()
//	                        : (tollTagRequest.getVinNumber().length() <= 10 ? tollTagRequest.getVinNumber()
//	                                : tollTagRequest.getVinNumber().substring(0, 10))));                  
//	          }else {
//	              detailList.add(new Detail(TollConstant.REGNUMBER, ""));
//	          }

			detailList.add(new Detail(TollConstant.VIN, tollTagRequest.getVinNumber()));
			// detailList.add(new Detail(TollConstant.VIN,
			// "5KL8M8ED6B0108371"/*tollTagRequest.getVinNumber()*/));

			detailList.add(new Detail(TollConstant.ENGINENO, tollTagRequest.getEngineNo()));
			// detailList.add(new Detail(TollConstant.ENGINENO,
			// "JH4G589HGB54842158"/*tollTagRequest.getEngineNo()*/));

			// PETROL|DIESEL|ELECTRIC|HYBRID|CNG
			detailList.add(new Detail(TollConstant.VEHICLEDESCRIPTOR, tollTagRequest.getVehicleDescriptor()));
			// detailList.add(new Detail(TollConstant.VEHICLEDESCRIPTOR,
			// reqVehicleDetailResponse.getVEHICLEDESCRIPTOR()));

			// T|F
			detailList.add(new Detail(TollConstant.NATIONALPERMIT, tollTagRequest.getNationalPermit()));
			// detailList.add(new Detail(TollConstant.NATIONALPERMIT,
			// "T"/*tollTagRequest.getNationalPermit()*/));

			// if national permit is false then there will be no permit expiry date
			if (tollTagRequest.getPermitExpiryDate() != null && !tollTagRequest.getPermitExpiryDate().isEmpty()) {
				detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE, tollTagRequest.getPermitExpiryDate()));
			} else {
				detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE, ""));
			}

			// detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE,
			// "15-12-2028"/*String.valueOf(tollTagRequest.getPermitExpiryDate())*/));

			detailList.add(new Detail(TollConstant.COMVEHICLE, tollTagRequest.getIsCommercial()));

			tag.setDetail(detailList);
			tag.setOp(operation);
			tag.setTagId(tollTagRequest.getTagId());
			tag.setSeqNum(String.valueOf(tollTagRequests.indexOf(tollTagRequest) + NumberUtils.INTEGER_ONE));
//	          tag.setType(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
//	                  ? (operation.equalsIgnoreCase(TollConstant.ADD_OP) ? operation : TollConstant.UPDATE_OP)
//	                  : tollTagRequest.getVinNumber());
			tag.setType("");

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

		// added to handle failure scenario
		if (tagNPCIApprovalStatus != null) {

			tagNPCIApprovalStatus.setTxnId(dipcoinReferenceNumber);
			tagNPCIApprovalStatus.setRawRequest(postData);

			if (tagNPCIApprovalStatusDBService.addTagNPCIApprovalStatus(tagNPCIApprovalStatus) == null) {
				return null;
			}

		}

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

		ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices
				.signatureGenerationServices(byteArrayInputStream, httpServletContext.getTraceId(), bank.getOrgId());
		String responseData = null;

		// NPCI Active Active Setup Phase2 changes
		String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
		if (StringUtils.isEmpty(ipAddress)) {

//	              tollTagUpdateResponse.addHeaderCode(HeaderCode.NETC_NPCI_SERVER_DOWN);
//	              return tollTagUpdateResponse;

			return null;

		}
		int port = this.tollProperties.getNetcHealthCheckPort();
		String endPoint = this.tollProperties.getAsyncManageTagsUrl();
		String url = "https://" + ipAddress + ":" + port + endPoint;

		try {
			TollHttpsServices.bankIin = bank.getIin();

			/*
			 * responseData = tollHttpsServices.send(tollProperties.getManageTagsUrl(),
			 * httpServletContext.getTraceId(), byteArrayOutputStream);
			 */

			tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);

			TollHttpsServices.bankIin = StringUtils.EMPTY;

		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught").format(), e);
		}

		/*
		 * TollTagUpdateResponse tollTagUpdateResponse = JAXB.unmarshal(new
		 * StringReader(responseData), TollTagUpdateResponse.class);
		 */

		return tollTagUpdateResponse;

	}

	/**
	 * Getting the NETC APPROVAL Function
	 * 
	 * @throws Exception
	 */
	public TollTagUpdateResponse uploadTheTollListToNETC(final HttpServletContext httpServletContext,
			List<TollTag> tollTagRequests, String operation, Bank bank, Merchant merchant)
			throws APIException, Exception {

		// For the time being return true response.

		TollTagUpdateRequest tollTagUpdateRequest = new TollTagUpdateRequest();
		String refUrl = StringUtils.EMPTY;
		TollTagUpdateResponse tollTagUpdateResponse = new TollTagUpdateResponse();

		TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();
		if (tollProperties.isConnectNpci()) {
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

			if (operation.equalsIgnoreCase(TollConstant.ADD_OP)) {

				detailList.add(new Detail(TollConstant.TID, tollTagRequest.getTid()));

				detailList.add(new Detail(TollConstant.ISSUEDATE, issueDateformatter.format(date)));

				detailList.add(new Detail(TollConstant.EXCCODE, tollTagRequest.getExcCode()));

				// Registered or unregistered vehicle at RTO should be T|F
				// detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE,
				// reqVehicleDetailResponse.getREGISTERED_VEHICLE()));
				// detailList.add(new Detail(TollConstant.REGISTERED_VEHICLE,
				// "T"/*tollTagRequest.getRegisteredVehicle()*/));

				// state should be short code
				// detailList.add(new Detail(TollConstant.STATE,
				// reqVehicleDetailResponse.getSTATE()));
				// detailList.add(new Detail(TollConstant.STATE,
				// "MH"/*tollTagRequest.getState()*/));

				// should be NEW_TAG|REPLACEMENT_TAG
				// detailList.add(new Detail(TollConstant.TAGDESCRIPTOR,
				// "NEW_TAG"/*tollTagRequest.getTagDescriptor()*/));
				// detailList.add(new Detail(TollConstant.TAGDESCRIPTOR,
				// "REPLACEMENT_TAG"/*tollTagRequest.getTagDescriptor()*/));

			}

			detailList.add(new Detail(TollConstant.VEHICLECLASS, mappedVehicleClass));

			/*
			 * detail = new Detail(); detail.setName(TollConstant.REGNUMBER);
			 * detail.setValue(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() ==
			 * tollTagRequest.getVinVrnFlag() ? tollTagRequest.getRegistrationNo() :
			 * (tollTagRequest.getVinNumber().length() <= 20 ? tollTagRequest.getVinNumber()
			 * : tollTagRequest.getVinNumber().substring(0, 19))); detailList.add(detail);
			 */

			detailList.add(new Detail(TollConstant.REGNUMBER,
					VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
							? tollTagRequest.getRegistrationNo()
							: (tollTagRequest.getVinNumber().length() <= 10 ? tollTagRequest.getVinNumber()
									: tollTagRequest.getVinNumber().substring(0, 10))));

			// detailList.add(new Detail(TollConstant.VIN,
			// reqVehicleDetailResponse.getVIN()));
			// detailList.add(new Detail(TollConstant.VIN,
			// "5KL8M8ED6B0108371"/*tollTagRequest.getVinNumber()*/));

			// detailList.add(new Detail(TollConstant.ENGINENO,
			// reqVehicleDetailResponse.getENGINENO()));
			// detailList.add(new Detail(TollConstant.ENGINENO,
			// "JH4G589HGB54842158"/*tollTagRequest.getEngineNo()*/));

			// PETROL|DIESEL|ELECTRIC|HYBRID|CNG
			// detailList.add(new Detail(TollConstant.VEHICLEDESCRIPTOR,
			// "PETROL"/*tollTagRequest.getVehicleDescriptor()*/));
			// detailList.add(new Detail(TollConstant.VEHICLEDESCRIPTOR,
			// reqVehicleDetailResponse.getVEHICLEDESCRIPTOR()));

			// T|F
			// detailList.add(new Detail(TollConstant.NATIONALPERMIT,
			// reqVehicleDetailResponse.getNATIONAL_PERMIT()));
			// detailList.add(new Detail(TollConstant.NATIONALPERMIT,
			// "T"/*tollTagRequest.getNationalPermit()*/));

			// detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE,
			// reqVehicleDetailResponse.getNATIONAL_PERMIT_END_DATE()));
			// detailList.add(new Detail(TollConstant.PERMITEXPIRYDATE,
			// "15-12-2028"/*String.valueOf(tollTagRequest.getPermitExpiryDate())*/));

			detailList.add(new Detail(TollConstant.COMVEHICLE, tollTagRequest.getIsCommercial()));

			tag.setDetail(detailList);
			tag.setOp(operation);
			tag.setTagId(tollTagRequest.getTagId());
			tag.setSeqNum(String.valueOf(tollTagRequests.indexOf(tollTagRequest) + NumberUtils.INTEGER_ONE));
			tag.setType(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
					? (operation.equalsIgnoreCase(TollConstant.ADD_OP) ? operation : TollConstant.UPDATE_OP)
					: tollTagRequest.getVinNumber());

//				tag.setType(VinVrn.VEHICLE_REGISTRATION_NUMBER.value() == tollTagRequest.getVinVrnFlag()
//						? (operation.equalsIgnoreCase(TollConstant.ADD_OP) ? operation : TollConstant.UPDATE_OP)
//						: tollTagRequest.getVinNumber());
			tag.setType(tollTagRequest.getRegistrationNo());

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

		// NPCI Active Active Setup Phase2 changes
		String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
		if (StringUtils.isEmpty(ipAddress)) {

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

			responseData = tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);

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
		tollTagUpdateResponse = JAXB.unmarshal(new StringReader(responseData), TollTagUpdateResponse.class);

		return tollTagUpdateResponse;

	}
	
	/*
	 * Updating Toll Customer by Bank.
	 */

	public ResponseEntity updateTollCustomerByBank(final User user, final Bank bank, final TollTagRequest updateReq,
			final String clientTransactionId) throws Exception, APIException {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", updateReq).format());

		String originIp = httpServletContext.getOriginIp();

		TollTagResponse response = new TollTagResponse();
		response.setClientTransactionId(clientTransactionId);

		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		// checking for the request null
		if (updateReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		if (bank == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		if (!userDBService.isBankSuperAdmin(user) && !userDBService.isBankTransactor(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}
		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (!this.bankDBService.isActive(bank)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.BANK_NOT_ACTIVE));
		}

		if (StringUtils.isBlank(updateReq.getApprovalFlag())) {
			response.addHeaderCode(HeaderCode.TOLL_USER_DETAILS_CANNOT_UPDATE);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		TollTag tollTag = this.tollDBService.findTollTagById(updateReq.getId());

		if (tollTag == null) {
			response.addHeaderCode(HeaderCode.TOLL_TAG_DOESNT_EXIST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		TollRegistration tollRegistration = tollTag.getTollRegistration();

		if (BankTagStatus.APPROVE.equals(Integer.parseInt(updateReq.getApprovalFlag()))) {

			if (updateReq.getRemarks() != null && StringUtils.isNoneBlank(updateReq.getRemarks())) {
				tollTag.setRemarks(updateReq.getRemarks());
			}
			tollTag.setRejectReason(null);
			tollTag.setApprovalFlag(String.valueOf(BankTagStatus.APPROVE.value()));

			// Tag Vendor has Approve then it goes to NETC For Approval.
			tollTag.setStatus(String.valueOf(TollTagApprovalStatus.TAGID_PENDING.value()));
		}

		if (BankTagStatus.REJECTED.equals(Integer.parseInt(updateReq.getApprovalFlag()))) {
			
			List<Dipcoin> dcoins = dipcoinDBService.asyncFindDipcoin(tollTag.getCustomerAccountId(),
					Arrays.asList(DBConstants.DipcoinUsageType.DEPOSIT.value(),
							DBConstants.DipcoinUsageType.FEE.value(), DBConstants.DipcoinUsageType.TOLL.value()))
					.get();
			
			
			if (CollectionUtils.isEmpty(dcoins)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("deposit and fees amount not available")
						.data("Customer Account Id", tollTag.getCustomerAccountId()).format());

				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TOLL_DEPOSIT_AND_FEE_AMOUNT_NOT_AVAILABLE));
			}

			List<Integer> userId = new ArrayList<Integer>();
			userId.add(tollTag.getTollRegistration().getUserId());
			List<User> dcoinUser = userDBService.asyncGetUsersByIds(userId).get();

			ResponseEntity deletedcoinResponse = null;
			BigDecimal initialAmount = null;
			CustomerAccount customerAccount = null;

			for (Dipcoin dcoin : dcoins) {
				initialAmount = null;
				if (StringUtils.isNotBlank(dcoin.getUsageCategory())
						&& dcoin.getUsageCategory().equalsIgnoreCase(tollTag.getRegistrationNo())
						&& DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()) {

					if (dcoin.getUsageType() == DBConstants.DipcoinUsageType.TOLL.value()) {

						initialAmount = dcoin.getAmount();
						if (!Hibernate.isInitialized(dcoin.getCustomerAccount())) {
							Hibernate.initialize(dcoin.getCustomerAccount());
						}
						customerAccount = dcoin.getCustomerAccount();
					}
					deletedcoinResponse = customerDipcoinResource.deleteDipcoin(dcoinUser.get(0),
							dcoinUser.get(0).getPhone().concat(dcoin.getCoin()), false);
					if (HttpStatus.BAD_REQUEST.value() <= deletedcoinResponse.getStatusCodeValue()) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("deposit and fees not reverted").format());

						throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
								APIResponse.error(HeaderCode.INTERNAL_ERROR));
					}
				}
				if (initialAmount == null && DBConstants.DipcoinStatus.ACTIVE.value() == dcoin.getStatus()
						&& dcoin.getUsageType() == DBConstants.DipcoinUsageType.TOLL.value()) {
					initialAmount = dcoin.getAmount();
					if (!Hibernate.isInitialized(dcoin.getCustomerAccount())) {
						Hibernate.initialize(dcoin.getCustomerAccount());
					}
					customerAccount = dcoin.getCustomerAccount();
					deletedcoinResponse = customerDipcoinResource.deleteDipcoin(dcoinUser.get(0),
							dcoinUser.get(0).getPhone().concat(dcoin.getCoin()), false);

					if (HttpStatus.BAD_REQUEST.value() <= deletedcoinResponse.getStatusCodeValue()) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("minimum not reverted")
								.format());

						throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
								APIResponse.error(HeaderCode.INTERNAL_ERROR));
					}

				}

			}

			if (null == deletedcoinResponse) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("deletedcoinResponse is null").format());
				
				List<Dipcoin> feeDipcoin = dipcoinDBService.asyncFindDipcoin(tollTag.getCustomerAccountId(),
						Arrays.asList(DBConstants.DipcoinUsageType.FEE.value()))
						.get();
				
					boolean foundCancelDipcoin =false;
					for(Dipcoin dcoin:feeDipcoin) {
						if(DBConstants.DipcoinStatus.CANCELLED.value() == dcoin.getStatus() 
								&& tollTag.getRegistrationNo().equalsIgnoreCase(dcoin.getUsageCategory())) {
							foundCancelDipcoin=true;
						}
					}
					if(!foundCancelDipcoin) {
						throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.INTERNAL_ERROR));
					}
			}

			List<TollTag> tollTags = tollDBService
					.findTollTagsByCustomerAccountId(Arrays.asList(tollTag.getCustomerAccountId()));

			boolean tollTagEnabled = false;
			for (TollTag tolltag : tollTags) {
				if (tollTag.getId() != tolltag.getId()
						&& (DBConstants.TollTagApprovalStatus.ACTIVE.value() == Integer.valueOf(tolltag.getStatus())
								|| DBConstants.TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer
										.valueOf(tolltag.getStatus())
								|| DBConstants.TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING.value() == Integer
										.valueOf(tolltag.getStatus()))) {
					tollTagEnabled = true;

				}
			}

			if (tollTagEnabled && initialAmount != null && customerAccount != null
					&& initialAmount.compareTo(tollTag.getAvailableAmount()) > NumberUtils.INTEGER_ZERO) {
				CustomerDipcoinRequest createReq = new CustomerDipcoinRequest();
				if (!Hibernate.isInitialized(customerAccount.getUser())) {
					Hibernate.initialize(customerAccount.getUser());
				}
				// Adding Request To create Osta.

				if (tollTag.getTollRegistration().getUserId() != tollTag.getTollRegistration().getCreatedBy()
						&& StringUtils.isNotBlank(tollTag.getTopUpDetails())) {

					try {
						TopUpDetails topUpDetails = objectMapper.readValue(tollTag.getTopUpDetails(),
								TopUpDetails.class);
						if (tollTag.getAvailableAmount().add(topUpDetails.getAutoTopUpAmount())
								.compareTo(initialAmount) < NumberUtils.INTEGER_ZERO) {
							createReq.setAmount(initialAmount
									.subtract(tollTag.getAvailableAmount().add(topUpDetails.getAutoTopUpAmount())));
						} else {
							createReq.setAmount(BigDecimal.ZERO);
						}

					} catch (Exception e) {
						LOG.debug(
								LogFormatter.instance(httpServletContext.getTraceId())
										.message("exception caught so so setting lien mark amount to zero").format(),
								e);
						createReq.setAmount(BigDecimal.ZERO);
					}

				} else {
					createReq.setAmount(initialAmount.subtract(tollTag.getAvailableAmount()));
				}

				createReq.setCardId(customerAccount.getUserCardId());
				createReq.setCurrency(TollConstant.INR_CURRENCY);
				createReq.setAuthorizationPin(APIConstants.PIN);
				createReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
				createReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);
				createReq.setFetchDipcoin(true);
				createReq.setEncryptDipcoin(false);

				ResponseEntity createDipcoinresponse = customerDipcoinResource.createDipcoin(customerAccount.getUser(),
						createReq, false);

			}
			
			
			
		}

		if (updateReq.getRejectReason() != null && StringUtils.isNotEmpty(updateReq.getRejectReason())) {
			tollTag.setRejectReason(updateReq.getRejectReason());
		}

		if (updateReq.getRemarks() != null && StringUtils.isNotEmpty(updateReq.getRemarks())) {
			tollTag.setRemarks(updateReq.getRemarks());
		}

		tollTag.setApprovalFlag(String.valueOf(BankTagStatus.REJECTED.value()));
		tollTag.setStatus(String.valueOf(TollTagApprovalStatus.BANK_REJECTED.value()));

		tollTag.setTollRegistration(tollRegistration);
		tollTag.setApprovedBy(user.getId());
		tollTag.setApprovedIPAddress(originIp);
		tollTag.setApprovedDateTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));

		//Epc epc = null;
		if (tollTag.getTagId() != null || tollTag.getTid() != null
				|| tollTag.getSerialNumber() != null) {
			
		   Epc epc = this.tollDBService.findEpcByTagId(tollTag.getTagId());
			if (epc == null) {

				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TAGID_IS_NOT_IN_CORRECT_FORMAT_OR_EMPTY));
			}
		
			if (epc != null && epc.getStatus() == DBConstants.EpcStatus.USED.value()) {
				epc.setStatus(DBConstants.EpcStatus.NOTUSED.value());

				epc = this.tollDBService.saveEpc(epc);
				if (epc == null) {
					LOG.error(
							LogFormatter.instance(httpServletContext.getTraceId()).message("Epc Not Updated").format());
				}
			}

				tollTag.setTagId(null);
				tollTag.setTid(null);
				tollTag.setSerialNumber(null);
		}
		
		
		// update the TollTag details.
		TollTag tollTagResponse = this.tollDBService.updateTollTag(tollTag);

		if (tollTagResponse == null) {
			response.addHeaderCode(HeaderCode.TOLL_USER_DETAILS_CANNOT_UPDATE);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get bank Account")
				.data("bankId", tollTag.getWalletBankId()> NumberUtils.INTEGER_ZERO ? tollTag.getWalletBankId() : tollTag.getBankId()).format());

		try {
			if (BankTagStatus.REJECTED.equals(Integer.parseInt(updateReq.getApprovalFlag()))) {
				// success mail
				if (!tollEmailUtils.sendTollCustomerBankRejectionEmail(originIp, tollTag, bank, tollRegistration)) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to send email to user").data("user email", tollRegistration.getEmailId())
							.format());
				}
				

				// success message
				if (!applicationProperties.getAwsSMSClient()
						&& !smsClient.sendSms(tollRegistration.getMobileNo(), Templates.TollCustomerBankRejection
								.format(tollTag.getRegistrationNo(), bank.getAlias(), tollTag.getRejectReason()),
								true)) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", tollRegistration.getMobileNo()).format());
				}

				if (applicationProperties.getAwsSMSClient()) {

					NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
					notificationRequestContext.setTraceId(httpServletContext.getTraceId());
					if (!notificationResource.sendSms(tollRegistration.getMobileNo(),
							Templates.TollCustomerBankRejection.format(tollTag.getRegistrationNo(), bank.getAlias(),
									tollTag.getRejectReason()),
							httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
								.data("phone", tollRegistration.getMobileNo()).format());
					}
				}
			}

			if (BankTagStatus.APPROVE.equals(Integer.parseInt(updateReq.getApprovalFlag()))) {
				// success mail
				if (!tollEmailUtils.sendTollCutomerBankApprovalEmail(originIp, tollTag, user, bank)) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to send email to user").data("user email", tollRegistration.getEmailId())
							.format());
				}

				// success message
				if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(tollRegistration.getMobileNo(),
						Templates.TollTagBankApproval.format(tollTag.getRegistrationNo(), bank.getAlias()), true)) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", tollRegistration.getMobileNo()).format());
				}

				if (applicationProperties.getAwsSMSClient()) {

					NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
					notificationRequestContext.setTraceId(httpServletContext.getTraceId());
					if (!notificationResource.sendSms(tollRegistration.getMobileNo(),
							Templates.TollTagBankApproval.format(tollTag.getRegistrationNo(), bank.getAlias()),
							httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
								.data("phone", tollRegistration.getMobileNo()).format());

					}
				}

				// Adding the count to metrix
				tollMetricRegistry.numberOfTagsApprovedByBank().increment();

			}
		} catch (Exception e) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Exception While Sending Email To Bank vendor For Tag Id Approval").format());
		}
		response.addHeaderCode(HeaderCode.TOLL_USER_DETAILS_UPDATED);
		return ResponseEntity.status(HttpStatus.OK).body(response);

	}
	
	
	/*
	 * Recharge the Functionality with Create Osta
	 * 
	 */
	public ResponseEntity tollRechargeCreateOsta(final User user, final TollRechargeRequest rechargeReq,
			String clientTransactionId) throws Exception, APIException {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", rechargeReq).format());

		// checking for the request null
		if (rechargeReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		if (UserRoles.bankRoles().contains(user.getRole()) || UserRoles.bankUserRoles().contains(user.getRole())) {
			if (!this.userDBService.isActive(user)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.USER_NOT_ACTIVE.message()).format());
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
			}
		} else {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.USER_UNAUTHORIZED.message()).format());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		List<TollTag> tollTags = tollDBService
				.asyncFindTollTagByRegistrationNo(Arrays.asList(rechargeReq.getVehicleRegNo().toUpperCase())).get();

		if (CollectionUtils.isEmpty(tollTags)) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to fetch toll tag")
					.data("Vehicle Reg No", rechargeReq.getVehicleRegNo()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_VEHICLE_REG_NO_NOT_PRESENT));
		}

		CustomerAccount customerAccount = null;
		List<User> users = null;
		User userCustomer = null;

		boolean active = false;
		TollTag tolltag = null;
		for (TollTag tollTag : tollTags) {
			if (DBConstants.TollTagApprovalStatus.ACTIVE.value() == Integer.parseInt(tollTag.getStatus())) {
				tolltag = tollTag;
				customerAccount = customerDBService.getAccountById(tollTag.getCustomerAccountId());
				users = userDBService.getUsersByIds(Arrays.asList(tollTag.getTollRegistration().getUserId()));
				if (CollectionUtils.isNotEmpty(users)) {
					userCustomer = users.get(NumberUtils.INTEGER_ZERO);
				}
				active = true;
				break;
			}

		}

		if (!active) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_VEHICLE_CUSTOMER_ACTIVATION_PENDING));
		}

		rechargeReq.setCardId(customerAccount.getUserCardId());
		rechargeReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
		rechargeReq.setAuthorizationPin(APIConstants.PIN);
		rechargeReq.setTagId(tolltag.getTagId());

		return tollRechargeResource.tollRechargeCreateOsta(userCustomer, rechargeReq, clientTransactionId, user);
	}

	/*
	 * Get Bank Transactions for Toll
	 */
	public ResponseEntity getTollTransactions(User user, Bank bank, Long startTime, Long endTime, Integer start,
			Integer count, List<Integer> statuses, List<String> vehicleNumber, String accountNumber) throws Exception {

		if (user == null && bank == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		if (!(this.userDBService.bankRepresentative(user) || this.userDBService.brontooRepresentative(user)
				|| this.userDBService.isCustomer(user))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("startTime", startTime)
				.data("endTime", endTime).data("start", start).data("count", count).data("status", statuses)
				.data("vehicleNumber", vehicleNumber).format());

		String startDateValue = String.valueOf(formatter.format(startTime));
		String endDateValue = String.valueOf(formatter.format(endTime));
		List<TollTransactionsReport> reports = new ArrayList<>();
		Map<String, Object> vehicleInfo = new HashMap<>();

		TollTransactionsReportResponse tollTransactionsReportResponse = new TollTransactionsReportResponse();
		if (CollectionUtils.isNotEmpty(vehicleNumber)) {

			List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNo(vehicleNumber);

			if (CollectionUtils.isEmpty(tollTags)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch toll tag")
						.data("Vehicle Number", vehicleNumber).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.VEHICLE_REG_NO_NOT_IN_DB));
			}

			TollTag tollTag = tollTags.get(tollTags.size() - 1);

			List<String> orderIds = new ArrayList<>();
			for (TollTag tolltag : tollTags) {
				if (StringUtils.isNotBlank(tolltag.getTagId())) {
					orderIds.add(tolltag.getTagId());
				} else if (Integer.parseInt(tolltag.getStatus()) == TollTagApprovalStatus.ACTIVE.value()) {
					tollTag = tolltag;
				}

			}

			if (StringUtils.isEmpty(tollTag.getSerialNumber())) {
				Epc epc = tollDBService.findEpcByTagId(tollTag.getTagId());
				if (epc != null) {
					tollTag.setSerialNumber(epc.getSerialNumber());
					tollTag = tollDBService.updateTollTag(tollTag);
				}
			}

			Dipcoin dCoin = dipcoinDBService.findDipcoin(tollTag.getCustomerAccountId(),
					DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());

			vehicleInfo.put("amount", dCoin != null ? dCoin.getAmount() : BigDecimal.ZERO);
			vehicleInfo.put("serialNumber", tollTag.getSerialNumber());
			vehicleInfo.put("regNumber", tollTag.getRegistrationNo());
			vehicleInfo.put("classId", tollTag.getCategory().replaceAll("VC", StringUtils.EMPTY));
			vehicleInfo.put("bankName", bank.getName());
			vehicleInfo.put("bankImage", bankAPIServices.getBankProperties(bank.getReferenceId()).getBankLogo());
			for (VehicleCategories VehicleCategory : VehicleCategories.values()) {

				if (VehicleCategory.getId() == tollTag.getCategoryId()) {
					vehicleInfo.put("class", VehicleCategory.getVehicleInfo().replaceAll("\\s", StringUtils.EMPTY));
				}

			}
			List<Integer> customerAccountIds = customerDBService.getCustomerAccountIdsByBankID(bank.getId());
			
			if(CollectionUtils.isEmpty(customerAccountIds)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch customerAccounts")
						.format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}
			
			List<DipcoinTransaction> dTxns = dipcoinDBService.getFatagTxnRecon(orderIds, startTime, endTime,
					Arrays.asList(DipcoinTransactionType.COMPLETELY_USED.value(),
							DipcoinTransactionType.PARTIALLY_USED.value(),
							DipcoinTransactionType.DEEMED_ACCEPTED.value(),
							DipcoinTransactionType.NON_FIN.value()),
					customerAccountIds, DipcoinTransactionsStatus.SUCCESS.value(),
					TransactionSource.TOLL.value(), start, count);

			if (CollectionUtils.isEmpty(dTxns)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch toll txn")
						.data("Vehicle Number", vehicleNumber).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));

			}

			List<Integer> dipcoinIds = new ArrayList<>();
			Map<String, DipcoinTransaction> dtxLookUp = new LinkedHashMap<>();
			Map<String, BankTransaction> btxLookUp = new LinkedHashMap<>();
			Map<String, Dipcoin> dCoinLookUp = new LinkedHashMap<>();

			for (DipcoinTransaction dtx : dTxns) {
				if (dtx.getDipcoinId() != NumberUtils.INTEGER_ZERO) {
					dipcoinIds.add(dtx.getDipcoinId());
					dtxLookUp.put(String.valueOf(dtx.getDipcoinId()), dtx);
				} else {
					dtxLookUp.put(String.valueOf(dtx.getDipcoinTransactionRefId()), dtx);
				}

			}

			List<Dipcoin> dCoins = dipcoinDBService.getDipcoins(dipcoinIds, null, null);

			if (CollectionUtils.isEmpty(dCoins)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch dCoins")
						.data("Vehicle Number", vehicleNumber).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}

			for (Dipcoin dipcoin : dCoins) {
				dCoinLookUp.put(String.valueOf(dipcoin.getId()), dipcoin);
			}

			List<BankTransaction> bTxs = bankDBService.getTransactionsByDipcoinIdsAndTypes(dipcoinIds,
					Arrays.asList(BankTransactionType.DEBIT_TO_ACCOUNT.value()),
					BankTransactionsStatus.SUCCESS.value());

			if (CollectionUtils.isNotEmpty(bTxs)) {
				for (BankTransaction btx : bTxs) {
					btxLookUp.put(String.valueOf(btx.getDipcoinId()), btx);
				}
			}

			// Populate the response

			reports = populateDipcoinTransactionInfo(tollTag, dtxLookUp, btxLookUp, dCoinLookUp, vehicleNumber,
					startDateValue, endDateValue);

			dtxLookUp.clear();
			btxLookUp.clear();
			dCoinLookUp.clear();
		} 
		else if (!StringUtils.isEmpty(accountNumber)) {

			List<TollTag> tollTags = tollDBService.findTollTagsByAccountNumber(accountNumber);

			if (CollectionUtils.isEmpty(tollTags)) {

				List<CustomerAccount> customerAccounts = this.customerDBService
						.findCustomerAccountByBankUID(accountNumber);
				if (!CollectionUtils.isEmpty(customerAccounts)) {
					List<Integer> customerAccountIds = new ArrayList<>();
					for (CustomerAccount customerAccount : customerAccounts) {
						customerAccountIds.add(customerAccount.getId());
					}
					tollTags = this.tollDBService.findTollTagsByCustomerAccountId(customerAccountIds);
					if (CollectionUtils.isEmpty(tollTags)) {
						LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Failed fetch toll tag").data("Account Number", accountNumber).format());
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body(APIResponse.error(HeaderCode.ACCOUNT_NOT_PRESENT));
					}
				}

			}

			List<String> orderIds = new ArrayList<>();
			
				 
			for (TollTag tollTag : tollTags) {
				if (StringUtils.isNotBlank(tollTag.getTagId())) {
					orderIds.add(tollTag.getTagId());
				} else if (Integer.parseInt(tollTag.getStatus()) == TollTagApprovalStatus.ACTIVE.value()) {
					tollTag = tollTag;
				}

			

			if (StringUtils.isEmpty(tollTag.getSerialNumber())) {
				Epc epc = tollDBService.findEpcByTagId(tollTag.getTagId());
				if (epc != null) {
					tollTag.setSerialNumber(epc.getSerialNumber());
					tollTag = tollDBService.updateTollTag(tollTag);
				}
			}
			Dipcoin dCoin = dipcoinDBService.findDipcoin(tollTag.getCustomerAccountId(),
					DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());

			vehicleInfo.put("amount", dCoin != null ? dCoin.getAmount() : BigDecimal.ZERO);
			vehicleInfo.put("serialNumber", tollTag.getSerialNumber());
			vehicleInfo.put("regNumber", tollTag.getRegistrationNo());
			vehicleInfo.put("classId", tollTag.getCategory().replaceAll("VC", StringUtils.EMPTY));
			vehicleInfo.put("bankName", bank.getName());
			vehicleInfo.put("bankImage", bankAPIServices.getBankProperties(bank.getReferenceId()).getBankLogo());
			for (VehicleCategories VehicleCategory : VehicleCategories.values()) {

				if (VehicleCategory.getId() == tollTag.getCategoryId()) {
					vehicleInfo.put("class", VehicleCategory.getVehicleInfo().replaceAll("\\s", StringUtils.EMPTY));
				}

			}
			List<Integer> customerAccountIds = customerDBService.getCustomerAccountIdsByBankID(bank.getId());
			
			if(CollectionUtils.isEmpty(customerAccountIds)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch customerAccounts")
						.format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}
			
			List<DipcoinTransaction> dTxns = dipcoinDBService.getFatagTxnRecon(orderIds, startTime, endTime,
					Arrays.asList(DipcoinTransactionType.COMPLETELY_USED.value(),
							DipcoinTransactionType.PARTIALLY_USED.value(),
							DipcoinTransactionType.DEEMED_ACCEPTED.value(),
							DipcoinTransactionType.NON_FIN.value()),
					customerAccountIds, DipcoinTransactionsStatus.SUCCESS.value(),
					TransactionSource.TOLL.value(), start, count);

			if (CollectionUtils.isEmpty(dTxns)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch toll txn")
						.data("Account Number", accountNumber).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));

			}

			List<Integer> dipcoinIds = new ArrayList<>();
			Map<String, DipcoinTransaction> dtxLookUp = new LinkedHashMap<>();
			Map<String, BankTransaction> btxLookUp = new LinkedHashMap<>();
			Map<String, Dipcoin> dCoinLookUp = new LinkedHashMap<>();

			for (DipcoinTransaction dtx : dTxns) {
				if (dtx.getDipcoinId() != NumberUtils.INTEGER_ZERO) {
					dipcoinIds.add(dtx.getDipcoinId());
					dtxLookUp.put(String.valueOf(dtx.getDipcoinId()), dtx);
				} else {
					dtxLookUp.put(String.valueOf(dtx.getDipcoinTransactionRefId()), dtx);
				}

			}

			List<Dipcoin> dCoins = dipcoinDBService.getDipcoins(dipcoinIds, null, null);

			if (CollectionUtils.isEmpty(dCoins)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch dCoins")
						.data("Account Number", accountNumber).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}

			for (Dipcoin dipcoin : dCoins) {
				dCoinLookUp.put(String.valueOf(dipcoin.getId()), dipcoin);
			}

			List<BankTransaction> bTxs = bankDBService.getTransactionsByDipcoinIdsAndTypes(dipcoinIds,
					Arrays.asList(BankTransactionType.DEBIT_TO_ACCOUNT.value()),
					BankTransactionsStatus.SUCCESS.value());

			if (CollectionUtils.isNotEmpty(bTxs)) {
				for (BankTransaction btx : bTxs) {
					btxLookUp.put(String.valueOf(btx.getDipcoinId()), btx);
				}
			}
			// Populate the response
			 List<TollTransactionsReport> tagReports = populateDipcoinTransactionInfo(tollTag, dtxLookUp, btxLookUp, dCoinLookUp, Arrays.asList(accountNumber), startDateValue, endDateValue);
	            reports.addAll(tagReports);
	            
			 
			dtxLookUp.clear();
			btxLookUp.clear();
			dCoinLookUp.clear();

			 }
			// set pagination info
			Pagination pagination = new Pagination();
			pagination.setStartRange(startTime);
			pagination.setEndRange(endTime);
			pagination.setScanCompleted(reports.size() < count);
			pagination.setTotal(reports.size());
			if (!pagination.getScanCompleted()) {
				pagination.setStart(start + count);
			}

			tollTransactionsReportResponse.setVehicleInfo(vehicleInfo);
			tollTransactionsReportResponse.setTransactions(reports);
			tollTransactionsReportResponse.setPagination(pagination);

			return ResponseEntity.ok(tollTransactionsReportResponse);
		}
		else {

			// Looping through each TollTag entry and fetching the dtx and btx of it
			List<Integer> customerAccountIds = customerDBService.getCustomerAccountIdsByBankID(bank.getId());
			
			if(CollectionUtils.isEmpty(customerAccountIds)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch customerAccounts")
						.format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}
			
			List<DipcoinTransaction> dTxns = dipcoinDBService.getFatagTxnRecon(null, startTime, endTime,
					Arrays.asList(DipcoinTransactionType.COMPLETELY_USED.value(),
							DipcoinTransactionType.PARTIALLY_USED.value(),
							DipcoinTransactionType.DEEMED_ACCEPTED.value(),
							DipcoinTransactionType.NON_FIN.value()),
					customerAccountIds, DipcoinTransactionsStatus.SUCCESS.value(),
					TransactionSource.TOLL.value(), start, count);

			if (CollectionUtils.isEmpty(dTxns)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch toll txn")
						.format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}

			List<Integer> dipcoinIds = new ArrayList<>();
			Map<String, DipcoinTransaction> dtxLookUp = new LinkedHashMap<>();
			Map<String, BankTransaction> btxLookUp = new LinkedHashMap<>();
			Map<String, Dipcoin> dCoinLookUp = new LinkedHashMap<>();

			for (DipcoinTransaction dtx : dTxns) {
				if (dtx.getDipcoinId() != NumberUtils.INTEGER_ZERO) {
					dipcoinIds.add(dtx.getDipcoinId());
					dtxLookUp.put(String.valueOf(dtx.getDipcoinId()), dtx);
				} else {
					dtxLookUp.put(String.valueOf(dtx.getDipcoinTransactionRefId()), dtx);
				}

			}

			List<Dipcoin> dCoins = dipcoinDBService.getDipcoins(dipcoinIds, null, null);

			if (CollectionUtils.isEmpty(dCoins)) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch dCoins")
						.data("Vehicle Number", vehicleNumber).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}

			for (Dipcoin dipcoin : dCoins) {
				dCoinLookUp.put(String.valueOf(dipcoin.getId()), dipcoin);
			}

			if (CollectionUtils.isEmpty(dCoins)) {
				LOG.error(
						LogFormatter.instance(httpServletContext.getTraceId()).message("Failed fetch dCoins").format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TRANSACTION_NOT_FOUND));
			}

			List<BankTransaction> bTxs = bankDBService.getTransactionsByDipcoinIdsAndTypes(dipcoinIds,
					Arrays.asList(BankTransactionType.DEBIT_TO_ACCOUNT.value()),
					BankTransactionsStatus.SUCCESS.value());

			if (CollectionUtils.isNotEmpty(bTxs)) {
				for (BankTransaction btx : bTxs) {
					btxLookUp.put(String.valueOf(btx.getDipcoinId()), btx);
				}
			}

			// Populate the response

			reports = populateDipcoinTransactionInfo(null, dtxLookUp, btxLookUp, dCoinLookUp, null, startDateValue,
					endDateValue);

			dtxLookUp.clear();
			btxLookUp.clear();
			dCoinLookUp.clear();
		}

		// set pagination info
		Pagination pagination = new Pagination();
		pagination.setStartRange(startTime);
		pagination.setEndRange(endTime);
		pagination.setScanCompleted(reports.size() < count);
		pagination.setTotal(reports.size());
		if (!pagination.getScanCompleted()) {
			pagination.setStart(start + count);
		}

		tollTransactionsReportResponse.setVehicleInfo(vehicleInfo);
		tollTransactionsReportResponse.setTransactions(reports);
		tollTransactionsReportResponse.setPagination(pagination);

		return ResponseEntity.ok(tollTransactionsReportResponse);

	}


}
