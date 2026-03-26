package com.dipcoin.api.request;

import java.util.Map;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.dipcoin.api.model.CustomerDipcoinResponse;
import com.dipcoin.api.model.TollRechargeResponse;
import com.dipcoin.api.model.TollRegistrationRequest;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.resource.CustomerDipcoinResource;
import com.dipcoin.api.resource.TollCustomerResource;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * JWT-based duplicate of TollCustomerRequestHandler.
 * Uses Bearer token authentication instead of cookie-based auth.
 * Base path: /v1/jwt/customer/toll
 */
@Api(value = "/v1/jwt/customer/toll")
@RestController
@RequestMapping(value = "/v1/jwt/customer/toll", consumes = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.ALL_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE,
                MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.ALL_VALUE })
@CrossOrigin
@Timed
public class TollCustomerRequestHandlerAuth {

    @Autowired
    private TollCustomerResource tollServicesResource;
    
    @Autowired
    private CustomerDipcoinResource customerDipcoinResource;

    @Autowired
    @Lazy
    private HttpServletContext httpServletContext;

    @PostMapping("register")
    @ApiOperation(value = "Registeration Details for toll services customer (JWT)", notes = "API to add a new customer account.", response = TollRegistrationResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
            @ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity registerTollCustomer(@RequestParam("RCImage") MultipartFile[] rcDoc,
            @RequestParam("idProof") MultipartFile idProof, @RequestParam("request") String request,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {
    	
    	 System.out.println("rcDoc: " + rcDoc);
    	    System.out.println("rcDoc length: " + (rcDoc != null ? rcDoc.length : "NULL"));
    	    System.out.println("idProof: " + idProof);


        return tollServicesResource.addAndUpdateTollCustomer(httpServletContext.getUser(), null, null, rcDoc, idProof,
                request, RegistrationType.DEFAULT.value(), clientTransactionId);
    }

    @PostMapping("register/apk")
    @ApiOperation(value = "Registeration Details for toll services customer from apk (JWT)", notes = "API to add a new customer account from apk.", response = TollRegistrationResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
            @ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity registerTollCustomerMobile(@RequestBody String request,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {
        return tollServicesResource.addAndUpdateTollCustomer(httpServletContext.getUser(), null, null, request,
                RegistrationType.DEFAULT.value(), clientTransactionId);
    }
    
	/*
	 * toll wallet registration amount lien mark
	 */
	@PostMapping("wallet/registration/payment")
	@ApiOperation(value = "Virtual bank registration amount lien mark", notes = "API to lien mark wallet virtual account", response = BankTransactionsResponse.class)
	public ResponseEntity walletRecharge(
			@ApiParam(value = "Toll Recharge details", required = true) @RequestBody final TollRegistrationRequest createReq,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception {

		return tollServicesResource.walletRecharge(httpServletContext.getUser(), createReq,
				RegistrationType.WALLET.value());
	}

    @PutMapping("register")
    @ApiOperation(value = "Toll Customer Update Account (JWT)", notes = "API to update existing Toll Customer account.", response = APIResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
            @ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity updateTollCustomer(@RequestParam("RCImage") MultipartFile[] rcDoc,
            @RequestParam("idProof") MultipartFile idProof, @RequestParam("request") String request,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.addAndUpdateTollCustomer(httpServletContext.getUser(), null, null, rcDoc, idProof,
                request, RegistrationType.DEFAULT.value(), clientTransactionId);
    }
    
	@PostMapping("updateRegistrationNumber")
	@ApiOperation(value = "Updated Registeration Details for toll services customer", notes = "API to update the registration number, response = TollTag.class")
	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
			@ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity updateTollCustomer(@RequestParam("RCImage") MultipartFile rcImg,
			@RequestParam("vehicleImg") MultipartFile vehicleImg, @RequestParam("vinNumber") String vinNumber,
			@RequestParam("serialNumber") String serialNumber,
			@RequestParam("registrationNumber") String registrationNumber,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.updateRegistrationNumberAndAddImages(httpServletContext.getUser(), vinNumber,
				serialNumber, registrationNumber, rcImg, vehicleImg);
	}

    @DeleteMapping("{tcid:.*}")
    @ApiOperation(value = "Toll Customer Delete Account (JWT)", notes = "API to delete toll customer account.", response = APIResponse.class)
    public ResponseEntity deleteAccount(
            @ApiParam(value = "Toll registration Id", required = true) @RequestParam(value = "tcid") final String encryptedTCID,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.deleteTollCustomerAccount(httpServletContext.getUser(), encryptedTCID);
    }

    @GetMapping
    @ApiOperation(value = "Getting the toll services Customer basis UserId | TagId (JWT)", notes = "API to get all the Toll Customer basis of UserId | TagId.", response = TollRegistrationResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Bank Code Invalid", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Tag Id Invalid", response = APIResponse.class),
            @ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity getAllTollCustomerDetails(
            @ApiParam(value = "list based on the Tag id for Vendor Login", required = false) @RequestParam(value = "tagId", required = false) final String tagId,
            @ApiParam(value = "list based on the Status for Login", required = false) @RequestParam(value = "status", required = false) final String status,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.getTollCustomerDetails(httpServletContext.getUser(), status, tagId);
    }

    @GetMapping("doc")
    @ApiOperation(value = "Getting the toll Customer Document based on doc path (JWT)", notes = "API to get Toll Customer Doc based on doc path", response = TollRegistrationResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity getTollCustomerDoc(
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "Get Toll Customer info based on Toll customer id", required = false) @RequestParam(value = "docPath", required = false) final String docPath,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.getTollCustomerDoc(httpServletContext.getUser(), clientTransactionId, docPath);
    }

    @GetMapping("vehicleVerification")
    @ApiOperation(value = "Getting the vehicle Verification from netc (JWT)", notes = "API to get vehicle verification details from netc", response = ApiResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity tollVehicleVerification(
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "vehicle verification from netc based on vehicle registration number", required = false) @RequestParam(value = "registrationNo", required = false) final String vehicleRegistrationNo,
            @ApiParam(value = "vehicle verification from netc based on vehicle vehicleClass", required = false) @RequestParam(value = "vehicleClass", required = false) final String vehicleClass,
            @ApiParam(value = "vehicle verification from netc based on vehicle tagId", required = false) @RequestParam(value = "tagId", required = false) final String tagId,
            @ApiParam(value = "vehicle verification from netc based on vehicle tid", required = false) @RequestParam(value = "tid", required = false) final String tid,
            @ApiParam(value = "cardId", required = true) @RequestParam(value = "cardId") final Integer cardId,
            @ApiParam(value = "regType", required = true) @RequestParam(value = "regType") final Integer regType,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.vehicleVerification(httpServletContext.getUser(), clientTransactionId,
                vehicleRegistrationNo, vehicleClass, tagId, tid, cardId, regType);
    }

    @GetMapping("charges")
    @ApiOperation(value = "Getting the vehicle charges based on bankId and vehicle class (JWT)", notes = "API to get all charges for vehicle based on bank and vehicle class", response = TollRegistrationResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Vehicle Class Not found", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Bank Not Found", response = APIResponse.class) })
    public ResponseEntity getTollVehicleCharges(
            @ApiParam(value = "bankId for particular vehicle to get the charges", required = false) @RequestParam(value = "cardId", required = false) final String cardId,
            @ApiParam(value = "Vehicle Class type", required = false) @RequestParam(value = "vehicleClass", required = false) final String vehicleClass,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.getTollVehicleCharges(httpServletContext.getUser(), cardId, vehicleClass,
                RegistrationType.DEFAULT.value());
    }

    @GetMapping("IHMCL/charges")
    @ApiOperation(value = "Getting the Vehicle Charges based on bankId and Vehicle Class for IHMCL (JWT)", notes = "API to get all Charges for Vehicle based on bank and vehicle Class for IHMCL", response = TollRegistrationResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Vehicle Class Not found", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Bank Not Found", response = APIResponse.class) })
    public ResponseEntity getTollVehicleChargesforIHMCL(
            @ApiParam(value = "bankId for particular Vehicle to get the charges", required = false) @RequestParam(value = "cardId", required = false) final String cardId,
            @ApiParam(value = "Vehicle Class type", required = false) @RequestParam(value = "vehicleClass", required = false) final String vehicleClass,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.getTollVehicleCharges(httpServletContext.getUser(), cardId, vehicleClass,
                RegistrationType.IHMCL.value());
    }

    @GetMapping("reqVehicleDetails")
    @ApiOperation(value = "Getting the vehicle details from Vahan (JWT)", notes = "API to get vehicle details from Vahan system using vin/vrn & engineNo combo", response = ApiResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity reqVehicleDetails(
            @ApiParam(value = "vehicle registration number", required = false) @RequestParam(value = "vrn", required = false) final String vrn,
            @ApiParam(value = "vehicle vin", required = false) @RequestParam(value = "vin", required = false) final String vin,
            @ApiParam(value = "last five digits of engine number", required = false) @RequestParam(value = "lastFiveDigitsOfEngineNo", required = false) final String lastFiveDigitsOfEngineNo,
            @ApiParam(value = "bankReferenceId", required = true) @RequestParam(value = "bankReferenceId") final String bankReferenceId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.reqVehicleDetails(vrn, vin, lastFiveDigitsOfEngineNo, null, bankReferenceId);
    }

    @GetMapping("vehicleVerificationStatus")
    @ApiOperation(value = "Getting the vehicle Verification Status from netc (JWT)", notes = "API to get vehicle verification status from netc", response = ApiResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
            @ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
            @ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
    public ResponseEntity tollVehicleVerificationStatus(
            @ApiParam(value = "vehicle registration number", required = false) @RequestParam(value = "registrationNo", required = false) final String vehicleRegistrationNo,
            @ApiParam(value = "vehicle tagId", required = false) @RequestParam(value = "tagId", required = false) final String tagId,
            @ApiParam(value = "vehicle tid", required = false) @RequestParam(value = "tid", required = false) final String tid,
            @ApiParam(value = "vehicle serialNumber", required = false) @RequestParam(value = "serialNumber", required = false) final String serialNumber,
            @ApiParam(value = "bankId for particular Vehicle", required = false) @RequestParam(value = "cardId", required = false) final Integer cardId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServicesResource.customerVehicleVerificationStatus(httpServletContext.getUser(),
                vehicleRegistrationNo, tagId, tid, serialNumber, RegistrationType.DEFAULT.value(), cardId, true);
    }
    
	/*
	 * toll transaction
	 */
	@GetMapping("transactions")
	@ApiOperation(value = "Bank Transactions", notes = "API to fetch bank transactions. Admin User access only.", response = BankTransactionsResponse.class)
	public ResponseEntity bankTollTransactions(
			@ApiParam(value = "Start Time", required = false, defaultValue = "0") @RequestParam(value = "startTime", defaultValue = "0") Long startTime,
			@ApiParam(value = "End Time", required = false, defaultValue = "2147483646999") @RequestParam(value = "endTime", defaultValue = "2147483646999") Long endTime,
			@ApiParam(value = "vehicleNumber", required = false) @RequestParam(value = "vehicleNumber", required = false) String vehicleNumber,
			@ApiParam(value = "Start", required = false, defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
			@ApiParam(value = "Count", required = false, defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count,
			@ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
			throws Exception {
		
		System.out.println("Received request for toll transactions with parameters - startTime: " + startTime + ", endTime: " + endTime + ", vehicleNumber: " + vehicleNumber + ", start: " + start + ", count: " + count);

		return tollServicesResource.getTollTransactions(httpServletContext.getUser(), startTime, endTime, start, count,
				vehicleNumber);
	}
	
	@PostMapping("add/tollTag/{serialNumber}")
	@ApiOperation(value = "Api to insert TollTag details in CustomerAccount Table", notes = "API to insert data in Customer Account table")
	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
			@ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity addDataInCustomerAccountTable(
			@ApiParam(value = "Toll Up Details", required = true) @RequestBody final TollRegistrationRequest addDetails,
			@ApiParam(value = "Serial Number", required = true) @PathVariable("serialNumber") final String serialNo,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.addTopUpDetails(httpServletContext.getUser(), addDetails, serialNo);
	}
	
	@GetMapping("minimumAmount")
	@ApiOperation(value = "Fetch the minimum amount", notes = "API to Fetch minimum amount.", response = TollRechargeResponse.class)
	public ResponseEntity getMinimumAmount(
			@ApiParam(value = "User CardId", required = true) @RequestParam(value = "userCardId", required = true) final Integer userCardId,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.getSummationOfMinimumAmount(httpServletContext.getUser(), userCardId);
	}
	
	@GetMapping("disputeOptions")
	@ApiOperation(value = "Fetch function codes & Reason codes", notes = "API to Fetch function codes & Reason codes based on PreRequisite Code", response = TollRegistrationResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "PreRequisite Codes Not Found", response = APIResponse.class) })
	public ResponseEntity getDisputeOptions(
			@ApiParam(value = "Pre-Requisite Code", required = true) @RequestParam(value = "preRequisiteCode", required = false) final Integer preRequisiteCode,
			@ApiParam(value = "Function Codes", required = true) @RequestParam(value = "functionCodes", required = false) final Integer functionCodes,
			@ApiParam(value = "Fetch All Function codes", required = true) @RequestParam(value = "fetchAll", required = false) final boolean fetchAll,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.getDisputeOptions(httpServletContext.getUser(), preRequisiteCode, functionCodes,
				fetchAll);
	}
	
	// this api works without login
	@GetMapping("ihmclBank")
	@ApiOperation(value = "Customer get IHMCL bank list and encrypted data if bank shortCode provided", notes = "API to get ihmcl bank list and encrypted data if bank shortCode provided for routing to ihmcl site", response = Map.class)
	public ResponseEntity getIhmclBank(
			@ApiParam(value = "bankShortCode", required = false) @RequestParam(value = "bankShortCode", required = false) final String bankShortCode)
			throws Exception {

		return tollServicesResource.getIhmclBank(bankShortCode);
	}
	
	@PostMapping("IHMCL/register")
	@ApiOperation(value = "Registration API for toll for IHMCL customers.", notes = "API to register a new fastag customer.", response = TollRegistrationResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
			@ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity registerBankCustomer(@RequestParam("RCImage") MultipartFile[] rcDoc,
			@RequestParam("idProof") MultipartFile idProof, @RequestParam("request") String request,
			@ApiParam(value = APIDoc.clientTransactionId, required = true) @QueryParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.addAndUpdateTollCustomer(httpServletContext.getUser(), null, null, rcDoc, idProof,
				request, RegistrationType.IHMCL.value(), clientTransactionId);
	}
	
	@PutMapping("tag/{ttid:.*}")
	@ApiOperation(value = "Toll Tag Customer Activation", notes = "API to activate toll tag from customer.", response = APIResponse.class)
	public ResponseEntity customerTollTagActivation(
			@ApiParam(value = "Toll Tag Id", required = true) @PathVariable("ttid") final String encryptedTTID,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.customerTollTagActivation(httpServletContext.getUser(), encryptedTTID, null,
				RegistrationType.DEFAULT.value());
	}
	
	@GetMapping("heirarchy/{cardId:.*}")
	@ApiOperation(value = "Customer Get fastag Osta hierarchy", notes = "API to get customer Osta.", response = CustomerDipcoinResponse.class)
	public ResponseEntity getTollHierarchy(
			@ApiParam(value = "cardId", required = true) @PathVariable("cardId") final String encryptedCarId,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception {

		return customerDipcoinResource.getTollDipcoinHierarchy(httpServletContext.getUser(), encryptedCarId, false);
	}
	
	@DeleteMapping("osta/{cdid:.*}")
	@ApiOperation(value = "Delete Toll Osta", notes = "API to Release toll Osta", response = APIResponse.class)
	public ResponseEntity deleteTollOsta(
			@ApiParam(value = "card Id", required = true) @PathVariable("cdid") final String cardId,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @RequestHeader(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieValue(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.deleteTollOsta(httpServletContext.getUser(), cardId);
	}
	
	@DeleteMapping("tag/{ttid:.*}")
	@ApiOperation(value = "Toll Tag Deactivate Account", notes = "API to Deactivate toll tag account.", response = APIResponse.class)
	public ResponseEntity deactivateTollTag(
			@ApiParam(value = "Toll Tag Id", required = true) @PathVariable("ttid") final String encryptedTTID,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.deactivateTollTagByCustomer(httpServletContext.getUser(), encryptedTTID);
	}
	
}
