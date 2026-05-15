/**
 * 
 */
package com.dipcoin.api.request;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.MerchantInfoResponse;
import com.dipcoin.api.model.SettlementsResponse;
import com.dipcoin.api.resource.MerchantResource;
import com.dipcoin.api.resource.MerchantSettlementResource;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

/**
 *
 */
@Api(value = "/v1/jwt/merchant")
@RestController
@RequestMapping(value = "/v1/jwt/merchant",
    consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
@CrossOrigin
@Timed
public class MerchantRequestHandlerAuth extends RequestHandler {

  private static final Logger LOG = LogManager.getLogger(MerchantRequestHandlerAuth.class);

  @Autowired
  private MerchantResource merchantResource;

  @Autowired
  private MerchantSettlementResource merchantSettlementResource;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;
  

  /*
   * Get Merchant Info
   */
  @GetMapping("merchant")
  @ApiOperation(value = "Merchant Info", notes = "API to get merchant info.",
      response = MerchantInfoResponse.class)
  public ResponseEntity get(
		  @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
			@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
	      throws Exception, APIException {

	    User user = httpServletContext.getUser();
	    Merchant merchant = httpServletContext.getMerchant();

	    return merchantResource.getMerchant(user, merchant);
	}

  
  @GetMapping("reports/aggregate")
  @ApiOperation(value = "Merchant Report Aggregate",
      notes = "API to fetch merchant Aggregated reports.")
  public ResponseEntity merchantSettlement(
      @ApiParam(value = "Start Time", required = false,
          defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
      @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
          value = "endTime", defaultValue = "2147483646999") Long endTime,
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
    throws Exception, APIException {

	  User user = httpServletContext.getUser();
    return merchantSettlementResource.getReportsAggregate(user, user.getBankMerchantId(), startTime, endTime);
  }
  
  @GetMapping("amount")
  public ResponseEntity getMerchantAmount(
		  @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
			@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
	      throws Exception, APIException {
	  
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
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
   throws Exception, APIException {

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
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
    throws Exception, APIException {

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
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
    throws Exception, APIException {

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
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
   throws Exception, APIException {

	  User user = httpServletContext.getUser();
	  return merchantSettlementResource.getTransactions(user, user.getBankMerchantId(), statuses, type, startTime, endTime, start, count, true,null);
  }
  
  @GetMapping("docs")
  @ApiOperation(value = "Fetching merchant uploaded  Docs",
      notes = "API to fetch list of merchant uploaded doc info. Admin User access only.")
  public ResponseEntity getMerchantDocs(
		  @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
			@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
	      throws Exception, APIException {

    return merchantResource.getMerchantDocs(httpServletContext.getUser(),
        httpServletContext.getMerchant());
  }

  @GetMapping("onboard/fees")
  @ApiOperation(value = "Get Merchant OnboardFee",
      notes = "API to get merchant OnboardFee.",
      response = MerchantInfoResponse.class)
  public ResponseEntity onboardFees(
		  @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
			@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
	      throws Exception, APIException {

    return merchantResource.getOnboardFeesForMerchant(httpServletContext.getUser(),
        httpServletContext.getMerchant());
  }
  
  @DeleteMapping("document")
  @ApiOperation(value = "Delete the uploaded documents", notes = "API to delete the uploaded documents")
  public ResponseEntity deleteDocument(
      @ApiParam(value = "DocumentType",
      required = true) @RequestParam(value = "documentType", required = true) final Integer documentType,
      @ApiParam(value = "DocumentId",
      required = true) @RequestParam(value = "documentId", required = true) final String documentId, 
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
    throws Exception, APIException {
	  
    return merchantResource.deleteDocument(httpServletContext.getUser(), httpServletContext.getMerchant(),
       documentType, documentId);
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
