package com.dipcoin.api.request;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;

import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.BankInfoResponse;
import com.dipcoin.api.resource.BankResource;

import org.springframework.http.MediaType;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(value = "/v1/jwt/bank")
@RestController
@RequestMapping(value = "/v1/bank",
    consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
@CrossOrigin
@Timed
public class BankRequestHandler extends RequestHandler{
	
	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;
	
	@Autowired
	private BankResource bankResource;
	
	/*
	 * Get banks info list
	 */
	@GetMapping("list")
	@ApiOperation(value = "Get Banks", notes = "Get list of banks", response = BankInfoResponse.class)
	public ResponseEntity getBanks(
			@ApiParam(value = "Filter by status.  Admin User access only.") @RequestParam(value = "status", required = false) final Integer status,
			@ApiParam(value = "Filter by type.") @RequestParam(value = "type", required = false) final Integer type,
			@ApiParam(value = "start", required = false, defaultValue = "0") @RequestParam(value = "start", defaultValue = "0") Integer start,
			@ApiParam(value = "count", required = false, defaultValue = "100") @RequestParam(value = "count", defaultValue = "100") Integer count)
			throws Exception {

		return bankResource.getBanks(httpServletContext.getUser(), status, type, start, count);
	}

}