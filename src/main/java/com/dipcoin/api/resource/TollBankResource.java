package com.dipcoin.api.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.TollTagRequest;
import com.dipcoin.api.model.TollTagUpdateResponse;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;

public class TollBankResource {

	public TollTagUpdateResponse uploadTheTollListToNETC(HttpServletContext httpServletContext, List<TollTag> asList,
			String addOp, Bank bank, Merchant merchant) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResponseEntity getTagsRejectedPendingCounts(User user, String bankReferenceId) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResponseEntity getTollCustomerDetails(User user, String bankReferenceId, String status, Long startTime,
			Long endTime, Integer start, Integer count, String vehicleNumber, String accountNumber, String serialNumber,
			String branchCode, String phone) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResponseEntity getTollTagCharges(User user, String encryptedTTID) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResponseEntity updateTollCustomerByVendor(User user, Bank bank, TollTagRequest updateReq,
			String clientTransactionId) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResponseEntity fetchApprovalstatus(String serialNumber) {
		// TODO Auto-generated method stub
		return null;
	}

}
