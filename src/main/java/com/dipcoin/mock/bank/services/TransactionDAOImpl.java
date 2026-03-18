package com.dipcoin.mock.bank.services;

import com.dipcoin.mock.bank.model.TransactionData;
import com.dipcoin.mock.bank.services.TransactionDAO;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component("tansactionDAO")
@PropertySource({ "classpath:query.properties" })
public class TransactionDAOImpl implements TransactionDAO {
	private static final Logger LOG = LogManager.getLogger(com.dipcoin.mock.bank.services.TransactionDAOImpl.class);

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return this.namedParameterJdbcTemplate;
	}

	public TransactionData addTransactionDetails(TransactionData transactionData) {
		String SQL = "INSERT INTO DipcoinTransactions (DipcoinAccountID,  BankID,  DipcoinReferenceNumber, BankTransactionReferenceNumber, TransactionTypeId, TransactionTypeDesc, TransactionRequestTime,  TransactionResponseTime, AttemptCount ,  TransactionStatus, BankRespDesc, RawRequest ) VALUES (:dipcoinAccountId, :bankId, :dipcoinReferenceNumber, :bankTransactionReferenceNumber,:transactionTypeId, :transactionTypeDesc, :transactionRequestTime, :transactionResponseTime, :attemptCount, :transactionStatus, :bankRespDesc, :rawData)";
		try {
			Map<String, String> namedParameters = new HashMap<>();
			namedParameters.put("dipcoinAccountId", transactionData.getDipcoinAccountId());
			namedParameters.put("bankId", transactionData.getBankID());
			namedParameters.put("dipcoinReferenceNumber", transactionData.getDipcoinRefNumber());
			namedParameters.put("bankTransactionReferenceNumber", transactionData.getTransactionReferenceNumber());
			namedParameters.put("transactionTypeId", transactionData.getTransactionType());
			namedParameters.put("transactionTypeDesc", transactionData.getTransactionTypeDesc());
			namedParameters.put("transactionRequestTime", transactionData.getTransactionReqTime());
			namedParameters.put("transactionResponseTime", transactionData.getTransactionResTime());
			namedParameters.put("attemptCount", transactionData.getAttempt());
			namedParameters.put("transactionStatus", transactionData.getTransactionStatus());
			namedParameters.put("bankRespDesc", transactionData.getTransactionDesc());
			namedParameters.put("rawData", transactionData.getRawRequest());
			GeneratedKeyHolder generatedKeyHolder = new GeneratedKeyHolder();
			MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource(namedParameters);
			int status = this.namedParameterJdbcTemplate.update(SQL, (SqlParameterSource) mapSqlParameterSource,
					(KeyHolder) generatedKeyHolder);
			long l = generatedKeyHolder.getKey().longValue();
		} catch (Exception e) {
			LOG.error("Error", e);
		}
		return transactionData;
	}

	public TransactionData updateTransactionResponse(TransactionData updatedTransactionData) {
		String SQL = "UPDATE transactions set status = :status where TransactionId = :transactionId";
		try {
			Map<String, String> namedParameters = new HashMap<>();
			int i = this.namedParameterJdbcTemplate.update(SQL, namedParameters);
		} catch (Exception e) {
			LOG.error("Error", e);
		}
		return updatedTransactionData;
	}
}
