package com.dipcoin.api.request;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import com.dipcoin.api.model.BankTransactionsResponse;
import com.dipcoin.api.model.TollRechargeRequest;
import com.dipcoin.api.model.TollRechargeResponse;
//import com.dipcoin.api.model.TollRechargeRequest;
//import com.dipcoin.api.model.TollRegistrationRequest;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.model.TollTagRequest;
//import com.dipcoin.api.model.TollTagVerificationRequest;
import com.dipcoin.api.resource.TollBankResource;
import com.dipcoin.api.resource.TollCustomerResource;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.User;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;

//import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value = "/v1/jwt/bank/customer")
@RestController
@RequestMapping(value = "/v1/jwt/bank/customer",  consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE,
        MediaType.ALL_VALUE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE,
        MediaType.ALL_VALUE})
@CrossOrigin
@Timed
public class TollBankRequestHandlerAuth extends RequestHandler {

  @Autowired
  private TollBankResource tollServiceBankResource;

  @Autowired
  private TollCustomerResource tollServicesResource;
  
  @Autowired
	private BankDBService bankDBService;

	@Autowired
	private UserDBService userDBService;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;



  @GetMapping("tagCounts/summary")
  public ResponseEntity getTagsRejectedPendingCounts(
      @ApiParam(value = "BankReferenceId", required = true) @RequestParam(value = "bankReferenceId",
          required = true) String bankReferenceId,
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
      throws Exception, APIException {
	  User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}

    return tollServiceBankResource.getTagsRejectedPendingCounts(user,
        bankReferenceId);
  }

  @GetMapping
	@ApiOperation(value = "Getting the toll Services Customer basis of BankReferenceId and Status", notes = "API to get all the Toll Customer basis of BankCode | TagId.", response = TollRegistrationResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Bank Code Invalid", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Tag Id Invalid", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity getAllTollCustomerDetails(
			@ApiParam(value = "BankCode of the Bank after login", required = false) @QueryParam(value = "bankReferenceId") final String bankReferenceId,
			@ApiParam(value = "branchCode", required = false) @QueryParam(value = "branchCode") final String branchCode,
			@ApiParam(value = "phone", required = false) @QueryParam(value = "phone") final String phone,
			@ApiParam(value = "list based on the status", required = false) @QueryParam(value = "status") final String status,
			@ApiParam(value = "Start Time", required = false, defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
			@ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(value = "endTime", defaultValue = "2147483646999") Long endTime,
			@ApiParam(value = "vehicleNumber", required = false) @RequestParam(value = "vehicleNumber", required = false) String vehicleNumber,
			@ApiParam(value = "accountNumber", required = false) @RequestParam(value = "accountNumber", required = false) String accountNumber,
			@ApiParam(value = "serialNumber", required = false) @RequestParam(value = "serialNumber", required = false) String serialNumber,
			@ApiParam(value = "Start", required = false, defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
			@ApiParam(value = "Count", required = false, defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
			@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
			throws Exception, APIException {
	  
	  User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}

		return tollServiceBankResource.getTollCustomerDetails(user, bankReferenceId, status,
				startTime, endTime, start, count, vehicleNumber, accountNumber,serialNumber, branchCode, phone);
	}
  
  // Get Toll customer Doc based on docpath
  @GetMapping("doc")
  @ApiOperation(value = "Getting the toll Customer Document based on doc path",
      notes = "API to get Toll Customer Doc based on doc path",
      response = TollRegistrationResponse.class)
  @ApiResponses(value = {
      @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
      @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
      @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
      @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class)})
  public ResponseEntity getTollCustomerDoc(
      @ApiParam(value = APIDoc.clientTransactionId, required = true) @QueryParam(
          value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
      @ApiParam(value = "Get Toll Customer info based on Toll customer id ",
          required = false) @QueryParam(value = "docPath") final String docPath,
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
      throws Exception, APIException {
	  
	  User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}

    return tollServicesResource.getTollCustomerDoc(user,
        clientTransactionId, docPath);
  }
  
  @GetMapping("charges/{ttid:.*}")
  @ApiOperation(value = "Api to get the Toll Tag Charges",
      notes = "API to get the toll tag charges.", response = APIResponse.class)
  public ResponseEntity getTollTagCharges(
      @ApiParam(value = "Toll Tag Charges",
          required = true) @PathVariable("ttid") final String encryptedTTID,
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
      throws Exception, APIException {
	  User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}

    return tollServiceBankResource.getTollTagCharges(user, encryptedTTID);

  }

  @PutMapping("tag")
  @ApiOperation(value = "Toll Customer Update Account By Bank Tag Vendor",
      notes = "API to update existing Toll Customer account By Vendor.",
      response = APIResponse.class)
  public ResponseEntity updateTollCustomerTagDetails(
      @ApiParam(value = "Toll Tag Details",
          required = true) @RequestBody final TollTagRequest updateReq,
      @ApiParam(value = APIDoc.clientTransactionId, required = true) @QueryParam(
          value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
      throws Exception, APIException {
	  User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}

    return tollServiceBankResource.updateTollCustomerByVendor(user,
        bank, updateReq, clientTransactionId);
  }
  
  @GetMapping("fetch/approvalstatus")
  public ResponseEntity fetchApprovalstatus(
 		 @ApiParam(value = "SerialNumber",
          required = true) @RequestParam(value = "serialNumber",
          required = true) final String serialNumber,
 		 @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
 			@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)throws Exception, APIException {
	  User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}
		
 	return tollServiceBankResource.fetchApprovalstatus(serialNumber);
 	 
  }
  
  @GetMapping("vehicle/charges/{category:.*}")
  @ApiOperation(value = "Api to get the Toll Tag Charges",
      notes = "API to get the toll tag charges.", response = APIResponse.class)
  public ResponseEntity getTollTagChargesByCategory(
      @ApiParam(value = "Toll Tag Charges",
          required = true) @PathVariable("category") final String category,
      @ApiParam(value = "Account Number",
      required = false) @RequestParam(value ="accountNumber",defaultValue = StringUtils.EMPTY) final String accountNumber,
      @ApiParam(value = "CustomerAccountId",
      required = false) @RequestParam(value ="cardId",defaultValue = StringUtils.EMPTY) final String cardId,
      @ApiParam(value = "Misc Charges", required = false,
      defaultValue = "true") @RequestParam(value = "miscCharges", defaultValue = "true") Boolean miscCharges,
      @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
      throws Exception, APIException {
	  User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}

    return tollServiceBankResource.getTollTagChargesByCategory(user,
        bank, category, RegistrationType.DEFAULT.value(),miscCharges, accountNumber,cardId);
  }
  
//vehicle verification netc
 @GetMapping("toll/vehicleVerification")
 @ApiOperation(value = "Getting the vehicle Verification from netc",
     notes = "API to get vehicle verification details from netc", response = ApiResponse.class)
 @ApiResponses(value = {
     @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
     @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
     @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
     @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class)})
 public ResponseEntity tollVehicleVerification(
     @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(
         value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
     @ApiParam(value = "vehicle verification from netc based on vehicle registration number",
         required = false) @RequestParam(
             value = "registrationNo",required=false) final String vehicleRegistrationNo,
     @ApiParam(value = "vehicle verification from netc based on vehicle tagId",
         required = false) @RequestParam(value = "tagId", required=false) final String tagId,
     @ApiParam(value = "vehicle verification from netc based on vehicle tid",
         required = false) @RequestParam(value = "tid", required=false) final String tid,
     @ApiParam(value = "bankReferenceId",
         required = false) @RequestParam(value = "bankReferenceId", required=true) final String bankReferenceId,
     @ApiParam(value = "regType",
     required = false) @RequestParam(value = "regType", required=true) final Integer regType,
     @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
     throws Exception, APIException {
	 User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
		Bank bank = bankDBService.getBank(user.getBankMerchantId());
		if (bank == null) {
			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
		}

   
   return tollServiceBankResource.vehicleVerification(user, vehicleRegistrationNo,
       tagId, tid, regType, bankReferenceId);
 }

//reqVehicle details this Api is similar to toll vehicle verification api 
//this api is used to get details from vahan system using vin & engineNo or vrn & engineNo combo
@GetMapping("reqVehicleDetails")
@ApiOperation(value = "Getting the vehicle Verification from netc",
    notes = "API to get vehicle verification details from netc", response = ApiResponse.class)
@ApiResponses(value = {
    @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
    @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
    @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
    @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class)})
public ResponseEntity reqVehicleDetails(
    @ApiParam(value = "vehicle verification from netc based on vehicle registration number",
        required = false) @RequestParam(value = "vrn",
            required = false) final String vrn,
    @ApiParam(value = "vehicle verification from netc based on vehicle vin",
        required = false) @RequestParam(value = "vin", required = false) final String vin,
    @ApiParam(value = "vehicle verification from netc based on vehicle lastFiveDigitsOfEngineNo",
        required = false) @RequestParam(value = "lastFiveDigitsOfEngineNo", required = false) final String lastFiveDigitsOfEngineNo,
    @ApiParam(value = "bankReferenceId",
    required = true) @RequestParam(value = "bankReferenceId") final String bankReferenceId,
    @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
    throws Exception, APIException {
	User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
	Bank bank = bankDBService.getBank(user.getBankMerchantId());
	if (bank == null) {
		return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
				.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
	}

  return tollServiceBankResource.reqVehicleDetails(vrn, vin, lastFiveDigitsOfEngineNo, null, bankReferenceId);
}

/**
 * Recharge on the Tag and Create Fastag OSTA.
 */
@PostMapping("tag/recharge")
@ApiOperation(value = "Recharge Create Osta API ", notes = "API to call Create Osta method")
@ApiResponses(value = {@ApiResponse(code = 201,
    message = "User registered AND (Failed to send otp sms OR Failed to send verification email)",
    response = APIResponse.class),
    @ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
    @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
    @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
    @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class)})
public ResponseEntity tollRecharge(
    @ApiParam(value = "Toll Recharge details",
        required = true) @RequestBody final TollRechargeRequest createReq,
    @ApiParam(value = APIDoc.clientTransactionId, required = true) @QueryParam(
        value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
    @ApiParam(value = APIDoc.tokenNotes, required = true,
        defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(
            value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
    @ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(
        value = APIConstants.DC_LOGIN_COOKIE) String dcl)
    throws Exception, APIException {
	User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
	Bank bank = bankDBService.getBank(user.getBankMerchantId());
	if (bank == null) {
		return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
				.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
	}

  return tollServiceBankResource.tollRechargeCreateOsta(user, createReq,
      clientTransactionId);
}

//toll vehicle verification at Bank side
@GetMapping("vehicleVerificationStatus")
@ApiOperation(value = "Getting the vehicle Verification from netc",
   notes = "API to get vehicle verification details from netc", response = ApiResponse.class)
@ApiResponses(value = {
   @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
   @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
   @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
   @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class)})
public ResponseEntity tollVehicleVerification(
   @ApiParam(value = "vehicle verification from netc based on vehicle registration number",
       required = false) @RequestParam(value = "registrationNo",
           required = false) final String vehicleRegistrationNo,
   @ApiParam(value = "vehicle verification from netc based on vehicle tagId",
       required = false) @RequestParam(value = "tagId", required = false) final String tagId,
   @ApiParam(value = "vehicle verification from netc based on vehicle tid",
       required = false) @RequestParam(value = "tid", required = false) final String tid,
   @ApiParam(value = "vehicle verification from netc based on vehicle serialNumber",
       required = false) @RequestParam(value = "serialNumber",
           required = false) final String serialNumber,   
   @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
		@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
   throws Exception, APIException {
	User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
	Bank bank = bankDBService.getBank(user.getBankMerchantId());
	if (bank == null) {
		return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
				.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
	}


 return tollServiceBankResource.bankVehicleVerification(user, vehicleRegistrationNo,
     tagId, tid, serialNumber);
} 

@GetMapping("fetch/vin/fastag")
public ResponseEntity getFastagWithVin(
		 @ApiParam(value = "branchCode",
    	 required = true) @QueryParam(value = "branchCode") final String branchCode,
		 @ApiParam(value = "serialNumber",
    	 required = true) @QueryParam(value = "serialNumber") final String serialNumber,
    @ApiParam(value = "Start Time", required = false,
        defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
    @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
        value = "endTime", defaultValue = "2147483646999") Long endTime,
    @ApiParam(value = "Start", required = false,
        defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
    @ApiParam(value = "Count", required = false,
        defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,  
    @ApiParam(value = APIDoc.clientTransactionId, required = true) @QueryParam(
            value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
    @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
			@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
    throws Exception, APIException {
	User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
	Bank bank = bankDBService.getBank(user.getBankMerchantId());
	if (bank == null) {
		return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
				.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
	}

	 return tollServiceBankResource.fetchTollTagsWithVin(user, branchCode, clientTransactionId, startTime, endTime, start, count ,serialNumber);
}
 

@GetMapping("tagRecharge/report")
@ApiOperation(value = "Fetch tag recharge report ",
    notes = "API to Fetch tag recharge report",
    response = TollRechargeResponse.class)
public ResponseEntity getTagRecharge(
    @ApiParam(value = "Start Time", required = false,
        defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
    @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
        value = "endTime", defaultValue = "2147483646999") Long endTime,
    @ApiParam(value = "Start", required = false,
        defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
    @ApiParam(value = "Count", required = false,
        defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
    @ApiParam(value = "Count", required = false, defaultValue = "100") @RequestParam(
        value = "vehicleNumber", required = false) String vehicleNumber,
    @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
	@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
throws Exception, APIException {
	User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
	Bank bank = bankDBService.getBank(user.getBankMerchantId());
	if (bank == null) {
		return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
				.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
	}

  return tollServiceBankResource.getTagRechargeReport(user, bank, startTime, endTime,
      start, count, vehicleNumber);
}

/*
 * toll transaction
 */
@GetMapping("toll/transactions")
@ApiOperation(value = "Bank Transactions",
    notes = "API to fetch bank transactions. Admin User access only.",
    response = BankTransactionsResponse.class)
public ResponseEntity bankTollTransactions(
    @ApiParam(value = "Status", required = false) @RequestParam(value = "statuses",
        required = false) List<Integer> statuses,
    @ApiParam(value = "Start Time", required = false,
        defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
    @ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(
        value = "endTime", defaultValue = "2147483646999") Long endTime,
    @ApiParam(value = "vehicleNumber", required = false) @RequestParam(value = "vehicleNumber",
        required = false) String vehicleNumber,
    @ApiParam(value = "accountNumber", required = false) @RequestParam(value = "accountNumber",
    required = false) String accountNumber,
    @ApiParam(value = "Start", required = false,
        defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
    @ApiParam(value = "Count", required = false,
        defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
    @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") 
	@RequestHeader(value = HttpHeaders.AUTHORIZATION) String authorizationHeader)
throws Exception, APIException {
	User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
	Bank bank = bankDBService.getBank(user.getBankMerchantId());
	if (bank == null) {
		return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
				.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
	}

  return tollServiceBankResource.getTollTransactions(user,
      bank, startTime, endTime, start, count, statuses, vehicleNumber != null 
      ? Arrays.asList(vehicleNumber)
      		: null,accountNumber);
}

}


