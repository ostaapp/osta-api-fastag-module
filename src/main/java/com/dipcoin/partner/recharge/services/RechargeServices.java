package com.dipcoin.partner.recharge.services;

import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Async;

import com.dipcoin.api.utils.RechargeServiceException;
import com.dipcoin.partner.recharge.comm.BillComplaintStatusRequest;
import com.dipcoin.partner.recharge.comm.BillInfoRequest;
import com.dipcoin.partner.recharge.comm.BillInfoResponse;
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

public interface RechargeServices {

  @Async
  public Future<RechargeValidateResponse> validateRecharge(
      final PartnerRequestContext requestContext, final RechargeValidateRequest request)
      throws RechargeServiceException;

  @Async
  public Future<RechargeServiceResponse> processRecharge(final PartnerRequestContext requestContext,
      final RechargeServiceRequest request) throws RechargeServiceException;

  @Async
  public Future<RechargeStatusResponse> getTransactionStatus(
      final PartnerRequestContext requestContext, final RechargeStatusRequest request)
      throws RechargeServiceException;

  @Async
  public Future<RechargeBalanceResponse> getWalletBalance(PartnerRequestContext requestContext,
      RechargeBalanceRequest request) throws RechargeServiceException;

  @Async
  public Future<BillInfoResponse> getBillInfo(final PartnerRequestContext requestContext,
      final BillInfoRequest request) throws RechargeServiceException;

  @Async
  public Future<BillPaymentServiceResponse> processBillPayment(
      final PartnerRequestContext requestContext, final BillPaymentServiceRequest request)
      throws RechargeServiceException;

  @Async
  public Future<BillPaymentValidateResponse> validateBill(
      final PartnerRequestContext requestContext, final BillPaymentValidateRequest request)
      throws RechargeServiceException;

  @Async
  public Future<BillStatusResponse> getBillTransactionStatus(
      final PartnerRequestContext requestContext, final BillStatusRequest request)
      throws RechargeServiceException;

  @Async
  public Future<BillRegisterComplaintResponse> registerComplaint(
      final PartnerRequestContext requestContext, final BillRegisterComplaintRequest request)
      throws RechargeServiceException;

  @Async
  public Future<BillRegisterComplaintResponse> getComplaintStatus(
      final PartnerRequestContext requestContext, final BillComplaintStatusRequest request)
      throws RechargeServiceException;

  @Async
  public Future<MDMResponse> getMDM(final PartnerRequestContext requestContext,
      final MDMRequest request) throws RechargeServiceException;

  @Async
  public Future<RechargeJioValidateResponse> validateJIORecharge(
      final PartnerRequestContext requestContext, final RechargeJioValidateRequest request)
      throws RechargeServiceException;
  
  @Async
  public Future<RechargePlanResponse> getRechargePlan(final PartnerRequestContext requestContext,
      final RechargePlanRequest request) throws RechargeServiceException;
  
}
