package com.dipcoin.api.model;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.dipcoin.api.commons.APIConstants.TransactionRequestType;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIRequest;
import com.dipcoin.db.services.commons.DBConstants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class AddMoneyToWalletRequest extends APIRequest {

  private BigDecimal amount;
  private String currency;
  private String partnerTransactionReferenceId;
  private String orderId;
  private String phonenum;
  private String walletId;
  private String partnerReferenceId;
  private String requestType;
  private String authorizationPin;
  private String noOfInvoice;
  private String merchantName;
  private String paymentMode;
  private Integer isSettlement = DBConstants.IsSettlement.DEFAULT.value();

  @Override
  public boolean validate(HttpServletContext httpServletContext) {

    if (!(this.amount != null && !StringUtils.isBlank(this.amount.toString()))) {
      this.setErrorCode(HeaderCode.MERCHANT_MISSING_AMOUNT);
      return false;
    }

    if (this.amount.doubleValue() <= 0) {
      this.setErrorCode(HeaderCode.OSTA_THRESHOLD_BREACHED);
      return false;
    }

    if (!(!StringUtils.isBlank(this.currency) && !this.currency.contains(" "))) {
      this.setErrorCode(HeaderCode.MERCHANT_MISSING_CURRENCY);
      return false;
    }

    if (!(!StringUtils.isBlank(this.phonenum)
        && !(this.phonenum.length() != DBConstants.MOBILE_LENGTH)
        && NumberUtils.isCreatable(this.phonenum) && !this.phonenum.contains(" "))) {
      this.setErrorCode(HeaderCode.USER_INVALID_PHONENUM);
      return false;
    }

    if (!(!StringUtils.isBlank(this.orderId) && !this.orderId.contains(" "))) {
      this.setErrorCode(HeaderCode.MERCHANT_MISSING_ORDERID);
      return false;
    }

    // ✅ Only validate walletId for WALLET_TOP_UP, not for CREATEOSTA
 	if (TransactionRequestType.WALLET_TOP_UP.value().equals(this.requestType)) {
 		if (!(!StringUtils.isBlank(this.walletId) && !this.walletId.contains(" "))) {
 			this.setErrorCode(HeaderCode.WALLETID_MISSING);
 			return false;
 		}
 	}

    if (!(!StringUtils.isBlank(this.partnerReferenceId)
        && !this.partnerReferenceId.contains(" "))) {
      this.setErrorCode(HeaderCode.PARTNER_REF_ID_NOT_PROVIDED);
      return false;
    }

    if (!(!StringUtils.isBlank(this.partnerTransactionReferenceId)
        && !this.partnerTransactionReferenceId.contains(" "))) {
      this.setErrorCode(HeaderCode.PARTNER_REF_ID_NOT_PROVIDED);
      return false;
    }

    if (!(!StringUtils.isBlank(this.requestType) && !this.requestType.contains(" "))) {
      this.setErrorCode(HeaderCode.BAD_REQUEST);
      return false;
    }

    if (!this.requestType.equals(TransactionRequestType.WALLET_TOP_UP.value())) {
      if (!this.requestType.equals(TransactionRequestType.CREATEOSTA.value())) {
        this.setErrorCode(HeaderCode.BAD_REQUEST);
        return false;
      }
    }

    return true;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    // TODO Auto-generated method stub

  }
}
