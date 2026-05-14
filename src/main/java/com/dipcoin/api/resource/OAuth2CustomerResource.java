package com.dipcoin.api.resource;

import static com.dipcoin.api.commons.OAuth2Utils.apiResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import com.dipcoin.amqp.RabbitMqConfiguration;
import com.dipcoin.api.async.AsyncRequest;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.OAuth2Token;
import com.dipcoin.api.commons.OAuth2TokenGenerator;
import com.dipcoin.api.commons.OAuth2Utils;
import com.dipcoin.api.commons.OAuth2Utils.PartnerType;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.BankCallbackRequest;
import com.dipcoin.api.model.BankSourceValidator;
import com.dipcoin.api.model.CustomerAccountRequest;
import com.dipcoin.api.model.CustomerAccountResponse;
import com.dipcoin.api.model.LoginSessionResponse;
import com.dipcoin.api.model.PartnerRegisterCustomerRequest;
import com.dipcoin.api.model.PartnerRegisterCustomerRequest.AccountDetails;
import com.dipcoin.api.model.PartnerRegisterCustomerRequest.UserProfile;
import com.dipcoin.api.model.UserInfoResponse;
import com.dipcoin.api.model.UserRegisterRequest;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.client.BankClient.Operation;
import com.dipcoin.bank.services.comm.BankUIDRequest;
import com.dipcoin.bank.services.comm.BankUIDResponse;
import com.dipcoin.bank.services.comm.TpinValidationRequest;
import com.dipcoin.bank.services.comm.TpinValidationResponse;
import com.dipcoin.bank.services.utils.BankConstants.BankResponseStatus;
import com.dipcoin.bank.services.utils.BankProperties;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankUtils;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.LogFormatter.Mask;
import com.dipcoin.commons.UserUtil;
import com.dipcoin.core.CryptoUtil;
import com.dipcoin.core.CryptoUtil.AlgoScheme;
import com.dipcoin.core.CryptoUtil.SupportedEncoding;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.PartnerDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.AppInfoUserOs;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.commons.DBConstants.WrapperSdkRedirectTo;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.PartnerCredential;
import com.dipcoin.db.services.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("oAuth2CustomerResource")
public class OAuth2CustomerResource {

  private static final Logger LOG = LogManager.getLogger(OAuth2CustomerResource.class);
  private static final ObjectMapper objectMappper = new ObjectMapper();
  private static final int tpinLength = 6;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private UserLoginResource userLoginResource;

  @Autowired
  private UserUtil userUtil;

  @Autowired
  private CustomerResource customerResource;

  @Autowired
  private CryptoUtil cryptoUtil;

  @Autowired
  private ApplicationProperties applicationProperties;

  @Autowired
  private PartnerDBService partnerDBService;

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  private PartnerEncDecResource partnerEncDecResource;

  @Autowired
  private MerchantDBService merchantDBService;

  @Autowired
  private DipcoinDBService dipcoinDBService;

  @Autowired
  private CustomerDBService customerDBService;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

  @Autowired
  private BankAPIServices bankAPIServices;
  
  @Autowired
  private DipcoinResource dipcoinResource;
  
  @Autowired
  private BankUtils bankUtils;
  
  @Autowired
  RabbitTemplate asyncRequestAmqpTemplate;
  
  @Autowired
  private Environment environment;


  public ResponseEntity registerPartnerUser(UserRegisterRequest createReq, String customer,
      String clientTransactionId, Bank bank,Merchant merchant, boolean authenticateSourceFlag,boolean isSetPin)
      throws Exception, APIException {
    UserInfoResponse response = new UserInfoResponse();
    String randomSalt = userUtil.getSecureSalt();
    
    
    
    
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("authenticateSourceFlag "+ createReq.toString()).data("authenticateSourceFlag", authenticateSourceFlag)
        .data("Bank", bank)
        .data("createReq", createReq)
        .format());
    
    
    if (bank == null) {
      if(merchant == null) {
        response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }
    }
    if (createReq == null) {
      response.addHeaderCode(HeaderCode.INVALID_REQUEST);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    if (authenticateSourceFlag) {

      BankSourceValidator sourceData = new BankSourceValidator();
      sourceData =
          new ObjectMapper().readValue(bank.getSourceIdentifier(), BankSourceValidator.class);
      if (sourceData.getAndroidBunddleId() == null && sourceData.getIosBunddleId() == null
          && sourceData.getUrl() == null) {

        response.addHeaderCode(HeaderCode.MISSING_INVALID_INFO);
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);

      }

      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("bank Customer register request").data("Request", createReq.toString())
          .format());
      if (createReq.getSource() == null) {

        LOG.debug(
            LogFormatter.instance(httpServletContext.getTraceId()).message("null source").format());
        response.addHeaderCode(HeaderCode.INVALID_SOURCE);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

      }
		if (!(APIConstants.RequestSource.ANDROID.value().equals(createReq.getSource())
				|| APIConstants.RequestSource.WEB.value().equals(createReq.getSource())
				|| APIConstants.RequestSource.IOS.value().equals(createReq.getSource())
				|| APIConstants.RequestSource.WEB2.value().equals(createReq.getSource()))) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid Source")
					.data("reason", response).format());
			response.addHeaderCode(HeaderCode.INVALID_SOURCE);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

		}

      if (APIConstants.RequestSource.IOS.value().equals(createReq.getSource())) {
        if (!createReq.getBundleId().equals(sourceData.getIosBunddleId())) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Invalid Ios BunddleID").data("reason", response).format());
          response.addHeaderCode(HeaderCode.BANK_UNAUTHORIZED);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

      }

      if (APIConstants.RequestSource.ANDROID.value().equals(createReq.getSource())) {
        if (!createReq.getBundleId().equals(sourceData.getAndroidBunddleId())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Invalid Android BunddleID").data("reason", response).format());
          response.addHeaderCode(HeaderCode.BANK_UNAUTHORIZED);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

      }
      
      if (APIConstants.RequestSource.WEB.value().equals(createReq.getSource()) ||
    		  APIConstants.RequestSource.WEB2.value().equals(createReq.getSource())) {

        if (!bank.getUrl().equals(createReq.getOrigin())) {

          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Invalid Web origin").data("reason", response).format());
          response.addHeaderCode(HeaderCode.BANK_UNAUTHORIZED);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

      }
    }
    // invalid request
    if (!createReq.validate(httpServletContext)) {

      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Invalid Request")
          .data("reason", createReq.getErrorCodes()).format());
      response.addHeaderCode(createReq.getErrorCodes().get(0));
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

    }

    List<User> existingUsers = null;
    existingUsers = this.userDBService.getUsers(createReq.getPhonenum(), 0,
        Arrays.asList(UserRoles.CUSTOMER.value()), DBConstants.UserStatus.ACTIVE.value());
    if (existingUsers != null) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("User is already exists").format());  
      if (authenticateSourceFlag && createReq.getAccountInfo() != null) {
        
        // call bank to validate tpin
    	  
    	// skip tpin validation for bank whose SSO is enabled
		if (createReq.getAccountInfo() != null
				&& BooleanStatus.YES.value() == createReq.getAccountInfo().getIsTpinEnabled()
				&& bank.getIsSsoEnabled() == BooleanStatus.NO.value()) {

          Boolean validTpin = validateTpin(createReq, bank);

          if (!validTpin) {

            response.addHeaderCode(HeaderCode.INCORRECT_TPIN);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
          }
        }
        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("check if user account exists or not").format());
        return verifyUserAccountdetails(createReq, response, existingUsers.get(0), bank, clientTransactionId);
      }
      
      
      response.addHeaderCode(HeaderCode.USER_EXISTS);
      return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
    // call bank to validate tpin
    // skip tpin validation for bank whose SSO is enabled
    if (createReq.getAccountInfo() != null
        && BooleanStatus.YES.value() == createReq.getAccountInfo().getIsTpinEnabled()
        && bank.getIsSsoEnabled() == BooleanStatus.NO.value()) {

      Boolean validTpin = validateTpin(createReq, bank);

      if (!validTpin) {

        response.addHeaderCode(HeaderCode.INCORRECT_TPIN);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }
    } else {
    	 LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
    	          .message("BANK is SSO Enabled so skipping Tpin Validation").format());
    }


    ResponseEntity userInfoResponse = userLoginResource.registerUser(createReq,
        APIConstants.CUSTOMER, clientTransactionId, null, merchant, Boolean.FALSE);
    if (userInfoResponse.getStatusCodeValue() != Status.CREATED.getStatusCode()) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("User is not able to register").data("response ", userInfoResponse).format());
      return ResponseEntity.status(userInfoResponse.getStatusCode()).body(userInfoResponse);
    }
    
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("userInfoResponse").data("userInfoResponse ", userInfoResponse).format());

    // Get the added user
    List<User> newlyaddedUsers =
        userLoginResource.getUser(createReq.getPhonenum(), DBConstants.UserStatus.INACTIVE.value(), merchant != null ? merchant.getId(): 0);
    
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("newlyaddedUsers").data("newlyaddedUsers ", newlyaddedUsers).format());

    // Not able to Find the Customer
    if (CollectionUtils.isEmpty(newlyaddedUsers)) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("New added user not found...").data("response ", userInfoResponse).format());
      response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    //String saltedPin = saltedPin = userUtil.getSecurePassword(createReq.getPin(), randomSalt);

    String saltedPin = "";
    
    // we'll not set any PIN if Bank's isSSOEnabled is enabled
    if(createReq.getPin() != null && !StringUtils.isEmpty(saltedPin) && bank.getIsSsoEnabled() == BooleanStatus.NO.value()) {
    	saltedPin = userUtil.getSecurePassword(createReq.getPin(), randomSalt);
    }
    
    //even though bank is sso enabled we are setting osta pin in user table.
    //Because fastag customer have to use customer login.
    //Customer can do forgot pin and set the pin.
    if(isSetPin) {
      saltedPin = userUtil.getSecurePassword(createReq.getPin(), randomSalt);
    }
   
    
    String saltedPassword = userUtil.getSecurePassword(createReq.getPassword(), randomSalt);

    User bankCustomer = newlyaddedUsers.get(0);
    bankCustomer.setEmail(bankCustomer.getEmail());
    bankCustomer.setIsOTPVerified(DBConstants.BooleanStatus.YES.value());
    
    if(!bankCustomer.getEmail().contains("notpresent@co.in"))
    bankCustomer.setIsEmailVerified(DBConstants.BooleanStatus.YES.value());
    bankCustomer.setStatus(DBConstants.UserStatus.ACTIVE.value());

    if (authenticateSourceFlag && createReq.getAccountInfo() == null) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("failed add user account as account info is missing")
          .data("response ", userInfoResponse).format());
      response.addHeaderCode(HeaderCode.BAD_REQUEST);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    if (authenticateSourceFlag && createReq.getAccountInfo() != null
        && createReq.getAccountInfo().getIsTpinEnabled() == BooleanStatus.YES.value()&& bank.getIsSsoEnabled() == BooleanStatus.NO.value()) {
      CustomerAccountRequest accountInfo = createReq.getAccountInfo();
      accountInfo.setTPin(saltedPin);
      accountInfo.setTpinSalt(randomSalt);
      createReq.setAccountInfo(accountInfo);
      bankCustomer.setPin(null);
    } else {
      bankCustomer.setPin(saltedPin);
    }
    bankCustomer.setPassword(saltedPassword);


    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("User data is set to verify and response")
        .data(" bankCustomer Id ", bankCustomer.getId()).data("Email", bankCustomer.getEmail())
        .maskedData(" pin", Mask.PASSWORD_MASKED.value())
        .maskedData("password", Mask.PASSWORD_MASKED.value()).format());


    User updatedUser = userLoginResource.updateUser(bankCustomer);

    // Not able to Find the Customer
    if (updatedUser == null) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Fail to verify user.").data("response ", userInfoResponse).format());
      response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    if (!authenticateSourceFlag) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("user is added as well as verified").data("response ", userInfoResponse)
          .format());

      return ResponseEntity.status(HttpStatus.OK).body(userInfoResponse);

    }
    
    ResponseEntity addedCustomerAccount =  customerResource.customerAddAccount(updatedUser, createReq.getAccountInfo(),
        clientTransactionId, true, false, bank, createReq.getSource());
    
    if(addedCustomerAccount.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
      
      return addedCustomerAccount;
    }

    // as of now, we have decided not to call BANK CALL BACK API for SSO ENABLED Banks
		return updateBankUid(bank, createReq.getSource(), addedCustomerAccount, updatedUser, createReq.getAccountInfo(),
				(CustomerAccountResponse) addedCustomerAccount.getBody(), bank.getIsSsoEnabled() == BooleanStatus.YES.value() ? true : false);
	
  }
  
public ResponseEntity updateBankUid( Bank bank, String source, ResponseEntity addedCustomerAccount, User user, CustomerAccountRequest addReq, CustomerAccountResponse addedCustomerAccount1, boolean byPassCallbackBankUrl) throws Exception {
    
    CustomerAccountResponse response = new CustomerAccountResponse();
    
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .data("user.getId()", user.getId())
        .data("addedCustomerAccount1.getCardId()", addedCustomerAccount1.getCardId())
        .format());
    
    CustomerAccount account =
        dipcoinResource.getAccount(user.getId(), addedCustomerAccount1.getCardId());
    if (account == null) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("acc not found").format());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
    }

    BankProperties bankProperties =
        bankAPIServices.getBankProperties(account.getBank().getReferenceId());

    if (bankProperties.fetchBankUID()) {
      String dipcoinReferenceNumber =
          bankUtils.generateDipcoinToBankReferenceNumber(account.getBank().getReferenceId(),
              Operation.BANK_UID, BankTransactionType.FETCH_BANK_UID.value());
      String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());

      BankUIDRequest bankUIDRequest =
          new BankUIDRequest(account.getBank().getReferenceId(), account.getBank().getCode());
      bankUIDRequest.setAccountNumber(addReq.getAccountNumber());
      bankUIDRequest.setTransactionType(String.valueOf(BankTransactionType.FETCH_BANK_UID.value()));
      bankUIDRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
      bankUIDRequest.setTransactionTime(requestTime);
      if (addReq.getAccountFlag() != null)
        bankUIDRequest.setAccountFlag(String.valueOf(addReq.getAccountFlag()));
      else
        bankUIDRequest.setAccountFlag(String.valueOf(BooleanStatus.NO.value()));

      BankRequestContext bankRequestContext = new BankRequestContext();
      bankRequestContext.setTraceId(httpServletContext.getTraceId());

      BankUIDResponse bResponse =
          bankAPIServices.getBankUID(bankRequestContext, bankUIDRequest).get();


      BankTransaction bTx = new BankTransaction();
      bTx.setType(BankTransactionType.FETCH_BANK_UID.value());
      bTx.setCustomerAccountId(account.getId());
      bTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
      bTx.setBankId(bank.getId());

      if (bResponse == null
          || !BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode())
          || !dipcoinReferenceNumber.equals(bResponse.getDipcoinReferenceNumber())) {
        LOG.debug("Failed to verify user account with bank");
        bTx.setStatus(BankTransactionsStatus.FAILED.value());
        if (bResponse != null) {
          bTx.setRequestTime(bResponse.getRequestTime());
          bTx.setResponseTime(bResponse.getResponseTime());
          bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
          bTx.setRawBankResponse(
              bResponse.getBankResponseDesc() != null ? bResponse.getBankResponseDesc()
                  : bResponse.getErrorMsg());
          bTx.setBankResponseCode(bResponse.getBankResponseCode());
          bTx.setRawBankRequest(objectMappper.writeValueAsString(bankUIDRequest));
        }
        if (this.bankDBService.asyncAddTransaction(bTx).get() == null) {
          LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Failed to add bank transaction").data("transaction", bTx).format());
        }
        account.setBankUId(null);
        account.setStatus(CustomerAccountStatus.UID_NOT_GENERATED.value());

        if (customerDBService.updateAccount(account) == null) {
          throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
              APIResponse.error(HeaderCode.INTERNAL_ERROR));
        }

        if (bResponse == null) {
          throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
              APIResponse.error(HeaderCode.INTERNAL_ERROR));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(APIResponse.error(HeaderCode.FAILED_TO_UPDATE_BANKUID));
      }

      bTx.setRawBankRequest(bResponse.getRawRequest());
      bTx.setStatus(BankTransactionsStatus.SUCCESS.value());
      bTx.setRawBankResponse(bResponse.getRawData());
      bTx.setBankResponseCode(bResponse.getBankResponseCode());
      bTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
      bTx.setRequestTime(bResponse.getRequestTime());
      bTx.setResponseTime(bResponse.getResponseTime());
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Bank Services fetch bankUID").data("requestTime", bResponse.getRequestTime())
          .data("responseTime", bResponse.getResponseTime()).format());

      if (this.bankDBService.asyncAddTransaction(bTx).get() == null)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));

      account.setStatus(CustomerAccountStatus.ACTIVE.value());
      account.setBankUId(bResponse.getBankUID());

      if (customerDBService.updateAccount(account) == null) {
        throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
            APIResponse.error(HeaderCode.INTERNAL_ERROR));
      }
	} else {
		if (StringUtils.isEmpty(addReq.getAccountNumber())) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("addReq.getAccountNumber() is empty").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.BAD_REQUEST));

		}
		account.setBankUId(addReq.getAccountNumber());
		account.setStatus(CustomerAccountStatus.ACTIVE.value());
		if (customerDBService.updateAccount(account) == null) {
			throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, APIResponse.error(HeaderCode.INTERNAL_ERROR));
		}
	}

    if (!byPassCallbackBankUrl) {
      
      this.bankCallback(user, addReq.getAccountNumber(),
          DBConstants.BankRequestType.USER_REGISTERATION.value(), HeaderCode.TRANSACTION_SUCCESSFUL.code(),
          bank, source, addReq.getBankCifNo(),
          Integer.toString(BooleanStatus.NO.value()), httpServletContext.getTraceId());
      
    }
    response.addHeaderCode(HeaderCode.BANK_ACCOUNT_ADDED);
    return ResponseEntity.status(addedCustomerAccount.getStatusCode()).body(response);
    
  }

public ResponseEntity verifyUserAccountdetails(UserRegisterRequest addReq,
	      UserInfoResponse response, User user, Bank bank,
	      String clientTransactionId) throws Exception {

	    List<CustomerAccount> existingAccounts = this.customerDBService.asyncGetAccounts(user.getId(),
	        Arrays.asList(DBConstants.CustomerAccountStatus.ACTIVE.value())).get();

	    if (StringUtils.isEmpty(addReq.getAccountInfo().getEncryptedLogin())
	        || StringUtils.isEmpty(addReq.getAccountInfo().getBankReferenceId())) {
	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	          .message("encryptedLogin is empty or bankReferenceId is empty ").format());
	      response.addHeaderCode(HeaderCode.BAD_REQUEST);
	      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

	    }
	    // generate hash of accountno comming in the request
	    String hashedLogin = Utils.getHashedLogin(StringUtils.stripStart(addReq.getAccountInfo().getEncryptedLogin(),"0"));
	    String randomSalt = userUtil.getSecureSalt();
	    
	   // String saltedPin = userUtil.getSecurePassword(addReq.getPin(), randomSalt);
	    
	    String saltedPin = "";
	    
	    if(addReq.getPin() != null && !StringUtils.isEmpty(saltedPin) && bank.getIsSsoEnabled() == BooleanStatus.NO.value()) {
	    	saltedPin = userUtil.getSecurePassword(addReq.getPin(), randomSalt);
	    }
	    
	    String saltedPassword = userUtil.getSecurePassword(addReq.getPassword(), randomSalt);
	    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	        .message("Get Existing CustomerAccout").data("User", user.getId())
	        .data("HashedLogin", hashedLogin).format());
	    // verify if account already added
	    for (CustomerAccount account : existingAccounts) {
	      // hashed login match
	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	          .message("Get Existing CustomerAccout").data("User", user.getId())
	          .data("account.getHashedLogin()", account.getHashedLogin()).format());

	      if ((StringUtils.isNotEmpty(account.getHashedLogin()) && account.getHashedLogin().equals(hashedLogin) 
	          || (StringUtils.isNotEmpty(account.getHashedAccountNumber()) && 
	              account.getHashedAccountNumber().equals(hashedLogin)))
	          && account.getStatus() != CustomerAccountStatus.DELETED.value()) {

	        // if bank not initialized, then initialize
	        if (!Hibernate.isInitialized(account.getBank()))
	          Hibernate.initialize(account.getBank());


	        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	            .message("account.getBank().getReferenceId() " + account.getBank().getReferenceId())
	            .message("addReq.getAccountInfo().getBankReferenceId() "
	                + addReq.getAccountInfo().getBankReferenceId())
	            .format());
	        // if hashed login and bank match, return error
	        if (account.getBank().getReferenceId()
	            .equals(addReq.getAccountInfo().getBankReferenceId())) {

	          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	              .message(" user account is already registered").format());

	          if (addReq.getAccountInfo().getIsTpinEnabled() == BooleanStatus.NO.value()
	        		  && (StringUtils.isBlank(user.getPin()) || userUtil.comparePassword("null", user.getPin()))) {

	              user.setPin(saltedPin);
	              user.setPinSalt(randomSalt);
	              user.setPassword(saltedPassword);
	              userLoginResource.updateUser(user);

	              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	                  .message(
	                      "user account is registered and osta pin is updated in user table")
	                  .format());
	            
	          }
	          if ((APIConstants.RequestSource.ANDROID.value().equals(addReq.getSource())
	              || APIConstants.RequestSource.IOS.value().equals(addReq.getSource()))
	              && addReq.getAccountInfo().getIsTpinEnabled() == BooleanStatus.YES.value()) {
	            // account
	            account.setTpinEnable(BooleanStatus.YES.value());
	            account.setTpinSalt(randomSalt);
	            account.setTpinLength(addReq.getAccountInfo().getTPinLength());
	            account.setTpin(saltedPin);
	            customerDBService.updateAccount(account);
	            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	                .message("user account is registered and TPIN  is updated in Customer Account Table table")
	                .format());

	          }
	          
	          this.bankCallback(user, addReq.getAccountInfo().getAccountNumber(),
	              DBConstants.BankRequestType.USER_REGISTERATION.value(), HeaderCode.TRANSACTION_SUCCESSFUL.code(),
	              bank, addReq.getSource(), addReq.getAccountInfo().getBankCifNo(),
	              Integer.toString(BooleanStatus.NO.value()), httpServletContext.getTraceId());
	          response.addHeaderCode(HeaderCode.BANK_ACCOUNT_ADDED);
	          return ResponseEntity.status(HttpStatus.OK).body(response);
	        }
	      }
	    }
	    
	    
	    if (addReq.getAccountInfo().getIsTpinEnabled() == BooleanStatus.YES.value()) {
	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	          .message(" user account is not registered and tpin is enabled so update customer account")
	          .format());

	      CustomerAccountRequest accountInfo = addReq.getAccountInfo();
	      accountInfo.setTPin(saltedPin);
	      accountInfo.setTpinSalt(randomSalt);
	      addReq.setAccountInfo(accountInfo);
	    }

	    if ( addReq.getAccountInfo().getIsTpinEnabled() == BooleanStatus.NO.value()
	    		&& (StringUtils.isBlank(user.getPin()) || userUtil.comparePassword("null", user.getPin()))) {
	        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	            .message("user account is not registered and tpin is not enabled so update user table")
	            .format());
	        //FOR FASTAG INB Customer
	        user.setPin(saltedPin);
	        user.setPinSalt(randomSalt);
	        user.setPassword(saltedPassword);
	        userLoginResource.updateUser(user);

	      
	    }
	    ResponseEntity addedCustomerAccount = customerResource.customerAddAccount(user, addReq.getAccountInfo(), clientTransactionId,
	        true, false, bank, addReq.getSource());
	    
	    
	    
	    CustomerAccountResponse customerAccount =
	            (CustomerAccountResponse) addedCustomerAccount.getBody();
	    
	    LOG.debug(
	            LogFormatter.instance(httpServletContext.getTraceId()).data("customerAccount", customerAccount).format());
	    
	    if (customerAccount.getCodes().get(0).getCode() != null
	        && customerAccount.getCodes().get(0).getCode().equals("C-030")) {

	      customerAccount.removeHeaderCodes();

	      customerAccount.addHeaderCode(HeaderCode.BANK_ACCOUNT_ADDED);

	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	          .data("customerAccount", customerAccount).format());
	      return addedCustomerAccount;
	    }
	    
	    if(addedCustomerAccount.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
	    	LOG.debug(
	                LogFormatter.instance(httpServletContext.getTraceId()).data("customerAccount", customerAccount).format());
	      
	      return addedCustomerAccount;
	    }
	    LOG.debug(
	            LogFormatter.instance(httpServletContext.getTraceId()).data("customerAccount", customerAccount).format());
	    
	    // as of now, we have decided not to call BANK CALL BACK API for SSO ENABLED Banks
	    return updateBankUid(bank, addReq.getSource(), addedCustomerAccount, user, addReq.getAccountInfo(),
	    		(CustomerAccountResponse)addedCustomerAccount.getBody(), bank.getIsSsoEnabled() == BooleanStatus.YES.value() ? true : false);

	  }
	  

public boolean validateTpin(UserRegisterRequest createReq, Bank bank) throws APIException {
    
    // decrypt tpin
    if (StringUtils.isEmpty(createReq.getAccountInfo().getBankCifNo())
        || StringUtils.isEmpty(createReq.getAccountInfo().getEncryptedPin())) {
      return false;
      /*
       * response.addHeaderCode(HeaderCode.BAD_REQUEST); return
       * ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       */
    }
    String decTpin = decryptTpin(createReq.getAccountInfo().getEncryptedPin(),
        createReq.getAccountInfo().getBankCifNo(),createReq.getWrapper());
    if (StringUtils.isEmpty(decTpin)) {

      LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
          .message("tpin decryption failed").format());
      
      return false;
      /*
       * response.addHeaderCode(HeaderCode.INCORRECT_TPIN); return
       * ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       */
    }
    Boolean validTpin;
    try {
      validTpin = this.tpinValidation(decTpin, createReq.getAccountInfo().getBankCifNo(), bank);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
          APIResponse.error(HeaderCode.TRANSACTION_FAILURE));
    }

    if (!validTpin) {
      LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
          .message("invalid tpin add req in dipcoin txn").format());
      DipcoinTransaction dipcoinTransaction = new DipcoinTransaction();
      // Generating dipcoinReference number i.e ostaTxnRefId for this transaction.
      String dipcoinReferenceNumber = CoreUtils.generateDipcoinToMerchantReferenceNumber();
      dipcoinTransaction.setDipcoinTransactionRefId(dipcoinReferenceNumber);
      dipcoinTransaction.setType(DipcoinTransactionType.BANKCUSTOMER_REGISTER.value());
      dipcoinTransaction
          .setResponseTime(String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis()));
      dipcoinTransaction.setPartnerRawRequest(createReq.toString());
      dipcoinTransaction.setStatus(DipcoinTransactionsStatus.FAILURE.value());
      dipcoinTransaction.setAmount(APIConstants.zeroAmount);
      dipcoinTransaction.setSource(TransactionSource.OAUTH.value());
      dipcoinTransaction.setPartnerReferenceId(bank.getReferenceId());
      // Add transaction in db
      if (this.dipcoinDBService.addTransaction(dipcoinTransaction) == null) {
        LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Failed to add details in Dipcoin Transaction Table.").format());
        return false;
        /*
         * throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
         * APIResponse.error(HeaderCode.TRANSACTION_FAILURE));
         */
      }
      
      return false;

      /*
       * response.addHeaderCode(HeaderCode.INCORRECT_TPIN); return
       * ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
       */

    }
    
    
    return true;
  }

public boolean tpinValidation(String tpin, String cifnumber, Bank bank)
	      throws Exception, APIException {
	    
	    boolean prodEnvironment = false;
	    boolean devEnvironment = false;
	    if (environment != null) {
	      Set<String> profiles = new HashSet<>(Arrays.asList(
	          ArrayUtils.isNotEmpty(environment.getActiveProfiles()) ? environment.getActiveProfiles()
	              : environment.getDefaultProfiles()));

	      if (CollectionUtils.isEmpty(profiles)) {
	        devEnvironment = true;
	      } else {
	        if (profiles.contains("prod") || profiles.contains("uat")) {
	          prodEnvironment = true;
	        }
	      }
	    }
	    
	    
	    if (!prodEnvironment) {
	      return true;
	    }

	    TpinValidationRequest request =
	        new TpinValidationRequest(bank.getReferenceId(), bank.getCode());
	    request.setTpin(tpin);
	    request.setDipcoinReferenceNumber(CoreUtils.generateDipcoinToBankReferenceNumber());
	    request.setCifNumber(cifnumber);
	    request.setTransactionType(String.valueOf(BankTransactionType.OTP_SUCCESS.value()));

	    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	        .data("BankRequest_DipcoinReferenceNumber", request.getDipcoinReferenceNumber())
	        .data("BankRequest_BankCode",request.getBankCode() )
	        .data("BankRequest_TransactionType",request.getTransactionType())
	        .format());
	    BankRequestContext bankRequestContext = new BankRequestContext();
	    bankRequestContext.setTraceId(httpServletContext.getTraceId());
	    Future<TpinValidationResponse> bResponseTask =
	        this.bankAPIServices.tpinValidationRequest(bankRequestContext, request);

	    // @TODO - set response time from bResponse
	    TpinValidationResponse bResponse = bResponseTask.get();
	    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("BankResponse", bResponse)
	        .format());


	    // system failure
	    if (bResponse == null
	        || !BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode())) {

	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	          .message("Failure in bank callBack response").format());
	      return false;

	    }

	    return true;
	  }

public static String decryptTpin(String encTpin, String cifNo, Boolean wrapper) {

	byte[] decryptedValue;
	try {

		if (!wrapper) {
			byte[] skey = Base64.encodeBase64(cifNo.getBytes());
			SecretKeySpec skeySpec = new SecretKeySpec(skey, "AES");
			IvParameterSpec ivParameterSpec = new IvParameterSpec(skey);
			Cipher cipher;
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);
			decryptedValue = cipher.doFinal(Base64.decodeBase64(encTpin));
		} else {
			if(cifNo.length() >16) {
	    		cifNo = cifNo.substring(0, 16);	
	    	}
	    	
	    	while(cifNo.length()< 16) {
	    		cifNo = cifNo.concat("0");
	    		System.out.println(cifNo);
	    	}
			
			SecretKeySpec skeySpec = new SecretKeySpec(cifNo.getBytes("utf-8"), "AES");
		    IvParameterSpec ivParameterSpec = new IvParameterSpec(cifNo.getBytes("utf-8"));
			Cipher cipher;
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);
			decryptedValue = cipher.doFinal(Base64.decodeBase64(encTpin));
		}
		
		LOG.debug(LogFormatter.instance()
	            .data("decryptedValue", new String(decryptedValue, StandardCharsets.UTF_8)).format());

	} catch (Exception e) {

		LOG.debug(LogFormatter.instance().message("Failed to decrypt tpin").format());
		return null;
	}

	return new String(decryptedValue, StandardCharsets.UTF_8);
}

public void bankCallback(User user, String accountNumber, String requestType, String code,
	      Bank bank, String source, String bankCif, String tPIN, final String traceId)
	      throws JsonProcessingException {

	   BankCallbackRequest bankCallbackRequest = new BankCallbackRequest();
	   if(!StringUtils.isEmpty(bankCif))
	   bankCallbackRequest.setCif(bankCif);
	   if(!StringUtils.isEmpty(tPIN))
	   bankCallbackRequest.setTPIN(tPIN);
	   bankCallbackRequest.setAccountId(accountNumber);
	   bankCallbackRequest.setBankReferenceId(bank.getReferenceId());
	   bankCallbackRequest.setCode(code);
	   bankCallbackRequest.setRequestType(requestType);
	   bankCallbackRequest.setSource(source);
	   
	   AsyncRequest<BankCallbackRequest> aReq = new AsyncRequest<>();
	   aReq.setRawRequest(bankCallbackRequest);
	   aReq.setUserId(user.getId());
	   aReq.setTraceId(httpServletContext.getTraceId());
	         
	   asyncRequestAmqpTemplate.convertAndSend(RabbitMqConfiguration.ASYNC_CALLBACK_REQUEST_QUEUE, aReq);

	  }
	  
  
}
