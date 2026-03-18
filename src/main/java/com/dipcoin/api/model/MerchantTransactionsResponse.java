package com.dipcoin.api.model;

import java.math.BigInteger;
import java.util.List;
import com.dipcoin.db.services.commons.AggregateSnapshot;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class MerchantTransactionsResponse extends APIResponse implements Comparable<MerchantTransactionsResponse> {

  private String merchantTransactionRefId;
  private String ostaTransactionRefId;
  private String orderId;
  // private String maskedDipcoin;
  private String amount;
  private String time;
  private Integer status;
  private Integer channelType;
  private String type;
  private Integer reconIssue;
  private String reconIssueDate;
  private Integer reconStatus;
  private String reconStatusDate;
  private Boolean settlementDone;
  private String  errorDescription;
  private String ostaCommission;
  private String merchantAmount;
  private String pgCharge;
  private String pgGst;
  private String ostaGst;
  private String ostaTds;
  private String cedgeCommission;
  private String cedgeGst;
  private String cedgeTds;
  private String bankCommission;
  private String bankGst;
  private String bankTds;
  private String paymentMode;
  private String gatewayName;
  //This is taken from the merchant.getReferenceId
  private String pgReferenceId; 
  private String settlementBankRefNo;
  private String settlementStatus;
  private String chargeableAmount;
  private String bankName;
  
  public String getBankName() {
	return bankName;
}

public void setBankName(String bankName) {
	this.bankName = bankName;
}

public String getChargeableAmount() {
    return chargeableAmount;
  }

  public void setChargeableAmount(String chargeableAmount) {
    this.chargeableAmount = chargeableAmount;
  }

  public String getOstaTds() {
    return ostaTds;
  }

  public void setOstaTds(String ostaTds) {
    this.ostaTds = ostaTds;
  }
  
  public String getPgReferenceId() {
    return pgReferenceId;
  }

  public void setPgReferenceId(String pgReferenceId) {
    this.pgReferenceId = pgReferenceId;
  }
  
  public String getSettlementBankRefNo() {
    return settlementBankRefNo;
  }

  public void setSettlementBankRefNo(String settlementBankRefNo) {
    this.settlementBankRefNo = settlementBankRefNo;
  }
  
public String getSettlementStatus() {
    return settlementStatus;
  }

  public void setSettlementStatus(String settlementStatus) {
    this.settlementStatus = settlementStatus;
  }

public String getPgGst() {
    return pgGst;
  }

  public void setPgGst(String pgGst) {
    this.pgGst = pgGst;
  }

  public String getOstaGst() {
    return ostaGst;
  }

  public void setOstaGst(String ostaGst) {
    this.ostaGst = ostaGst;
  }

  public String getCedgeCommission() {
    return cedgeCommission;
  }

  public void setCedgeCommission(String cedgeCommission) {
    this.cedgeCommission = cedgeCommission;
  }

  public String getCedgeGst() {
    return cedgeGst;
  }

  public void setCedgeGst(String cedgeGst) {
    this.cedgeGst = cedgeGst;
  }

  public String getCedgeTds() {
    return cedgeTds;
  }

  public void setCedgeTds(String cedgeTds) {
    this.cedgeTds = cedgeTds;
  }

  public String getBankCommission() {
	return bankCommission;
}

public void setBankCommission(String bankCommission) {
	this.bankCommission = bankCommission;
}

public String getBankGst() {
	return bankGst;
}

public void setBankGst(String bankGst) {
	this.bankGst = bankGst;
}

public String getBankTds() {
	return bankTds;
}

public void setBankTds(String bankTds) {
	this.bankTds = bankTds;
}

public String getPaymentMode() {
    return paymentMode;
  }

  public void setPaymentMode(String paymentMode) {
    this.paymentMode = paymentMode;
  }

  public String getGatewayName() {
    return gatewayName;
  }

  public void setGatewayName(String gatewayName) {
    this.gatewayName = gatewayName;
  }
  
public String getOstaCommission() {
	return ostaCommission;
}

public void setOstaCommission(String ostaCommission) {
	this.ostaCommission = ostaCommission;
}

public String getMerchantAmount() {
	return merchantAmount;
}

public void setMerchantAmount(String merchantAmount) {
	this.merchantAmount = merchantAmount;
}

public String getPgCharge() {
	return pgCharge;
}

public void setPgCharge(String pgCharge) {
	this.pgCharge = pgCharge;
}

public String getErrorDescription() {
    return errorDescription;
  }

  public void setErrorDescription(String errorDescription) {
    this.errorDescription = errorDescription;
  }
  public Boolean getSettlementDone() {
    return settlementDone;
  }

  public void setSettlementDone(Boolean settlementDone) {
    this.settlementDone = settlementDone;
  }

  public final String getMerchantTransactionRefId() {
    return merchantTransactionRefId;
  }

  public final MerchantTransactionsResponse setMerchantTransactionRefId(
      String merchantTransactionRefId) {
    this.merchantTransactionRefId = merchantTransactionRefId;
    return this;
  }

  public final String getOstaTransactionRefId() {
    return ostaTransactionRefId;
  }

  public final MerchantTransactionsResponse setOstaTransactionRefId(String ostaTransactionRefId) {
    this.ostaTransactionRefId = ostaTransactionRefId;
    return this;
  }

  public final String getOrderId() {
    return orderId;
  }

  public final MerchantTransactionsResponse setOrderId(String orderId) {
    this.orderId = orderId;
    return this;
  }

  /*
   * public final String getMaskedDipcoin() { return maskedDipcoin; } public final
   * MerchantTransactionsResponse setMaskedDipcoin(String maskedDipcoin) { this.maskedDipcoin =
   * maskedDipcoin; return this; }
   */
  public final String getAmount() {
    return amount;
  }

  public final MerchantTransactionsResponse setAmount(String amount) {
    this.amount = amount;
    return this;
  }

  public final String getTime() {
    return time;
  }

  public final MerchantTransactionsResponse setTime(String time) {
    this.time = time;
    return this;
  }

  public final Integer getStatus() {
    return status;
  }

  public final MerchantTransactionsResponse setStatus(Integer status) {
    this.status = status;
    return this;
  }

  public final Integer getChannelType() {
    return channelType;
  }

  public final MerchantTransactionsResponse setChannelType(Integer channelType) {
    this.channelType = channelType;
    return this;
  }

  public final String getType() {
    return type;
  }

  public final MerchantTransactionsResponse setType(String type) {
    this.type = type;
    return this;
  }

  public final Integer getReconIssue() {
    return reconIssue;
  }

  public final MerchantTransactionsResponse setReconIssue(Integer reconIssue) {
    this.reconIssue = reconIssue;
    return this;
  }

  public final String getReconIssueDate() {
    return reconIssueDate;
  }

  public final MerchantTransactionsResponse setReconIssueDate(String reconIssueDate) {
    this.reconIssueDate = reconIssueDate;
    return this;
  }

  public final Integer getReconStatus() {
    return reconStatus;
  }

  public final MerchantTransactionsResponse setReconStatus(Integer reconStatus) {
    this.reconStatus = reconStatus;
    return this;
  }

  public final String getReconStatusDate() {
    return reconStatusDate;
  }

  public final MerchantTransactionsResponse setReconStatusDate(String reconStatusDate) {
    this.reconStatusDate = reconStatusDate;
    return this;
  }

  private String clientTransactionId;
  private AggregateSnapshot snapshot;
  private List<MerchantTransactionsResponse> transactions;
  private Pagination pagination;

  public final String getClientTransactionId() {
    return clientTransactionId;
  }

  public final void setClientTransactionId(String clientTransactionId) {
    this.clientTransactionId = clientTransactionId;
  }

  public AggregateSnapshot getSnapshot() {
    return snapshot;
  }

  public void setSnapshot(AggregateSnapshot snapshot) {
    this.snapshot = snapshot;
  }

  public List<MerchantTransactionsResponse> getTransactions() {
    return transactions;
  }

  public void setTransactions(List<MerchantTransactionsResponse> transactions) {
    this.transactions = transactions;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  @Override
  public int compareTo(MerchantTransactionsResponse merchantTxn) {
    BigInteger requestTime = new BigInteger(merchantTxn.getTime());
    BigInteger time = new BigInteger(this.time);
    return requestTime.compareTo(time) ;
  }
}
