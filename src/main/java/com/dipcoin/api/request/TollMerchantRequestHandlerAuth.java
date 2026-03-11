package com.dipcoin.api.request;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.dipcoin.api.model.UserInfoResponse;
import com.dipcoin.api.resource.TollBankResource;
import com.dipcoin.api.resource.TollMerchantResource;

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
    private TollBankResource tollServiceBankResource;

    @Autowired
    @Lazy
    private HttpServletContext httpServletContext;

    @Autowired
    private TollMerchantResource tollMerchantResource;

    @PutMapping
    @ApiOperation(value = "Toll Customer Update Account By Bank (JWT)", notes = "API to update existing Toll Customer account By Bank.", response = APIResponse.class)
    public ResponseEntity updateTollCustomerDetails(
            @ApiParam(value = "Toll Tag details", required = true) @RequestBody final com.dipcoin.api.model.TollTagRequest updateReq,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServiceBankResource.updateTollCustomerByBank(httpServletContext.getUser(),
                httpServletContext.getBank(), updateReq, clientTransactionId);
    }

    @PutMapping("update")
    @ApiOperation(value = "Update Address and Vehicle Number By Bank (JWT)", notes = "API to update Address and Vehicle Number by Bank.", response = APIResponse.class)
    public ResponseEntity updateAddressAndVehicleNoByBank(
            @ApiParam(value = "Toll Tag details", required = true) @RequestBody final com.dipcoin.api.model.TollTagRequest updateReq,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServiceBankResource.updateAddressAndVehicleNoByBank(httpServletContext.getUser(),
                httpServletContext.getBank(), updateReq, clientTransactionId);
    }

    @PutMapping("tag")
    @ApiOperation(value = "Toll Customer Update Account By Bank Tag Vendor (JWT)", notes = "API to update existing Toll Customer account By Vendor.", response = APIResponse.class)
    public ResponseEntity updateTollCustomerTagDetails(
            @ApiParam(value = "Toll Tag Details", required = true) @RequestBody final com.dipcoin.api.model.TollTagRequest updateReq,
            @ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollServiceBankResource.updateTollCustomerByVendor(httpServletContext.getUser(),
                httpServletContext.getBank(), updateReq, clientTransactionId);
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

    @PutMapping("update/phone")
    @ApiOperation(value = "Merchant User Update Phone Number (JWT)", notes = "API to update toll user's phone number.", response = UserInfoResponse.class)
    public ResponseEntity updatePhoneNumber(
            @ApiParam(value = "id", required = false) @RequestParam(value = "id") Integer id,
            @ApiParam(value = "phoneNumber", required = false) @RequestParam(value = "phoneNumber", required = false) String phoneNumber,
            @ApiParam(value = "JWT Access Token - Format: Bearer {access_token}", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIs...") @RequestHeader(value = org.springframework.http.HttpHeaders.AUTHORIZATION) String authorizationHeader)
            throws Exception, APIException {

        return tollMerchantResource.updatePhoneNumber(httpServletContext.getUser(), id, phoneNumber);
    }
}
