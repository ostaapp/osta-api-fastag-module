package com.dipcoin.api.resource;

import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIConstants.RequestSource;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.PartnerApprovalHelper;
//import com.dipcoin.api.commons.PartnerApprovalHelper;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APICustomization;
import com.dipcoin.api.model.APIResponse;
//import com.dipcoin.api.model.BankCustomerRegisterRequest;
import com.dipcoin.api.model.BankInfoResponse;
//import com.dipcoin.api.model.BankSubUserRequest;
import com.dipcoin.api.model.BrontooUserRegisterRequest;
//import com.dipcoin.api.model.CustomerAccountRequest;
//import com.dipcoin.api.model.CustomerAccountResponse;
//import com.dipcoin.api.model.CustomerAccountVerifyRequest;
//import com.dipcoin.api.model.EmailVerificationResponse;
import com.dipcoin.api.model.LoginSessionResponse;
//import com.dipcoin.api.model.LoginUsingPinRequest;
import com.dipcoin.api.model.MerchantInfoResponse;
import com.dipcoin.api.model.PartnerUserRegisterRequest;
//import com.dipcoin.api.model.UserCredentialResetRequest;
import com.dipcoin.api.model.UserInfoResponse;
//import com.dipcoin.api.model.UserLoginRequest;
import com.dipcoin.api.model.UserPreferencesResponse;
import com.dipcoin.api.model.UserRegisterRequest;
import com.dipcoin.api.model.UserUpdateRequest;
import com.dipcoin.api.model.UserVerifyRequest;
//import com.dipcoin.api.model.UserVerifyRequest;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.comm.UserAuthenticationResponse;
import com.dipcoin.bank.services.comm.VerifyOTPResponse;
import com.dipcoin.bank.services.utils.BankConstants.BankResponseStatus;
import com.dipcoin.bank.services.utils.BankProperties;
import com.dipcoin.bank.services.utils.BankServiceException;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.LogFormatter.Mask;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.commons.config.CommonsProperties;
import com.dipcoin.commons.qrcode.QRCodeUtils;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.db.services.AppInfoDBService;
//import com.dipcoin.db.services.BankCustomerMappingDBService;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.BankSubUsersDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.UserDBServiceImpl;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BankStatus;
import com.dipcoin.db.services.commons.DBConstants.BankSubUserStatus;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.DeviceStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantStatus;
import com.dipcoin.db.services.commons.DBConstants.TollTagApprovalStatus;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.UserStatus;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.model.AppInfo;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankAccount;
import com.dipcoin.db.services.model.BankCustomerMap;
import com.dipcoin.db.services.model.BankSubUsers;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.MerchantOnboard;
import com.dipcoin.db.services.model.Session;
import com.dipcoin.db.services.model.User;
import com.dipcoin.db.services.model.UserDevice;
//import com.dipcoin.jwt.JwtBlacklistService;
//import com.dipcoin.jwt.JwtUtil;
import com.dipcoin.metrics.UserMetricRegistry;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalFields;
import com.dipcoin.db.services.commons.DBConstants.PasswordPinReset;

@Component("userLoginResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class UserLoginResource {

	private static final Logger LOG = LogManager.getLogger(UserLoginResource.class);
	private final static ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private HttpServletContext httpServletContext;

	@Autowired
	private BankSubUsersDBService bankSubUsersDBService;

	@Autowired
	private UserDBService userDBService;

//	@Autowired
//	private JwtBlacklistService jwtBlacklistService;
//
//	@Autowired
//	private JwtUtil jwtUtil;

	@Autowired
	private CryptoUtil cryptoUtil;

	@Autowired
	private UserUtil userUtil;

	@Autowired
	private ApplicationProperties applicationProperties;

	@Autowired
	@Qualifier("com.dipcoin.metrics.UserMetricRegistry")
	private UserMetricRegistry userMetricRegistry;

	@Autowired
	private CustomerDBService customerDBService;

//	@Autowired
//	private CustomerResource customerResource;

	@Autowired
	private UserLoginSession userLoginSession;

	@Autowired
	private EncryptionResource encryptionResource;

	@Autowired
	private AppInfoDBService appInfoDBService;

	@Autowired
	private MerchantDBService merchantDBService;

	@Autowired
	private BankDBService bankDBService;

//	@Autowired
//	private BankCustomerMappingDBService bankCustomerMappingDBService;

	@Autowired
	private SmsClient smsClient;

	@Autowired
	private EmailUtils emailUtils;

	@Autowired
	private NotificationResource notificationResource;

	@Autowired
	private DipcoinDBService coinDBService;

	@Autowired
	private TollDBService tollDBService;

	// @Autowired
	// private MerchantInfoResponse merchantInfoResponse;

//	@Autowired
//	private OAuth2CustomerResource oAuth2CustomerResource;

	 @Autowired
	 private PartnerApprovalHelper partnerApprovalHelper;

	@Autowired
	private BankAPIServices bankAPIServices;

	@Autowired
	private UserDBServiceImpl userDBServiceImpl;

	/*
	 * User Register
	 */
	public ResponseEntity<UserInfoResponse> registerUser(final UserRegisterRequest createReq, String partner,
			String clientTransactionId,
			User oauthUser, Merchant oauthMerchant, Boolean sendOtpEmail) throws Exception, APIException {

		HttpServletRequest httpServletRequest = httpServletContext.getServletRequest();
		String originIp = httpServletContext.getOriginIp();
		int originPort = httpServletContext.getOriginPort();

		boolean merchantRequest = APIConstants.MERCHANT.equals(partner.toUpperCase());
		boolean bankRequest = APIConstants.BANK.equals(partner.toUpperCase());
		boolean dipcoinRequest = APIConstants.BRONTOO.equals(partner.toUpperCase());
		boolean virtual = APIConstants.VIRTUAL_CUSTOMER.equals(partner.toUpperCase())
				|| APIConstants.VIRTUAL_MERCHANT.equals(partner.toUpperCase());

		UserInfoResponse response = new UserInfoResponse();
		response.setClientTransactionId(clientTransactionId);

		// verify request is valid and clientTransactionId is provided

		if (clientTransactionId == null) {
			response.addHeaderCode(HeaderCode.MISSING_CLIENTTRANSACTIONID);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		if (createReq == null) {
			response.addHeaderCode(HeaderCode.BAD_REQUEST);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		if (((merchantRequest || bankRequest) && !((PartnerUserRegisterRequest) createReq).validate(httpServletContext))
				|| !createReq.validate(httpServletContext)) {
			// LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
			// .data("Invalid Request", createReq).format());

			response.addHeaderCodes(createReq.getErrorCodes());
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Invalid Request", createReq)
					.data("Request Header Error code", createReq.getErrorCodes()).format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Register User")
				.data("EmailId", createReq.getEmail()).data("Phonenum", createReq.getPhonenum())
				.data("originIp", originIp).format());

		List<String> userRoles = APIUtils.getUserRoles(merchantRequest, bankRequest, dipcoinRequest, virtual);
		// fetch user on the basis of roles to see if existing

		List<User> existingUsers = null;
		if (oauthUser != null) {
			existingUsers = this.userDBService.getUsers(null, createReq.getPhonenum(), userRoles,
					oauthUser.getBankMerchantId());

		} else if (!(merchantRequest || bankRequest)) {
			existingUsers = this.userDBService.getUsers(null, createReq.getPhonenum(), userRoles, null);

		} else {
			existingUsers = this.userDBService.getUsers(createReq.getEmail(), createReq.getPhonenum(), userRoles, null);

		}

		Merchant merchant = null;
		MerchantOnboard merchantOnboard = null;
		Bank bank = null;

		String partnerReferenceId = null;
		if (merchantRequest) {
			// if merchant user request, fetch merchant and admin users
			partnerReferenceId = ((PartnerUserRegisterRequest) createReq).getPartnerReferenceId();
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Merchant Request: " + partnerReferenceId).format());
			if (!StringUtils.isEmpty(partnerReferenceId) && partnerReferenceId.charAt(0) != 'M') {
				response.addHeaderCode(HeaderCode.PARTNER_REF_ID_NOT_PROVIDED);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			merchant = this.merchantDBService.getMerchant(partnerReferenceId);
			merchantOnboard = this.merchantDBService.getMerchantOnboardByReferenceId(partnerReferenceId);
		} else if (bankRequest) {
			// if bank user request, fetch bank and admin users
			partnerReferenceId = ((PartnerUserRegisterRequest) createReq).getPartnerReferenceId();
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Bank Request: " + partnerReferenceId).format());
			if (!StringUtils.isEmpty(partnerReferenceId) && partnerReferenceId.charAt(0) != 'B') {
				response.addHeaderCode(HeaderCode.PARTNER_REF_ID_NOT_PROVIDED);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			bank = this.bankDBService.getBank(partnerReferenceId);
		}

		// fetch user
		List<User> adminsList = null;

		// if merchant user request, verify is user belongs to merchant
		if (merchantRequest) {
			// fetch merchant
			if (merchant == null) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Merchant doesn't exists")
						.format());
				response.addHeaderCode(HeaderCode.MERCHANT_DOESNT_EXIST);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			List<String> roles = new ArrayList<>();
			roles.add(UserRoles.MERCHANT_SUPERADMIN.value());
			roles.add(UserRoles.MERCHANT_ADMIN.value());

			adminsList = this.userDBService.getBankMerchantUsers(merchant.getId(), roles);

		} else if (bankRequest) {
			// fetch bank
			if (bank == null) {
				LOG.debug(
						LogFormatter.instance(httpServletContext.getTraceId()).message("Bank doesn't exists").format());
				response.addHeaderCode(HeaderCode.BANK_DOESNT_EXISTS);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			List<String> roles = new ArrayList<>();
			roles.add(UserRoles.BANK_SUPERADMIN.value());
			roles.add(UserRoles.BANK_ADMIN.value());
			adminsList = this.userDBService.getBankMerchantUsers(bank.getId(), roles);
		}

		// is wallet user already existing
		/*
		 * if(virtual) {
		 * 
		 * List<String> walletUserRoles = APIUtils.getWalletUserRoles(); //fetch users
		 * on basis or phone,email,role,bankmerchnatId List<User> existingWalletUsers =
		 * this.userDBService.getUsers(createReq.getEmail(), createReq.getPhonenum(),
		 * oauthUser.getBankMerchantId(), walletUserRoles); if(existingWalletUsers !=
		 * null) { for (User user : existingWalletUsers) {
		 * 
		 * if (user.getStatus() != UserStatus.INACTIVE.value()){
		 * response.addHeaderCode(HeaderCode.USER_EXISTS); return
		 * RequestHandler.responseBuilder(response).status(Status.BAD_REQUEST).build();
		 * } } }
		 * 
		 * }
		 */
		// is user already existing
		/* if(!(virtual)) { */

		if (!CollectionUtils.isEmpty(existingUsers)) {
			User existingUser = existingUsers.get(0);
			if (existingUser.getStatus() != UserStatus.INACTIVE.value()
					|| existingUser.getIsOTPVerified() != BooleanStatus.NO.value()) {

				// if same transactionId is requested
				if (existingUser.getClientTransactionId() != null
						&& existingUser.getClientTransactionId().equals(clientTransactionId)
						&& existingUser.getPhone().equals(createReq.getPhonenum())
						&& existingUser.getEmail().equals(createReq.getEmail())) {
					// if user doesnt belong to merchant
					if (merchant != null && existingUser.getBankMerchantId() != merchant.getId()) {
						response.addHeaderCode(HeaderCode.MERCHANT_UNAUTHORIZED);
						return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
					}
					// if user doesnt belong to institue
					else if (bank != null && existingUser.getBankMerchantId() != bank.getId()) {
						response.addHeaderCode(HeaderCode.BANK_UNAUTHORIZED);
						return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
					}

					else if (virtual) {
						response.addHeaderCode(HeaderCode.USER_EXISTS);
						return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
					}

					populateUserInfoResponse(existingUser, merchant, bank, response);
					return ResponseEntity.status(HttpStatus.CREATED).body(response);
				}

				// else
				response.addHeaderCode(HeaderCode.USER_EXISTS);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			// update unverified existing user
			existingUser.setEmail(existingUser.getEmail() + CoreUtils.getRandomNumber(1000, 9000));
			existingUser.setPhone(existingUser.getPhone() + CoreUtils.getRandomNumber(1000, 9000));
			if (this.userDBService.updateUser(existingUser) == null)
				throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}
		/* } */

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Create new user").format());
		// create new user
		User user = new User();
		user.setFirstName(createReq.getFname());
		user.setLastName(createReq.getLname());
		user.setEmail(createReq.getEmail());
		user.setPhone(createReq.getPhonenum());
		if (createReq.getTnc() != null) {
			if (BooleanUtils.isTrue(createReq.getTnc())) {
				user.setTnC(BooleanStatus.YES.value());
			} else {
				user.setTnC(BooleanStatus.NO.value());
			}
		}
		user.setPin(createReq.getPin());
		user.setIPAddress(originIp);
		user.setClientTransactionId(clientTransactionId);

		// if merchant user request, process user roles
		if (merchantRequest) {
			String requestedRole = ((PartnerUserRegisterRequest) createReq).getRole();

			Set<String> admins = null;
			if (!CollectionUtils.isEmpty(adminsList) && (UserRoles.MERCHANT_SUPERADMIN.equals(requestedRole)
					|| UserRoles.MERCHANT_ADMIN.equals(requestedRole))) {

				admins = new HashSet<>();

				for (User mu : adminsList) {
					if (UserRoles.MERCHANT_SUPERADMIN.equals(mu.getRole()))
						admins.add(UserRoles.MERCHANT_SUPERADMIN.value());
					else if (UserRoles.MERCHANT_ADMIN.equals(mu.getRole()))
						admins.add(UserRoles.MERCHANT_ADMIN.value());
				}
			}

			// let merchant superadmin and admin register without merchant being
			// active
			if (UserRoles.MERCHANT_SUPERADMIN.equals(requestedRole)) {
				if (admins != null && admins.contains(UserRoles.MERCHANT_SUPERADMIN.value())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Requested merchant user not superadmin").format());
					response.addHeaderCode(HeaderCode.MERCHANT_USER_NOT_SUPERADMIN);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}

				user.setRole(UserRoles.MERCHANT_SUPERADMIN.value());
			} else if (UserRoles.MERCHANT_ADMIN.equals(requestedRole)) {
				if (admins != null && admins.contains(UserRoles.MERCHANT_ADMIN.value())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Requested merchant user not admin").format());
					response.addHeaderCode(HeaderCode.MERCHANT_USER_NOT_ADMIN);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}

				user.setRole(UserRoles.MERCHANT_ADMIN.value());
			} else {
				if (!MerchantStatus.ACTIVE.equals(merchant.getStatus())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Merchant not active")
							.format());
					response.addHeaderCode(HeaderCode.MERCHANT_NOT_ACTIVE);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				if (UserRoles.MERCHANT_TRANSACTOR.equals(((PartnerUserRegisterRequest) createReq).getRole())) {
					user.setRole(UserRoles.MERCHANT_TRANSACTOR.value());
				} else if (UserRoles.MERCHANT.value().equals(((PartnerUserRegisterRequest) createReq).getRole())) {
					user.setRole(UserRoles.MERCHANT.value());
				} else {
					response.addHeaderCode(HeaderCode.MERCHANT_INVALID_ROLE);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}
			user.setBankMerchantId(merchant.getId());

			// update number of merchant users
			MerchantOnboard updatemerchantOnboard = (MerchantOnboard) BeanUtils.cloneBean(merchantOnboard);
			updatemerchantOnboard.setNumberOfUsers(merchant.getNumberOfUsers() + 1);
			updatemerchantOnboard.setOfficeNumber(createReq.getPhonenum());
			if (this.merchantDBService.updateMerchantOnboard(updatemerchantOnboard) == null) {
				response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
			}

		} else if (bankRequest) {
			String requestedRole = ((PartnerUserRegisterRequest) createReq).getRole();

			Set<String> admins = null;
			if (!CollectionUtils.isEmpty(adminsList) && (UserRoles.BANK_SUPERADMIN.equals(requestedRole)
					|| UserRoles.BANK_ADMIN.equals(requestedRole))) {

				admins = new HashSet<>();
				for (User mu : adminsList) {
					if (UserRoles.BANK_SUPERADMIN.value().equals(mu.getRole()))
						admins.add(UserRoles.BANK_SUPERADMIN.value());
					else if (UserRoles.BANK_ADMIN.value().equals(mu.getRole()))
						admins.add(UserRoles.BANK_ADMIN.value());
				}
			}

			// let bank superadmin and admin register without bank being active
			if (UserRoles.BANK_SUPERADMIN.equals(requestedRole)) {
				if (admins != null && admins.contains(UserRoles.BANK_SUPERADMIN.value())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Requested bank user not superadmin").format());
					response.addHeaderCode(HeaderCode.BANK_USER_NOT_SUPERADMIN);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}

				user.setRole(UserRoles.BANK_SUPERADMIN.value());
			} else if (UserRoles.BANK_ADMIN.equals(requestedRole)) {
				if (admins != null && admins.contains(UserRoles.BANK_ADMIN.value())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Requested bank user not admin").format());
					response.addHeaderCode(HeaderCode.BANK_USER_NOT_ADMIN);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}

				user.setRole(UserRoles.BANK_ADMIN.value());
			} else {
				if (!BankStatus.ACTIVE.equals(bank.getStatus())) {
					LOG.debug(
							LogFormatter.instance(httpServletContext.getTraceId()).message("Bank not active").format());
					response.addHeaderCode(HeaderCode.BANK_NOT_ACTIVE);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				if (UserRoles.BANK_TRANSACTOR.equals(((PartnerUserRegisterRequest) createReq).getRole())) {
					user.setRole(UserRoles.BANK_TRANSACTOR.value());
				} else if (UserRoles.BANK.equals(((PartnerUserRegisterRequest) createReq).getRole())) {
					user.setRole(UserRoles.BANK.value());
				} else {
					response.addHeaderCode(HeaderCode.BANK_INVALID_ROLE);
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}
			user.setBankMerchantId(bank.getId());
		} else if (dipcoinRequest) {
			String requestedRole = ((BrontooUserRegisterRequest) createReq).getRole();
			if (UserRoles.BRONTOO_SUPERADMIN.equals(requestedRole)) {
				user.setRole(UserRoles.BRONTOO_SUPERADMIN.value());
			} else if (UserRoles.BRONTOO_ADMIN.equals(requestedRole)) {
				user.setRole(UserRoles.BRONTOO_ADMIN.value());
			} else if (UserRoles.BRONTOO_EDITOR.equals(requestedRole)) {
				user.setRole(UserRoles.BRONTOO_EDITOR.value());
			} else {
				response.addHeaderCode(HeaderCode.BRONTOO_INVALID_ROLE);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

		} else if (virtual) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Virtual Customer request")
					.format());
			if (APIConstants.VIRTUAL_CUSTOMER.equals(partner.toUpperCase())) {
				user.setRole(UserRoles.VIRTUAL_CUSTOMER.value());
			} else if (APIConstants.VIRTUAL_MERCHANT.equals(partner.toUpperCase())) {
				user.setRole(UserRoles.VIRTUAL_MERCHANT.value());
			}
			user.setBankMerchantId(oauthUser.getBankMerchantId());
		} else {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Customer request").format());
			user.setRole(UserRoles.CUSTOMER.value());
			if (oauthMerchant != null) {

				user.setBankMerchantId(oauthMerchant.getId());

			}
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Adding new user").data("user", user)
				.maskedData("password", Mask.PASSWORD_MASKED.value()).maskedData("SecretKey", Mask.KEY_MASKED.value())
				.format());

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Register User")
				.data("createReq", createReq).format());
		// add user
		user = this.userDBService.addUser(user, createReq.getPassword());
		if (user != null) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("User object").data("user", user)
					.format());
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("New user added").format());
			response.addHeaderCode(HeaderCode.USER_REGISTERED);
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("sendOtpEmail", sendOtpEmail)
					.data("virtual", virtual).format());
			if (!virtual && sendOtpEmail) {
				// send email and sms verifications
				boolean emailSend = emailUtils.sendFirstTimeVerificationEmail(user);
				boolean smsSend = sendSmsOtp(user);
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("sending sms and email")
						.format());

				if (emailSend && smsSend) {
					response.addHeaderCode(HeaderCode.USER_RESET_EMAIL_SMS_SEND);
				} else {
					if (!emailSend) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send email")
								.format());
						response.addHeaderCode(HeaderCode.USER_VERIFY_EMAIL_FAILURE);
					}

					if (!smsSend) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Failed to send sms")
								.format());
						response.addHeaderCode(HeaderCode.USER_VERIFY_SMS_FAILURE);
					}
				}
			}

			populateUserInfoResponse(user, merchant, bank, response);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		}

		response.addHeaderCode(HeaderCode.FAIL_TO_ADD_USER);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	  public User updateUser(User bankCustomer) throws APIException, Exception {
		    return this.userDBService.updateUser(bankCustomer);
		  }

	  public List<User> getUser(String phonenum, Integer status) throws APIException, Exception {
		    // return this.userDBService.asyncGetUsers(createReq.getPhonenum(), 0,
		    // Arrays.asList(UserRoles.CUSTOMER.value()), DBConstants.UserStatus.INACTIVE.value()).get();
		    return this.userDBService.asyncGetUsers(phonenum, 0, Arrays.asList(UserRoles.CUSTOMER.value()),
		        DBConstants.UserStatus.INACTIVE.value() == status ? DBConstants.UserStatus.INACTIVE.value()
		            : DBConstants.UserStatus.ACTIVE.value())
		        .get();
		  }
		  
		  
		  public List<User> getUser(String phonenum, Integer status, Integer bankMerchantId) throws APIException, Exception {
		    // return this.userDBService.asyncGetUsers(createReq.getPhonenum(), 0,
		    // Arrays.asList(UserRoles.CUSTOMER.value()), DBConstants.UserStatus.INACTIVE.value()).get();
		    return this.userDBService.asyncGetUsers(phonenum, bankMerchantId, Arrays.asList(UserRoles.CUSTOMER.value()),
		        DBConstants.UserStatus.INACTIVE.value() == status ? DBConstants.UserStatus.INACTIVE.value()
		            : DBConstants.UserStatus.ACTIVE.value())
		        .get();
		  }
		  
		  /*
		   * 
		   */
		  public static void populateUserInfoResponse(final User user, final Merchant merchant,
		      final Bank bank, UserInfoResponse response) {

		    if (user != null) {
		      if (response == null)
		        response = new UserInfoResponse();

		      response.setEmail(user.getEmail());
		      response.setFname(user.getFirstName());
		      response.setLname(user.getLastName());
		      response.setPhonenum(user.getPhone());
		      response.setStatus(user.getStatus());
		      response.setExpertise(user.getExpertise());
		      response.setRole(user.getRole());
		      response.setRegistrationDate(user.getRegistrationDate());
		      response.setAgentId(user.getAgentId());
		      response.setPhoneAlias(user.getPhoneAlias());

		      // populate required merchant fields
		      if (merchant != null) {
		        MerchantInfoResponse info = new MerchantInfoResponse();
		        info.setPartnerReferenceId(merchant.getReferenceId());
		        info.setType(merchant.getType());
		        info.setStatus(merchant.getStatus());
		        info.setMerchantId(merchant.getId());
		        response.setMerchantInfo(info);
		      }

		      // populate required bank fields
		      if (bank != null) {
		        BankInfoResponse info = new BankInfoResponse();
		        info.setPartnerReferenceId(bank.getReferenceId());
		        info.setType(bank.getType());
		        info.setStatus(bank.getStatus());

		        response.setBankInfo(info);
		      }
		    }

		  }
	 
		  /*
		   * Send OTP SMS
		   */
		  private boolean sendSmsOtp(User user) throws IOException {
		    // @TODO - Remove once SMS gateway is confirmed
		    if (UserDBServiceImpl.getTestSMS() != null && !UserDBServiceImpl.getTestSMS().isEmpty())
		      return true;

		    if (!applicationProperties.getAwsSMSClient()
		        && smsClient.sendSms(user.getPhone(), Templates.RegistrationOtp.format(user.getOtp(),
		            user.getOTPExpiryTime(), TimeZone.getTimeZone("IST"), "HH:mm"), true))
		      return true;

		    if (applicationProperties.getAwsSMSClient()) {

		      NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
		      notificationRequestContext.setTraceId(httpServletContext.getTraceId());
		      if (!notificationResource.sendSms(user.getPhone(),
		          Templates.RegistrationOtp.format(user.getOtp(), user.getOTPExpiryTime(),
		              TimeZone.getTimeZone("IST"), "HH:mm"),
		          httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {
		        
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		            .message("Failed to send SMS").data("phone", user.getPhone())
		            .data("template", Templates.RegistrationOtp.format(user.getOtp(), user.getOTPExpiryTime(),
		                TimeZone.getTimeZone("IST"), "HH:mm")).format());
		        
		        return true;

		      }
		    }
		    
		    return false;
		  }
		  
		  /*
		   * User verify
		   */
		  public ResponseEntity verifyUserCredentials(final UserVerifyRequest verifyReq, String partner,
		      User oauthUser, Merchant oauthMerchant) throws Exception, APIException {

		    String remoteIp = httpServletContext.getOriginIp();
		    Long now = DateTime.now(DateTimeZone.UTC).getMillis();

		    boolean merchantRequest = APIConstants.MERCHANT.equals(partner.toUpperCase());
		    boolean bankRequest = APIConstants.BANK.equals(partner.toUpperCase());
		    boolean brontooRequest = APIConstants.BRONTOO.equals(partner.toUpperCase());
		    boolean virtual = APIConstants.VIRTUAL_CUSTOMER.equals(partner.toUpperCase())
		        || APIConstants.VIRTUAL_MERCHANT.equals(partner.toUpperCase());

		    UserInfoResponse userVerifyResponse = new UserInfoResponse();

		    if (verifyReq == null) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid Request")
		          .format());
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		          .body(APIResponse.error(HeaderCode.INVALID_REQUEST));
		    }
		    
		    if (verifyReq.getPin() == null) {
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Pin Cannot be empty")
		            .format());
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(APIResponse.error(HeaderCode.INVALID_REQUEST));
		      }

		    // set crypto util
		    verifyReq.initCrypto(cryptoUtil, null, httpServletContext);

		    if (!verifyReq.validate(httpServletContext)) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()) 
		          .message("Failed to validate Request").format());
		      userVerifyResponse.addHeaderCodes(verifyReq.getErrorCodes());
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(userVerifyResponse);
		    }
		    if (!verifyReq.isAccepted()) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .message("Verification Request Rejected").format());
		      return ResponseEntity.ok(APIResponse.error(HeaderCode.USER_VERIFICATION_REJECT));
		    }

		    // use async to detach @Transactional so model is not auto-updated by JPA
		    User user = null;
		    // use async to detach @Transactional so model is not auto-updated by JPA
		    List<String> userRoles =
		        APIUtils.getUserRoles(merchantRequest, bankRequest, brontooRequest, virtual);
		    if (merchantRequest || bankRequest || virtual || brontooRequest) {
		      List<User> users = null;
		      if (oauthUser != null) {    	 
		        users = APIConstants.CUSTOMER.equals(partner.toUpperCase()) ? this.userDBService
		                .asyncGetUsers(verifyReq.getEmail(), verifyReq.getPhonenum(), userRoles, null).get() : this.userDBService.asyncGetUsers(verifyReq.getEmail(), verifyReq.getPhonenum(),
		            userRoles, oauthUser.getBankMerchantId()).get();
		      } else {
		        users = this.userDBService
		            .asyncGetUsers(verifyReq.getEmail(), verifyReq.getPhonenum(), userRoles, null).get();
		      }

		      if (CollectionUtils.isEmpty(users)) {
		        userVerifyResponse.addHeaderCode(HeaderCode.USER_DOESNT_EXIST);
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(userVerifyResponse);
		      } else {
		        user = users.get(0);
		      }
		    } else {
		      List<User> users = this.userDBService.asyncGetUsers(verifyReq.getEmail(),
		          verifyReq.getPhonenum(), Arrays.asList(UserRoles.CUSTOMER.value()), null).get();
		      if (CollectionUtils.isEmpty(users)) {
		        userVerifyResponse.addHeaderCode(HeaderCode.USER_DOESNT_EXIST);
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(userVerifyResponse);
		      } else {
		        user = users.get(0);
		      }
		    }

		    // is wallet user already existing
		    /*
		     * if(virtual) {
		     * 
		     * List<String> walletUserRoles = APIUtils.getWalletUserRoles(); //fetch users on basis or
		     * phone,email,role,bankmerchnatId List<User> existingWalletUsers =
		     * this.userDBService.getUsers(verifyReq.getEmail(), verifyReq.getPhonenum(),
		     * oauthUser.getBankMerchantId(), walletUserRoles);
		     * 
		     * if(CollectionUtils.isEmpty(existingWalletUsers)) { return
		     * RequestHandler.responseBuilder(userVerifyResponse).status(Status.
		     * INTERNAL_SERVER_ERROR).build(); }else { user = existingWalletUsers.get(0); }
		     * 
		     * }
		     */
		    if (user == null) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .message("Failed to obtain user- Email: " + verifyReq.getEmail() + ", Phone: "
		              + verifyReq.getPhonenum())
		          .format());
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		          .body(APIResponse.error(HeaderCode.USER_DOESNT_EXIST));
		    }
		    
		    // check if user is on hold after exceeding max login attempts
		    // if yes check his on_hold offset
		    Session session = this.userDBService.getUserSession(user.getId());
		    if (session != null && session.getNumOfRetries() >= DBConstants.MAX_LOGIN_ATTEMPT) {
		      Long onHoldTime = Long.valueOf(user.getStateTimestamp());
		      if ((now - onHoldTime) < DBConstants.ON_HOLD_TIMEOUT_OFFSET_MILLIS) {
		        LOG.debug(LogFormatter.instance().message("User will have to wait for")
		            .data("Next", DBConstants.OTP_TIMEOUT_OFFSET_MILLIS - (now - onHoldTime) + " MilliSec")
		            .format());
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(APIResponse.error(HeaderCode.HOLD_OFFSET_NOT_COMPLETED));
		      }
		    }

		    if ((verifyReq.getTnc() != null && verifyReq.getTnc() != BooleanStatus.YES.value())
		        || (verifyReq.getTnc() == null && user.getTnC() != BooleanStatus.YES.value())) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .message("TNC not accepted by User OR not provided in request").format());
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		          .body(APIResponse.error(HeaderCode.TNC_NOT_PROVIDED));
		    }

		    boolean isNewPin = false;
		    // pin will be provided only with OTP. so if ONLY email verification requested, skip this check
		    if (!verifyReq.emailOnlyValidation()) {

		      if (StringUtils.isEmpty(verifyReq.getTpin()) && StringUtils.isEmpty(verifyReq.getCardId())) {

		        if (StringUtils.isEmpty(verifyReq.getPin())) {
		          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .message("PIN not provided in request").format());
		          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		              .body(APIResponse.error(HeaderCode.USER_INVALID_PIN));
		        }
		      }

		      /*
		       * if tpin and cardId are provided in request fetch customerAccount and compare tpin or else
		       * compare ostapin
		       */
		      if (!StringUtils.isEmpty(verifyReq.getTpin())
		          && !StringUtils.isEmpty(verifyReq.getCardId())) {
		        // fetch user account of this bank by userId and cardId
		        CustomerAccount account = this.customerDBService.getAccount(user.getId(),
		            Integer.parseInt(verifyReq.getCardId()));

		        if (account == null) {
		          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		              .body(APIResponse.error(HeaderCode.BAD_REQUEST));
		        }

		        if (!userUtil.comparePassword(verifyReq.getTpin(), account.getTpin())) {
		          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .message("Pin does not match with bank tpin").format());
		          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		              .body(APIResponse.error(HeaderCode.USER_INVALID_PIN));
		        }

		      }
		      // if not first time verification, pin HAS to exist and cross check with existing pin
		      // if user is inactive i.e 1st time verification, accept pin as it is
		      if (UserStatus.INACTIVE.equals(user.getStatus()) && user.getCreatedBy() == 0) {
		        isNewPin = true;
		      }

		      // Removing this check as we are resetting the pin also

		      if (user.getPin() != null
		          && (UserStatus.ACTIVE.equals(user.getStatus()) || user.getCreatedBy() != 0)
		          && !userUtil.comparePassword(verifyReq.getPin(), user.getPin())) {
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		        		 .message("******Setting New Pin********").format());

		        isNewPin = true;
		        
				if (APIConstants.RequestSource.WEB.value().equals(verifyReq.getSource())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Incorrect PIN redirecting to OTP page")
							.data("Redirect URL", applicationProperties.getWebOtpUrl()).data("Source", verifyReq.getSource())
							.format());

					httpServletContext.getServletResponse().sendError(HttpStatus.BAD_REQUEST.value());

				}

		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(APIResponse.error(HeaderCode.USER_INVALID_PIN));

		      }
		    }

		    if (!isPartnerUser(partner, user)) {
		      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
		          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		    }

		    // in case both email and phone provided make sure they are both valid.
		    if ((!StringUtils.isEmpty(verifyReq.getEmail())
		        && !verifyReq.getEmail().equals(user.getEmail()))
		        || (!StringUtils.isEmpty(verifyReq.getPhonenum())
		            && !verifyReq.getPhonenum().equals(user.getPhone()))) {
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		          .body(APIResponse.error(HeaderCode.FAIL_TO_VALIDATE_EMAIL_PHONE));
		    }

		    // This conditions checks if elapsed timed after exceeding max otp attempt is less then for
		    // OTP_TIMEOUT_OFFSET_MILLIS
		    // if ONLY email verification requested, skip this check

		    if (!verifyReq.emailOnlyValidation()) {
		      if (user.getOTPAttempt() >= DBConstants.MAX_VERIFY_OTP_ATTEMPT) {
		        Long verificationTime = Long.valueOf(user.getOTPVerificationTime());
		        if ((now - verificationTime) < DBConstants.OTP_TIMEOUT_OFFSET_MILLIS) {
		          LOG.debug(LogFormatter.instance().message("User have to wait for")
		              .data("Next",
		                  DBConstants.OTP_TIMEOUT_OFFSET_MILLIS - (now - verificationTime) + " MilliSec")
		              .format());
		          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		              .body(APIResponse.error(HeaderCode.MAX_OTP_VERIFICATION_ATTEMPT));
		        }
		        user.setOTPAttempt(0);
		      }
		      user.setOTPAttempt(user.getOTPAttempt() + 1);
		      this.userDBService.updateUser(user);

		      // verify is OTP expired
		      if (!StringUtils.isEmpty(verifyReq.getPhonenum()) && !StringUtils.isEmpty(verifyReq.getOtp())
		          && (StringUtils.isEmpty(user.getOTPExpiryTime())
		              || Long.valueOf(user.getOTPExpiryTime()) < now)) {
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(APIResponse.error(HeaderCode.USER_OTP_EXPIRED));
		      }
		    }

		    Merchant merchant = null;
		    MerchantOnboard merchantOnboard = null;
		    Bank bank = null;
		    boolean superUser = false;
		    if (merchantRequest) {
		      superUser =
		          this.userDBService.isMerchantSuperAdmin(user) || this.userDBService.isMerchantAdmin(user);
		      merchant = this.merchantDBService.getMerchant(user.getBankMerchantId());
		      merchantOnboard =
		          this.merchantDBService.getMerchantOnboardByMerchantId(user.getBankMerchantId());

		    } else if (bankRequest) {
		      superUser = this.userDBService.isBankSuperAdmin(user) || this.userDBService.isBankAdmin(user);
		      bank = this.bankDBService.getBank(user.getBankMerchantId());
		    }

		    // only merchant/bank admins can verify account without merchant being
		    // activated.
		    if (merchant != null && !superUser && !this.userDBService.isActive(merchant)) {
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		          .body(APIResponse.error(HeaderCode.MERCHANT_NOT_ACTIVE));
		    } else if (bank != null && !superUser && !this.userDBService.isActive(bank)) {
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		          .body(APIResponse.error(HeaderCode.BANK_NOT_ACTIVE));
		    }

		    // verify and activate user + update password
		    boolean inactiveUser = (user.getStatus() == BooleanStatus.NO.value());
		    boolean phoneChange = (!verifyReq.emailOnlyValidation() && verifyReq.getPassword() == null
		        && user.getStatus() == UserStatus.ONHOLD.value());

		    boolean emailWasVerified =
		        (user.getIsEmailVerified() == BooleanStatus.YES.value()) ? true : false;
		    boolean phoneWasVerified =
		        (user.getIsOTPVerified() == BooleanStatus.YES.value()) ? true : false;
		    boolean userWasActivated = (user.getStatus() == BooleanStatus.YES.value()) ? true : false;
		    boolean passwordChange = !verifyReq.emailOnlyValidation() && !phoneChange && !(StringUtils.isEmpty(user.getPassword()))
		        &&  !userUtil.comparePassword(verifyReq.getPassword(), user.getPassword());
		    boolean password =
		        (StringUtils.isEmpty(user.getPassword())) ? StringUtils.isNotEmpty(verifyReq.getPassword())
		            : false;

		    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("remoteIp", remoteIp)
		        .data("emailWasVerified", emailWasVerified).data("phoneWasVerified", phoneWasVerified)
		        .data("phoneChange", phoneChange).data("userWasActivated", userWasActivated)
		        .maskedData("password change", Mask.PASSWORD_MASKED.value())
		        .data("emailHash", verifyReq.getEmailHash()).maskedData("pin", Mask.PIN_MASKED.value())
		        .data("password change" , passwordChange )
		        .data("password", password )
		        .format());
		     if(!passwordChange ) {
		    	   if(verifyReq.getPassword()!=null && user.getRole().equalsIgnoreCase(DBConstants.UserRoles.CUSTOMER.value())) {
		    	    	if (this.userDBService.verifyPassword(user,verifyReq.getPassword())) {
		    	            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		    	                    .message("New Password  same as Old Password").format());
		    	            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("passwordChange", passwordChange)
		       	           		 .data("verifyReq.getPassword()", verifyReq.getPassword())
		       	           				 .data("userPassword", user.getPassword()).format());
		    	            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		   	                    .body(APIResponse.error(HeaderCode.NEW_PASSWORD_SAME_AS_OLD_PASSWORD));
		    	    	}
		    	}
		     }
		  
		    
		     
		     
		    user = this.userDBService.activateResetCredentials(user, verifyReq.getEmailHash(),
		        verifyReq.getOtp(), remoteIp,
		        password ? verifyReq.getPassword() : (passwordChange ? verifyReq.getPassword() : null),
		        isNewPin ? verifyReq.getPin() : null, verifyReq.getTnc());

		    if (user != null) {

		      APIResponse response = new APIResponse();

		      if (passwordChange && verifyReq.getPassword() != null) {
		        response.addHeaderCode(HeaderCode.USER_PASSWORD_RESET);
		        // Metrics for forgot password
		        userMetricRegistry.userForgotPasswordCounter().increment();
		      }
		      if (!emailWasVerified && user.getIsEmailVerified() == BooleanStatus.YES.value()) {
		        response.addHeaderCode(HeaderCode.USER_EMAIL_VERIFIED);
		      }
		      if (!phoneWasVerified && user.getIsOTPVerified() == BooleanStatus.YES.value()) {
		        response.addHeaderCode(HeaderCode.USER_MOBILE_VERIFIED);
		      }
		      if (!userWasActivated && user.getStatus() == BooleanStatus.YES.value()) {
		        response.addHeaderCode(HeaderCode.USER_ACTIVATED);
		      }
		      /* Add data in Approval for lead */
		      if (superUser) {
		        if (merchantRequest) {
		          this.partnerApprovalHelper.submitPartnerApproval(user, merchant, merchantOnboard, null,
		              PartnerApprovalFields.LEAD);
		        } else if (bankRequest) {
		          this.partnerApprovalHelper.submitPartnerApproval(user, null, null, bank,
		              PartnerApprovalFields.LEAD);
		        }
		      }

		      if (!(virtual)) {

		        // send SMS
		        if (inactiveUser && user.getStatus() == BooleanStatus.YES.value()) {
		          if (!applicationProperties.getAwsSMSClient() && (user.getRole().equals(UserRoles.CUSTOMER.value())
		              && !smsClient.sendSms(user.getPhone(), Templates.CustomerRegistrationSuccess.format(),
		                  httpServletContext.getClientFeatureFlags().smsEnabled()))
		              || (user.getRole().equals(UserRoles.MERCHANT_SUPERADMIN.value())
		                  && !smsClient.sendSms(user.getPhone(),
		                      Templates.MerchantRegistrationSuccess.format(),
		                      httpServletContext.getClientFeatureFlags().smsEnabled()))) {
		            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		                .message("Failed to send SMS").data("phone", user.getPhone()).format());
		          }
		          
		          if (applicationProperties.getAwsSMSClient()) {

		            NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
		            notificationRequestContext.setTraceId(httpServletContext.getTraceId());
		            
		            if ((user.getRole().equals(UserRoles.CUSTOMER.value())
		                && !notificationResource.sendSms(user.getPhone(),
		                    Templates.CustomerRegistrationSuccess.format(),
		                    httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext))
		                || (user.getRole().equals(UserRoles.MERCHANT_SUPERADMIN.value())
		                    && !notificationResource.sendSms(user.getPhone(),
		                        Templates.MerchantRegistrationSuccess.format(),
		                        httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext))) {
		              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		                  .message("Failed to send SMS").data("phone", user.getPhone()).format());
		            }
		          }


		          if (user.getIsEmailVerified() == BooleanStatus.YES.value()
		              && ((user.getRole().equals(UserRoles.CUSTOMER.value())
		                  && !emailUtils.sendCustomerRegisterationSuccessfulEmail(user))
		                  || (user.getRole().equals(UserRoles.MERCHANT_SUPERADMIN.value())
		                      && !emailUtils.sendMerchantRegisterationSuccessfulEmail(user)))) {
		            LOG.error("Failed to send email to user " + user.getId());
		          }
		        }

		        else if (!verifyReq.emailOnlyValidation() && !phoneChange) {
		          if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(user.getPhone(),
		              Templates.PasswordChange.format(user.getUpdateDate()),
		              httpServletContext.getClientFeatureFlags().smsEnabled())) {
		            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		                .message("Failed to send SMS").data("phone", user.getPhone())
		                .data("template", Templates.PasswordChange.format(user.getUpdateDate())).format());
		          }
		          
		          if (applicationProperties.getAwsSMSClient()) {

		            NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
		            notificationRequestContext.setTraceId(httpServletContext.getTraceId());
		            if (!notificationResource.sendSms(user.getPhone(),
		                Templates.PasswordChange.format(user.getUpdateDate()),
		                httpServletContext.getClientFeatureFlags().smsEnabled(), notificationRequestContext)) {
		              
		              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		                  .message("Failed to send SMS").data("phone", user.getPhone())
		                  .data("template", Templates.PasswordChange.format(user.getUpdateDate())));

		            }
		          }

		          if (user.getIsEmailVerified() == BooleanStatus.YES.value()
		              && !emailUtils.sendPasswordChangeEmail(user, user)) {
		            LOG.error("Failed to send email to user " + user.getId());
		          }

		        }

		      }
		      return ResponseEntity.ok(response);
		    }

			if (!verifyReq.emailOnlyValidation()) {

				if (APIConstants.RequestSource.WEB.value().equals(verifyReq.getSource())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Incorrect OTP redirecting to OTP page")
							.data("Redirect URL", applicationProperties.getWebOtpUrl()).data("Source", verifyReq.getSource())
							.format());

					httpServletContext.getServletResponse().sendError(HttpStatus.BAD_REQUEST.value());
				}

				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.INCORRECT_OTP));

			}

		    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		        .body(APIResponse.error(HeaderCode.USER_FAIL_TO_VERIFY));

		  }
		  
		  /*
		   * Verify Source with User
		   */
		  boolean isPartnerUser(String partner, final User user) {
		    boolean merchantRequest = APIConstants.MERCHANT.equals(partner.toUpperCase());
		    boolean brontooRequest = APIConstants.BRONTOO.equals(partner.toUpperCase());
		    boolean bankRequest = APIConstants.BANK.equals(partner.toUpperCase());

		    if (merchantRequest) {
		      if (this.userDBService.merchantRepresentative(user))
		        return true;
		      else
		        return false;
		    } else if (bankRequest) {
		      if (this.userDBService.bankRepresentative(user))
		        return true;
		      else
		        return false;
		    } else if (brontooRequest) {
		      if (this.userDBService.brontooRepresentative(user))
		        return true;
		      else
		        return false;
		    } else if (this.userDBService.isCustomer(user)) {
		      return true;
		    }

		    return false;
		  }
}
