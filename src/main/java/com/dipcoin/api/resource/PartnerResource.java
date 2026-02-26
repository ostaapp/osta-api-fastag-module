package com.dipcoin.api.resource;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
//import com.dipcoin.api.commons.PartnerApprovalHelper;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
//import com.dipcoin.api.model.BrontooConvenienceFeeResponse;
import com.dipcoin.api.model.PartnerApprovalStatus;
import com.dipcoin.api.model.PartnerPaymentRequest;
//import com.dipcoin.api.model.PartnerPaymentResponse;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.ConvenienceFeeManagementDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.PartnerDBService;
import com.dipcoin.db.services.commons.DBConstants.PartnerApprovalFields;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.ConvenienceFeeManagement;
import com.dipcoin.db.services.model.FeesAMCPaymentDetail;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.MerchantOnboard;
import com.dipcoin.db.services.model.User;

@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public abstract class PartnerResource {

  private static final Logger LOG = LogManager.getLogger(MerchantResource.class);

  @Autowired
  private PartnerDBService partnerDBService;
  
//  @Autowired
//  private PartnerApprovalHelper partnerApprovalHelper;
  
  @Autowired
  private MerchantDBService merchantDBService;
  
  @Autowired
  private ConvenienceFeeManagementDBService convenienceFeeManagementDBService;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

  /*
   * Accept payment details for partner.
   */
//  protected ResponseEntity addPaymentDetails(final User user, final Bank bank,
//      final Merchant merchant, final PartnerPaymentRequest request) throws APIException, Exception {
//
//    if (request == null) {
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//          .body(APIResponse.error(HeaderCode.BAD_REQUEST));
//    }
//
//    if (!request.validate(httpServletContext)) {
//      PartnerPaymentResponse response = new PartnerPaymentResponse();
//      response.addHeaderCodes(request.getErrorCodes());
//      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }
//
//    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//        .data("BankMerchantId", user.getBankMerchantId()).format());
//
//    String requestTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
//
//    FeesAMCPaymentDetail paymentDetails = new FeesAMCPaymentDetail();
//
//    paymentDetails.setAmount(request.getAmount());
//    paymentDetails.setPaymentReferenceNo(request.getReferenceNumber());
//    paymentDetails.setPaymentMethod(request.getMethod());
//    paymentDetails.setPaymentType(request.getType());
//    paymentDetails.setBankMerchantId(user.getBankMerchantId());
//    paymentDetails.setDetailsEnteredBy(user.getId());
//    paymentDetails.setRequestTime(requestTime);
//    paymentDetails.setPaymentRefDate(request.getReferenceDate());
//    paymentDetails.setPartnerReferenceId(
//        (merchant != null) ? merchant.getReferenceId() : bank.getReferenceId());
//    if (!StringUtils.isEmpty(request.getRemark()))
//      paymentDetails.setPaymentRemarks(request.getRemark());
//
//    paymentDetails = partnerDBService.addFeesAndAMCDetails(paymentDetails);
//
//    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//        .data("PaymentDetails", paymentDetails).format());
//
//    if (paymentDetails != null) {
//        if(merchant!=null) {
//            
//            MerchantOnboard merchantOnboard = merchantDBService.getMerchantOnboardByReferenceId(merchant.getReferenceId());
//            
//            if(merchantOnboard==null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.MERCHANT_DOESNT_EXIST)); 
//            }
//        
//          PartnerApprovalStatus approvalStatus =
//              partnerApprovalHelper.submitPartnerApproval(user, merchant, merchantOnboard, null, PartnerApprovalFields.PAYMENT);
//          
//          LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                    .data("Approval Status", approvalStatus).format());
//
//          if (approvalStatus == null) {
//            throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//                APIResponse.error(HeaderCode.INTERNAL_ERROR));
//          }
//        }else{
//              PartnerApprovalStatus approvalStatus =
//                      partnerApprovalHelper.submitPartnerApproval(user, null, null, bank, PartnerApprovalFields.PAYMENT);
//              
//              LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
//                        .data("Approval Status", approvalStatus).format());
//                
//                  if (approvalStatus == null) {
//                    throw new APIException(HttpStatus.INTERNAL_SERVER_ERROR,
//                        APIResponse.error(HeaderCode.INTERNAL_ERROR));
//          }
//        }
//      return ResponseEntity.status(HttpStatus.CREATED)
//          .body(APIResponse.error(HeaderCode.PAYMENT_CAPTURED));
//    }
//
//    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//        .body(APIResponse.error(HeaderCode.INTERNAL_ERROR));
//  }

  
//  public ResponseEntity getPartnerConvenienceFee(final User user, final Bank bank,
//      final Merchant merchant, final Integer requestSource) {
//    
//    BrontooConvenienceFeeResponse response = new BrontooConvenienceFeeResponse();
//    
//    List<ConvenienceFeeManagement> convenienceFees = convenienceFeeManagementDBService
//     .getConvenienceFeeManagement((bank!=null ? bank.getReferenceId() : merchant.getReferenceId()), requestSource);
//
//    if (!CollectionUtils.isEmpty(convenienceFees)) {
//      
//      ConvenienceFeeManagement convenienceFee = convenienceFees.get(0);
//      
//      if (convenienceFee != null) {
//        
//        response.setPartnerCommission(convenienceFee.getPartnerCommission());
//        response.setCedgeCommission(convenienceFee.getCedgeCommission());
//        response.setConvenienceFee(convenienceFee.getCustomerConvenienceFee());
//        response.setOstaCommission(convenienceFee.getOstaCommission());
//        response.setRequestSource(convenienceFee.getRequestSource());
//        response.setPartnerReferenceId(convenienceFee.getPartnerReferenceId());
//        response.setMediumProviderCommission(convenienceFee.getMediumProviderCommission());
//      }
//    }
//    
//    return ResponseEntity.status(HttpStatus.OK).body(response);
//  }
}
