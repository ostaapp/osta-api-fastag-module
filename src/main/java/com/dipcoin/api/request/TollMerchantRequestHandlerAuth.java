package com.dipcoin.api.request;

import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.dipcoin.api.model.BankInfoResponse;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.model.UserInfoResponse;
import com.dipcoin.api.resource.TollBankResource;
import com.dipcoin.api.resource.TollCustomerResource;
import com.dipcoin.api.resource.TollMerchantResource;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.User;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * JWT-based duplicate of TollMerchantRequestHandler.
 * Uses Bearer token authentication instead of cookie-based auth.
 * Base path: /v1/jwt/merchant/toll
 */
@Api(value = "/v1/jwt/merchant/toll")
@RestController
@RequestMapping(value = "/v1/jwt/merchant/toll", consumes = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.ALL_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE,
                MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.ALL_VALUE })
@CrossOrigin
@Timed
public class TollMerchantRequestHandlerAuth extends RequestHandler {
	
	@Autowired
	private TollCustomerResource tollServicesResource;

    @Autowired
    private TollBankResource tollServiceBankResource;

    @Autowired
    @Lazy
    private HttpServletContext httpServletContext;

    @Autowired
    private TollMerchantResource tollMerchantResource;
    
    @Autowired
   	private BankDBService bankDBService;

   	@Autowired
   	private UserDBService userDBService;
    
    @GetMapping
   	@ApiOperation(value = "Get Tag based on merchnat and other filters", notes = "API to get  Toll Customer based on  Merchant and other filters", response = TollRegistrationResponse.class)
   	@ApiResponses(value = { @ApiResponse(code = 400, message = "Bank Code Invalid", response = APIResponse.class),
   			@ApiResponse(code = 400, message = "Tag Id Invalid", response = APIResponse.class),
   			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
   			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
   			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
   	public ResponseEntity getAllTollCustomerDetails(
   			@ApiParam(value = "phone", required = false) @QueryParam(value = "phone") final String phone,
   			@ApiParam(value = "list based on the status", required = false) @QueryParam(value = "status") final String status,
   			@ApiParam(value = "Start Time", required = false, defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
   			@ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(value = "endTime", defaultValue = "2147483646999") Long endTime,
   			@ApiParam(value = "vehicleNumber", required = false) @RequestParam(value = "vehicleNumber", required = false) String vehicleNumber,
   			@ApiParam(value = "Start", required = false, defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
   			@ApiParam(value = "Count", required = false, defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
   		 @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
   	            throws Exception, APIException {

   		return tollMerchantResource.getTollCustomerDetails(httpServletContext.getUser(), status, startTime, endTime,
   				start, count, vehicleNumber, phone);
   	}

    @PutMapping
    @ApiOperation(value = "Toll Customer Update Account By Bank (JWT)", notes = "API to update existing Toll Customer account By Bank.", response = APIResponse.class)
    public ResponseEntity updateTollCustomerDetails(
            @ApiParam(value = "Toll Tag details", required = true) @RequestBody final com.dipcoin.api.model.TollTagRequest updateReq,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {
    	 User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
  		Bank bank = bankDBService.getBank(user.getBankMerchantId());
  		if (bank == null) {
  			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
  					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
  		}
        return tollServiceBankResource.updateTollCustomerByBank(user,bank, updateReq, clientTransactionId);
    }

    @PutMapping("update")
    @ApiOperation(value = "Update Address and Vehicle Number By Bank (JWT)", notes = "API to update Address and Vehicle Number by Bank.", response = APIResponse.class)
    public ResponseEntity updateAddressAndVehicleNoByBank(
            @ApiParam(value = "Toll Tag details", required = true) @RequestBody final com.dipcoin.api.model.TollTagRequest updateReq,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {
    	
    	 User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
 		Bank bank = bankDBService.getBank(user.getBankMerchantId());
 		if (bank == null) {
 			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
 					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
 		}

        return tollServiceBankResource.updateAddressAndVehicleNoByBank(user,bank, updateReq, clientTransactionId);
    }

    @PutMapping("tag")
    @ApiOperation(value = "Toll Customer Update Account By Bank Tag Vendor (JWT)", notes = "API to update existing Toll Customer account By Vendor.", response = APIResponse.class)
    public ResponseEntity updateTollCustomerTagDetails(
            @ApiParam(value = "Toll Tag Details", required = true) @RequestBody final com.dipcoin.api.model.TollTagRequest updateReq,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {
    	
    	User user = userDBService.getUsersByIds(java.util.Arrays.asList(httpServletContext.getUser().getId())).get(0);
 		Bank bank = bankDBService.getBank(user.getBankMerchantId());
 		if (bank == null) {
 			return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
 					.body(APIResponse.error(com.dipcoin.api.commons.HeaderCode.USER_UNAUTHORIZED));
 		}

        return tollServiceBankResource.updateTollCustomerByVendor(user,bank, updateReq, clientTransactionId);
    }
    
    // Get Toll customer Doc based on docpath
 	@GetMapping("doc")
 	@ApiOperation(value = "Getting the toll Customer Document based on doc path", notes = "API to get Toll Customer Doc based on doc path", response = TollRegistrationResponse.class)
 	@ApiResponses(value = {
 			@ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
 			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
 			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
 			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
 	public ResponseEntity getTollCustomerDoc(
 			@ApiParam(value = APIDoc.clientTransactionId, required = true) @QueryParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
 			@ApiParam(value = "Get Toll Customer info based on Toll customer id ", required = false) @QueryParam(value = "docPath") final String docPath,
 			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
 		            throws Exception, APIException {

 		return tollServicesResource.getTollCustomerDoc(httpServletContext.getUser(), clientTransactionId, docPath);
 	}

 	@GetMapping("tag/charges")
 	@ApiOperation(value = "Getting the vehicle charges based on bankRefId and vehicle class.", notes = "API to get all charges for vehicle based on bank and vehicle class", response = TollRegistrationResponse.class)
 	@ApiResponses(value = { @ApiResponse(code = 400, message = "Vehicle Class Not found", response = APIResponse.class),
 			@ApiResponse(code = 400, message = "Bank Not Found", response = APIResponse.class) })
 	public ResponseEntity getTollVehicleCharges(
 			@ApiParam(value = "bankId for particular vehicle to get the charges", required = false) @RequestParam(value = "bankRefId") final String bankRefId,
 			@ApiParam(value = "Vehicle Class type", required = false) @RequestParam(value = "vehicleClass") final String vehicleClass,
 			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
 		            throws Exception, APIException {

 		return tollMerchantResource.getTollVehicleCharges(httpServletContext.getUser(), bankRefId, vehicleClass,
 				RegistrationType.DEFAULT.value());
 	}

    @PostMapping("tag/register")
    @ApiOperation(value = "Registration Details for tollServices Customer (JWT)", notes = "API to add a new customer account.", response = TollRegistrationResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
            @ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity registerTollCustomer(@RequestParam("RCImage") MultipartFile[] rcDoc,
            @RequestParam("idProof") MultipartFile idProof, @RequestParam("request") String request,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        ResponseEntity addTagResponse = tollMerchantResource.addAndUpdateTollCustomer(httpServletContext.getUser(),
                rcDoc, idProof, request);

        if (addTagResponse.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
            return addTagResponse;
        }

        Map<String, Object> objectLookUp = (Map<String, Object>) addTagResponse.getBody();

        ResponseEntity addFileReponse = tollMerchantResource.addFile(httpServletContext.getUser(), rcDoc, idProof,
                request, objectLookUp);

        ResponseEntity addMoney = tollMerchantResource.addMoney(httpServletContext.getUser(), rcDoc, idProof, request,
                objectLookUp);

        if (addMoney.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
            return addMoney;
        }

        ResponseEntity createOsta = tollMerchantResource.createOsta(httpServletContext.getUser(), rcDoc, idProof,
                request, objectLookUp);

        if (createOsta.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
            return createOsta;
        }

        return tollMerchantResource.updateSerialNumber(httpServletContext.getUser(), rcDoc, idProof, request,
                objectLookUp);
    }
    
    @GetMapping("bank/list")
 	@ApiOperation(value = "API to Fetch tag bank list", notes = "API to Fetch tag bank list", response = BankInfoResponse.class)
 	@ApiResponses(value = { @ApiResponse(code = 200, message = "get bank list", response = APIResponse.class),
 			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
 			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
 			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
 	public ResponseEntity getBankList(
 			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
 		            throws Exception, APIException {

 		return tollMerchantResource.getBanks(httpServletContext.getUser());

 	}

 //vehicle verification netc
 	@GetMapping("vehicleVerification")
 	@ApiOperation(value = "Getting the vehicle Verification from netc", notes = "API to get vehicle verification details from netc", response = ApiResponse.class)
 	@ApiResponses(value = {
 			@ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
 			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
 			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
 			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
 	public ResponseEntity tollVehicleVerification(
 			@ApiParam(value = "vehicle verification from netc based on vehicle registration number", required = false) @RequestParam(value = "registrationNo", required = false) final String vehicleRegistrationNo,
 			@ApiParam(value = "vehicle verification from netc based on vehicle tagId", required = false) @RequestParam(value = "tagId", required = false) final String tagId,
 			@ApiParam(value = "vehicle verification from netc based on vehicle tid", required = false) @RequestParam(value = "tid", required = false) final String tid,
 			@ApiParam(value = "bankReferenceId", required = false) @RequestParam(value = "bankReferenceId", required = true) final String bankReferenceId,
 			@ApiParam(value = "regType", required = false) @RequestParam(value = "regType", required = true) final Integer regType,
 			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
 		            throws Exception, APIException {

 		return tollServiceBankResource.vehicleVerification(httpServletContext.getUser(), vehicleRegistrationNo, tagId,
 				tid, regType, bankReferenceId);
 	}

 	@GetMapping("tags")
 	@ApiOperation(value = "Get Tags Details based on Merchant", notes = "API to Fetch Tag Details based on Bank", response = APIResponse.class)
 	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid Account", response = APIResponse.class),
 			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
 			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
 			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
 	public ResponseEntity getTags(
 			@ApiParam(value = "Start Time", required = false, defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
 			@ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(value = "endTime", defaultValue = "2147483646999") Long endTime,
 			@ApiParam(value = "Start", required = false, defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
 			@ApiParam(value = "Count", required = false, defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
 			@ApiParam(value = "Category", required = false) @RequestParam(value = "category", required = false) String category,
 			@ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
 			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
 		            throws Exception, APIException {

 		return tollServiceBankResource.getTags(httpServletContext.getUser(), clientTransactionId, startTime, endTime,
 				start, count, category);

 	}

 	@GetMapping("tagCounts/summary")
 	public ResponseEntity getTagsRejectedPendingCounts(			
 			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
 		            throws Exception, APIException {
 		return tollMerchantResource.getTagsRejectedPendingCounts(httpServletContext.getUser());
 	}

    @PutMapping("update/phone")
    @ApiOperation(value = "Merchant User Update Phone Number (JWT)", notes = "API to update toll user's phone number.", response = UserInfoResponse.class)
    public ResponseEntity updatePhoneNumber(
            @ApiParam(value = "id", required = false) @RequestParam(value = "id") Integer id,
            @ApiParam(value = "phoneNumber", required = false) @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollMerchantResource.updatePhoneNumber(httpServletContext.getUser(), id, phoneNumber);
    }
    
    @GetMapping("verify/registered")
   	@ApiOperation(value = "Check whether customer is already registered or not", notes = "API to Check whether customer is already registered with same phone no or not", response = APIResponse.class)
   	public ResponseEntity verifyCustomerRegistered(
   			@ApiParam(value = "phoneNumber", required = false) @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
   			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
   		            throws Exception, APIException {

   		return tollMerchantResource.verifyCustomerRegistered(httpServletContext.getUser(), phoneNumber);
   	}

   	@GetMapping("bulk/tag/register/download-sample")
   	@ApiOperation(value = "Download sample bulk registration excel file", notes = "API to to download sample template for Bulk Fastag registration", response = APIResponse.class)
   	public ResponseEntity downloadBulkRegistrationSampleFile(
   			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
   		            throws Exception, APIException {

   	    return tollMerchantResource.downloadBulkRegistrationSampleFile(httpServletContext.getUser());
   	}
}
