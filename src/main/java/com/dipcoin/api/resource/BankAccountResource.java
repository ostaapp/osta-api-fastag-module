package com.dipcoin.api.resource;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.PartnerApprovalHelper;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.BankAccountRequest;
import com.dipcoin.api.model.BankAccountResponse;
import com.dipcoin.api.model.CustomerAccountRequest;
import com.dipcoin.api.model.CustomerAccountResponse;
import com.dipcoin.api.model.PartnerApprovalStatus;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.PartnerDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.Utils;
import com.dipcoin.db.services.commons.DBConstants.BankAccountCodes;
import com.dipcoin.db.services.commons.DBConstants.BankAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.BankStatus;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.CustomerAccountStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantStatus;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalFields;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalLevels;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankAccount;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.MerchantOnboard;
import com.dipcoin.db.services.model.PartnerApproval;
import com.dipcoin.db.services.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("bankAccountResource")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class BankAccountResource {

  private static final Logger LOG = LogManager.getLogger(BankAccountResource.class);

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private EncryptionResource encryptionResource;

  @Autowired
  private PartnerApprovalHelper partnerApprovalHelper;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;
  
  @Autowired
  private CustomerResource customerResource;
  

  @Autowired
  private PartnerDBService partnerDBService;

  @Autowired
  private MerchantDBService merchantDBService;
  
  @Autowired
  private CustomerDBService customerDBService;

  /*
   * 
   */
  public ResponseEntity addBankAccount(final User user, final Merchant merchant, final Bank bank,
      final BankAccountRequest addReq, final String clientTransactionId)
      throws Exception, APIException {

    LOG.debug(
        LogFormatter.instance(httpServletContext.getTraceId()).data("Request", addReq).format());

    boolean merchantRequest = merchant != null ? true : false;
    boolean bankRequest = bank != null ? true : false;

    BankAccountResponse response = new BankAccountResponse();
    response.setClientTransactionId(clientTransactionId);

    if (clientTransactionId == null) {
      response.addHeaderCode(HeaderCode.MISSING_CLIENTTRANSACTIONID);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    if (addReq == null) {
      response.addHeaderCode(HeaderCode.BAD_REQUEST);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    if (!addReq.validate(httpServletContext)) {
      response.addHeaderCodes(addReq.getErrorCodes());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    if (!this.userDBService.isActive(user)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
    }

    if (merchantRequest) {
      // verify user is active and a merchant admin
      if (!(this.userDBService.isMerchantAdmin(user)
          || this.userDBService.isMerchantSuperAdmin(user))) {
        response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }
    } else if (bankRequest) {
      // verify user is active and a bank admin
    	if (!(this.userDBService.isBankAdmin(user) || this.userDBService.isBankSuperAdmin(user)
    			|| this.userDBService.isBrontooSuperAdmin(user) || this.userDBService.isBrontooAdmin(user))) {
        response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }
    } else {
      response.addHeaderCode(HeaderCode.USER_UNAUTHORIZED);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // get merchant/bank all accts
    Future<List<BankAccount>> existingAccountsTask =
        merchantRequest ? this.bankDBService.asyncGetMerchantBankAccounts(merchant.getId())
            : this.bankDBService.asyncGetBankAccounts(bank.getId());

    // get merchant account
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get Merchant/Bank")
        .data("BankMerchantId", user.getBankMerchantId()).format());
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get BankAccount")
    		.data("Masked AccountNumber", Utils.getMaskedAccountNumber(addReq.getAccountNumber())).data("IfscCode", addReq.getIfscCode())
        .format());

    BankAccount account = null;
    List<BankAccount> listOfAccounts=null;
    
    if(merchantRequest) {
      account=this.bankDBService.getMerchantBankAccount(merchant.getId(), addReq.getAccountNumber(),
          addReq.getIfscCode());
      
      if (account != null) {
        if (account.getClientTransactionId() != null
            && account.getClientTransactionId().equals(clientTransactionId)) {
          populateBankAccountResponse(encryptionResource, user, merchant, bank, account, response);
          response.addHeaderCode(HeaderCode.BANK_ACCOUNT_ADDED);
          return ResponseEntity.ok(response);
        }

        response.addHeaderCode(HeaderCode.BANK_ACCOUNT_EXISTS);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
      }
    }
    
    /*
     * we can add only one bank account in each account type 
     * we can use same account number for each accounnt type
     */
    else if(bankRequest) {
      listOfAccounts= this.bankDBService.getBankAccountByCode(bank.getId(), BankAccountCodes.valueOf(addReq.getAccountType()));
      
      if(listOfAccounts!=null) {
        for(BankAccount acc : listOfAccounts) {
          
          if (acc.getClientTransactionId() != null
              && !acc.getClientTransactionId().equals(clientTransactionId)) {
            
            if(acc.getStatus() == BankAccountStatus.ACTIVE.value()) {
              response.addHeaderCode(HeaderCode.BANK_ACCOUNT_EXISTS);
              return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }  
          }
        }
      } 
    }   

    // existing accounts
    List<BankAccount> existingAccounts = existingAccountsTask.get();
    boolean setAsPrimaryAccount = true;
    BankAccount primaryAccount = null;
    if (!CollectionUtils.isEmpty(existingAccounts)) {
      for (BankAccount acct : existingAccounts) {
        if (acct.getIsPrimaryAccount() == BooleanStatus.YES.value()) {
          primaryAccount = acct;
          setAsPrimaryAccount = false;
          break;
        }
      }
    }

    BankAccount bAccount = new BankAccount();
    bAccount.setAccountHolderName(addReq.getAccountHolderName());
    bAccount.setAccountNumber(addReq.getAccountNumber());
    bAccount.setIsPrimaryAccount(
        setAsPrimaryAccount == true ? BooleanStatus.YES.value() : addReq.getIsPrimaryAccount());
    bAccount.setAccountType(addReq.getBankAccountType() != null? addReq.getBankAccountType() : DBConstants.BankAccountType.SAVINGS.value());
    bAccount.setIFSCCode(addReq.getIfscCode());
    bAccount.setClientTransactionId(clientTransactionId);
    
    if (bankRequest) {
      bAccount.setBank(bank);
      bAccount.setCode(BankAccountCodes.valueOf(addReq.getAccountType()));
    }
    
    if (merchantRequest) {
      bAccount.setMerchant(merchant);
      bAccount.setCode(BankAccountCodes.valueOf(addReq.getAccountType()));
    }

    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("Adding new Bank account").data("BankAccount", bAccount).format());
    
    if (bankRequest) {
    this.partnerApprovalHelper.submitPartnerApproval(user, null, null, bank,
          PartnerApprovalFields.BANK_ACCOUNT);
    }
    
    if (merchantRequest) {
      
     MerchantOnboard  merchantOnboard = merchantDBService.getMerchantOnboardByMerchantId(merchant.getId());
     this.partnerApprovalHelper.submitPartnerApproval(user, merchant, merchantOnboard, null,
          PartnerApprovalFields.BANK_ACCOUNT);
    }

    if ((account = this.bankDBService.addBankAccount(bAccount)) != null) {
      // if account is requested to be primary reset older primary acct
      if (primaryAccount != null && addReq.getIsPrimaryAccount() == BooleanStatus.YES.value()) {
        primaryAccount.setIsPrimaryAccount(BooleanStatus.NO.value());
        if (this.bankDBService.updateBankAccount(primaryAccount) == null) {
          response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
          throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);
        }
      }

      populateBankAccountResponse(encryptionResource, user, merchant, bank, account, response);
      response.addHeaderCode(HeaderCode.BANK_ACCOUNT_ADDED);

      return ResponseEntity.ok(response);
    }

    response.addHeaderCode(HeaderCode.MISSING_INVALID_INFO);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  /*
   * 
   */
  public ResponseEntity deleteBankAccount(final User user, final Merchant merchant, final Bank bank,
      final String encAcctId, boolean force) throws Exception, APIException {

    boolean merchantRequest = merchant != null ? true : false;
    boolean bankRequest = bank != null ? true : false;

      if (merchantRequest) {
        if (!(this.userDBService.isMerchantAdmin(user)
            || this.userDBService.isMerchantSuperAdmin(user)) || !this.userDBService.isActive(user)) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
              .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
        }
    } else if (bankRequest) {
      if (!(this.userDBService.isBankAdmin(user) || this.userDBService.isBankSuperAdmin(user))
          || !this.userDBService.isActive(user)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
    }

    // Decrypt the acct id
    Integer acctId = Integer.valueOf(encryptionResource.decrypt(user, merchant, bank, encAcctId));

    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get BankAccount")
        .data("BankMerchantId", user.getBankMerchantId()).data("BankAccountNumber", acctId)
        .format());

    BankAccount account = this.bankDBService.getBankAccount(acctId);
    if (account == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(APIResponse.error(HeaderCode.USER_ACCOUNT_DOESNT_EXIST));
    }

    // get merchant/bank all accts, if account to be deleted is primary account
    Future<List<BankAccount>> existingAccountsTask = null;
    if (account.getIsPrimaryAccount() == BooleanStatus.YES.value()) {
      existingAccountsTask =
          merchantRequest ? this.bankDBService.asyncGetMerchantBankAccounts(merchant.getId())
              : this.bankDBService.asyncGetBankAccounts(bank.getId());
    }

    if (merchantRequest) {
      Hibernate.initialize(account.getMerchant());
      if (account.getMerchant().getId() != user.getBankMerchantId()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    } else {
      Hibernate.initialize(account.getBank());
      if (account.getBank().getId() != user.getBankMerchantId()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    }

    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("Deleting merchant account").format());

    // remove account
    account = this.bankDBService.deleteBankAccount(account);
    if (account != null) {
      BankAccountResponse response = new BankAccountResponse();

      if (existingAccountsTask != null) {
        // existing accounts
        List<BankAccount> existingAccounts = existingAccountsTask.get();
        // reset primary account
        if (!CollectionUtils.isEmpty(existingAccounts)) {
          for (BankAccount acct : existingAccounts) {
            if (acct.getId() != account.getId()
                && acct.getStatus() != DBConstants.BankAccountStatus.DELETED.value()) {
              acct.setIsPrimaryAccount(BooleanStatus.YES.value());
              if (this.bankDBService.updateBankAccount(acct) == null) {
                response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
                throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);
              }
              break;
            }
          }
        }
      }
      if (bankRequest) {
        // update status
        this.partnerApprovalHelper.deletePartnerApproval(user, null, bank,
            PartnerApprovalFields.BANK_ACCOUNT);
      } else if (merchantRequest) {
        // update status
        this.partnerApprovalHelper.deletePartnerApproval(user, merchant, null,
            PartnerApprovalFields.BANK_ACCOUNT);
      }
      populateBankAccountResponse(encryptionResource, user, merchant, bank, account, response);

      return ResponseEntity.ok(response);
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
  }

  /*
   * 
   */
  public ResponseEntity getBankAccounts(final User user, final Merchant merchant, final Bank bank,
      Integer status) throws APIException {

    boolean merchantRequest = merchant != null ? true : false;
    boolean bankRequest = bank != null ? true : false;
    boolean brontooRequest =
        (user != null && (this.userDBService.brontooRepresentative(user))) ? true : false;

    if (merchantRequest) {
      if ((!this.userDBService.isMerchantSuperAdmin(user)
          && !this.userDBService.isMerchantAdmin(user))
          && (!this.userDBService.isBrontooSuperAdmin(user)
              && !this.userDBService.isBrontooAdmin(user)
              && !this.userDBService.isBrontooEditor(user))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    } else if (bankRequest) {
      if ((!this.userDBService.isBankSuperAdmin(user) && !this.userDBService.isBankAdmin(user))
          && (!this.userDBService.isBrontooSuperAdmin(user)
              && !this.userDBService.isBrontooAdmin(user)
              && !this.userDBService.isBrontooEditor(user))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    } else if (brontooRequest) {
      if ((!this.userDBService.isBrontooSuperAdmin(user)
          && !this.userDBService.isBrontooAdmin(user))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
    }

    List<BankAccount> accounts = null;

    if (merchantRequest) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get BankAccounts")
          .data("MerchantId", user.getBankMerchantId()).format());
      accounts = this.bankDBService.getMerchantBankAccounts(merchant.getId());
    } else if (bankRequest) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get BankAccounts")
          .data("BankId", user.getBankMerchantId()).format());
      accounts = this.bankDBService.getBankAccounts(bank.getId(),
          Arrays.asList(BankAccountCodes.BANK_COMMISSION.value(),
              BankAccountCodes.BRONTOO_POOL.value(), BankAccountCodes.BANK_BBPS_POOL.value(),
              BankAccountCodes.BANK_FASTAG_DEPOSIT_ACCOUNT.value(),
              BankAccountCodes.BANK_FASTAG_FEE_ACCOUNT.value(), BankAccountCodes.MERCHANT.value()));
    } else {
      accounts =
          this.bankDBService.getBankAccounts(DBConstants.BankAccountCodes.BRONTOO_BANK_CODES);
    }

    if (accounts != null) {

      List<APIResponse> responses = new LinkedList<>();
      for (int i = 0; i < accounts.size(); i++) {
        BankAccount account = accounts.get(i);
        if (status != null && account.getStatus() != status)
          continue;

        BankAccountResponse response = new BankAccountResponse();
        populateBankAccountResponse(encryptionResource, user, merchant, bank, account, response);

        responses.add(response);
      }

      return ResponseEntity.ok(responses);
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
  }

  /*
   * 	
   */
  public static void populateBankAccountResponse(final EncryptionResource encryptionResource,
      final User user, final Merchant merchant, final Bank bank, final BankAccount account,
      BankAccountResponse response) throws APIException {
    if (account != null && user != null) {
      if (response == null)
        response = new BankAccountResponse();

      // Encrypt account id
      String encryptedAcctId =
          encryptionResource.encrypt(user, merchant, bank, String.valueOf(account.getId()));

      response.setAccountNumber(account.getAccountNumber())
          .setAccountHolderName(account.getAccountHolderName())
          .setAccountType(BankAccountCodes.valueByCode(account.getCode())).setIfscCode(account.getIFSCCode())
          .setIsPrimaryAccount(account.getIsPrimaryAccount()).setStatus(account.getStatus())
          .setEncAccountId(encryptedAcctId).setBankAccountType(account.getAccountType());
    }
  }
  

  public ResponseEntity updateBankAccount(final User user, Merchant merchant, final Bank bank,
      String encAccountId, final BankAccountRequest updateReq, String clientTransactionId)
      throws Exception, APIException {
    boolean merchantRequest = merchant != null ? true : false;
    boolean bankRequest = bank != null ? true : false;

    BankAccountResponse response = new BankAccountResponse();

    if (!this.userDBService.isActive(user)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(APIResponse.error(HeaderCode.USER_NOT_ACTIVE));
    }

    if (merchantRequest) {
      if (!(this.userDBService.isMerchantAdmin(user)
          || this.userDBService.isMerchantSuperAdmin(user))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    } else if (bankRequest) {
      if (!(this.userDBService.isBankAdmin(user) || this.userDBService.isBankSuperAdmin(user))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }
    } else {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));

    }

    if (updateReq.getIsPrimaryAccount() == null) {
      response.addHeaderCode(HeaderCode.MISSING_IS_PRIMARY_FIELD);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    if (clientTransactionId == null) {
      response.addHeaderCode(HeaderCode.MISSING_CLIENTTRANSACTIONID);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Decrypt the account Id
    Integer acctId =
        Integer.valueOf(encryptionResource.decrypt(user, merchant, bank, encAccountId));

    if (acctId == null) {
      response.addHeaderCode(HeaderCode.INVALID_ENCRYPTED_DATA);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Get BankAccount")
        .data("BankMerchantId", user.getBankMerchantId()).data("BankAccountNumber", acctId)
        .format());

    // existing account
    BankAccount account = this.bankDBService.getBankAccount(acctId);

    // account doesn't exists
    if (account == null) {
      response.addHeaderCode(HeaderCode.USER_ACCOUNT_DOESNT_EXIST);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // account is not deleted
    if (BankAccountStatus.DELETED.equals(account.getStatus())) {
      response.addHeaderCode(HeaderCode.USER_ACCOUNT_DOESNT_EXIST);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // if primary flag set and account not verified
    if (BooleanStatus.YES.value() == updateReq.getIsPrimaryAccount()
        && !BankAccountStatus.ACTIVE.equals(account.getStatus())) {
      response.addHeaderCode(HeaderCode.USER_ACCOUNT_INACTIVE);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // if primary account flag set and request account is not already
    // primary
    if (updateReq.getIsPrimaryAccount() == BooleanStatus.YES.value()
        && account.getIsPrimaryAccount() != BooleanStatus.YES.value()) {

      BankAccount primary =
          (merchantRequest) ? this.bankDBService.getPrimaryMerchantBankAccount(merchant.getId())
              : this.bankDBService.getPrimaryBankAccount(bank.getId());

      if (primary == null) {
        response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
      }

      primary.setIsPrimaryAccount(BooleanStatus.NO.value());
      if (this.bankDBService.updateBankAccount(primary) == null) {
        LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Failed to reset primary account")
            .data("BankMerchantId", user.getBankMerchantId()).data("AccountId", primary.getId())
            .format());

        throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
            APIResponse.error(HeaderCode.INTERNAL_ERROR));
      }
      account.setIsPrimaryAccount(BooleanStatus.YES.value());
    }
    // update account
    if (this.bankDBService.updateBankAccount(account) != null) {
      response.addHeaderCode(HeaderCode.USER_ACCOUNT_UPDATED);
      return ResponseEntity.ok(response);

    }
    // else throw exception to rollback
    response.addHeaderCode(HeaderCode.INTERNAL_ERROR);
    throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR, response);
  }
  
  public ResponseEntity addVirtualBankAccount(User user, Bank bank, BankAccount account,
      String clientTransactionId) throws APIException, Exception {
    BankAccountResponse response = new BankAccountResponse();
    CustomerAccountRequest addVirtualAccount = new CustomerAccountRequest();
    addVirtualAccount.setBankReferenceId(bank.getReferenceId());
    addVirtualAccount.setMethodType(DBConstants.CustomerAccountMethodType.BANK_ACCOUNTOTP.value());
    addVirtualAccount.setAutoGenerateOsta(false);
    addVirtualAccount.setAccountNumber(account.getAccountNumber());
    addVirtualAccount.setIsPrimaryAccount(1);
    addVirtualAccount.setOstaTTLInHrs(DBConstants.DIPCOIN_MAX_TTL_HRS);
    addVirtualAccount.setMaskedLogin(account.getAccountNumber());// this is for Emailutils.
    addVirtualAccount.setAccountFlag(BooleanStatus.YES.value());

    List<CustomerAccount> existingAccounts =
        this.customerDBService.asyncGetAccounts(user.getId()).get();

    if (!CollectionUtils.isEmpty(existingAccounts)) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Get Existing CustomerAccout").data("User", user.getId()).format());
      // verify if account already added
      for (CustomerAccount customerAccount : existingAccounts) {
        // hashed login match
        if (customerAccount.getStatus() == CustomerAccountStatus.ACTIVE.value()) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Account already exists no need to add another").format());
          // if same request, return success
          return ResponseEntity.ok(response);
        }else if(customerAccount.getStatus() == CustomerAccountStatus.DELETED.value()
            || customerAccount.getStatus() == CustomerAccountStatus.DEACTIVATED.value()) {
          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Account deleted or deactivated").format());
          // if same request, return success
          response.addHeaderCode(HeaderCode.BANK_ACCOUNT_DELETED);
          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .body(APIResponse.error(HeaderCode.BAD_REQUEST));
        }
      }
    }
    // Add customer account for virtual bank user
    ResponseEntity addAcctResponse = this.customerResource.customerAddAccount(user,
        addVirtualAccount, clientTransactionId, true, true, bank, null);
    
    if (HttpStatus.BAD_REQUEST.value() == addAcctResponse.getStatusCodeValue()) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Fail to Add the Account of Customer by bank.").format());
      throw new APIException(HttpStatus.BAD_REQUEST, APIResponse.error(HeaderCode.BAD_REQUEST));
    }

    if (HttpStatus.INTERNAL_SERVER_ERROR.value() <= addAcctResponse.getStatusCodeValue()) {
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Fail to Add the Account of Customer by bank.").format());
      throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
          APIResponse.error(HeaderCode.INTERNAL_ERROR));
    }

    response.addHeaderCode(HeaderCode.BANK_ACCOUNT_ADDED);
    return ResponseEntity.ok(response);
  }
}
