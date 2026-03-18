/**
 *
 */
package com.dipcoin.api.resource;

import java.text.MessageFormat;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
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
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.CreditRequest;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.client.BankClient.Operation;
import com.dipcoin.bank.services.comm.CustomerFundTransferRequest;
import com.dipcoin.bank.services.comm.CustomerFundTransferResponse;
import com.dipcoin.bank.services.comm.MarkLienRequest;
import com.dipcoin.bank.services.comm.MarkLienResponse;
import com.dipcoin.bank.services.utils.BankConstants.BankResponseStatus;
import com.dipcoin.bank.services.utils.BankConstants.Comment;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankUtils;
import com.dipcoin.commons.Constants.Currency;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.commons.DBConstants.BankAccountCodes;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import com.dipcoin.db.services.dao.BankAccountDao;
import com.dipcoin.db.services.dao.BankTransactionDao;
import com.dipcoin.db.services.dao.DipcoinDao;
import com.dipcoin.db.services.dao.DipcoinTransactionDao;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankAccount;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.User;
import com.dipcoin.metrics.DipcoinMetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
@Component("dipcoinBankHelper")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class DipcoinBankHelper {

  @Autowired
  @Qualifier("com.dipcoin.metrics.DipcoinMetricRegistry")
  private DipcoinMetricRegistry dipcoinMetricRegistry;

  @Autowired
  private BankUtils bankUtils;

  @Autowired
  private BankAPIServices bankAPIServices;

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  private BankTransactionDao bankTransactionDao;
  
  @Autowired
  private CustomerDBService customerDBService;
  
  @Autowired
  private BankAccountDao bankAccountDao;
  
  
  @Autowired
  @Lazy
  protected HttpServletContext httpServletContext;

  // *********** ALL METHODS USED FOR CBI ************************
  public void setHttpServletContext(HttpServletContext httpServletContext) {
    this.httpServletContext = httpServletContext;
  }

  private static final Logger LOG = LogManager.getLogger(DipcoinBankHelper.class);
  private final static ObjectMapper objectMapper = new ObjectMapper();

	  public ResponseEntity markLien(final User user, final CustomerAccount originDcoinCustomerAccount,
		      final Dipcoin lienMarkedDcoin, final BankTransaction lienMarkedBTx,
		      BankRequestContext bankRequestContext) throws APIException {

		    if (lienMarkedDcoin.getUsageType() == DipcoinUsageType.TOLL.value()) {

		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .message("Lien mark initiated for hold doesnot exist").format());

		      String dipcoinReferenceNumber = bankUtils.generateDipcoinToBankReferenceNumber(
		          originDcoinCustomerAccount.getBank().getReferenceId(), Operation.MARK_LIEN,
		          BankTransactionType.LIEN_MARK.value());

		      MarkLienRequest bRequestMark =
		          new MarkLienRequest(originDcoinCustomerAccount.getBank().getReferenceId(),
		              originDcoinCustomerAccount.getBank().getCode());
		      bRequestMark.setAmount(String.valueOf(lienMarkedBTx.getAmount()));
		      bRequestMark.setTransactionType(Integer.toString(BankTransactionType.LIEN_MARK.value()));
		      bRequestMark.setBankUID(originDcoinCustomerAccount.getBankUId());
		      bRequestMark.setUserId(String.valueOf(user.getId()));
		      bRequestMark.setCardId(String.valueOf(originDcoinCustomerAccount.getUserCardId()));
		      bRequestMark.setAccountId(String.valueOf(originDcoinCustomerAccount.getId()));

		      bRequestMark.setDipcoinReferenceNumber(dipcoinReferenceNumber);
		      bRequestMark.setCurrency(Currency.INDIA.value());
		      bRequestMark.setComment(Comment.LIENMARK.value());

		      try {
		        MarkLienResponse markLienResponse =
		            this.bankAPIServices.markLien(bankRequestContext, bRequestMark).get();

		        if (markLienResponse != null
		            && BankResponseStatus.SUCCESS.code().equals(markLienResponse.getBankResponseCode())
		            && dipcoinReferenceNumber.equals(markLienResponse.getDipcoinReferenceNumber())) {
		          lienMarkedBTx.setCBSLienDate(markLienResponse.getCbsDate());
		          lienMarkedBTx.setCBSReferenceID(markLienResponse.getCbsJournalNumber());
		          lienMarkedBTx.setRawBankRequest(markLienResponse.getRawRequest());
		          lienMarkedBTx
		              .setBankTransactionRefId(markLienResponse.getBankTransactionReferenceNumber());
		          lienMarkedBTx.setRawBankResponse(markLienResponse.getRawData());
		          lienMarkedBTx.setBankResponseCode(markLienResponse.getBankResponseCode());
		          lienMarkedBTx.setStatus(BankTransactionsStatus.SUCCESS.value());
		          lienMarkedBTx.setRequestTime(markLienResponse.getRequestTime());
		          lienMarkedBTx.setResponseTime(markLienResponse.getResponseTime());
		          lienMarkedBTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);

		          if (bankDBService.updateTransaction(lienMarkedBTx) == null) {
		            LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		                .message("Bank updation fail").format());
		          }
		          return ResponseEntity.status(HttpStatus.OK)
		              .body(APIResponse.error(HeaderCode.BANK_HOLD_MARK_FOR_HOLD_DOES_NOT_EXIT));

		        }

		      } catch (Exception e) {
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("exception caught")
		            .format(), e);
		      }

		    }

		    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		        .body(APIResponse.error(HeaderCode.BAD_REQUEST));
		  }

	  public void creditAmount(CreditRequest creditRequest, String traceId) throws Exception{

		    HttpServletContext httpServletContext = HttpServletContext.instance();
		    httpServletContext.setTraceId(traceId); 
		    int customerAccountId = creditRequest.getCustomerAccountId();
		    CustomerAccount customerAccount = customerDBService.getAccountById(customerAccountId);
		    if(Hibernate.isInitialized(customerAccount.getBank())) {
		      Hibernate.initialize(customerAccount.getBank());
		    }
		    Bank bank = bankDBService.getBank(customerAccount.getBank().getId());
		    
		    if(bank == null) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Bank is null")
		          .format());
		      return;
		    }
		    
		    // get Brontoo Pool Account for bank under consideration
		    List<BankAccount> brontooPoolAccounts =
		        bankAccountDao.findBankAccounts(bank.getId(), BankAccountCodes.BRONTOO_POOL.value());
		    if (CollectionUtils.isEmpty(brontooPoolAccounts)) {
		      return;
		    }
		    BankAccount brontooPoolAccount = brontooPoolAccounts.get(0);

		    CustomerFundTransferRequest bRequest =
		        new CustomerFundTransferRequest(bank.getReferenceId(), bank.getCode());
		    
		    bRequest.setAmount(creditRequest.getAmount().toString());
		    bRequest.setBankUID(customerAccount.getBankUId());
		    bRequest.setAccount(brontooPoolAccount.getAccountNumber());
		    bRequest.setTransactionType(Integer.toString(BankTransactionType.CREDIT_TO_ACCOUNT.value()));
		    String dipcoinReferenceNumber =
		        bankUtils.generateDipcoinToBankReferenceNumber(bank.getReferenceId(),
		            bRequest.getOperation(), BankTransactionType.CREDIT_TO_ACCOUNT.value());
		    bRequest.setDipcoinReferenceNumber(dipcoinReferenceNumber);
		    bRequest.setCurrency(Currency.INDIA.value());
		    bRequest.setComment(MessageFormat.format(Comment.CREDIT.value(), creditRequest.getUsedAt()));
		    bRequest.setCreditNarration("Toll Chrg Ac "+customerAccount.getBankUId());
		    bRequest.setDebitNarration(creditRequest.getUsedAt());

		    BankRequestContext bankRequestContext = new BankRequestContext();
		    bankRequestContext.setTraceId(traceId);
		    LOG.debug(LogFormatter.instance(traceId).data("BankRequest", bRequest).format());

		    CustomerFundTransferResponse bResponse =
		        this.bankAPIServices.customerFundTransfer(bankRequestContext, bRequest).get();
		    LOG.debug(LogFormatter.instance(traceId).data("BankResponse", bResponse)
		        .data("DipcoinReferenceNumber", dipcoinReferenceNumber).format());

		    // store parent dipcoin btx/dtx
		    BankTransaction nBTx = new BankTransaction();
		    // nBTx.setDipcoinId(dcoin.getId());
		    nBTx.setAmount(creditRequest.getAmount());
		    nBTx.setType(BankTransactionType.CREDIT_TO_ACCOUNT.value());
		    nBTx.setDipcoinTransactionRefId(dipcoinReferenceNumber);
		    nBTx.setCustomerAccountId(customerAccount.getId());
		    nBTx.setBankId(bank.getId());

		    // @TODO - system failure
		    if (bResponse == null
		        || !BankResponseStatus.SUCCESS.code().equals(bResponse.getBankResponseCode())
		        || !dipcoinReferenceNumber.equals(bResponse.getDipcoinReferenceNumber())) {
		      LOG.error(LogFormatter.instance(traceId).message("Failed to debit credit").format());
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("1rs Credit failed")
		          .format());
		      nBTx.setStatus(BankTransactionsStatus.FAILED.value());

		      if (bResponse != null) {
		        nBTx.setRequestTime(bResponse.getRequestTime());
		        nBTx.setResponseTime(bResponse.getResponseTime());
		        nBTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
		        nBTx.setRawBankResponse(
		            bResponse.getBankResponseDesc() != null ? bResponse.getBankResponseDesc()
		                : bResponse.getErrorMsg());
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		            .message("1rs Credit Successfull").format());
		      }
		      LOG.debug(LogFormatter.instance(traceId).data("BankTransaction", nBTx).format());
		      if (bankTransactionDao.create(nBTx) == null) {
		        LOG.error(LogFormatter.instance(traceId).message("Failed to add Bank Transaction")
		            .data("transaction", nBTx).format());
		      }
		      throw new Exception("Failed to add Credit to Account");
		    }
		    nBTx.setRawBankRequest(bResponse.getRawRequest());
		    nBTx.setBankTransactionRefId(bResponse.getBankTransactionReferenceNumber());
		    nBTx.setRawBankResponse(bResponse.getRawData());
		    nBTx.setStatus(BankTransactionsStatus.SUCCESS.value());
		    nBTx.setRequestTime(bResponse.getRequestTime());
		    nBTx.setResponseTime(bResponse.getResponseTime());
		    this.bankDBService.updateTransaction(nBTx);


		    return;

		  }
}
