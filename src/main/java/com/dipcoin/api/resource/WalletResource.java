package com.dipcoin.api.resource;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIConstants.TransactionRequestType;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.resource.EncryptionResource;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.AddMoneyToWalletRequest;
import com.dipcoin.api.model.CreateUserWalletRequest;
import com.dipcoin.api.model.CustomerAccountRequest;
import com.dipcoin.api.model.CustomerAccountResponse;
import com.dipcoin.api.model.CustomerAccountVerifyRequest;
import com.dipcoin.api.model.CustomerDipcoinRequest;
import com.dipcoin.api.model.CustomerDipcoinResponse;
import com.dipcoin.api.model.PaymentTopupWalletResponse;
import com.dipcoin.api.model.UserRegisterRequest;
import com.dipcoin.api.model.UserVerifyRequest;
import com.dipcoin.api.model.WalletUserInfo;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.comm.AddMoneyInVirtualAccountRequest;
import com.dipcoin.bank.services.comm.AddMoneyInVirtualAccountResponse;
import com.dipcoin.bank.services.comm.CreateVirtualAccountRequest;
import com.dipcoin.bank.services.comm.CreateVirtualAccountResponse;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankUtils;
import com.dipcoin.bank.services.utils.VirtualBankProperties;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BankAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.BankStatus;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankType;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountMethodType;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.IsSettlement;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
import com.dipcoin.metrics.WalletMetricRegistry;

@Component("WalletResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class WalletResource {

	private final static SecureRandom randomGenerator = new SecureRandom();

	@Autowired
	private BankDBService bankDBService;
	
	@Autowired
	private EncryptionResource encryptionResource;

	@Autowired
	private UserDBService userDBService;
	
	@Autowired
	private UserLoginResource userLoginResource;

	@Autowired
	private CustomerDBService customerDBService;
	
	@Autowired
	private CustomerResource customerResource;

	@Autowired
	private BankAPIServices bankAPIServices;

	@Autowired
	private BankUtils bankUtils;

	@Autowired
	private DipcoinDBService coinDBService;

	@Autowired
	private CustomerDipcoinResource customerDipcoinResource;
	
	@Autowired
	private WalletMetricRegistry walletMetricRegistry;
	
	@Autowired
	private VirtualBankProperties virtualBankProperties;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;
	
	public static final String OTP = "789987";
	public static final String PASSWORD = "P@ssword123";

	private static final Logger LOG = LogManager.getLogger(WalletResource.class);

	  public ResponseEntity topupWallet(AddMoneyToWalletRequest topupReq, User oauthUser,
		      Merchant oauthMerchant, TransactionSource source) throws APIException, Exception {
			  
			  System.out.println("topupWallet request received for phone num: " + topupReq.getRequestType());
		    BigDecimal topupReqAmount = roundOfBigDecimalAmount(topupReq.getAmount());
		    PaymentTopupWalletResponse paymentTopupWalletResponse = new PaymentTopupWalletResponse();

		    // * fetch users on basis or phone,email,role,bankmerchnatId
		    User user;
		    if (topupReq.getRequestType().equals(TransactionRequestType.WALLET_TOP_UP.value())) {
		      user =
		          getUserDetails(topupReq.getPhonenum(), oauthMerchant.getId(), topupReq.getRequestType());
		    } else {
		      user = getUserDetails(topupReq.getPhonenum(), 0, topupReq.getRequestType());

		      // have commented this for now will remove it later
		      /*
		       * if (topupReq.getIsSettlement().equals(DBConstants.IsSettlement.DEFAULT.value())) { if
		       * (!userUtil.comparePassword(topupReq.getAuthorizationPin(), user.getPin())) {
		       * LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		       * .message("Pin does not match with Osta Pin").format());
		       * paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED); return
		       * ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse); } }
		       */

		    }

		    if (user == null) {
		      LOG.info(HeaderCode.USER_INVALID_PHONENUM);
		      paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		    }

		    // * get virtual bank details
		    Bank bank = getVirtualBankDetails();

		    // check if bank is valid and active
		    if (!this.bankDBService.isActive(bank)) {
		      LOG.info(HeaderCode.BANK_NOT_ACTIVE);
		      paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		    }

		    // * Get Customer Account
		   
//		    CustomerAccount account = getCustomerAccount(user.getId(), bank.getId(), null);
//		    if (account == null) {
//		      LOG.info(HeaderCode.USER_UNAUTHORIZED);
//		      paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
//		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		//
//		    }
//		    CustomerAccount customerAccount =  customerDBService.getAccount(topupReq.getWalletId());
		//
//		    if (!customerAccount.getBankUId().equals(topupReq.getWalletId())) {
//		      LOG.info("WALLET_ID_INVALID");
//		      paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
//		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
//		    }
		//    
//		    if(customerAccount.getWalletBankId() != NumberUtils.INTEGER_ZERO) {
//		    	// we add only money into account for fastag and then return
//		  	  return addFastagMoney(topupReq, oauthUser,
//		  		       bank, customerAccount);
//		    }
		    
		 // * Get Customer Account
		    CustomerAccount customerAccount;

		    if (TransactionRequestType.CREATEOSTA.value().equals(topupReq.getRequestType())) {
		        // ✅ For CREATEOSTA, get existing account or create new one
		        customerAccount = null;
		        if (StringUtils.isNotBlank(topupReq.getWalletId())) {
		            customerAccount = customerDBService.getAccount(topupReq.getWalletId());

		            if (customerAccount == null || customerAccount.getUser() == null
		                || customerAccount.getUser().getId() != user.getId()
		                || customerAccount.getStatus() != CustomerAccountStatus.ACTIVE.value()) {
		                LOG.error("Customer account not found for walletId: " + topupReq.getWalletId());
		                paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		            }

		            if (!topupReq.getWalletId().equals(customerAccount.getBankUId())) {
		                LOG.info("WALLET_ID_INVALID");
		                paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		            }
		        }

		        if (customerAccount == null) {
		            customerAccount = getCustomerAccount(user.getId(), bank.getId(), null);
		        }
		        
		        if (customerAccount == null) {
		            // Fresh user - need to create wallet
		            LOG.info("Creating new wallet for fresh user: " + user.getPhone());
		            
		            // Create virtual account
		            CreateUserWalletRequest createReq = new CreateUserWalletRequest();
		            createReq.setFname(user.getFirstName());
		            createReq.setLname(user.getLastName());
		            createReq.setEmail(user.getEmail());
		            createReq.setPhonenum(user.getPhone());
		            
		            CreateVirtualAccountRequest createVirtualAccountRequest =
		                populateCreateVirtualAccountRequest(createReq, bank, 0, oauthUser, source);
		            
		            BankRequestContext bankRequestContext = new BankRequestContext();
		            bankRequestContext.setTraceId(httpServletContext.getTraceId());
		            Future<CreateVirtualAccountResponse> bankResponse =
		                bankAPIServices.createVirtualAccount(bankRequestContext, createVirtualAccountRequest);
		            CreateVirtualAccountResponse createVirtualAccountResponse = bankResponse.get();
		            
            String createVirtualResponseCode =
                createVirtualAccountResponse == null ? null : createVirtualAccountResponse.getBankResponseCode();
            if (!Integer.toString(DBConstants.BankTransactionsStatus.SUCCESS.value())
                .equals(createVirtualResponseCode)) {
                LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Failed to create virtual account")
                    .data("bankResponseCode", createVirtualResponseCode)
                    .data("bankResponseDesc",
                        createVirtualAccountResponse != null
                            ? createVirtualAccountResponse.getBankResponseDesc()
                            : "No response from bank")
                    .format());
                paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
                throw new APIException(HttpStatus.BAD_REQUEST, paymentTopupWalletResponse);
            }
		            
		            // ✅ FIX: Use bankUId from creation response
		            String newBankUId = createVirtualAccountResponse.getBankUID();
		            LOG.info("Virtual account created with bankUId: " + newBankUId);
		            
		            // Save customer account
		            Map<String, Object> map = new HashMap<>();
		            map.put("bank", bank);
		            map.put("createVirtualAccountResponse", createVirtualAccountResponse);
		            map.put("role", null);
		            map.put("createVirtualAccountRequest", createVirtualAccountRequest);
		            map.put("clientTransactionId", APIConstants.VIRTUAL + String.valueOf(Math.abs(randomGenerator.nextLong())));
		            
		            ResponseEntity walletCreated = customerResource.addPaymentSource(createReq, user,
		                oauthMerchant, false, TransactionSource.OAUTH, map, null);
		            
		            if (walletCreated.getStatusCode() != HttpStatus.OK) {
		                LOG.error("Failed to save customer account");
		                paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		                throw new APIException(HttpStatus.BAD_REQUEST, paymentTopupWalletResponse);
		            }

		            WalletUserInfo createdWalletInfo =
		                walletCreated.getBody() instanceof WalletUserInfo
		                    ? (WalletUserInfo) walletCreated.getBody()
		                    : null;

		            if (createdWalletInfo == null || createdWalletInfo.getCardId() == null) {
		                LOG.error("Wallet created but cardId is missing from response");
		                paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		            }

		            customerAccount =
		                customerDBService.getAccount(user.getId(), createdWalletInfo.getCardId());

		            if (customerAccount == null) {
		                LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
		                    .message("Customer account not found after creation")
		                    .data("userId", user.getId())
		                    .data("cardId", createdWalletInfo.getCardId())
		                    .data("walletId", createdWalletInfo.getWalletId())
		                    .format());
		                paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		            }

		            if (StringUtils.isBlank(customerAccount.getBankUId())) {
		                customerAccount.setBankUId(newBankUId);
		                customerAccount = customerDBService.updateAccount(customerAccount);
		            }

		            topupReq.setWalletId(
		                StringUtils.isNotBlank(customerAccount.getBankUId())
		                    ? customerAccount.getBankUId()
		                    : newBankUId);
		            LOG.info("Wallet created and resolved with bankUId: " + topupReq.getWalletId());

		        } else {
		            // ✅ Existing wallet - check if bankUId is valid
		            if (customerAccount.getBankUId() == null || customerAccount.getBankUId().isEmpty()) {
		                LOG.error("Existing customer account has null bankUId for user: " + user.getPhone());
		                LOG.info("Creating virtual account for existing customer account");
		                
		                // Create virtual account for this existing customer
		                CreateUserWalletRequest createReq = new CreateUserWalletRequest();
		                createReq.setFname(user.getFirstName());
		                createReq.setLname(user.getLastName());
		                createReq.setEmail(user.getEmail());
		                createReq.setPhonenum(user.getPhone());
		                
		                CreateVirtualAccountRequest createVirtualAccountRequest =
		                	    populateCreateVirtualAccountRequest(createReq, bank, 0, user, source);
		                
		                BankRequestContext bankRequestContext = new BankRequestContext();
		                bankRequestContext.setTraceId(httpServletContext.getTraceId());
		                Future<CreateVirtualAccountResponse> bankResponse =
		                    bankAPIServices.createVirtualAccount(bankRequestContext, createVirtualAccountRequest);
		                CreateVirtualAccountResponse createVirtualAccountResponse = bankResponse.get();
		                
                String existingCustomerCreateWalletCode =
                    createVirtualAccountResponse == null ? null : createVirtualAccountResponse.getBankResponseCode();
                if (!Integer.toString(DBConstants.BankTransactionsStatus.SUCCESS.value())
                    .equals(existingCustomerCreateWalletCode)) {
                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                        .message("Failed to create virtual account for existing customer")
                        .data("bankResponseCode", existingCustomerCreateWalletCode)
                        .data("bankResponseDesc",
                            createVirtualAccountResponse != null
                                ? createVirtualAccountResponse.getBankResponseDesc()
                                : "No response from bank")
                        .format());
                    paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
                    throw new APIException(HttpStatus.BAD_REQUEST, paymentTopupWalletResponse);
                }
		                
		                String newBankUId = createVirtualAccountResponse.getBankUID();
		                topupReq.setWalletId(newBankUId);
		                LOG.info("Virtual account created for existing customer with bankUId: " + newBankUId);
		                
		                // ✅ Update the customerAccount with the new bankUId
		                customerAccount.setBankUId(newBankUId);
		                customerAccount = customerDBService.updateAccount(customerAccount);
		                LOG.info("Updated customerAccount with new bankUId: " + newBankUId);
		                
		             // ✅ ADD THIS: Refetch by bankUId to verify the update
		                CustomerAccount refetchedAccount = customerDBService.getAccount(newBankUId);
		                if (refetchedAccount != null) {
		                    LOG.info("Refetched customerAccount - bankUId: " + refetchedAccount.getBankUId());
		                    customerAccount = refetchedAccount;
		                } else {
		                    LOG.error("Failed to refetch customerAccount by bankUId: " + newBankUId);
		                }
		                
		            } else {
		                topupReq.setWalletId(customerAccount.getBankUId());
		                LOG.info("Using existing wallet with bankUId: " + customerAccount.getBankUId());
		            }
		        }
		    } else {
		        // For WALLET_TOP_UP, fetch by walletId
		        customerAccount = customerDBService.getAccount(topupReq.getWalletId());
		        
		        if (!customerAccount.getBankUId().equals(topupReq.getWalletId())) {
		            LOG.info("WALLET_ID_INVALID");
		            paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		        }
		    }

		    if (TransactionRequestType.CREATEOSTA.value().equals(topupReq.getRequestType())) {
		        ResponseEntity cleanupResponse = releaseLegacyCreateOstaDipcoins(user, customerAccount);
		        if (cleanupResponse != null) {
		            return cleanupResponse;
		        }
		        LOG.info("Using fastag CREATEOSTA topup path without wallet dipcoin generation");
		        return addFastagMoney(topupReq, oauthUser, bank, customerAccount);
		    }

		    // * Get Customer osta
		    List<Dipcoin> existingDipcoin = this.coinDBService.getDipcoins(user.getId(),
		        DBConstants.DipcoinStatus.ACTIVE.value(), customerAccount.getId());

		    CustomerDipcoinResponse customerDipcoinResponse = null;
		    if (existingDipcoin.size() == 0) {

		      if (DBConstants.USER_MAX_PER_TX_LIMIT.compareTo(topupReq.getAmount()) < 0) {
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(APIResponse.error(HeaderCode.WALLET_TOPUP_FAILED));
		      }

		      if (topupReq.getIsSettlement().equals(DBConstants.IsSettlement.DEFAULT.value())) {

		        // add money to virtual account with that
		        AddMoneyInVirtualAccountRequest addMoneyInVirtualAccountRequest =
		            populateAddMoneyInVirtualAccountRequest(topupReq, bank, oauthUser, customerAccount,
		                topupReq.getRequestType());
		        BankRequestContext bankRequestContext = new BankRequestContext();
		        bankRequestContext.setTraceId(httpServletContext.getTraceId());
		        
		        LOG.info("Adding money to wallet with bankUId: " + customerAccount.getBankUId());
		        LOG.info("AddMoneyRequest details - Amount: " + topupReq.getAmount() + ", WalletId: " + topupReq.getWalletId());
		        
		        Future<AddMoneyInVirtualAccountResponse> bankResponse = bankAPIServices
		            .addMoneyToVirtualAccount(bankRequestContext, addMoneyInVirtualAccountRequest);
		        AddMoneyInVirtualAccountResponse addMoneyInVirtualAccountResponse = bankResponse.get();

		        if (!addMoneyInVirtualAccountResponse.getBankResponseCode()
		            .equals(Integer.toString(DBConstants.BankTransactionsStatus.SUCCESS.value()))) {
		          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .data("Reason", "rollback associated exception being handled").format());
		          throw new APIException(HttpStatus.BAD_REQUEST,
		              APIResponse.error(HeaderCode.WALLET_TOPUP_FAILED));
		          // return
		          // ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.WALLET_TOPUP_FAILED));
		        }

		      }

		     
		      
		      // create Dipcoion(lienmark)
		      String clientTransactionId =
		          APIConstants.VIRTUAL + String.valueOf(Math.abs(randomGenerator.nextLong()));
		      httpServletContext.setClientTransactionId(clientTransactionId);
		      CustomerDipcoinRequest customerDipcoinRequest =
		          populateCustomerDipcoinRequest(customerAccount, topupReq, topupReq.getRequestType());
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .data("customerDipcoinRequest for create dipcoin", customerDipcoinRequest).format());
		      ResponseEntity dipcoionCreated =
		          customerDipcoinResource.createDipcoin(user, customerDipcoinRequest, false);
		     

		      // customerDipcoinResponse = objectMapper.convertValue(dipcoionCreated,
		      // CustomerDipcoinResponse.class);
		      if (dipcoionCreated.getStatusCode() != HttpStatus.OK) {
		        LOG.info(dipcoionCreated.getBody());
		        paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		            .data("Reason", "rollback associated exception being handled").format());
		        throw new APIException(HttpStatus.BAD_REQUEST, paymentTopupWalletResponse);
		        // return
		        // ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		      }
		      customerDipcoinResponse = (CustomerDipcoinResponse) dipcoionCreated.getBody();
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .data("dipcoionCreated", customerDipcoinResponse.getDipcoinId()).format());
		    } else {

		      // *store the amount of existing dipcoin
		      BigDecimal existingDipcoinAmount = existingDipcoin.get(0).getAmount();

		      if (DBConstants.USER_MAX_PER_TX_LIMIT
		          .compareTo(topupReq.getAmount().add(existingDipcoinAmount)) < 0) {
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		            .body(APIResponse.error(HeaderCode.WALLET_TOPUP_FAILED));
		      }

		      // *release previous dipcoin(remove liem mark)
		      String encryptedCoin = user.getPhone().concat(existingDipcoin.get(0).getCoin());
		      ResponseEntity releasedDipcoin = customerDipcoinResource.deleteDipcoin(user, encryptedCoin, false);

		      if (releasedDipcoin.getStatusCode() != HttpStatus.OK) {

		        LOG.info(releasedDipcoin.getBody());
		        paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		      }

		      customerDipcoinResponse = (CustomerDipcoinResponse) releasedDipcoin.getBody();

		      if (topupReq.getIsSettlement().equals(DBConstants.IsSettlement.DEFAULT.value())) {

		        // *add money to virtual account with that
		        AddMoneyInVirtualAccountRequest addMoneyInVirtualAccountRequest =
		            populateAddMoneyInVirtualAccountRequest(topupReq, bank, oauthUser, customerAccount,
		                topupReq.getRequestType());
		        BankRequestContext bankRequestContext = new BankRequestContext();
		        bankRequestContext.setTraceId(httpServletContext.getTraceId());
		        Future<AddMoneyInVirtualAccountResponse> bankResponse = bankAPIServices
		            .addMoneyToVirtualAccount(bankRequestContext, addMoneyInVirtualAccountRequest);
		        AddMoneyInVirtualAccountResponse addMoneyInVirtualAccountResponse = bankResponse.get();

		        if (!addMoneyInVirtualAccountResponse.getBankResponseCode()
		            .equals(Integer.toString(DBConstants.BankTransactionsStatus.SUCCESS.value()))) {
		          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .data("Reason", "rollback associated exception being handled").format());
		          throw new APIException(HttpStatus.BAD_REQUEST,
		              APIResponse.error(HeaderCode.WALLET_TOPUP_FAILED));
		          // return
		          // ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.WALLET_TOPUP_FAILED));
		        }

		      }

		      // * create new dipcoin by adding new amount and released dipcoin
		      // amount(lienmark)
		      String clientTransactionId =
		          APIConstants.VIRTUAL + String.valueOf(Math.abs(randomGenerator.nextLong()));
		      httpServletContext.setClientTransactionId(clientTransactionId);
		      BigDecimal addingexistandnewAmount = topupReq.getAmount().add(existingDipcoinAmount);
		      topupReq.setAmount(addingexistandnewAmount);
		      
		      CustomerDipcoinRequest customerDipcoinRequest =
		          populateCustomerDipcoinRequest(customerAccount, topupReq, topupReq.getRequestType());
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .data("CustomerDipcoinRequest for create dipcoin", customerDipcoinRequest).format());
		      
		      ResponseEntity dipcoionCreated =
		          customerDipcoinResource.createDipcoin(user, customerDipcoinRequest, false);
		      if (dipcoionCreated.getStatusCode() != HttpStatus.OK) {

		        LOG.info(dipcoionCreated.getBody());
		        paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		            .data("Reason", "rollback associated exception being handled").format());
		        throw new APIException(HttpStatus.BAD_REQUEST, paymentTopupWalletResponse);
		        // return
		        // ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		      }
		      customerDipcoinResponse = (CustomerDipcoinResponse) dipcoionCreated.getBody();
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		              .data("DipcoinId created", customerDipcoinResponse.getDipcoinId()).format());
		    }
		    
		    if (topupReq.getRequestType().equals(TransactionRequestType.CREATEOSTA.value())
		        && topupReq.getIsSettlement().equals(IsSettlement.DEFAULT.value())) {

		      walletMetricRegistry.countOfAmountLoadedByAggrepay().increment();
		    }
		    // response

		    paymentTopupWalletResponse.setAmount(topupReqAmount.toString());
		    paymentTopupWalletResponse.setCurrency(topupReq.getCurrency());
		    paymentTopupWalletResponse.setWalletId(topupReq.getWalletId());
		    paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_SUCCESSFUL);
		    paymentTopupWalletResponse
		        .setOstaTransactionReferenceId(customerDipcoinResponse.getOstaTransactionReferenceId());
		    paymentTopupWalletResponse
		        .setPartnerTransactionReferenceId(topupReq.getPartnerTransactionReferenceId());
		    paymentTopupWalletResponse.setOrderId(topupReq.getOrderId());

		    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		        .message("Add money to wallet Response")
		        .data("Amount", paymentTopupWalletResponse.getAmount())
		        .data("Currency", paymentTopupWalletResponse.getCurrency())
		        .data("PartnerTransactionRefrenceId",
		            paymentTopupWalletResponse.getPartnerTransactionReferenceId())
		        .data("Phonenum", paymentTopupWalletResponse.getOstaTransactionReferenceId())
		        .data("OstaTransactionRefrenceId",
		            paymentTopupWalletResponse.getOstaTransactionReferenceId())
		        .data("WalletId", paymentTopupWalletResponse.getWalletId()).format());

		    return (ResponseEntity.ok(paymentTopupWalletResponse));
		  }

	public ResponseEntity addFastagMoney(AddMoneyToWalletRequest topupReq, User oauthUser, Bank bank,
			CustomerAccount customerAccount) throws APIException, Exception {
		LOG.info("Adding fastag money directly to wallet with bankUId: " + customerAccount.getBankUId());
		AddMoneyInVirtualAccountRequest addMoneyInVirtualAccountRequest = populateAddMoneyInVirtualAccountRequest(
				topupReq, bank, oauthUser, customerAccount, topupReq.getRequestType());
		BankRequestContext bankRequestContext = new BankRequestContext();
		bankRequestContext.setTraceId(httpServletContext.getTraceId());
		Future<AddMoneyInVirtualAccountResponse> bankResponse = bankAPIServices
				.addMoneyToVirtualAccount(bankRequestContext, addMoneyInVirtualAccountRequest);
		AddMoneyInVirtualAccountResponse addMoneyInVirtualAccountResponse = bankResponse.get();

		if (!addMoneyInVirtualAccountResponse.getBankResponseCode()
				.equals(Integer.toString(DBConstants.BankTransactionsStatus.SUCCESS.value()))) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.data("Reason", "rollback associated exception being handled").format());
			throw new APIException(HttpStatus.BAD_REQUEST, APIResponse.error(HeaderCode.WALLET_TOPUP_FAILED));
		}

		PaymentTopupWalletResponse paymentTopupWalletResponse = new PaymentTopupWalletResponse();
		paymentTopupWalletResponse.setAmount(topupReq.getAmount().toString());
		paymentTopupWalletResponse.setCurrency(topupReq.getCurrency());
		paymentTopupWalletResponse.setWalletId(topupReq.getWalletId());
		paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_SUCCESSFUL);
		paymentTopupWalletResponse.setPartnerTransactionReferenceId(topupReq.getPartnerTransactionReferenceId());
		paymentTopupWalletResponse.setOrderId(topupReq.getOrderId());

		if (topupReq.getRequestType().equals(TransactionRequestType.CREATEOSTA.value())
				&& topupReq.getIsSettlement().equals(IsSettlement.DEFAULT.value())) {
			walletMetricRegistry.countOfAmountLoadedByAggrepay().increment();
		}

		return ResponseEntity.ok(paymentTopupWalletResponse);
	}

	private ResponseEntity releaseLegacyCreateOstaDipcoins(User user, CustomerAccount customerAccount)
			throws Exception, APIException {
		List<Dipcoin> existingDipcoins = this.coinDBService.getDipcoins(user.getId(),
				DBConstants.DipcoinStatus.ACTIVE.value(), customerAccount.getId());

		if (CollectionUtils.isEmpty(existingDipcoins)) {
			return null;
		}

		for (Dipcoin existingDipcoin : existingDipcoins) {
			if (existingDipcoin == null) {
				continue;
			}

			Integer usageType = existingDipcoin.getUsageType();
			boolean legacyWalletHold = usageType == null
					|| DBConstants.DipcoinUsageType.GENERIC.value() == usageType
					|| DBConstants.DipcoinUsageType.WALLET.value() == usageType;

			if (!legacyWalletHold) {
				continue;
			}

			LOG.info("Releasing legacy CREATEOSTA dipcoin before direct wallet credit. dipcoinId: "
					+ existingDipcoin.getId() + ", usageType: " + usageType + ", amount: "
					+ existingDipcoin.getAmount());
			ResponseEntity releasedDipcoin = customerDipcoinResource.deleteDipcoin(user,
					user.getPhone().concat(existingDipcoin.getCoin()), false);

			if (releasedDipcoin.getStatusCode() != HttpStatus.OK) {
				LOG.info(releasedDipcoin.getBody());
				PaymentTopupWalletResponse paymentTopupWalletResponse = new PaymentTopupWalletResponse();
				paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
			}
		}

		return null;
	}

	public User getUserDetails(String phonenum, int merchantId, String requestType) {
		User user;
		if (requestType.equals(TransactionRequestType.CREATEOSTA.value())) {
			List<String> roles = new ArrayList<>();
			roles.add(UserRoles.CUSTOMER.value());
			roles.addAll(UserRoles.bankRoles());
			user = this.userDBService.getUserDetails(phonenum, merchantId, roles,
					DBConstants.UserStatus.ACTIVE.value());
		} else {
			user = this.userDBService.getUserDetails(phonenum, merchantId,
					Arrays.asList(UserRoles.VIRTUAL_CUSTOMER.value(), UserRoles.VIRTUAL_MERCHANT.value()),
					DBConstants.UserStatus.ACTIVE.value());
		}
		return user;
	}

	public Bank getVirtualBankDetails() throws InterruptedException, ExecutionException {
		List<Bank> banks = bankDBService.getBanks(Arrays.asList(BankStatus.ACTIVE.value()),
				Arrays.asList(BankType.VIRTUAL_BANK.value()), null, null);
		if (CollectionUtils.isEmpty(banks)) {
			return null;
		}
		return banks.get(0);
	}

	public AddMoneyInVirtualAccountRequest populateAddMoneyInVirtualAccountRequest(
			final AddMoneyToWalletRequest addMoneyToWallet, final Bank bank, User oauthUser,
			CustomerAccount customerAccount, String requestType) {

		AddMoneyInVirtualAccountRequest addMoneyInVirtualAccountRequest = new AddMoneyInVirtualAccountRequest(
				bank.getReferenceId(), bank.getCode());
		String dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(bank.getReferenceId(),
				addMoneyInVirtualAccountRequest.getOperation(), BankTransactionType.ADD_MONEY_TO_VIRTUAL_BANK.value());

		addMoneyInVirtualAccountRequest.setAmount(roundOfBigDecimalAmount(addMoneyToWallet.getAmount()).toString());
		addMoneyInVirtualAccountRequest.setBankUId(addMoneyToWallet.getWalletId());
		addMoneyInVirtualAccountRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
		addMoneyInVirtualAccountRequest
				.setTransactionType(Integer.toString(BankTransactionType.ADD_MONEY_TO_VIRTUAL_BANK.value()));
		addMoneyInVirtualAccountRequest.setIsSettlement(String.valueOf(addMoneyToWallet.getIsSettlement()));

		return addMoneyInVirtualAccountRequest;
	}

	public CustomerDipcoinRequest populateCustomerDipcoinRequest(CustomerAccount customerAccount,
			AddMoneyToWalletRequest addMoneyToWalletRequest, String requestType) {
		CustomerDipcoinRequest customerDipcoinRequest = new CustomerDipcoinRequest();
		customerDipcoinRequest.setCardId(customerAccount.getUserCardId());
		customerDipcoinRequest.setAmount(roundOfBigDecimalAmount(addMoneyToWalletRequest.getAmount()));
		customerDipcoinRequest.setCurrency(addMoneyToWalletRequest.getCurrency());

		if (!StringUtils.isEmpty(addMoneyToWalletRequest.getNoOfInvoice())) {
			customerDipcoinRequest.setNoOfInvoice(addMoneyToWalletRequest.getNoOfInvoice());
		}
		if (!StringUtils.isEmpty(addMoneyToWalletRequest.getPaymentMode())) {
			customerDipcoinRequest.setPaymentMode(addMoneyToWalletRequest.getPaymentMode());
		}
		if (!StringUtils.isEmpty(addMoneyToWalletRequest.getMerchantName())) {
			customerDipcoinRequest.setMerchantName(addMoneyToWalletRequest.getMerchantName());
		}

		// Using a default PIN since VirtualBankProperties is not easily
		// reachable/available in this module's config
		customerDipcoinRequest.setAuthorizationPin("789987");

		if (requestType.equals(TransactionRequestType.WALLET_TOP_UP.value())) {
			customerDipcoinRequest.setUsageType(DBConstants.DipcoinUsageType.WALLET.value());
		}
		customerDipcoinRequest.setValidateAuthorizationPin(Boolean.FALSE);
		customerDipcoinRequest.setOrderId(addMoneyToWalletRequest.getOrderId());
		customerDipcoinRequest
				.setPartnerTransactionReferenceId(addMoneyToWalletRequest.getPartnerTransactionReferenceId());
		customerDipcoinRequest.setTtlInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);
		customerDipcoinRequest.setEncryptDipcoin(Boolean.FALSE);
		return customerDipcoinRequest;
	}

	public BigDecimal roundOfBigDecimalAmount(BigDecimal amount) {
		BigDecimal roundOfamount = amount.setScale(2, BigDecimal.ROUND_HALF_UP);
		LOG.info(roundOfamount);
		return roundOfamount;
	}

	// -------------------------------------------------------------------------------------------------------
	public CustomerAccount getCustomerAccount(int userId, int bankId, Integer walletBankId) {

		List<CustomerAccount> account = this.customerDBService.getUserAccountsByUserIdAndStatus(userId,
				DBConstants.CustomerAccountStatus.ACTIVE.value(),
				DBConstants.CustomerAccountMethodType.WALLET_INB.value(), bankId, walletBankId);

		if (account == null || account.size() == 0) {
			return null;
		}

		return account.get(0);

	}
	
	public CreateVirtualAccountRequest populateCreateVirtualAccountRequest(final CreateUserWalletRequest createReq,
			final Bank bank, int noOfUserAccounts, User oauthUser, TransactionSource source) {
		String accounNumber = null;
		String now = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());

		accounNumber = now;
		CreateVirtualAccountRequest createVirtualAccountRequest = new CreateVirtualAccountRequest(bank.getReferenceId(),
				bank.getCode());

		String dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(bank.getReferenceId(),
				createVirtualAccountRequest.getOperation(), BankTransactionType.CREATE_ACCOUNT.value());

		createVirtualAccountRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
		createVirtualAccountRequest.setAccountNumber(accounNumber);
		createVirtualAccountRequest.setIPAddress(httpServletContext.getOriginIp());
		createVirtualAccountRequest.setPassword(virtualBankProperties.getPassword());
		createVirtualAccountRequest.setPartnerRefrenceId(oauthUser != null ? Integer.toString(oauthUser.getBankMerchantId()) : "0");
		createVirtualAccountRequest.setSource(Integer.toString(source.value()));
		createVirtualAccountRequest.setStatus(Integer.toString(BankAccountStatus.ACTIVE.value()));
		createVirtualAccountRequest.setLogin(createReq.getPhonenum());
		createVirtualAccountRequest.setUpdateDate(now);
		createVirtualAccountRequest.setTransactionType(Integer.toString(BankTransactionType.CREATE_ACCOUNT.value()));
		createVirtualAccountRequest.setFname(createReq.getFname());
		createVirtualAccountRequest.setLname(createReq.getLname());

		return createVirtualAccountRequest;
	}
	
	public CustomerAccountRequest populateCustomerAccountRequest(CreateUserWalletRequest createReq, Bank bank,
			CreateVirtualAccountRequest createVirtualAccountRequest,
			CreateVirtualAccountResponse createVirtualAccountResponse, User user) throws APIException {
		
		String encryptedPassword = encryptionResource.encrypt(user, null, bank, "P@ssword123");
		
		CustomerAccountRequest customerAccountRequest = new CustomerAccountRequest();

		customerAccountRequest.setAccountNumber(createVirtualAccountRequest.getAccountNumber());
		customerAccountRequest.setBankReferenceId(bank.getReferenceId());
		customerAccountRequest.setMethodType(DBConstants.CustomerAccountMethodType.WALLET_INB.value());
		customerAccountRequest.setTypeOfMethod(DBConstants.CustomerAccountMethodType.WALLET_INB.value());
		customerAccountRequest.setAutoGenerateOsta(Boolean.TRUE);
		
		customerAccountRequest.setEncryptedCredential(encryptedPassword);
		customerAccountRequest.setEncryptedLogin(createReq.getPhonenum());
		customerAccountRequest.setHashedLogin(createReq.getPhonenum());
		customerAccountRequest.setMaskedLogin(createReq.getPhonenum());
		customerAccountRequest.setDipcoinRefrenceNumber(createVirtualAccountResponse.getDipcoinReferenceNumber());
		customerAccountRequest
				.setBankTransactionRefrenceNumber(createVirtualAccountResponse.getBankTransactionReferenceNumber());
		customerAccountRequest.setTransactionTime(createVirtualAccountResponse.getTransactionTime());
		customerAccountRequest.setOstaTTLInHrs(DBConstants.DIPCOIN_ONE_YEAR_TTL_HRS);

		return customerAccountRequest;

	}
	
	public CustomerAccountVerifyRequest populateCustomerAccountVerifyRequest(
			CreateVirtualAccountResponse createVirtualAccountResponse, CustomerAccountRequest customerAccountRequest,
			CustomerAccountResponse customerAccountResponse) {
		CustomerAccountVerifyRequest customerAccountVerifyRequest = new CustomerAccountVerifyRequest();
		customerAccountVerifyRequest.setBankUId(createVirtualAccountResponse.getBankUID());
		customerAccountVerifyRequest.setCardId(customerAccountResponse.getCardId());
		customerAccountVerifyRequest.setMethodType(customerAccountRequest.getMethodType());
		customerAccountVerifyRequest.setOtp(OTP);
		customerAccountVerifyRequest.setDipcoinRefrenceNumber(createVirtualAccountResponse.getDipcoinReferenceNumber());
		customerAccountVerifyRequest
				.setBankTransactionRefrenceNumber(createVirtualAccountResponse.getBankTransactionReferenceNumber());
		customerAccountVerifyRequest.setTransactionTime(createVirtualAccountResponse.getTransactionTime());

		return customerAccountVerifyRequest;

	}
	
	  public ResponseEntity createUserWallet(final CreateUserWalletRequest createReq, User oauthUser,
		      Merchant oauthMerchant, Boolean createUser, TransactionSource source, String walletBankReferenceId)
		      throws Exception, APIException {

		    WalletUserInfo walletUserInfo = new WalletUserInfo();
		    String role = null;

		    if (createUser) {
		      boolean customer = APIConstants.CUSTOMER.equals(createReq.getRole());
		      boolean merchant = APIConstants.MERCHANT.equals(createReq.getRole());

		      if (customer) {
		        role = APIConstants.VIRTUAL_CUSTOMER;
		      } else if (merchant) {
		        role = APIConstants.VIRTUAL_MERCHANT;
		      }
		    }

		    String clientTransactionId =
		        APIConstants.VIRTUAL + String.valueOf(Math.abs(randomGenerator.nextLong()));

		    // * get virtual bank details
		    Bank bank = getVirtualBankDetails();

		    // check if bank is valid and active
		    if (!this.bankDBService.isActive(bank)) {

		      walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(walletUserInfo);
		    }

		    if (createUser) {

		      // * register user
		      UserRegisterRequest userRegisterRequest = populateUserRegisterRequest(createReq);
		      ResponseEntity userRegistered = userLoginResource.registerUser(userRegisterRequest, role,
		          clientTransactionId, oauthUser, oauthMerchant, Boolean.FALSE);

		      if (userRegistered.getStatusCode() != HttpStatus.CREATED) {
		        walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		        LOG.info(userRegistered.getBody());
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(walletUserInfo);
		      }

		      // * verify user
		      UserVerifyRequest userVerifyRequest = populateUserVerifyRequest(createReq);
		      ResponseEntity userVerified = userLoginResource.verifyUserCredentials(userVerifyRequest, role,
		          oauthUser, oauthMerchant);

		      if (userVerified.getStatusCode() != HttpStatus.OK) {
		        LOG.info(userVerified.getBody());
		        walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		            .data("Reason", "rollback associated exception being handled").format());
		        throw new APIException(HttpStatus.BAD_REQUEST, walletUserInfo);
		      }

		    }

		    if (!createUser) {
		      // * Get Customer Account
		    	Bank walletBank = null;
		    	if(StringUtils.isNoneBlank(walletBankReferenceId)) {
		    	 walletBank = bankDBService.getBank(walletBankReferenceId);
		    	}
		      CustomerAccount account = getCustomerAccount(oauthUser.getId(), bank.getId(), walletBank != null ? walletBank.getId() : null );
		      if (account != null) {
		        // response
		        walletUserInfo.setFname(createReq.getFname().trim().replaceAll(" ", ""));
		        walletUserInfo.setLname(createReq.getLname().trim().replaceAll(" ", ""));
		        walletUserInfo.setPhonenum(createReq.getPhonenum());
		        walletUserInfo.setEmail(createReq.getEmail());
		        walletUserInfo.setWalletId(account.getBankUId());
		        walletUserInfo.setCardId(account.getUserCardId());

		        walletUserInfo.addHeaderCode(HeaderCode.WALLET_CREATED);

		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		            .message("Create User Wallet Response")
		            .data("Fname", walletUserInfo.getFname().trim().replaceAll(" ", ""))
		            .data("Lname", walletUserInfo.getLname().trim().replaceAll(" ", ""))
		            .data("Email", walletUserInfo.getEmail()).data("Phonenum", walletUserInfo.getPhonenum())
		            .data("WalletId", walletUserInfo.getWalletId()).format());

		        return ResponseEntity.ok(walletUserInfo);
		      }
		    }

		    // * create virtual account

    CreateVirtualAccountRequest createVirtualAccountRequest =
        populateCreateVirtualAccountRequest(createReq, bank, 0, oauthUser, source);

    BankRequestContext bankRequestContext = new BankRequestContext();
    bankRequestContext.setTraceId(httpServletContext.getTraceId());
    Future<CreateVirtualAccountResponse> bankResponse =
        bankAPIServices.createVirtualAccount(bankRequestContext, createVirtualAccountRequest);
    CreateVirtualAccountResponse createVirtualAccountResponse = null;
    try {
      createVirtualAccountResponse = bankResponse.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Virtual account creation interrupted").format(), e);
      walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
      throw new APIException(HttpStatus.BAD_REQUEST, walletUserInfo);
    } catch (ExecutionException e) {
      LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Virtual account creation failed").data("reason", e.getMessage()).format(), e);
      walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
      throw new APIException(HttpStatus.BAD_REQUEST, walletUserInfo);
    }

    String bankResponseCode =
        createVirtualAccountResponse != null ? createVirtualAccountResponse.getBankResponseCode() : null;
    if (!Integer.toString(DBConstants.BankTransactionsStatus.SUCCESS.value())
        .equals(bankResponseCode)) {

      walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
      LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
          .message("createVirtualAccount failed").data("bankResponseCode", bankResponseCode)
          .data("bankResponseDesc",
              createVirtualAccountResponse != null ? createVirtualAccountResponse.getBankResponseDesc()
                  : "No response from bank")
          .format());
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .data("Reason", "rollback associated exception being handled").format());
      throw new APIException(HttpStatus.BAD_REQUEST, walletUserInfo);
      // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(walletUserInfo);
    }

		    Map<String, Object> map = new HashMap<>();
		    map.put("bank", bank);
		    map.put("createVirtualAccountResponse", createVirtualAccountResponse);
		    map.put("role", role);
		    map.put("createVirtualAccountRequest", createVirtualAccountRequest);
		    map.put("clientTransactionId", clientTransactionId);
		    
		    walletMetricRegistry.numberOfWalletCreated().increment();

		    return customerResource.addPaymentSource(createReq, oauthUser,
		        httpServletContext.getMerchant(), createUser, TransactionSource.OAUTH, map, walletBankReferenceId);

		  }
	  

	  // to build UserRegister request to call existing resource
	  public UserRegisterRequest populateUserRegisterRequest(final CreateUserWalletRequest createReq) {

	    UserRegisterRequest userRegisterRequest = new UserRegisterRequest();
	    userRegisterRequest.setEmail(createReq.getEmail());
	    userRegisterRequest.setFname(createReq.getFname().trim().replaceAll(" ", ""));
	    userRegisterRequest.setLname(createReq.getLname().trim().replaceAll(" ", ""));
	    userRegisterRequest.setPhonenum(createReq.getPhonenum());
	    // set default fields
	    userRegisterRequest.setPassword(virtualBankProperties.getPassword());
	    userRegisterRequest.setPin(virtualBankProperties.getPin());
	    userRegisterRequest.setTnc(Boolean.TRUE);

	    return userRegisterRequest;
	  }
	  
	  // to build UserRegister request to call existing resource
	  public UserVerifyRequest populateUserVerifyRequest(final CreateUserWalletRequest createReq) {

	    UserVerifyRequest userVerifyRequest = new UserVerifyRequest();
	    userVerifyRequest.setOtp("987789");
	    userVerifyRequest.setPassword(virtualBankProperties.getPassword());
	    userVerifyRequest.setPin(virtualBankProperties.getPin());
	    userVerifyRequest.setPhonenum(createReq.getPhonenum());

	    return userVerifyRequest;
	  }

}
