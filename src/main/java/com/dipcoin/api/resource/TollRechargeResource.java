package com.dipcoin.api.resource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.fraudMgmt.UserEventResource;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.APIResponse.ResponseHeader;
import com.dipcoin.api.model.CustomerDipcoinRequest;
import com.dipcoin.api.model.CustomerDipcoinResponse;
import com.dipcoin.api.model.TollRechargeReceipt;
import com.dipcoin.api.model.TollRechargeRequest;
import com.dipcoin.api.model.TollRechargeResponse;
import com.dipcoin.api.utils.TollMetricRegistry;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.TollRechargeDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TollRecharge;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollConstant.TransactionInitiator;

@Component("tollRechargeResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class TollRechargeResource {

	private static final Logger LOG = LogManager.getLogger(TollRechargeResource.class);

	@Autowired
	private CustomerDipcoinResource customerDipcoinResource;

	@Autowired
	private CustomerDBService customerDBService;

	@Autowired
	private DipcoinDBService dipcoinDBService;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private UserUtil userUtil;

	@Autowired
	private TollDBService tollDBService;

	@Autowired
	private TollRechargeDBService tollRechargeDBService;

	@Autowired
	private BrontooResource brontooResource;

	@Autowired
	private MerchantDBService merchantDBService;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	@Autowired
	private TollProperties tollProperties;

	@Autowired
	private EncryptionResource encryptionResource;

	@Autowired
	@Qualifier("com.dipcoin.metrics.TollMetricRegistry")
	private TollMetricRegistry tollMetricRegistry;

	@Autowired
	private UserEventResource userEventResource;

	public UserEventResource getUserEventResource() {
		return userEventResource;
	}

	public void setHttpServletContext(HttpServletContext httpServletContext) {
		this.httpServletContext = httpServletContext;
	}

	public CustomerDipcoinResource getCustomerDipcoinResource() {
		return customerDipcoinResource;
	}

	public BrontooResource getBrontooResource() {
		return brontooResource;
	}

	public EncryptionResource getEncryptionResource() {
		return encryptionResource;
	}

	@Autowired
	private RedissonClient redissonclient;

	/**
	 * addRechargeAmount is adding the recharge into the recharge table. It will
	 * find the active row based on customerAccountId and Status=1 and INACTIVE the
	 * row And add the entry and mark it active
	 */
	public ResponseEntity addRechargeAmount(final User user, final String clientTransactionId,
			final TollRechargeRequest rechargeReq, CustomerAccount customerAccount, User bankUser)
			throws APIException, Exception {

		TollRechargeResponse response = new TollRechargeResponse();

		// @TODO - Validate existing user is active, etc

		response.setClientTransactionId(clientTransactionId);

		// set the customerAccountId into recharge object
		rechargeReq.setCustomerAccountId(customerAccount.getId());

		// using tolltag ouside the braces
		TollTag tollTag = null;

		if (rechargeReq.getTagId() != null) {
//	      if (rechargeReq.getTagId().contains("-")) {
//	        
//	        Epc epc = tollDBService.asyncFindEpcBySerialNumber(rechargeReq.getTagId()).get();
//	        if (epc != null) {
//	          tollTag = tollDBService.findTollCustomersByTagId(epc.getRfidTag());
//	        }
//	      } else {
			tollTag = tollDBService.findTollCustomersByTagId(rechargeReq.getTagId());
//	      }
			if (tollTag == null) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("TollTag is null Pratik")
						.format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TOLL_TAG_DOESNT_EXIST));
			}

//	      List<TollRecharge> tollRecharges = tollRechargeDBService.findTollRechargeByTagIdOrderByIdDesc(
//	          tollTag.getTagId(), NumberUtils.INTEGER_ONE, NumberUtils.INTEGER_ZERO);
			//
//	      if (CollectionUtils.isNotEmpty(tollRecharges)) {
//	        rechargeReq.setAvailableAmount(
//	            tollRecharges.get(0).getAvailableAmount().add(rechargeReq.getRechargeAmount()));
//	      }

			// Call for populating the request for save
			TollRecharge rechargeSaveObj = populateForSaveAndUpdateTollCustomer(httpServletContext, user,
					clientTransactionId, rechargeReq, tollTag, bankUser);

			// save recharge
			tollRechargeDBService.save(rechargeSaveObj);
		} else {

			// Call for populating the request for save
			TollRecharge rechargeSaveObj = populateForSaveAndUpdateTollCustomer(httpServletContext, user,
					clientTransactionId, rechargeReq, tollTag, bankUser);

			// save recharge
			tollRechargeDBService.save(rechargeSaveObj);

		}

		populateTollRechargeResponse(user, rechargeReq, response, tollTag);

		// response.addHeaderCode(HeaderCode.TOLL_USER_DETAILS_UPDATED);
		return ResponseEntity.status(HttpStatus.OK).body(response);

	}

	public static void populateTollRechargeResponse(final User user, final TollRechargeRequest request,
			TollRechargeResponse response, TollTag tollTag) throws APIException, Exception {
		TollRechargeReceipt tollRechargeReceipt = new TollRechargeReceipt();
		tollRechargeReceipt.setRechargeAmount(request.getRechargeAmount());
		tollRechargeReceipt.setAvailableAmount(String.valueOf(request.getAvailableAmount()));
		tollRechargeReceipt.setOstaTransactionRefId(request.getDipcoinTransactionId());
		tollRechargeReceipt.setDateAndTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));

		if (tollTag != null) {
			tollRechargeReceipt.setVehicleNumber(tollTag.getRegistrationNo());
			tollRechargeReceipt.setAccountNumber(tollTag.getAccountNumber());
			tollRechargeReceipt.setStatus(String.valueOf(DBConstants.TollRechargeStatus.ACTIVE.value()));
		}
		response.setTollRechargeReceipt(tollRechargeReceipt);
	}

	private static TollRecharge populateForSaveAndUpdateTollCustomer(HttpServletContext httpServletContext, User user,
			String clientTransactionId, TollRechargeRequest rechargeReq, TollTag tollTag, User bankUser) {

		TollRecharge tollRecharge = new TollRecharge();

		tollRecharge.setClientTransactionId(clientTransactionId);
		tollRecharge.setRechargeAmount(rechargeReq.getRechargeAmount());
		tollRecharge.setDipcoinId(rechargeReq.getDipcoinId());
		tollRecharge.setTagId(rechargeReq.getTagId() != null ? rechargeReq.getTagId() : null);
		tollRecharge.setRechargeBy(bankUser != null ? bankUser.getId() : user.getId());
		tollRecharge.setRechargeTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
		tollRecharge.setIPAddress(httpServletContext.getOriginIp());
		tollRecharge.setAvailableAmount(rechargeReq.getAvailableAmount());
		tollRecharge.setStatus(String.valueOf(DBConstants.TollRechargeStatus.ACTIVE.value()));
		tollRecharge.setDipcoinTransactionRefId(rechargeReq.getDipcoinTransactionId());
		tollRecharge.setCustomerAccountId(rechargeReq.getCustomerAccountId());
		tollRecharge.setReferenceId(DBConstants.TollRechargeStatus.INACTIVE.value());

		if (tollTag != null) {
			tollRecharge.setVehicleNo(tollTag.getRegistrationNo());

		}
		return tollRecharge;
	}

	/**
	 * Recharge the Functionality with Create Osta
	 * 
	 */
	public ResponseEntity tollRechargeCreateOsta(final User user, final TollRechargeRequest rechargeReq,
			String clientTransactionId, User bankUser) throws Exception, APIException {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", rechargeReq)
				.data("Role", bankUser != null ? bankUser.getRole() : StringUtils.EMPTY)
				.data("userRole", user.getRole()).format());

		CustomerDipcoinResponse coinResponse = new CustomerDipcoinResponse();

		// Checking for the clientTransactionID is null
		if (clientTransactionId == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MISSING_CLIENTTRANSACTIONID));
		}

		// @TODO - Validate existing user is active, etc
		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		// checking for the request null
		if (rechargeReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		CustomerAccount customerAccount = null;
		if (rechargeReq.getCustomerAccountId() != null
				&& rechargeReq.getCustomerAccountId() > NumberUtils.INTEGER_ZERO) {
			customerAccount = customerDBService.getAccountById(rechargeReq.getCustomerAccountId());
			if (customerAccount != null && customerAccount.getUser() != null
					&& customerAccount.getUser().getId() != user.getId()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
			}
		}

		if (customerAccount == null) {
			customerAccount = customerDBService.asyncGetAccount(user.getId(), rechargeReq.getCardId()).get();
		}

		if (customerAccount == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.ACCOUNT_NUMBER_NOT_CORRECT));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Resolved recharge customer account")
				.data("CustomerAccountId", customerAccount.getId()).data("CardId", rechargeReq.getCardId())
				.data("RequestedCustomerAccountId", rechargeReq.getCustomerAccountId())
				.data("BankUId", customerAccount.getBankUId()).format());
		rechargeReq.setCustomerAccountId(customerAccount.getId());
		rechargeReq.setCardId(customerAccount.getUserCardId());

		if ((StringUtils.isEmpty(rechargeReq.getAuthorizationPin())
				&& !rechargeReq.getAuthorizationPin().equalsIgnoreCase(APIConstants.PIN))
				&& !userUtil.comparePassword(rechargeReq.getAuthorizationPin(), user.getPin())
				&& !(customerAccount.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value())
				&& !(customerAccount.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()) // cbi SSO
				&& !(customerAccount.getWalletBankId() > NumberUtils.INTEGER_ZERO)) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Pin does not match with Osta Pin")
					.format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_INVALID_PIN));
		}

		if (Hibernate.isInitialized(customerAccount.getBank())) {
			Hibernate.initialize(customerAccount.getBank());
		}

		if (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()) {
			rechargeReq.setAuthorizationPin(APIConstants.PIN);
		}

		// added check for SDK for CBI SSO
		if (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()
				|| customerAccount.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()) {
			rechargeReq.setAuthorizationPin(APIConstants.PIN);
		}

//	    if (StringUtils.isNotBlank(bankAPIServices.getBankProperties(customerAccount.getBank().getReferenceId()).getNotCallBankServer())) {
//	    	
//	 	   SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
//	 	   String dateString = dateFormat.format(new Date(System.currentTimeMillis()));
//	 	   	
//	 	   	LocalTime serverTime = LocalTime.parse( dateString ) ;
//	 	      	   	
//	 	   	if(serverTime.isAfter( LocalTime.parse(bankAPIServices.getBankProperties(customerAccount.getBank().getReferenceId()).getNotCallBankServer().trim().split("-")[NumberUtils.INTEGER_ZERO]) ) 
//	     	   	&& serverTime.isBefore( LocalTime.parse(bankAPIServices.getBankProperties(customerAccount.getBank().getReferenceId()).getNotCallBankServer().trim().split("-")[NumberUtils.INTEGER_ONE]))) {
//	 	   	  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//	 	   			.message("Bank server connection is prohibited because of time constraint")
//	 	   			.data("serverTime", serverTime).format());
//	 	   	 return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//	 	             .body(APIResponse.error(HeaderCode.BANK_SERVER_DOWN));
//	 	   	}    	
		// }

		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();
		List<String> roleList = new ArrayList<>();
		roleList.add(DBConstants.UserRoles.MERCHANT_INTERNAL.value());

		if (CollectionUtils.isEmpty(merchants)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("merchant is null").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
		}

		CustomerDipcoinRequest createReq = new CustomerDipcoinRequest();
		BigDecimal initialAmount = null;
		ResponseEntity createDipcoinresponse = null;
		ResponseEntity deleteDipcoinResponse = null;
		ResponseEntity updateExceptionResponse = null;

		RLock lock = null;
		if (TransactionInitiator.OSTA.value().equals(rechargeReq.getInitiatedFrom())) {
			lock = redissonclient.getFairLock(String.valueOf(customerAccount.getId()) + rechargeReq.getUsageType());
			lock.lock(60, TimeUnit.SECONDS);
		} else if (APIConstants.WEB.equals(rechargeReq.getInitiatedFrom())) {
			lock = redissonclient.getFairLock(String.valueOf(customerAccount.getId()));
			lock.lock(60, TimeUnit.SECONDS);
		}

		Dipcoin dipcoin = dipcoinDBService.findDipcoin(customerAccount.getId(),
				DBConstants.DipcoinStatus.ACTIVE.value(), DBConstants.DipcoinUsageType.TOLL.value());

		// Request To create Osta.
		if (dipcoin != null) {
			// Adding Request To create Osta.
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.data("Active Toll Dcoin Which will be deleted", dipcoin.getAmount()).format());
			initialAmount = dipcoin.getAmount();
			try {
				httpServletContext.setClientTransactionId(
						clientTransactionId.concat(StringUtils.SPACE).concat(rechargeReq.getInitiatedFrom())
								.concat(StringUtils.SPACE) + (bankUser != null ? bankUser.getId() : user.getId()));
				customerDipcoinResource.getDipcoinBankHelper().setHttpServletContext(httpServletContext);
				customerDipcoinResource.getDipcoinResource().setHttpServletContext(httpServletContext);
				deleteDipcoinResponse = customerDipcoinResource.deleteDipcoin(user,
						user.getPhone().concat(dipcoin.getCoin()), false);

			} catch (APIException e) {

				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Active Toll Dcoin Which will be deleted Exception").format(), e);
				boolean holdmarked = false;
				if (e.getResponse() != null) {

					for (ResponseHeader responseHeader : e.getResponse().getCodes()) {
						if (responseHeader.getCode().toUpperCase()
								.contains(HeaderCode.BANK_HOLD_MARK_FOR_HOLD_DOES_NOT_EXIT.code().toUpperCase())
								|| responseHeader.getMessage().toUpperCase().contains(
										HeaderCode.BANK_HOLD_MARK_FOR_HOLD_DOES_NOT_EXIT.message().toUpperCase())) {
							holdmarked = true;

						}
					}
				}

				if (holdmarked) {
					deleteDipcoinResponse = customerDipcoinResource.deleteDipcoin(user,
							user.getPhone().concat(dipcoin.getCoin()), false);
				} else {
					deleteDipcoinResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getResponse());
				}

			} catch (Exception e) {

				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Active Toll Dcoin Which will be deleted Exception").format(), e);
				deleteDipcoinResponse = ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(new APIResponse().addHeaderCode(HeaderCode.BANK_ACCOUNT_HOLD_NOT_PERMITTED));
			}

			if (deleteDipcoinResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				LOG.debug(
						LogFormatter.instance(httpServletContext.getTraceId()).message("Dipcoin Not Deleted").format());
				return deleteDipcoinResponse;
			}

			createReq.setAmount(rechargeReq.getRechargeAmount().add(dipcoin.getAmount()));
		} else {

			createReq.setAmount(rechargeReq.getRechargeAmount());
		}

		// Adding Request To create Osta.
		createReq.setEncryptDipcoin(false);
		createReq.setAccountId(customerAccount.getId());
		createReq.setCardId(rechargeReq.getCardId());
		createReq.setCurrency(rechargeReq.getCurrency());
		createReq.setAuthorizationPin(rechargeReq.getAuthorizationPin());
		createReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
		createReq.setFetchDipcoin(true);
		createReq.setUsageCategory(
				StringUtils.isNotEmpty(rechargeReq.getUsageCategory()) ? rechargeReq.getUsageCategory()
						: StringUtils.EMPTY);
		// Setting the Osta Expiry for one year.
		if (rechargeReq.getUsageType() == DBConstants.DipcoinUsageType.TOLL.value()) {
			createReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);
		}

		if (customerAccount.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()
				&& customerAccount.getBank().getIsSsoEnabled() == BooleanStatus.YES.value()) {
			createReq.setValidateAuthorizationPin(false);
		}

		try {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.data("Actual Recharge Amount", createReq.getAmount()).format());

			httpServletContext.setClientTransactionId(
					clientTransactionId.concat(StringUtils.SPACE).concat(rechargeReq.getInitiatedFrom())
							.concat(StringUtils.SPACE) + (bankUser != null ? bankUser.getId() : user.getId()));
			customerDipcoinResource.getDipcoinResource().setHttpServletContext(httpServletContext);

			createDipcoinresponse = customerDipcoinResource.createDipcoin(user, createReq, false);

			if (lock != null)
				lock.unlock();

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("create dipcoin response")
					.data("response", createDipcoinresponse).format());

			if (createDipcoinresponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {

				revertDipcoin(user, rechargeReq, null, dipcoin);

				ResponseEntity updateExcCodeResponse = updateExceptionCode(TollConstant.ADD_OP, customerAccount,
						dipcoin, user, null, rechargeReq, coinResponse);
				if (lock != null)
					lock.unlock();

				return createDipcoinresponse;

			}

			coinResponse = (CustomerDipcoinResponse) createDipcoinresponse.getBody();

			if (coinResponse == null) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("create dipcoin response is null").format());

				ResponseEntity responseEntity = revertDipcoin(user, rechargeReq, null, dipcoin);
				CustomerDipcoinResponse customerDipcoinResponse = (CustomerDipcoinResponse) responseEntity.getBody();

				if (lock != null)
					lock.unlock();

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customerDipcoinResponse);

			}
		} catch (APIException e) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("exception caught while recharging").format(), e);

			ResponseEntity responseEntity = revertDipcoin(user, rechargeReq, null, dipcoin);

			CustomerDipcoinResponse customerDipcoinResponse = (CustomerDipcoinResponse) responseEntity.getBody();

			customerDipcoinResponse.setCodes(e.getResponse().getCodes());

			ResponseEntity updateExcCodeResponse = updateExceptionCode(TollConstant.ADD_OP, customerAccount, dipcoin,
					user, null, rechargeReq, coinResponse);

			if (lock != null)
				lock.unlock();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customerDipcoinResponse);

		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("exception caught while recharging").format());

			ResponseEntity responseEntity = revertDipcoin(user, rechargeReq, null, dipcoin);

			CustomerDipcoinResponse customerDipcoinResponse = (CustomerDipcoinResponse) responseEntity.getBody();
			ResponseEntity updateExcCodeResponse = updateExceptionCode(TollConstant.ADD_OP, customerAccount, dipcoin,
					user, null, rechargeReq, coinResponse);
			if (lock != null)
				lock.unlock();

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customerDipcoinResponse);

		}

		ResponseEntity updateExcCodeResponse = updateExceptionCode(TollConstant.REMOVE_OP, customerAccount, dipcoin,
				user, null, rechargeReq, coinResponse);

		if (updateExcCodeResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
			return updateExcCodeResponse;
		}

		Object responseBody = updateExcCodeResponse.getBody();
		TollTag tollTag = null;
		if (responseBody == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Skipping toll tag exception code update because tagId is not available").format());
		} else if (responseBody instanceof TollTag) {
			tollTag = (TollTag) responseBody;
		} else if (responseBody instanceof APIResponse) {
			APIResponse apiResponse = (APIResponse) responseBody;
			LOG.error("Received APIResponse instead of TollTag - ", apiResponse);
			throw new IllegalStateException("Received APIResponse instead of TollTag: " + apiResponse);
		} else {
			throw new IllegalStateException("Unexpected response type: " + responseBody.getClass());
		}

		// Recharge table Insertion after Successful create Osta.

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("Add And Update Recharge Table with dipcoin transaction reference id")
				.data("dcoinId", coinResponse.getDipcoinId())
				.data("OstaTransactionReferenceId", coinResponse.getOstaTransactionReferenceId()).format());

		TollRechargeRequest tollrequest = new TollRechargeRequest();

		tollrequest.setDipcoinId(coinResponse.getDipcoinId());
		tollrequest.setBankReferenceId(customerAccount.getBank().getReferenceId());
		tollrequest.setType(rechargeReq.getType());
		tollrequest.setCardId(rechargeReq.getCardId());
		tollrequest.setDipcoinTransactionId(coinResponse.getOstaTransactionReferenceId());

		tollrequest.setAutomaticFlag(rechargeReq.getAutomaticFlag());
		tollrequest.setRechargeAmount(rechargeReq.getRechargeAmount());
		tollrequest.setAvailableAmount(BigDecimal.valueOf(Double.parseDouble(coinResponse.getAmount())));
		tollrequest.setTagId(rechargeReq.getTagId() != null ? rechargeReq.getTagId() : null);

		// using addRechargeResponse outside the try block
		ResponseEntity addRechargeResponse = null;

		try {
			addRechargeResponse = this.addRechargeAmount(user, clientTransactionId, tollrequest, customerAccount,
					bankUser);

			if (addRechargeResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("dipcoin reverted to original state addRecharge Amount code ").format());
				ResponseEntity response = revertDipcoin(user, rechargeReq, coinResponse, dipcoin);
				if (response.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("dipcoin not reverted to original state").format());
					return response;
				}

				CustomerDipcoinResponse customerDipcoinResponse = (CustomerDipcoinResponse) response.getBody();

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customerDipcoinResponse);
			}

		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception Caught").format(), e);
			ResponseEntity response = revertDipcoin(user, rechargeReq, coinResponse, dipcoin);

			if (response.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				return response;
			}

			CustomerDipcoinResponse customerDipcoinResponse = (CustomerDipcoinResponse) response.getBody();

			customerDipcoinResponse.addHeaderCode(HeaderCode.TOLL_RECHARGE_FAILURE);

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("toll recharge failed")
					.data("response ", customerDipcoinResponse).format());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customerDipcoinResponse);
		}

		TollRechargeResponse tollRechargeResponse = (TollRechargeResponse) addRechargeResponse.getBody();

		coinResponse.setTollRechargeResponse(tollRechargeResponse);

		tollMetricRegistry.tollAmountRecharged().increment(createReq.getAmount().doubleValue());
		if (tollTag != null
				&& tollTag.getAvailableAmount().compareTo(tollTag.getMinimumAmount()) < NumberUtils.INTEGER_ZERO) {

			coinResponse.addHeaderCode(HeaderCode.TOLL_RECHARGE_SUCCESS_TAG_IN_LOW_BALANCE);
		} else {

			coinResponse.addHeaderCode(HeaderCode.TOLL_RECHARGE_SUCCESS);
		}
		coinResponse.setTollRechargeResponse(tollRechargeResponse);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("toll recharge success")
				.data("coinResponse ", coinResponse).format());

		return ResponseEntity.status(HttpStatus.OK).body(coinResponse);

	}

	private ResponseEntity updateExceptionCode(String operation, CustomerAccount customerAccount, Dipcoin dipcoin,
			User user, TollTag tollTag, TollRechargeRequest rechargeReq, CustomerDipcoinResponse coinResponse)
			throws Exception {

		if (rechargeReq.getTagId() != null) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Toll Tag Passed :" + rechargeReq.getTagId()).format());

//				if (rechargeReq.getTagId().contains("-")) {
//					Epc epc = tollDBService.asyncFindEpcBySerialNumber(rechargeReq.getTagId()).get();
//					if (epc == null) {
//						ResponseEntity response = revertDipcoin(user, rechargeReq, coinResponse, dipcoin);
//						if (response.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
//							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//									.message("dipcoin not reverted to original state").format());
//							return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//									.body(APIResponse.error(HeaderCode.TOLL_TAG_SERIAL_NUMBER_NOT_CORRECT));
//						}
//					} else {
//						tollTag = tollDBService.asyncFindTollCustomersByTagId(epc.getRfidTag()).get();
//					}
//				} else {
			tollTag = tollDBService.findTollCustomersByTagId(rechargeReq.getTagId());
//				}

			if (tollTag == null) {
				ResponseEntity response = revertDipcoin(user, rechargeReq, coinResponse, dipcoin);
				if (response.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("dipcoin not reverted to original state").format());
					return response;
				}
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("dipcoin reverted to original state").format());
				CustomerDipcoinResponse customerDipcoinResponse = (CustomerDipcoinResponse) response.getBody();

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(customerDipcoinResponse);

			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Tag List mapped with Toll Customer Id")
					.data("TollRegistrationId", tollTag.getTollRegistration().getId())
					.data("customerAccountId", tollTag.getCustomerAccountId()).format());

			// Checking If the TollTag->CustomerAccountID is equal to Payment
			// Source-->CustomerAccountID
			if (tollTag.getCustomerAccountId() != customerAccount.getId()) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.TAGID_NOT_ASSOCIATED_WITH_ISSUER_BANK));
			}

			if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)) {

				if (tollTag.getAvailableAmount().add(rechargeReq.getRechargeAmount())
						.compareTo(tollTag.getMinimumAmount()) < NumberUtils.INTEGER_ZERO
						&& operation.equalsIgnoreCase(TollConstant.REMOVE_OP)) {
					tollTag.setAvailableAmount(tollTag.getAvailableAmount().add(rechargeReq.getRechargeAmount()));
				} else {

					// UPDATING excode to netc
					if (tollProperties.isConnectNpci()) {

						try {
							ResponseEntity updateExceptionResponse = brontooResource.updateExceptionList(tollTag,
									operation, null);
							if (updateExceptionResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("ExcCode not updated at Npci").format());
								revertDipcoin(user, rechargeReq, coinResponse, dipcoin);
								return updateExceptionResponse;
							}

						} catch (Exception e) {
							LOG.error(LogFormatter.instance().message("Exception caught").format(), e);
							return ResponseEntity.status(HttpStatus.BAD_REQUEST)
									.body(APIResponse.error(HeaderCode.TOLL_USER_DETAILS_CANNOT_UPDATE));
						}
					}
					if (operation.equalsIgnoreCase(TollConstant.REMOVE_OP)) {
						tollTag.setAvailableAmount(tollTag.getMinimumAmount());
						tollTag.setExcCodeUpdateTime(String.valueOf(DateTime.now().getMillis()));
						tollTag.setExcCode(TollConstant.EXC_CODE_ACTIVE);
					}

				}
				if (operation.equalsIgnoreCase(TollConstant.REMOVE_OP)) {
					tollTag = tollDBService.updateTollTag(tollTag);
					if (tollTag != null) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("ExcCode Update with " + TollConstant.EXC_CODE_ACTIVE).format());
					}
				}

			}
			return ResponseEntity.status(HttpStatus.OK).body(tollTag);
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.message("Skipping toll tag exception code update because recharge request does not contain tagId")
				.format());
		return ResponseEntity.status(HttpStatus.OK).build();
	}

	public ResponseEntity revertDipcoin(User user, TollRechargeRequest rechargeReq,
			CustomerDipcoinResponse coinResponse, Dipcoin dipcoin) throws APIException, Exception {

		if (coinResponse != null) {

			ResponseEntity deleteDipcoinResponse = customerDipcoinResource.deleteDipcoin(user, coinResponse.getOsta(),
					false);

			if (deleteDipcoinResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Dipcoin not deleted for reverting to original state").format());
				return deleteDipcoinResponse;
			}
		}

		if (dipcoin != null) {

			CustomerDipcoinRequest createReq = new CustomerDipcoinRequest();
			createReq.setEncryptDipcoin(false);
			createReq.setAmount(dipcoin.getAmount());
			createReq.setAccountId(rechargeReq.getCustomerAccountId());
			createReq.setCardId(rechargeReq.getCardId());
			createReq.setCurrency(rechargeReq.getCurrency());
			createReq.setAuthorizationPin(rechargeReq.getAuthorizationPin());
			createReq.setUsageType(DBConstants.DipcoinUsageType.TOLL.value());
			createReq.setFetchDipcoin(true);
			createReq.setUsageCategory(
					StringUtils.isNotEmpty(rechargeReq.getUsageCategory()) ? rechargeReq.getUsageCategory()
							: StringUtils.EMPTY);
			createReq.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);

			ResponseEntity createDipcoinResponse = customerDipcoinResource.createDipcoin(user, createReq, false);

			if (createDipcoinResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Dipcoin Not created for reverting to original state").format());
				return createDipcoinResponse;
			}
			return createDipcoinResponse;
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CustomerDipcoinResponse());

	}

//			  public static void populateTollRechargeResponse(final User user,
//			      final TollRechargeRequest request, TollRechargeResponse response, TollTag tollTag)
//			      throws APIException, Exception {
//			    TollRechargeReceipt tollRechargeReceipt = new TollRechargeReceipt();
//			    tollRechargeReceipt.setRechargeAmount(request.getRechargeAmount());
//			    tollRechargeReceipt.setAvailableAmount(String.valueOf(request.getAvailableAmount()));
//			    tollRechargeReceipt.setOstaTransactionRefId(request.getDipcoinTransactionId());
//			    tollRechargeReceipt.setDateAndTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
//
//			    if (tollTag != null) {
//			      tollRechargeReceipt.setVehicleNumber(tollTag.getRegistrationNo());
//			      tollRechargeReceipt.setAccountNumber(tollTag.getAccountNumber());
//			      tollRechargeReceipt.setStatus(String.valueOf(DBConstants.TollRechargeStatus.ACTIVE.value()));
//			    }
//			    response.setTollRechargeReceipt(tollRechargeReceipt);
//			  }

}
