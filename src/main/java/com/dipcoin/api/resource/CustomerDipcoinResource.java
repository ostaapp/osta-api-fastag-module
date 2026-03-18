package com.dipcoin.api.resource;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import com.dipcoin.metrics.DipcoinMetricRegistry;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIFeatureFlags;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.fraudMgmt.ProcessEvent;
import com.dipcoin.api.fraudMgmt.UserEventResource;
import com.dipcoin.api.model.APICustomization;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.AspectContext;
import com.dipcoin.api.model.CustomerDipcoinRequest;
import com.dipcoin.api.model.CustomerDipcoinResponse;
import com.dipcoin.api.model.DipcoinResponse;
import com.dipcoin.api.model.DipcoinThreadLocal;
import com.dipcoin.api.fraudMgmt.EventUtils;
import com.dipcoin.api.model.RequestMetadata;
import com.dipcoin.api.utils.RechargeConstants;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.client.BankClient.Operation;
import com.dipcoin.bank.services.comm.MarkLienRequest;
import com.dipcoin.bank.services.comm.MarkLienResponse;
import com.dipcoin.bank.services.comm.RemoveLienRequest;
import com.dipcoin.bank.services.comm.RemoveLienResponse;
import com.dipcoin.bank.services.utils.BankConstants.BankResponseStatus;
import com.dipcoin.bank.services.utils.BankConstants.Comment;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankServiceException;
import com.dipcoin.bank.services.utils.BankUtils;
import com.dipcoin.commons.Constants.Currency;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.RechargeDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.BankType;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.MerchantRecon;
import com.dipcoin.db.services.model.Recharge;
import com.dipcoin.db.services.model.User;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;

@Component("customerDipcoinResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class CustomerDipcoinResource {

	private static final Logger LOG = LogManager.getLogger(CustomerDipcoinResource.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;
	
	@Autowired
	private MerchantDBService merchantDBService;
	
	@Autowired
	private DipcoinBankHelper dipcoinBankHelper;

	@Autowired
	private CoreUtils coreUtils;
	
	@Autowired
	private EncryptionResource encryptionResource;

	@Autowired
	private BankUtils bankUtils;
	
	@Autowired
	private NotificationResource notificationResource;
	
	@Autowired
	@Qualifier("com.dipcoin.metrics.DipcoinMetricRegistry")
	private DipcoinMetricRegistry dipcoinMetricRegistry;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private CustomerDBService customerDBService;
	
	@Autowired
	private APIFeatureFlags apiFeatureFlags;
	
	@Autowired
	private UserUtil userUtil;
	
	@Autowired
	private DipcoinDBService coinDBService;
	
	@Autowired
	private BankAPIServices bankAPIServices;
	
	@Autowired
	private RechargeDBService rechargeDBService;
	
	@Autowired
	private UserEventResource userEventResource;

	@Autowired
	private ApplicationProperties applicationProperties;
	
	@Autowired
	private EmailUtils emailUtils;
	
	@Autowired
	private SmsClient smsClient;
	
	@Autowired
	private DipcoinResource dipcoinResource;

	@Autowired
	private BankDBService bankDBService;
	
	public void setHttpServletContext(HttpServletContext httpServletContext) {
	    this.httpServletContext = httpServletContext;
	}
	
	
	  public DipcoinResource getDipcoinResource() {    
		    return this.dipcoinResource;
		  }
	
	  public DipcoinBankHelper getDipcoinBankHelper() {    
		    return this.dipcoinBankHelper;
	  }
	

	public ResponseEntity createDipcoin(final User user, final CustomerDipcoinRequest createReq,
			boolean rollbackDipcoinTxs) throws Exception, APIException {
		return createDipcoin(user, createReq, rollbackDipcoinTxs, Optional.empty(), Optional.empty());
	}

	/**
	 * Customer Create DipcoinRequest
	 * 
	 * @param userAgent
	 * @return
	 * @throws APIException
	 */

	public ResponseEntity createDipcoin(final User user, final CustomerDipcoinRequest createReq,
			boolean rollbackDipcoinTxs, final Optional<User> delegateUser, final Optional<Dipcoin> detachedDcoin)
			throws Exception, APIException {

		String clientTransactionId = httpServletContext.getClientTransactionId();

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("DipcoinRequest", createReq)
				.data("clientTransactionId", clientTransactionId).format());
		CustomerDipcoinResponse response = new CustomerDipcoinResponse();
		response.setClientTransactionId(clientTransactionId);

		if (clientTransactionId == null) {
			response.addHeaderCode(HeaderCode.MISSING_CLIENTTRANSACTIONID);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		// verify request
		if (createReq == null) {
			response.addHeaderCode(HeaderCode.BAD_REQUEST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		createReq.setCoreUtils(coreUtils);
		if (!createReq.validate(httpServletContext)) {
			response.addHeaderCodes(createReq.getErrorCodes());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// verify if user is a customer
		if (!this.userDBService.isCustomer(user) && !this.userDBService.isVirtualBank(user)) {
			response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}

		// fetch customer account
		CustomerAccount account = null;
		if (createReq.getAccountId() != null && createReq.getAccountId() > NumberUtils.INTEGER_ZERO) {
			account = this.customerDBService.getAccountById(createReq.getAccountId());
			if (account != null && account.getUser() != null && account.getUser().getId() != user.getId()) {
				response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
		}

		if (account == null) {
			account = this.customerDBService.getAccount(user.getId(), createReq.getCardId());
		}

		if (account == null) {
			response.addHeaderCode(HeaderCode.USER_ACCOUNT_DOESNT_EXIST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// get customer account, fetch bank and verify bank
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetch CustomerAccount")
				.data("Dipcoin Customer Account", account.getId()).data("UserId", user.getId())
				.data("CardId", createReq.getCardId()).data("AccountId", createReq.getAccountId())
				.data("BankUId", account.getBankUId()).format());
		LOG.info("Resolved dipcoin customer account requestedAccountId:" + createReq.getAccountId()
				+ " requestedCardId:" + createReq.getCardId() + " resolvedCustomerAccountId:" + account.getId()
				+ " resolvedBankUId:" + account.getBankUId() + " usageType:" + createReq.getUsageType()
				+ " usageCategory:" + createReq.getUsageCategory());

		// account is inactive
		if (!CustomerAccountStatus.ACTIVE.equals(account.getStatus())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.USER_ACCOUNT_INACTIVE.message()).data("CustomerAccountId", account.getId())
					.data("Status", account.getStatus()).format());
			response.addHeaderCode(HeaderCode.USER_ACCOUNT_INACTIVE);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// bank active
		if (!bankDBService.isActive(account.getBank())) {
			response.addHeaderCode(HeaderCode.BANK_NOT_ACTIVE);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// cbi sso check ...skip pin checks for cbi sso
		if (account.getBank().getIsSsoEnabled() == BooleanStatus.YES.value()
				&& account.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()) {
			createReq.setValidateAuthorizationPin(false);
		}

		if (!this.userDBService.isVirtualBank(user) && (createReq.getUsageType() != DipcoinUsageType.TOLL.value()
				&& createReq.getUsageType() != DipcoinUsageType.FEE.value()
				&& createReq.getUsageType() != DipcoinUsageType.DEPOSIT.value())) {
			// check boundary conditions
			if ((createReq.getUsageType() != DipcoinUsageType.TOLL.value()
					&& createReq.getUsageType() != DipcoinUsageType.FEE.value()
					&& createReq.getUsageType() != DipcoinUsageType.DEPOSIT.value())
					&& account.getMaxCoinLimit() != null && createReq.getAmount() != null
					&& account.getMaxCoinLimit().compareTo(createReq.getAmount()) < 0) {
				response.addHeaderCode(HeaderCode.BAD_REQUEST);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			if (DBConstants.USER_MAX_PER_TX_LIMIT.compareTo(createReq.getAmount()) < 0) {
				LOG.debug(LogFormatter.instance().data("RequestAmount", createReq.getAmount())
						.data("MaxTxLimit", DBConstants.USER_MAX_PER_TX_LIMIT).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.USER_PER_TRANSACTION_LIMIT));
			}
			if (user.getPerDayLimit() == null || user.getPerMonthLimit() == null || user.getPerYearLimit() == null) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}
			if (DBConstants.USER_MAX_PER_DAY_LIMIT.compareTo(user.getPerDayLimit().add(createReq.getAmount())) < 0) {
				LOG.debug(LogFormatter.instance()
						.data("UserPerDayLimit", (user.getPerDayLimit().add(createReq.getAmount())))
						.data("MaxPerDayLimitSetByOsta", DBConstants.USER_MAX_PER_DAY_LIMIT).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.USER_PER_DAY_LIMIT));
			}

			if (DBConstants.USER_MAX_PER_MONTH_LIMIT
					.compareTo(user.getPerMonthLimit().add(createReq.getAmount())) < 0) {
				LOG.debug(LogFormatter.instance()
						.data("MaxPerMonthLimitSetByOsta", DBConstants.USER_MAX_PER_MONTH_LIMIT)
						.data("UserPerMonthLimit ", (user.getPerMonthLimit()).add(createReq.getAmount())).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.USER_PER_MONTH_LIMIT));
			}
			if (DBConstants.USER_MAX_PER_YEAR_LIMIT.compareTo(user.getPerYearLimit().add(createReq.getAmount())) < 0) {
				LOG.debug(LogFormatter.instance()
						.data("MaxPerMonthLimitSetByOsta", DBConstants.USER_MAX_PER_MONTH_LIMIT)
						.data("UserPerYearLimit", (user.getPerYearLimit()).add(createReq.getAmount())).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(HeaderCode.USER_PER_YEAR_LIMIT));
			}
		}

		return createDipcoin(user, createReq, rollbackDipcoinTxs, delegateUser, detachedDcoin, account, response);
	}

	/**
	 * Customer Create DipcoinRequest
	 * 
	 * @param userAgent
	 * @return
	 * @throws APIException
	 */

	public ResponseEntity createDipcoin(final User user, final CustomerDipcoinRequest createReq,
			boolean rollbackDipcoinTxs, final Optional<User> delegateUser, final Optional<Dipcoin> detachedDcoin,
			CustomerAccount account, CustomerDipcoinResponse response) throws Exception, APIException {

		AspectContext ctx = new AspectContext(applicationProperties.getRunAudit());
		RequestMetadata requestMetadata = new RequestMetadata("createDipcoin");
		requestMetadata.setBankTransactionType(String.valueOf(BankTransactionType.LIEN_MARK.value()));
		requestMetadata.setDipcoinTransactionType(String.valueOf(DipcoinTransactionType.GENERATE_DIPCOIN.value()));
		ctx.setAudit("CreateDipcoin");
		ctx.setUserId(String.valueOf(user.getId()));
		ctx.setCustomerAccountId(Integer.toString(account.getId()));
		ctx.setRequestMetadata(requestMetadata);
		DipcoinThreadLocal.set(ctx);

		// send request to bank
		String dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(
				account.getBank().getReferenceId(), Operation.MARK_LIEN, BankTransactionType.LIEN_MARK.value());

		// commented for vahan

		Either<ResponseEntity, Future<MarkLienResponse>> markLienResponseT = markLien(createReq, user, account,
				dipcoinReferenceNumber);
//		  
//		  // if error return
		if (markLienResponseT.isLeft()) {
			return markLienResponseT.getLeft();
		}

		Future<MarkLienResponse> bResponseTask = markLienResponseT.get();

		// @TODO - set response time from bResponse
		MarkLienResponse bResponse = bResponseTask.get();

//	    MarkLienResponse bResponse = new MarkLienResponse(); 
//	    bResponse.setBankResponseCode("0");
//	    bResponse.setBankResponseDesc("Lien Marked Successful");
//	    bResponse.setBankTransactionReferenceNumber("1530794156");
//	    bResponse.setCbsDate("20231127");
//	    bResponse.setCbsJournalNumber("8");
//	    bResponse.setBankTransactionReferenceNumber(dipcoinReferenceNumber);
//	    bResponse.setTransactionTime("1701093599133");
//	    bResponse.setDipcoinReferenceNumber(dipcoinReferenceNumber);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankResponse", bResponse)
				.data("DipcoinReferenceNumber", dipcoinReferenceNumber).format());

		// init bank transaction
		BankTransaction bTx = new BankTransaction();
		bTx.setAmount(createReq.getAmount());
		bTx.setType(BankTransactionType.LIEN_MARK.value());
		bTx.setCustomerAccountId(account.getId());
		bTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
		bTx.setBankId(account.getBank().getId());

		// system failure
		if (bResponse == null || !BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode())
				|| !dipcoinReferenceNumber.equals(bResponse.getDipcoinReferenceNumber())) {
			bTx.setStatus(BankTransactionsStatus.FAILED.value());
			if (bResponse != null) {
				bTx.setRequestTime(bResponse.getRequestTime());
				bTx.setResponseTime(bResponse.getResponseTime());
				bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
				bTx.setRawBankResponse(bResponse.getBankResponseDesc() != null ? bResponse.getBankResponseDesc()
						: bResponse.getErrorMsg());
				bTx.setBankResponseCode(bResponse.getBankResponseCode());
			}
			if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to add Bank Transaction").data("transaction", bTx).format());
			}
			dipcoinMetricRegistry.dipcoinFailedToCreate().increment();

			// throw exception to initiate rollback
			if (bResponse != null)
				response.setCodes(Arrays.asList(
						response.new ResponseHeader(bResponse.getBankResponseCode(), bResponse.getBankResponseDesc())));

			// response.addHeaderCode(HeaderCode.INSUFFICIENT_AMOUNT);
			else
				response.addHeaderCode(HeaderCode.BANK_FAILED_TO_MARK_LIEN);

			if (bResponse == null) {
				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);
			}

			throw new APIException(HttpStatus.BAD_REQUEST, response);

		}

		requestMetadata.setBankRawRequest(objectMapper.writeValueAsString(bTx));
		requestMetadata.setBankRawResponse(objectMapper.writeValueAsString(bResponse));
		requestMetadata.setBankTransaction(bResponse.getBankRequest());
		ctx.setRequestMetadata(requestMetadata);
		DipcoinThreadLocal.set(ctx);

		// check conditions which are mandated for dipcoin creating
		if (StringUtils.isEmpty(bResponse.getCbsJournalNumber())) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Failed to obtain CbsJournalNumber").format());
			// throw exception to initiate rollback
			response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
			throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank Services Lien Marking")
				.data("requestTime", bResponse.getRequestTime()).data("responseTime", bResponse.getResponseTime())
				.format());

		Dipcoin dcoin = (detachedDcoin != null && detachedDcoin.isPresent()) ? detachedDcoin.get() : null;

		// if dipcoin not provided
		User owner = delegateUser != null ? delegateUser.orElse(user) : user;

		String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());

		if (dcoin == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Generating Dipcoin")
					.data("CustomerAccountId", account.getId()).format());

			boolean isDuplicate = true;
			int retry = applicationProperties.getGenerateDipcoinRetry();
			do {
				try {
					dcoin = dipcoinResource.generateDipcoin(createReq, owner, account, Boolean.FALSE);
					isDuplicate = false;
				} catch (Exception e) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Exception caught while generate Dipcoin cause is" + e.getMessage()).format());

					if (ExceptionUtils.getRootCause(e) instanceof SQLIntegrityConstraintViolationException) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Duplicate Coin Exception occoured so continuing to generate").format());
						isDuplicate = true;
					} else {
						break;
					}
				}

			} while (isDuplicate && --retry > NumberUtils.INTEGER_ZERO);

			if (isDuplicate) {

				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("maximum attempt reached to generate dipcoin hence removing lien").format());
				removeLien(account.getBank().getReferenceId(), account.getBank().getCode(), account.getBankUId(),
						createReq.getAmount(), bResponse.getCbsJournalNumber(), bResponse.getCbsDate());

				response.addHeaderCode(HeaderCode.MAXIMUM_ATTEMPT_REACHED_COIN_GENERATION);
				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);

			}

		} else if (dcoin.getCustomerAccount() == null) {
			// update dcoin with account details. do not update status
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Updating Dipcoin")
					.data("CustomerAccountId", account.getId()).format());
			dcoin.setCustomerAccount(account);
			dcoin.setUser(owner);

			dcoin = this.coinDBService.updateCoin(dcoin, dcoin.getStatus());
		}

		if (dcoin != null) {
			// rules are applied before processing the request further
			boolean failure = userEventResource.applyRule(user, null, Integer.toString(dcoin.getId()), requestTime,
					ProcessEvent.EventType.OstaCreation, EventUtils.dipcoinCreationFailure);

			if (!failure) {

				// throw exception to initiate rollback
				response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);
			}
		}

		requestMetadata.setDipcoin(objectMapper.writeValueAsString(dcoin));
		ctx.setRequestMetadata(requestMetadata);
		DipcoinThreadLocal.set(ctx);

		DipcoinTransaction dTx = saveMarkLienDipcoinTransaction(dcoin, owner, bResponse, rollbackDipcoinTxs, createReq);

		if (dcoin != null)
			bTx.setDipcoinId(dcoin.getId());

		// populate bank transaction
		bTx.setRawBankRequest(bResponse.getRawRequest());
		bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
		bTx.setRawBankResponse(bResponse.getRawData());
		bTx.setStatus(BankTransactionsStatus.SUCCESS.value());
		bTx.setRequestTime(bResponse.getRequestTime());
		bTx.setResponseTime(bResponse.getResponseTime());
		bTx.setCBSReferenceID(bResponse.getCbsJournalNumber());
		bTx.setCBSLienDate(bResponse.getCbsDate());
		bTx.setBankResponseCode(bResponse.getBankResponseCode());
		LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).data("transaction", bTx).format());

		if (dTx != null)
			bTx.setDipcoinTransactionId(dTx.getId());
		if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to add Bank Transaction")
					.data("transaction", bTx).format());
		}

		// dipcoin created
		if (dcoin != null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Create dipcoin")
					.data("Coin", dcoin.getCoin()).data("ExpiryTime", dcoin.getExpiryTime()).format());

			if (user.getPerDayLimit() != null && user.getPerMonthLimit() != null && user.getPerYearLimit() != null) {
				user.setPerDayLimit(user.getPerDayLimit().add(createReq.getAmount()));
				user.setPerMonthLimit(user.getPerMonthLimit().add(createReq.getAmount()));
				user.setPerYearLimit(user.getPerYearLimit().add(createReq.getAmount()));
				this.userDBService.updateUser(user);
			}

			// initialize customer if not done.
			if (!Hibernate.isInitialized(dcoin.getCustomerAccount())) {
				Hibernate.initialize(dcoin.getCustomerAccount());
			}

			CustomerAccount customerAccount = dcoin.getCustomerAccount();

			// initialize bank if not done.
			if (!Hibernate.isInitialized(customerAccount.getBank())) {
				Hibernate.initialize(customerAccount.getBank());
			}

			if (createReq.getUsageType() == DBConstants.DipcoinUsageType.RESTRICTED_BBPS_CASH.value()) {
				BankTransaction bankTransactn = saveUsageTypeBasedBankTransaction(null, dTx, dcoin, user, createReq,
						customerAccount.getBank());

				if (this.bankDBService.asyncAddTransaction(bankTransactn).get() == null) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to add BBPS cash Bank Transaction").data("transaction", bTx).format());
				}

			} else if (createReq.getUsageType() == DBConstants.DipcoinUsageType.RESTRICTED_BBPS_FUND_TRANSFER.value()) {

				List<BankTransaction> bTes = bankDBService.getBankTransactions(Arrays.asList(customerAccount.getId()),
						null, null, Arrays.asList(DBConstants.BankTransactionsStatus.SUCCESS.value()),
						Arrays.asList(DBConstants.BankTransactionType.BBPS_ACCOUNT_FUND_TRANSFER.value()),
						createReq.getPartnerTransactionReferenceId(), null, null);

				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Fetching bbps account transfer transaction").data("transaction", bTes).format());

				if (!CollectionUtils.isEmpty(bTes) && bTes.get(0) != null && bTes.get(0).getDipcoinId() == 0) {
					BankTransaction bTs = bTes.get(0);
					bTs = saveUsageTypeBasedBankTransaction(bTs, dTx, dcoin, user, createReq,
							customerAccount.getBank());
					if (this.bankDBService.updateTransaction(bTs) == null) {
						LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Failed to add dipcoin id for acount transfer bank transaction")
								.data("transaction", bTs.getDipcoinId()).data("transaction", bTs.getId())
								.data("transaction", bTs.getCBSReferenceID()).format());
					}
				}
			}

			Bank bank = customerAccount.getBank();

			// Updating branchCode in CustomerAccount
			if (StringUtils.isNoneBlank(bResponse.getBranchCode())) {
				account.setBranchCode(bResponse.getBranchCode());

				CustomerAccount customerAcc = this.customerDBService.updateAccount(account);

				if (customerAcc == null) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to update branchCode in CustomerAccount table").format());
				}
			}

			// populate response
			populateCustomerDipcoinResponse(encryptionResource, user, dcoin, null, dTx, response, coreUtils, null,
					createReq.getEncryptDipcoin());

			if (createReq.getFetchDipcoin() == true) {
				response.setDipcoinId(dcoin.getId());
			}

			// send email to user
			if (applicationProperties.enableSMS() && !APIUtils.smsEmailAreNotAllowed(dcoin.getUsageType())
					&& (!BankType.VIRTUAL_BANK.equals(bank.getType())
							&& !UserRoles.VIRTUAL_BANK.equals(user.getRole()))) {
				// send email to user
				if (user.getIsEmailVerified() == BooleanStatus.YES.value()
						&& !emailUtils.sendGenerateDipcoinEmail(user, account, response)) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to send email to user " + user.getId() + " for dipcoin " + dcoin.getId())
							.format());
				}

				// send SMS
				if (!applicationProperties.getAwsSMSClient()
						&& !smsClient.sendSms(user.getPhone(),
								Templates.CustomerCreateDipcoin.format(dcoin.getCoin(), dcoin.getAmount(),
										dcoin.getExpiryTime()),
								httpServletContext.getClientFeatureFlags().smsEnabled())) {
					LOG.debug(
							LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
									.data("phone", user.getPhone()).data("template", Templates.CustomerCreateDipcoin
											.format(dcoin.getCoin(), dcoin.getAmount(), dcoin.getExpiryTime()))
									.format());
				}

				if (applicationProperties.getAwsSMSClient()) {

					NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
					notificationRequestContext.setTraceId(httpServletContext.getTraceId());
					if (!notificationResource.sendSms(user.getPhone(),
							Templates.CustomerCreateDipcoin.format(dcoin.getCoin(), dcoin.getAmount(),
									dcoin.getExpiryTime()),
							httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

						LOG.debug(
								LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
										.data("phone", user.getPhone()).data("template", Templates.CustomerCreateDipcoin
												.format(dcoin.getCoin(), dcoin.getAmount(), dcoin.getExpiryTime()))
										.format());

					}
				}

				// @TODO verify if notification enabled
				if (apiFeatureFlags.notificationEnabled()
						&& httpServletContext.getClientFeatureFlags().notificationEnabled()) {
					notificationResource.notifyDipcoinCreate(user, response);
				}

			}

			// state saved in queue
			userEventResource.processInQueue(user, requestTime, EventUtils.dipcoinCreationSuccess,
					Integer.toString(dcoin.getId()));

			// register metric
			dipcoinMetricRegistry.dipcoinCreated().increment();

			return ResponseEntity.ok(response);
		}

		LOG.error(
				LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to generate dipcoin").format());
		// throw exception to initiate rollback
		response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
		throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);
	}

	public Either<ResponseEntity, Future<MarkLienResponse>> markLien(final CustomerDipcoinRequest createReq,
			final User user, final CustomerAccount account, String dipcoinReferenceNumber)
			throws BankServiceException, IOException {

		String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());

		MarkLienRequest bRequest = new MarkLienRequest(account.getBank().getReferenceId(), account.getBank().getCode());
		bRequest.setAmount(CoreUtils.round2(createReq.getAmount()).toString());
		bRequest.setTransactionType(Integer.toString(BankTransactionType.LIEN_MARK.value()));
		bRequest.setBankUID(account.getBankUId());
		bRequest.setUserId(String.valueOf(user.getId()));
		bRequest.setCardId(String.valueOf(account.getUserCardId()));
		bRequest.setAccountId(String.valueOf(account.getId()));

		bRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
		bRequest.setCurrency(Currency.INDIA.value());
		bRequest.setComment(Comment.LIENMARK.value());

		// Not yet implemented for OTP check.
		if (DBConstants.BankAuthorizationMethod.BANK_PIN.value() == account.getBank().getAuthorizationMethod()) {
			bRequest.setAuthorizationPin(createReq.getAuthorizationPin());
		} else if (DBConstants.BankAuthorizationMethod.OSTA_PIN.value() == account.getBank().getAuthorizationMethod()) {

			if (createReq.getValidateAuthorizationPin()) {

				if (BooleanStatus.YES.value() == account.getTpinEnable()) {

					if (!userUtil.comparePassword(createReq.getAuthorizationPin(), account.getTpin())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Pin does not match with TPin").format());
						return Either.left(ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body(APIResponse.error(HeaderCode.USER_INVALID_PIN)));
					}

				} else {

					if (!createReq.getAuthorizationPin().equalsIgnoreCase(APIConstants.PIN)
							&& !userUtil.comparePassword(createReq.getAuthorizationPin(), user.getPin())) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Pin does not match with Osta Pin").format());
						return Either.left(ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.body(APIResponse.error(HeaderCode.USER_INVALID_PIN)));
					}
				}
			}

		} else if (DBConstants.BankAuthorizationMethod.UPI_PIN.value() == account.getBank().getAuthorizationMethod()
				|| DBConstants.BankAuthorizationMethod.NO_PIN.value() == account.getBank().getAuthorizationMethod()) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Pin verification not required")
					.format());
		} else {
			return Either.left(
					ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST)));
		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankRequest", bRequest).format());
		LOG.info("Sending bank lien request requestAccountId:" + bRequest.getAccountId() + " requestCardId:"
				+ bRequest.getCardId() + " requestBankUId:" + bRequest.getBankUID() + " resolvedCustomerAccountId:"
				+ account.getId() + " resolvedCustomerCardId:" + account.getUserCardId()
				+ " resolvedCustomerBankUId:" + account.getBankUId() + " amount:" + bRequest.getAmount()
				+ " dipcoinReferenceNumber:" + dipcoinReferenceNumber);
		BankRequestContext bankRequestContext = new BankRequestContext();
		bankRequestContext.setTraceId(httpServletContext.getTraceId());
		Future<MarkLienResponse> bResponseTask = this.bankAPIServices.markLien(bankRequestContext, bRequest);

		return Either.right(bResponseTask);
	}
	
	public void removeLien(String bankReferenceId, String bankCode, String bankUid, BigDecimal amount,
			String cbsJournalNumber, String cbsLienDate) throws Exception {
		RemoveLienRequest bRequest = new RemoveLienRequest(bankReferenceId, bankCode);
		bRequest.setTransactionType(Integer.toString(BankTransactionType.LIEN_REMOVAL.value()));
		bRequest.setBankUID(bankUid);
		bRequest.setAmount(CoreUtils.round2(amount).toString());
		bRequest.setCbsJournalNumber(cbsJournalNumber);
		bRequest.setCbsDate(cbsLienDate);
		String removeLienDipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(bankReferenceId,
				bRequest.getOperation(), BankTransactionType.LIEN_REMOVAL.value());
		bRequest.setDipcoinReferenceNumber(removeLienDipcoinReferenceNumber);
		bRequest.setCurrency(Currency.INDIA.value());
		bRequest.setComment(Comment.LIENREMOVE.value());

		LOG.debug(LogFormatter.instance().data("BankRequest", bRequest).format());
		BankRequestContext bankRequestContext = new BankRequestContext();
		bankRequestContext.setTraceId(CoreUtils.randomAlphaString(6));
		this.bankAPIServices.removeLien(bankRequestContext, bRequest);

	}
	
	public DipcoinTransaction saveMarkLienDipcoinTransaction(final Dipcoin dcoin, final User user,
			MarkLienResponse bResponse, boolean rollbackDipcoinTxs, CustomerDipcoinRequest createReq)
			throws InterruptedException, ExecutionException {
		String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());

		String originIp = httpServletContext.getOriginIp();

		DipcoinTransaction dTx = new DipcoinTransaction();
		if (dcoin == null) {
			dTx.setStatus(DipcoinTransactionsStatus.FAILURE.value());
			dTx.setType(DipcoinTransactionType.OTHER_FAILURES.value());
			dTx.setAmount(BigDecimal.ZERO);
			dTx.setDipcoinId(NumberUtils.INTEGER_ZERO);
		} else {
			// add transaction
			dTx = initiateDipcoinTransaction(dcoin, user, originIp);
			dTx.setType(DipcoinTransactionType.GENERATE_DIPCOIN.value());
			dTx.setDipcoinTransactionRefId(bResponse.getDipcoinReferenceNumber());
			dTx.setStatus(DipcoinTransactionsStatus.SUCCESS.value());
			dTx.setSource(TransactionSource.DEFAULT.value());
			if (createReq.getUsageType() == DBConstants.DipcoinUsageType.WALLET.value() || createReq.getUsageType()
					.equals(DBConstants.DipcoinUsageType.RESTRICTED_BBPS_FUND_TRANSFER.value())) {
				dTx.setOrderId(createReq.getOrderId());
				dTx.setPartnerTransactionReferenceId(createReq.getPartnerTransactionReferenceId());
			}
			if (!StringUtils.isEmpty(createReq.getNoOfInvoice())) {
				dTx.setNoOfInvoice(createReq.getNoOfInvoice());
			}
			if (!StringUtils.isEmpty(createReq.getOrderId())) {
				dTx.setOrderId(createReq.getOrderId());
			}
			if (!StringUtils.isEmpty(createReq.getMerchantName())) {
				dTx.setMerchantName(createReq.getMerchantName());
			}
			if (!StringUtils.isEmpty(createReq.getPartnerTransactionReferenceId())) {
				dTx.setPartnerTransactionReferenceId(createReq.getPartnerTransactionReferenceId());
			}

			if (!StringUtils.isEmpty(createReq.getPaymentMode())) {
				dTx.setPaymentMode(createReq.getPaymentMode());
			}

			LOG.error(
					LogFormatter.instance(httpServletContext.getTraceId()).data("DTX added for Partner", dTx).format());
		}

		dTx.setRequestTime(requestTime);
		dTx.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
		if ((dTx = (rollbackDipcoinTxs ? this.coinDBService.addTransaction(dTx)
				: this.coinDBService.asyncAddTransaction(dTx).get())) == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Failed to add Dipcoin Transaction").data("transaction", dTx).format());
		}

		return dTx;
	}

	/*
	 * 
	 */
	public static DipcoinTransaction initiateDipcoinTransaction(final Dipcoin dcoin, final User user, String originIp) {
		DipcoinTransaction transaction = new DipcoinTransaction();
		String now = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
		transaction.setAmount(dcoin.getAmount());
		transaction.setDipcoinId(dcoin.getId());
		transaction.setUpdateDate(now);
		transaction.setIPAddress(originIp);
		transaction.setUser(user);
		if (dcoin.getCustomerAccount() != null)
			transaction.setCustomerAccountId(dcoin.getCustomerAccount().getId());

		return transaction;
	}
	
	public BankTransaction saveUsageTypeBasedBankTransaction(final BankTransaction bTx, final DipcoinTransaction dTx,
			final Dipcoin dcoin, final User user, CustomerDipcoinRequest createReq, Bank bank)
			throws InterruptedException, ExecutionException {
		String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());

		if (bTx == null && createReq.getUsageType() == DBConstants.DipcoinUsageType.RESTRICTED_BBPS_CASH.value()) {
			BankTransaction bnkTx = new BankTransaction();
			bnkTx.setRequestTime(requestTime);
			bnkTx.setType(DBConstants.BankTransactionType.BBPS_CASH.value());
			bnkTx.setStatus(DBConstants.BankTransactionsStatus.SUCCESS.value());
			bnkTx.setAmount(createReq.getAmount());
			bnkTx.setDipcoinTransactionId(dTx.getId());
			bnkTx.setDipcoinTransactionRefId(dTx.getDipcoinTransactionRefId());
			bnkTx.setDipcoinId(dcoin.getId());
			bnkTx.setCustomerAccountId(dTx.getCustomerAccountId());
			bnkTx.setBankId(bank.getId());
			return bnkTx;
		}

		bTx.setDipcoinId(dcoin.getId());
		bTx.setDipcoinTransactionId(dTx.getId());
		return bTx;
	}
	
	/*
	   *
	   */
	public static void populateCustomerDipcoinResponse(final EncryptionResource encryptionResource, final User user,
			final Dipcoin dcoin, final Merchant merchant, final DipcoinTransaction transaction,
			CustomerDipcoinResponse response, final CoreUtils coreUtils, final Recharge recharge,
			final boolean encryptDipcoin) throws APIException, Exception {

		if (dcoin != null) {
			if (response == null)
				response = new CustomerDipcoinResponse();

			response.setCardId(dcoin.getCustomerAccount().getUserCardId())
					.setAccountNumber(dcoin.getCustomerAccount().getAccountNumber())
					.setBankName(dcoin.getCustomerAccount().getBank().getName())
					.setLastUpdatedTime(dcoin.getUpdateTime()).setUsageCategoryId(dcoin.getUsageCategory())
					.setMaskedLogin(dcoin.getCustomerAccount().getLogin());
			response.setMode(dcoin.getCustomerAccount().getTypeOfMethod());

			if (!StringUtils.isEmpty(dcoin.getCustomerAccount().getBank().getApiCustomization())) {
				Object alpha = objectMapper.readValue(dcoin.getCustomerAccount().getBank().getApiCustomization(),
						Object.class);
				response.setApiCustomization((List<APICustomization>) alpha);

			}

			// if (coreUtils != null)
			// response.setUsageCategory(coreUtils.getDipcoinCategory(dcoin.getUsageCategory()));
			response.setUsageType(dcoin.getUsageType());
			response.setUpdateTime(dcoin.getUpdateTime());

			String token = user.getPhone() + dcoin.getCoin();
			if (encryptDipcoin) {
				// encrypt dipcoin
				// @NOTE - do not pass merchant since this is customer dipcoin
				String encDcoin = encryptionResource.encrypt(user, null, null, token);
				response.setEncryptedOsta(encDcoin);
			}

			// base settings
			response.setExpiryTime(dcoin.getExpiryTime()).setAmount(dcoin.getAmount().toString())
					.setOsta(encryptDipcoin && dcoin.getUsageType() == DBConstants.DipcoinUsageType.TOLL.value()
							? CoreUtils.maskedDipcoin(token)
							: token)
					.setStatus(dcoin.getStatus()).setIssueTime(dcoin.getIssueTime());

			if (transaction != null) {
				response.setOstaTransactionReferenceId(transaction.getDipcoinTransactionRefId());
			}

			if (merchant == null)
				return;

			if (transaction != null && transaction.getPartnerReferenceId().equals(merchant.getReferenceId())
					&& (DipcoinTransactionType.COMPLETELY_USED.equals(transaction.getType())
							|| DipcoinTransactionType.PARTIALLY_USED.equals(transaction.getType())
							|| DipcoinTransactionType.CARDLESS_CASH_WITHDRAWAL.equals(transaction.getType()))) {
				response.setUsedAmount(transaction.getAmount().toString());
				response.setUsedTime(transaction.getRequestTime());
				response.setOrderStatus(transaction.getStatus());
				response.setOstaTransactionReferenceId(transaction.getDipcoinTransactionRefId());
				response.setMerchantName(merchant.getName());
				if (StringUtils.isNotEmpty(transaction.getUsageDetails())) {
					response.setUsageDetails(transaction.getUsageDetails());
				}

				if (recharge != null && RechargeConstants.Source.EURONET.equals(recharge.getSource())) {
					response.setRechargeStatus(StringUtils
							.capitalize(RechargeConstants.RechargeStatus.nameOf(recharge.getStatus()).toLowerCase()));
					response.setRechargeBillPayMessage(recharge.getResponseMessage());
					response.setRechargeBillPaymentReferenceId(recharge.getPaymentRefNo());
				} else if (recharge != null
						&& RechargeConstants.Source.BHARAT_BILL_PAYMENT_SYSTEM.equals(recharge.getSource())) {
					response.setBillPayStatus(StringUtils
							.capitalize(RechargeConstants.RechargeStatus.nameOf(recharge.getStatus()).toLowerCase()));
					response.setRechargeBillPayMessage(recharge.getResponseMessage());
					response.setRechargeBillPaymentReferenceId(recharge.getPaymentRefNo());
				}

				if (DBConstants.MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.equals(merchant.getBusinessSegment())
						&& recharge == null && transaction.getPartnerReferenceId().equals(merchant.getReferenceId())
						&& StringUtils.isNotEmpty(transaction.getDipcoinTransactionRefId())) {
					response.setRechargeBillPayStatus(
							StringUtils.capitalize(RechargeConstants.BillPaymentStatus.PENDING.value().toLowerCase()));
					response.setRechargeBillPayMessage(HeaderCode.BILL_PAYMENT_STATUS_CHECK.message());
				}
			}
		}
	}

	/**
	 * Customer Delete Dipcoin
	 *
	 * @return
	 * @throws APIException
	 */
	public ResponseEntity deleteDipcoin(final User user, final String encDcoin, final boolean encryptDcoin)
			throws Exception, APIException {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Encrypted Coin", encDcoin).format());

		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		String decDcoin = encryptDcoin ? encryptionResource.decrypt(user, null, null, encDcoin) : encDcoin;
		if (decDcoin == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.INVALID_ENCRYPTED_DATA));
		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Decrypted Coin", encDcoin).format());

		// parse token
		Either<ResponseEntity, String> validDipcoin = validateDipcoin(decDcoin, user);
		if (validDipcoin.isLeft()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to validate dipcoin")
					.format());
			return validDipcoin.getLeft();
		}

		// get dipcoin by token value
		Dipcoin dcoinToDelete = this.coinDBService.getCoin(user.getId(), validDipcoin.get(), true);
		if (dcoinToDelete == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.DIPCOIN_INVALID));
		}

		// only active dipcoin can be deleted
		if (!DipcoinStatus.ACTIVE.equals(dcoinToDelete.getStatus())) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.DIPCOIN_NOT_ACTIVE));
		}

		if (DipcoinUsageType.TOLL.value() == dcoinToDelete.getUsageType()) {
			dcoinToDelete.setClientTransactionId(httpServletContext.getClientTransactionId());
		}

		return deleteDipcoin(user, dcoinToDelete, Optional.empty());
	}
	
	protected ResponseEntity deleteDipcoin(final User user, final Dipcoin dcoinToDelete,
			final Optional<User> delegateUser) throws Exception, APIException {

		String originIp = httpServletContext.getOriginIp();

		final long startTime = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay().getMillis();
		final long startOfThisMonth = DateTime.now(DateTimeZone.UTC).dayOfMonth().withMinimumValue()
				.withTimeAtStartOfDay().getMillis();
		final long startOfThisYear = DateTime.now(DateTimeZone.UTC).dayOfYear().withMinimumValue()
				.withTimeAtStartOfDay().getMillis();

		// verify user and account are associated
		CustomerAccount dcoinToDeleteCustomerAccount = dcoinToDelete.getCustomerAccount();
		if (dcoinToDeleteCustomerAccount.getUser().getId() != user.getId()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		// verify if dipcoin is expired.
		if (new DateTime(Long.parseLong(dcoinToDelete.getExpiryTime())).isBeforeNow()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.DIPCOIN_EXPIRED));
		}

		// get lien marked bank tx's associated with origin dipcoin
		final List<Integer> bTxTypes = new LinkedList<>(
				Arrays.asList(BankTransactionType.LIEN_MARK.value(), BankTransactionType.RELIEN_MARK.value()));
		LOG.info(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching LienMark BankTransaction")
				.data("dipcoinId", dcoinToDelete.getId()).format());
		List<BankTransaction> bTxs = bankDBService.asyncGetTransactionsByDipcoinIdsAndTypes(
				Arrays.asList(dcoinToDelete.getId()), bTxTypes, BankTransactionsStatus.SUCCESS.value()).get();
		if (CollectionUtils.isEmpty(bTxs) && dcoinToDelete.getParentDipcoinId() <= 0) {
			// if no BTX and a origin coin exit
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}

		/*
		 * 1. In case of parent, lienMarkedDcoin == dcoinToDelete
		 * 
		 * 2. In case of child whose parent is processed on previous day offline phase,
		 * lienMarkedDcoin = dcoinToDelete
		 * 
		 * 3. In case of child whose parent is not processed on previous day offline
		 * phase. lienMarkedDcoin = ParentOf(dcoinToDelete)
		 */
		Dipcoin lienMarkedDcoin = dcoinToDelete;

		// init new lien amount
		BigDecimal lienAmount = new BigDecimal(0);

		// if lien mark BTX doesnt exists for dcoinToDelete and is child dipcoin,
		// process hierarchy
		if (CollectionUtils.isEmpty(bTxs) && dcoinToDelete.getParentDipcoinId() > 0) {
			LOG.info(LogFormatter.instance(httpServletContext.getTraceId()).message("Processing child dipcoin delete")
					.format());
			// fetch child parents e.g dIds = 4(child deleted) -> 3 -> 2(offline processed)
			// -> 1(root)
			List<Dipcoin> parents = new LinkedList<>();
			List<Integer> parentIds = new LinkedList<>();
			int pId = dcoinToDelete.getParentDipcoinId();
			while (pId > 0) {
				parentIds.add(pId);
				LOG.info(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching parent dcoin")
						.data("parentDipcoinId", pId).format());
				Dipcoin dc = coinDBService.getById(pId, false);
				if (dc == null) {
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
				}
				parents.add(dc);
				pId = dc.getParentDipcoinId();

				LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Fetching parent dcoin LienMark BTX").data("dipcoinId", dc.getId()).format());
				// bTxs = bankDBService.asyncGetTransactions(dc, bTxTypes).get();

				bTxs = bankDBService.asyncGetTransactionsByDipcoinIdsAndTypes(Arrays.asList(dc.getId()), bTxTypes,
						BankTransactionsStatus.SUCCESS.value()).get();

				// if marklien BXT encountered for a parent stop the search
				if (!CollectionUtils.isEmpty(bTxs)) {
					break;
				}
			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Fetched parent dipcoins " + Arrays.asList(parentIds)).format());

			// fetch dipcoin to remove lien and lazy load customeraccount
			lienMarkedDcoin = parents.remove(parents.size() - 1); // remove origin dipcoin from the list
			// ie. dId = 1(origin)
			Hibernate.initialize(lienMarkedDcoin.getCustomerAccount());

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Parent LienMarked dipcoin " + lienMarkedDcoin.getId()).format());

			// fetch parent used/partially-used DTX's i.e dIds = 3 & 2
			final List<Integer> dTxTypes = Arrays.asList(DipcoinTransactionType.COMPLETELY_USED.value(),
					DipcoinTransactionType.PARTIALLY_USED.value());
			List<DipcoinTransaction> parentDTxs = coinDBService.getTransactions(parentIds, dTxTypes, null, null);
			if (CollectionUtils.isEmpty(parentDTxs)) {
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}

			// fetch associated merchant recon records
			List<Integer> parentDTxsIds = new LinkedList<>();
			for (DipcoinTransaction dTx : parentDTxs) {
				parentDTxsIds.add(dTx.getId());
			}
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("parentDTxsIds", parentDTxsIds)
					.format());
			List<MerchantRecon> merchantRecons = merchantDBService.asyncGetMerchantRecons(parentDTxsIds, null, null)
					.get();
			Map<Integer, MerchantRecon> reconLookup = new HashMap<>();
			if (!CollectionUtils.isEmpty(merchantRecons)) {
				for (MerchantRecon recon : merchantRecons) {
					reconLookup.put(recon.getDipcoinTransactionId(), recon);
				}
			}

			// sum parents dtx amounts i.e dId's = 3 & 2
			for (DipcoinTransaction dTx : parentDTxs) {
				// @TODO - update constants
				// if merchant recon issue, i.e non-0, dont consider dtx for remark
				if (reconLookup.containsKey(dTx.getId()) && reconLookup.get(dTx.getId()).getIssue() != 0) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Skipping DTX")
							.data("dId", dTx.getDipcoinId()).data("dTx", dTx.getId())
							.data("MerchantReconIssue", reconLookup.get(dTx.getId()).getIssue())
							.data("MerchantReconStatus", reconLookup.get(dTx.getId()).getStatus()).format());
					continue;
				}
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Processing DTX")
						.data("dTx", dTx.getId()).data("amount", dTx.getAmount()).format());
				lienAmount = lienAmount.add(dTx.getAmount());
			}
		}

		// customer account associated with lienmarked dipcoin
		CustomerAccount originDcoinCustomerAccount = lienMarkedDcoin.getCustomerAccount();
		LOG.info(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetched LienMark CustomerAccount")
				.data("dipcoinId", lienMarkedDcoin.getId())
				.data("originDcoinCustomerAccount", originDcoinCustomerAccount.getId())
				.data("bankReferenceId", originDcoinCustomerAccount.getBank().getReferenceId())
				.data("BUID", originDcoinCustomerAccount.getBankUId()).format());

		// Sort bTxs by requestTime in desc order.
		Collections.sort(bTxs, (bTx1, bTx2) -> bTx2.getRequestTime().compareTo(bTx1.getRequestTime()));

		// fetch the corresponding Lien Mark BTX to be processed
		BankTransaction lienMarkedBTx = bTxs.get(0);
		LOG.info(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetched LienMark BankTransaction")
				.data("dipcoinId", lienMarkedDcoin.getId()).data("lienMarkedBTx", lienMarkedBTx.getId())
				.data("lienAmount", lienAmount).format());

		// remove previously marked lien
		User owner = delegateUser != null ? delegateUser.orElse(user) : user;
		removeLien(owner, originDcoinCustomerAccount, lienMarkedDcoin, dcoinToDelete, lienMarkedBTx, originIp);

		// if child dipcoin deleted and lien mark amount set, mark a new lien
		remarkLien(originDcoinCustomerAccount, lienMarkedDcoin, dcoinToDelete, lienMarkedBTx, lienAmount);

		// mark the parent OR child dipcoin as cancelled
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Marking dcoin as cancelled")
				.data("dipcoinId", dcoinToDelete.getId()).format());
		Dipcoin updatedDcoin = this.coinDBService.updateCoin(dcoinToDelete, DipcoinStatus.CANCELLED.value());
		if (updatedDcoin != null) {

			if (Long.valueOf(dcoinToDelete.getIssueTime()) >= startTime) {
				user.setPerDayLimit(user.getPerDayLimit().subtract(dcoinToDelete.getAmount()));
				user.setPerMonthLimit(user.getPerMonthLimit().subtract(dcoinToDelete.getAmount()));
				user.setPerYearLimit(user.getPerYearLimit().subtract(dcoinToDelete.getAmount()));
			} else if (Long.valueOf(dcoinToDelete.getIssueTime()) >= startOfThisMonth) {
				user.setPerMonthLimit(user.getPerMonthLimit().subtract(dcoinToDelete.getAmount()));
				user.setPerYearLimit(user.getPerYearLimit().subtract(dcoinToDelete.getAmount()));
			} else if (Long.valueOf(dcoinToDelete.getIssueTime()) >= startOfThisYear) {
				user.setPerYearLimit(user.getPerYearLimit().subtract(dcoinToDelete.getAmount()));
			}

			this.userDBService.updateUser(user);

			CustomerDipcoinResponse response = new CustomerDipcoinResponse();
			response.setOsta(updatedDcoin.getCoin());
			response.addHeaderCode(HeaderCode.DIPCOIN_DELETED);
			response.setBankName(dcoinToDeleteCustomerAccount.getBank().getName());
			response.setAmount(updatedDcoin.getAmount().toString());
			response.setIssueTime(updatedDcoin.getIssueTime());
			response.setExpiryTime(updatedDcoin.getExpiryTime());
			response.setUpdateTime(updatedDcoin.getUpdateTime());

			// initialize bank if not done.
			if (!Hibernate.isInitialized(originDcoinCustomerAccount.getBank())) {
				Hibernate.initialize(originDcoinCustomerAccount.getBank());
			}

			Bank bank = originDcoinCustomerAccount.getBank();

			// send email to user which do not have customer account of virtual bank//
			/*
			 * as virtual bank and fastag can only have one osta at a time during auto topup
			 * lien remove is done this cancellation mail and message should not go to
			 * customer as it might confuse him or her as it is not authorised by him or her
			 * but internal system does that
			 * 
			 */
			if (applicationProperties.enableSMS() && !(BankType.VIRTUAL_BANK.equals(bank.getType())
					|| APIUtils.smsEmailAreNotAllowed(updatedDcoin.getUsageType()))) {
				if (user.getIsEmailVerified() == BooleanStatus.YES.value()
						&& !emailUtils.sendDeleteDipcoinEmail(user, dcoinToDeleteCustomerAccount, response)) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message(
							"Failed to send email to user " + user.getId() + " for dipcoin " + updatedDcoin.getId())
							.format());
				}

				// send SMS
				if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(user.getPhone(),
						Templates.CustomerDeleteDipcoin.format(response.getOsta(), response.getUpdateTime()),
						httpServletContext.getClientFeatureFlags().smsEnabled())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", user.getPhone()).data("template", Templates.CustomerDeleteDipcoin
									.format(response.getOsta(), response.getUpdateTime()))
							.format());
				}

				if (applicationProperties.getAwsSMSClient()) {

					NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
					notificationRequestContext.setTraceId(httpServletContext.getTraceId());
					if (!notificationResource.sendSms(user.getPhone(),
							Templates.CustomerDeleteDipcoin.format(response.getOsta(), response.getUpdateTime()),
							httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
								.data("phone", user.getPhone()).data("template", Templates.CustomerDeleteDipcoin
										.format(response.getOsta(), response.getUpdateTime()))
								.format());
					}
				}
			}
			// register metric
			dipcoinMetricRegistry.dipcoinDeleted().increment();
			return ResponseEntity.ok(response);
		}

		LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to delete dipcoin").format());
		// throw exception to initiate rollback
		dipcoinMetricRegistry.dipcoinFailedToDelete().increment();
		throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.INTERNAL_ERROR));
	}
	
	public Either<ResponseEntity, String> validateDipcoin(String dcoin, final User user) {
		// parse token
		Pair<String, String> dcoinSegments = CoreUtils.parseDipcoinToken(dcoin);
		if (dcoinSegments == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid Dipcoin")
					.data("dcoin", dcoin).format());
			return Either.left(
					ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.DIPCOIN_INVALID)));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("DcoinSegments")
				.data("phone", dcoinSegments.getLeft()).data("token", dcoinSegments.getRight()).format());

		// validate token belongs to the user
		if (!user.getPhone().equalsIgnoreCase(dcoinSegments.getLeft())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Unauthorized Dipcoin")
					.data("userPhone", user.getPhone()).data("phone", dcoinSegments.getLeft()).format());
			return Either.left(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED)));
		}

		return Either.right(dcoinSegments.getRight());
	}
	
	public void removeLien(final User user, final CustomerAccount originDcoinCustomerAccount,
			final Dipcoin lienMarkedDcoin, final Dipcoin dcoinToDelete, final BankTransaction lienMarkedBTx,
			String originIp)
			throws APIException, BankServiceException, InterruptedException, ExecutionException, IOException {

		String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());

		RemoveLienRequest bRequest = new RemoveLienRequest(originDcoinCustomerAccount.getBank().getReferenceId(),
				originDcoinCustomerAccount.getBank().getCode());
		bRequest.setTransactionType(Integer.toString(BankTransactionType.LIEN_REMOVAL.value()));
		bRequest.setBankUID(originDcoinCustomerAccount.getBankUId());
		if (bankAPIServices.getBankProperties(originDcoinCustomerAccount.getBank().getReferenceId())
				.isOstaSideAccountVerification()) {
			bRequest.setAmount("0");
		} else {
			bRequest.setAmount(lienMarkedBTx.getAmount().toString());
		}
		bRequest.setCbsJournalNumber(lienMarkedBTx.getCBSReferenceID());
		bRequest.setCbsDate(lienMarkedBTx.getCBSLienDate());
		String dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(
				originDcoinCustomerAccount.getBank().getReferenceId(), bRequest.getOperation(),
				BankTransactionType.LIEN_REMOVAL.value());
		bRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
		bRequest.setCurrency(Currency.INDIA.value());
		bRequest.setComment(Comment.LIENREMOVE.value());
		bRequest.setCbsDate(lienMarkedBTx.getCBSLienDate());
		// @Populate pending fields

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankRequest", bRequest).format());
		BankRequestContext bankRequestContext = new BankRequestContext();
		bankRequestContext.setTraceId(httpServletContext.getTraceId());
		Future<RemoveLienResponse> bResponseTask = this.bankAPIServices.removeLien(bankRequestContext, bRequest);

		// init bank transaction
		BankTransaction bTx = new BankTransaction();
		bTx.setType(BankTransactionType.LIEN_REMOVAL.value());
		bTx.setDipcoinId(lienMarkedDcoin.getId()); // remove lien on the lien marked dipcoin
		bTx.setCustomerAccountId(originDcoinCustomerAccount.getId());
		bTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
		bTx.setAmount(lienMarkedBTx.getAmount());
		bTx.setBankId(originDcoinCustomerAccount.getBank().getId());

		RemoveLienResponse bResponse = bResponseTask.get();
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankResponse", bResponse)
				.data("DipcoinReferenceNumber", dipcoinReferenceNumber).format());

		/*
		 * If success for Bank, mark Osta as deleted. If logical failure from Bank, keep
		 * Osta active. For all other failures, mark Osta as deleted.
		 */
		if (bResponse == null || (!BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode()))
				|| !dipcoinReferenceNumber.equals(bResponse.getDipcoinReferenceNumber())) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to remove lien").format());
			bTx.setStatus(BankTransactionsStatus.FAILED.value());
			if (bResponse != null) {
				bTx.setRequestTime(bResponse.getRequestTime());
				bTx.setResponseTime(bResponse.getResponseTime());
				bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
				bTx.setRawBankResponse(bResponse.getBankResponseDesc() != null ? bResponse.getBankResponseDesc()
						: bResponse.getErrorMsg());
				bTx.setBankResponseCode(bResponse.getBankResponseCode());
			}
			if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to add Bank Transaction").data("transaction", bTx).format());
			}

			// throw exception to initiate rollback
			if (bResponse == null) {
				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
						APIResponse.error(HeaderCode.BANK_FAILED_TO_REMOVE_LIEN));
			}

			if (bResponse.getBankResponseDesc() != null
					&& StringUtils.isNotBlank(
							bankAPIServices.getBankProperties(originDcoinCustomerAccount.getBank().getReferenceId())
									.getHoldNotExistCode())
					&& (bResponse.getBankResponseCode().toUpperCase()
							.contains(bankAPIServices
									.getBankProperties(originDcoinCustomerAccount.getBank().getReferenceId())
									.getHoldNotExistCode().toUpperCase())
							|| bResponse.getBankResponseDesc().toUpperCase()
									.contains(bankAPIServices
											.getBankProperties(originDcoinCustomerAccount.getBank().getReferenceId())
											.getHoldNotExistCode().toUpperCase()))) {

				ResponseEntity lienMarkResponse = dipcoinBankHelper.markLien(user, originDcoinCustomerAccount,
						lienMarkedDcoin, lienMarkedBTx, bankRequestContext);

				if (lienMarkResponse.getStatusCodeValue() == HttpStatus.OK.value()) {
					throw new APIException(HttpStatus.BAD_REQUEST, (APIResponse) lienMarkResponse.getBody());
				}

				throw new APIException(HttpStatus.BAD_REQUEST,
						APIResponse.error(HeaderCode.BANK_FAILED_HOLD_DOES_NOT_EXIT));
			}

			// throw exception to initiate rollback
			throw new APIException(HttpStatus.BAD_REQUEST, APIResponse.error(HeaderCode.BANK_FAILED_TO_REMOVE_LIEN));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank Services Lien Removal")
				.data("requestTime", bResponse.getRequestTime()).data("responseTime", bResponse.getResponseTime())
				.format());

		// add dipcoin transaction for dipcoin to be deleted
		DipcoinTransaction dTx = initiateDipcoinTransaction(dcoinToDelete, user, originIp);
		dTx.setType(DipcoinTransactionType.DELETE_DIPCOIN.value());
		dTx.setDipcoinTransactionRefId(bResponse.getDipcoinReferenceNumber());
		dTx.setStatus(DipcoinTransactionsStatus.SUCCESS.value());
		dTx.setRequestTime(requestTime);
		dTx.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
		dTx.setSource(TransactionSource.DEFAULT.value());
		if (this.coinDBService.asyncAddTransaction(dTx).get() == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Failed to add Dipcoin Transaction").data("transaction", dTx).format());
		}

		// populate bank transaction
		bTx.setRawBankRequest(bResponse.getRawRequest());
		bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
		bTx.setRawBankResponse(bResponse.getRawData());
		bTx.setBankResponseCode(bResponse.getBankResponseCode());
		bTx.setStatus(BankTransactionsStatus.SUCCESS.value());
		bTx.setRequestTime(bResponse.getRequestTime());
		bTx.setResponseTime(bResponse.getResponseTime());

		if (!StringUtils.isEmpty(bResponse.getCbsJournalNumber()))
			bTx.setCBSReferenceID(bResponse.getCbsJournalNumber());

		if (dTx != null) {
			bTx.setDipcoinTransactionId(dTx.getId());
		}
		if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to add Bank Transaction")
					.data("transaction", bTx).format());
		}
	}
	
	public void remarkLien(final CustomerAccount originDcoinCustomerAccount, final Dipcoin lienMarkedDcoin,
			final Dipcoin dcoinToDelete, final BankTransaction lienMarkedBTx, BigDecimal lienAmount)
			throws InterruptedException, ExecutionException, BankServiceException, APIException, IOException {

		if (dcoinToDelete.getParentDipcoinId() > 0 && lienAmount.doubleValue() > 0) {
			LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Remarking lien of amount " + lienAmount).format());

			String dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(
					originDcoinCustomerAccount.getBank().getReferenceId(), Operation.MARK_LIEN,
					BankTransactionType.LIEN_MARK.value());

			MarkLienRequest bRequest = new MarkLienRequest(originDcoinCustomerAccount.getBank().getReferenceId(),
					originDcoinCustomerAccount.getBank().getCode());
			bRequest.setAmount(CoreUtils.round2(lienAmount).toString());
			bRequest.setTransactionType(Integer.toString(BankTransactionType.LIEN_MARK.value()));
			bRequest.setBankUID(originDcoinCustomerAccount.getBankUId());
			bRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
			bRequest.setCurrency(Currency.INDIA.value());
			bRequest.setComment(Comment.LIENMARK.value());
			bRequest.setCbsJournalNumber(lienMarkedBTx.getCBSReferenceID());
			bRequest.setUserId(String.valueOf(originDcoinCustomerAccount.getUser().getId()));
			bRequest.setCardId(String.valueOf(originDcoinCustomerAccount.getUserCardId()));
			bRequest.setAccountId(String.valueOf(originDcoinCustomerAccount.getId()));

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankRequest", bRequest).format());
			BankRequestContext bankRequestContext = new BankRequestContext();
			bankRequestContext.setTraceId(httpServletContext.getTraceId());
			MarkLienResponse bResponse = this.bankAPIServices.markLien(bankRequestContext, bRequest).get();
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankResponse", bResponse)
					.data("DipcoinReferenceNumber", dipcoinReferenceNumber).format());

			// clone existing bank Tx
			BankTransaction nBTx = new BankTransaction();
			nBTx.setDipcoinId(lienMarkedDcoin.getId());
			nBTx.setAmount(lienAmount);
			nBTx.setType(BankTransactionType.RELIEN_MARK.value());
			nBTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
			nBTx.setCustomerAccountId(originDcoinCustomerAccount.getId());
			nBTx.setBankId(originDcoinCustomerAccount.getBank().getId());

			// @TODO - system failure
			if (bResponse == null || !BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode())
					|| !dipcoinReferenceNumber.equals(bResponse.getDipcoinReferenceNumber())) {
				LOG.error(
						LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to mark lien").format());

				// write failed bank tx
				nBTx.setStatus(BankTransactionsStatus.FAILED.value());
				if (bResponse != null) {
					nBTx.setRequestTime(bResponse.getRequestTime());
					nBTx.setResponseTime(bResponse.getResponseTime());
					nBTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
					nBTx.setRawBankResponse(bResponse.getBankResponseDesc() != null ? bResponse.getBankResponseDesc()
							: bResponse.getErrorMsg());
					nBTx.setBankResponseCode(bResponse.getBankResponseCode());
				}
				LOG.debug(
						LogFormatter.instance(httpServletContext.getTraceId()).data("BankTransaction", nBTx).format());
				if (this.bankDBService.addTransaction(nBTx) == null) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to add Bank Transaction").data("transaction", nBTx).format());
				}

				// throw exception to initiate rollback
				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
						APIResponse.error(HeaderCode.BANK_FAILED_TO_MARK_LIEN));
			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank Services Mark Lien")
					.data("requestTime", bResponse.getRequestTime()).data("responseTime", bResponse.getResponseTime())
					.format());

			// check conditions which are mandated for dipcoin creating
			if (StringUtils.isEmpty(bResponse.getCbsJournalNumber())) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to obtain CbsJournalNumber").format());
				// throw exception to initiate rollback
				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.INTERNAL_ERROR));
			}

			// populate bank transaction
			nBTx.setRawBankRequest(bResponse.getRawRequest());
			nBTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
			nBTx.setRawBankResponse(bResponse.getRawData());
			nBTx.setBankResponseCode(bResponse.getBankResponseCode());
			nBTx.setStatus(BankTransactionsStatus.SUCCESS.value());
			nBTx.setRequestTime(bResponse.getRequestTime());
			nBTx.setResponseTime(bResponse.getResponseTime());
			nBTx.setCBSReferenceID(bResponse.getCbsJournalNumber());
			// reassign origin Dipcoin TX to new Bank Tx
			nBTx.setDipcoinTransactionId(lienMarkedBTx.getDipcoinTransactionId());
			nBTx.setCBSLienDate(bResponse.getCbsDate());

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankTransaction", nBTx).format());
			if (this.bankDBService.addTransaction(nBTx) == null) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to add Bank Transaction").data("transaction", nBTx).format());
			}
		}
	}

	public ResponseEntity getTollDipcoinHierarchy(final User user, final String encCardId, final boolean encryptDipcoin)
			throws Exception {

		String originIp = httpServletContext.getOriginIp();
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Encrypted CarId", encCardId).format());
		String decCarId = encryptDipcoin ? encryptionResource.decrypt(user, null, null, encCardId) : encCardId;
		if (decCarId == null) {

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.INVALID_ENCRYPTED_DATA));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("TollOsta Account")
				.data("UserId", user.getId()).data("CardId", decCarId).format());

		CustomerAccount customerAccount = this.customerDBService.getAccount(user.getId(), Integer.parseInt(decCarId));

		// Card Id is not found
		if (customerAccount == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
		}

		// Check the UserId and DipcoinCreated User Id is Same
		if (user.getId() != customerAccount.getUser().getId()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		Dipcoin dipcoin = coinDBService.findDipcoin(customerAccount.getId(), DBConstants.DipcoinStatus.ACTIVE.value(),
				DBConstants.DipcoinUsageType.TOLL.value());

		if (dipcoin == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.DIPCOIN_DOESNT_EXIST));
		}

		return getDipcoinHierarchy(user, user.getPhone().concat(dipcoin.getCoin()), encryptDipcoin);

	}
	
	/*
	   *
	   */
	public ResponseEntity getDipcoinHierarchy(final User user, final String encDcoin, final boolean encryptDipcoin)
			throws Exception {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Encrypted Coin", encDcoin).format());
		if (!this.userDBService.isCustomer(user)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		String decDcoin = encryptDipcoin ? encryptionResource.decrypt(user, null, null, encDcoin) : encDcoin;
		if (decDcoin == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.INVALID_ENCRYPTED_DATA));
		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Decrypted Coin", decDcoin).format());

		// validate dipcoin
		Either<ResponseEntity, String> validDipcoin = validateDipcoin(decDcoin, user);
		if (validDipcoin.isLeft()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to validate dipcoin")
					.format());
			return validDipcoin.getLeft();
		}

		Dipcoin dcoin = this.coinDBService.getCoin(user.getId(), validDipcoin.get(), true);
		if (dcoin == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.DIPCOIN_INVALID));
		}

		if (dcoin.getCustomerAccount().getUser().getId() != user.getId()) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("User Id", user.getId())
					.data("Expected Id", dcoin.getCustomerAccount().getUser().getId()).format());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		}

		Integer pId = dcoin.getParentDipcoinId();
		Integer dcoinId = dcoin.getId();
		List<DipcoinResponse> dipcoins = new LinkedList<>();

		if (pId > 0) {
			LOG.info(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching parent dcoin")
					.data("dcoin", dcoin.getId()).format());
			List<Dipcoin> parents = coinDBService.getParentDipcoins(dcoinId);
			if (!CollectionUtils.isEmpty(parents)) {
				for (int i = parents.size() - 1; i >= 0; i--) {
					Dipcoin parentDcoin = parents.get(i);
					dipcoins.add(populateDipcoinResponse(user, parentDcoin));
				}
			}
		}
		dipcoins.add(populateDipcoinResponse(user, dcoin));

		LOG.info(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching child dcoin")
				.data("dcoin", dcoin.getId()).format());
		List<Dipcoin> children = coinDBService.getChildrenDipcoins(dcoinId);
		if (!CollectionUtils.isEmpty(children)) {
			for (int i = 0; i < children.size(); i++) {
				Dipcoin childDcoin = children.get(i);
				dipcoins.add(populateDipcoinResponse(user, childDcoin));
			}
		}
		List<APIResponse> responses = new LinkedList<APIResponse>();
		responses.addAll(dipcoins);
		return ResponseEntity.ok(responses);
	}
	
	/*
	   *
	   */
	private DipcoinResponse populateDipcoinResponse(final User user, final Dipcoin dcoin) throws Exception {
		CustomerDipcoinResponse response = new CustomerDipcoinResponse();
		Merchant merchant = null;
		DipcoinTransaction transaction = null;
		// @TODO - paginate here if performance impact. Use async calls to fetch
		// results in parallel.
		List<Integer> types = new LinkedList<>();
		types.add(DipcoinTransactionType.COMPLETELY_USED.value());
		types.add(DipcoinTransactionType.PARTIALLY_USED.value());
		types.add(DipcoinTransactionType.CARDLESS_CASH_WITHDRAWAL.value());
		List<DipcoinTransaction> transactions = this.coinDBService.getTransactions(dcoin, types, null, null);
		Recharge recharge = null;
		if (!CollectionUtils.isEmpty(transactions)) {
			try {
				transaction = transactions.get(0);
				merchant = merchantDBService.asyncGetMerchant(transaction.getPartnerReferenceId()).get();
			} catch (InterruptedException | ExecutionException e) {
			}

			if (DBConstants.MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.equals(merchant.getBusinessSegment())) {
				recharge = rechargeDBService.getTransactionByRequestType(transaction.getDipcoinTransactionRefId(),
						RechargeConstants.RequestType.SERVICE.value());
			}
		}

		populateCustomerDipcoinResponse(encryptionResource, user, dcoin, merchant, transaction, response, coreUtils,
				recharge, true);

		return response;
	}

}
