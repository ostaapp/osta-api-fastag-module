package com.dipcoin.api.resource;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.springframework.util.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.commons.LogFormatter;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APICustomization;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.BankInfoResponse;
import com.dipcoin.api.model.BanksResponse;
import com.dipcoin.api.model.ChargebackResponse;
import com.dipcoin.api.model.Pagination;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants.BankStatus;
import com.dipcoin.db.services.commons.DBConstants.BankType;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Chargeback;
import com.dipcoin.db.services.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("bankResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class BankResource extends PartnerResource {

	protected static boolean encryptCardId = false;
	private static final Logger LOG = LogManager.getLogger(BankResource.class);
	private final static ObjectMapper objectMapper = new ObjectMapper();
	private static final String dateFormatter = "yyyy-MM-dd hh:mm:ss";
	private static final TimeZone dateTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
	private static final String NA = "NA";

	@Autowired
	private UserDBService userDBService;
	
	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;
	
	@Autowired
	private BankDBService bankDBService;
	
	@Autowired
	private ChargebackResource chargebackResource;
	
	/*
	 * Get banks info list
	 */
	public ResponseEntity getBanks(final User user, Integer status, Integer type, Integer start, Integer count)
			throws Exception {

		boolean match = false;
		for (BankStatus s : BankStatus.values()) {
			if (status != null && s.value() == status) {
				match = true;
				break;
			}
		}
		if (status != null && !match) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.INVALID_BANK_STATUS));
		}

		boolean isTypeMatch = false;
		for (BankType t : BankType.values()) {
			if (type != null && t.value() == type) {
				isTypeMatch = true;
				break;
			}
		}
		if (type != null && !isTypeMatch) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.INVALID_BANK_TYPE));
		}

		// if not Dipcoin admin/superadmin, return ONLY active banks
		if (!(this.userDBService.isBrontooSuperAdmin(user) || this.userDBService.isBrontooAdmin(user))) {
			status = BankStatus.ACTIVE.value();
		}

		if (start == null)
			start = 0;
		if (count == null)
			count = APIConstants.MAX_PAGINATE_COUNT;

		List<Integer> statuses = Arrays.asList(status);
		if (type == null) {
			type = BankType.BANK.value();
		}
		List<Integer> types = Arrays.asList(type);

		List<BankInfoResponse> banks = new LinkedList<>();
		List<Bank> bankList = this.bankDBService.getBanks(statuses, types, start, count);
		if (CollectionUtils.isEmpty(bankList)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		for (Bank bank : bankList) {

			BankInfoResponse response = new BankInfoResponse();
			response.setName(bank.getName());
			response.setPartnerReferenceId(bank.getReferenceId());
			response.setType(bank.getType());
			response.setTollStatus(bank.getTollStatus());
			response.setOrgId(StringUtils.isBlank(bank.getOrgId()) ? StringUtils.EMPTY : bank.getOrgId());

			if (!(this.userDBService.isBrontooSuperAdmin(user) || this.userDBService.isBrontooAdmin(user))) {
				if (status == BankStatus.ACTIVE.value() && StringUtils.isEmpty(bank.getAuthenticationMethods())) {
					LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
							.message("Failed to fetch authentication methods").data("Bank", bank.getId()).format());
					continue;
				}
			}
			response.setAuthenticationMethodsAsString(bank.getAuthenticationMethods());
			if (!StringUtils.isBlank(bank.getApiCustomization())) {
				Object alpha = objectMapper.readValue(bank.getApiCustomization(), Object.class);
				response.setApiCustomization((List<APICustomization>) alpha);

			}
			// @TODO - add more fields as required

			banks.add(response);
		}

		Pagination pagination = new Pagination();
		pagination.setScanCompleted(banks.size() < count);
		pagination.setTotal(banks.size());
		if (!pagination.getScanCompleted() && start != null && count != null) {
			pagination.setStart(start + count);
		}

		BanksResponse response = new BanksResponse();
		response.setBanks(banks);
		response.setPagination(pagination);

		return ResponseEntity.ok(response);
	}
	
	public ResponseEntity getChargeBackTransactions(User user, String partnerRefId, Integer status,
		      Long startDate, Long endDate, String partner, Integer start, Integer count) {

		    LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
		        .data("BankUserId :-", user.getId()).format());

		    ChargebackResponse chargebackResponse = new ChargebackResponse();
		    List<Chargeback> chargebackTransaction = new ArrayList<Chargeback>();
		    
		    
		    if(!this.userDBService.bankRepresentative(user)) {
		      LOG.error(HeaderCode.USER_UNAUTHORIZED);

		      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
		          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED));
		    }

		    Bank bank = this.bankDBService.getBank(user.getBankMerchantId());

		    if (bank == null) {
		      LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
		          .data("bank :-", HttpStatus.BAD_REQUEST).format());
		      chargebackResponse.addHeaderCode(HeaderCode.INVALID_REQUEST);

		      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(chargebackResponse);
		    }

		    chargebackTransaction = chargebackResource.fetchChargebackTransaction(null, bank.getId(),
		        status, startDate, endDate, start, count);

		    chargebackResponse.setChargebackTransaction(chargebackTransaction);
		    return ResponseEntity.ok(chargebackResponse);
		  }

}
