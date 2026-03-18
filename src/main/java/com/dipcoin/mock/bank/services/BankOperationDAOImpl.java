package com.dipcoin.mock.bank.services;


import com.dipcoin.mock.bank.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Component;

@Component("bankOperationDAO")
@PropertySource({"classpath:query.properties"})
public class BankOperationDAOImpl implements BankOperationDAO {
  private static final Logger LOG = LogManager.getLogger(com.dipcoin.mock.bank.services.BankOperationDAOImpl.class);
  
  private SimpleJdbcCall simpleJdbcCall;
  
  @Autowired
  private DataSource dataSource;
  
  @Autowired
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  
  @Autowired
  private Environment env;
  
  public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
    return this.namedParameterJdbcTemplate;
  }
  
  @Override
  public MarkLienResponse markLien(MarkLienRequest markLienRequest, TransactionData transactionData) throws Exception {
    MarkLienResponse response = new MarkLienResponse();
    this.simpleJdbcCall = (new SimpleJdbcCall(this.dataSource)).withProcedureName("Marklien");
    String responseCode = "", responseDescription = "", cbsJournalNumber = "";
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
    LocalDate localDate = LocalDate.now();
    String cbsDate = dtf.format(localDate);
    try {
      MapSqlParameterSource sqlParamMap = new MapSqlParameterSource();
      sqlParamMap.addValue("lienMarkAttempt", markLienRequest.getAttempt());
      sqlParamMap.addValue("bkUID", markLienRequest.getBankUID());
      sqlParamMap.addValue("lienAmtRequested", markLienRequest.getAmount());
      sqlParamMap.addValue("bankID", markLienRequest.getBankID());
      sqlParamMap.addValue("transactionTime", markLienRequest.getTransactionTime());
      sqlParamMap.addValue("bankTransactionRefNo", transactionData.getTransactionReferenceNumber());
      sqlParamMap.addValue("currency", markLienRequest.getCurrency());
      sqlParamMap.addValue("cbsDate", cbsDate);
      MapSqlParameterSource mapSqlParameterSource1 = sqlParamMap;
      Map<String, Object> out = this.simpleJdbcCall.execute((SqlParameterSource)mapSqlParameterSource1);
      responseCode = out.get("responsecode").toString();
      responseDescription = out.get("responsestr").toString();
      cbsJournalNumber = out.get("cbsJournalNumber").toString();
    } catch (Exception e) {
      responseCode = "2";
      responseDescription = " Lien mark failure : Database error";
      LOG.error("Error", e);
    } 
    response.setCbsJournalNumber(cbsJournalNumber);
    response.setBankResponseCode(responseCode);
    response.setBankResponseDesc(responseDescription);
    response.setCbsDate(cbsDate);
    return response;
  }
  
  @Override
  public RemoveLienResponse removeLien(RemoveLienRequest removeLienRequest, TransactionData transactionData) {
    RemoveLienResponse response = new RemoveLienResponse();
    this.simpleJdbcCall = (new SimpleJdbcCall(this.dataSource)).withProcedureName("Removelien");
    String responseCode = "", responseDescription = "";
    try {
      MapSqlParameterSource sqlParamMap = new MapSqlParameterSource();
      sqlParamMap.addValue("lienRemovalAttempt", removeLienRequest.getAttempt());
      sqlParamMap.addValue("bkUID", removeLienRequest.getBankUID());
      sqlParamMap.addValue("lienAmtRequested", removeLienRequest.getAmount());
      sqlParamMap.addValue("bankID", removeLienRequest.getBankID());
      sqlParamMap.addValue("transactionTime", removeLienRequest.getTransactionTime());
      sqlParamMap.addValue("bankTransactionRefNo", transactionData.getTransactionReferenceNumber());
      sqlParamMap.addValue("cbsJournalNumber", removeLienRequest.getCbsJournalNumber());
      sqlParamMap.addValue("currency", removeLienRequest.getCurrency());
      sqlParamMap.addValue("cbsDate", removeLienRequest.getCbsDate());
      MapSqlParameterSource mapSqlParameterSource1 = sqlParamMap;
      Map<String, Object> out = this.simpleJdbcCall.execute((SqlParameterSource)mapSqlParameterSource1);
      responseCode = out.get("responsecode").toString();
      responseDescription = out.get("responsestr").toString();
    } catch (Exception e) {
      responseCode = "2";
      responseDescription = " Remove Lien failure : Database error";
      LOG.error("Error", e);
    } 
    response.setBankResponseCode(responseCode);
    response.setBankResponseDesc(responseDescription);
    return response;
  }
  
  @Override
  public CustomerFundTransferResponse customerFundTransfer(CustomerFundTransferRequest customerFundTransferRequest, TransactionData transactionData) {
    CustomerFundTransferResponse response = new CustomerFundTransferResponse();
    if (customerFundTransferRequest.getTransactionType().equals("6")) {
      this.simpleJdbcCall = (new SimpleJdbcCall(this.dataSource)).withProcedureName("DebitFromCustAccount");
    } else if (customerFundTransferRequest.getTransactionType().equals("7")) {
      this.simpleJdbcCall = (new SimpleJdbcCall(this.dataSource)).withProcedureName("CreditToCustAccount");
    } 
    String responseCode = "", responseDescription = "";
    try {
      MapSqlParameterSource sqlParamMap = new MapSqlParameterSource();
      sqlParamMap.addValue("attempt", customerFundTransferRequest.getAttempt());
      sqlParamMap.addValue("bkUID", customerFundTransferRequest.getBankUID());
      sqlParamMap.addValue("amount", customerFundTransferRequest.getAmount());
      sqlParamMap.addValue("bankTransactionRefNo", transactionData.getTransactionReferenceNumber());
      sqlParamMap.addValue("bankID", customerFundTransferRequest.getBankID());
      sqlParamMap.addValue("transactionTime", customerFundTransferRequest.getTransactionTime());
      sqlParamMap.addValue("account", customerFundTransferRequest.getAccount());
      sqlParamMap.addValue("currency", customerFundTransferRequest.getCurrency());
      sqlParamMap.addValue("cbsDate", customerFundTransferRequest.getCbsDate());
      MapSqlParameterSource mapSqlParameterSource1 = sqlParamMap;
      Map<String, Object> out = this.simpleJdbcCall.execute((SqlParameterSource)mapSqlParameterSource1);
      responseCode = out.get("responsecode").toString();
      responseDescription = out.get("responsestr").toString();
    } catch (Exception e) {
      responseCode = "2";
      responseDescription = " Debit Account failure : Database error";
      LOG.error("Error", e);
    } 
    response.setBankResponseCode(responseCode);
    response.setBankResponseDesc(responseDescription);
    return response;
  }
  
  @Override
  public AccountFundTransferResponse accountFundTransfer(AccountFundTransferRequest accountFundTransferRequest, TransactionData transactionData) {
    AccountFundTransferResponse response = new AccountFundTransferResponse();
    this.simpleJdbcCall = (new SimpleJdbcCall(this.dataSource)).withProcedureName("AccountFundTransfer");
    String responseCode = "", responseDescription = "";
    try {
      MapSqlParameterSource sqlParamMap = new MapSqlParameterSource();
      sqlParamMap.addValue("attempt", accountFundTransferRequest.getAttempt());
      sqlParamMap.addValue("transactionType", accountFundTransferRequest.getTransactionType());
      sqlParamMap.addValue("dipcoinReferenceNumber", accountFundTransferRequest.getDipcoinReferenceNumber());
      sqlParamMap.addValue("bankTransactionRefNo", transactionData.getTransactionReferenceNumber());
      sqlParamMap.addValue("bankID", accountFundTransferRequest.getBankID());
      sqlParamMap.addValue("transactionTime", accountFundTransferRequest.getTransactionTime());
      sqlParamMap.addValue("currency", accountFundTransferRequest.getCurrency());
      sqlParamMap.addValue("amount", accountFundTransferRequest.getAmount());
      sqlParamMap.addValue("toAccount", accountFundTransferRequest.getToAccount());
      sqlParamMap.addValue("fromAccount", accountFundTransferRequest.getFromAccount());
      sqlParamMap.addValue("toAccountIFSCCode", accountFundTransferRequest.getToAccountIFSCCode());
      sqlParamMap.addValue("fromAccountIFSCCode", accountFundTransferRequest.getFromAccountIFSCCode());
      MapSqlParameterSource mapSqlParameterSource1 = sqlParamMap;
      Map<String, Object> out = this.simpleJdbcCall.execute((SqlParameterSource)mapSqlParameterSource1);
      responseCode = out.get("responsecode").toString();
      responseDescription = out.get("responsestr").toString();
    } catch (Exception e) {
      responseCode = "2";
      responseDescription = "Account fund transfer failure : Database error";
      LOG.error("Error", e);
    } 
    response.setBankResponseCode(responseCode);
    response.setBankResponseDesc(responseDescription);
    return response;
  }
  
  @Override
  public DeleteAccountResponse deleteAccount(DeleteAccountRequest deleteAccountRequest, TransactionData transactionData) {
    DeleteAccountResponse response = new DeleteAccountResponse();
    String SQL = "UPDATE DipcoinAccount set AccountStatus = :accountStatus, AccountStatusDesc = :accountStatusDesc,  DipcoinRequestTime = :transactionTime, BankTransactionReferenceNum = :bankTransactionNo WHERE BankUID =  :bankUID and BankID = :bankID";
    String responseCode = "", responseDescription = "";
    int status = 0;
    try {
      Map<String, String> namedParameters = new HashMap<>();
      namedParameters.put("accountStatus", "2");
      namedParameters.put("accountStatusDesc", "Dipcoin Account deleted");
      namedParameters.put("transactionTime", deleteAccountRequest.getTransactionTime());
      namedParameters.put("bankTransactionNo", transactionData.getTransactionReferenceNumber());
      namedParameters.put("bankUID", deleteAccountRequest.getBankUID());
      namedParameters.put("bankID", deleteAccountRequest.getBankID());
      status = this.namedParameterJdbcTemplate.update(SQL, namedParameters);
      response = buildDeleteAccountResponse(deleteAccountRequest);
      responseCode = (status == 1) ? "0" : "2";
      responseDescription = "Delete Account successfully";
    } catch (Exception e) {
      responseCode = "2";
      responseDescription = " Delete Account failure : Database error";
      LOG.error("Error", e);
    } 
    response.setBankResponseCode(responseCode);
    response.setBankResponseDesc(responseDescription);
    return response;
  }
  
  private DeleteAccountResponse buildDeleteAccountResponse(DeleteAccountRequest deleteAccountRequest) {
    DeleteAccountResponse deleteAccountResponse = new DeleteAccountResponse();
    deleteAccountResponse.setDipcoinReferenceNumber(deleteAccountRequest.getDipcoinReferenceNumber());
    return deleteAccountResponse;
  }
  
  
  
  public class UserMapper implements RowMapper<UserData> {
	    public UserData mapRow(ResultSet rs, int rowNum) throws SQLException {
	      UserData user = new UserData();
	      if (isColumnPresent(rs, "ID"))
	        user.setId(rs.getString("Id")); 
	      if (isColumnPresent(rs, "bankID"))
	        user.setBankID(rs.getString("bankID")); 
	      if (isColumnPresent(rs, "IFSCCode"))
	        user.setIfscCode(rs.getString("ifscCode")); 
	      if (isColumnPresent(rs, "accountNumber"))
	        user.setAccountNumber(rs.getString("accountNumber")); 
	      if (isColumnPresent(rs, "authenticationId"))
	        user.setAuthenticationId(rs.getString("authenticationId")); 
	      if (isColumnPresent(rs, "authenticationType"))
	        user.setAuthenticationType(rs.getString("authenticationType")); 
	      if (isColumnPresent(rs, "password"))
	        user.setPassword(rs.getString("password")); 
	      if (isColumnPresent(rs, "otp"))
	        user.setOtpCode(rs.getString("otp")); 
	      if (isColumnPresent(rs, "availableAmount"))
	        user.setAvailableAmount(rs.getString("availableAmount")); 
	      return user;
	    }
	    
	    private boolean isColumnPresent(ResultSet rs, String column) {
	      try {
	        rs.findColumn(column);
	        return true;
	      } catch (SQLException sQLException) {
	        return false;
	      } 
	    }
	  }


}
