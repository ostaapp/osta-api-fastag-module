package com.dipcoin.api.model;

import java.math.BigInteger;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class SettlementsResponse extends APIResponse implements Comparable<SettlementsResponse> {

  private String SettlementAmount;
  private String creditAccountNumber;
  private String debitAccountNumber;
  private String requestTime;
  private String payDate;
  private Integer status;
  private String bankResponse;
  private String bankTxnRefId;

  private List<Transactions> transactions;

  @JsonInclude(Include.NON_NULL)
  public static class Transactions {
    private String merchantTransactionRefId;
    private String ostaTransactionRefId;
    private String orderId;
    private String amount;
    private String txnTime;
    private String type;
    private String merchantAmount;
    
    private String ostaCommission;
    private String ostaGst;
    private String pgCharge;
    private String pgGst;
    private String pgReferenceId;

    public String getMerchantTransactionRefId() {
      return merchantTransactionRefId;
    }

    public void setMerchantTransactionRefId(String merchantTransactionRefId) {
      this.merchantTransactionRefId = merchantTransactionRefId;
    }

    public String getOstaTransactionRefId() {
      return ostaTransactionRefId;
    }

    public void setOstaTransactionRefId(String ostaTransactionRefId) {
      this.ostaTransactionRefId = ostaTransactionRefId;
    }

    public String getOrderId() {
      return orderId;
    }

    public void setOrderId(String orderId) {
      this.orderId = orderId;
    }

    public String getAmount() {
      return amount;
    }

    public void setAmount(String amount) {
      this.amount = amount;
    }

    public String getTxnTime() {
      return txnTime;
    }

    public void setTxnTime(String txnTime) {
      this.txnTime = txnTime;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getMerchantAmount() {
      return merchantAmount;
    }

    public void setMerchantAmount(String merchantAmount) {
      this.merchantAmount = merchantAmount;
    }
    
    public String getOstaCommission() {
        return ostaCommission;
      }

      public void setOstaCommission(String ostaCommission) {
        this.ostaCommission = ostaCommission;
      }

      public String getOstaGst() {
        return ostaGst;
      }

      public void setOstaGst(String ostaGst) {
        this.ostaGst = ostaGst;
      }

      public String getPgCharge() {
        return pgCharge;
      }

      public void setPgCharge(String pgCharge) {
        this.pgCharge = pgCharge;
      }

      public String getPgGst() {
        return pgGst;
      }

      public void setPgGst(String pgGst) {
        this.pgGst = pgGst;
      }

      public String getPgReferenceId() {
        return pgReferenceId;
      }

      public void setPgReferenceId(String pgReferenceId) {
        this.pgReferenceId = pgReferenceId;
      }
  }

  public List<Transactions> getTransactions() {
      return transactions;
    }

    public void setTransactions(List<Transactions> transactions) {
      this.transactions = transactions;
    }

  public String getCreditAccountNumber() {
    return creditAccountNumber;
  }

  public void setCreditAccountNumber(String creditAccountNumber) {
    this.creditAccountNumber = creditAccountNumber;
  }

  public String getDebitAccountNumber() {
    return debitAccountNumber;
  }

  public void setDebitAccountNumber(String debitAccountNumber) {
    this.debitAccountNumber = debitAccountNumber;
  }

  public String getRequestTime() {
    return requestTime;
  }

  public void setRequestTime(String requestTime) {
    this.requestTime = requestTime;
  }

  public String getPayDate() {
    return payDate;
  }

  public void setPayDate(String payDate) {
    this.payDate = payDate;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getBankResponse() {
    return bankResponse;
  }

  public void setBankResponse(String bankResponse) {
    this.bankResponse = bankResponse;
  }

  public String getBankTxnRefId() {
    return bankTxnRefId;
  }

  public void setBankTxnRefId(String bankTxnRefId) {
    this.bankTxnRefId = bankTxnRefId;
  }

  public String getSettlementAmount() {
    return SettlementAmount;
  }

  public void setSettlementAmount(String settlementAmount) {
    SettlementAmount = settlementAmount;
  }

  



  private List<SettlementsResponse> settlements;
  private Pagination pagination;

  public List<SettlementsResponse> getSettlements() {
    return settlements;
  }

  public void setSettlements(List<SettlementsResponse> settlements) {
    this.settlements = settlements;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public void setPagination(Pagination pagination) {
    this.pagination = pagination;
  }

  @Override
  public int compareTo(SettlementsResponse settlementsResponse) {
    BigInteger requestTime = new BigInteger(settlementsResponse.getRequestTime());
    BigInteger time = new BigInteger(this.requestTime);
    return requestTime.compareTo(time);
  }

}
