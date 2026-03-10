package com.dipcoin.api.resource;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.core.CryptoUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APICustomization;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.CreateUserWalletRequest;
import com.dipcoin.api.model.CustomerAccountRequest;
import com.dipcoin.api.model.CustomerAccountResponse;
import com.dipcoin.api.model.CustomerAccountVerifyRequest;
import com.dipcoin.api.model.TopUpDetails;
import com.dipcoin.api.model.WalletUserInfo;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.comm.CreateVirtualAccountRequest;
import com.dipcoin.bank.services.comm.CreateVirtualAccountResponse;
import com.dipcoin.bank.services.comm.UserAuthenticationRequest;
import com.dipcoin.bank.services.comm.UserAuthenticationResponse;
import com.dipcoin.bank.services.comm.VerifyOTPRequest;
import com.dipcoin.bank.services.comm.VerifyOTPResponse;
import com.dipcoin.bank.services.utils.BankConstants.BankResponseStatus;
import com.dipcoin.bank.services.utils.BankProperties;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankUtils;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConfig;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BankAuthenticationType;
import com.dipcoin.db.services.commons.DBConstants.BankTollStatus;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankAccount;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.dipcoin.metrics.PaymentSourceMetricRegistry;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("customerResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class CustomerResource {

	private static final Logger LOG = LogManager.getLogger(CustomerResource.class);

	// @TODO - configure this via config
	protected static boolean encryptCardId = false;
	private final static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	@Qualifier("com.dipcoin.metrics.PaymentSourceMetricRegistry")
	private PaymentSourceMetricRegistry paymentSourceMetricRegistry;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private CryptoUtil cryptoUtil;

	@Autowired
	private CustomerDBService customerDBService;

	@Autowired
	private WalletResource walletResource;

	@Autowired
	private BankDBService bankDBService;

	@Autowired
	private CustomerResource customerResource;

	@Autowired
	private EncryptionResource encryptionResource;

	@Autowired
	private BankAPIServices bankAPIServices;

	@Autowired
	private SmsClient smsClient;

	@Autowired
	private BankUtils bankUtils;

	@Autowired
	private ApplicationProperties applicationProperties;

	@Autowired
	private NotificationResource notificationResource;

	@Autowired
	private EmailUtils emailUtils;

	@Autowired
	private DBConfig dbConfig;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;
	
	  public ResponseEntity addPaymentSource(final CreateUserWalletRequest createReq, User oauthUser,
		      Merchant oauthMerchant, Boolean createUser, TransactionSource source, Map<String, Object> map, String walletBankReferenceId)
		      throws APIException, Exception {
		    // *Add payment source

		    // fetch users on basis or phone,email,role,bankmerchnatId
		    WalletUserInfo walletUserInfo = new WalletUserInfo();
		    List<User> users = new ArrayList<User>();
		    if (createUser) {
		      if (APIConstants.VIRTUAL_CUSTOMER == (map.get("role"))) {

		        users = this.userDBService
		            .asyncGetUsers(createReq.getEmail(), createReq.getPhonenum(),
		                oauthUser.getBankMerchantId(), Arrays.asList(UserRoles.VIRTUAL_CUSTOMER.value()))
		            .get();
		      } else if (map.get("role") == APIConstants.VIRTUAL_MERCHANT) {
		        users = this.userDBService
		            .asyncGetUsers(createReq.getEmail(), createReq.getPhonenum(),
		                oauthUser.getBankMerchantId(), Arrays.asList(UserRoles.VIRTUAL_MERCHANT.value()))
		            .get();
		      }
		    } else {
		      users.add(oauthUser);
		    }

		    // add account and verify account
		    CustomerAccountRequest customerAccountRequest =
		        walletResource.populateCustomerAccountRequest(createReq, (Bank) map.get("bank"),
		            (CreateVirtualAccountRequest) map.get("createVirtualAccountRequest"),
		            (CreateVirtualAccountResponse) map.get("createVirtualAccountResponse"), users.get(0));
		    ResponseEntity addedCustomerAccount = customerResource.addAccount(users.get(0),
		        customerAccountRequest, (String) map.get("clientTransactionId"), Boolean.FALSE, walletBankReferenceId);

		    if (addedCustomerAccount.getStatusCode() != HttpStatus.OK) {
		      LOG.info(addedCustomerAccount.getBody());
		      walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .data("Reason", "rollback associated exception being handled").format());
		      throw new APIException(HttpStatus.BAD_REQUEST, walletUserInfo);
		      // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(walletUserInfo);
		    }

		    CustomerAccountResponse addedCustomerAccount1 =
		        (CustomerAccountResponse) addedCustomerAccount.getBody();

		    CustomerAccountVerifyRequest customerAccountVerifyRequest =
		        walletResource.populateCustomerAccountVerifyRequest(
		            (CreateVirtualAccountResponse) map.get("createVirtualAccountResponse"),
		            customerAccountRequest, addedCustomerAccount1);

		    ResponseEntity customerAccountVerified =
		        customerResource.verifyAccount(users.get(0), customerAccountVerifyRequest, Boolean.FALSE);

		    if (customerAccountVerified.getStatusCode() == HttpStatus.OK) {
		      walletUserInfo.setWalletId(customerAccountVerifyRequest.getBankUId());
		      
		    } else {

		      walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .data("Reason", "rollback associated exception being handled").format());
		      throw new APIException(HttpStatus.BAD_REQUEST, walletUserInfo);
		    }

		    // response
		    walletUserInfo.setFname(createReq.getFname().trim().replaceAll(" ", ""));
		    walletUserInfo.setLname(createReq.getLname().trim().replaceAll(" ", ""));
		    walletUserInfo.setPhonenum(createReq.getPhonenum());
		    walletUserInfo.setEmail(createReq.getEmail());
		    walletUserInfo.setCardId(customerAccountVerifyRequest.getCardId());
		    

		    walletUserInfo.addHeaderCode(HeaderCode.WALLET_CREATED);

		    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		        .message("Create User Wallet Response")
		        .data("Fname", walletUserInfo.getFname().trim().replaceAll(" ", ""))
		        .data("Lname", walletUserInfo.getLname().trim().replaceAll(" ", ""))
		        .data("Email", walletUserInfo.getEmail()).data("Phonenum", walletUserInfo.getPhonenum())
		        .data("WalletId", walletUserInfo.getWalletId()).format());

		    return ResponseEntity.ok(walletUserInfo);
		  }

	public ResponseEntity addAccount(final User user, final CustomerAccountRequest addReq,
			final String clientTransactionId, Boolean oauthRequest, String walletBankReferenceId)
			throws Exception, APIException {

		String originIp = httpServletContext.getOriginIp();
		CustomerAccountResponse response = new CustomerAccountResponse();

		if (addReq == null) {
			response.addHeaderCode(HeaderCode.BAD_REQUEST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		addReq.setDoMask(true);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request from oauth", addReq.getDoMask())
				.format());

		response.setClientTransactionId(clientTransactionId);

		if (clientTransactionId == null) {
			response.addHeaderCode(HeaderCode.MISSING_CLIENTTRANSACTIONID);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// initialize encryption/decryption
		if (!addReq.getForgotPin() && !oauthRequest && !addReq.initCrypto(cryptoUtil, user, httpServletContext)) {
			response.addHeaderCode(HeaderCode.ENCRYPTION_EXPIRED);
			paymentSourceMetricRegistry.paymentSourceTotal().increment();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// validate request
		if (!addReq.validate(httpServletContext)) {
			response.addHeaderCodes(addReq.getErrorCodes());
			paymentSourceMetricRegistry.paymentSourceTotal().increment();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// verify bank exists
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get Bank")
				.data("ReferenceId", addReq.getBankReferenceId()).format());
		Bank bank = this.bankDBService.getBank(null, addReq.getBankReferenceId(), true, false);
		if (bank == null) {
			response.addHeaderCode(HeaderCode.BANK_INVALID);
			paymentSourceMetricRegistry.paymentSourceTotal().increment();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// verify the bank has atleast 1 account associated with it
		if (bank.getBankAccounts().size() < 1) {
			response.addHeaderCode(HeaderCode.BANK_INVALID_ACCT);
			paymentSourceMetricRegistry.paymentSourceTotal().increment();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// check if bank is valid and active

		if (!this.bankDBService.isActive(bank)) {
			response.addHeaderCode(HeaderCode.BANK_NOT_ACTIVE);
			paymentSourceMetricRegistry.paymentSourceTotal().increment();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// verify user is active and a customer
		if (!this.userDBService.isActive(user)) {
			response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		if (!(this.userDBService.isCustomer(user) || this.userDBService.isBankSuperAdmin(user)
				|| this.userDBService.isVirtualBank(user))) {
			response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// get user all accts. We fetch all account to make sure if current account
		// needs to be forced
		// set PRIMARY
		// and to also check if the account is already added
		List<CustomerAccount> existingAccounts = this.customerDBService
				.asyncGetAccounts(user.getId(), Arrays.asList(DBConstants.CustomerAccountStatus.ACTIVE.value(),
						DBConstants.CustomerAccountStatus.BANK_OTP_NOT_VERIFIED.value()))
				.get();

		Iterator<CustomerAccount> customerAccount = existingAccounts.iterator();
		while (customerAccount.hasNext()) {
			CustomerAccount account = customerAccount.next();

			if (CustomerAccountMethodType.WALLET_INB.value() == account.getTypeOfMethod()) {

				existingAccounts.remove(account);

				if (account.getIsPrimary() == BooleanStatus.YES.value()) {
					account.setIsPrimary(BooleanStatus.NO.value());
					this.customerDBService.updateAccount(account);
				}
				break;
			}
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get Existing CustomerAccout")
				.data("User", user.getId()).data("HashedLogin", addReq.getHashedLogin()).format());

		// verify if account already added
		if (!addReq.getForgotPin()) {
			for (CustomerAccount account : existingAccounts) {
				// hashed login match
				if (((StringUtils.isNotEmpty(account.getHashedLogin())
						&& account.getHashedLogin().equals(addReq.getHashedLogin()))
						|| (StringUtils.isNotEmpty(account.getHashedAccountNumber())
								&& account.getHashedAccountNumber().equals(addReq.getHashedLogin())))
						&& account.getStatus() != CustomerAccountStatus.DELETED.value()) {
					// if same request, return success
					if (account.getClientTransactionId() != null
							&& account.getClientTransactionId().equals(clientTransactionId)) {

						if (DBConstants.CustomerAccountMethodType.ADDED_BY_BRANCH.value() == account
								.getTypeOfMethod()) {
							account.setTypeOfMethod(DBConstants.CustomerAccountMethodType.BANK_ACCOUNTOTP.value());
							account = this.customerDBService.addAccount(account);
							if (account == null) {
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("CustomerAccout not updated").format());
							}
						}

						populateCustomerAccountResponse(encryptionResource, user, account, bank, response);
						response.addHeaderCode(HeaderCode.USER_ACCOUNT_EXIST);
						paymentSourceMetricRegistry.paymentSourceTotal().increment();
						return ResponseEntity.ok(response);
					}

					// if bank not initialized, then initialize
					if (!Hibernate.isInitialized(account.getBank()))
						Hibernate.initialize(account.getBank());

					// if hashed login and bank match, return error
					if (account.getBank().getReferenceId().equals(addReq.getBankReferenceId())) {
						response.addHeaderCode(HeaderCode.USER_ACCOUNT_EXIST);
						paymentSourceMetricRegistry.paymentSourceTotal().increment();
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
					}
				}
				if (!StringUtils.isEmpty(account.getHashedAccountNumber())
						&& !StringUtils.isEmpty(addReq.getAccountNumber())) {

					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Account Number is there in the Request").data("User", user.getId()).format());

					if (account.getBank().getReferenceId().equals(bank.getReferenceId())) {
						if (((StringUtils.isNotEmpty(account.getHashedLogin())
								&& account.getHashedLogin().equals(addReq.getHashedLogin()))
								|| (StringUtils.isNotEmpty(account.getHashedAccountNumber())
										&& account.getHashedAccountNumber()
												.equals(Utils.getHashedLogin(
														StringUtils.stripStart(addReq.getAccountNumber(), "0")))))
								&& account.getStatus() == CustomerAccountStatus.ACTIVE.value()) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("User", user.getId())
									.data("account num", addReq.getAccountNumber())
									.data("hash of existing account", account.getHashedAccountNumber())
									.data("hashed of account number which is in the request", Utils
											.getHashedLogin(StringUtils.stripStart(addReq.getAccountNumber(), "0")))
									.format());
							response.addHeaderCode(HeaderCode.USER_ACCOUNT_EXIST);
							paymentSourceMetricRegistry.paymentSourceTotal().increment();
							return ResponseEntity.status(HttpStatus.OK).body(response);
						}
					}
				}
			}

		}

		// bank services will have the logic of generating UID and sending to
		// Bank, if configured
		// initiate bank transaction
		UserAuthenticationRequest bRequest = new UserAuthenticationRequest(bank.getReferenceId(), bank.getCode());
		bRequest.setLogin(addReq.getLogin());
		bRequest.setPassword(addReq.getCredential());
		bRequest.setAccountNumber(addReq.getAccountNumber());
		if (!StringUtils.isEmpty(addReq.getKycNumber()))
			bRequest.setKycNumber(addReq.getKycNumber());
		if (!StringUtils.isEmpty(addReq.getMobileNumber()))
			bRequest.setMobileNumber(addReq.getMobileNumber());
		if (StringUtils.isNotBlank(addReq.getCardExpiryDate()))
			bRequest.setCardExpiry(addReq.getCardExpiryDate());

		Integer transactionType = null;
		if (addReq.getMethodType() == CustomerAccountMethodType.BANK_DEBITPIN.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.BANK_ACCOUNT.value()));
			transactionType = BankTransactionType.AUTHENTICATION_CARD.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.BANK_INB.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.BANK_ACCOUNT.value()));
			transactionType = BankTransactionType.AUTHENTICATION_INB.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.BANK_MOBILEPIN.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.BANK_ACCOUNT.value()));
			transactionType = BankTransactionType.AUTHENTICATION_MOBILE.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.CREDITCARD_PIN.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.CREDIT_CARD.value()));
			transactionType = BankTransactionType.AUTHENTICATION_CARD.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.CREDITCARD_INB.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.CREDIT_CARD.value()));
			transactionType = BankTransactionType.AUTHENTICATION_INB.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.CREDITCARD_MOBILEPIN.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.CREDIT_CARD.value()));
			transactionType = BankTransactionType.AUTHENTICATION_MOBILE.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.PAYMENTBANK_PIN.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.PAYMENT_BANK.value()));
			transactionType = BankTransactionType.AUTHENTICATION_CARD.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.PAYMENTBANK_INB.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.PAYMENT_BANK.value()));
			transactionType = BankTransactionType.AUTHENTICATION_INB.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.PAYMENTBANK_MOBILEPIN.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.PAYMENT_BANK.value()));
			transactionType = BankTransactionType.AUTHENTICATION_MOBILE.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.WALLET_INB.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.WALLET.value()));
			transactionType = BankTransactionType.AUTHENTICATION_INB.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.BANK_ACCOUNTOTP.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.BANK_ACCOUNT.value()));
			transactionType = BankTransactionType.AUTHENTICATION_ACCOUNT.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.BANK_SDK.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.BANK_ACCOUNT.value()));
			transactionType = BankTransactionType.AUTHENTICATION_INB.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.BANK_DEBITCVV.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.BANK_ACCOUNT.value()));
			transactionType = BankTransactionType.AUTHENTICATION_INB.value();
		} else if (addReq.getMethodType() == CustomerAccountMethodType.ADDED_BY_BRANCH.value()) {
			bRequest.setAuthenticationType(Integer.toString(BankAuthenticationType.BANK_ACCOUNT.value()));
			transactionType = BankTransactionType.AUTHENTICATION_ACCOUNT.value();
		}

		// add transaction type
		bRequest.setTransactionType(String.valueOf(transactionType));
		bRequest.setUserId(String.valueOf(user.getId()));
		String dipcoinReferenceNumber = null;

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankRequest", bRequest).format());
		BankRequestContext bankRequestContext = new BankRequestContext();
		bankRequestContext.setTraceId(httpServletContext.getTraceId());

		Future<UserAuthenticationResponse> bResponseTask = null;

		boolean authenticateUser = false;
		if (!(addReq.getMethodType() == CustomerAccountMethodType.WALLET_INB.value())) {
			authenticateUser = true;
		}
		if (oauthRequest) {
			authenticateUser = false;
		}

		if (authenticateUser) {
			dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(bank.getReferenceId(),
					bRequest.getOperation(), transactionType);
			bRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
			bResponseTask = bankAPIServices.userAuthorization(bankRequestContext, bRequest);
		} else {
			dipcoinReferenceNumber = addReq.getDipcoinRefrenceNumber();
		}
		// get existing primary account
		BankAccount iAccount = null;
		for (BankAccount acct : bank.getBankAccounts()) {
			if (acct.getIsPrimaryAccount() == BooleanStatus.YES.value()) {
				iAccount = bank.getBankAccounts().get(0);
				break;
			}
		}
		// if no primary account
		if (iAccount == null) {
			// cancel bank transaction
			bResponseTask.cancel(true);

			paymentSourceMetricRegistry.paymentSourceTotal().increment();
			// return error
			response.addHeaderCode(HeaderCode.BANK_INVALID_ACCT);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		// init new account
		CustomerAccount cAccount = new CustomerAccount();
		UserAuthenticationResponse bResponse = null;
		if (authenticateUser) {
			bResponse = bResponseTask.get();
		}

		if (addReq.getForgotPin()) {

			response.setUserAuthenticationResponse(bResponse);
			response.setDipcoinReferenceNumber(dipcoinReferenceNumber);
			LOG.debug(LogFormatter.instance().message("Forgot pin verify request").data("response", response).format());
			return ResponseEntity.ok(response);
		}

		cAccount.setUser(user);
		cAccount.setLogin(addReq.getMaskedLogin());
		if (oauthRequest) {
			cAccount.setLogin(addReq.getMaskedAccountNumber());
		}
		cAccount.setAccountNumber(addReq.getMaskedAccountNumber());
		cAccount.setIsPrimary(
				CollectionUtils.isEmpty(existingAccounts) ? BooleanStatus.YES.value() : addReq.getIsPrimaryAccount());
		cAccount.setBank(bank);
		cAccount.setIFSCCode(iAccount.getIFSCCode());
		cAccount.setTypeOfMethod(addReq.getMethodType());
		cAccount.setClientTransactionId(clientTransactionId);
		cAccount.setIPAddress(originIp);
		cAccount.setThresholdAmount(addReq.getThresholdAmount());

		if (StringUtils.isNoneBlank(walletBankReferenceId)) {
			Bank walletReferenceId = bankDBService.getBank(walletBankReferenceId);
			cAccount.setWalletBankId(walletReferenceId != null ? walletReferenceId.getId() : NumberUtils.INTEGER_ZERO);

			// For WALLET_INB accounts, set the bankUId from the walletBankReferenceId
			// parameter
			// This is the virtual account's bankUId that was just created
			if (addReq.getTypeOfMethod() != null
					&& addReq.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()) {
				cAccount.setBankUId(walletBankReferenceId);
				LOG.info("Set bankUId for WALLET_INB account: " + walletBankReferenceId);
			}
		}

		if (userDBService.isVirtualBank(user)) {
			cAccount.setMaxCoinLimit(DBConstants.DIPCOIN_THRESHOLD_MAX_AMOUNT);
		} else {
			cAccount.setMaxCoinLimit(dbConfig.getMaxDipcoinThreshold());
		}
		cAccount.setDipcoinExpiryTTL(addReq.getOstaTTLInHrs());
		if (addReq.getAccountNumber() != null) {
			LOG.debug(LogFormatter.instance().message("update hashed account num in the DB")
					.data("addReq.getAccountNumber() ", addReq.getAccountNumber()).format());
			cAccount.setHashedAccountNumber(
					Utils.getHashedLogin(StringUtils.stripStart(addReq.getAccountNumber(), "0")));

		}

		cAccount.setDipcoinAutoGenerate(
				addReq.getAutoGenerateOsta() ? BooleanStatus.YES.value() : BooleanStatus.NO.value());
		cAccount.setHashedLogin(addReq.getHashedLogin());
		if (oauthRequest && BooleanStatus.YES.value() == addReq.getIsTpinEnabled()) {

			// we are not storing Tpin if Bank's SSO is enabled
			if (addReq.getTPin().isBlank() && bank.getIsSsoEnabled() == BooleanStatus.NO.value()) {
				LOG.debug(LogFormatter.instance().message("tpin is not in the request").format());
				response.addHeaderCode(HeaderCode.BAD_REQUEST);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			cAccount.setTpinSalt(addReq.getTpinSalt());
			cAccount.setTpinEnable(BooleanStatus.YES.value());
			cAccount.setTpinLength(addReq.getTPinLength());
			cAccount.setTpin(addReq.getTPin());

		}

		if (!StringUtils.isEmpty(addReq.getKycNumber()))
			cAccount.setKycNumber(addReq.getKycNumber());
		if (authenticateUser) {

			cAccount.setStatus(
					(bResponse != null && BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode()))
							? CustomerAccountStatus.BANK_OTP_NOT_VERIFIED.value()
							: CustomerAccountStatus.DELETED.value());
		} else {
			// For WALLET_INB accounts, set to ACTIVE immediately (no OTP verification
			// needed)
			if (addReq.getTypeOfMethod() != null
					&& addReq.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()) {
				cAccount.setStatus(CustomerAccountStatus.ACTIVE.value());
			} else {
				cAccount.setStatus(CustomerAccountStatus.BANK_OTP_NOT_VERIFIED.value());
			}
		}
		// add new account
		// NOTE: Use addAccount() directly instead of asyncAddAccount().get().
		// asyncAddAccount() is @Async and spawns a NEW DB transaction in a separate
		// thread.
		// Calling .get() while the main thread's @Transactional context still holds
		// row-level locks on CustomerAccount causes a Lock Wait Timeout deadlock.
		// The synchronous call participates in the existing transaction safely.
		LOG.debug(LogFormatter.instance().message("Adding customer account").format());
		cAccount = this.customerDBService.addAccount(cAccount);

		// failed to create new account
		if (cAccount == null) {
			// cancel bank transaction if one was started (not started for WALLET_INB /
			// oauth flow)
			if (bResponseTask != null) {
				bResponseTask.cancel(true);
			}

			paymentSourceMetricRegistry.paymentSourceTotal().increment();

			response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		// init bank transaction
		BankTransaction bTx = new BankTransaction();
		bTx.setCustomerAccountId(cAccount.getId());
		bTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
		bTx.setType(transactionType);
		bTx.setBankId(bank.getId());

		// get response from bank
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankResponse", bResponse)
				.data("DipcoinReferenceNumber", dipcoinReferenceNumber).format());

		// bank failure
		if (authenticateUser) {
			if (bResponse == null || !BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode())
					|| !dipcoinReferenceNumber.equals(bResponse.getDipcoinReferenceNumber())) {

				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Failed to add user account with bank").format());
				bTx.setStatus(BankTransactionsStatus.FAILED.value());
				if (bResponse != null) {
					bTx.setRequestTime(bResponse.getRequestTime());
					bTx.setResponseTime(bResponse.getResponseTime());
					bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
					bTx.setRawBankResponse(bResponse.getErrorMsg());
					bTx.setBankResponseCode(bResponse.getBankResponseCode());
					if (StringUtils.isNotEmpty(bResponse.getAccountNumber())) {
						cAccount.setHashedAccountNumber(
								Utils.getHashedLogin(StringUtils.stripStart(bResponse.getAccountNumber(), "0")));
						response.addHeaderCode(HeaderCode.USER_ACCOUNT_ALREADY_LINKED);
						LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Account is already added by another methode")
								.data("bResponse.getAccountNumber()", cAccount.getHashedAccountNumber()).format());
					}
				}
				if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
					LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to add Bank Transaction").data("transaction", bTx).format());
				}

				if (bResponse != null
						&& BankResponseStatus.MOBILE_NOT_REGISTERED.code().equals(bResponse.getBankResponseCode()))
					response.addHeaderCode(HeaderCode.MOBILE_NOT_REGISTERED);
				else
					response.addHeaderCode(HeaderCode.BANK_FAILED_TO_AUTHORIZE_USER);

				paymentSourceMetricRegistry.paymentSourceTotal().increment();

				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
			}
		}

		bTx.setRawBankRequest(bResponse != null ? bResponse.getRawRequest() : null);

		// populate bank transaction
		bTx.setStatus(BankTransactionsStatus.SUCCESS.value());
		if (authenticateUser && bResponse != null) {
			bTx.setRawBankResponse(bResponse.getRawData());
			bTx.setBankResponseCode(bResponse.getBankResponseCode());
			bTx.setRequestTime(bResponse.getRequestTime());
			bTx.setResponseTime(bResponse.getResponseTime());
			bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank Services User Authorization")
					.data("requestTime", bResponse.getRequestTime()).data("responseTime", bResponse.getResponseTime())
					.format());

			if (!StringUtils.isEmpty(bResponse.getAccountId()))
				cAccount.setBankUId(bResponse.getAccountId());
			if (!StringUtils.isEmpty(bResponse.getOtp()))
				cAccount.setOtp(bResponse.getOtp());
			if (!StringUtils.isEmpty(bResponse.getPhoneNumber()))
				cAccount.setMobileNumber(bResponse.getPhoneNumber());

			if (!StringUtils.isEmpty(bResponse.getAccountNumber()))
				cAccount.setRawAccountNumber(bResponse.getAccountNumber());
			if (!StringUtils.isEmpty(addReq.getAccountNumber()))
				cAccount.setRawAccountNumber(addReq.getAccountNumber());
		} else {
			if (!StringUtils.isEmpty(addReq.getAccountNumber()))
				cAccount.setRawAccountNumber(addReq.getAccountNumber());

			bTx.setRequestTime(addReq.getTransactionTime());
			bTx.setResponseTime(addReq.getTransactionTime());
			bTx.setBankTransactionRefId(addReq.getBankTransactionRefrenceNumber());
		}
		// add bank transaction
		if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to add bank transaction")
					.data("transaction", bTx).format());
		}

		// update account with kyc flag
		// @TODO - This flag is a mandate. Remove 'if' check once Bank starts sending
		// the flag.
		if (authenticateUser && bResponse != null) {
			if (!StringUtils.isBlank(bResponse.getKYCStatus())) {
				cAccount.setKycFlag(Integer.parseInt(bResponse.getKYCStatus().trim()));
			}
		}

		// failed to update account
		if (this.customerDBService.updateAccount(cAccount) == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Failed to update Customer Account").data("CustomerAccount", cAccount.getId()).format());
			response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
			paymentSourceMetricRegistry.paymentSourceTotal().increment();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

		// response setup
		populateCustomerAccountResponse(encryptionResource, user, cAccount, bank, response);
		response.addHeaderCode(HeaderCode.USER_ACCOUNT_ADDED);
		response.setId(cAccount.getId());

		paymentSourceMetricRegistry.paymentSourceSuccess().increment();
		paymentSourceMetricRegistry.paymentSourceTotal().increment();

		return ResponseEntity.ok(response);
	}

	public void populateCustomerAccountResponse(final EncryptionResource encryptionResource, final User user,
			final CustomerAccount account, final Bank bank, CustomerAccountResponse response)
			throws APIException, Exception {
		if (account != null) {
			if (response == null)
				response = new CustomerAccountResponse();

			// Encrypt account id
			if (encryptCardId) {
				String encCardId = encryptionResource.encrypt(user, null, null,
						String.valueOf(account.getUserCardId()));
				response.setEncCardId(encCardId);
			}

			// Autotop details setting.
			try {
				TopUpDetails topUpDetails = null;
				if (StringUtils.isNoneBlank(account.getTopUpDetails())) {
					topUpDetails = objectMapper.readValue(account.getTopUpDetails(), TopUpDetails.class);

					if (topUpDetails != null) {
						response.setAutoTopAmount(topUpDetails.getAutoTopUpAmount());
						response.setAutoTopUpThresHold(topUpDetails.getAutoTopUpThresHold());
						response.setTagAutoTop(topUpDetails.getAutoTopUp());
					}

				}
			} catch (Exception e) {
				LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
						.message("Error in adding response date of top-up details.").format());
			}

			response.setAccountNumber(account.getAccountNumber()).setDateAdded(account.getRegistrationTime())
					.setIsPrimaryAccount(account.getIsPrimary()).setCardId(account.getUserCardId())
					.setStatus(account.getStatus()).setThresholdAmount(account.getThresholdAmount().toString())
					.setTypeOfMethod(account.getTypeOfMethod()).setOstaTTLInHrs(account.getDipcoinExpiryTTL())
					.setAutoGenerateOsta(account.getDipcoinAutoGenerate() == BooleanStatus.YES.value())
					.setMaskedLogin(account.getLogin()).setPerMonthLimit(account.getPerMonthLimit())
					.setMaxCoinLimit(account.getMaxCoinLimit().toString()).setTagAutoTop(bank.getAutoTopup())
					.setTollStatus(account.getTypeOfMethod() == CustomerAccountMethodType.WALLET_INB.value()
							? BankTollStatus.ACTIVE.value()
							: bank.getTollStatus());
		}
		if (bank != null) {
			if (response == null)
				response = new CustomerAccountResponse();

			response.setPerDayLimit(bank.getPerDayLimit().toString());
			response.setPerTransactionLimit(bank.getPerTransactionLimit().toString());
			response.setBankName(bank.getName());
			response.setAuthorizationMethod(bank.getAuthorizationMethod());
			response.setAlias(bank.getAlias());
			response.setReferenceId(bank.getReferenceId());

			if (!StringUtils.isEmpty(bank.getApiCustomization())) {
				Object alpha = objectMapper.readValue(bank.getApiCustomization(), Object.class);
				response.setApiCustomization((List<APICustomization>) alpha);

			}

		}
	}

	public ResponseEntity verifyAccount(final User user, final CustomerAccountVerifyRequest verifyReq,
			Boolean verifyOTP) throws Exception, APIException {

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Request", verifyReq).format());
		CustomerAccountResponse accountResponse = new CustomerAccountResponse();

		if (verifyReq == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(accountResponse);
		}

		if (!verifyReq.validate(httpServletContext)) {
			accountResponse.addHeaderCodes(verifyReq.getErrorCodes());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(accountResponse);
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Fetch CustomerAccount")
				.data("UserId", user.getId()).data("CardId", verifyReq.getCardId()).format());
		CustomerAccount account = this.customerDBService.getAccount(user.getId(), verifyReq.getCardId());

		// account exists
		if (account == null) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
		}

		// fetch verify account transactions
		List<Integer> types = new LinkedList<>();
		types.add(BankTransactionType.AUTHENTICATION_CARD.value());
		types.add(BankTransactionType.AUTHENTICATION_INB.value());
		types.add(BankTransactionType.AUTHENTICATION_MOBILE.value());
		types.add(BankTransactionType.AUTHENTICATION_ACCOUNT.value());
		Future<List<BankTransaction>> bTxAcctTask = this.bankDBService.asyncGetTransactions(account, types);

		// fetch verify otp transactions
		// types = new LinkedList<>();
		// types.add(BankTransactionType.OTP_SUCCESS.value());
		// Future<List<BankTransaction>> bTxVerifyTask =
		// this.bankDBService.asyncGetTransactions(account, types);

		// account bank is active
		Bank bank = account.getBank();
		if (!this.bankDBService.isActive(bank)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_NOT_ACTIVE));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.data("bank reference Id", bank.getReferenceId()).format());

		// account is not deleted
		if (CustomerAccountStatus.DELETED.equals(account.getStatus())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.USER_ACCOUNT_DOESNT_EXIST.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
		}

		BankProperties bankProperties = null;
		try {
			bankProperties = bankAPIServices.getBankProperties(bank.getReferenceId());
		} catch (NoSuchBeanDefinitionException e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.BANK_CONFIG_DOESNT_EXIST.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.BANK_CONFIG_DOESNT_EXIST));
		}

		if ((bankProperties != null && bankProperties.isOstaSideAccountVerification())
				|| account.getTypeOfMethod() == CustomerAccountMethodType.BANK_SDK.value()) {
			final Long timeLeft = Long.valueOf(account.getVerificationTime())
					- DateTime.now(DateTimeZone.UTC).getMillis();
			if (timeLeft < 0) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.BAD_REQUEST.message()).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));
			}
			if (verifyReq.getOtp().equals(account.getOtp())) {
				CustomerAccount updatedAccount = this.customerDBService.activateAccount(account);
				if (updatedAccount == null) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message(HeaderCode.BAD_REQUEST.message()).format());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body(APIResponse.error(HeaderCode.BAD_REQUEST));
				}

				if (verifyReq.getForgotPin()) {
					accountResponse.setOstaSideAccountVerification(Boolean.TRUE);
					return ResponseEntity.ok().body(accountResponse);

				}
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.USER_ACCOUNT_ACTIVATED.message()).format());
				return ResponseEntity.ok(APIResponse.error(HeaderCode.USER_ACCOUNT_ACTIVATED));
			}
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message(HeaderCode.USER_INVALID_OTP.message()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.USER_INVALID_OTP));
		}

		// send request to bank to verify OTP
		// @TODO - move the UID logic to bank-services
		VerifyOTPRequest bRequest = new VerifyOTPRequest(bank.getReferenceId(), bank.getCode());
		bRequest.setOtpCode(verifyReq.getOtp());
		if (DBConstants.BankAuthorizationMethod.BANK_PIN.value() == bank.getAuthorizationMethod()) {
			if (StringUtils.isEmpty(verifyReq.getAuthorizationPin())) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
						.message(HeaderCode.EMPTY_BANK_PIN.message()).format());
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.EMPTY_BANK_PIN));
			}

			bRequest.setAuthorizationPin(verifyReq.getAuthorizationPin());
		}
		bRequest.setTransactionType(Integer.toString(BankTransactionType.OTP_SUCCESS.value()));
		String dipcoinReferenceNumber = null;
		// set bank provided tx number
		List<BankTransaction> bTxs = bTxAcctTask.get();
		if (bTxs == null || bTxs.size() == 0) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to fetch Bank Transaction")
					.data("CustomerAccountId", account.getId()).data("Type", account.getTypeOfMethod()).format());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Btx").data("bTxs", bTxs).format());

		bRequest.setBankTransactionReferenceNumber(bTxs.get(0).getBankTransactionRefId());

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankRequest", bRequest).format());
		BankRequestContext bankRequestContext = new BankRequestContext();
		bankRequestContext.setTraceId(httpServletContext.getTraceId());

		if (verifyReq.getMethodType() == null) {
			verifyOTP = true;
		}
		Future<VerifyOTPResponse> bResponseTask = null;
		if (verifyOTP) {
			dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(bank.getReferenceId(),
					bRequest.getOperation(), BankTransactionType.OTP_SUCCESS.value());
			bRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
			bResponseTask = this.bankAPIServices.verifyOTP(bankRequestContext, bRequest);
		} else {
			dipcoinReferenceNumber = verifyReq.getDipcoinRefrenceNumber();
		}

		// init bank transaction
		BankTransaction bTx = new BankTransaction();
		bTx.setType(BankTransactionType.OTP_SUCCESS.value());
		bTx.setCustomerAccountId(account.getId());
		bTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
		bTx.setBankId(bank.getId());

		// system failure
		VerifyOTPResponse bResponse = null;
		if (verifyOTP) {
			bResponse = bResponseTask.get();
		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankResponse", bResponse)
				.data("DipcoinReferenceNumber", dipcoinReferenceNumber).format());

		if (verifyReq.getForgotPin()) {
			accountResponse.setDipcoinReferenceNumber(dipcoinReferenceNumber);
			accountResponse.setVerifyOTPResponse(bResponse);
			return ResponseEntity.ok().body(accountResponse);

		}

		// bank failure
		if (verifyOTP) {
			if (bResponse == null || !BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode())
					|| !dipcoinReferenceNumber.equals(bResponse.getDipcoinReferenceNumber())) {
				LOG.debug("Failed to verify user account with bank");
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
							.message("Failed to verify user with bank").data("transaction", bTx).format());
				}

				// throw exception to initiate rollback
				if (bResponse == null) {
					throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
							APIResponse.error(HeaderCode.INTERNAL_ERROR));
				}
				throw new APIException(HttpStatus.BAD_REQUEST, APIResponse.error(HeaderCode.BAD_REQUEST));
			}
		}

		// populate bank transaction
		bTx.setStatus(BankTransactionsStatus.SUCCESS.value());
		bTx.setRawBankRequest(bResponse != null ? bResponse.getRawRequest() : null);
		if (verifyOTP && bResponse != null) {
			bTx.setRawBankResponse(bResponse.getRawData());
			bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
			bTx.setRequestTime(bResponse.getRequestTime());
			bTx.setResponseTime(bResponse.getResponseTime());
			bTx.setBankResponseCode(bResponse.getBankResponseCode());
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank Services User Verification")
					.data("requestTime", bResponse.getRequestTime()).data("responseTime", bResponse.getResponseTime())
					.format());
			if (StringUtils.isNotEmpty(bResponse.getAccountNumber())) {

				LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
						.message("account number is there in response of verify OTP from bank").format());
				// verify if account already added
				List<CustomerAccount> existingAccounts = this.customerDBService
						.asyncGetAccounts(user.getId(), Arrays.asList(DBConstants.CustomerAccountStatus.ACTIVE.value()))
						.get();
				for (CustomerAccount userAccount : existingAccounts) {
					// hashed login match
					if (StringUtils.isNotEmpty(userAccount.getHashedAccountNumber())) {
						LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
								.message("hasehed account num field of user account is not empty").format());
						LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
								.data(" userAccount.getHashedAccountNumber()", userAccount.getHashedAccountNumber())
								.data("hash of account num in the response ",
										Utils.getHashedLogin(StringUtils.stripStart(bResponse.getAccountNumber(), "0")))
								.format());

						if (userAccount.getBank().getReferenceId().equals(bank.getReferenceId())
								&& ((StringUtils.isNotEmpty(userAccount.getHashedLogin()) && userAccount
										.getHashedLogin()
										.equals(Utils.getHashedLogin(
												StringUtils.stripStart(bResponse.getAccountNumber(), "0"))))
										|| ((StringUtils.isNotEmpty(userAccount.getHashedAccountNumber()) && userAccount
												.getHashedAccountNumber().equals(Utils.getHashedLogin(StringUtils
														.stripStart(bResponse.getAccountNumber(), "0"))))))) {

							LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
									.message("bank ref id and accounthased of existing accountnum are matching")
									.format());

							LOG.info(
									LogFormatter.instance(httpServletContext.getTraceId())
											.message("user account already linked using another method."
													+ "soft deleting latest added account")
											.data("transaction", bTx).format());
							account.setStatus(DBConstants.CustomerAccountStatus.DELETED.value());

							if (this.customerDBService.updateAccount(account) == null) {
								LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
										.message("failed to update data in DB").format());
								return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
										.body(HeaderCode.INTERNAL_ERROR);
							}

							// if account is already added then , return success
							return ResponseEntity.status(HttpStatus.OK).body(HeaderCode.USER_ACCOUNT_ALREADY_LINKED);
						}
					}
				}
				LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
						.message("bank ref id and accounthashed of existing accountnum are not matching so "
								+ "generating hash of account num and updating db")
						.format());
				account.setHashedAccountNumber(
						Utils.getHashedLogin(StringUtils.stripStart(bResponse.getAccountNumber(), "0")));

			}
		} else {
			bTx.setRequestTime(verifyReq.getTransactionTime());
			bTx.setResponseTime(verifyReq.getTransactionTime());
			bTx.setBankTransactionRefId(verifyReq.getBankTransactionRefrenceNumber());
		}
		if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to add bank transaction")
					.data("transaction", bTx).format());
		}

		// activate account
		LOG.debug(
				LogFormatter.instance(httpServletContext.getTraceId()).message("Activating customer account").format());
		account.setOtp(verifyReq.getOtp());
		if (verifyOTP && bResponse != null) {
			account.setBankUId(bResponse.getBankUID());

			if (StringUtils.isEmpty(account.getAccountNumber())) {
				if (!StringUtils.isEmpty(bResponse.getAccountNumber())) {
					account.setRawAccountNumber(bResponse.getAccountNumber());
				} else {
					if (!StringUtils.isEmpty(bResponse.getBankUID()))
						account.setRawAccountNumber(bResponse.getBankUID());
				}
			}
		} else {
			account.setBankUId(verifyReq.getBankUId());
		}
		CustomerAccount updatedAccount = this.customerDBService.activateAccount(account);
		if (updatedAccount != null) {

			// send SMS
			if (verifyOTP) {
				if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(user.getPhone(),
						Templates.CustomerAddPaymentSource.format(
								Utils.getMaskedLogin(account.getLogin(), account.getTypeOfMethod()), bank.getAlias()),
						httpServletContext.getClientFeatureFlags().smsEnabled())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
							.data("phone", user.getPhone())
							.data("template",
									Templates.CustomerAddPaymentSource.format(
											Utils.getMaskedLogin(account.getLogin(), account.getTypeOfMethod()),
											bank.getAlias()))
							.format());
				}

				if (applicationProperties.getAwsSMSClient()) {

					NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
					notificationRequestContext.setTraceId(httpServletContext.getTraceId());
					if (!notificationResource.sendSms(user.getPhone(), Templates.CustomerAddPaymentSource.format(
							Utils.getMaskedLogin(account.getLogin(), account.getTypeOfMethod()), bank.getAlias()),
							httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {

						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send SMS")
								.data("phone", user.getPhone())
								.data("template",
										Templates.CustomerAddPaymentSource.format(
												Utils.getMaskedLogin(account.getLogin(), account.getTypeOfMethod()),
												bank.getAlias()))
								.format());

					}
				}

				// send email to user
				if (user.getIsEmailVerified() == BooleanStatus.YES.value()
						&& !emailUtils.sendNewCustomerAccountEmail(user, account, bank)) {
					LOG.error("Failed to send email to user " + user.getId() + " for account "
							+ updatedAccount.getUserCardId());
				}
			}

			return ResponseEntity.ok(APIResponse.error(HeaderCode.USER_ACCOUNT_ACTIVATED));

		}

		LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to validate customer account")
				.format());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
	}

}
