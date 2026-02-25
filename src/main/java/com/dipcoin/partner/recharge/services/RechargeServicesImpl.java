
package com.dipcoin.partner.recharge.services;

import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import com.dipcoin.api.utils.RechargeConfigurations;
import com.dipcoin.api.utils.RechargeConstants.RechargeResponseStatus;
import com.dipcoin.api.utils.RechargeServiceException;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.client.RechargeClient;
import com.dipcoin.partner.recharge.client.RechargeClient.OperationType;
import com.dipcoin.partner.recharge.client.RechargeClientFactory;
import com.dipcoin.partner.recharge.comm.BillComplaintStatusRequest;
import com.dipcoin.partner.recharge.comm.BillInfoRequest;
import com.dipcoin.partner.recharge.comm.BillInfoResponse;
import com.dipcoin.partner.recharge.comm.BillMerchantValidationDetails;
import com.dipcoin.partner.recharge.comm.BillPaymentServiceRequest;
import com.dipcoin.partner.recharge.comm.BillPaymentServiceResponse;
import com.dipcoin.partner.recharge.comm.BillPaymentValidateRequest;
import com.dipcoin.partner.recharge.comm.BillPaymentValidateResponse;
import com.dipcoin.partner.recharge.comm.BillRegisterComplaintRequest;
import com.dipcoin.partner.recharge.comm.BillRegisterComplaintResponse;
import com.dipcoin.partner.recharge.comm.BillStatusRequest;
import com.dipcoin.partner.recharge.comm.BillStatusResponse;
import com.dipcoin.partner.recharge.comm.MDMRequest;
import com.dipcoin.partner.recharge.comm.MDMResponse;
import com.dipcoin.partner.recharge.comm.RechargeBalanceRequest;
import com.dipcoin.partner.recharge.comm.RechargeBalanceResponse;
import com.dipcoin.partner.recharge.comm.RechargeJioValidateRequest;
import com.dipcoin.partner.recharge.comm.RechargeJioValidateResponse;
import com.dipcoin.partner.recharge.comm.RechargePlanRequest;
import com.dipcoin.partner.recharge.comm.RechargePlanResponse;
import com.dipcoin.partner.recharge.comm.RechargeServiceRequest;
import com.dipcoin.partner.recharge.comm.RechargeServiceResponse;
import com.dipcoin.partner.recharge.comm.RechargeStatusRequest;
import com.dipcoin.partner.recharge.comm.RechargeStatusResponse;
import com.dipcoin.partner.recharge.comm.RechargeValidateRequest;
import com.dipcoin.partner.recharge.comm.RechargeValidateResponse;
import com.dipcoin.partner.utils.PartnerRequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("rechargeServices")
public class RechargeServicesImpl implements RechargeServices {

  private static final Logger LOG = LogManager.getLogger(RechargeServicesImpl.class);
  public static ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private RechargeClientFactory rechargeClientFactory;

  @Autowired
  private RechargeConfigurations rechargeConfigurations;

  private String clientLookupKey(String merchantId, OperationType operationType) {
    return operationType.value();
  }

  @Override
  public Future<RechargeValidateResponse> validateRecharge(PartnerRequestContext requestContext,
      RechargeValidateRequest request) throws RechargeServiceException {

    LOG.debug(LogFormatter.instance(requestContext.getTraceId()).message("Validating the request")
        .format());

    request.setRequestType(OperationType.VALIDATE_RECHARGE.value());

    RechargeValidateResponse errorResp = new RechargeValidateResponse();
    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        RechargeValidateResponse response = client.processRequest(requestContext, request);

        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            LOG.info(LogFormatter.instance(requestContext.getTraceId())
                .data("Recharge Validation Response has failed" , response).format());
            errorResp = (RechargeValidateResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }

        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .message("RechargeValidateResponse is successful" + response).format());
        return new AsyncResult<>(response);
      }
    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    // return errorResp;
    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<RechargeServiceResponse> processRecharge(PartnerRequestContext requestContext,
      RechargeServiceRequest request) throws RechargeServiceException {

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Requested for recharge service").format());

    request.setRequestType(OperationType.SERVICE_RECHARGE.value());

    RechargeServiceResponse errorResp = new RechargeServiceResponse();
    try {

      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        RechargeServiceResponse response = client.processRequest(requestContext, request);

        if (response != null) {
          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
        	  LOG.info(LogFormatter.instance(requestContext.getTraceId())
        	            .data("RechargeServiceResponse " , response).format());

            errorResp = (RechargeServiceResponse) response.clone();
            return new AsyncResult<>(errorResp);

          }
        }

        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("RechargeServiceResponse " , response).format());

        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<BillInfoResponse> getBillInfo(PartnerRequestContext requestContext,
      BillInfoRequest billInfoRequest) throws RechargeServiceException {
    LOG.debug(LogFormatter.instance(requestContext.getTraceId()).message("Validating the request")
        .format());

    billInfoRequest.setRequestType(OperationType.VALIDATE_RECHARGE.value());

    BillInfoResponse errorResp = new BillInfoResponse();
    try {
      // validate incoming request
      if (billInfoRequest == null || !billInfoRequest.validate()) {
        String desc = "Request parameters are invalid " + billInfoRequest.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      LOG.info(LogFormatter.instance(billInfoRequest.toString())
          .data("BillRequest", billInfoRequest).message("Bill Request"));

      String rechargeClientName = clientLookupKey(billInfoRequest.getMerchantReferenceId(),
          billInfoRequest.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        BillInfoResponse response = client.processRequest(requestContext, billInfoRequest);
        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
        	  LOG.info(LogFormatter.instance(requestContext.getTraceId())
      	            .data("BillInfoResponse has failed" , response).format());

            errorResp = (BillInfoResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }
        LOG.info(LogFormatter.instance(requestContext.getTraceId())
	            .data("BillInfoResponse is successful " , response).format());

        return new AsyncResult<>(response);
      }
    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    // return errorResp;
    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<BillPaymentServiceResponse> processBillPayment(PartnerRequestContext requestContext,
      BillPaymentServiceRequest request) throws RechargeServiceException {
    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Requested for recharge service").format());

    request.setRequestType(OperationType.SERVICE_RECHARGE.value());

    BillPaymentServiceResponse errorResp = new BillPaymentServiceResponse();
    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        
        BillPaymentServiceResponse response = client.processRequest(requestContext, request);
        
        if (response != null) {
   	 
          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            
        	  LOG.info(LogFormatter.instance(requestContext.getTraceId())
        	            .data("BillPaymentServiceResponse has failed" , response).format());
        	  
            errorResp = (BillPaymentServiceResponse) response.clone();
            errorResp.setMerchantRefNo(
                request.getBillMerchantServiceDetails().getMerchantRefNo().toString());
            errorResp.setEuronetRefNo(response.getEuronetRefNo());
            return new AsyncResult<>(errorResp);
          }
        }
        
        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("BillPaymentServiceResponse is sucessful" , response).format());
        
        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<RechargeStatusResponse> getTransactionStatus(PartnerRequestContext requestContext,
      RechargeStatusRequest request) throws RechargeServiceException {
    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Requested for recharge service").format());

    RechargeStatusResponse errorResp = new RechargeStatusResponse();
    try {

      request.setMerchantRefNo(CoreUtils.generateDipcoinToMerchantReferenceNumber());
      request.setMerchantCode(rechargeConfigurations.getMerchantCode());
      request.setUserName(rechargeConfigurations.getUserName());
      request.setUserPass(rechargeConfigurations.getUserPass());
      request.setRequesterIP(rechargeConfigurations.getIPAdress());
      request.setRequestType(OperationType.STATUS_RECHARGE.value());

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        RechargeStatusResponse response = client.processRequest(requestContext, request);
        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            LOG.info("The RechargeStatusResponse has failed");
            errorResp = (RechargeStatusResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
          
          if(!RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode()))
          LOG.info(LogFormatter.instance(requestContext.getTraceId())
              .data("RechargeStatusResponse" , response).format());
        }
        
        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("RechargeStatusResponse is successful" , response).format());

        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    // return errorResp;
    return new AsyncResult<>(errorResp);

  }

  @Override
  public Future<RechargeBalanceResponse> getWalletBalance(PartnerRequestContext requestContext,
      RechargeBalanceRequest request) throws RechargeServiceException {
    // TODO Auto-generated method stub

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Requested for balance  check").format());

    RechargeBalanceResponse errorResp = new RechargeBalanceResponse();
    try {

//      request.setMerchantRefNo(CoreUtils.generateDipcoinToMerchantReferenceNumber());
//      request.setMerchantCode(rechargeConfigurations.getMerchantCode());
//      request.setUserName(rechargeConfigurations.getUserName());
//      request.setUserPass(rechargeConfigurations.getUserPass());
//      request.setStoreCode(rechargeConfigurations.getStoreCode());
//      request.setRequesterIP(rechargeConfigurations.getIPAdress());
      
      
      request.setRequestType(OperationType.BALANCE_RECHARGE.value());

      BillMerchantValidationDetails balanceCheckRequest = new BillMerchantValidationDetails();
      balanceCheckRequest.setRequestType(OperationType.BALANCE_RECHARGE.value());
      balanceCheckRequest.setMerchantRefNo(CoreUtils.generateDipcoinToMerchantReferenceNumber());
      balanceCheckRequest.setMerchantCode(rechargeConfigurations.getMerchantCode());
      balanceCheckRequest.setUserName(rechargeConfigurations.getUserName());
      balanceCheckRequest.setUserPass(rechargeConfigurations.getUserPass());
      balanceCheckRequest.setStoreCode(rechargeConfigurations.getStoreCode());
      balanceCheckRequest.setAgentId(rechargeConfigurations.getAgentId());
      
      request.setBalanceCheckRequest(balanceCheckRequest);
      
      LOG.info(LogFormatter.instance(requestContext.getTraceId())
              .data("RechargeBalanceRequest" , request).format());
      
      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());
      
      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);
      
      if (client != null) {
        
        RechargeBalanceResponse response = client.processRequest(requestContext, request);
        
        if (response != null) {
          
          LOG.info(LogFormatter.instance(requestContext.getTraceId())
              .data("RechargeBalanceResponse is successful" , response).format());

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            
            LOG.info(LogFormatter.instance(requestContext.getTraceId())
                .data("RechargeBalanceResponse has failed" , response).format());
            
            errorResp = (RechargeBalanceResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }

        return new AsyncResult<>(response);
      }
    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);

  }

  @Override
  public Future<BillPaymentValidateResponse> validateBill(PartnerRequestContext requestContext,
      BillPaymentValidateRequest request) throws RechargeServiceException {

    BillPaymentValidateResponse errorResp = new BillPaymentValidateResponse();
    request.setRequestType(OperationType.VALIDATE_BILL.value());

    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        BillPaymentValidateResponse response = client.processRequest(requestContext, request);
        if (response != null) {
          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            
        	  LOG.info(LogFormatter.instance(requestContext.getTraceId())
      	            .data("BillPaymentValidateResponse has failed" , response).format());
        	  
            errorResp = (BillPaymentValidateResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }
        
        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("BillPaymentValidateResponse is sucessful" , response).format());

        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<BillStatusResponse> getBillTransactionStatus(PartnerRequestContext requestContext,
      BillStatusRequest request) throws RechargeServiceException {
    BillStatusResponse errorResp = new BillStatusResponse();
    request.setRequestType(OperationType.FETCH_STATUS.value());

    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        BillStatusResponse response = client.processRequest(requestContext, request);
        if (response != null) {


          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {

            LOG.info(LogFormatter.instance(requestContext.getTraceId())
                .data("BillStatusResponse is not sucessful" , response).format());

            errorResp = (BillStatusResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }

        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("BillStatusResponse is sucessful" , response).format());
        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<BillRegisterComplaintResponse> registerComplaint(
      PartnerRequestContext requestContext, BillRegisterComplaintRequest request)
      throws RechargeServiceException {
    request.setRequestType(OperationType.REGISTER_COMPLAINT.value());

    BillRegisterComplaintResponse errorResp = new BillRegisterComplaintResponse();

    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      LOG.info(request);

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        BillRegisterComplaintResponse response = client.processRequest(requestContext, request);
        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            
       
            LOG.info(LogFormatter.instance(requestContext.getTraceId())
                .data("The BillRegisterComplaintResponse has failed" , response).format());
            
            errorResp = (BillRegisterComplaintResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }
        
        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("The BillRegisterComplaintResponse is succesful" , response).format());
        
        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<BillRegisterComplaintResponse> getComplaintStatus(
      PartnerRequestContext requestContext, BillComplaintStatusRequest request)
      throws RechargeServiceException {

    request.setRequestType(OperationType.COMPLAINT_STATUS.value());

    BillRegisterComplaintResponse errorResp = new BillRegisterComplaintResponse();

    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }

      LOG.info(request);

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        BillRegisterComplaintResponse response = client.processRequest(requestContext, request);
        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            
            LOG.info(LogFormatter.instance(requestContext.getTraceId())
                .data("The Bill Complaint Status Response has failed" , response).format());
            
            errorResp = (BillRegisterComplaintResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }

        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("The Bill Complaint Status Response is successful" , response).format());
        
        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<MDMResponse> getMDM(PartnerRequestContext requestContext, MDMRequest request)
      throws RechargeServiceException {
    MDMResponse errorResp = new MDMResponse();
    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }
      request.setRequestType(OperationType.MDM.value());

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        MDMResponse response = client.processRequest(requestContext, request);

        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            LOG.info("The MDMResponse  has Failed");
            errorResp = (MDMResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }

        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .message("MDMResponse " + response.size()).format());
        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }

  @Override
  public Future<RechargeJioValidateResponse> validateJIORecharge(
      PartnerRequestContext requestContext, RechargeJioValidateRequest request)
      throws RechargeServiceException {

    LOG.debug(LogFormatter.instance(requestContext.getTraceId())
        .message("Requested for recharge service").format());

    RechargeJioValidateResponse errorResp = new RechargeJioValidateResponse();
    try {

      request.setMerchantRefNo(CoreUtils.generateDipcoinToMerchantReferenceNumber());
      request.setMerchantCode(rechargeConfigurations.getMerchantCode());
      request.setRequestType(OperationType.RECHARE_JIO_VALIDATION.toString());

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        RechargeJioValidateResponse response = client.processRequest(requestContext, request);

        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            
            
            LOG.info(LogFormatter.instance(requestContext.getTraceId())
                .data("The Recharge Jio Validate Response has failed" , response).format());
            
            errorResp = (RechargeJioValidateResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }

        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .data("The Recharge Jio Validate Response is successful" , response).format());
        
        return new AsyncResult<>(response);
      }
    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);

  }
  
  @Override
  public Future<RechargePlanResponse> getRechargePlan(PartnerRequestContext requestContext, RechargePlanRequest request)
      throws RechargeServiceException {
    RechargePlanResponse errorResp = new RechargePlanResponse();
    try {
      // validate incoming request
      if (request == null || !request.validate()) {
        String desc = "Request parameters are invalid " + request.toString();
        LOG.error(LogFormatter.instance(requestContext.getTraceId()).message(desc).format());
        errorResp.setResponseCode(RechargeResponseStatus.BAD_REQUEST.code());
        errorResp.setResponseMessage(desc);
        return new AsyncResult<>(errorResp);
      }
      request.setRequestType(OperationType.RECHARGE_PLAN.value());

      String rechargeClientName =
          clientLookupKey(request.getMerchantReferenceId(), request.getOperationType());

      RechargeClient client = this.rechargeClientFactory.getClient(rechargeClientName);

      if (client != null) {
        RechargePlanResponse response = client.processRequest(requestContext, request);

        if (response != null) {

          if (response.getResponseCode() == null
              || !RechargeResponseStatus.SUCCESS.code().equals(response.getResponseCode())) {
            LOG.info("The RechargePlan  has Failed");
            errorResp = (RechargePlanResponse) response.clone();
            return new AsyncResult<>(errorResp);
          }
        }

        LOG.info(LogFormatter.instance(requestContext.getTraceId())
            .message("RechargePlan " + response.size()).format());
        return new AsyncResult<>(response);
      }

    } catch (RechargeServiceException e) {
      LOG.error(
          LogFormatter.instance(requestContext.getTraceId()).message("Exception caught").format(),
          e);
    }

    errorResp.setResponseCode(RechargeResponseStatus.INTERNAL_ERROR.code());
    errorResp.setResponseMessage(RechargeResponseStatus.INTERNAL_ERROR.description());

    return new AsyncResult<>(errorResp);
  }
}
