package com.dipcoin.api.model;

import java.util.List;
import com.dipcoin.db.services.commons.AggregateSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class BankTransactionsResponse extends APIResponse {
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class BankTransactionResponse {
    private String bankTransactionRefId;
    private String ostaTransactionRefId;
    private String bankUID;
    private String amount;
    private String time;
    private Integer status;
    private Integer type;
    private Integer reconIssue;
    private Integer reconStatus;
    private String reconIssueDate;
    private String reconStatusDate;
    private String bankCommission;
    private String bankGst;
    private String bankTds;
    private String settlementBankRefNo;
    private String ostaCommission;
    private Integer settlementStatus;
    private String txnTime;
    private String bankTxnAmount;
    private String additionalInformation;
    private String cBSJournalNumber;
    private String accountNumber;
    private String bankName;
    private String commissionAmount;
    private String branchCode;
    private String usageDetails;
    private String utrNumber;

    public String getUsageDetails() {
		return usageDetails;
	}

	public void setUsageDetails(String usageDetails) {
		this.usageDetails = usageDetails;
	}

	public String getAccountNumber() {
      return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
      this.accountNumber = accountNumber;
    }

    public String getcBSJournalNumber() {
      return cBSJournalNumber;
    }

    public void setcBSJournalNumber(String cBSJournalNumber) {
      this.cBSJournalNumber = cBSJournalNumber;
    }

    public String getAdditionalInformation() {
      return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
      this.additionalInformation = additionalInformation;
    }

    public String getBankTxnAmount() {
      return bankTxnAmount;
    }

    public void setBankTxnAmount(String bankTxnAmount) {
      this.bankTxnAmount = bankTxnAmount;
    }

    public String getSettlementBankRefNo() {
      return settlementBankRefNo;
    }

    public void setSettlementBankRefNo(String settlementBankRefNo) {
      this.settlementBankRefNo = settlementBankRefNo;
    }

    public final String getBankTransactionRefId() {
      return bankTransactionRefId;
    }

    public final BankTransactionResponse setBankTransactionRefId(String bankTransactionRefId) {
      this.bankTransactionRefId = bankTransactionRefId;
      return this;
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

    public String getOstaCommission() {
      return ostaCommission;
    }

    public void setOstaCommission(String ostaCommission) {
      this.ostaCommission = ostaCommission;
    }

    public final String getBankUID() {
      return bankUID;
    }

    public final BankTransactionResponse setBankUID(String bankUID) {
      this.bankUID = bankUID;
      return this;
    }

    public final String getAmount() {
      return amount;
    }

    public final BankTransactionResponse setAmount(String amount) {
      this.amount = amount;
      return this;
    }

    public final String getTime() {
      return time;
    }

    public final BankTransactionResponse setTime(String time) {
      this.time = time;
      return this;
    }

    public final Integer getStatus() {
      return status;
    }

    public final BankTransactionResponse setStatus(Integer status) {
      this.status = status;
      return this;
    }

    public final Integer getType() {
      return type;
    }

    public final BankTransactionResponse setType(Integer type) {
      this.type = type;
      return this;
    }

    public final Integer getReconIssue() {
      return reconIssue;
    }

    public final BankTransactionResponse setReconIssue(Integer reconIssue) {
      this.reconIssue = reconIssue;
      return this;
    }

    public final Integer getReconStatus() {
      return reconStatus;
    }

    public final BankTransactionResponse setReconStatus(Integer reconStatus) {
      this.reconStatus = reconStatus;
      return this;
    }

    public final String getReconIssueDate() {
      return reconIssueDate;
    }

    public final BankTransactionResponse setReconIssueDate(String reconIssueDate) {
      this.reconIssueDate = reconIssueDate;
      return this;
    }

    public final String getReconStatusDate() {
      return reconStatusDate;
    }

    public final BankTransactionResponse setReconStatusDate(String reconStatusDate) {
      this.reconStatusDate = reconStatusDate;
      return this;
    }

    public final String getOstaTransactionRefId() {
      return ostaTransactionRefId;
    }

    public final BankTransactionResponse setOstaTransactionRefId(String ostaTransactionRefId) {
      this.ostaTransactionRefId = ostaTransactionRefId;
      return this;
    }

    public Integer getSettlementStatus() {
      return settlementStatus;
    }

    public void setSettlementStatus(Integer settlementStatus) {
      this.settlementStatus = settlementStatus;
    }

    public String getTxnTime() {
      return txnTime;
    }

    public void setTxnTime(String txnTime) {
      this.txnTime = txnTime;
    }

    public String getBankName() {
      return bankName;
    }

    public void setBankName(String bankName) {
      this.bankName = bankName;
    }

    public String getCommissionAmount() {
      return commissionAmount;
    }

    public void setCommissionAmount(String commissionAmount) {
      this.commissionAmount = commissionAmount;
    }

	public String getBranchCode() {
		return branchCode;
	}

	public void setBranchCode(String branchCode) {
		this.branchCode = branchCode;
	}
	
	public String getUtrNumber() {
      return utrNumber;
    }

    public void setUtrNumber(String utrNumber) {
      this.utrNumber = utrNumber;
    }
  }

  private String clientTransactionId;
  private AggregateSnapshot snapshot;
  private List<BankTransactionResponse> transactions;
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

  public List<BankTransactionResponse> getTransactions() {
    return transactions;
  }

  public void setTransactions(List<BankTransactionResponse> transactions) {
    this.transactions = transactions;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }
}
