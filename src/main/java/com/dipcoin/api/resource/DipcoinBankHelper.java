/**
 *
 */
package com.dipcoin.api.resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.bank.services.BankAPIServices;
import com.dipcoin.bank.services.client.BankClient.Operation;
import com.dipcoin.bank.services.comm.MarkLienRequest;
import com.dipcoin.bank.services.comm.MarkLienResponse;
import com.dipcoin.bank.services.utils.BankConstants.BankResponseStatus;
import com.dipcoin.bank.services.utils.BankConstants.Comment;
import com.dipcoin.bank.services.utils.BankRequestContext;
import com.dipcoin.bank.services.utils.BankUtils;
import com.dipcoin.commons.Constants.Currency;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionType;
import com.dipcoin.db.services.commons.DBConstants.BankTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinUsageType;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.User;
import com.dipcoin.metrics.DipcoinMetricRegistry;
//import com.dipcoin.partner.toll.model.TollReqPayRequest;
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

}
