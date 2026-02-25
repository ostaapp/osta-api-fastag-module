package com.dipcoin.api.resource;

import com.dipcoin.api.fraudMgmt.ProcessEvent.EventType;
import com.dipcoin.db.services.model.User;

public class UserEventResource {

	public boolean applyRule(User user, Object object, String string, String requestTime, EventType ostacreation,
			String dipcoincreationfailure) {
		// TODO Auto-generated method stub
		return false;
	}

	public void processInQueue(User user, String requestTime, String dipcoincreationsuccess, String string) {
		// TODO Auto-generated method stub
		
	}

}
