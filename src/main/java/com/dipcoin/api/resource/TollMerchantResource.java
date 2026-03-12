package com.dipcoin.api.resource;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataValidation;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.toll.commons.TollConstant;

import javax.ws.rs.core.Response.Status;
//import java.util.Base64;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
//import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.commons.APIConstants.RequestType;
import com.dipcoin.api.commons.APIConstants.TransactionRequestType;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APICustomization;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.AddMoneyToWalletRequest;
import com.dipcoin.api.model.BankInfoResponse;
import com.dipcoin.api.model.BanksResponse;
import com.dipcoin.api.model.CreateUserWalletRequest;
import com.dipcoin.api.model.EncryptionResponse;
import com.dipcoin.api.model.Pagination;
import com.dipcoin.api.model.TollRegistrationRequest;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.model.TollTagCountResponse;
import com.dipcoin.api.model.TollTagFeeAndChargesResponse;
import com.dipcoin.api.model.TollTagRequest;
import com.dipcoin.api.model.UserRegisterRequest;
import com.dipcoin.api.model.UserVerifyRequest;
//import com.dipcoin.api.model.WalletBalanceResponse;
import com.dipcoin.api.model.WalletUserInfo;
import com.dipcoin.bank.services.utils.VirtualBankProperties;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.commons.LogFormatter.Mask;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.commons.SmsClient.Templates.TollTagBankApproval;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.FeesAndDepositDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.dao.UserDao;
import com.dipcoin.db.services.commons.DBConstants.BankStatus;
import com.dipcoin.db.services.commons.DBConstants.BankTollStatus;
import com.dipcoin.db.services.commons.DBConstants.EpcStatus;
import com.dipcoin.db.services.commons.DBConstants.TollTagApprovalStatus;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.UserStatus;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.FeesAndDeposit;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
//import com.dipcoin.metrics.TollMetricRegistry;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("tollMerchantResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class TollMerchantResource {

	private static final Logger LOG = LogManager.getLogger(TollMerchantResource.class);

	private final static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private FeesAndDepositDBService feesAndDepositDBService;

	@Autowired
	private MerchantDBService merchantDBService;

	@Autowired
//	@Qualifier("com.dipcoin.metrics.TollMetricRegistry")
	private com.dipcoin.api.utils.TollMetricRegistry tollMetricRegistry;

	@Autowired
	private BankDBService bankDBService;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private TollDBService tollDBService;

	@Autowired
	private TollBankResource tollServiceBankResource;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	@Autowired
	private UserLoginResource userLoginResource;

	@Autowired
	private EncryptionResource encryptionResource;

//	@Autowired
//	private OAuth2CustomerResource oAuth2CustomerResource;

	@Autowired
	private CryptoUtil cryptoUtil;

	@Autowired
	private OAuth2WalletServiceResource oauth2WalletServiceResource;

	@Autowired
	private TollCustomerResource tollCustomerResource;

	@Autowired
	private DipcoinResource dipcoinResource;

	@Autowired
	private RedissonClient redissonclient;

	@Autowired
	private UserUtil userUtil;

	@Autowired
	private VirtualBankProperties virtualBankProperties;

	@Autowired
	private WalletResource walletResource;
	
	@Autowired
	private SmsClient smsClient;
	
	@Autowired
	private UserDao userDao;

	@Autowired
	private NotificationResource notificationResource;

	@Autowired
	private ApplicationProperties applicationProperties;

	private static final TimeZone dateTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
	public static final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

	/*
	 * Getting tollCustomer Details of basis of USERID OR BANKCODE OR TAGID.
	 */
	public ResponseEntity getTollVehicleCharges(final User user, final String bankRefId, final String vehicleClass,
			final Integer registrationType) throws Exception, APIException {

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		Bank bank = bankDBService.getBank(bankRefId);
		if (bank == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		TollTagFeeAndChargesResponse responseCharges = new TollTagFeeAndChargesResponse();
		List<APIResponse> listresponses = new ArrayList<APIResponse>();

		// @TODO WRITE THE CODE FOR FETCHING.
		List<FeesAndDeposit> feesAndDeposits = feesAndDepositDBService
				.findByBankIdAndChargeCategory(Arrays.asList(bank.getId()), new HashSet<>(Arrays.asList(vehicleClass)));

		// Data check.
		if (CollectionUtils.isEmpty(feesAndDeposits)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(APIResponse.error(HeaderCode.BANK_CHARGES_NOT_AVAILABLE));
		}
				
				
		BigDecimal totalMinimumAmount = BigDecimal.ZERO;	

		// Setter at Account Level
		responseCharges.setMinAmountMaintainForAccount(totalMinimumAmount);

		try {

			for (int i = 0; i < feesAndDeposits.size(); i++) {

				responseCharges.setGstAmount(tollServiceBankResource
						.getGstAmountCalculation(RegistrationType.IHMCL.value() == registrationType
								? feesAndDeposits.get(i).getIHMCLRegistrationAmount()
								: feesAndDeposits.get(i).getRegistrationAmount()));
				tollServiceBankResource.populateTollChargesResponse(user, feesAndDeposits.get(i), responseCharges,
						registrationType, false);
				
				listresponses.add(responseCharges);
			}

		} catch (Exception e) {
			LOG.error("Failed to set values for toll tag charges and Vehicle class " + feesAndDeposits);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		return ResponseEntity.status(HttpStatus.OK).body(listresponses);

	}

	/*
	 * Get banks info list
	 */
	public ResponseEntity getBanks(final User user) throws Exception {

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		Merchant merchant = merchantDBService.getMerchant(user.getBankMerchantId());

		if (merchant == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		List<BankInfoResponse> banks = new LinkedList<>();
		List<Bank> bankList = this.bankDBService.findBanksByIds(
				Arrays.stream(merchant.getBankId().split(",")).map(Integer::parseInt).collect(Collectors.toList()),
				BankStatus.ACTIVE.value(), BankTollStatus.ACTIVE.value());
		if (CollectionUtils.isEmpty(bankList)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		for (Bank bank : bankList) {

			BankInfoResponse response = new BankInfoResponse();
			response.setName(bank.getName());
			response.setPartnerReferenceId(bank.getReferenceId());
			response.setType(bank.getType());
			response.setTollStatus(bank.getTollStatus());
			response.setOrgId(StringUtils.isBlank(bank.getOrgId()) ? StringUtils.EMPTY : bank.getOrgId());

			response.setAuthenticationMethodsAsString(bank.getAuthenticationMethods());
			if (!StringUtils.isBlank(bank.getApiCustomization())) {
				Object alpha = objectMapper.readValue(bank.getApiCustomization(), Object.class);
				response.setApiCustomization((List<APICustomization>) alpha);

			}
			// @TODO - add more fields as required

			banks.add(response);
		}

		Pagination pagination = new Pagination();
		pagination.setScanCompleted(true);
		pagination.setTotal(banks.size());

		BanksResponse response = new BanksResponse();
		response.setBanks(banks);
		response.setPagination(pagination);

		return ResponseEntity.ok(response);

	}

	public ResponseEntity addAndUpdateTollCustomer(User merchantUser, final MultipartFile[] rcDoc,
			final MultipartFile idProof, final String request) throws Exception, APIException {

		String clientTransactionId = httpServletContext.getTraceId();
		if (!this.userDBService.isActive(merchantUser)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		TollRegistrationResponse response = new TollRegistrationResponse();

		final TollRegistrationRequest createReq = new ObjectMapper().readValue(request, TollRegistrationRequest.class);

		MultipartFile filepart = idProof;
		if (null == filepart || NumberUtils.INTEGER_ZERO == rcDoc.length) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_IMAGE_MISSING));
		}
		
		Bank walletBank = bankDBService.getBank(createReq.getVehicleList().get(0).getBankReferenceId());
		if (walletBank == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.BANK_DOESNT_EXISTS.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));

		}
		
		Epc epc = this.tollDBService.findEpcBySerialNumber(createReq.getVehicleList().get(0).getSerialNumber());

		if (epc == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.MISSING_EPC_DATA.message())
					.data("serialNumber",createReq.getVehicleList().get(0).getSerialNumber()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.MISSING_EPC_DATA));
		} else if(epc.getStatus() == EpcStatus.USED.value()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.TOLL_SERIAL_NUMBER_USED.message())
					.data("serialNumber",createReq.getVehicleList().get(0).getSerialNumber()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.TOLL_SERIAL_NUMBER_USED));
		} else if(!epc.getIin().equals(walletBank.getIin())) {
			LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.IIN_FROM_TAG_ID_DOES_N0T_MATCH_PAYER_IIN.message())
					.data("serialNumber",createReq.getVehicleList().get(0).getSerialNumber())
					.data("tagIin", epc.getIin())
					.data("payerIin", walletBank.getIin()).format());			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.IIN_FROM_TAG_ID_DOES_N0T_MATCH_PAYER_IIN));
		}else if(!("VC"+ Integer.parseInt(epc.getCategory())).equalsIgnoreCase(createReq.getVehicleList().get(0).getCategory())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.EPC_CATEGORY_MISMATCH.message())
					.data("category",createReq.getVehicleList().get(0).getCategory())
					.data("epc category",epc.getCategory()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.EPC_CATEGORY_MISMATCH));
		}

		CryptoUtil.AlgoScheme algoScheme = CryptoUtil.AlgoScheme.AES_CBC_PKCS5PADDING;
		String secretkey = cryptoUtil.base64RandomSecretKey(algoScheme);

		User newUser = null;
		List<User> users = userDBService.getUsersByRoles(createReq.getMobileNo(),
				Arrays.asList(UserRoles.CUSTOMER.value()), NumberUtils.INTEGER_ZERO);

		if (CollectionUtils.isNotEmpty(users)) {
			String expiryTime = String.valueOf(DateTime.now(DateTimeZone.UTC).plusMinutes(10).getMillis());
			for (User user : users) {
				if (user.getStatus() == UserStatus.ACTIVE.value()) {
					user.setEncryptionKeyExpiryTime(expiryTime);
					newUser = userLoginResource.updateUser(user);
				} else if (user.getStatus() == UserStatus.ONHOLD.value()) {
					user.setStatus(UserStatus.ACTIVE.value());
					
					user.setEncryptionKeyExpiryTime(expiryTime);
					newUser = userLoginResource.updateUser(user);
					if (newUser == null) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message(HeaderCode.USER_NOT_UPDATED.message()).format());
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body(APIResponse.error(HeaderCode.BAD_REQUEST));

					}
				}
			}
		}
		if (newUser == null) {

			// Register the Customer in our system
			UserRegisterRequest userRegReq = new UserRegisterRequest();
			userRegReq.setFname(createReq.getFirstName());
			userRegReq.setLname(createReq.getLastName());
			userRegReq.setEmail(
					StringUtils.isEmpty(createReq.getEmailId()) ? "notpresent@co.in" : createReq.getEmailId());
			userRegReq.setPhonenum(createReq.getMobileNo());
			userRegReq.setPassword(createReq.getVehicleList().get(0).getTempPassword());
			userRegReq.setTnc(true);
			
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Add new user requesr")
					.data("UserRegisterRequest", userRegReq).format());

			ResponseEntity userInfoResponse = userLoginResource.registerUser(userRegReq, APIConstants.CUSTOMER,
					clientTransactionId, null, null, Boolean.FALSE);

			if (HttpStatus.BAD_REQUEST.value() <= userInfoResponse.getStatusCodeValue()) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.FAIL_TO_ADD_USER.message()).format());

				throw new APIException(HttpStatus.BAD_REQUEST, APIResponse.error(HeaderCode.FAIL_TO_ADD_USER));
			}

			// Get the added user
			List<User> newlyaddedUsers = userLoginResource.getUser(createReq.getMobileNo(),
					DBConstants.UserStatus.INACTIVE.value());

			// Not able to Find the Customer
			if (CollectionUtils.isEmpty(newlyaddedUsers)) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("New added s not found...")
						.data("response ", userInfoResponse).format());
				response.addHeaderCode(HeaderCode.USER_DOESNT_EXIST);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			User customerUser = newlyaddedUsers.get(0);

			customerUser.setStatus(DBConstants.UserStatus.ACTIVE.value());

			String expiryTime = String.valueOf(DateTime.now(DateTimeZone.UTC).plusMinutes(10).getMillis());

			customerUser.setEncryptionKey(secretkey);
			customerUser.setEncryptionAlgo(algoScheme.algo());
			customerUser.setEncryptionPadding(algoScheme.scheme());
			customerUser.setEncryptionKeyExpiryTime(expiryTime);

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("User data is set to verify and response").data(" bankCustomer Id ", customerUser.getId())
					.data("Email", customerUser.getEmail()).maskedData(" pin", Mask.PASSWORD_MASKED.value())
					.maskedData("password", Mask.PASSWORD_MASKED.value()).format());
			
			
//			if (!smsClient.sendSms(customerUser.getPhone().substring(0,4)+"@"+createReq.getVehicleList().get(0).getRegistrationNo().
//					substring(createReq.getVehicleList().get(0).getRegistrationNo().length()-4,
//							createReq.getVehicleList().get(0).getRegistrationNo().length()),
//					Templates.LoginPassword.format(customerUser.getFirstName()),
//					true)) {
//				LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
//						.data("phone", customerUser.getPhone()).format());
//			}

//			byte[] decoder = Base64.decodeBase64(createReq.getPlainTempPassword());

			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Pratiik")
					.data("createReq1",createReq).format());
			
//			Base64.Decoder decoder = Base64.getDecoder();
//
//			
//			if (!smsClient.sendSms(customerUser.getPhone(),
//				Templates.LoginPassword.format(new String(decoder.decode(createReq.getPlainTempPassword()))),
//				true)) {
//			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
//					.data("phone", customerUser.getPhone()).format());
//		}
			
			newUser = userLoginResource.updateUser(customerUser);		

		}

		if (newUser == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.FAIL_TO_ADD_USER.message()).format());

			throw new APIException(HttpStatus.BAD_REQUEST, APIResponse.error(HeaderCode.FAIL_TO_ADD_USER));
		}

		CreateUserWalletRequest createUserWalletRequest = new CreateUserWalletRequest();
		createUserWalletRequest.setEmail(newUser.getEmail());
		createUserWalletRequest.setFname(newUser.getFirstName());
		createUserWalletRequest.setLname(newUser.getLastName());
		createUserWalletRequest.setPhonenum(newUser.getPhone());
		createUserWalletRequest.setRole(APIConstants.CUSTOMER);

		ResponseEntity responseOauthWallet = oauth2WalletServiceResource.createUserWallet(createUserWalletRequest,
				newUser, httpServletContext.getMerchant(), Boolean.FALSE, TransactionSource.WEB,
				createReq.getVehicleList().get(0).getBankReferenceId());

		if (HttpStatus.BAD_REQUEST.value() <= responseOauthWallet.getStatusCodeValue()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.WALLET_Not_CREATED.message()).format());

			return responseOauthWallet;
		}

		WalletUserInfo walletUserInfo = (WalletUserInfo) responseOauthWallet.getBody();

		
		CustomerAccount customerAccount = dipcoinResource.getAccount(newUser.getId(), walletUserInfo.getCardId());

		if (customerAccount == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.FAILED_FETCHING_CUSTOMER_ACCOUNT.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.FAILED_FETCHING_CUSTOMER_ACCOUNT));
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

		RLock vehicleNumberLock;

		RKeys rkeys = redissonclient.getKeys();

		Iterable<String> serialNumberRkeys = rkeys
				.getKeysByPattern(createReq.getVehicleList().get(0).getRegistrationNo());

		if (IterableUtils.size(serialNumberRkeys) > NumberUtils.INTEGER_ZERO) {
			Iterator<String> rkey = serialNumberRkeys.iterator();
			while (rkey.hasNext()) {

				String key = rkey.next();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.TOLL_TAG_VEHICLE_NUMBER_REGISTRATON_IN_PROGRESS_TRY_LATER.message())
						.data("key", key).format());
				if (key.equalsIgnoreCase(createReq.getVehicleList().get(0).getRegistrationNo())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.TOLL_TAG_VEHICLE_NUMBER_REGISTRATON_IN_PROGRESS_TRY_LATER.message())
							.format());

					response.addHeaderCode(HeaderCode.TOLL_TAG_VEHICLE_NUMBER_REGISTRATON_IN_PROGRESS_TRY_LATER);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}
		}

		vehicleNumberLock = redissonclient.getFairLock(createReq.getVehicleList().get(0).getRegistrationNo());
		vehicleNumberLock.lock(120, TimeUnit.SECONDS);
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("Locking Vehicle number " + createReq.getVehicleList().get(0).getRegistrationNo()).format());

		List<TollTag> tollTags = tollDBService
				.findTollTagByRegistrationNo(Arrays.asList(createReq.getVehicleList().get(0).getRegistrationNo()));
		if (CollectionUtils.isNotEmpty(tollTags)) {
			for (TollTag tollTag : tollTags) {
				if (Integer.parseInt(tollTag.getStatus()) == TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()
						|| Integer.parseInt(tollTag.getStatus()) == TollTagApprovalStatus.ACTIVE.value()
						|| Integer.parseInt(tollTag.getStatus()) == TollTagApprovalStatus.CUSTOMER_ACTIVATION_PENDING
								.value()) {
					response.addHeaderCode(HeaderCode.TOLL_USER_VEHICLE_NUMBER_ALREADY_EXIST);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}
		}

		// Call for populating the request for save
		TollRegistration registerationSaveObj = tollCustomerResource.populateForSaveAndUpdateTollCustomer(newUser, null,
				createReq, clientTransactionId, customerAccount, feesAndDeposits, RegistrationType.WALLET.value(), httpServletContext.getUser());

		
		TollRegistration tollRegistration = tollDBService.asyncSave(registerationSaveObj).get();

		if (null == tollRegistration) {

			removeVehicleLock(vehicleNumberLock);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_NOT_GENERATED));
		}
		
		Map<String, Object> objectLookUp = new HashMap<>();
		objectLookUp.put("feesAndDeposits", feesAndDeposits);
		objectLookUp.put("customerAccount", customerAccount);		
		objectLookUp.put("walletBank", walletBank);
		objectLookUp.put("walletUserInfo", walletUserInfo);
		objectLookUp.put("vehicleNumberLock", vehicleNumberLock);

		LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Toll Created").format());

		response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);

		// Adding the count to metrix
		tollMetricRegistry.numberOfTagsApplied().increment();

		return ResponseEntity.ok(objectLookUp);
	}

	public void removeVehicleLock(RLock vehicleNumberLock) {
		if (vehicleNumberLock != null) {
			try {
				vehicleNumberLock.unlock();
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("vehicleNumberLock unlocked")
						.format());
			} catch (Exception ex) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Exception Caught while unlocking vehicleNumberLock").format(), ex);
			}
		}
	}
	
	public ResponseEntity addFile(User merchantUser, final MultipartFile[] rcDoc, final MultipartFile idProof,
			final String request, Map<String, Object> objectLookUp) throws Exception, APIException {
		
		final TollRegistrationRequest createReq = new ObjectMapper().readValue(request, TollRegistrationRequest.class);
		List<User> users = userDBService.getUsersByRoles(createReq.getMobileNo(),
				Arrays.asList(UserRoles.CUSTOMER.value()), NumberUtils.INTEGER_ZERO);
		User newUser = users.get(0);
		
		MultipartFile filepart = idProof;	
		
		
		List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNo(Arrays.asList(createReq.getVehicleList().get(0).getRegistrationNo()));
		TollRegistration tollRegistration = null;
		for(TollTag tolltag : tollTags) {
			if(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer.parseInt(tolltag.getStatus())) {
				tollRegistration = tolltag.getTollRegistration();
			}
		}
		
		tollCustomerResource.setFilePath(tollRegistration, newUser, filepart, createReq, rcDoc);
		
		TollRegistrationResponse response = new TollRegistrationResponse();
		response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		return ResponseEntity.ok(response);
	}

	public ResponseEntity addMoney(User merchantUser, final MultipartFile[] rcDoc, final MultipartFile idProof,
			final String request, Map<String, Object> objectLookUp) throws Exception, APIException {
		final TollRegistrationRequest createReq = new ObjectMapper().readValue(request, TollRegistrationRequest.class);

		List<FeesAndDeposit> feesAndDeposits = (List<FeesAndDeposit>) objectLookUp.get("feesAndDeposits");
		WalletUserInfo walletUserInfo = (WalletUserInfo) objectLookUp.get("walletUserInfo");
		Merchant merchant = httpServletContext.getMerchant();
		if (merchant == null && merchantUser != null) {
			merchant = merchantDBService.getMerchant(merchantUser.getBankMerchantId());
		}
		if (merchant == null || CollectionUtils.isEmpty(feesAndDeposits) || walletUserInfo == null) {
			removeVehicleLock((RLock) objectLookUp.get("vehicleNumberLock"));
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Failed to prepare add money request")
					.data("merchantFound", merchant != null)
					.data("hasFees", !CollectionUtils.isEmpty(feesAndDeposits))
					.data("hasWalletUserInfo", walletUserInfo != null)
					.format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}
		List<User> users = userDBService.getUsersByRoles(createReq.getMobileNo(),
				Arrays.asList(UserRoles.CUSTOMER.value()), NumberUtils.INTEGER_ZERO);
		User newUser = users.get(0);
		AddMoneyToWalletRequest addMoneyToWalletRequest = new AddMoneyToWalletRequest();
		addMoneyToWalletRequest.setAmount(feesAndDeposits.get(0).getRegistrationAmount().add(feesAndDeposits.get(0).getMinimumAmount()));
		addMoneyToWalletRequest.setCurrency("INR");
		addMoneyToWalletRequest
				.setOrderId(createReq.getVehicleList().get(0).getRegistrationNo() + System.currentTimeMillis());
		addMoneyToWalletRequest.setPartnerTransactionReferenceId(CoreUtils.generateDipcoinToMerchantReferenceNumber());
		addMoneyToWalletRequest.setPartnerReferenceId(merchant.getReferenceId());
		addMoneyToWalletRequest.setRequestType(TransactionRequestType.CREATEOSTA.value());
		addMoneyToWalletRequest.setPhonenum(createReq.getMobileNo());
		addMoneyToWalletRequest.setWalletId(walletUserInfo.getWalletId());

		ResponseEntity responseOauthAddMoneyWallet = oauth2WalletServiceResource.addMoneyToWallet(
				addMoneyToWalletRequest, newUser, merchant, TransactionSource.OAUTH);

		if (HttpStatus.BAD_REQUEST.value() <= responseOauthAddMoneyWallet.getStatusCodeValue()) {
			removeVehicleLock((RLock) objectLookUp.get("vehicleNumberLock"));
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.WALLET_TOPUP_FAILED.message()).format());

			return responseOauthAddMoneyWallet;
		}

		TollRegistrationResponse response = new TollRegistrationResponse();
		response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		return ResponseEntity.ok(response);

	}

	public ResponseEntity createOsta(User merchantUser, final MultipartFile[] rcDoc, final MultipartFile idProof,
			final String request, Map<String, Object> objectLookUp) throws Exception, APIException {
		final TollRegistrationRequest createReq = new ObjectMapper().readValue(request, TollRegistrationRequest.class);
		List<FeesAndDeposit> feesAndDeposits = (List<FeesAndDeposit>) objectLookUp.get("feesAndDeposits");
		CustomerAccount customerAccount = (CustomerAccount) objectLookUp.get("customerAccount");
		List<User> users = userDBService.getUsersByRoles(createReq.getMobileNo(),
				Arrays.asList(UserRoles.CUSTOMER.value()), NumberUtils.INTEGER_ZERO);
		User newUser = users.get(0);
		createReq.setAuthorizationPin(APIConstants.PIN);
		createReq.setMiscCharges(false);

		ResponseEntity createFeeOstaResponse = tollCustomerResource.createFeesandDepositOsta(newUser, null,
				customerAccount.getBank(), customerAccount, feesAndDeposits, createReq, RegistrationType.WALLET.value(),
				httpServletContext.getTraceId());

		if (createFeeOstaResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
			removeVehicleLock((RLock) objectLookUp.get("vehicleNumberLock"));
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("osta creation for fee failed")
					.format());
			return createFeeOstaResponse;
		}

		TollRegistrationResponse response = new TollRegistrationResponse();
		response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		return ResponseEntity.ok(response);

	}

	public ResponseEntity updateSerialNumber(User merchantUser, final MultipartFile[] rcDoc,
			final MultipartFile idProof, final String request, Map<String, Object> objectLookUp)
			throws Exception, APIException {

		

		Bank walletBank = (Bank) objectLookUp.get("walletBank");

		final TollRegistrationRequest createReq = new ObjectMapper().readValue(request, TollRegistrationRequest.class);
		
		List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNo(Arrays.asList(createReq.getVehicleList().get(0).getRegistrationNo()));
		TollRegistration tollRegistration = null;
		for(TollTag tolltag : tollTags) {
			if(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value() == Integer.parseInt(tolltag.getStatus())) {
				tollRegistration = tolltag.getTollRegistration();
			}
		}
		
		TollTagRequest updateReq = new TollTagRequest();
		updateReq.setSerialNumber(createReq.getVehicleList().get(0).getSerialNumber());
		updateReq.setId(tollRegistration.getTollTag().get(0).getId());
		updateReq.setIin(walletBank.getIin());

		ResponseEntity tagUpdateResponce = tollServiceBankResource.updateTollCustomerByVendor(
				httpServletContext.getUser(), walletBank, updateReq, httpServletContext.getTraceId());
		TollRegistrationResponse response = new TollRegistrationResponse();
		if (HttpStatus.BAD_REQUEST.value() <= tagUpdateResponce.getStatusCodeValue()) {
			removeVehicleLock((RLock) objectLookUp.get("vehicleNumberLock"));
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.TOLL_USER_INVALID_STATE.message()).format());

			response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_SUCCESSFUL_TAG_ASSIGNMENT_PENDING);
			return ResponseEntity.ok(response);
		}

		
		response.addHeaderCode(HeaderCode.TOLL_REGISTRATION_BY_CUSTOMER_SUCCESSFULL);
		return ResponseEntity.ok(response);
	}

	public ResponseEntity getTollCustomerDetails(final User user, final String status, Long startTime, Long endTime,
			Integer start, Integer count, String vehicleNumber, String phone) throws Exception, APIException {

		if (!this.userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("Get All Toll Customer list based on just BankId")
				.data("bankId", httpServletContext.getMerchant().getReferenceId()).format());

		// Getting the all Register user
		List<TollTag> tollTags = this.tollDBService.findTollCustomersByMerchantId(
				httpServletContext.getMerchant().getId(), start, count, startTime, endTime, vehicleNumber);

		if (CollectionUtils.isEmpty(tollTags)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.TOLL_TAG_DOESNT_EXIST.message()).format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_TAG_DOESNT_EXIST));
		}

		List<APIResponse> listresponses = new LinkedList<APIResponse>();
		for (TollTag tollTag : tollTags) {

			TollRegistrationResponse responseObj = new TollRegistrationResponse();

			tollServiceBankResource.populateTollCustomersResponse(user, tollTag.getTollRegistration(),
					Arrays.asList(tollTag), responseObj);

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

	
	/*
	 * 
	 */
	public ResponseEntity getTagsRejectedPendingCounts(final User user)

			throws Exception, APIException {

		TollTagCountResponse response = new TollTagCountResponse();
		if (user == null) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}
		
		if(!userDBService.isActive(user)) {		
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		Long pendingTagCount = this.tollDBService.countTagByMerchantId(httpServletContext.getMerchant().getId(),
				Arrays.asList(Integer.valueOf(TollTagApprovalStatus.BANK_APPROVAL_PENDING.value()).toString()));
		response.setTagApprovalPendingCounts(pendingTagCount);

		Long rejectedTagCount = this.tollDBService.countOfTags(httpServletContext.getMerchant().getId(),
				Arrays.asList(Integer.valueOf(TollTagApprovalStatus.BANK_REJECTED.value()).toString()));
		response.setTagRejectedCounts(rejectedTagCount);
		

		return ResponseEntity.ok(response);
	}
	
	public ResponseEntity updatePhoneNumber(User user, Integer tollRegistrationId, String newPhoneNumber)  throws Exception{

		if (user == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
		}

		if (!this.userDBService.isMerchantAdmin(user) && !this.userDBService.isMerchantSuperAdmin(user)
				&& !this.userDBService.isBrontooAdmin(user) && !this.userDBService.isBrontooSuperAdmin(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		if (!userDBService.isActive(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
		}

		if (tollRegistrationId == null || newPhoneNumber == null || StringUtils.isEmpty(newPhoneNumber)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
		}

		TollRegistration tollRegistration = tollDBService.findById(tollRegistrationId);

		if (tollRegistration == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("TollRegistration is not present")
					.data("TollRegistrationId", tollRegistrationId).format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.TOLL_REGISTRATION_DOESNT_EXIST));
		}

		List<User> users = userDBService.getUsersByPhoneAndRole(newPhoneNumber,
				Arrays.asList(DBConstants.UserRoles.CUSTOMER.value()), StringUtils.EMPTY, null);

		if (CollectionUtils.isNotEmpty(users)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Phone Number already present")
					.data("Phone", newPhoneNumber).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.PHONE_NUMBER_ALREADY_REGISTERED));
		}

		tollRegistration.setMobileNo(newPhoneNumber);

		if(tollDBService.update(tollRegistration) == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to updateTollRegistration")
					.data("TollRegistration", tollRegistration).format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.FAILED_TO_UPDATE_TOLL_REGISTRATION));
		}

		User tollCustomer = userDao.find(tollRegistration.getUserId());

		if (tollCustomer == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("User not found")
					.data("User Id", tollRegistration.getUserId()).format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
		}

		String oldPhoneNumber = tollCustomer.getPhone();

		tollCustomer.setPhone(newPhoneNumber);

		if(userDBService.updateUser(tollCustomer) == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to upadte User")
					.data("User Id", tollRegistration.getUserId()).format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.FAILED_TO_UPDATE_USER));

		}

		/**
		// success message
		if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(newPhoneNumber,
			Templates.MerchantPhoneUpdate.format(tollRegistration.getMobileNo(), newPhoneNumber,
				user.getFirstName() + " " + user.getLastName(),
				String.valueOf(formatter.format(new Date()))),
			true)) {
		    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
			    .data("phone", tollRegistration.getMobileNo()).format());
		}
		if (applicationProperties.getAwsSMSClient()) {
		    NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
		    notificationRequestContext.setTraceId(httpServletContext.getTraceId());
		    if (!notificationResource.sendSms(tollRegistration.getMobileNo(),
			    Templates.MerchantPhoneUpdate.format("oldNumber", newPhoneNumber, user.getFirstName(),
				    String.valueOf(formatter.format(new Date()))),
			    httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
				.data("phone", tollRegistration.getMobileNo()).format());
		    }
		}
		
		**/

		formatter.setTimeZone(dateTimeZone);

		if (!smsClient.sendSms(newPhoneNumber,
			Templates.MerchantPhoneUpdate.format(oldPhoneNumber,newPhoneNumber,user.getFirstName(),
				String.valueOf(formatter.format(new Date()))),
			true)) {
		LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
				.data("phone", newPhoneNumber).format());
	}

		return ResponseEntity.status(HttpStatus.OK).body(APIResponse.error(HeaderCode.USER_UPDATED));

	}

	public ResponseEntity verifyCustomerRegistered(User merchant, String phoneNumber) {

	    if (merchant == null) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
	    }

	    if (!userDBService.isActive(merchant)) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
	    }

	    if (phoneNumber == null) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(APIResponse.error(HeaderCode.MISSING_INVALID_INFO));
	    }

	    boolean isRegistered = false;

	    List<User> users = userDBService.getUsersByPhoneAndRole(phoneNumber,
			Arrays.asList(DBConstants.UserRoles.CUSTOMER.value()), StringUtils.EMPTY, null);

	    if(CollectionUtils.isNotEmpty(users)) {

		User user = users.get(0);

		List<TollRegistration> tollRegistrations = tollDBService.findTollCustomersByUserId(user.getId());

		if (CollectionUtils.isNotEmpty(tollRegistrations)) {
		    isRegistered = true;
		}

	    }

	    Map<String,Boolean> response = new HashMap<String,Boolean>();

	    response.put("isRegistered", isRegistered);

	    return ResponseEntity.status(HttpStatus.OK).body(response);
	}


	private static ByteArrayInputStream createSampleBulkRegFile() {

	    ByteArrayOutputStream out = new ByteArrayOutputStream();

	    final String[] columns = { "firstName", "lastName", "mobileNo", "emailId", "address", "pincode",
		    "idProofNo", "registrationNo", "category", "registeredState", "state", "serialNumber",
		    "idProofName", "vehicleTypeName" };

	    List<String> tollStates = TollConstant.states;

	    String[] states = tollStates.toArray(new String[tollStates.size()]);

	    HSSFWorkbook workbook = new HSSFWorkbook();
	    HSSFSheet realSheet = workbook.createSheet("Sheet xls");
	    HSSFSheet hidden = workbook.createSheet("hidden");
	    for (int i = 0, length = states.length; i < length; i++) {
		String name = states[i];
		HSSFRow row = hidden.createRow(i);
		HSSFCell cell = row.createCell(0);
		cell.setCellValue(name);
	    }

	    Row headerRow = realSheet.createRow(0);

	    for (int column = 0; column < columns.length; column++) {
		Cell cell = headerRow.createCell(column);
		cell.setCellValue(columns[column]);
	    }

	    Name namedCell = workbook.createName();
	    namedCell.setNameName("hidden");
	    namedCell.setRefersToFormula("hidden!$A$1:$A$" + states.length);

	    DVConstraint constraint = DVConstraint.createFormulaListConstraint("hidden");
	    CellRangeAddressList addressList = new CellRangeAddressList(1, 50, 9, 9);
	    HSSFDataValidation validation = new HSSFDataValidation(addressList, constraint);
	    workbook.setSheetHidden(1, true);
	    realSheet.addValidationData(validation);

	    DVConstraint constraint1 = DVConstraint.createFormulaListConstraint("hidden");
	    CellRangeAddressList addressList1 = new CellRangeAddressList(1, 50, 10, 10);
	    HSSFDataValidation validation1 = new HSSFDataValidation(addressList1, constraint1);
	    workbook.setSheetHidden(1, true);
	    realSheet.addValidationData(validation1);

	    CellRangeAddressList addressList2 = new CellRangeAddressList(1, 500, 8, 8);
	    DVConstraint constraint2 = DVConstraint
		    .createExplicitListConstraint(new String[] { DBConstants.VehicleCategory.VC4.name(),
			    DBConstants.VehicleCategory.VC5.name(), DBConstants.VehicleCategory.VC6.name(),
			    DBConstants.VehicleCategory.VC7.name(), DBConstants.VehicleCategory.VC12.name(),
			    DBConstants.VehicleCategory.VC15.name(), DBConstants.VehicleCategory.VC16.name() });
	    HSSFDataValidation validation2 = new HSSFDataValidation(addressList2, constraint2);
	    realSheet.addValidationData(validation2);

	    List<String> ids = TollConstant.tollRegistrationIdProofs;

	    String[] idProofs = ids.toArray(new String[ids.size()]);

	    CellRangeAddressList addressList3 = new CellRangeAddressList(1, 500, 12, 12);
	    DVConstraint constraint3 = DVConstraint.createExplicitListConstraint(idProofs);
	    HSSFDataValidation validation3 = new HSSFDataValidation(addressList3, constraint3);
	    realSheet.addValidationData(validation3);

	    List<String> tollVehicleTypes = TollConstant.vehicleTypes;

	    String[] vehicleTypes = tollVehicleTypes.toArray(new String[tollVehicleTypes.size()]);

	    CellRangeAddressList addressList4 = new CellRangeAddressList(1, 500, 13, 13);
	    DVConstraint constraint4 = DVConstraint.createExplicitListConstraint(vehicleTypes);
	    HSSFDataValidation validation4 = new HSSFDataValidation(addressList4, constraint4);
	    realSheet.addValidationData(validation4);

	    try {
		workbook.write(out);
	    } catch (IOException e) {
		LOG.error("Failed to create Sample download excel file");
		e.printStackTrace();
	    }
	    finally{
		try {
		    out.close();
		} catch (IOException e) {
		    LOG.error("Failed to close Sample download excel file");
		    e.printStackTrace();
		}
	    }
	    return new ByteArrayInputStream(out.toByteArray());

	}

	public ResponseEntity downloadBulkRegistrationSampleFile(User user)  {

	    ByteArrayInputStream in = createSampleBulkRegFile();

	    HttpHeaders headers = new HttpHeaders();
//	    headers.add("Content-Disposition", "attachment; filename=SampleFile.xls");
//	    return ResponseEntity.ok().headers(headers).body(new InputStreamResource(in));

	    return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=SampleFile.xls")
		    .body(new InputStreamResource(in));

	}

	
}
