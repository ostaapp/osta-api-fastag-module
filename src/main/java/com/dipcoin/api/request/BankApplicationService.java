package com.dipcoin.api.request;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dipcoin.mock.bank.model.MarkLienRequest;
import com.dipcoin.mock.bank.model.MarkLienResponse;
import com.dipcoin.mock.bank.model.TransactionData;
import com.dipcoin.mock.bank.services.BankOperationDAO;
import com.dipcoin.mock.bank.services.TransactionDAOImpl;
import com.dipcoin.partner.utils.RandomGenerator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RequestMapping({"mock/bank"})
@Api(value = "mock/bank", description = "bank entry")
@Consumes({"application/json"})
@Produces({"application/json"})
@RestController
public class BankApplicationService {
	
	private static final Logger LOG = LogManager.getLogger(BankApplicationService.class);

	@Autowired
	private BankOperationDAO bankOperationDAO;

	@Autowired
	private TransactionDAOImpl transactionDAO;

	@GetMapping({ "/healthcheck" })
	public ResponseEntity verifyBankRESTService() {
		return ResponseEntity.status(200).body("{\"message\":\"OK\"}");
	}
	
	@PostMapping({ "/marklien" })
	@ApiOperation(value = "Mark Lien Request", notes = "API to Mark Lien Request", response = MarkLienResponse.class)
	public ResponseEntity markLien(@RequestBody MarkLienRequest markLienRequest) {
		try {
			LOG.debug("Mark Lien Request: " + markLienRequest.toString());
			String responseTime = String.valueOf(DateTime.now(DateTimeZone.UTC).getMillis());
			String bankTransactionReferenceNumber = RandomGenerator.generateRandom();
			TransactionData transactionData = new TransactionData();
			transactionData.setTransactionReferenceNumber(bankTransactionReferenceNumber);
			transactionData.setDipcoinRefNumber(markLienRequest.getDipcoinReferenceNumber());
			transactionData.setTransactionResTime(responseTime);
			transactionData.setRawRequest(markLienRequest.toString());
			this.transactionDAO.addTransactionDetails(transactionData);
			MarkLienResponse markLienResponse = this.bankOperationDAO.markLien(markLienRequest, transactionData);
			markLienResponse.setTransactionTime(responseTime);
			markLienResponse.setBankTransactionReferenceNumber(bankTransactionReferenceNumber);
			markLienResponse.setDipcoinReferenceNumber(markLienRequest.getDipcoinReferenceNumber());
			LOG.debug("Mark Lien Response:", markLienResponse.getBankResponseCode() + "  "
					+ markLienResponse.getBankResponseCode() + "  " + markLienResponse.getBankResponseDesc());
			return ResponseEntity.ok().body(markLienResponse);
		} catch (Exception e) {
			LOG.error("Error", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

}
