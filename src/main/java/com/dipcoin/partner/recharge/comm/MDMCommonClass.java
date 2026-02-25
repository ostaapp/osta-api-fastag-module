package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;
import com.dipcoin.partner.recharge.comm.BillMerchantStatusDetails.ChannelDetails.Fields;

public class MDMCommonClass extends HashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  public static interface Fields {
    public static String REQUEST_TYPE = "RequestType";
    public static String MERCHANT_REFERENCE_NUMBER = "MerchantRefNo";
    public static String MERCHANT_CODE = "MerchantCode";
    public static String USER_NAME = "UserName";
    public static String USER_PASS = "UserPass";
    public static String STORE_CODE = "StoreCode";
    public static String ENTITY_TYPE_ID = "EntityTypeId";
    public static String CHANNEL_DETAILS = "ChannelDetails";
    public static String AGENT_ID = "AgentId";
    public static String SEARCH_BY_DATE = "SearchByDate";
    public static String DATE = "Date";
    public static String REQUESTER_IP = "RequesterIP";
    public static String SEARCH_MY_BILLER = "Searchmybiller";
    public static String BILLER_ID = "BillerId";

  }

	public final Object getRequesterIP() {
		return this.get(Fields.REQUESTER_IP);
	}

	public final void setRequesterIP(String requesterIP) {
		this.put(Fields.REQUESTER_IP, requesterIP);
	}

	public final Object getSearchmybiller() {
		return this.get(Fields.SEARCH_MY_BILLER);
	}

	public final void setSearchmybiller(String searchmybiller) {
		this.put(Fields.SEARCH_MY_BILLER, searchmybiller);
	}
	
	public final Object getSearchByDate() {
		return this.get(Fields.SEARCH_BY_DATE);
	}

	public final void setSearchByDate(String searchByDate) {
		this.put(Fields.SEARCH_BY_DATE, searchByDate);
	}
	
	public final Object getDate() {
		return this.get(Fields.DATE);
	}

	public final void setDate(String date) {
		this.put(Fields.DATE, date);
	}
	  
  public final Object getRequestType() {
    return this.get(Fields.REQUEST_TYPE);
  }

  public final void setRequestType(String requestType) {
    this.put(Fields.REQUEST_TYPE, requestType);
  }

  public final Object getMerchantRefNo() {
    return this.get(Fields.MERCHANT_REFERENCE_NUMBER);
  }

  public final void setMerchantRefNo(String merchantRefNo) {
    this.put(Fields.MERCHANT_REFERENCE_NUMBER, merchantRefNo);
  }

  public final Object getMerchantCode() {
    return this.get(Fields.MERCHANT_CODE);
  }

  public final void setMerchantCode(String merchantCode) {
    this.put(Fields.MERCHANT_CODE, merchantCode);
  }

  public final Object getUserName() {
    return this.get(Fields.USER_NAME);
  }

  public final void setUserName(String userName) {
    this.put(Fields.USER_NAME, userName);
  }

  public final Object getUserPass() {
    return this.get(Fields.USER_PASS);
  }

  public final void setUserPass(String userPass) {
    this.put(Fields.USER_PASS, userPass);
  }

  public final Object getStoreCode() {
    return this.get(Fields.STORE_CODE);
  }

  public final void setStoreCode(String storeCode) {
    this.put(Fields.STORE_CODE, storeCode);
  }

  public final Object getEntityTypeId() {
    return this.get(Fields.ENTITY_TYPE_ID);
  }

  public final void setEntityTypeId(String entityTypeId) {
    this.put(Fields.ENTITY_TYPE_ID, entityTypeId);
  }
  
  public final Object getAgentId() {
    return this.get(Fields.AGENT_ID);
  }

  public final void setAgentId(String agentId) {
    this.put(Fields.AGENT_ID, agentId);
  }

  public final ChannelDetails getChannelDetails() {
    return (ChannelDetails) this.get(Fields.CHANNEL_DETAILS);
  }

  public final void setChannelDetails(ChannelDetails channelDetails) {
    this.put(Fields.CHANNEL_DETAILS, channelDetails);
  }
  
  public final Object getBillerId() {
    return this.get(Fields.BILLER_ID);
  }

  public final void setBillerId(String billerId) {
    this.put(Fields.BILLER_ID, billerId);
  }

  public static class ChannelDetails extends HashMap<String, Object> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static interface Fields {
      public static String CHANNELCODE = "ChannelCode";
      public static String CHANNELPARAMS = "ChannelParams";
    }

    public String getChannelCode() {
      return (String) this.get(Fields.CHANNELCODE);
    }

    public void setChannelCode(String channelCode) {
      this.put(Fields.CHANNELCODE, channelCode);
    }

    @SuppressWarnings("unchecked")
    public List<Details> getChannelParams() {
      return (List<Details>) this.get(Fields.CHANNELPARAMS);
    }

    public void setChannelParams(List<Details> channelParams) {
      this.put(Fields.CHANNELPARAMS, channelParams);
    }
  }

}
