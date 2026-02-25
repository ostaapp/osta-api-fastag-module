package com.dipcoin.partner.recharge.client;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dipcoin.api.utils.RechargeServiceException;
import com.dipcoin.commons.HttpsUtils.Proxy;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.partner.recharge.comm.BillInfoResponse;
import com.dipcoin.partner.recharge.comm.BillPaymentServiceResponse;
import com.dipcoin.partner.recharge.comm.BillPaymentValidateResponse;
import com.dipcoin.partner.recharge.comm.BillRegisterComplaintResponse;
import com.dipcoin.partner.recharge.comm.BillRequest;
import com.dipcoin.partner.recharge.comm.BillResponse;
import com.dipcoin.partner.recharge.comm.BillStatusResponse;
import com.dipcoin.partner.recharge.comm.MDMResponse;
import com.dipcoin.partner.recharge.comm.RechargeBalanceResponse;
import com.dipcoin.partner.recharge.comm.RechargePlanResponse;
import com.dipcoin.partner.recharge.comm.RechargeRequest;
import com.dipcoin.partner.recharge.comm.RechargeResponse;
import com.dipcoin.partner.recharge.comm.RechargeServiceResponse;
import com.dipcoin.partner.recharge.comm.RechargeStatusResponse;
import com.dipcoin.partner.recharge.comm.RechargeValidateResponse;
import com.dipcoin.partner.utils.PartnerClient;
import com.dipcoin.partner.utils.PartnerRequestContext;

public abstract class RechargeClient extends PartnerClient {
  private static final Logger LOG = LogManager.getLogger(RechargeClient.class);

  public enum OperationType {
    // @formatter:off
    VALIDATE_RECHARGE("Validate"), 
    SERVICE_RECHARGE("Service"),
    BALANCE_RECHARGE("BalanceCheck"),
    STATUS_RECHARGE("Status"),
    RECHARE_JIO_VALIDATION("JioValidation"),
    VALIDATE_BILL("BillValidation"),
    FETCH_BILL("ValidateBill"),
    FETCH_STATUS("billPayStatus"),
    REGISTER_COMPLAINT("complaint"),
    COMPLAINT_STATUS("complaintstatus"),
    SERVICE_BILL("ServiceBill"),
    MDM("GetMDMResponse"),
    RECHARGE_PLAN("RechargePlan");
    // @formatter:on

    private String value;

    private OperationType(String value) {
      this.value = value;
    }

    public String value() {
      return this.value;
    }
  };

  public RechargeClient(Protocol protocol, String host) {
    super(PartnerClient.Protocol.HTTPS, host);
  }

  private Proxy proxy = new Proxy();

  public void setProxy(Proxy proxy) {
    if (proxy != null) {
      LOG.info(LogFormatter.instance().message("Proxy Setup ").data("host", proxy.getHost())
          .data("port", proxy.getPort()).data("enabled", proxy.isEnabled()).format());
      this.proxy.setHost(proxy.getHost());
      this.proxy.setPort(proxy.getPort());
      this.proxy.setEnabled(proxy.isEnabled());
    }
  }

  protected final Proxy getProxy() {
    return proxy;
  }



  protected abstract void executeRequest(PartnerRequestContext requestContext,
      RechargeRequest dcRequest, RechargeResponse rcResponse)
      throws RechargeServiceException, IOException;

  protected abstract void executeBillRequest(PartnerRequestContext requestContext,
      BillRequest rcRequest, BillResponse rcResponse) throws RechargeServiceException, IOException;

  // public interface for the permitted operations
  // cannot be overridden
  @SuppressWarnings("unchecked")
  public final <T extends RechargeResponse> T processRequest(
      final PartnerRequestContext requestContext, final RechargeRequest request)
      throws RechargeServiceException {
    // deep clone request object
    RechargeResponse rcResponse = null;
    try {
      RechargeRequest rcRequest = request.deepCopy();
      switch (rcRequest.getOperationType()) {
        case SERVICE_RECHARGE:
          rcResponse = new RechargeServiceResponse();
          executeRequest(requestContext, rcRequest, rcResponse);
          break;
        case VALIDATE_RECHARGE:
          rcResponse = new RechargeValidateResponse();
          executeRequest(requestContext, rcRequest, rcResponse);
          break;
        case STATUS_RECHARGE:
          rcResponse = new RechargeStatusResponse();
          executeRequest(requestContext, rcRequest, rcResponse);
          break;
        case BALANCE_RECHARGE:
          rcResponse = new RechargeBalanceResponse();
          executeRequest(requestContext, rcRequest, rcResponse);
          break;
        default:
          LOG.error("Invalid Request Type: " + rcRequest.getRequestType());
          return null;
      }
    }

    catch (Exception ex) {
      LOG.debug(ex);
    }

    return (T) rcResponse;
  }

  @SuppressWarnings("unchecked")
  public final <T extends BillResponse> T processRequest(PartnerRequestContext requestContext,
      BillRequest request) throws RechargeServiceException {

    BillResponse rcResponse = null;
    try {
      BillRequest rcRequest = request;
      switch (rcRequest.getOperationType()) {
        case FETCH_BILL:
          rcResponse = new BillInfoResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        case VALIDATE_BILL:
          rcResponse = new BillPaymentValidateResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        case SERVICE_BILL:
          rcResponse = new BillPaymentServiceResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        case FETCH_STATUS:
          rcResponse = new BillStatusResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        case REGISTER_COMPLAINT:
          rcResponse = new BillRegisterComplaintResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        case COMPLAINT_STATUS:
          rcResponse = new BillRegisterComplaintResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        case MDM:
          rcResponse = new MDMResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        case RECHARGE_PLAN:
          rcResponse = new RechargePlanResponse();
          executeBillRequest(requestContext, rcRequest, rcResponse);
          break;
        default:
          LOG.error("Invalid Request Type: " + rcRequest.getRequestType());
          return null;
      }
    }

    catch (Exception ex) {
      LOG.debug("No Response");
    }

    return (T) rcResponse;
  }
}
