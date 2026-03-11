package com.dipcoin.api.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.dipcoin.api.model.TollRegistrationResponse;
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
}
