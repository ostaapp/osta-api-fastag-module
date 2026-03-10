package com.dipcoin.api.resource;

import java.security.SecureRandom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.AddMoneyToWalletRequest;
//import com.dipcoin.api.model.AddMoneyToWalletRequest;
import com.dipcoin.api.model.CreateUserWalletRequest;
import com.dipcoin.api.model.PaymentTopupWalletResponse;
//import com.dipcoin.api.model.PaymentTopupWalletResponse;
import com.dipcoin.api.model.WalletUserInfo;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants.TransactionSource;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;

@Component("OAuth2WalletServiceResource")
public class OAuth2WalletServiceResource {

	private static final Logger LOG = LogManager.getLogger(OAuth2WalletServiceResource.class);

	private final static SecureRandom randomGenerator = new SecureRandom();

	@Autowired
	private MerchantDBService merchantDBService;

	@Autowired
	private UserDBService userDBService;

	@Autowired
	private WalletResource walletResource;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;
	
	  public ResponseEntity addMoneyToWallet(final AddMoneyToWalletRequest topupReq, User user,
		      Merchant merchant, TransactionSource source) throws Exception, APIException {

		    LOG.info("Adding money to user wallet");
		    if (topupReq != null) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .message("Add money to wallet Request").data("Amount", topupReq.getAmount())
		          .data("Currency", topupReq.getCurrency()).data("orderId", topupReq.getOrderId())
		          .data("Phonenum", topupReq.getPhonenum()).data("WalletId", topupReq.getWalletId())
		          .format());
		    }

		    PaymentTopupWalletResponse paymentTopupWalletResponse = new PaymentTopupWalletResponse();
		    if (topupReq == null || !topupReq.validate(httpServletContext)) {
		      LOG.error(
		          LogFormatter.instance(httpServletContext.getTraceId()).message("Bad_Request").format());
		      LOG.info(topupReq.getErrorCodes());
		      paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		    }

		    final Merchant getmerchant = merchantDBService.getMerchant(topupReq.getPartnerReferenceId());

		    if (getmerchant == null) {

		      LOG.info(HeaderCode.MISSING_INVALID_INFO);
		      paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);

		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);

		    }
		    // verify if partner is active
		    if (!this.userDBService.isActive(getmerchant)) {

		      LOG.info(HeaderCode.MERCHANT_NOT_ACTIVE);
		      paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);

		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
		    }

		    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		        .message("Add money to wallet Request").data("Amount", topupReq.getAmount())
		        .data("Currency", topupReq.getCurrency()).data("orderId", topupReq.getOrderId())
		        .data("Phonenum", topupReq.getPhonenum()).data("WalletId", topupReq.getWalletId())
		        .format());

		    String clientTransactionId =
		        APIConstants.VIRTUAL + String.valueOf(Math.abs(randomGenerator.nextLong()));

		    return walletResource.topupWallet(topupReq, user, getmerchant, source);
		  }
	
	public ResponseEntity createUserWallet(final CreateUserWalletRequest createReq, User user,
		      Merchant merchant, Boolean createUser, TransactionSource source, String walletBankReferenceId)
		      throws Exception, APIException {

		    WalletUserInfo walletUserInfo = new WalletUserInfo();
		    LOG.info("Creating user wallet");
		    if (createReq != null) {
		      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		          .message("Create User Wallet Request").data("Fname", createReq.getFname())
		          .data("Lname", createReq.getLname()).data("Email", createReq.getEmail())
		          .data("Phonenum", createReq.getPhonenum())
		          .data("walletBankReferenceId", walletBankReferenceId).format());
		    }
		    createReq.setCreateUser(createUser);

		    if (createReq == null || !createReq.validate(httpServletContext)) {
		      LOG.error(
		          LogFormatter.instance(httpServletContext.getTraceId()).message("Bad_Request").format());

		      walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		      LOG.info(createReq.getErrorCodes());
		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(walletUserInfo);
		    }

		    // verify if partner is active
		    if (merchant != null) {
		      if (!this.userDBService.isActive(merchant)) {
		        walletUserInfo.addHeaderCode(HeaderCode.WALLET_Not_CREATED);
		        LOG.info(createReq.getErrorCodes());
		        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(walletUserInfo);
		      }
		    }

		    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		        .message("Create User Wallet Request").data("Fname", createReq.getFname())
		        .data("Lname", createReq.getLname()).data("Email", createReq.getEmail())
		        .data("Phonenum", createReq.getPhonenum()).format());

		    String clientTransactionId =
		        APIConstants.VIRTUAL + String.valueOf(Math.abs(randomGenerator.nextLong()));

		    return walletResource.createUserWallet(createReq, user, merchant, createUser, source, walletBankReferenceId);
		  }

//	public ResponseEntity addMoneyToWallet(final AddMoneyToWalletRequest topupReq, User user, Merchant merchant,
//			TransactionSource source) throws Exception, APIException {
//
//		LOG.info("Adding money to user wallet");
//		if (topupReq != null) {
//			LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
//					.message("addMoney FULL Request Body")
//					.data("amount", topupReq.getAmount())
//					.data("currency", topupReq.getCurrency())
//					.data("orderId", topupReq.getOrderId())
//					.data("phonenum", topupReq.getPhonenum())
//					.data("walletId", topupReq.getWalletId())
//					.data("partnerReferenceId", topupReq.getPartnerReferenceId())
//					.data("partnerTransactionReferenceId", topupReq.getPartnerTransactionReferenceId())
//					.data("requestType", topupReq.getRequestType())
//					.data("merchantName", topupReq.getMerchantName())
//					.data("paymentMode", topupReq.getPaymentMode())
//					.data("noOfInvoice", topupReq.getNoOfInvoice())
//					.data("isSettlement", topupReq.getIsSettlement())
//					.format());
//		}
//
//		PaymentTopupWalletResponse paymentTopupWalletResponse = new PaymentTopupWalletResponse();
//		if (topupReq == null || !topupReq.validate(httpServletContext)) {
//			LOG.error(LogFormatter.instance(httpServletContext.getTraceId()).message("Bad_Request").format());
//			LOG.info(topupReq.getErrorCodes());
//			paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
//		}
//
//		final Merchant getmerchant = merchantDBService.getMerchant(topupReq.getPartnerReferenceId());
//
//		LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
//				.message("Merchant lookup result")
//				.data("partnerReferenceId", topupReq.getPartnerReferenceId())
//				.data("merchantFound", getmerchant != null)
//				.data("merchantId", getmerchant != null ? getmerchant.getId() : "NULL")
//				.data("merchantStatus", getmerchant != null ? getmerchant.getStatus() : "NULL")
//				.data("merchantName", getmerchant != null ? getmerchant.getName() : "NULL")
//				.format());
//
//		if (getmerchant == null) {
//
//			LOG.info(HeaderCode.MISSING_INVALID_INFO);
//			paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
//
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
//
//		}
//		// verify if partner is active
//		if (!this.userDBService.isActive(getmerchant)) {
//
//			LOG.info(HeaderCode.MERCHANT_NOT_ACTIVE);
//			paymentTopupWalletResponse.addHeaderCode(HeaderCode.WALLET_TOPUP_FAILED);
//
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(paymentTopupWalletResponse);
//		}
//
//		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Add money to wallet Request")
//				.data("Amount", topupReq.getAmount()).data("Currency", topupReq.getCurrency())
//				.data("orderId", topupReq.getOrderId()).data("Phonenum", topupReq.getPhonenum())
//				.data("WalletId", topupReq.getWalletId()).format());
//
//		String clientTransactionId = APIConstants.VIRTUAL + String.valueOf(Math.abs(randomGenerator.nextLong()));
//
//		return walletResource.topupWallet(topupReq, null, getmerchant, source);
//	}

}
