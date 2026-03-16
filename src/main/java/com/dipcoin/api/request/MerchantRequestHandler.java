/**
 * 
 */
package com.dipcoin.api.request;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIDoc;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
//import com.dipcoin.api.model.BankDetailsResponse;
//import com.dipcoin.api.model.BrontooConvenienceFeeResponse;
//import com.dipcoin.api.model.ClientLoggingRequest;
import com.dipcoin.api.model.DipcoinApprovalRequest;
import com.dipcoin.api.model.MerchantInfoResponse;
//import com.dipcoin.api.model.MerchantRegisterRequest;
//import com.dipcoin.api.model.MerchantReportsResponse;
import com.dipcoin.api.model.PartnerPaymentRequest;
import com.dipcoin.api.model.SettlementsResponse;
//import com.dipcoin.api.model.SettlementsResponse;
//import com.dipcoin.api.model.TransactionReportResponse;
import com.dipcoin.api.resource.MerchantResource;
import com.dipcoin.api.resource.MerchantSettlementResource;
//import com.dipcoin.api.resource.MerchantSettlementResource;
//import com.dipcoin.api.resource.MiscellaneousResource;
import com.dipcoin.db.services.commons.DBConstants.ReportType;
import com.dipcoin.db.services.model.User;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 *
 */
@Api(value = "/v1/merchant")
@RestController
@RequestMapping(value = "/v1/merchant",
    consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
@CrossOrigin
@Timed
public class MerchantRequestHandler extends RequestHandler {

  private static final Logger LOG = LogManager.getLogger(MerchantRequestHandler.class);

  @Autowired
  private MerchantResource merchantResource;

  @Autowired
  private MerchantSettlementResource merchantSettlementResource;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;
  
//  @Autowired
//  private MiscellaneousResource miscellaneousResource;
  

  /*
   * Get Merchant Info
   */
  @GetMapping
  @ApiOperation(value = "Merchant Info", notes = "API to get merchant info.",
      response = MerchantInfoResponse.class)
  public ResponseEntity get(
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws Exception {

	  User user = httpServletContext.getUser();
	  return merchantResource.getMerchant(user, user.getBankMerchantId());
  }
  
  @GetMapping("reports/aggregate")
  @ApiOperation(value = "Merchant Report Aggregate",
      notes = "API to fetch merchant Aggregated reports.")
  public ResponseEntity merchantSettlement(
      @ApiParam(value = "Start Time", required = false,
          defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
      @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
          value = "endTime", defaultValue = "2147483646999") Long endTime,
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws Exception {

	  User user = httpServletContext.getUser();
    return merchantSettlementResource.getReportsAggregate(user, user.getBankMerchantId(), startTime, endTime);
  }
  
  @GetMapping("amount")
  public ResponseEntity getMerchantAmount(
		  @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
		  @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2
		  ) throws Exception{
      User user = httpServletContext.getUser();
	  return merchantResource.getMerchantAmount(user, user.getBankMerchantId());
}

  /*
   * Settlement & Recon
   */
  @GetMapping("transactions")
  @ApiOperation(value = "Merchant Transactions",
      notes = "API to fetch merchant transactions.")
  public ResponseEntity merchantTransactions(
     @ApiParam(value = "Transaction status", required = false) @RequestParam(value = "statuses",
          required = false) List<Integer> statuses,
      @ApiParam(value = "Start Time", required = false,
          defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
      @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
          value = "endTime", defaultValue = "2147483646999") Long endTime,
      @ApiParam(value = "Transaction type", required = false) @RequestParam(value = "type",
      required = false) Integer type,
      @ApiParam(value = "start", required = false,
          defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
      @ApiParam(value = "count", required = false,
          defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
      @ApiParam(value = "partnerTransactionReferenceId", required = false)
     				@RequestParam(value = "partnerTransactionReferenceId",required = false)String partnerTransactionReferenceId,
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws Exception {

	  User user = httpServletContext.getUser();
	  return merchantSettlementResource.getTransactions(user, user.getBankMerchantId(), statuses, type, startTime, endTime, start, count, false,partnerTransactionReferenceId);
  }
  
  @GetMapping("settlements")
  @ApiOperation(value = "Merchant Settlements",
      notes = "API to fetch Merchant Settlements.",
      response = SettlementsResponse.class)
  public ResponseEntity merchantSettlementSummary(
      @ApiParam(value = "Start Time", required = false,
          defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
      @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
          value = "endTime", defaultValue = "2147483646999") Long endTime,
      @ApiParam(value = "start", required = false,
          defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
      @ApiParam(value = "count", required = false,
          defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
      @ApiParam(value = "Status", required = false) @RequestParam(value = "status",
          required = false) Integer status,
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws Exception {

	  User user = httpServletContext.getUser();
	    return merchantSettlementResource.getSettlements(user, user.getBankMerchantId(), startTime, endTime, start, count, status);
  }

  @GetMapping("reconciliations")
  @ApiOperation(value = "Merchant reconciliations",
      notes = "API to fetch merchant reconciliations.")
  public ResponseEntity merchantReconciliation(
      @ApiParam(value = "Start Time", required = false,
          defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
      @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
          value = "endTime", defaultValue = "2147483646999") Long endTime,
      @ApiParam(value = "Recon Issue", required = false) @RequestParam(value = "reconIssue",
      required = false) Integer reconIssue,
      @ApiParam(value = "Recon Status", required = false) @RequestParam(value = "reconStatus",
      required = false) Integer reconStatus,
      @ApiParam(value = "Transaction type", required = false) @RequestParam(value = "type",
      required = false) Integer type,
      @ApiParam(value = "start", required = false,
          defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
      @ApiParam(value = "count", required = false,
          defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws Exception {

    return merchantSettlementResource.getReconciliation(httpServletContext.getUser(),
        httpServletContext.getMerchant(), startTime, endTime, start, count, reconIssue, reconStatus, type);
  }
  
  @GetMapping("transaction/commission")
  @ApiOperation(value = "Merchant Commissions",
      notes = "API to fetch the commission amount of Merchants")
  public ResponseEntity getCommissionCharges(
     @ApiParam(value = "Transaction status", required = false) @RequestParam(value = "statuses",
          required = false) List<Integer> statuses,
      @ApiParam(value = "Start Time", required = false,
          defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
      @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
          value = "endTime", defaultValue = "2147483646999") Long endTime,
      @ApiParam(value = "Transaction type", required = false) @RequestParam(value = "type",
      required = false) Integer type,
      @ApiParam(value = "start", required = false,
          defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
      @ApiParam(value = "count", required = false,
          defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws Exception {

	  User user = httpServletContext.getUser();
	  return merchantSettlementResource.getTransactions(user, user.getBankMerchantId(), statuses, type, startTime, endTime, start, count, true,null);
  }
  
  @GetMapping("docs")
  @ApiOperation(value = "Fetching merchant uploaded  Docs",
      notes = "API to fetch list of merchant uploaded doc info. Admin User access only.")
  public ResponseEntity getMerchantDocs(
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws IOException {

    return merchantResource.getMerchantDocs(httpServletContext.getUser(),
        httpServletContext.getMerchant());
  }

  @GetMapping("onboard/fees")
  @ApiOperation(value = "Get Merchant OnboardFee",
      notes = "API to get merchant OnboardFee.",
      response = MerchantInfoResponse.class)
  public ResponseEntity onboardFees(
      @ApiParam(value = APIDoc.tokenNotes, required = true,
          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
      throws Exception, APIException {

    return merchantResource.getOnboardFeesForMerchant(httpServletContext.getUser(),
        httpServletContext.getMerchant());
  }
  
//  /*
//   * submit merchant Setup/AMC fees
//   */
//  @PostMapping("payment")
//  @ApiOperation(value = "Merchant api for capturing Setup/AMC fees",
//      notes = "API to store merchant's Setup/AMC payment details", response = APIResponse.class)
//  public ResponseEntity Payment(
//      @ApiParam(value = "Details required for payment",
//          required = true) @RequestBody final PartnerPaymentRequest paymentRequest,
//      @ApiParam(value = APIDoc.tokenNotes, required = true,
//          defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(
//              value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
//      @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(
//          value = APIConstants.DC_LOGIN_COOKIE) String apiDocPurposeOnly2)
//      throws Exception {
//
//    return merchantResource.addPaymentDetails(httpServletContext.getUser(),
//        httpServletContext.getMerchant(), paymentRequest);
//  }
  
  
  
}
