package com.dipcoin.api.resource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
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
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIFeatureFlags;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.filter.HttpServletContext.ClientFeatureFlags;
import com.dipcoin.api.fraudMgmt.EventUtils;
import com.dipcoin.api.fraudMgmt.ProcessEvent;
import com.dipcoin.api.fraudMgmt.UserEventResource;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.AspectContext;
import com.dipcoin.api.model.DipcoinApprovalRequest;
import com.dipcoin.api.model.DipcoinThreadLocal;
import com.dipcoin.api.model.PartnerProcessDipcoinRequest;
import com.dipcoin.api.model.PartnerProcessDipcoinResponse;
import com.dipcoin.api.model.PartnerResponse;
import com.dipcoin.api.model.RequestMetadata;
import com.dipcoin.api.model.UserDeviceInfoResponse;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionSettlementDone;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.Session;
import com.dipcoin.db.services.model.User;
import com.dipcoin.metrics.DipcoinMetricRegistry;
import com.dipcoin.metrics.OauthMetricRegistry;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("partnerDipcoinResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class PartnerDipcoinResource extends DipcoinResource {

	private static final Logger LOG = LogManager.getLogger(PartnerDipcoinResource.class);

	private static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private MerchantDBService merchantDBService;

	@Autowired
	private BankDBService bankDBService;

	@Autowired
	private DipcoinDBService coinDBService;

	@Autowired
	private EmailUtils emailUtils;

	@Autowired
	private NotificationResource notificationResource;

	@Autowired
	private APIFeatureFlags apiFeatureFlags;

	@Autowired
	private SmsClient smsClient;

	@Autowired
	private CryptoUtil cryptoUtil;

	@Autowired
	private PartnerEncDecResource partnerEncDecResource;

	@Autowired
	@Qualifier("com.dipcoin.metrics.DipcoinMetricRegistry")
	private DipcoinMetricRegistry dipcoinMetricRegistry;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	@Autowired
	private UserUtil userUtil;

	@Autowired
	private OauthMetricRegistry oauthMetricRegistry;

	@Autowired
	private UserEventResource userEventResource;

	@Autowired
	private ApplicationProperties applicationProperties;

	public void setHttpServletContext(HttpServletContext httpServletContext) {
		super.setHttpServletContext(httpServletContext);
		this.httpServletContext = httpServletContext;
	}

	public EmailUtils getEmailUtils() {
		return emailUtils;
	}

	public PartnerEncDecResource getPartnerEncDecResource() {
		return partnerEncDecResource;
	}

	public UserEventResource getUserEventResource() {
		return userEventResource;
	}

	/**
	 * Partner Process Dipcoin
	 * 
	 */

	public ResponseEntity<APIResponse> processCustomerDipcoin(final User partnerUser, final Merchant merchant,
			final PartnerProcessDipcoinRequest dcoinReq, TransactionSource source, Optional<Dipcoin> existingDcoin,
			final boolean encrypt) throws Exception, APIException {
		return processCustomerDipcoin(partnerUser, null, merchant, dcoinReq, source, existingDcoin, encrypt);
	}

	public ResponseEntity<APIResponse> processCustomerDipcoin(final User partnerUser, final Bank bank,
			final PartnerProcessDipcoinRequest dcoinReq,

			TransactionSource source, final boolean encrypt) throws Exception, APIException {

		return processCustomerDipcoin(partnerUser, bank, null, dcoinReq, source, Optional.empty(), encrypt);
	}

	public ResponseEntity<APIResponse> processCustomerDipcoin(final User partnerUser, final Bank bank,
			final Merchant merchant, final PartnerProcessDipcoinRequest dcoinReq, TransactionSource source,
			final boolean encrypt) throws Exception, APIException {

		return processCustomerDipcoin(partnerUser, bank, merchant, dcoinReq, source, Optional.empty(), encrypt);
	}

	private ResponseEntity<APIResponse> processCustomerDipcoin(final User partnerUser, final Bank bank,
			final Merchant merchant, final PartnerProcessDipcoinRequest dcoinReq, TransactionSource source,
			Optional<Dipcoin> existingDcoin, final boolean encrypt) throws Exception, APIException {

		String originIp = httpServletContext.getOriginIp();
		String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
		String dipcoinReferenceNumber = CoreUtils.generateDipcoinToMerchantReferenceNumber();

		AspectContext ctx = new AspectContext(applicationProperties.getRunAudit());
		RequestMetadata requestMetadata = new RequestMetadata("processCustomerDipcoin");
		ctx.setAudit("ProcessDipcoin");
		DipcoinThreadLocal.set(ctx);

		PartnerProcessDipcoinResponse response = new PartnerProcessDipcoinResponse();
		response.setRequestTime(requestTime);
		response.setOstaTransactionReferenceId(dipcoinReferenceNumber);

		// initiate transaction
		DipcoinTransaction transaction = new DipcoinTransaction();
		transaction.setSettlementDone(DipcoinTransactionSettlementDone.DEFAULT.value());
		transaction.setRequestTime(requestTime);
		transaction.setIPAddress(originIp);
		transaction.setDipcoinTransactionRefId(dipcoinReferenceNumber);
		transaction.setPartnerReferenceId(merchant != null ? merchant.getReferenceId() : bank.getReferenceId());
		transaction.setUser(partnerUser);

		if (dcoinReq == null) {
			saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.BAD_REQUEST);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.BAD_REQUEST);
			}
			if (encrypt) {
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		} else {
			// response fields
			response.setPartnerTransactionReferenceId(dcoinReq.getPartnerTransactionReferenceId());

			// transaction fields
			transaction.setPartnerTransactionReferenceId(dcoinReq.getPartnerTransactionReferenceId());
			transaction.setOrderId(dcoinReq.getOrderId());
			transaction.setAmount(dcoinReq.getAmount());
			transaction.setPartnerRawRequest(dcoinReq.getRawRequest());
			transaction.setSource(source.value());
			transaction.setUsageDetails(usageType(dcoinReq, merchant, bank, source));
			transaction.setComments(dcoinReq.getComment());
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("request", dcoinReq)
				.data("PartnerUser", partnerUser.getId()).data("Merchant", merchant != null ? merchant.getId() : null)
				.data("Bank", bank != null ? bank.getId() : null).format());

		// validate request
		if (!dcoinReq.validate(httpServletContext)) {
			// save failure transaction
			saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.BAD_REQUEST);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));

			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCodes(dcoinReq.getErrorCodes());
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				partnerResponse.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching existing DTX")
				.data("PartnerTransactionReferenceId", dcoinReq.getPartnerTransactionReferenceId())
				.data("PartnerReferenceId", merchant != null ? merchant.getReferenceId() : bank.getReferenceId())
				.format());
		// same transaction requested
		List<DipcoinTransaction> existingDTxs = coinDBService
				.asyncGetPartnerTransactions(merchant != null ? merchant.getReferenceId() : bank.getReferenceId(),
						Arrays.asList(dcoinReq.getPartnerTransactionReferenceId()), null, null, null, null)
				.get();
		if (!CollectionUtils.isEmpty(existingDTxs)) {

			for (DipcoinTransaction dTx : existingDTxs) {
				if (DBConstants.DipcoinTransactionType.PAYMENTGATEWAY_INITIATE.value() != dTx.getType()
						&& DBConstants.DipcoinTransactionType.CHARGE_BACK.value() != dTx.getType()
						&& dTx.getStatus() == DipcoinTransactionsStatus.SUCCESS.value()) {
					response.setOstaTransactionReferenceId(null);
					response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));

					if (source.equals(TransactionSource.OAUTH)) {
						response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
					} else {
						response.addHeaderCode(HeaderCode.BAD_REQUEST);
					}
					if (encrypt) {
						response.setAmount(dcoinReq.getAmount().toString());
						PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse(
								merchant.getReferenceId(), objectMapper.writeValueAsString(response));
						partnerResponse.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);

						// state saved in queue
						userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
								Integer.toString(existingDTxs.get(0).getDipcoinId()));

						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
					}

					// state saved in queue
					userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
							Integer.toString(existingDTxs.get(0).getDipcoinId()));

					response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Verifying Bank/Merchant user")
				.format());
		if (!(this.userDBService.bankRepresentative(partnerUser)
				|| this.userDBService.merchantRepresentative(partnerUser))
				|| !this.userDBService.isActive(partnerUser)) {

			// state saved in queue
			userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
					dcoinReq.getOsta());
			// save failure transaction
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Uauthorized User").format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.UNAUTHORIZED, HeaderCode.USER_UNAUTHORIZED);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.BAD_REQUEST);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				partnerResponse.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}

		// AsyncTask - verify merchant. merchant ID and incoming IP should be
		// associated

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Verifying Merchant").format());
		if ((merchant != null && !this.merchantDBService.isActive(merchant))
				|| (bank != null && !this.bankDBService.isActive(bank))) {

			// state saved in queue
			userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
					dcoinReq.getOsta());

			// save failure transaction
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid/Inactive Merchant/Bank")
					.format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.UNAUTHORIZED, HeaderCode.USER_UNAUTHORIZED);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(merchant != null ? HeaderCode.MERCHANT_NOT_ACTIVE : HeaderCode.BANK_NOT_ACTIVE);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				partnerResponse.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}

		// if dipcoin invalid
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Validating Dipcoin")
				.data("Dipcoin", dcoinReq.getOsta()).format());

		Pair<String, String> dcoinSegments = CoreUtils.parseDipcoinToken(dcoinReq.getOsta());
		if (dcoinSegments == null
				|| (existingDcoin.isPresent() && !existingDcoin.get().getCoin().equals(dcoinSegments.getRight()))) {

			// state saved in queue
			userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
					dcoinReq.getOsta());

			// save failure transaction
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid Dipcoin")
					.data("dcoin", dcoinReq.getOsta()).format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.BAD_REQUEST);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));

			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.BAD_REQUEST);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				partnerResponse.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}

			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// if dipcoin not provided fetch based on requested coin
		Dipcoin dcoin = (existingDcoin != null && existingDcoin.isPresent()) ? existingDcoin.get() : null;
		if (dcoin == null) {
			// parse incoming dipcoin
			if (dcoinSegments == null
					|| (existingDcoin.isPresent() && !existingDcoin.get().getCoin().equals(dcoinSegments.getRight()))) {

				// state saved in queue
				userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
						dcoinReq.getOsta());

				// save failure transaction
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid Dipcoin")
						.data("dcoin", dcoinReq.getOsta()).format());
				saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.BAD_REQUEST);

				response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
				if (source.equals(TransactionSource.OAUTH)) {
					response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				} else {
					response.addHeaderCode(HeaderCode.BAD_REQUEST);
				}
				if (encrypt) {
					response.setAmount(dcoinReq.getAmount().toString());
					PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse(
							merchant.getReferenceId(), objectMapper.writeValueAsString(response));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
				}
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching User")
					.data("phone", dcoinSegments.getLeft()).format());

			// get dipcoin by coin
			/*
			 * Dipcoin dipcoin = this.coinDBService.getDipcoin(dcoinSegments.getRight());
			 * 
			 * User owner = dipcoin.getUser();
			 */

			// User owner = this.userDBService.getUser(dcoinSegments.getLeft());
			User owner = null;
			if (dcoinReq.getWalletUser()) {
				owner = this.userDBService.getUsers(dcoinSegments.getLeft(), merchant.getId(),
						Arrays.asList(UserRoles.VIRTUAL_CUSTOMER.value(), UserRoles.VIRTUAL_MERCHANT.value()),
						DBConstants.UserStatus.ACTIVE.value()).get(0);

			} else if (StringUtils.isNotEmpty(dcoinReq.getPartnerReferenceId()) && dcoinReq.getRestrictedUsage()) {

				Bank bbpsBranchBank = this.bankDBService.getBank(dcoinReq.getPartnerReferenceId());

				if (bbpsBranchBank == null) {
					// state saved in queue
					userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
							dcoinReq.getOsta());
					saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.BANK_DOESNT_EXISTS);

					response.addHeaderCode(HeaderCode.BANK_DOESNT_EXISTS);
					response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}

				if (!bankDBService.isActive(bbpsBranchBank)) {
					// state saved in queue
					userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
							dcoinReq.getOsta());
					saveFailureDipcoinTransaction(transaction, HttpStatus.UNAUTHORIZED, HeaderCode.BANK_NOT_ACTIVE);

					response.addHeaderCode(HeaderCode.BANK_NOT_ACTIVE);
					response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
				}

				owner = this.userDBService
						.getUsers(dcoinSegments.getLeft(), bbpsBranchBank.getId(),
								Arrays.asList(UserRoles.VIRTUAL_BANK.value()), DBConstants.UserStatus.ACTIVE.value())
						.get(0);

				if (owner == null) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid User")
							.data("phone", dcoinSegments.getLeft()).format());
					// state saved in queue
					userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
							dcoinReq.getOsta());
					saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.USER_DOESNT_EXIST);
					response.addHeaderCode(HeaderCode.USER_DOESNT_EXIST);
					response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
					return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
				}

			} else {

				if (source == TransactionSource.TOLL) {
					owner = this.userDBService.getUsersByPhone(Arrays.asList(UserRoles.CUSTOMER.value()),
							NumberUtils.INTEGER_ZERO, dcoinSegments.getLeft());
				} else {
					List<User> owners = this.userDBService.getUsers(dcoinSegments.getLeft(), 0,
							Arrays.asList(UserRoles.CUSTOMER.value()), DBConstants.UserStatus.ACTIVE.value());

					if (CollectionUtils.isEmpty(owners)) {
						response.addHeaderCode(HeaderCode.USER_DOESNT_EXIST);
						response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
						return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
					}

					owner = owners.get(NumberUtils.INTEGER_ZERO);
				}

			}

			if (owner == null) {

				// state saved in queue
				userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
						dcoinReq.getOsta());

				// save failure transaction
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid User")
						.data("phone", dcoinSegments.getLeft()).format());
				saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.USER_INVALID_PHONENUM);

				response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
				if (source.equals(TransactionSource.OAUTH)) {
					response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				} else {
					response.addHeaderCode(HeaderCode.USER_INVALID_PHONENUM);
				}
				if (encrypt) {
					response.setAmount(dcoinReq.getAmount().toString());
					PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse(
							merchant.getReferenceId(), objectMapper.writeValueAsString(response));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
				}
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			ctx.setUserId(String.valueOf(owner.getId()));
			DipcoinThreadLocal.set(ctx);

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching User Dipcoin")
					.data("phone", dcoinSegments.getLeft()).data("Dipcoin", dcoinSegments.getRight()).format());

			//@formatter:on
			// Session validation

			if (owner.getRole().equals(DBConstants.UserRoles.VIRTUAL_CUSTOMER.value())
					|| owner.getRole().equals(DBConstants.UserRoles.VIRTUAL_MERCHANT.value())) {
				// don't check session

			} else {
				if (source.equals(TransactionSource.OAUTH) || source.equals(TransactionSource.JS)) {
					if (!Hibernate.isInitialized(owner.getSessions())) {
						Hibernate.initialize(owner.getSessions());
					}
					Pair<Boolean, List<Session>> sessionPair = CoreUtils.isEmptyList(owner.getSessions());
					Session session = !sessionPair.getLeft() ? sessionPair.getRight().get(0) : null;
					if (session == null
							|| DateTime.now(DateTimeZone.UTC).isAfter(Long.parseLong(session.getStopTime()))) {
						// state saved in queue
						userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
								dcoinReq.getOsta());

						saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.BAD_REQUEST);

						response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
						if (source.equals(TransactionSource.OAUTH)) {
							response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
						} else {
							response.addHeaderCode(HeaderCode.INVALID_SESSION);
						}
						if (encrypt) {
							response.setAmount(dcoinReq.getAmount().toString());
							PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse(
									merchant.getReferenceId(), objectMapper.writeValueAsString(response));
							return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
						}
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
					}
					if (dcoinReq.getAmount().compareTo(DBConstants.MIN_NOTIFICATION_AMOUNT) >= 0) { // if
																									// needed
																									// only in
																									// prod
																									// then
																									// add
																									// this -
																									// &&
																									// httpServletContext.isProdEnvironment())
																									// {
						// notification set as true if prod
						ClientFeatureFlags clientFeatureFlags = ClientFeatureFlags.instance();
						clientFeatureFlags.setNotificationEnabled(APIFeatureFlags.notificationEnabled());
						httpServletContext.setClientFeatureFlags(clientFeatureFlags);
					}
				}

			}
			dcoin = this.coinDBService.asyncGetCoin(owner.getId(), dcoinSegments.getRight(), true).get();

			if (dcoin == null) {
				// state saved in queue
				userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
						dcoinReq.getOsta());

				// save failure transaction
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to fetch dcoin")
						.data("dcoin", dcoinSegments).format());
				saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.DIPCOIN_DOESNT_EXIST);

				response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
				if (source.equals(TransactionSource.OAUTH)) {
					response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				} else {
					response.addHeaderCode(HeaderCode.DIPCOIN_DOESNT_EXIST);
				}
				if (encrypt) {
					response.setAmount(dcoinReq.getAmount().toString());
					PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse(
							merchant.getReferenceId(), objectMapper.writeValueAsString(response));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
				}
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
		}

		if (source.equals(TransactionSource.OAUTH) && dcoin.getUsageType() != DipcoinUsageType.WALLET.value() && dcoin
				.getCustomerAccount().getTypeOfMethod() == DBConstants.CustomerAccountMethodType.WALLET_INB.value()) {
			// state saved in queue
			userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
					dcoinReq.getOsta());

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Invalid Dipcoin used is OAuth Transaction").format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.DIPCOIN_INVALID);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.DIPCOIN_INVALID);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// validate account active
		CustomerAccount account = dcoin.getCustomerAccount();
		if (account == null || !CustomerAccountStatus.ACTIVE.equals(account.getStatus())) {
			// save failure transaction
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to fetch CustomerAccount")
					.format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.UNAUTHORIZED, HeaderCode.USER_UNAUTHORIZED);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.USER_ACCOUNT_INACTIVE);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		ctx.setCustomerAccountId(Integer.toString(account.getId()));
		DipcoinThreadLocal.set(ctx);

		// update transaction
		transaction.setDipcoinId(dcoin.getId());
		transaction.setCustomerAccountId(account.getId());
		transaction.setUpdateDate(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
		transaction.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));

		if (DipcoinUsageType.RESTRICTED_BBPS_CASH.ordinal() == dcoin.getUsageType()
				|| DipcoinUsageType.RESTRICTED_BBPS_FUND_TRANSFER.ordinal() == dcoin.getUsageType()) {

			if (!DBConstants.MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.equals(merchant.getBusinessSegment())) {

				// state saved in queue
				userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
						dcoinReq.getOsta());

				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Unauthorized User").format());
				saveFailureDipcoinTransaction(transaction, HttpStatus.UNAUTHORIZED, HeaderCode.ACCESS_FORBIDDEN);

				response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
				response.addHeaderCode(HeaderCode.ACCESS_FORBIDDEN);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
		}

		// @NOTE - since the request can come from both ATM (i,e BI) and Customer,
		// we fetch the user from Account instead of User passed from ATM type
		if (!Hibernate.isInitialized(account.getUser())) {
			Hibernate.initialize(account.getUser());
		}
		User dipcoinOwner = account.getUser();
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetching owner")
				.data("User", dipcoinOwner.getId()).format());

		// rules are applied before processing the request further
		boolean processRequest = userEventResource.applyRule(dipcoinOwner, partnerUser, Integer.toString(dcoin.getId()),
				requestTime, ProcessEvent.EventType.OstaUsage, EventUtils.dipcoinUsageFailure);

		if (!processRequest) {

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		}

		// state saved in queue
		userEventResource.processInQueue(dipcoinOwner, requestTime, EventUtils.dipcoinUsageInprocess,
				Integer.toString(dcoin.getId()));

		if (TransactionSource.ATM.equals(source)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("ATM Request").format());
			// if the owner is inactive
			if (!this.userDBService.isActive(dipcoinOwner)) {
				// state saved in queue
				userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
						dcoinReq.getOsta());

				// save failure transaction
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Unauthorized User").format());
				saveFailureDipcoinTransaction(transaction, HttpStatus.UNAUTHORIZED, HeaderCode.USER_UNAUTHORIZED);

				response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
				if (source.equals(TransactionSource.OAUTH)) {
					response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
				} else {
					response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
				}
				if (encrypt) {
					response.setAmount(dcoinReq.getAmount().toString());
					PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse(
							merchant.getReferenceId(), objectMapper.writeValueAsString(response));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
				}
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

			}

			transaction.setUser(dipcoinOwner);
		}

		Long now = DateTime.now(DateTimeZone.UTC).getMillis();
		if (!DipcoinStatus.ACTIVE.equals(dcoin.getStatus())) {

			// state saved in queue
			userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
					dcoinReq.getOsta());

			// save failure transaction
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Dipcoin not active").format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.DIPCOIN_NOT_ACTIVE);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.DIPCOIN_NOT_ACTIVE);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

		} else if (now > Long.parseLong(dcoin.getExpiryTime())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("NOW", now)
					.data("ExpiryTime", Long.parseLong(dcoin.getExpiryTime())).format());

			// state saved in queue
			userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
					dcoinReq.getOsta());

			// save failure transaction
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Dipcoin expired").format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.DIPCOIN_EXPIRED);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.DIPCOIN_EXPIRED);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

		} else if (dcoinReq.getAmount().compareTo(dcoin.getAmount()) > 0) {
			// @TODO - if bank ATM request, fetch bank commisiion & the send notification to
			// user on the
			// amount that can be used

			// state saved in queue
			userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure,
					dcoinReq.getOsta());

			// save failure transaction
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Dipcoin insufficient funds")
					.format());
			saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST, HeaderCode.DIPCOIN_INSUFFICIENT_FUNDS);

			response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
			if (source.equals(TransactionSource.OAUTH)) {
				response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
			} else {
				response.addHeaderCode(HeaderCode.DIPCOIN_INSUFFICIENT_FUNDS);
			}
			if (encrypt) {
				response.setAmount(dcoinReq.getAmount().toString());
				PartnerResponse partnerResponse = partnerEncDecResource
						.encryptPartnerResponse(merchant.getReferenceId(), objectMapper.writeValueAsString(response));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(partnerResponse);
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// These changes are made due these issues
		// https://github.com/dipcoin/dipcoin-api-system/issues/828 and
		// https://github.com/dipcoin/dipcoin-api-system/issues/813.
		// The following code commented to for convenience sake in future.

	//@formatter:off
    // if ATM type then compute commission and validate
    /*if (RequestSource.ATM.equals(source)) {
      if (dcoin.getUsageType() != DipcoinUsageType.ATM.value()) {
        // save failure transaction
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Dipcoin not of ATM UsageType").format());
        saveFailureDipcoinTransaction( transaction, HttpStatus.BAD_REQUEST,
            HeaderCode.DIPCOIN_INVALID_USAGE_TYPE);

        response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
        response.addHeaderCode(HeaderCode.DIPCOIN_INVALID_USAGE_TYPE);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
(response).status(HttpStatus.BAD_REQUEST).build();
      }

      // fetch commission associated with this dipcoin
      // @NOTE - might want to pass the dipcoin inside to avoid additional call to getDipcoin in
      // calculatCommission
      ResponseEntity commission = this.customerDipcoinResource.calculatCommission(
          dipcoinOwner, dcoinReq.getOsta(), dcoinReq.getAmount(), DipcoinUsageType.ATM.value());

      // if requested amount + commission > dipcoin amount, throw error
      if (commission.getStatus() >= HttpStatus.OK.getStatusCode()
          && commission.getStatus() < HttpStatus.BAD_REQUEST) {
        DipcoinSurchargeResponse surcharge = (DipcoinSurchargeResponse) commission.getEntity();
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Surcharge", surcharge)
            .data("Dipcoin Amount", dcoin.getAmount()).data("Request Amount", dcoinReq.getAmount())
            .format());

        if (dcoinReq.getAmount().add(surcharge.getCommission()).compareTo(dcoin.getAmount()) > 0) {
          response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
          response.addHeaderCode(HeaderCode.DIPCOIN_INSUFFICIENT_FUNDS);

          // save failure transaction
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Dipcoin not of ATM UsageType").format());
          saveFailureDipcoinTransaction( transaction, HttpStatus.BAD_REQUEST,
              HeaderCode.DIPCOIN_INSUFFICIENT_FUNDS);

          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
(response).status(HttpStatus.BAD_REQUEST).build();
        }

        // update the actual value to be used. i.e requested + commission
        dcoinReq.setAmount(dcoinReq.getAmount().add(surcharge.getCommission()));
      }else{
        // something went wrong during commission calculation.
        response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
        response.addHeaderCode(HeaderCode.INTERNAL_ERROR);

        // save failure transaction
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Failed to fetch commission").data("dipcoin", dcoin.getId()).format());
        saveFailureDipcoinTransaction( transaction, HttpStatus.INTERNAL_SERVER_ERROR,
            HeaderCode.INTERNAL_ERROR);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
(response).status(HttpStatus.INTERNAL_SERVER_ERROR)
            .build();
      }

    }*/

    String partnerName = bank != null ? bank.getName() : merchant.getName();

    
    // @TODO check if 2 factor is opted for by user
    DipcoinApprovalRequest callbackRequest = null;
    if(!(dipcoinOwner.getRole().equals(DBConstants.UserRoles.VIRTUAL_CUSTOMER.value()) 
        || dipcoinOwner.getRole().equals(DBConstants.UserRoles.VIRTUAL_MERCHANT.value()))) {
    if (apiFeatureFlags.notificationEnabled()  && httpServletContext.getClientFeatureFlags().notificationEnabled() 
        && dcoinReq.getAmount().compareTo(DBConstants.MIN_NOTIFICATION_AMOUNT) >= 0) { 
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Sending Notification to Customer")
          .data("dipcoinOwner", dipcoinOwner)
          .data("dcoin",dcoin)
          .data("partnerName",partnerName).format());
      callbackRequest = notificationResource.getUserDipcoinApproval(dipcoinOwner,
          dcoin, partnerName, dcoinReq.getAmount());
      
      if (callbackRequest == null) {
        // save failure transaction
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("No Response of the notification set to user").format());
        transaction.setType(DipcoinTransactionType.CANCELLED_BY_USER.value());
        transaction.setDCResponseCode(String.valueOf(HttpStatus.BAD_REQUEST.value()));
        transaction.setDCResponseDesc(HeaderCode.USER_VERIFICATION_REJECT.message());
        transaction.setStatus(DipcoinTransactionsStatus.FAILURE.value());
        saveDipcoinTransaction(transaction);

        response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
        if (source.equals(TransactionSource.OAUTH)) {
            response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
          }else {
              response.addHeaderCode(HeaderCode.USER_VERIFICATION_REJECT);
          }
        if (encrypt) {
          response.setAmount(dcoinReq.getAmount().toString());
          PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse( 
                  merchant.getReferenceId(), objectMapper.writeValueAsString(response));
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(partnerResponse);
      }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }
      

      if (callbackRequest != null) {
        // send notification to (non MI) merchant user
        if (TransactionSource.WEB.equals(source) && !StringUtils.isEmpty(dcoinReq.getPartnerDevice())
            && !(DBConstants.UserRoles.MERCHANT_INTERNAL.equals(partnerUser.getRole())
                || DBConstants.UserRoles.BANK_INTERNAL.equals(partnerUser.getRole()))) {
          UserDeviceInfoResponse merchantUserDeviceInfo =
              APIUtils.decryptUserDeviceInfo(cryptoUtil, dcoinReq.getPartnerDevice());
          if (merchantUserDeviceInfo != null) {

            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Sending Notification to Merchant")
                .data("merchantUserDeviceInfo", merchantUserDeviceInfo)
                .data("approved", callbackRequest.getApproved()).format());
            notificationResource.notifyPartnerUserApproval(merchantUserDeviceInfo,
                partnerUser, merchant, bank, dcoin, dcoinReq.getAmount(), dipcoinReferenceNumber,
                dcoinReq.getPartnerTransactionReferenceId(), callbackRequest.getApproved());
          }
        }
        
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Callback Response")
            .data("response", callbackRequest).format());
        
        // return error response
        if (!callbackRequest.getApproved()) {
          
        //state saved in queue
          userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure
                , dcoinReq.getOsta());
          
          // save failure transaction
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("2-Factor authentication rejecetd by User").format());
          transaction.setType(DipcoinTransactionType.CANCELLED_BY_USER.value());
          transaction.setDCResponseCode(String.valueOf(HttpStatus.BAD_REQUEST.value()));
          transaction.setDCResponseDesc(HeaderCode.USER_VERIFICATION_REJECT.message());
          transaction.setStatus(DipcoinTransactionsStatus.FAILURE.value());
          saveDipcoinTransaction(transaction);

          response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
          if (source.equals(TransactionSource.OAUTH)) {
              response.addHeaderCode(HeaderCode.TRANSACTION_FAILURE);
            }else {
                response.addHeaderCode(HeaderCode.USER_VERIFICATION_REJECT);
            }
          if (encrypt) {
            response.setAmount(dcoinReq.getAmount().toString());
            PartnerResponse partnerResponse = partnerEncDecResource.encryptPartnerResponse( 
                    merchant.getReferenceId(), objectMapper.writeValueAsString(response));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(partnerResponse);
        }
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        
        if(new BigDecimal(callbackRequest.getAmount()).compareTo(DBConstants.MAX_NOTIFICATION_AMOUNT_FOR_PIN) >= 0) {
          
          if(StringUtils.isEmpty(callbackRequest.getAuthorizationPin())) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Pin not received").format());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_INVALID_PIN));
          }
          
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Callback Response")
              .data("Pin received in callbackRequest", callbackRequest.getAuthorizationPin())
              .data("dipcoinOwners Pin", dipcoinOwner.getPin()).format());
          
          if(!userUtil.comparePassword(callbackRequest.getAuthorizationPin(), dipcoinOwner.getPin())) {
            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
                .message("Pin does not match with Osta Pin").format());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(APIResponse.error(HeaderCode.USER_INVALID_PIN));
          }
          
        }
      }
    }
    }
    

    // if success, update DB
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("Marking Dipcoin as PROCESSED").format());
  //dcoin = this.coinDBService.updateCoin(dcoin, DipcoinStatus.PROCESSED.value());
    dcoin.setStatus(DipcoinStatus.PROCESSED.value());
    dcoin = this.coinDBService.updateCoin(dcoin);
    

    // Failure of updation of dipcoin status
    if (dcoin == null) {
      response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
      response.addHeaderCode(HeaderCode.DIPCOIN_DOESNT_EXIST);
      
    //state saved in queue
      userEventResource.processInQueue(partnerUser, requestTime, EventUtils.dipcoinUsageFailure
            , dcoinReq.getOsta());

      // save failure transaction
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Failed to fetch dipcoin").data("dcoin", dcoin).format());
      saveFailureDipcoinTransaction(transaction, HttpStatus.BAD_REQUEST,
          HeaderCode.DIPCOIN_DOESNT_EXIST);
      requestMetadata.setDipcoinTransactionType(String.valueOf(transaction.getType()));
      requestMetadata.setAmount(transaction.getAmount());
      ctx.setRequestMetadata(requestMetadata);
      DipcoinThreadLocal.set(ctx);

      throw new APIException(HttpStatus.BAD_REQUEST, response);
    }

    // update response
    response.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));

    // save success transaction
    transaction.setAmount(dcoinReq.getAmount());
    transaction.setDCResponseCode(String.valueOf(HttpStatus.OK.value()));
    transaction.setDCResponseDesc(HeaderCode.DIPCOIN_PROCESS_SUCCESS.message());
    transaction.setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
    if (TransactionSource.ATM.equals(source)) {
      transaction.setType(DipcoinTransactionType.CARDLESS_CASH_WITHDRAWAL.value());
    } else {
      transaction.setType((dcoinReq.getAmount().compareTo(dcoin.getAmount()) == 0)
          ? DipcoinTransactionType.COMPLETELY_USED.value()
          : DipcoinTransactionType.PARTIALLY_USED.value());
    }
    transaction.setStatus(DipcoinTransactionsStatus.SUCCESS.value());
    transaction.setResponseTime(response.getResponseTime());
    saveDipcoinTransaction(transaction);

    // send email to user
    
    if(applicationProperties.enableSMS() &&!(UserRoles.VIRTUAL_CUSTOMER.equals(dipcoinOwner.getRole())
        || UserRoles.VIRTUAL_MERCHANT. equals(dipcoinOwner.getRole())
        || UserRoles.VIRTUAL_BANK.equals(dipcoinOwner.getRole())
        || (merchant != null && MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment())
        ||(APIUtils.smsEmailAreNotAllowed(dcoin.getUsageType())))) {
    if (dipcoinOwner.getIsEmailVerified() == BooleanStatus.YES.value()
        && !emailUtils.sendDipcoinUsedEmailCustomer(dipcoinOwner, partnerName,
        dcoin, dcoinReq.getAmount())) {
      LOG.error(
          "Failed to send email to user " + dipcoinOwner.getId() + " for dipcoin " + dcoin.getId());
    }
    }

    /*
     * auto generate dipcoin if configured. No auto generate for ATM
     */
    if (!TransactionSource.ATM.equals(source) ) {
      BigDecimal newAmount = dcoin.getAmount().subtract(dcoinReq.getAmount());
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Submitting Dipcoin AutoGenerate").data("Amount", newAmount)
          .data("Account", account.getId()).data("Dipcoin", dcoin.getId()).format());
      autogenerateDipcoin(dcoin, account, newAmount, source);

      // send email and sms to merchant
      if(bank == null) {   	  
      
    	  List<User> merchantSuperAdmins = userDBService.getBankMerchantUsers(merchant.getId(),
          Arrays.asList(UserRoles.MERCHANT_SUPERADMIN.value()));
    	  if(merchantSuperAdmins.isEmpty()) {
        	  response.addHeaderCode(HeaderCode.MERCHANT_USER_NOT_SUPERADMIN);
              return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
          }
    	  User merchantUser = merchantSuperAdmins.get(0);
      
  	if(!(dipcoinOwner.getRole().equals(DBConstants.UserRoles.VIRTUAL_CUSTOMER.value()) 
			|| dipcoinOwner.getRole().equals(DBConstants.UserRoles.VIRTUAL_MERCHANT.value()))) {
	
  	  try {
			if ((!(MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value() == merchant.getBusinessSegment()
					|| MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment()))
					&& (!emailUtils.sendDipcoinUsedEmailMerchant(merchantUser.getFirstName(), merchant.getEmailId(),
							dcoin, dcoinReq.getAmount()))) {
				LOG.error("Failed to send email to merchant or email configuration not enabled as business segment of merchant is toll"
								+ merchantUser.getId() + " for dipcoin " + dcoin.getId());
			}

      //Sms should not be send to Merchant having BussinessSegment of Recharge and Toll

      if (MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value() == merchant.getBusinessSegment()){
    	  
    	  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("recharge sms")
    	            .format());
    	if(!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(dipcoinOwner.getPhone(),
    		          Templates.BillPayRecharge.format(dcoinReq.getAmount()),
    		          httpServletContext.getClientFeatureFlags().smsEnabled()))
          {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS to merchant or sms configuration not enabled as business segment of merchant is recharge or toll")
            .data("phone", dipcoinOwner.getPhone())
            .data("template",
                Templates.BillPayRecharge.format(dcoinReq.getAmount()))
            .format());
        }
        
        if (dipcoinOwner.getIsEmailVerified() == BooleanStatus.YES.value()
                && !emailUtils.sendDipcoinUsedForRechargeEmailCustomer(dipcoinOwner,
                 dcoinReq.getAmount())) {
              LOG.error(
                  "Failed to send email to user " + dipcoinOwner.getId() + " for dipcoin " + dcoin.getId());
            }
        
      }
      
      if (applicationProperties.getAwsSMSClient()) {

        NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
        notificationRequestContext.setTraceId(httpServletContext.getTraceId());
		if ((!(MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value() == merchant.getBusinessSegment()
				|| MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment()))
				&& (!notificationResource.sendSms(merchant.getOfficeNumber(),
						Templates.MerchantDipcoinUsage.format(dcoin.getCoin(), dcoinReq.getAmount()),
						httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext))){
          
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS to merchant or sms configuration not enabled as business segment of merchant is recharge or toll")
              .data("phone", merchant.getOfficeNumber())
              .data("template",
                  Templates.MerchantDipcoinUsage.format(dcoin.getCoin(), dcoinReq.getAmount()))
              .format());

        }
      }

  	  }catch(Exception e) {
  	  LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send email or sms")
          .format(), e);
  	  }
  	}
    }
    }
    
    
    // send SMS
    try {
    if(applicationProperties.enableSMS() && !(UserRoles.VIRTUAL_CUSTOMER.equals(dipcoinOwner.getRole())
        || UserRoles.VIRTUAL_MERCHANT. equals(dipcoinOwner.getRole())
        || UserRoles.VIRTUAL_BANK.equals(dipcoinOwner.getRole())
        || (merchant != null && MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment()))) {
    if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(dipcoinOwner.getPhone(),
        Templates.CustomerDipcoinUsage.format(partnerName, dcoinReq.getAmount()),
        httpServletContext.getClientFeatureFlags().smsEnabled())) {
    	 LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message(" sms blocked")
   	            .format());
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
          .data("phone", dipcoinOwner.getPhone()).data("template", Templates.CustomerDipcoinUsage
              .format(partnerName, dcoinReq.getAmount()))
          .format());
    }
    
    if (applicationProperties.getAwsSMSClient()) {

      NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
      notificationRequestContext.setTraceId(httpServletContext.getTraceId());
      if (!notificationResource.sendSms(dipcoinOwner.getPhone(),
          Templates.CustomerDipcoinUsage.format(partnerName, dcoinReq.getAmount()),
          httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {
   
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
            .data("phone", dipcoinOwner.getPhone()).data("template", Templates.CustomerDipcoinUsage
                .format(partnerName, dcoinReq.getAmount()))
            .format());

      }
    }
    }
    
    if(!(UserRoles.VIRTUAL_CUSTOMER.equals(dipcoinOwner.getRole())
        || UserRoles.VIRTUAL_MERCHANT. equals(dipcoinOwner.getRole())
        || UserRoles.VIRTUAL_BANK.equals(dipcoinOwner.getRole())
        || (merchant != null && MerchantBusinessSegment.TOLL.value() == merchant.getBusinessSegment()))) {
    // finally send notification back to user indicating success
    // @TODO check if 2 factor is opted for by user
    
    if (apiFeatureFlags.notificationEnabled()  && httpServletContext.getClientFeatureFlags().notificationEnabled() ) { // apiFeatureFlags.notificationEnabled()&& httpServletContext.getClientFeatureFlags().notificationEnabled()
      if (callbackRequest != null 
          && callbackRequest.getApproved()) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Sending Client Callback Notification")
            .data("clientCallback", callbackRequest.getClientCallback())
            .data("approved", callbackRequest.getApproved()).format());
        notificationResource.notifyClientCallback(dipcoinOwner,
            callbackRequest);
      }
    }
    }
    }catch(Exception e) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send notification")
          .format(), e);
    }
    

    if (source.equals(TransactionSource.OAUTH)) {
    	response.addHeaderCode(HeaderCode.TRANSACTION_SUCCESSFUL);
        //metric for dipcoin used via oauth trnx
    	oauthMetricRegistry.dipcoinUsageUsingOauth().increment();
    	
      }else {
    	  response.addHeaderCode(HeaderCode.DIPCOIN_PROCESS_SUCCESS);
      }
    
    dipcoinMetricRegistry.dipcoinUsage().increment();
    
    if (encrypt) {
      PartnerResponse partnerResponse = new PartnerResponse();
      try {
		response.setAmount(dcoinReq.getAmount().toString());
		 partnerResponse = partnerEncDecResource.encryptPartnerResponse(
				merchant.getReferenceId(), objectMapper.writeValueAsString(response));
      }catch(Exception e) {
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to encrypt partner Response")
            .format(), e);
      }
		if (source.equals(TransactionSource.OAUTH)) {
          partnerResponse.addHeaderCode(HeaderCode.TRANSACTION_SUCCESSFUL);
      }else {
        partnerResponse.addHeaderCode(HeaderCode.DIPCOIN_PROCESS_SUCCESS);
      }
      return ResponseEntity.status(HttpStatus.OK).body(partnerResponse);
	}
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
  
  
  // ##########################################################################################################################
  
//recharge
  public String usageType(PartnerProcessDipcoinRequest request, Merchant merchant, Bank bank, TransactionSource source) {
    
    
    if(!StringUtils.isEmpty(request.getUsageDetails())) {
      return request.getUsageDetails();
    }

    if (merchant != null) {

      if (request.getSubPartner() == null 
          &&  StringUtils.isNotEmpty(merchant.getOfficeCity())  && 
          StringUtils.isNotEmpty(merchant.getOfficeDistrict())) {
        return String.format("%s, %s, %s",
            merchant.getAlias() != null ? merchant.getAlias() : merchant.getName(),
            merchant.getOfficeCity(), merchant.getOfficeDistrict());
      }

      if (DBConstants.MerchantBusinessSegment.RECHARGE_BILLPAYMENTS.value()
          .equals(merchant.getBusinessSegment())) {
    	  return String.format("%s, %s", request.getSubPartner().getName(),
    			  (request.getSubPartner().getPhone() != null ? request.getSubPartner().getPhone()
    					  : request.getSubPartner().getSubscriptionDetails()));
        
      }

      if (DBConstants.MerchantBusinessSegment.TOLL.value().equals(merchant.
          getBusinessSegment())) { 
        return String.format("%s, %s, %s", request.getSubPartner().getVehicleNumber() +" - "+ merchant.getName(),
          request.getSubPartner().getTollLocation(),"Lane Direction: "+request.getSubPartner().getTollLaneDirection());
          
       }
			 

      if (DBConstants.MerchantPartnerType.MERCHANT_AGGREGATOR.equals(merchant.getPartnerType())) {
        return String.format("%s, %s, %s", request.getSubPartner().getName(),
            request.getSubPartner().getPartnerReferenceId(), request.getSubPartner().getAddress());

      }
    }

    if (bank != null) {
    	if(DBConstants.TransactionSource.WEB.value() == source.value()) {
    		return String.format("%s , %s, %s", bank.getName(), bank.getOfficeCity(), request.getSubPartner().getName());
    	}
    	
      return String.format("%s , %s", bank.getName(), bank.getOfficeCity(),
          bank.getOfficeDistrict());
    }

    return String.format("%s",
        merchant != null ? (merchant.getAlias() != null ? merchant.getAlias() : merchant.getName()) : (bank!=null ? bank.getName() : null));
  }
  

}
