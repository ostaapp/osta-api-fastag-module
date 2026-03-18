package com.dipcoin.api.request;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dipcoin.amqp.TollExceptionListListener;
import com.dipcoin.amqp.TollExceptionListRabbitMQRequest;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.NETCAdviceResponse;
import com.dipcoin.api.model.RespVehicleDetails;
import com.dipcoin.api.model.TollResPayResponse;
import com.dipcoin.api.resource.BrontooResource;
import com.dipcoin.api.resource.TollNetcResource;
//import com.dipcoin.api.resource.TollNetcResource;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.partner.toll.commons.TollConstant;
//import com.dipcoin.partner.toll.model.NETCAdviceResponse;
//import com.dipcoin.partner.toll.model.NETCDeclineResponse;
//import com.dipcoin.partner.toll.model.NETCNotificationResponse;
//import com.dipcoin.partner.toll.model.RespVehicleDetails;
//import com.dipcoin.partner.toll.model.TollResPayResponse;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@Api(value = "/jwt/")
@RestController
@RequestMapping(value = "/jwt/",
    consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE},
    produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE, MediaType.ALL_VALUE})
@CrossOrigin
@Timed
public class TollNetcRequestHandlerAuth extends RequestHandler {

  private static final Logger LOG = LogManager.getLogger(TollNetcRequestHandlerAuth.class);

  @Autowired
  private TollNetcResource tollServicesNetcResource;

//  @Autowired
//  private BrontooResource brontooResource;
  
  @Autowired
  private TollNetcResource tollNetcResource;

//  @Autowired
//  private TollDBService tollDBService;  
  
  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;
  
  @Autowired
  private RabbitTemplate tollExceptionListRabbitTemplate;
  
//  @Autowired
//  private RabbitTemplate respVehicleDetalilsRabbitTemplate;


  @PostMapping(value = "getExceptionResponse")
  @ApiOperation(value = "Receive Exception List From Npci",
      notes = "API to Receive Exception List From Npci", response = TollResPayResponse.class)
  public ResponseEntity getExceptionResponse(@RequestBody final String getExceptionResponse)
      throws Exception, APIException {
	  
	  TollExceptionListRabbitMQRequest tollReq = new TollExceptionListRabbitMQRequest();

	    tollReq.setTraceId(httpServletContext.getTraceId());
	    tollReq.setTollExceptionList(getExceptionResponse);

	    tollExceptionListRabbitTemplate.convertAndSend(TollExceptionListListener.EXCHANGE,
	    		TollExceptionListListener.ROUTINGKEY, tollReq);

	    return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
	  
  }

  @PostMapping(value = "reqPayService")
  @ApiOperation(value = "Process toll request for payment",
      notes = "API to handle netc request for payment", response = TollResPayResponse.class)
  public ResponseEntity netcRequestPay(
      @ApiParam(required = true) @RequestBody final String netcReqPay)
      throws Exception, APIException {

    return tollServicesNetcResource.netcRequest(netcReqPay);

  }

  @PostMapping(value = "heartBeat")
  @ApiOperation(value = "Process toll request to check server is alive",
      notes = "API to check server is alive", response = TollResPayResponse.class)
  public ResponseEntity heartBeat(@RequestBody final String heartBeat)
      throws Exception, APIException {

    LOG.debug(LogFormatter.instance().message("Request from NPCI server " + heartBeat).format());
    return ResponseEntity.status(HttpStatus.ACCEPTED).build();
  }

  @PostMapping(value = "queryExceptionResponse")
  @ApiOperation(value = "Receive Queried Exception From Npci",
      notes = "API to Receive Queried Exception From Npci", response = TollResPayResponse.class)
  public ResponseEntity queryException(@RequestBody final String queryExceptionResponse)
      throws Exception, APIException {

	  TollExceptionListRabbitMQRequest tollReq = new TollExceptionListRabbitMQRequest();

	    tollReq.setTraceId(httpServletContext.getTraceId());
	    tollReq.setTollExceptionList(queryExceptionResponse);

	    tollExceptionListRabbitTemplate.convertAndSend(TollExceptionListListener.EXCHANGE,
	    		TollExceptionListListener.ROUTINGKEY, tollReq);

	    return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
  }


  @PostMapping(value = "successManageTagEntry")
  @ApiOperation(value = "Receive Vehicle Details from NPCI",
      notes = "API to Receive Vehicle Details from NPCI", response = NETCAdviceResponse.class)
  public ResponseEntity TollNETCAdviceResponse(@RequestBody final String tollNETCAdvice)
      throws Exception, APIException {

    return tollNetcResource.setvahanResponse(tollNETCAdvice, TollConstant.NETCResponseType.ADVICE.code());
  }

//  @PostMapping(value = "declineManageTagEntry")
//  @ApiOperation(value = "Receive Declined Vehicle Details from NPCI", 
//     notes = "API to Receive Declined Vehicle Details from NPCI", response = NETCDeclineResponse.class)
//  public ResponseEntity NETCDeclineResponse(@RequestBody final String nETCDecline)
//          throws Exception, APIException {
//
//      return tollNetcResource.setvahanResponse(nETCDecline, TollConstant.NETCResponseType.DECLINE.code());
//  }
  
	// RespVehicleDetails

    @GetMapping(value = "RespVehicleDetails")
	@ApiOperation(value = "Receive request vehicle details response from NPCI for VAHAN", notes = "API to Receive request vehicle details response from NPCI for VAHAN", response = RespVehicleDetails.class)
	public ResponseEntity ReqVehicleDetailsResponse(@RequestBody final String respVehicleDetails)
			throws Exception, APIException {

		return tollNetcResource.reqVehicleDetailsResponse(respVehicleDetails);
	}
	
	@PostMapping(value = "NETCNotification")
	@ApiOperation(value = "send ack after receiving NETC Notification", notes = "send ack after receiving NETC Notification", response = RespVehicleDetails.class)
	public ResponseEntity sendAckToNpci(@RequestBody final String notification)
			throws Exception, APIException {

//		return tollNetcResource.sendAckToNpci(bankId);
	  return tollNetcResource.setvahanResponse(notification, TollConstant.NETCResponseType.NOTIFICATION.code());
	}

	@PostMapping(value = "tagEntryServiceResponse")
    @ApiOperation(value = "Receive ReqMngTagEntries response from NPCI for VAHAN", notes = "API to Receive ReqMngTagEntries response from NPCI for VAHAN", response = RespVehicleDetails.class)
    public ResponseEntity RespMngTagEntriesResponse(@RequestBody final String respMngTagEntries)
            throws Exception, APIException {

        return tollNetcResource.respMngTagEntriesResponse(respMngTagEntries);
    }
  
}
