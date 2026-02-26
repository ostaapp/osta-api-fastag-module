package com.dipcoin.api.request;

import java.util.List;
import java.util.Map;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
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
import com.dipcoin.api.model.CustomerDipcoinResponse;
import com.dipcoin.api.model.TollRechargeResponse;
import com.dipcoin.api.model.TollRegistrationResponse;
import com.dipcoin.api.resource.TollCustomerResource;
import com.dipcoin.partner.toll.commons.TollConstant.RegistrationType;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Api(value = "/v1/customer/toll")
@RestController
@RequestMapping(value = "/v1/customer/toll", consumes = { MediaType.APPLICATION_JSON_VALUE,
		MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.ALL_VALUE }, produces = { MediaType.APPLICATION_JSON_VALUE,
				MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.ALL_VALUE })
@CrossOrigin
@Timed
public class TollCustomerRequestHandler {

	@Autowired
	private TollCustomerResource tollServicesResource;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	@PostMapping("register")
	@ApiOperation(value = "Registeration Details for toll services customer", notes = "API to add a new customer account.", response = TollRegistrationResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 201, message = "User registered AND (Failed to send otp sms OR Failed to send verification email)", response = APIResponse.class),
			@ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity registerTollCustomer(@RequestParam("RCImage") MultipartFile[] rcDoc,
			@RequestParam("idProof") MultipartFile idProof, @RequestParam("request") String request,
			@ApiParam(value = APIDoc.clientTransactionId, required = true) @QueryParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.addAndUpdateTollCustomer(httpServletContext.getUser(), null, null, rcDoc, idProof,
				request, RegistrationType.DEFAULT.value(), clientTransactionId);
	}

	@GetMapping
	@ApiOperation(value = "Getting the toll services Customer basis UserId | TagId", notes = "API to get all the Toll Customer basis of UserId | TagId.", response = TollRegistrationResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Bank Code Invalid", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Tag Id Invalid", response = APIResponse.class),
			@ApiResponse(code = 400, message = "User already exists", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity getAllTollCustomerDetails(
			@ApiParam(value = "list based on the Tag id for Vendor Login", required = false) @QueryParam(value = "tagId") final String tagId,
			@ApiParam(value = "list based on the Status for Login", required = false) @QueryParam(value = "status") final String status,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.getTollCustomerDetails(httpServletContext.getUser(), status, tagId);
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
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.getTollCustomerDoc(httpServletContext.getUser(), clientTransactionId, docPath);
	}

	// vehicle verification netc
	@GetMapping("vehicleVerification")
	@ApiOperation(value = "Getting the vehicle Verification from netc", notes = "API to get vehicle verification details from netc", response = ApiResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity tollVehicleVerification(
			@ApiParam(value = APIDoc.clientTransactionId, required = true) @RequestParam(value = APIConstants.CLIENT_TRANSACTION_ID) final String clientTransactionId,
			@ApiParam(value = "vehicle verification from netc based on vehicle registration number", required = false) @RequestParam(value = "registrationNo", required = false) final String vehicleRegistrationNo,
			@ApiParam(value = "vehicle verification from netc based on vehicle registration number", required = false) @RequestParam(value = "vehicleClass", required = false) final String vehicleClass,
			@ApiParam(value = "vehicle verification from netc based on vehicle tagId", required = false) @RequestParam(value = "tagId", required = false) final String tagId,
			@ApiParam(value = "vehicle verification from netc based on vehicle tid", required = false) @RequestParam(value = "tid", required = false) final String tid,
			@ApiParam(value = "cardId", required = false) @RequestParam(value = "cardId", required = true) final Integer cardId,
			@ApiParam(value = "regType", required = false) @RequestParam(value = "regType", required = true) final Integer regType,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.vehicleVerification(httpServletContext.getUser(), clientTransactionId,
				vehicleRegistrationNo, vehicleClass, tagId, tid, cardId, regType);
	}

	@GetMapping("charges")
	@ApiOperation(value = "Getting the vehicle charges based on bankId and vehicle class.", notes = "API to get all charges for vehicle based on bank and vehicle class", response = TollRegistrationResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Vehicle Class Not found", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Bank Not Found", response = APIResponse.class) })
	public ResponseEntity getTollVehicleCharges(
			@ApiParam(value = "bankId for particular vehicle to get the charges", required = false) @QueryParam(value = "cardId") final String cardId,
			@ApiParam(value = "Vehicle Class type", required = false) @QueryParam(value = "vehicleClass") final String vehicleClass,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.getTollVehicleCharges(httpServletContext.getUser(), cardId, vehicleClass,
				RegistrationType.DEFAULT.value());
	}

	@GetMapping("IHMCL/charges")
	@ApiOperation(value = "Getting the Vehicle Charges based on bankId and Vehicle Class for IHMCL.", notes = "API to get all Charges for Vehicle based on bank and vehicle Class for IHMCL", response = TollRegistrationResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Vehicle Class Not found", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Bank Not Found", response = APIResponse.class) })
	public ResponseEntity getTollVehicleChargesforIHMCL(
			@ApiParam(value = "bankId for particular Vehicle to get the charges", required = false) @QueryParam(value = "cardId") final String cardId,
			@ApiParam(value = "Vehicle Class type", required = false) @QueryParam(value = "vehicleClass") final String vehicleClass,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.getTollVehicleCharges(httpServletContext.getUser(), cardId, vehicleClass,
				RegistrationType.IHMCL.value());
	}

	// reqVehicle details this Api is similar to toll vehicle verification api
	// this api is used to get details from vahan system using vin & engineNo or vrn
	// & engineNo combo
	@GetMapping("reqVehicleDetails")
	@ApiOperation(value = "Getting the vehicle Verification from netc", notes = "API to get vehicle verification details from netc", response = ApiResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Toll Customer Id Invalid", response = APIResponse.class),
			@ApiResponse(code = 400, message = "Missing/Invalid request", response = APIResponse.class),
			@ApiResponse(code = 401, message = "Unauthorized User", response = APIResponse.class),
			@ApiResponse(code = 500, message = "Internal Error", response = APIResponse.class) })
	public ResponseEntity reqVehicleDetails(
			@ApiParam(value = "vehicle verification from netc based on vehicle registration number", required = false) @RequestParam(value = "vrn", required = false) final String vrn,
			@ApiParam(value = "vehicle verification from netc based on vehicle vin", required = false) @RequestParam(value = "vin", required = false) final String vin,
			@ApiParam(value = "vehicle verification from netc based on vehicle lastFiveDigitsOfEngineNo", required = false) @RequestParam(value = "lastFiveDigitsOfEngineNo", required = false) final String lastFiveDigitsOfEngineNo,
			@ApiParam(value = "bankReferenceId", required = true) @RequestParam(value = "bankReferenceId") final String bankReferenceId,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.reqVehicleDetails(vrn, vin, lastFiveDigitsOfEngineNo, null, bankReferenceId);
	}

	// toll vehicle verification at Customer side
	@GetMapping("vehicleVerificationStatus")
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
			@ApiParam(value = "vehicle verification from netc based on vehicle serialNumber", required = false) @RequestParam(value = "serialNumber", required = false) final String serialNumber,
			@ApiParam(value = "bankId for particular Vehicle to get the charges", required = false) @QueryParam(value = "cardId") final Integer cardId,
			@ApiParam(value = APIDoc.tokenNotes, required = true, defaultValue = APIDoc.authorizationTokenDefaultValue) @HeaderParam(value = HttpHeaders.AUTHORIZATION) String apiDocPurposeOnly1,
			@ApiParam(value = APIDoc.dcCookieNotes, required = true) @CookieParam(value = APIConstants.DC_LOGIN_COOKIE) String dcl)
			throws Exception, APIException {

		return tollServicesResource.customerVehicleVerificationStatus(httpServletContext.getUser(),
				vehicleRegistrationNo, tagId, tid, serialNumber, RegistrationType.DEFAULT.value(), cardId, true);
	}

}
