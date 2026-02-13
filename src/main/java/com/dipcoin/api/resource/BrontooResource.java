package com.dipcoin.api.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.db.services.model.Bank;

@Component("brontooResource")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class BrontooResource {

	public ResponseEntity syncTime(Bank bank) {
		// TODO Auto-generated method stub
		return null;
	}

}
