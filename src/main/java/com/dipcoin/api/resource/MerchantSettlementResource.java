package com.dipcoin.api.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.EmailUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.document.PdfUtils;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.MerchantTransactionsResponse;
//import com.dipcoin.api.model.MerchantReportsResponse;
//import com.dipcoin.api.model.MerchantTransactionsResponse;
import com.dipcoin.api.model.Pagination;
import com.dipcoin.api.model.SettlementsResponse;
import com.dipcoin.api.model.SettlementsResponse.Transactions;
//import com.dipcoin.api.model.SettlementsResponse;
//import com.dipcoin.api.model.SettlementsResponse.Transactions;
//import com.dipcoin.api.model.TransactionReportResponse;
import com.dipcoin.commons.Constants;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.CustomerDBService;
import com.dipcoin.db.services.DipcoinAccountTransactionDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.SettlementDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.AggregateSnapshot;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.BooleanStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinAccountTransactionType;
import com.dipcoin.db.services.commons.DBConstants.DipcoinStatus;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionType;
import com.dipcoin.db.services.commons.DBConstants.DipcoinTransactionsStatus;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.commons.DBConstants.ReportType;
import com.dipcoin.db.services.commons.DBConstants.SettlementStatus;
import com.dipcoin.db.services.commons.DBConstants.SettlementType;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankAccount;
import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinAccountTransaction;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.MerchantMI;
import com.dipcoin.db.services.model.MerchantRecon;
import com.dipcoin.db.services.model.Settlement;
import com.dipcoin.db.services.model.User;
//import com.dipcoin.metrics.MerchantMetricRegistry;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.db.services.PartnerAccountDBService;
import com.dipcoin.partner.db.services.PartnerAccountTransactionDBService;
//import com.dipcoin.partner.db.services.PartnerAccountTransactionDBService;
import com.dipcoin.partner.db.services.PartnerEntityDBService;
import com.dipcoin.partner.db.services.PartnerSettlementDBService;
//import com.dipcoin.partner.db.services.PartnerSettlementDBService;
import com.dipcoin.partner.db.services.PartnerTransactionDBService;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerAccountTransactionType;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerBankAccountCodes;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerSettlementStatus;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerTransactionRequestType;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerTransactionStatus;
import com.dipcoin.partner.db.services.commons.DBConstants.PartnerTransactionType;
import com.dipcoin.partner.db.services.model.Partner;
import com.dipcoin.partner.db.services.model.PartnerAccount;
import com.dipcoin.partner.db.services.model.PartnerAccountTransaction;
import com.dipcoin.partner.db.services.model.PartnerRecon;
import com.dipcoin.partner.db.services.model.PartnerSettlement;
import com.dipcoin.partner.db.services.model.PartnerTransaction;
import com.dipcoin.partner.paymentGateway.model.AggrepayPaymentStatusResponse;
//import com.dipcoin.partner.paymentGateway.PartnerInternalServices;
//import com.dipcoin.partner.paymentGateway.model.AggrepayPaymentStatusResponse;
//import com.dipcoin.partner.paymentGateway.model.PartnerRefundRequest;
//import com.dipcoin.partner.paymentGateway.model.PartnerRefundResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 */
@Component("merchantSettlementResource")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class MerchantSettlementResource {

  private static final Logger LOG = LogManager.getLogger(MerchantSettlementResource.class);
  private static final String zero = "0.00";
  private static final DateFormat FORMATTER = new SimpleDateFormat("ddMMyyyy");
  
  @Autowired
  private UserDBService userDBService;

  @Autowired
  private MerchantDBService merchantDBService;

  @Autowired
  private SettlementDBService settlementDBService;
  
  @Autowired
  private PartnerEntityDBService PartnerEntityDBService;
  
  @Autowired
  private PartnerSettlementDBService partnerSettlementDBService;

  @Autowired
  private PartnerAccountDBService partnerAccountDBService;
  
  @Autowired
  private DipcoinDBService coinDBService;

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  private EmailUtils emailUtils;

  @Autowired
  private SmsClient smsClient;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

  @Autowired
  private  DipcoinAccountTransactionDBService dipcoinAccountTransactionDBService;
  
  @Autowired
  private  PartnerAccountTransactionDBService partnerAccountTransactionDBService;
 
  @Autowired
  private PdfUtils pdfUtils;
  
  @Autowired
  private MerchantSettlementResource merchantSettlementResource;
  
  @Autowired
  private PartnerTransactionDBService partnerTransactionDBService;
  
  @Autowired
  private PartnerEntityDBService partnerEntityDBService;
  
  @Autowired
  private CustomerDBService customerDBService;
  
//  @Autowired
//  private PartnerInternalServices partnerInternalServices;
  
  
//  @Autowired
//  @Qualifier("com.dipcoin.metrics.MerchantMetricRegistry")
//  private MerchantMetricRegistry merchantMetricRegistry;
  
  @Autowired
  private NotificationResource notificationResource;

  @Autowired
  private ApplicationProperties applicationProperties;

  /*
   * Get Reports Aggregate
   */
  public ResponseEntity getReportsAggregate(final User user, final Integer merchantId,
      Long startTime, Long endTime) throws Exception {
	  
	  Merchant merchant = merchantDBService.getMerchant(merchantId);

    if (user != null && merchant != null) {
      if (!(this.userDBService.isMerchantSuperAdmin(user)
          || this.userDBService.isMerchantAdmin(user)
          || this.userDBService.isMerchantInternalUser(user)
          || this.userDBService.brontooRepresentative(user))) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
      }

      MerchantTransactionsResponse response = new MerchantTransactionsResponse();
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("startTime", startTime)
          .data("endTime", endTime).format());
      AggregateSnapshot snapshot = this.merchantDBService.getAggregatedReport(merchant, startTime, endTime);

   // add snapshot details
      response.setSnapshot(snapshot);
      return ResponseEntity.ok(response);
    }

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
  }
  
  
  /*
   * Get Merchant Transactions & partnerTransaction
   */
  public ResponseEntity getTransactions(final User user, final Integer merchantId, final List<Integer> statuses,
  		Integer type, Long startTime, Long endTime, Integer start, Integer count, Boolean isCommission,
  		String partnerTransactionReferenceId) throws Exception {

	  Merchant merchant = merchantDBService.getMerchant(merchantId);
  	if (user != null && merchant != null) {
  		if (!this.userDBService.isMerchantSuperAdmin(user) && !this.userDBService.isMerchantAdmin(user)
  				&& !this.userDBService.isMerchantInternalUser(user)
  				&& !this.userDBService.brontooRepresentative(user)) {
  			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
  		}

  		// Overiding the start and count value and setting startCountOfList and
  		// endCountOfList
  		Integer startCountOfList = start;
  		Integer endCountOfList = start + count;

  		// overiding the count and start to fetch and sort the list
  		count = start + count; // Eg: count = start(50) + count(50) = total 100 transaction from DTxn
  								// and PTXn.
  		start = 0; // This because we have to fetch from 0th transaction

  		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("merchant", merchant.getReferenceId())
  				.data("startTime", startTime).data("endTime", endTime).data("start", start).data("count", count)
  				.data("type", type).format());

  		List<Integer> types = type == null ? Arrays.asList(DipcoinTransactionType.COMPLETELY_USED.value(),
  				DipcoinTransactionType.PARTIALLY_USED.value(), DipcoinTransactionType.CANCELLED_BY_MERCHANT.value(),
  				DipcoinTransactionType.PARTIALLY_CANCELLED_BY_MERCHANT.value(),
  				DipcoinTransactionType.CANCELLED_BY_USER.value(), DipcoinTransactionType.CHARGE_BACK.value())
  				: Arrays.asList(type);

  		List<Integer> requestTypes = Arrays.asList(PartnerTransactionRequestType.REPAYMENT.value(),
  				PartnerTransactionRequestType.PAYMENT_GATEWAY_TRANSACTION.value(),
  				PartnerTransactionRequestType.WALLET_TOPUP.value(), PartnerTransactionRequestType.CREATEOSTA.value(),
  				PartnerTransactionRequestType.COLLECT_MONEY.value());

  		List<Integer> pTxStatuses = Arrays.asList(PartnerTransactionStatus.SUCCESS.value(),
  				PartnerTransactionStatus.FAILURE.value(), PartnerTransactionStatus.PENDING.value(),
  				PartnerTransactionStatus.STATUS_NA.value(), PartnerTransactionStatus.INCOMPLETE.value());

  		List<Integer> pTxnTypes = Arrays.asList(PartnerTransactionType.PAYMENT_GATEWAY_TRANSACTION.value());

  		List<PartnerTransaction> pTxns = partnerTransactionDBService.findByRanges(pTxnTypes, requestTypes, pTxStatuses,
  				startTime, endTime, start, count, merchant.getReferenceId());

  		List<PartnerTransaction> ptxns = partnerTransactionDBService.findByRanges(
  				Arrays.asList(PartnerTransactionType.FETCH_PG_OPTIONS.value()),
  				Arrays.asList(PartnerTransactionRequestType.FETCH_PG_OPTIONS.value()),
  				Arrays.asList(PartnerTransactionStatus.STATUS_NA.value()), startTime, endTime, start, count,
  				merchant.getReferenceId());

  		if (!CollectionUtils.isEmpty(pTxns)) {
  			if (!CollectionUtils.isEmpty(ptxns)) {
  				for (PartnerTransaction ptxn : ptxns) {
  					pTxns.add(ptxn);
  				}
  			}
  		} else {
  			pTxns = ptxns;
  		}

  		MerchantTransactionsResponse response = new MerchantTransactionsResponse();
  		List<MerchantTransactionsResponse> transactions = new ArrayList<>();
  		List<MerchantTransactionsResponse> filteredTransactions = new ArrayList<>();

  		if (partnerTransactionReferenceId != null) {
  			List<DipcoinTransaction> dtxnsByPartnerTxnRefId = this.coinDBService
  					.getMerchantTransactionsByPartnerTxnRefId(partnerTransactionReferenceId);

  			if (!CollectionUtils.isEmpty(dtxnsByPartnerTxnRefId)) {
  				for (DipcoinTransaction dtxnByPartnerTxnRefId : dtxnsByPartnerTxnRefId) {
  					MerchantTransactionsResponse info = new MerchantTransactionsResponse();
  					populateMerchantTransactionInfo(dtxnByPartnerTxnRefId, info);
  					transactions.add(info);
  				}

  			}
  		} else {
  			List<DipcoinTransaction> dTxns = this.coinDBService.getMerchantTransactions(merchant, statuses, types,
  					startTime, endTime, start, count);

  			if (!CollectionUtils.isEmpty(dTxns)) {
  				for (DipcoinTransaction dTxn : dTxns) {
  					MerchantTransactionsResponse info = new MerchantTransactionsResponse();

  					populateMerchantTransactionInfo(dTxn, info);

  					// Fetch all the data from DipcoinAccountTransaction with the help of
  					// dipcoinTrnxId
  					List<DipcoinAccountTransaction> dipcoinAccountTranx = this.dipcoinAccountTransactionDBService
  							.getByDipcoinTransactionId(dTxn.getId());
  					if (!CollectionUtils.isEmpty(dipcoinAccountTranx)) {
  						Settlement settlement = settlementDBService.findById(
  								dipcoinAccountTranx.get(0).getSettlementId(),
  								SettlementType.BRONTOO_PRINCIPAL_TO_MERCHANT_ACCOUNT.value());
  						if (settlement != null) {
  							if (SettlementStatus.FAILED.value() == settlement.getStatus()
  									|| SettlementStatus.SUCCESS.value() == settlement.getStatus()) {
  								info.setSettlementBankRefNo(settlement.getBankTransactionRefId());
  								info.setSettlementStatus(SettlementStatus.valueOf(settlement.getStatus()));
  							} else {
  								info.setSettlementStatus(SettlementStatus.valueOf(SettlementStatus.PROCESS.value()));
  							}
  						}

  						if (isCommission) {
  							BigDecimal amt = BigDecimal.ZERO, cedgeAmt = BigDecimal.ZERO, bankAmt = BigDecimal.ZERO;
  							BigDecimal amtGst = BigDecimal.ZERO, cedgeAmtGst = BigDecimal.ZERO,
  									bankAmtGst = BigDecimal.ZERO;
  							BigDecimal amtTds = BigDecimal.ZERO, cedgeAmtTds = BigDecimal.ZERO,
  									bankAmtTds = BigDecimal.ZERO;
  							for (DipcoinAccountTransaction trnx : dipcoinAccountTranx) {
  								if (DipcoinAccountTransactionType.BRONTOO_PRINCIPAL_TO_MERCHANT
  										.equals(trnx.getType())) {
  									info.setMerchantAmount(
  											trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  								} else if (DipcoinAccountTransactionType.BRONTOO_PRINCIPAL_TO_BRONTOO_COMMISSION
  										.equals(trnx.getType())) {
  									info.setOstaCommission(
  											trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  								} else if (DipcoinAccountTransactionType.BRONTOO_PRINCIPAL_TO_BRONTOO_GST
  										.equals(trnx.getType())) {
  									info.setOstaGst(trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  								}
  								if (this.userDBService.isMerchantInternalUser(user)
  										|| this.userDBService.brontooRepresentative(user)) {

  									// Fetching the CustomerAccount with the help of
  									// DipcoinTransaction.getCustomerAccountId

  									CustomerAccount customerAccount = customerDBService
  											.getAccountById(dTxn.getCustomerAccountId());
  									if (customerAccount != null) {

  										Hibernate.initialize(customerAccount.getBank());
  										Bank bank = customerAccount.getBank();
  										info.setBankName(bank.getName());
  										// Hibernate.initialize(customerAccount.getUser());
  										// final User users = customerAccount.getUser();
  									}

  									if (DipcoinAccountTransactionType.BRONTOO_PRINCIPAL_TO_BRONTOO_COMMISSION
  											.equals(trnx.getType())) {
  										info.setOstaCommission(
  												trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										amt = trnx.getAmount();
  									} else if (DipcoinAccountTransactionType.BRONTOO_PRINCIPAL_TO_BRONTOO_GST
  											.equals(trnx.getType())) {
  										info.setOstaGst(trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										amtGst = trnx.getAmount();
  									} else if (DipcoinAccountTransactionType.BRONTOO_COMMISSION_TO_CEDGE_COMMISSION
  											.equals(trnx.getType())) {
  										info.setCedgeCommission(
  												trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										cedgeAmt = trnx.getAmount();
  									} else if (DipcoinAccountTransactionType.BRONTOO_GST_TO_CEDGE_COMMISSION
  											.equals(trnx.getType())) {
  										info.setCedgeGst(trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										cedgeAmtGst = trnx.getAmount();
  									} else if (DipcoinAccountTransactionType.CEDGE_COMMISSION_TO_BRONTOO_TDS
  											.equals(trnx.getType())) {
  										info.setCedgeTds(trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										cedgeAmtTds = trnx.getAmount();
  									} else if (DipcoinAccountTransactionType.BRONTOO_COMMISSION_TO_BANK_COMMISSION
  											.equals(trnx.getType())) {
  										info.setBankCommission(
  												trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										bankAmt = trnx.getAmount();
  									} else if (DipcoinAccountTransactionType.BRONTOO_GST_TO_BANK_COMMISSION
  											.equals(trnx.getType())) {
  										info.setBankGst(trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										bankAmtGst = trnx.getAmount();
  									} else if (DipcoinAccountTransactionType.BANK_COMMISSION_TO_BRONTOO_TDS
  											.equals(trnx.getType())) {
  										info.setBankTds(trnx.getAmount() != null ? trnx.getAmount().toString() : null);
  										bankAmtTds = trnx.getAmount();
  									}
  									info.setOstaCommission((amt.subtract(cedgeAmt.subtract(bankAmt))) != null
  											? (amt.subtract(cedgeAmt.subtract(bankAmt))).toString()
  											: null);
  									info.setOstaGst((amtGst.subtract(cedgeAmtGst.subtract(bankAmtGst))) != null
  											? trnx.getAmount().toString()
  											: null);
  									// info.setOstaTds(info.getOstaTds().subtract(info.getCedgeTds().subtract(info.getBankTds())));
  								}
  							}
  						}
  					}
  					transactions.add(info);
  				}
  			}

  			if (!CollectionUtils.isEmpty(pTxns)) {
  				for (PartnerTransaction pTxn : pTxns) {
  					MerchantTransactionsResponse info = new MerchantTransactionsResponse();

  					populatePartnerTransactionInfo(pTxn, info);

  					// Fetch all the data from DipcoinAccountTransaction with the help of
  					// dipcoinTrnxId
  					List<PartnerAccountTransaction> partnerAccountTranx = this.partnerAccountTransactionDBService
  							.findByPartnerTransactionId(pTxn.getId());
  					if (!CollectionUtils.isEmpty(partnerAccountTranx)) {

  						PartnerSettlement pSettlement = partnerSettlementDBService.findById(
  								partnerAccountTranx.get(0).getSettlementId(),
  								Arrays.asList(
  										com.dipcoin.partner.db.services.commons.DBConstants.SettlementType.PARTNER_SETTLEMENT_ACCOUNT_TO_PARTNER_PRINCIPAL_ACCOUNT
  												.value(),
  										com.dipcoin.partner.db.services.commons.DBConstants.SettlementType.BRONTOO_SETTLEMENT_TO_PARTNER_PRINCIPAL_ACCOUNT
  												.value()));
  						if (pSettlement != null) {
  							if (PartnerSettlementStatus.FAILED.value() == pSettlement.getStatus()
  									|| PartnerSettlementStatus.SUCCESS.value() == pSettlement.getStatus()) {
  								info.setSettlementBankRefNo(pSettlement.getBankTransactionRefId());
  								info.setSettlementStatus(SettlementStatus.valueOf(pSettlement.getStatus()));
  							} else {
  								info.setSettlementStatus(SettlementStatus.valueOf(SettlementStatus.PROCESS.value()));
  							}
  						}
  						if (isCommission) {
  							BigDecimal amt = BigDecimal.ZERO;
  							BigDecimal amtGst = BigDecimal.ZERO;
  							for (PartnerAccountTransaction trnx : partnerAccountTranx) {
  								if (PartnerAccountTransactionType.PARTNER_SETTLEMENT_TO_PARTNER_PRINCIPAL
  										.value() == trnx.getType()) {
  									info.setMerchantAmount(trnx.getAmount().toString());
  								} else if (PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_BRONTOO_COMMISSION
  										.value() == trnx.getType()) {
  									amt = amt.add(trnx.getAmount());
  									info.setOstaCommission(amt.toString());
  								} else if (PartnerAccountTransactionType.BRONTOO_COMMISSION_TO_CEDGE_COMMISSION
  										.value() == trnx.getType()) {
  									amt = amt.add(trnx.getAmount());
  									info.setOstaCommission(amt.toString());
  								} else if (PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_BRONTOO_COMMISSION_GST
  										.value() == trnx.getType()) {
  									amtGst = amtGst.add(trnx.getAmount());
  									info.setOstaGst(amtGst.toString());
  								} else if (PartnerAccountTransactionType.BRONTOO_GST_TO_CEDGE_COMMISSION.value() == trnx
  										.getType()) {
  									amtGst = amtGst.add(trnx.getAmount());
  									info.setOstaGst(amtGst.toString());
  								} else if (PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_PG_COMMISSION
  										.value() == trnx.getType()) {
  									info.setPgCharge(trnx.getAmount().toString());
  								} else if (PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_PG_COMMISSION_GST
  										.value() == trnx.getType()) {
  									info.setPgGst(trnx.getAmount().toString());
  								}
  								if (this.userDBService.isMerchantInternalUser(user)
  										|| this.userDBService.brontooRepresentative(user)) {
  									if (PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_BRONTOO_COMMISSION
  											.value() == trnx.getType()) {
  										info.setOstaCommission(trnx.getAmount().toString());
  									} else if (PartnerAccountTransactionType.BRONTOO_COMMISSION_TO_CEDGE_COMMISSION
  											.value() == trnx.getType()) {
  										info.setCedgeCommission(trnx.getAmount().toString());
  									} else if (PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_BRONTOO_COMMISSION_GST
  											.value() == trnx.getType()) {
  										info.setOstaGst(trnx.getAmount().toString());
  									} else if (PartnerAccountTransactionType.BRONTOO_GST_TO_CEDGE_COMMISSION
  											.value() == trnx.getType()) {
  										info.setCedgeGst(trnx.getAmount().toString());
  									}
  								}
  							}
  						}
  					}
  					transactions.add(info);
  				}
  			}

  		}

  		if (!CollectionUtils.isEmpty(transactions)) {

  			// printing the list.
  			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Trasaction", merchant.getId())
  					.format());

  			// Sorting the MerchantTransactionsResponse according to requestTime.
  			Collections.sort(transactions);

  			// overriding the value of endCountOfList with transactions list size. if
  			// condition statisfy.
  			if (transactions.size() < count) {
  				endCountOfList = transactions.size();
  			}
  			// get count form the list depending upon the startCountOfList and
  			// endCountOfList
  			for (int i = startCountOfList; i < endCountOfList; i++) {
  				filteredTransactions.add(transactions.get(i));
  			}
  		}

  		// set pagination info
  		Pagination pagination = new Pagination();
  		pagination.setStartRange(startTime);
  		pagination.setEndRange(endTime);
  		pagination.setScanCompleted(transactions.size() < count);
  		pagination.setTotal(transactions.size());
  		if (!pagination.getScanCompleted()) {
  			pagination.setStart(startCountOfList + count);
  		}
  		response.setTransactions(filteredTransactions);
  		response.setPagination(pagination);

  		return ResponseEntity.ok(response);
  	}

  	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
  }
  
  
  public static void populateMerchantTransactionInfo(final DipcoinTransaction tx, MerchantTransactionsResponse info) {
	    if (info == null)
	      info = new MerchantTransactionsResponse();

	    if (tx != null) {
	      info.setAmount(tx.getAmount() != null ? tx.getAmount().toString() : zero).setStatus(tx.getStatus())
	          .setTime(tx.getRequestTime())
	          .setMerchantTransactionRefId(tx.getPartnerTransactionReferenceId())
	          .setOstaTransactionRefId(tx.getDipcoinTransactionRefId()).setOrderId(tx.getOrderId())
	          .setChannelType(0).setType(DipcoinTransactionType.valueOf(tx.getType())); // @TODO - populate from
	      if (DipcoinTransactionsStatus.FAILURE.value() == tx.getStatus()) {
	        if(StringUtils.isNotEmpty(tx.getDCResponseDesc())) {
	          info.setErrorDescription(tx.getDCResponseDesc()); }
	      }   
	    }
	  }
	  
	  public static void populatePartnerTransactionInfo(PartnerTransaction pTxn, MerchantTransactionsResponse info) throws JsonMappingException, JsonProcessingException {
	    if (info == null)
	      info = new MerchantTransactionsResponse();

	    if (pTxn != null) {
	      
	      if (PartnerTransactionStatus.FAILURE.value()== pTxn.getStatus() ) {
	        if (pTxn.getTransactionStatusRawResponse() != null) {
	          AggrepayPaymentStatusResponse pgResponse =
	              new ObjectMapper().readValue(pTxn.getTransactionStatusRawResponse(),
	                  AggrepayPaymentStatusResponse.class);
	          if (pgResponse.getData() != null) {
	          if (pgResponse.getData().get(0) != null
	              && !StringUtils.isEmpty(pgResponse.getData().get(0).getError_desc())) { 
	            info.setErrorDescription(pgResponse.getData().get(0).getError_desc());
	          }

	        }}
	      }
	      
			info.setAmount(pTxn.getAmount() != null ? pTxn.getAmount().toString() : zero).setStatus(pTxn.getStatus());
			info.setTime(pTxn.getRequestTime().toString());
			info.setMerchantTransactionRefId(pTxn.getPartnerTransactionRefId());
			info.setOstaTransactionRefId(pTxn.getOstaTransactionRefId()).setOrderId(pTxn.getOrderId());
			info.setPaymentMode(pTxn.getPaymentMode());
			info.setType(PartnerTransactionType.valueOf(pTxn.getType()));
			info.setPgReferenceId(pTxn.getPgTransactionRefId());
			info.setGatewayName(pTxn.getGatewayName());
			info.setChargeableAmount(pTxn.getChargeableAmount() != null ? pTxn.getAmount().toString() : zero);
		 }
	  }
	  
	  
	  
	  /*
	   * Get Settlements from settlement table
	   */
	  public ResponseEntity getSettlements(final User user, final Integer merchantId, Long startTime,
	      Long endTime, Integer start, Integer count, Integer status) throws Exception {
		  
		  Merchant merchant = merchantDBService.getMerchant(merchantId);
	    if (user != null && merchant != null) {
	      if (!(this.userDBService.isMerchantSuperAdmin(user)
	          || this.userDBService.isMerchantAdmin(user)
	          || this.userDBService.isMerchantInternalUser(user)
	          || this.userDBService.brontooRepresentative(user))) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
	      }

	      List<Integer> types = Arrays.asList(
	          SettlementType.BRONTOO_PRINCIPAL_TO_MERCHANT_ACCOUNT.value(),
	          com.dipcoin.partner.db.services.commons.DBConstants.SettlementType.PARTNER_SETTLEMENT_ACCOUNT_TO_PARTNER_PRINCIPAL_ACCOUNT
	              .value(),
	          com.dipcoin.partner.db.services.commons.DBConstants.SettlementType.BRONTOO_SETTLEMENT_TO_PARTNER_PRINCIPAL_ACCOUNT
	              .value());

	      // Overiding the start and count value and setting startCountOfList and endCountOfList
	      Integer startCountOfList = start;
	      Integer endCountOfList = start + count;

	      // overiding the count and start to fetch and sort the list
	      count = start + count; // Eg: count = start(50) + count(50) = total 100 transaction from settlement
	                             // and Partnersettlement.
	      start = 0; // This because we have to fetch from 0th transaction

	      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	          .data("merchant Id", merchant.getId()).data("startTime", startTime)
	          .data("endTime", endTime).data("start", start).data("count", count).data("status", status)
	          .data("type", types).format());

	      List<BankAccount> accounts = this.bankDBService.getMerchantBankAccounts(merchant.getId());
	      if (CollectionUtils.isEmpty(accounts)) {
	        return ResponseEntity.status(HttpStatus.OK)
	            .body(APIResponse.error(HeaderCode.BANK_ACCOUNT_DOESNT_EXISTS));
	      }

	      List<Integer> accountIds = new ArrayList<Integer>();
	      for (BankAccount account : accounts) {
	        accountIds.add(account.getId());
	      }

	      SettlementsResponse response = new SettlementsResponse();
	      List<Settlement> settlements = this.settlementDBService
	          .asyncGetMerchantSettlements(accountIds, types, status, startTime, endTime, start, count)
	          .get();

	      List<PartnerSettlement> partnerSettlements = new LinkedList<>();
	      if (DBConstants.MerchantPartnerType.PARTNER.value() == merchant.getPartnerType()
	          || DBConstants.MerchantPartnerType.PARTNER_ORGANISATION.value() == merchant
	              .getPartnerType()
	          || DBConstants.MerchantPartnerType.PARTNER_AGGREGATOR.value() == merchant.getPartnerType()
	          || DBConstants.MerchantPartnerType.INTERNAL_PARTNER.value() == merchant.getPartnerType()
	          || DBConstants.MerchantPartnerType.PARTNER_WHOLESALER.value() == merchant.getPartnerType()
	          || DBConstants.MerchantPartnerType.PARTNER_RETAILER.value() == merchant
	              .getPartnerType()) {

	        Partner partner = PartnerEntityDBService.getPartner(merchant.getReferenceId());
	        if (partner != null) {
	          PartnerAccount pAccount = this.partnerAccountDBService.findByPartnerIdAndCode(
	              partner.getId(), PartnerBankAccountCodes.PARTNER_PRINCIPAL_ACCOUNT.value());
	          if (pAccount != null) {
	            partnerSettlements = this.partnerSettlementDBService.getPartnerSettlementByTypes(
	                pAccount.getId(), types, status, startTime, endTime, start, count);
	          }
	        }
	      }

	      List<SettlementsResponse> transactions = new ArrayList<>();
	      List<SettlementsResponse> filteredTransactions = new ArrayList<>();

	      if (!CollectionUtils.isEmpty(settlements)) {
	        for (Settlement settlement : settlements) {
	          SettlementsResponse settlementInfo = new SettlementsResponse();
	          // to get Transactions start
	          List<Integer> dIds = new ArrayList<Integer>(); 
	          List<DipcoinAccountTransaction> datxs = dipcoinAccountTransactionDBService.findBySettlementIds(Arrays.asList(settlement.getId()));
	          if (!CollectionUtils.isEmpty(datxs)) {
	        	  for (DipcoinAccountTransaction datx : datxs) {
	        		 /* if(DipcoinAccountTransactionType.BRONTOO_PRINCIPAL_TO_MERCHANT.value() == datx.getType()) {
	        			  transactionInfo.setMerchantAmount(datx.getAmount() != null ? datx.getAmount().toString() : zero);
	        		  }else if(DipcoinAccountTransactionType.BANK_COMMISSION_TO_BRONTOO_COMMISSION.value() == datx.getType()) {
	        			  transactionInfo.setOstaCommission(datx.getAmount() != null ? datx.getAmount().toString() : zero);
	        		  }else if(DipcoinAccountTransactionType.BANK_COMMISSION_TO_BRONTOO_GST.value() == datx.getType()) {
	        			  transactionInfo.setOstaGst(datx.getAmount() != null ? datx.getAmount().toString() : zero);
	        		  } */
	        		  dIds.add(datx.getDipcoinTransactionId());
	        		  
	        	  } 
	        	  List<DipcoinTransaction> dtxs = coinDBService.getTransactionsByIds(dIds, null, null);
	        	  if (!CollectionUtils.isEmpty(dtxs)) {
	        	    List<Transactions> dipcoinTransactions = new LinkedList<>();
	        	    for(DipcoinTransaction dTx : dtxs) {
	        	      Transactions transaction = new Transactions();
	        	      transaction.setOrderId(dTx.getOrderId());
	        	      transaction.setOstaTransactionRefId(dTx.getDipcoinTransactionRefId());
	        	      transaction.setMerchantTransactionRefId(dTx.getPartnerTransactionReferenceId());
	        	      transaction.setTxnTime(dTx.getRequestTime());
	        	      transaction.setAmount(dTx.getAmount() != null ? dTx.getAmount().toString() : zero);
	        	      transaction.setType(DipcoinTransactionType.valueOf(dTx.getType()));
	        	      dipcoinTransactions.add(transaction);
	        	    }
	        	    settlementInfo.setTransactions(dipcoinTransactions);
	        	  }
	          }
	       // to get Transactions end
	          
	          settlementInfo.setCreditAccountNumber(settlement.getCreditAccountNumber());
	          settlementInfo.setSettlementAmount(settlement.getAmount() == null ? BigDecimal.ZERO.toString()
	              : settlement.getAmount().toString());
	          settlementInfo.setStatus(settlement.getStatus());
	          settlementInfo.setRequestTime(settlement.getRequestTime());
	          settlementInfo.setPayDate(settlement.getPayDate());
	          settlementInfo.setBankResponse(settlement.getReason());
	          settlementInfo.setBankTxnRefId(settlement.getBankTransactionRefId());

	          transactions.add(settlementInfo);
	        }
	      }

	      if (!CollectionUtils.isEmpty(partnerSettlements)) {
	        for (PartnerSettlement partnerSettlement : partnerSettlements) {
	          SettlementsResponse settlementInfo = new SettlementsResponse();
	          // to get Transactions start
	          List<Integer> pIds = new ArrayList<Integer>(); 
	          
	          List<PartnerAccountTransaction> paTxs = partnerAccountTransactionDBService.getByPartnerSettlementIds(Arrays.asList(partnerSettlement.getId()));
	          if (!CollectionUtils.isEmpty(paTxs)) {
	        	  for (PartnerAccountTransaction pTx : paTxs) {
	        		 /* if(PartnerAccountTransactionType.PARTNER_SETTLEMENT_TO_PARTNER_PRINCIPAL.value() == patx.getType()) {
	        			  settlementInfo.setMerchantAmount(patx.getAmount() != null ? patx.getAmount().toString() : zero);
	        		  }else if(PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_BRONTOO_COMMISSION.value() == patx.getType()) {
	        			  settlementInfo.setOstaCommission(patx.getAmount() != null ? patx.getAmount().toString() : zero);
	        		  }else if(PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_BRONTOO_COMMISSION_GST.value() == patx.getType()) {
	        			  settlementInfo.setOstaGst(patx.getAmount() != null ? patx.getAmount().toString() : zero);
	        		  }else if(PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_PG_COMMISSION.value() == patx.getType()) {
	        			  settlementInfo.setOstaGst(patx.getAmount() != null ? patx.getAmount().toString() : zero);
	        		  }else if(PartnerAccountTransactionType.PARTNER_PRINCIPAL_TO_PG_COMMISSION_GST.value() == patx.getType()) {
	        			  settlementInfo.setOstaGst(patx.getAmount() != null ? patx.getAmount().toString() : zero);
	        		  }*/
	        		  pIds.add(pTx.getPartnerTransactionId());
	        	  }
	        	  List<PartnerTransaction> pTxs = partnerTransactionDBService.getPartnerTransactionsByIds(pIds, null, null);
	        	  if (!CollectionUtils.isEmpty(pTxs)) {
	                List<Transactions> partnerTransactions = new LinkedList<>();
	        	    for(PartnerTransaction pTx : pTxs) {
	        	      Transactions transaction = new Transactions();
	        	      transaction.setOrderId(pTx.getOrderId());
	        	      transaction.setOstaTransactionRefId(pTx.getOstaTransactionRefId());
	        	      transaction.setMerchantTransactionRefId(pTx.getPartnerTransactionRefId());
	        	      transaction.setTxnTime(pTx.getRequestTime());
	        	      transaction.setAmount(pTx.getAmount() != null ? pTx.getAmount().toString() : zero);
	        	      transaction.setType(DipcoinTransactionType.valueOf(pTx.getType()));
	        	      partnerTransactions.add(transaction);
	        		  }
	        	    settlementInfo.setTransactions(partnerTransactions);
	        	  }
	          }
	       // to get Transactions end
	          settlementInfo.setCreditAccountNumber(partnerSettlement.getCreditAccountNumber());
	          settlementInfo
	              .setSettlementAmount(partnerSettlement.getAmount() == null ? BigDecimal.ZERO.toString()
	                  : partnerSettlement.getAmount().toString());
	          settlementInfo.setStatus(partnerSettlement.getStatus());
	          settlementInfo.setRequestTime(partnerSettlement.getRequestTime());
	          settlementInfo.setPayDate(partnerSettlement.getPayDate());
	          settlementInfo.setBankResponse(partnerSettlement.getReason());
	          settlementInfo.setBankTxnRefId(partnerSettlement.getBankTransactionRefId());

	          transactions.add(settlementInfo);
	        }
	      }

	      if (!CollectionUtils.isEmpty(transactions)) {

	        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	            .data("Trasaction", merchant.getId()).format());

	        // Sorting the SettlementResponse according to requestTime.
	        Collections.sort(transactions);

	        // overriding the value of endCountOfList with transactions list size. if condition
	        // statisfy.
	        if (transactions.size() < count) {
	          endCountOfList = transactions.size();
	        }
	        // get count form the list depending upon the startCountOfList and endCountOfList
	        for (int i = startCountOfList; i < endCountOfList; i++) {
	          filteredTransactions.add(transactions.get(i));
	        }
	      }

	      // set pagination info
	      Pagination pagination = new Pagination();
	      pagination.setStartRange(startTime);
	      pagination.setEndRange(endTime);
	      pagination.setScanCompleted(transactions.size() < count);
	      pagination.setTotal(transactions.size());
	      if (!pagination.getScanCompleted()) {
	        pagination.setStart(startCountOfList + count);
	      }
	      response.setSettlements(filteredTransactions);
	      response.setPagination(pagination);

	      return ResponseEntity.ok(response);
	    }

	    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
	  }
	  
	  
	  
	  public ResponseEntity getReconciliation(User user, Merchant merchant, Long startTime,
		      Long endTime, Integer start, Integer count, Integer reconIssue, Integer reconStatus,
		      Integer type) throws Exception {

		    if (user != null && merchant != null) {
		      if (!this.userDBService.isMerchantSuperAdmin(user)
		          && !this.userDBService.isMerchantAdmin(user)
		          && !this.userDBService.isMerchantInternalUser(user)
		          && !this.userDBService.brontooRepresentative(user)) {
		        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
		            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		      }
		      
		      // Overiding the start and count value and setting startCountOfList and endCountOfList
		      Integer startCountOfList = start;
		      Integer endCountOfList = start + count;

		      // overiding the count and start to fetch and sort the list
		      count = start + count; // Eg: count = start(50) + count(50) = total 100 transaction from DTxn
		                             // and PTXn.
		      start = 0; // This because we have to fetch from 0th transaction

		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .data("merchant", merchant.getReferenceId()).data("startTime", startTime)
		          .data("endTime", endTime).data("start", start).data("count", count).data("type", type)
		          .data("reconStatus", reconStatus).data("reconIssue", reconIssue).format());


		      List<MerchantRecon> merchantRecons = merchantDBService.getMerchantRecons(merchant, reconIssue,
		          reconStatus, type, startTime, endTime, startCountOfList, endCountOfList);
		      
		      List<PartnerRecon> partnerRecons = partnerEntityDBService.getPartnerReconsByPartnerReferenceId(merchant.getReferenceId().toString(), reconIssue, 
		          reconStatus, type, startTime, endTime, startCountOfList, endCountOfList);
		      
		      
		      MerchantTransactionsResponse response = new MerchantTransactionsResponse();
		      List<MerchantTransactionsResponse> transactions = new LinkedList<>();
		      List<MerchantTransactionsResponse> filteredTransactions= new ArrayList<>();
		      
		      
		      if (!CollectionUtils.isEmpty(merchantRecons)) {
		        for (MerchantRecon merchantRecon : merchantRecons) {
		          MerchantTransactionsResponse info = new MerchantTransactionsResponse();
		          info.setMerchantTransactionRefId(merchantRecon.getMerchantTransactionRefId())
		              .setOstaTransactionRefId(merchantRecon.getDipcoinTransactionRefId())
		              .setAmount(merchantRecon.getAmount() != null ? merchantRecon.getAmount().toString()
		                  : BigDecimal.ZERO.toString())
		              .setType(DipcoinTransactionType.valueOf(merchantRecon.getType())).setTime(merchantRecon.getRequestTime())
		              .setReconIssue(merchantRecon.getIssue())
		              .setReconIssueDate(merchantRecon.getIssueDate())
		              .setReconStatus(merchantRecon.getStatus())
		              .setReconStatusDate(merchantRecon.getStatusDate());
		          transactions.add(info);
		        }
		      }
		      
		      if (!CollectionUtils.isEmpty(partnerRecons)) {
		        for (PartnerRecon partnerRecon : partnerRecons) {
		          MerchantTransactionsResponse info = new MerchantTransactionsResponse();
		          info.setMerchantTransactionRefId(partnerRecon.getPartnerTransactionReferenceId())
		              .setOstaTransactionRefId(partnerRecon.getOstaTransactionReferenceId())
		              .setAmount(partnerRecon.getAmount() != null ? partnerRecon.getAmount().toString()
		                  : BigDecimal.ZERO.toString())
		              .setType(DipcoinTransactionType.valueOf(2))
		              .setTime(partnerRecon.getRequestTime())
		              .setReconIssue(partnerRecon.getIssue())
		              .setReconIssueDate(partnerRecon.getIssueDate())
		              .setReconStatus(partnerRecon.getStatus())
		              .setReconStatusDate(partnerRecon.getStatusDate());
		          transactions.add(info);
		        }
		      }
		      
		      if (!CollectionUtils.isEmpty(transactions)) {

		        // printing the list.
		        LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		            .data("Trasaction", merchant.getId()).format());

		        // Sorting the MerchantTransactionsResponse according to requestTime.
		        Collections.sort(transactions);

		        // overriding the value of endCountOfList with transactions list size. if condition
		        // statisfy.
		        if (transactions.size() < count) {
		          endCountOfList = transactions.size();
		        }
		        // get count form the list depending upon the startCountOfList and endCountOfList
		        for (int i = startCountOfList; i < endCountOfList; i++) {
		          filteredTransactions.add(transactions.get(i));
		        }
		      }

		      // set pagination info
		      Pagination pagination = new Pagination();
		      pagination.setStartRange(startTime);
		      pagination.setEndRange(endTime);
		      pagination.setScanCompleted(transactions.size() < count);
		      pagination.setTotal(transactions.size());
		      if (!pagination.getScanCompleted()) {
		        pagination.setStart(startCountOfList + count);
		      }
		      response.setTransactions(filteredTransactions);
		      response.setPagination(pagination);

		      return ResponseEntity.ok(response);

		    }
		    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));

		  }

}
