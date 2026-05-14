package com.dipcoin.api.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.ChargeBackList;
import com.dipcoin.api.model.ChargebackDocResponse;
import com.dipcoin.api.model.ChargebackResponse;
import com.dipcoin.api.model.Pagination;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.ChargebackDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Chargeback;
import com.dipcoin.db.services.model.ChargebackDoc;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;

@Component("chargebackResource")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class ChargebackResource {

  private static final Logger LOG = LogManager.getLogger(ChargebackResource.class);

  @Autowired
  private DipcoinDBService coinDBService;

  @Autowired
  private MerchantDBService merchantDBService;

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  private ChargebackDBService chargebackDBService;
  
  @Autowired
  private MerchantResource merchantResource;

  @Autowired
  private CustomerResource customerResource;

  @Autowired
  private BrontooResource brontooResource;

  @Autowired
  private CustomerDBService customerDBService;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

  @Autowired
  private BankResource bankResource;


  private static final String MERCHANT_DOC_PATH = "Merchant" + "/" + "chargeBack";

  private static final String CUSTOMER_DOC_PATH = "Customer" + "/" + "chargeBack";

  public static final String LOCAL_UPLOAD_LOCATION_FOLDER = "C:/tmp/";

  /*
   * Get Initaited Chargeback transaction merchnat/customer/admin
   */
  public ResponseEntity getChargebackTransaction(final User user, String partnerRefId,
      Integer status, Long startDate, Long endDate, String partner, Integer start, Integer count)
      throws InterruptedException, ExecutionException {

    ChargebackResponse chargebackResponse = new ChargebackResponse();

    List<Chargeback> chargebackTransaction = new ArrayList<Chargeback>();
    List<ChargebackResponse> chargebackResponseList = new ArrayList<ChargebackResponse>();

    if (user == null) {
      LOG.error(Status.BAD_REQUEST);
      chargebackResponse.addHeaderCode(HeaderCode.INVALID_REQUEST);

      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(chargebackResponse);
    }

    boolean merchantRequest = APIConstants.MERCHANT.equals(partner.toUpperCase());
    boolean customerRequest = APIConstants.CUSTOMER.equals(partner.toUpperCase());
    boolean dipcoinRequest = APIConstants.BRONTOO.equals(partner.toUpperCase());
    boolean bankRequest = APIConstants.BANK.equals(partner.toUpperCase());

    ChargeBackList chargebackList = new ChargeBackList();

    if (merchantRequest) {

      ResponseEntity response = this.merchantResource.getChargeBackTransactions(user, partnerRefId,
          status, startDate, endDate, partner, start, count);

      chargebackResponse = (ChargebackResponse) response.getBody();
      chargebackTransaction = chargebackResponse.getChargebackTransaction();

    }

    if (bankRequest) {

      ResponseEntity response = this.bankResource.getChargeBackTransactions(user, partnerRefId,
          status, startDate, endDate, partner, start, count);

      chargebackResponse = (ChargebackResponse) response.getBody();
      chargebackTransaction = chargebackResponse.getChargebackTransaction();
    }

    if (customerRequest) {
      ResponseEntity response = customerResource.getChargeBackTransactions(user, partnerRefId,
          status, startDate, endDate, partner, start, count);
      chargebackResponseList = (List<ChargebackResponse>) response.getBody();
    }

    if (dipcoinRequest) {

      ResponseEntity response = this.brontooResource.getChargeBackTransactions(user, partnerRefId,
          status, startDate, endDate, partner, start, count);

      chargebackResponse = (ChargebackResponse) response.getBody();
      chargebackTransaction = chargebackResponse.getChargebackTransaction();
    }

    if (chargebackTransaction != null && chargebackTransaction.size() != 0) {
      LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
          .data("ChargebackTransactions count", chargebackTransaction.size()).format());

      chargebackResponseList = fetchDipcoinTransactionData(chargebackTransaction);

    }

    if (!CollectionUtils.isEmpty(chargebackResponseList)) {
      Pagination pagination = new Pagination();
      pagination.setScanCompleted(chargebackResponseList.size() < count);
      pagination.setTotal(chargebackResponseList.size());
      if (!pagination.getScanCompleted() && start != null && count != null) {
        pagination.setStart(start + count);

      }

      chargebackList.setChargebackResponse(chargebackResponseList);
      chargebackList.setPagination(pagination);


      return ResponseEntity.ok(chargebackList);

    }

    chargebackResponse.addHeaderCode(HeaderCode.EMPTY_REQUEST);

    return ResponseEntity.ok(chargebackResponse);

  }
  
  private List<ChargebackResponse> fetchDipcoinTransactionData(
	      List<Chargeback> chargeBackTransaction) throws InterruptedException, ExecutionException {

	    List<ChargebackResponse> chargebackResponseList = new ArrayList<ChargebackResponse>();

	    List<DipcoinTransaction> dipcoinTransaction = new ArrayList<DipcoinTransaction>();

	    for (int i = 0; i <= chargeBackTransaction.size() - 1; i++) {
	      List<Integer> dipcoinTxnId = new ArrayList<Integer>();
	      dipcoinTxnId.add(chargeBackTransaction.get(i).getDipcoinTransactionId());

	      dipcoinTransaction = coinDBService.asyncGetTransactionsByIds(dipcoinTxnId, null, null).get();

	      Merchant merchant = null;
	      if (chargeBackTransaction.get(i).getMerchantId() != null) {
	        merchant = this.merchantDBService.getMerchant(chargeBackTransaction.get(i).getMerchantId());
	      }

	      Bank bank = null;
	      if (chargeBackTransaction.get(i).getBankId() != null) {
	        bank = this.bankDBService.getBank(chargeBackTransaction.get(i).getBankId());
	      }

	      ChargebackResponse chargebackResponse = new ChargebackResponse();
	      chargebackResponse.setChargebackTxnId(chargeBackTransaction.get(i).getId());
	      chargebackResponse.setAmount(chargeBackTransaction.get(i).getAmount().toString());
	      chargebackResponse.setProvedByMerchant(chargeBackTransaction.get(i).getProvedByMerchant());
	      chargebackResponse.setTime(chargeBackTransaction.get(i).getRequestTime());
	      chargebackResponse.setStatus(chargeBackTransaction.get(i).getStatus());
	      chargebackResponse
	          .setGracePeriodEndDate(chargeBackTransaction.get(i).getGracePeriodEndDate());
	      chargebackResponse.setMerchantName(merchant != null ? merchant.getName() : "");
	      chargebackResponse.setBankName(bank != null ? bank.getName() : "");

	      if (!chargeBackTransaction.get(i).getChargeBackTransactionDoc().isEmpty()) {

	        List<ChargebackDocResponse> chargebackDocResponse = new ArrayList<ChargebackDocResponse>();

	        for (int j = 0; j <= chargeBackTransaction.get(i).getChargeBackTransactionDoc().size()
	            - 1; j++) {

	          chargebackDocResponse.add(populateChargebackDocResponse(
	              chargeBackTransaction.get(i).getChargeBackTransactionDoc().get(j)));
	        }
	        chargebackResponse.setCommentsAndDocPaths(chargebackDocResponse);
	      }

	      chargebackResponse.setPartnerReferenceId(dipcoinTransaction.get(0).getPartnerReferenceId());
	      chargebackResponse.setPartnerTransactionReferenceId(
	          dipcoinTransaction.get(0).getPartnerTransactionReferenceId());
	      chargebackResponse
	          .setOstaTransactionReferenceId(dipcoinTransaction.get(0).getDipcoinTransactionRefId());
	      chargebackResponse.setOrderId(dipcoinTransaction.get(0).getOrderId());
	      chargebackResponseList.add(chargebackResponse);

	    }

	    return chargebackResponseList;

	  }
  
  private ChargebackDocResponse populateChargebackDocResponse(ChargebackDoc chargebackTxnDoc) {
	    ChargebackDocResponse chargebackDocResponse = new ChargebackDocResponse();
	    chargebackDocResponse.setComment(chargebackTxnDoc.getComment());
	    chargebackDocResponse.setDocumentPath(chargebackTxnDoc.getDocumentPath());
	    // chargebackDocResponse.setUpdatedBy(chargebackTxnDoc.getUpdatedBy());
	    chargebackDocResponse.setRole(chargebackTxnDoc.getUser().getRole());
	    chargebackDocResponse.setUpdatedDate(chargebackTxnDoc.getUpdatedDate());

	    return chargebackDocResponse;

	  }
  
//Fetch ChargeBackTranction
 public List<Chargeback> fetchChargebackTransaction(Integer merchnatId, Integer bankId,
     Integer status, Long startDate, Long endDate, Integer start, Integer count) {

   List<Chargeback> chargebackTranction = chargebackDBService.getChargeback(merchnatId, bankId,
       status, startDate, endDate, start, count, false);

   return chargebackTranction;

 }
 
 public List<ChargebackResponse> fetchChargeBackTransactionData(
	      List<DipcoinTransaction> dipcoinTransactions) {

	    List<ChargebackResponse> chargebackResponseList = new ArrayList<ChargebackResponse>();

	    for (DipcoinTransaction dipcoinTransaction : dipcoinTransactions) {
	      // Chargeback chargebackTransaction = new Chargeback();
	      
	      Dipcoin dipcoin = coinDBService.getById(dipcoinTransaction.getDipcoinId(), true);
	      
	      if(dipcoin == null) {
	        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	            .message("dipcoin not found").format());
	        continue;
	      }
	      
	      if( DipcoinUsageType.TOLL.value() == dipcoin.getUsageType()) {
	        chargebackResponseList = popullateTollChargeback(dipcoinTransaction, chargebackResponseList, dipcoin);
	        continue;
	      }
	      
	      List<Chargeback> chargebackTransactions =
	          chargebackDBService.getChargebackByDipcoinTxnId(dipcoinTransaction.getId());
	      if (!CollectionUtils.isEmpty(chargebackTransactions)) {

	        ChargebackResponse chargebackResponse = new ChargebackResponse();

	        CustomerAccount customerAccount =
	            this.customerDBService.getAccountById(dipcoinTransaction.getCustomerAccountId());
	        if (customerAccount != null) {
	          chargebackResponse.setAccountNumber(customerAccount.getAccountNumber());
	          chargebackResponse.setBankName(customerAccount.getBank().getName());
	        }
	        for (Chargeback chargebackTransaction : chargebackTransactions) {

	          Merchant merchant = null;
	          if (chargebackTransaction.getMerchantId() != null) {
	            merchant = this.merchantDBService.getMerchant(chargebackTransaction.getMerchantId());
	          }

	          Bank bank = null;
	          if (chargebackTransaction.getBankId() != null) {
	            bank = this.bankDBService.getBank(chargebackTransaction.getBankId());
	          }

	          chargebackResponse.setChargebackTxnId(chargebackTransaction.getId());
	          chargebackResponse.setAmount(chargebackTransaction.getAmount().toString());
	          chargebackResponse.setProvedByMerchant(chargebackTransaction.getProvedByMerchant());
	          chargebackResponse.setTime(chargebackTransaction.getRequestTime());
	          
	          
	          
	          chargebackResponse.setStatus(chargebackTransaction.getStatus());
	          chargebackResponse.setGracePeriodEndDate(chargebackTransaction.getGracePeriodEndDate());
	          chargebackResponse.setPartnerReferenceId(dipcoinTransaction.getPartnerReferenceId());
	          chargebackResponse.setPartnerTransactionReferenceId(
	              dipcoinTransaction.getPartnerTransactionReferenceId());
	          chargebackResponse
	              .setOstaTransactionReferenceId(dipcoinTransaction.getDipcoinTransactionRefId());
	          chargebackResponse.setOrderId(dipcoinTransaction.getOrderId());
	          chargebackResponse.setMerchantName(merchant != null ? merchant.getName() : "");
	          chargebackResponse.setBankName(bank != null ? bank.getName() : "");

	          if (!chargebackTransaction.getChargeBackTransactionDoc().isEmpty()) {

	            List<ChargebackDocResponse> chargebackDocResponse =
	                new ArrayList<ChargebackDocResponse>();

	            for (ChargebackDoc chargebackDoc : chargebackTransaction
	                .getChargeBackTransactionDoc()) {

	              chargebackDocResponse.add(populateChargebackDocResponse(chargebackDoc));
	            }
	            chargebackResponse.setCommentsAndDocPaths(chargebackDocResponse);
	          }

	          chargebackResponseList.add(chargebackResponse);
	        }
	      }
	    }
	    return chargebackResponseList;
	  }
 
 public List<ChargebackResponse> popullateTollChargeback(
	      DipcoinTransaction dipcoinTransaction,List<ChargebackResponse> chargebackResponseList, Dipcoin dipcoin) {
	    
	    
	    List<Chargeback> chargebackTransactions =
	        chargebackDBService.getChargebackByDipcoinTxnId(dipcoinTransaction.getId());
	    if (!CollectionUtils.isEmpty(chargebackTransactions)) {     
	      
	      for (Chargeback chargebackTransaction : chargebackTransactions) {
	        
	        if (!chargebackTransaction.getChargeBackTransactionDoc().isEmpty()) {

	          for (ChargebackDoc chargebackDoc : chargebackTransaction
	              .getChargeBackTransactionDoc()) {
	            ChargebackResponse chargebackResponse = new ChargebackResponse();
	            List<ChargebackDocResponse> chargebackDocResponse =
	                new ArrayList<ChargebackDocResponse>();
	            
	            chargebackResponse.setAccountNumber(dipcoin.getCustomerAccount().getAccountNumber());
	            chargebackResponse.setBankName(dipcoin.getCustomerAccount().getBank().getName());
	            
	            chargebackResponse.setChargebackTxnId(chargebackTransaction.getId());
	            chargebackResponse.setAmount(chargebackTransaction.getAmount().toString());
	            chargebackResponse.setProvedByMerchant(chargebackDoc.getAction());
	            chargebackResponse.setTime(chargebackTransaction.getRequestTime());
	            
	            chargebackResponse.setStatus(chargebackDoc.getCode());
	            chargebackResponse.setGracePeriodEndDate(chargebackTransaction.getGracePeriodEndDate());
	            chargebackResponse.setPartnerReferenceId(dipcoinTransaction.getPartnerReferenceId());
	            chargebackResponse.setPartnerTransactionReferenceId(
	                dipcoinTransaction.getPartnerTransactionReferenceId());
	            chargebackResponse
	                .setOstaTransactionReferenceId(dipcoinTransaction.getDipcoinTransactionRefId());
	            chargebackResponse.setOrderId(dipcoinTransaction.getOrderId());
	            
	            chargebackDocResponse.add(populateChargebackDocResponse(chargebackDoc));
	            chargebackResponse.setCommentsAndDocPaths(chargebackDocResponse);
	                        
	            chargebackResponseList.add(chargebackResponse);
	          }
	        }
	        
	      }     
	    }    
	    return chargebackResponseList;
	  }

	  
}
